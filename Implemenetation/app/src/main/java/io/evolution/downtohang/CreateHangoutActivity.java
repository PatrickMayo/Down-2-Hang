package io.evolution.downtohang;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Activity to create hangouts.
 * User's search through all available online users and add them
 * to their hangout.
 */
public class CreateHangoutActivity extends AppCompatActivity implements View.OnClickListener,
        TextView.OnEditorActionListener{

    private OkHttpClient client;
    private EditText manageContactsSearchUserEditText;
    private ListView manageContactsSearchedUsersListView;
    private Button manageContactsSearchButton;
    private Set<User> usersFound;
    private List<User> usersFoundList;
    private String userToSearch;
    String resp;

    private User you;
    private String uuidLeader;
    private LocalDB db;
    private SharedPreferences savedValues;
    private Button hangoutButton;
    private Set<User> participants = new HashSet();
    User currentUser;


    /**
     * Create the create hangout activity
     * @param savedInstanceState the applications current saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage_contacts);
        resp = "";

        //Get references to widgets
        manageContactsSearchUserEditText = (EditText) findViewById(R.id.manageContactsSearchUserEditText);
        manageContactsSearchUserEditText.setOnEditorActionListener(this);
        manageContactsSearchedUsersListView = (ListView) findViewById(R.id.manageContactsSearchedUsersListView);
        manageContactsSearchButton = (Button) findViewById(R.id.manageContactsSearchButton);
        manageContactsSearchButton.setOnClickListener(this);

        hangoutButton = (Button) findViewById(R.id.hangoutButton);
        hangoutButton.setOnClickListener(this);

        client = new OkHttpClient();
        usersFound = new HashSet();
        usersFoundList = new ArrayList(usersFound);

        savedValues = getSharedPreferences("Saved Values", MODE_PRIVATE);
        generateYou();

        uuidLeader = you.getUUID();
        db = new LocalDB(this);
    }

    /**
     * Create the options menu
     * @param menu a menu object
     * @return true if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.zero_menu, menu);
        return true;
    }

    /**
     * Given a view, take appropriate action when tapped.
     * @param v a view
     */
    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.manageContactsSearchButton:
                // search button
                populateList();
                break;
            case R.id.manageContactsAdapterActionButton:
                // Add button
                participants.add(currentUser);
                String allParts = "Users Invited: ";
                for(User user : participants){
                    allParts += user.getUsername() + ", ";
                }
                Toast.makeText(this, allParts, Toast.LENGTH_SHORT).show();
                break;
            case R.id.hangoutButton:
                // hangout button
                if(participants.size() > 0) {
                    currentUser.setStatus(0);
                    currentUser.setHangoutStatus(currentUser.getUUID());
                    new RefreshAddedUsers().execute();
                }
                break;
        }
    }

    /**
     * Given a edit text view, deal with edit actions.
     * @param v a text view
     * @param actionId an action Id
     * @param event a Key Event
     * @return true or false.
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        int keyCode = -1;
        if (event != null) {
            keyCode = event.getKeyCode();
        }
        if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
            populateList();
        }
        return false;
    }

    /**
     * Populate the list of matches
     */
    private void populateList() {
        userToSearch = manageContactsSearchUserEditText.getText().toString();
        new GetUsersWithName().execute();
    }

    /**
     * Populate the list view with matches
     */
    public void populateListView(){
        ArrayAdapter<User> adapter = new MyListAdapter();
        manageContactsSearchedUsersListView = (ListView) findViewById(R.id.manageContactsSearchedUsersListView);
        manageContactsSearchedUsersListView.setAdapter(adapter);
    }

    /**
     * Generate the object for you, the user
     */
    public void generateYou() {
        String uuid = savedValues.getString("yourUUID",null);
        String username = savedValues.getString("yourName",null);
        int status = savedValues.getInt("yourStatus", -1);
        String hangoutStatus = savedValues.getString("yourHangoutStatus",null);
        String latitude = savedValues.getString("yourLat",null);
        String longitude = savedValues.getString("yourLong",null);
        you = new User(uuid,username,status,hangoutStatus,Double.parseDouble(latitude),
                Double.parseDouble(longitude));
    }

    /**
     * Go to the main activity.
     */
    public void goToMainActivity() {
        savedValues = getSharedPreferences("Saved Values", MODE_PRIVATE);
        SharedPreferences.Editor editor = savedValues.edit();
        editor.putInt("yourStatus", 0);
        editor.putString("yourHangoutStatus", uuidLeader);
        editor.commit();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
        finish();
    }


    /**
     * An array adapter for CreateHangoutActivity
     */
    private class MyListAdapter extends ArrayAdapter<User> implements View.OnClickListener{

        /**
         * Create the list adapter
         */
        public MyListAdapter() {
            super(CreateHangoutActivity.this, R.layout.manage_contacts_adapter, usersFoundList);
        }

        /**
         * Get the view of this list adapter
         * @param position a position
         * @param convertView a view
         * @param parent a view group
         * @return the view to be shown.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View itemView = convertView;
            //makes sure we have a view to work with , if not we create one
            if(itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.manage_contacts_adapter, parent, false);
            }

            //get the user from list
            currentUser = usersFoundList.get(position);

            //username
            TextView usernameView = (TextView) itemView.findViewById(R.id.manageContactsAdapterUserNameLabel);
            usernameView.setText(currentUser.getUsername());

            Button manageContactsAdapterActionButton = (Button) itemView.findViewById(R.id.manageContactsAdapterActionButton);
            manageContactsAdapterActionButton.setOnClickListener(this);
            return itemView;
        }

        /**
         * Given a view in this item, perform an appropriate action
         * @param v a view
         */
        @Override
        public void onClick(View v){
            switch (v.getId()) {
                case R.id.manageContactsAdapterActionButton:
                    participants.add(currentUser);
                    String allParts = "Users Invited: ";
                    for(User user : participants){
                        allParts += user.getUsername() + ", ";
                    }
                    Toast.makeText(getContext(), allParts, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }


    // ----- Asynchronous Task Classes -----

    /**
     * Get all users with a given name
     */
    class GetUsersWithName extends AsyncTask<Void, Void, String> {

        /**
         * Task to perform in the background
         * @param params a list of void parameters
         * @return Three possible types of strings:
         *          "200" if the request went through.
         *          The message of the response if the HTTP code was not 200.
         *          "failed" if the request failed.
         */
        Response response;

        @Override
        protected String doInBackground(Void... params ) {
            // params must be in a particular order.
            usersFound.clear();
            try {
                Request request = new Request.Builder()
                        .url("http://www.3volution.io:4001/api/Users?filter={\"where\":{\"userName\":\""+userToSearch+"\"}}")
                        .get()
                        .addHeader("x-ibm-client-id", "default")
                        .addHeader("x-ibm-client-secret", "SECRET")
                        .addHeader("content-type", "application/json")
                        .addHeader("accept", "application/json")
                        .build();

                this.response = client.newCall(request).execute();
                if(response.code() == 200) {
                    resp = response.body().string();
                    return "200";
                }
                else {
                    return response.message();
                }
            }
            catch (IOException e) {
                System.err.println(e.toString());
                return "failed";
            }
        }

        /**
         * Actions to perform after the asynchronous request
         * @param message the message returned by the request
         */
        @Override
        protected void onPostExecute(String message) {
            if(message.equals("200")) {
                // success, do what you need to.
                try {
                    JSONArray userJSONArray = new JSONArray(resp);
                    for (int i = 0; i < userJSONArray.length(); i++) {
                        JSONObject o = userJSONArray.getJSONObject(i);
                        User u = new User(o.getString("uuid"),
                                o.getString("userName"),
                                o.getInt("status"),
                                o.getString("hangoutStatus"),
                                o.getDouble("latitude"),
                                o.getDouble("longitude"));
                        if(!u.equals(you) && u.getStatus() == 1) {
                            usersFound.add(u);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                usersFoundList = new ArrayList(usersFound);
                populateListView();
                System.out.println("Search Complete!");
            }
            else {
                System.out.println("Async Task CreateHangout Failed");
            }
        }
    }

    /**
     * Update the server database and local database when a hangout begins
     */
    class CreateHangout extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {
            try {
                String responseCode = "";
                you.setHangoutStatus(uuidLeader);
                Set<User> allParticipants = new HashSet(participants);
                allParticipants.add(you);
                for(User user: allParticipants) {
                    MediaType mediaType = MediaType.parse("application/json");
                    RequestBody body = RequestBody.create(mediaType, "{" +
                            "\"hangoutStatus\":" + "\"" + uuidLeader + "\"" +
                            ",\"status\":0}");
                    Request request = new Request.Builder()
                            .url("http://www.3volution.io:4001/api/Users/update?where={\"uuid\": \"" + user.getUUID() + "\"}")
                            .post(body)
                            .addHeader("x-ibm-client-id", "default")
                            .addHeader("x-ibm-client-secret", "SECRET")
                            .addHeader("content-type", "application/json")
                            .addHeader("accept", "application/json")
                            .build();

                    Response response = client.newCall(request).execute();

                    if (response.code() == 200) {
                        responseCode = ""+ response.code()+"";
                    } else {
                        responseCode = response.message();
                    }
                }
                if (responseCode.equals("200")) {
                    return "200";
                } else {
                    return responseCode;
                }
            }catch (IOException e) {
                System.err.println(e);
                return "failed";
            }

        }

        /**
         * Actions to perform after the asynchronous request
         * @param message the message returned by the request
         */
        @Override
        protected void onPostExecute(String message) {
            if(message.equals("200")) {
                db.addRecentUsers(new ArrayList(participants));
                goToMainActivity();
                System.out.println("Hangout Created with " + participants.toString());
            }
            else {
                System.out.println("Async Task CreateHangout Failed");
            }
        }
    }

    /**
     * Refreshes the recent users list and updates the local database.
     * Used in this class to prevent the following:
     *      User A wants to invite User B to hangout.
     *      As A invites B, B is invited to or creates another hangout.
     *      A should not be able to invite B, even if B is already added in the list.
     */
    class RefreshAddedUsers extends AsyncTask<String, Void, String> {
        Response response;
        Set<User> updatedParticipants;

        /**
         * Task to perform in the background
         * @param params a list of void parameters
         * @return Three possible types of strings:
         *          "200" if the request went through.
         *          The message of the response if the HTTP code was not 200.
         *          "failed" if the request failed.
         */
        @Override
        protected String doInBackground(String... params ) {
            updatedParticipants = new HashSet();
            for(User user: participants) {
                String uuid = user.getUUID();
                try {
                    Request request = new Request.Builder()
                            .url("http://www.3volution.io:4001/api/Users?filter={\"where\":{\"uuid\":\""+uuid+"\"}}")
                            .get()
                            .addHeader("x-ibm-client-id", "default")
                            .addHeader("x-ibm-client-secret", "SECRET")
                            .addHeader("content-type", "application/json")
                            .addHeader("accept", "application/json")
                            .build();

                    this.response = client.newCall(request).execute();
                    if(response.code() == 200) {
                        resp = response.body().string();
                    }
                    else {
                        return response.message();
                    }
                }
                catch (IOException e) {
                    System.err.println(e.toString());
                    return "failed";
                }
                try {
                    JSONArray userJSONArray = new JSONArray(resp);
                    for (int i = 0; i < userJSONArray.length(); i++) {
                        JSONObject o = userJSONArray.getJSONObject(i);
                        User u = new User(o.getString("uuid"),
                                o.getString("userName"),
                                o.getInt("status"),
                                o.getString("hangoutStatus"),
                                o.getDouble("latitude"),
                                o.getDouble("longitude"));
                        if(u.getHangoutStatus().equals("0")) {
                            updatedParticipants.add(u);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return "200";
        }

        /**
         * Actions to perform after the asynchronous request
         * @param message the message returned by the request
         */
        @Override
        protected void onPostExecute(String message) {
            if(message.equals("200")) {
                participants = updatedParticipants;
                if(!participants.isEmpty()) {
                    new CreateHangout().execute();
                    System.out.println("Updated Users!");
                }
            }
            else {
                System.out.println("Async Task RefreshAddedUsers failed!");
            }
        }
    }
}
