package io.evolution.downtohang;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Activity for managing already established hangouts. Has
 * a leader and a member version.
 */
public class HangoutActivity extends AppCompatActivity implements View.OnClickListener  {
    private Button leave_button;
    private Button leaderHangoutDisbandButton;

    private ListView hangout_ListView;
    private List<User> users = new ArrayList<User>();
    private LocalDB db;
    private Context context;
    private User you;
    private SharedPreferences savedValues;
    private OkHttpClient client;
    String resp;
    private List<User> usersFound = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hangout);
        context = this;
        db = new LocalDB(context);
        client = new OkHttpClient();

        savedValues = getSharedPreferences("Saved Values",MODE_PRIVATE);
        if(savedValues.getString("yourName",null) == null) {
            // end main, need to create an account first.
            goToActivity(CreateAccountActivity.class);
            finish();
            return;
        }

        leave_button = (Button) findViewById(R.id.leave_Button);
        leaderHangoutDisbandButton = (Button) findViewById(R.id.leaderHangoutDisbandButton);


        generateYou();
        if(you.getHangoutStatus().equals(you.getUUID())) {
            //leader mode
            leave_button.setVisibility(View.GONE);
        }
        else {
            // member
            leaderHangoutDisbandButton.setVisibility(View.GONE);
        }

        leave_button.setOnClickListener(this);
        leaderHangoutDisbandButton.setOnClickListener(this);

        new GetUsersFromDB().execute() ;
    }


    /**
     * Create the options menu.
     * @param menu a menu
     * @return true if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.limited_menu, menu);
        return true;
    }

    /**
     * Perform an action when a given menu item is tapped.
     * @param item a menu item
     * @return true or false
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_refresh:
                new GetUsersFromDB().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Handle a button press (tap or click event) for a given view
     * @param v the view
     */
    public void onClick(View v) {
        SharedPreferences.Editor editor = savedValues.edit();
        switch(v.getId()) {
            case R.id.leave_Button:
                users = new ArrayList();
                users.add(you);
                new LeaveHangout().execute();
                break;
            case R.id.leaderHangoutDisbandButton:
                new LeaveHangout().execute();
                break;
        }
    }

    /**
     * Go to the specified activity
     * @param c the class of the specified activity.
     */
    public void goToActivity(Class c) {
        Intent intent = new Intent(getApplicationContext(), c);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
    }

    /**
     * Set shared preference values and go to the main activity.
     */
    public void goToMainActivity() {
        savedValues = getSharedPreferences("Saved Values", MODE_PRIVATE);
        SharedPreferences.Editor editor = savedValues.edit();
        editor.putInt("yourStatus", 1);
        editor.putString("yourHangoutStatus", "0");
        editor.commit();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
        finish();
    }

    /**
     * Generate the object for you, the user.
     */
    public void generateYou() {
        String uuid = savedValues.getString("yourUUID",null);
        String username = savedValues.getString("yourName",null);
        int status = savedValues.getInt("yourStatus",-1);
        String hangoutStatus = savedValues.getString("yourHangoutStatus",null);
        String latitude = savedValues.getString("yourLat",null);
        String longitude = savedValues.getString("yourLong",null);
        you = new User(uuid,username,status,hangoutStatus,Double.parseDouble(latitude),
                Double.parseDouble(longitude));
    }


    /**
     * Generate the adapter for the list view
     */
    public void populateListView(){
        ArrayAdapter<User> adapter = new MyListAdapter();
        hangout_ListView = (ListView) findViewById(R.id.hangout_ListView);
        hangout_ListView.setAdapter(adapter);

    }

    /**
     * The list adapter for the hangout activity.
     */
    private class MyListAdapter extends ArrayAdapter<User>{

        /**
         * Create the list adapter
         */
        public MyListAdapter() {
            super(HangoutActivity.this, R.layout.hangout_adapter, users);
        }

        /**
         * Get a view in the list adapter
         * @param position the position
         * @param convertView a view
         * @param parent a view group
         * @return the view
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View itemView = convertView;
            //makes sure we have a view to work with , if not we create one
            if(itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.hangout_adapter, parent, false);
            }

            //get the user from list
            User currentUser = users.get(position);
            //username
            TextView usernameView = (TextView) itemView.findViewById(R.id.username_TextView);
            usernameView.setText(currentUser.getUsername());
            return itemView;
        }
    }

    // ----- Asynchronous Task Classes -----

    /**
     * Get users information from the database. The users are only those in the hangout
     */
    class GetUsersFromDB extends AsyncTask<Void, Void, String> {

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
            try {
                users.clear();
                Request request = new Request.Builder()
                        .url("http://www.3volution.io:4001/api/Users?filter={\"where\":{\"hangoutStatus\":\""+you.getHangoutStatus()+"\"}}")
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
                // success, do what you need to.
                try {
                    JSONArray userJSONArray = new JSONArray(resp);
                    for (int i = 0; i < userJSONArray.length(); i++) {
                        JSONObject o = userJSONArray.getJSONObject(i);
                        users.add(new User(o.getString("uuid"),
                                o.getString("userName"),
                                o.getInt("status"),
                                o.getString("hangoutStatus"),
                                o.getDouble("latitude"),
                                o.getDouble("longitude")));
                    }

                    populateListView();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
//
            }
            else if(message.equals("failed")) {

            }
            else {
                // HTTP Error Message

            }
            System.out.println("done");
        }
    }

    /**
     * Leave the hangout, if in the leader view, forces everyone in the hangout to leave.
     * If in the member view, only you leave.
     */
    class LeaveHangout extends AsyncTask<Void, Void, String> {

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
            for(User user: users) {
                String uuid = user.getUUID();

                String esc_quote = "\"";

                StringBuilder requestBody = new StringBuilder();
                requestBody.append("{")
                        .append(esc_quote).append("hangoutStatus").append(esc_quote).append(":")
                        .append(esc_quote).append("0").append(esc_quote).append("}");

                StringBuilder url = new StringBuilder();
                url.append("http://www.3volution.io:4001/api/Users/update?where={")
                        .append(esc_quote).append("uuid").append(esc_quote).append(":")
                        .append(esc_quote).append(uuid).append(esc_quote).append("}");

                try {
                    MediaType mediaType = MediaType.parse("application/json");
                    RequestBody body = RequestBody.create(mediaType, requestBody.toString());

                    Request request = new Request.Builder()
                            .url(url.toString())
                            .post(body)
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
                    System.err.println(e);
                    return "failed";
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
                SharedPreferences.Editor editor = savedValues.edit();
                editor.putString("yourHangoutStatus", "0");
                editor.commit();
                db.updateRecentUsers(users);
                goToMainActivity();
            }
            else if(message.equals("failed")) {

            }
            else {
                // HTTP Error Message
            }
            System.out.println(message);
        }
    }
}
