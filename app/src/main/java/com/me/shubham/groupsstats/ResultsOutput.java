package com.me.shubham.groupsstats;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*NOTE
* This job could theoretically be done by using paging and was initially done using this method.
* (This involves creating a feed with the parameter 'since' = '1', reading all the posts on the
* current page, and simply calling: response.getRequestForPagedResults(Response.PagingDirection.NEXT)).
*
* This method is not current used because the Facebook Graph Explorer is not accurate with this.
* With a bit of debugging, I was able to go ~1 year back in a group of mine using this method and
* noticed that the response gave me this nugget:
* {
*   "id": <id>
*   "updated_time": "2014-01-14T00:30:20+0000"
* },
* {
*   "id": <id>
*   "updated_time": "2013-09-07T17:07:50+0000"
* }
*
* This seemed odd so I used a new request and set the 'until' parameter to the Unix time corresponding
* to the first post. This revealed that there were in fact MANY posts that the response had just ignored.
* This was not a problem for smaller groups where the number of posts was probably <200 so it seems
* like either this is a bug or (more likely) sending large amounts of data is costly to Facebook
* and is working to limit the amount of data (just a guess). Either way, this simpler method is
* something I had to abandon because of the lack of accuracy.
*
* Instead, this repeatedly creates new request which seems to remove most of the problems although I
* worry about a VERY large group and how the 100 post request will be handled. Because I need an end
* date, I initially picked 2006 as an arbitrary limit. I changed this when I noticed that while the
* middle portion of the response is very inaccurate, the ending and beginning are very accurate (????)
* so I get the last date using paging. This is to ensure any posts made before 2006 are included. The
* performance difference between this and using 2006 is not clear and the variation between groups is
* something I need to test.
*
* If Facebook fixes their feed issues, I will switch the code over to the more efficient way said earlier.
* */


public class ResultsOutput extends ActionBarActivity {


    //Number of seconds between the 'since' and 'until' parameter
    //Currently set to two weeks
    private static final long SEARCH_INTERVAL = 1209600;


    //ID of the group passed by the intent
    String groupID;


    //Determines which results the user wants
    //0 = People who have posted the most
    //1 = People who have the most liked posts
    int inputChoice;


    //Stores the results of processing the feed
    //This is what is displayed
    HashMap<String, Integer> resultsMap;


    //Temporary TextView to output the results
    TextView testingOutputTextView;


    ProgressDialog loadingDialog;

    //Read names
    private long unixTimeOfLatestPost;
    private long unixTimeOfLastPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_output);

        Intent intent = getIntent();
        inputChoice = intent.getIntExtra("inputChoice", -1);
        groupID = intent.getStringExtra("groupID");

        if (inputChoice == 0)
            getSupportActionBar().setTitle("Most posts");
        else
            getSupportActionBar().setTitle("Most liked posters");

        testingOutputTextView = (TextView) findViewById(R.id.testingOutputTextView);


        //If something horribly HORRIBLY long (or a little wrong)
        if (inputChoice == -1 || groupID == null)
            testingOutputTextView.setText("Error: Input Choice: " + inputChoice + ",  groupID: " + groupID);
        else {
            resultsMap = new HashMap<>();

            loadingDialog = new ProgressDialog(ResultsOutput.this);
            loadingDialog.setMessage("Getting Posts...");
            loadingDialog.show();

            getLatestPostUnixTime();
        }
    }

    private void getLatestPostUnixTime() {
        loadingDialog.setMessage("Getting First Post...");

        Bundle parameters = new Bundle();
        parameters.putString("limit", "1000");
        parameters.putString("since", "1");
        parameters.putString("fields", "updated_time");

        new Request(Session.getActiveSession(), groupID + "/feed", parameters, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                GraphObject allGraphObject = response.getGraphObject();
                if (allGraphObject != null) {
                    try {
                        JSONArray posts = allGraphObject.getInnerJSONObject().getJSONArray("data");
                        if (posts.length() == 0)
                            testingOutputTextView.setText("Your group has no posts...");
                        else {
                            JSONObject firstPost = posts.getJSONObject(0);
                            String updatedTime = firstPost.get("updated_time").toString();
                            unixTimeOfLatestPost = getTime(updatedTime);

                            getLastPostUnixTime(response);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).executeAsync();
    }


    private void getLastPostUnixTime(Response response) {
        loadingDialog.setMessage("Getting Last Post...");

        GraphObject allGraphObject = response.getGraphObject();
        if (allGraphObject != null) {
            try {
                JSONArray posts = allGraphObject.getInnerJSONObject().getJSONArray("data");
                if (posts.length() != 0) {
                    String latestPostUpdatedTime = posts.getJSONObject(posts.length() - 1).get("updated_time").toString();
                    unixTimeOfLastPost = getTime(latestPostUpdatedTime);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Request next = response.getRequestForPagedResults(Response.PagingDirection.NEXT);
        if (next != null) {
            next.setCallback(new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    getLastPostUnixTime(response);
                }
            });
            next.executeAsync();
        } else {
            //Request for posts starting at the time of the first post and DO read the first post
            loadingDialog.setMessage("Processing Posts...");
            getFeed(unixTimeOfLatestPost, true);
        }
    }


    private void getFeed(final long upperLimit, final boolean processFirstPost) {

        if (upperLimit >= unixTimeOfLastPost) {//make sure you haven't exceed the last post
            final long lowerLimit = upperLimit - SEARCH_INTERVAL;

            Bundle parameters = new Bundle();
            parameters.putString("limit", "100");//Not higher because the feed is unreliable. Testing?
            parameters.putString("since", String.valueOf(lowerLimit));
            parameters.putString("until", String.valueOf(upperLimit));

            //Inclusion of 'from' loads an object with 'name' and 'id' of the poster
            //Inclusion of 'likes' loads an object with an array which holds an 'id' and a 'name' of the liker
            if (inputChoice == 1)
                parameters.putString("fields", "from, likes.summary(true)");
            else
                parameters.putString("fields", "from");

            new Request(Session.getActiveSession(), (groupID) + "/feed", parameters, HttpMethod.GET, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    //testingOutputTextView.setText(response.toString());
                    processFeed(processFirstPost, response);
                    getNextResponse(response, upperLimit);
                }
            }).executeAsync();
        } else
            publishResults();
    }


    private void processFeed(boolean processFirstPost, Response response) {
        GraphObject allDataGraphObject = response.getGraphObject();
        if (allDataGraphObject != null) {
            try {
                JSONArray listOfPosts = allDataGraphObject.getInnerJSONObject().getJSONArray("data");
                for (int numPost = processFirstPost ? 0 : 1; numPost < listOfPosts.length(); numPost++) {
                    JSONObject currentPost = listOfPosts.getJSONObject(numPost);
                    addToMap(currentPost);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private void getNextResponse(Response response, long oldUpperLimit) {
        GraphObject allDataGraphObject = response.getGraphObject();
        if (allDataGraphObject != null) {
            try {
                long oldLowerLimit = oldUpperLimit - SEARCH_INTERVAL;
                JSONArray listOfPosts = allDataGraphObject.getInnerJSONObject().getJSONArray("data");
                int length = listOfPosts.length();
                if (length == 0)
                    getFeed(oldLowerLimit, true);
                else {
                    String lastPostTime = listOfPosts.getJSONObject(listOfPosts.length() - 1).get("updated_time").toString();
                    long lastPostUnixTime = getTime(lastPostTime);
                    if (length == 1 && lastPostUnixTime == oldUpperLimit)
                        getFeed(oldLowerLimit, true);
                    else
                        getFeed(lastPostUnixTime, false);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private void addPostsToMap(JSONObject currentPost) {
        try {
            String currentPerson = currentPost.getJSONObject("from").get("name").toString();
            Integer numTimes = resultsMap.get(currentPerson);

            if (numTimes == null)
                resultsMap.put(currentPerson, 1);
            else
                resultsMap.put(currentPerson, numTimes + 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addLikesToMap(JSONObject currentPost) {
        try {
            String currentPerson = currentPost.getJSONObject("from").get("name").toString();
            Integer numTimes = resultsMap.get(currentPerson);

            if (currentPost.has("likes")) {
                Integer total_count = (Integer) currentPost.getJSONObject("likes").getJSONObject("summary").get("total_count");

                if (numTimes == null)
                    resultsMap.put(currentPerson, total_count);
                else
                    resultsMap.put(currentPerson, numTimes + total_count);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void addToMap(JSONObject currentPost) {
        if (inputChoice == 0)
            addPostsToMap(currentPost);
        else if (inputChoice == 1)
            addLikesToMap(currentPost);
    }


    private void publishResults() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(resultsMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> lhs, Map.Entry<String, Integer> rhs) {
                return rhs.getValue() - lhs.getValue();
            }
        });

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : list)
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");

        loadingDialog.dismiss();
        testingOutputTextView.setText(sb.toString());
    }


    private long getTime(String time) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            long dateinMili = df.parse(time).getTime();
            return dateinMili / 1000;
        } catch (ParseException e) {
            return -1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_likes_output, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Session.getActiveSession().closeAndClearTokenInformation();

        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);

        return true;
    }
}
