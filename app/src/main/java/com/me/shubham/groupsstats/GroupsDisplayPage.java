package com.me.shubham.groupsstats;

import android.app.ProgressDialog;
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
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupsDisplayPage extends ActionBarActivity {

    public View.OnClickListener groupsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TableRow tableRow = (TableRow) v.getParent();

            Intent intent = new Intent(GroupsDisplayPage.this, resultsDisplayPage.class);
            intent.putExtra("groupID", tableRow.getTag().toString());
            startActivity(intent);
        }
    };
    ProgressDialog loadingDialog;
    private TableLayout groupsTableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_page);

        groupsTableLayout = (TableLayout) findViewById(R.id.tableLayout);

        loadingDialog = new ProgressDialog(GroupsDisplayPage.this);
        loadingDialog.setMessage("Getting Groups...");
        loadingDialog.show();

        Request.newMeRequest(Session.getActiveSession(), new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser graphUser, Response response) {
                getGroups();
            }
        }).executeAsync();
    }

    private void getGroups() {

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name");

        new Request(Session.getActiveSession(), "/me/groups", parameters, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                if (response != null) {
                    GraphObject innerGraphObject = response.getGraphObject();
                    if (innerGraphObject != null) {
                        JSONObject innerObject = innerGraphObject.getInnerJSONObject();
                        try {
                            JSONArray listOfGroups = innerObject.getJSONArray("data");
                            for (int numGroup = 0; numGroup < listOfGroups.length(); numGroup++) {
                                JSONObject group = listOfGroups.getJSONObject(numGroup);
                                createButton(group.get("name").toString(), group.get("id").toString());
                            }
                        } catch (JSONException ignored) {
                        }
                    }
                }
                loadingDialog.dismiss();
            }
        }).executeAsync();
    }

    private void createButton(String groupName, String groupID) {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View newGroupRow = layoutInflater.inflate(R.layout.group_table_row, null);

        Button groupNameButton = (Button) newGroupRow.findViewById(R.id.button);
        groupNameButton.setText(groupName);
        newGroupRow.setTag(groupID);
        groupsTableLayout.addView(newGroupRow);

        groupNameButton.setOnClickListener(groupsClickListener);
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
