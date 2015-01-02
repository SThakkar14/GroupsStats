package com.me.shubham.groupsstats;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class resultsDisplayPage extends ActionBarActivity {

    Button likesButton;
    Button commentsButton;

    String groupID;

    private View.OnClickListener likesListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(resultsDisplayPage.this, LikesOutput.class);
            intent.putExtra("groupID", groupID);
            intent.putExtra("fields", "likes");
            startActivity(intent);
        }
    };

    private View.OnClickListener commentsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(resultsDisplayPage.this, CommentsOutput.class);
            intent.putExtra("groupID", groupID);
            intent.putExtra("fields", "from");
            startActivity(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_display_page);

        Intent intent = getIntent();
        groupID = intent.getStringExtra("groupID");

        likesButton = (Button) findViewById(R.id.likesButton);
        commentsButton = (Button) findViewById(R.id.PostsButton);

        likesButton.setOnClickListener(likesListener);
        commentsButton.setOnClickListener(commentsListener);
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
