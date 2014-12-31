package com.me.shubham.groupsstats;

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
* (This involves creating a feed with the parameter 'since' 1, reading all the posts on the current page,
* and simply calling: response.getRequestForPagedResults(Response.PagingDirection.NEXT)).
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
* date, I picked 2006. This is just seemed far back enough for me but this is only for me.
*
* If Facebook fixes their feed issues, I will switch the code over to the more efficient way said earlier.
* */

public class CommentsOutput extends ActionBarActivity {

    //Number of seconds between the 'since' and 'until parameter'
    //Currently set to two weeks
    private static final long SEARCH_INTERVAL = 1209600;

    //Only posts whose 'updated_time' is after this will be considered
    //This is an arbitrary date. See the note before the class
    //Currently set to 2006
    private static final long LAST_DATE = 1136073600;


    String groupID;
    HashMap<String, Integer> fieldsMap;
    TextView textView;
    private long unixTimeOfLatestPost;
    //ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments_output);

        Intent intent = getIntent();
        groupID = intent.getStringExtra("groupID");
        fieldsMap = new HashMap<>();

        textView = (TextView) findViewById(R.id.CommentsTesting);

        //loadingDialog = new ProgressDialog(CommentsOutput.this);
        //loadingDialog.setMessage("Getting Posts...");
        //loadingDialog.show();

        getLatestPostUnixTime();
    }

    private void getLatestPostUnixTime() {


        Bundle parameters = new Bundle();
        parameters.putString("limit", "100");
        parameters.putString("since", "1");
        parameters.putString("fields", "from");

        new Request(Session.getActiveSession(), groupID + "/feed", parameters, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                GraphObject allGraphObject = response.getGraphObject();
                if (allGraphObject != null) {
                    try {
                        JSONObject post = allGraphObject.getInnerJSONObject().getJSONArray("data").getJSONObject(0);
                        String updatedTime = post.get("updated_time").toString();
                        unixTimeOfLatestPost = getTime(updatedTime);
                        getFeed(unixTimeOfLatestPost, true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).executeAsync();
    }

    private void getFeed(final long upperLimit, final boolean isFirst) {
        //textView.setText(String.valueOf(upperLimit));
        if (upperLimit > LAST_DATE) {
            final long lowerLimit = upperLimit - SEARCH_INTERVAL;

            Bundle parameters = new Bundle();
            parameters.putString("limit", "100");
            parameters.putString("since", String.valueOf(lowerLimit));
            parameters.putString("until", String.valueOf(upperLimit));
            parameters.putString("fields", "from");

            new Request(Session.getActiveSession(), (groupID) + "/feed", parameters, HttpMethod.GET, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    processFeed(isFirst, response);
                    getNextResponse(response, upperLimit, lowerLimit);
                }
            }).executeAsync();
        } else {
            publishResults();
        }
    }

    private void processFeed(boolean isFirst, Response response) {
        GraphObject allDataGraphObject = response.getGraphObject();
        if (allDataGraphObject != null) {
            try {
                JSONArray listOfPosts = allDataGraphObject.getInnerJSONObject().getJSONArray("data");
                for (int numPost = isFirst ? 0 : 1; numPost < listOfPosts.length(); numPost++) {
                    JSONObject currentPost = listOfPosts.getJSONObject(numPost);
                    addToMap(currentPost);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void getNextResponse(Response response, long oldUpperLimit, long oldLowerLimit) {
        GraphObject allDataGraphObject = response.getGraphObject();
        if (allDataGraphObject != null) {
            try {
                JSONArray listOfPosts = allDataGraphObject.getInnerJSONObject().getJSONArray("data");
                int length = listOfPosts.length();
                if (length == 0)
                    getFeed(oldLowerLimit, false);
                else {
                    String lastPostTime = listOfPosts.getJSONObject(listOfPosts.length() - 1).get("updated_time").toString();
                    long lastPostUnixTime = getTime(lastPostTime);
                    textView.setText(lastPostTime + "\n");
                    if (length == 1 && lastPostUnixTime == oldUpperLimit)
                        getFeed(oldLowerLimit, false);
                    else
                        getFeed(lastPostUnixTime, false);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void addToMap(JSONObject currentPost) {
        try {
            String currentPerson = currentPost.getJSONObject("from").get("name").toString();
            Integer numTimes = fieldsMap.get(currentPerson);
            if (numTimes == null)
                fieldsMap.put(currentPerson, 1);
            else
                fieldsMap.put(currentPerson, numTimes + 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void publishResults() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(fieldsMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> lhs, Map.Entry<String, Integer> rhs) {
                return rhs.getValue() - lhs.getValue();
            }
        });

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : list) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        textView.setText(sb.toString());
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
        getMenuInflater().inflate(R.menu.menu_comments_output, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}