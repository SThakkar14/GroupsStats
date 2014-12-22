package com.me.shubham.groupsstats;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ResultsPage extends ActionBarActivity {
    public final static String GROUP_NAME = "com.me.shubham.groupStats.GROUP_NAME";
    public View.OnClickListener groupsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TableRow tableRow = (TableRow) v.getParent();
            Button button = (Button) tableRow.findViewById(R.id.button);
            String groupName = button.getText().toString();

            Intent intent = new Intent(ResultsPage.this, resultsDisplayPage.class);
            intent.putExtra(GROUP_NAME, groupName);
            startActivity(intent);
        }
    };
    private TableLayout groupsScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_page);

        groupsScrollView = (TableLayout) findViewById(R.id.tableLayout);

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

    private void createButton(String groupName) {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View newGroupRow = layoutInflater.inflate(R.layout.group_table_row, null);
        Button groupNameButton = (Button) newGroupRow.findViewById(R.id.button);
        groupNameButton.setText(groupName);
        groupNameButton.setOnClickListener(groupsClickListener);
        groupsScrollView.addView(newGroupRow);
    }

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
