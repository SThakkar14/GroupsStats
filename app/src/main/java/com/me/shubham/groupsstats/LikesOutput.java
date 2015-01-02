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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LikesOutput extends ActionBarActivity {
    String groupID;
    String field;
    HashMap<String, Integer> fieldsMap;
    ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_likes_output);

        Intent intent = getIntent();
        groupID = intent.getStringExtra("groupID");
        field = intent.getStringExtra("fields");
        fieldsMap = new HashMap<>();

        loadingDialog = new ProgressDialog(LikesOutput.this);
        loadingDialog.setMessage("Getting Posts...");
        loadingDialog.show();

        getFeed();
    }

    private void getFeed() {
        Bundle parameters = new Bundle();
        parameters.putString("limit", "100");
        parameters.putString("since", "1");
        parameters.putString("fields", field);

        new Request(Session.getActiveSession(), (groupID + "/feed"), parameters, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                if (response != null) {
                    processResponse(response);
                    getNextResponse(response);
                }
            }
        }).executeAsync();
    }

    private void getNextResponse(Response response) {
        Request next = response.getRequestForPagedResults(Response.PagingDirection.NEXT);
        if (next != null) {
            next.setCallback(new Request.Callback() {
                @Override
                public void onCompleted(Response newResponse) {
                    if (newResponse != null) {
                        processResponse(newResponse);
                        getNextResponse(newResponse);
                    }
                }
            });
            next.executeAsync();
        } else {
            sortHashMap();
        }
    }

    private void sortHashMap() {
        loadingDialog.setMessage("Sorting List...");
        List<Map.Entry<String, Integer>> list = new ArrayList<>(fieldsMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> lhs, Map.Entry<String, Integer> rhs) {
                return rhs.getValue() - lhs.getValue();
            }
        });

        loadingDialog.setMessage("Displaying List...");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : list) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        TextView textView = (TextView) findViewById(R.id.TestingLikes);
        textView.setText(sb.toString());
        loadingDialog.dismiss();
    }

    private void processResponse(Response response) {
        GraphObject allDataGraphObject = response.getGraphObject();
        if (allDataGraphObject != null) {
            try {
                JSONObject allData = allDataGraphObject.getInnerJSONObject();
                JSONArray listOfPosts = allData.getJSONArray("data");
                for (int postNum = 0; postNum < listOfPosts.length(); postNum++) {
                    JSONObject currentPost = listOfPosts.getJSONObject(postNum);
                    getField(currentPost);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void getField(JSONObject currentPost) {
        try {
            JSONObject currentField = currentPost.getJSONObject(field);
            if (currentField != null) {
                JSONArray currentArray = currentField.getJSONArray("data");

                for (int numPerson = 0; numPerson < currentArray.length(); numPerson++) {
                    JSONObject person = currentArray.getJSONObject(numPerson);
                    String personName = person.get("name").toString();

                    if (fieldsMap.get(personName) == null)
                        fieldsMap.put(personName, 1);
                    else
                        fieldsMap.put(personName, fieldsMap.get(personName) + 1);
                }
            }
        } catch (JSONException ignored) {
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_final_result_output, menu);
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
