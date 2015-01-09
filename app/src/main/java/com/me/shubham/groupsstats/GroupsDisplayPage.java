package com.me.shubham.groupsstats;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupsDisplayPage extends ActionBarActivity {
    ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_display_page);

        loadingDialog = new ProgressDialog(GroupsDisplayPage.this);
        loadingDialog.setMessage("Getting Groups...");
        loadingDialog.show();

        getGroups();
    }

    private void getGroups() {
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id, name");

        new Request(Session.getActiveSession(), "/me/groups", parameters, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                processResponse(response, true);
                getNextPage(response);
            }
        }).executeAsync();
    }

    private void getNextPage(Response response) {
        Request next = response.getRequestForPagedResults(Response.PagingDirection.NEXT);
        if (next != null) {
            next.setCallback(new Request.Callback() {
                @Override
                public void onCompleted(Response newResponse) {
                    if (newResponse != null) {
                        processResponse(newResponse, false);
                        getNextPage(newResponse);
                    }
                }
            });
            next.executeAsync();
        } else
            loadingDialog.dismiss();
    }

    private void processResponse(Response response, boolean isFirstPage) {
        if (response != null) {
            GraphObject innerGraphObject = response.getGraphObject();
            if (innerGraphObject != null) {
                JSONObject innerObject = innerGraphObject.getInnerJSONObject();
                try {
                    JSONArray listOfGroups = innerObject.getJSONArray("data");
                    if (listOfGroups.length() == 0 && isFirstPage)
                        noGroups();
                    else {
                        for (int numGroup = 0; numGroup < listOfGroups.length(); numGroup++) {
                            JSONObject group = listOfGroups.getJSONObject(numGroup);
                            createButton(group.get("name").toString(), group.get("id").toString());
                        }
                    }
                } catch (JSONException ignored) {
                }
            }
        }
    }

    private void noGroups() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Whoops...");
        builder.setMessage("You don't have any groups");
        builder.setCancelable(false);
        builder.setNegativeButton("FIRE ZE MISSILES!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void createButton(String groupName, String groupID) {
        Button groupNameButton = new Button(this);
        groupNameButton.setText(groupName);
        groupNameButton.setTag(groupID);

        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.addView(groupNameButton, layoutParams);

        groupNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupID = (String) v.getTag();
                Intent intent = new Intent(GroupsDisplayPage.this, resultsDisplayPage.class);
                intent.putExtra("groupID", groupID);
                startActivity(intent);
            }
        });
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
