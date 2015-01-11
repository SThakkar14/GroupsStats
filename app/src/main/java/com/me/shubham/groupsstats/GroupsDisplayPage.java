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
import com.facebook.model.GraphUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupsDisplayPage extends ActionBarActivity {
    ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_display_page);

        //So the title isn't "GroupsDisplayPage"
        getSupportActionBar().setTitle("Groups");

        //Adds the first name of the user to the title
        Request.newMeRequest(Session.getActiveSession(), new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser graphUser, Response response) {
                if (graphUser != null)
                    getSupportActionBar().setTitle(graphUser.getFirstName() + "'s Groups");
            }
        }).executeAsync();

        //Temporary Loading Screen
        //Just a spinner because the number of groups is unknown
        loadingDialog = new ProgressDialog(GroupsDisplayPage.this);
        loadingDialog.setMessage("Getting Groups...");
        loadingDialog.show();

        getGroups();
    }

    //Gets the first page of groups
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
        Request nextPage = response.getRequestForPagedResults(Response.PagingDirection.NEXT);
        if (nextPage != null) {
            nextPage.setCallback(new Request.Callback() {
                @Override
                public void onCompleted(Response newResponse) {
                    if (newResponse != null) {
                        processResponse(newResponse, false);
                        getNextPage(newResponse);
                    }
                }
            });
            nextPage.executeAsync();
        } else
            //Done loading groups
            loadingDialog.dismiss();
    }

    //Creates buttons for each one and displays them
    private void processResponse(Response response, boolean isFirstPage) {
        if (response != null) {
            GraphObject innerGraphObject = response.getGraphObject();
            if (innerGraphObject != null) {
                JSONObject innerObject = innerGraphObject.getInnerJSONObject();
                try {
                    JSONArray listOfGroups = innerObject.getJSONArray("data");
                    if (listOfGroups.length() == 0 && isFirstPage)
                        //Just so there isn't an empty page facing the user
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

    //Alerts the user they aren't part of any groups
    private void noGroups() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Whoops...")
                .setMessage("You don't have any groups")
                .setCancelable(false)
                .setNegativeButton("FIRE ZE MISSILES!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    //Creates and adds the button to the layout
    private void createButton(String groupName, String groupID) {
        final Button groupNameButton = new Button(this);
        groupNameButton.setText(groupName);
        groupNameButton.setTag(groupID);

        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.addView(groupNameButton, layoutParams);

        groupNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog(groupNameButton);
            }
        });
    }

    //Further selection menu to pick what exactly to show
    private void createDialog(final Button button) {
        String[] options = {"Most posts", "Most liked posts"};
        final String groupID = (String) button.getTag();

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Pick an option")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(GroupsDisplayPage.this, CommentsOutput.class);
                intent.putExtra("groupID", groupID);
                intent.putExtra("inputChoice", which);
                startActivity(intent);
            }
        });
        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_groups_display_page, menu);
        return true;
    }

    @Override
    //What to do if someone clicks the logout button
    public boolean onOptionsItemSelected(MenuItem item) {
        Session.getActiveSession().closeAndClearTokenInformation();

        Intent intent = new Intent(GroupsDisplayPage.this, LoginPage.class);
        startActivity(intent);

        return true;
    }

}
