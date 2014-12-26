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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class resultsDisplayPage extends ActionBarActivity {

    public String groupID;
    HashMap<String, Integer> likesMap;
    HashMap<String, Integer> commentsMap;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_display_page);

        Intent intent = getIntent();
        groupID = intent.getStringExtra(ResultsPage.GROUP_ID);

        textView = (TextView) findViewById(R.id.WhatWhat);

        likesMap = new HashMap<>();
        likesMap = new HashMap<>();

        getFeed();

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : likesMap.entrySet()) {
            sb.append(entry.getKey()).append(" ").append(entry.getValue());
        }
        textView.setText(sb.toString());
    }

    private void getFeed() {
        try {
            Bundle parameters = new Bundle();
            parameters.putString("limit", "100");
            parameters.putString("since", "1");

            new Request(Session.getActiveSession(), (groupID + "/feed"), parameters, HttpMethod.GET, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    if (response != null) {
                        processResponse(response);


                    }
                }
            }).executeAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void processResponse(Response response) {
        GraphObject allDataGraphObject = response.getGraphObject();
        if (allDataGraphObject != null) {
            try {
                JSONObject allData = allDataGraphObject.getInnerJSONObject();
                JSONArray listOfPosts = allData.getJSONArray("data");
                for (int postNum = 0; postNum < listOfPosts.length(); postNum++) {
                    JSONObject currentPost = listOfPosts.getJSONObject(postNum);
                    getData(currentPost);
                }

                Request next = response.getRequestForPagedResults(Response.PagingDirection.NEXT);
                if (next != null) {
                    next.setCallback(new Request.Callback() {
                        @Override
                        public void onCompleted(Response newresponse) {
                            if (newresponse != null)
                                processResponse(newresponse);
                        }
                    });
                    next.executeAsync().get();
                }
            } catch (JSONException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void getData(JSONObject currentPost) {
        getField(currentPost, "likes", likesMap);
        getField(currentPost, "comments", commentsMap);
    }

    private void getField(JSONObject currentPost, String field, HashMap<String, Integer> map) {
        try {
            JSONObject currentField = currentPost.getJSONObject(field);
            if (currentField != null) {
                JSONArray currentArray = currentField.getJSONArray("data");

                for (int numPerson = 0; numPerson < currentArray.length(); numPerson++) {
                    JSONObject person = currentArray.getJSONObject(numPerson);
                    String personName = person.get("name").toString();

                    if (map.get(personName) == null)
                        map.put(personName, 1);
                    else
                        map.put(personName, map.get(personName) + 1);
                }
            }
        } catch (JSONException ignored) {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_results_display_page, menu);
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
