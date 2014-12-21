package com.me.shubham.groupsstats;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.AppEventsLogger;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;

import java.util.Arrays;

public class MainActivity extends Activity {

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState sessionState, Exception e) {
            if (session != null && sessionState.isOpened()) {
                Intent intent = new Intent(MainActivity.this, ResultsPage.class);
                startActivity(intent);
            }
        }
    };
    private UiLifecycleHelper uiLifecycleHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uiLifecycleHelper = new UiLifecycleHelper(this, callback);
        uiLifecycleHelper.onCreate(savedInstanceState);

        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("user_groups", "user_location", "user_birthday", "user_likes"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiLifecycleHelper.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiLifecycleHelper.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiLifecycleHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiLifecycleHelper.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiLifecycleHelper.onSaveInstanceState(outState);
    }

}
