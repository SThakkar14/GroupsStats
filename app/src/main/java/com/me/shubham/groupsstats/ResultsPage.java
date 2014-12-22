package com.me.shubham.groupsstats;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ResultsPage extends Activity {
    private TableLayout groupsScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_page);

        groupsScrollView=(TableLayout) findViewById(R.id.tableLayout);

        Request.newMeRequest(Session.getActiveSession(), new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser graphUser, Response response) {
                getGroups();
            }
        }).executeAsync();
    }

    private void getGroups() {
        new Request(Session.getActiveSession(), "/me/groups", null, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                JSONObject jsonObject = response.getGraphObject().getInnerJSONObject();
                try {
                    JSONArray array = jsonObject.getJSONArray("data");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject thing = array.getJSONObject(i);
                        createButton(thing.get("name").toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).executeAsync();
    }

    private void createButton(String groupName){
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View newGroupRow = layoutInflater.inflate(R.layout.group_table_row, null);
        Button groupNameButton = (Button) newGroupRow.findViewById(R.id.button);
        groupNameButton.setText(groupName);
        groupNameButton.setOnClickListener(groupsClickListener);
        groupsScrollView.addView(newGroupRow);
    }

    public View.OnClickListener groupsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_results_page, menu);
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