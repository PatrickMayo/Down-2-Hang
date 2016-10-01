package io.evolution.downtohang;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.*;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private final String NO_HANGOUT = "0";
    private final String DECIDE_VIEW = "decide";
    private final String REFRESH = "refresh";
    private final String STATUS = "status";
    private final String NOTHING = "none";

    public final int REFRESH_LOC = 1;


    private User you;
    private ArrayList<User> users;
    private ImageButton changeStatusImageButton;
    private ListView usersListView;
    private Button mainHangoutButton;
    private LocalDB db;
    private OkHttpClient client;
    private SharedPreferences savedValues;

    // location services
    private AppLocationListener locationListener;
    private LocationManager locationManager;

    private String resp;

    /**
     * Create the main activity
     * @param savedInstanceState the applications current saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // go to create account if you haven't created one.
        savedValues = getSharedPreferences("Saved Values", MODE_PRIVATE);
        if (savedValues.getString("yourName", null) == null) {
            // end main, need to create an account first.
            goToActivity(CreateAccountActivity.class);
            finish();
            return;
        }

        setContentView(R.layout.main_layout);
        client = new OkHttpClient();
        changeStatusImageButton = (ImageButton) findViewById(R.id.changeStatusImageButton);
        changeStatusImageButton.setOnClickListener(this);

        usersListView = (ListView) findViewById(R.id.usersListView);
        mainHangoutButton = (Button) findViewById(R.id.mainHangoutButton);

        mainHangoutButton.setOnClickListener(this);
        db = new LocalDB(this);
        users = db.getAllUsers();
        generateYou();
        handleLocationPermission();
    }

    /**
     * Destroy the activity
     * User goes offline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(you != null) {
            updateYouServer(-1, NO_HANGOUT, you.getLatitude(), you.getLongitude(), NOTHING);
        }
    }

    /**
     * Generate a user for this activities you object.
     */
    public void generateYou() {
        String uuid = savedValues.getString("yourUUID", null);
        String username = savedValues.getString("yourName", null);
        int status = savedValues.getInt("yourStatus", -1);
        String hangoutStatus = savedValues.getString("yourHangoutStatus", null);
        String latitude = savedValues.getString("yourLat", null);
        String longitude = savedValues.getString("yourLong", null);
        you = new User(uuid, username, status, hangoutStatus, Double.parseDouble(latitude), Double.parseDouble(longitude));
    }

    /**
     * Create the options menu
     * @param menu a Menu object
     * @return true if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Given a menu item, determine it's id and take appropriate action.
     * @param item a menu item
     * @return true if successful
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                // setup location, then refresh
                handleLocationPermission();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Determine if the user has location services enabled or disabled. If
     * disabled, request to enable them.
     */
    public void handleLocationPermission() {
        // Create location manager and listener
        if(locationManager == null) {
            locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        }
        if(locationListener == null) {
            locationListener = new AppLocationListener();
            if(savedValues.getString("yourLat",null) != null) {
                Location currLoc = new Location("");
                currLoc.setLatitude(you.getLatitude());
                currLoc.setLongitude(you.getLongitude());
                locationListener.setLocation(currLoc);
            }
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {
            // request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REFRESH_LOC);
        }
        else {
            // already have permission
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1,
                    locationListener);
            refresh();
        }
    }

    /**
     * Perform some action after the user has denied or accepted
     * a request to use a permission.
     * @param requestCode the code of the request
     * @param permissions the list of permissions being to be granted
     * @param grantResults array corresponding to permissions determining whether
     *                     the permission was granted or denied.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode) {
            case REFRESH_LOC:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1,
                                locationListener);
                    }
                }
                refresh();
                break;
            }
        }
    }

    /**
     * Refresh the list of recent users
     */
    public void refresh() {
        setButtonsEnabled(false);
        new GetYourUpdatedData().execute(you.getUUID(),REFRESH);
    }

    /**
     * Populate the list view with users in your recent user's database.
     */
    private void populateListView() {
        usersListView.setEmptyView(findViewById(R.id.emptyListView));
        users = db.getAllUsers();
        if(users.size() > 0) {
            ArrayAdapter<User> adapter = new MainListAdapter();
            usersListView.setAdapter(adapter);
        }
    }

    /**
     * Handle click events for a given view
     * @param v a view
     */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.changeStatusImageButton:
                changeStatus();
                break;
            case R.id.mainHangoutButton:
                decideHangoutView();
                break;
            default:
                break;
        }
    }

    /**
     * Decide which hangout activity to go to. If the hangout status is 0, go
     * to the create hangout screen, otherwise go to the hangout screen.
     */
    public void decideHangoutView() {
        setButtonsEnabled(false);
        new GetYourUpdatedData().execute(you.getUUID(),DECIDE_VIEW);
    }

    /**
     * Go to a specified activity
     * @param c the class of the activity
     */
    public void goToActivity(Class c) {
        Intent intent = new Intent(getApplicationContext(), c);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
    }

    /**
     * Change your status when clicking the change status button.
     */
    private void changeStatus() {
        int status = you.getStatus();
        int newStatus = (status == 1) ? 0:1;
        updateYouObject(newStatus,you.getHangoutStatus(),you.getLatitude(),you.getLongitude());
        setStatusButtonImage();
        Toast.makeText(this,"User: " + you.getUsername() + " Status: " + newStatus,Toast.LENGTH_SHORT).show();
        changeStatusImageButton.setEnabled(false);
        updateYouPreferences(newStatus,you.getHangoutStatus(),you.getLatitude(),you.getLongitude());
        updateYouServer(newStatus,null,you.getLatitude(),you.getLongitude(), STATUS);
    }

    /**
     * Update the you object.
     * @param newStatus update status
     * @param newHangoutStatus updated hangout status
     * @param latitude updated latitude coordinate
     * @param longitude updated longitude coordinate
     */
    private void updateYouObject(int newStatus, String newHangoutStatus, double latitude,
                                 double longitude) {
        // you object
        you.setStatus(newStatus);
        you.setHangoutStatus(newHangoutStatus);
        you.setLatitude(latitude);
        you.setLongitude(longitude);
    }

    /**
     * Update you in shared preferences
     * @param newStatus update status
     * @param newHangoutStatus updated hangout status
     * @param latitude updated latitude coordinate
     * @param longitude updated longitude coordinate
     */
    private void updateYouPreferences(int newStatus, String newHangoutStatus, double latitude,
                                      double longitude) {
        // shared preferences
        String latString = Double.toString(latitude);
        String longString = Double.toString(longitude);
        SharedPreferences.Editor editor = savedValues.edit();
        editor.putInt("yourStatus", newStatus);
        editor.putString("yourHangoutStatus", newHangoutStatus);
        editor.putString("yourLat", latString);
        editor.putString("yourLong", longString);
        editor.commit();
    }

    /**
     * Update yourself in the server database
     * @param newStatus update status
     * @param newHangoutStatus updated hangout status
     * @param latitude updated latitude coordinate
     * @param longitude updated longitude coordinate
     * @param calledFrom string to identify what should be done next in async calls
     */
    private void updateYouServer(int newStatus, String newHangoutStatus, double latitude,
                                 double longitude, String calledFrom) {
        String latString = Double.toString(latitude);
        String longString = Double.toString(longitude);
        changeStatusImageButton.setEnabled(false);
        // server database
        new UpdateYouDB().execute(you.getUUID(), you.getUsername(), Integer.toString(newStatus),
                newHangoutStatus, latString, longString,calledFrom);
    }

    /**
     * Update you data in three ways:
     *  1. you object
     *  2. SharedPreferences
     *  3. Server
     * @param newStatus update status
     * @param newHangoutStatus updated hangout status
     * @param latitude updated latitude coordinate
     * @param longitude updated longitude coordinate
     */
    private void updateYouAll(int newStatus, String newHangoutStatus, double latitude,
                              double longitude) {
        updateYouObject(newStatus,newHangoutStatus,latitude,longitude);
        updateYouPreferences(newStatus,newHangoutStatus,latitude,longitude);
        updateYouServer(newStatus,newHangoutStatus,latitude,longitude,NOTHING);
    }

    /**
     * Enable or disabled all buttons in the layout
     * @param enabled determines if buttons should be enabled or disabled.
     */
    private void setButtonsEnabled(boolean enabled) {
        changeStatusImageButton.setEnabled(enabled);
        mainHangoutButton.setEnabled(enabled);
    }

    /**
     * Set the image and text of the status button.
     */
    public void setStatusButtonImage() {
        if(you.getHangoutStatus().equals(NO_HANGOUT)) {
            if(you.getStatus() == 1) {
                // green
                changeStatusImageButton.setImageResource(R.mipmap.green_circle_icone_4156_128);
            }
            else {
                // red
                changeStatusImageButton.setImageResource(R.mipmap.red_circle_icone_5751_128);
            }
        }
        else {
            // orange
            changeStatusImageButton.setImageResource(R.mipmap.orange_circle_icone_6032_128);
        }
    }

    /**
     * Set hangout button text
     */
    public void setHangoutButtonText() {
        if(you.getHangoutStatus().equals(NO_HANGOUT)) {
            mainHangoutButton.setText(getString(R.string.main_hangout_button_text_create));
        }
        else {
            mainHangoutButton.setText(getString(R.string.main_hangout_button_text_your));
        }
    }

    // ----- List Adapter -----

    /**
     * List adapter for MainListItemLayout
     */
    private class MainListAdapter extends ArrayAdapter<User> {

        public int lastExpanded;

        /**
         * Create a main list adapter
         */
        public MainListAdapter() {
            super(MainActivity.this, R.layout.main_list_layout, users);
            this.lastExpanded = -1;
        }

        /**
         * Get the main list layout view
         * @param position the view's position in the list
         * @param convertView a view
         * @param parent a view group
         * @return a MainListItemLayout view
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //get the user from list
            User currentUser = users.get(position);
            MainListItemLayout mainListItemLayout;
            if (convertView == null) {
                mainListItemLayout = new MainListItemLayout(getContext(), currentUser, false, you);
            } else {
                mainListItemLayout = (MainListItemLayout) convertView;
            }
            return mainListItemLayout;
        }

        /**
         * @return the number of views in the lsit
         */
        @Override
        public int getViewTypeCount() {
            return getCount();
        }

        /**
         *
         * @param position a position
         * @return the given position.
         */
        @Override
        public int getItemViewType(int position) {
            return position;
        }

    }

    // ----- Asynchronous Task Inner Classes -----

    /**
     * Refreshes the recent users list and updates the local database.
     */
    class RefreshRecentUsersFromDB extends AsyncTask<String, Void, String> {
        Response response;
        ArrayList<User> updatedUsers;

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
            updatedUsers = new ArrayList<User>();
            for(User user: users) {
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
                        updatedUsers.add(u);
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
                db.updateRecentUsers(updatedUsers);
                populateListView();
                System.out.println("Updated Recent Users!");
            }
            else {
                System.out.println("Async Task RefreshRecentUserDB failed!");
            }
            setStatusButtonImage();
            setHangoutButtonText();
            changeStatusImageButton.setEnabled(you.getHangoutStatus().equals(NO_HANGOUT));
            mainHangoutButton.setEnabled(true);
        }
    }

    /**
     * Update your information in the server database
     */
    class UpdateYouDB extends AsyncTask<String, Void, String> {

        String calledFrom;
        /**
         * Task to perform in the background
         *
         * @param params a list of void parameters
         * @return Three possible types of strings:
         * "200" if the request went through.
         * The message of the response if the HTTP code was not 200.
         * "failed" if the request failed.
         */
        @Override
        protected String doInBackground(String... params) {

            // params must be in a particular order.
            String uuid = params[0];
            String username = params[1];
            int status = Integer.parseInt(params[2]);
            String hangoutStatus = params[3];
            double latitude = Double.parseDouble(params[4]);
            double longitude = Double.parseDouble(params[5]);
            calledFrom = params[6];


            String esc_quote = "\"";

            StringBuilder requestBody = new StringBuilder();
            requestBody.append("{").append(esc_quote).append("userName").append(esc_quote).append(":").append(esc_quote).append(username).append(esc_quote).append(",")
                    .append(esc_quote).append("status").append(esc_quote).append(":").append(status).append(",");
                    if(hangoutStatus != null) {
                        requestBody.append(esc_quote).append("hangoutStatus").append(esc_quote).append(":").append(esc_quote).append(hangoutStatus).append(esc_quote).append(",");
                    }
                    requestBody.append(esc_quote).append("latitude").append(esc_quote).append(":").append(latitude).append(",")
                    .append(esc_quote).append("longitude").append(esc_quote).append(":").append(longitude).append("}");

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

                Response response = client.newCall(request).execute();
                if (response.code() == 200) {
                    return "200";
                } else {
                    return response.message();
                }
            } catch (IOException e) {
                System.err.println(e.toString());
                return "failed";
            }
        }

        /**
         * Actions to perform after the asynchronous request
         *
         * @param message the message returned by the request
         */
        @Override
        protected void onPostExecute(String message) {
            if (message.equals("200")) {
                switch(calledFrom) {
                    case REFRESH:
                        new RefreshRecentUsersFromDB().execute();
                        break;
                    case STATUS:
                        refresh();
                        break;
                    default:
                        break;
                }
                System.out.println("You have been updated!");
            }
            else {
                System.out.println("Async Task UpdateYouDB failed");
            }
            // re-enable the button
            changeStatusImageButton.setEnabled(you.getHangoutStatus().equals(NO_HANGOUT));
        }
    }

    /**
     * Get your information from the server database
     */
    class GetYourUpdatedData extends AsyncTask<String, Void, String> {

        // help us determine what to do at the end
        String calledFrom;

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
            // disable the status button
            String uuid = params[0];
            calledFrom = params[1];
            try {
                Request request = new Request.Builder()
                        .url("http://www.3volution.io:4001/api/Users?filter={\"where\":{\"uuid\":\""+uuid+"\"}}")
                        .get()
                        .addHeader("x-ibm-client-id", "default")
                        .addHeader("x-ibm-client-secret", "SECRET")
                        .addHeader("content-type", "application/json")
                        .addHeader("accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();
                if(response.code() == 200) {
                    resp = response.body().string();
                    try {
                        JSONArray userJSONArray = new JSONArray(resp);
                        for (int i = 0; i < 1; i++) {
                            JSONObject o = userJSONArray.getJSONObject(i);
                            updateYouObject(o.getInt("status"),
                                    o.getString("hangoutStatus"),
                                    o.getDouble("latitude"),
                                    o.getDouble("longitude"));
                            updateYouPreferences(o.getInt("status"),
                                    o.getString("hangoutStatus"),
                                    o.getDouble("latitude"),
                                    o.getDouble("longitude"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
                switch(calledFrom) {
                    case DECIDE_VIEW:
                        setStatusButtonImage();
                        setHangoutButtonText();
                        changeStatusImageButton.setEnabled(you.getHangoutStatus()
                                .equals(NO_HANGOUT));
                        mainHangoutButton.setEnabled(true);
                        if(you.getHangoutStatus().equals(NO_HANGOUT)) {
                            goToActivity(CreateHangoutActivity.class);
                        }
                        else {
                            goToActivity(HangoutActivity.class);
                        }
                        break;
                    case REFRESH:
                        System.out.println("Updated you, now setting latlong");
                        Location currentLocation = locationListener.getLocation();
                        double newLatitude = Double.parseDouble(getString(R.string.default_lat));
                        double newLongitude = Double.parseDouble(getString(R.string.default_long));
                        if(currentLocation != null) {
                            newLatitude = locationListener.getLocation().getLatitude();
                            newLongitude = locationListener.getLocation().getLongitude();
                        }
                        updateYouObject(you.getStatus(),you.getHangoutStatus(), newLatitude,
                                newLongitude);
                        updateYouPreferences(you.getStatus(),you.getHangoutStatus(), newLatitude,
                                newLongitude);
                        updateYouObject(you.getStatus(),you.getHangoutStatus(), newLatitude,
                                newLongitude);
                        updateYouServer(you.getStatus(),you.getHangoutStatus(), newLatitude,
                                newLongitude,REFRESH);
                        break;
                    default:
                        break;
                }
            }
            else {
                setStatusButtonImage();
                changeStatusImageButton.setEnabled(you.getHangoutStatus().equals(NO_HANGOUT));
                setHangoutButtonText();
                mainHangoutButton.setEnabled(true);
                System.out.println("Async Task GetYourUpdatedData failed");
            }
        }
    }
}
