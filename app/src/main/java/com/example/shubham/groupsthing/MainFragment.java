package com.example.shubham.groupsthing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    private TextView userInfoTextView;
    private UiLifecycleHelper uiHelper;

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            try {
                onSessionStateChange(session, state);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_results_page, container, false);

        LoginButton authButton = (LoginButton) view.findViewById(R.id.authButton);
        authButton.setFragment(this);
        authButton.setReadPermissions(Arrays.asList("user_groups", "user_location", "user_birthday", "user_likes"));

        userInfoTextView = (TextView) view.findViewById(R.id.userInfoTextView);
        return view;
    }

    private void onSessionStateChange(Session session, SessionState state) throws JSONException {
        if (state.isOpened()) {
            Log.i(TAG, "Logged in...");
            userInfoTextView.setVisibility(View.VISIBLE);
            /* make the API call */
            new Request(session, "/me/groups", null, HttpMethod.GET, new Request.Callback() {
                public void onCompleted(Response response) {
                    /* handle the result */
                    StringBuilder stringBuilder = new StringBuilder();
                    JSONObject jsonObject = response.getGraphObject().getInnerJSONObject();
                    try {
                        JSONArray array = jsonObject.getJSONArray("data");
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject thing = array.getJSONObject(i);
                            stringBuilder.append(thing.get("name")).append(" ");
                        }
                        userInfoTextView.setText(stringBuilder.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).executeAsync();
        } else if (state.isClosed()) {
            Log.i(TAG, "Logged out...");
            userInfoTextView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed())) {
            try {
                onSessionStateChange(session, session.getState());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

}