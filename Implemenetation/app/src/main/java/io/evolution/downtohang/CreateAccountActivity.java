package io.evolution.downtohang;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.*;

/**
 * The create account activity. Users create an account and are added to
 * the database if they have a valid username. Valid user names can be up
 * to 30 characters and consist of characters and numbers separated by spaces.
 */
public class CreateAccountActivity extends AppCompatActivity
    implements View.OnClickListener {

    private final int REQUEST_LOCATION_PERMISSION_UPDATE_LOCATION = 1;

    private EditText editUsername;
    private TextView errorLabel;

    private Button createAccountButton;

    private AppLocationListener locationListener;
    private LocationManager locationManager;
    private OkHttpClient client;

    private String uuid;
    private String username;
    private int status;
    private String hangoutStatus;
    private String latitude;
    private String longitude;

    private SharedPreferences savedValues;

    /**
     * Create the Activity
     * @param savedInstanceState the applications current saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_account);

        // OkHttpClient for database requests
        client = new OkHttpClient();

        // get references to each widget
        editUsername = (EditText) findViewById(R.id.createAccountEditUsername);
        errorLabel = (TextView) findViewById(R.id.createAccountErrorLabel);
        createAccountButton = (Button) findViewById(R.id.createAccountCreateAccountButton);

        // set click listeners
        createAccountButton.setOnClickListener(this);
    }


    /**
     * Resume the activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        handleLocationPermission();
    }

    /**
     * Pause the Activity
     */
    @Override
    protected void onPause() {
        super.onPause();
        try {
            locationManager.removeUpdates(locationListener);
        }
        catch(SecurityException se) {
            System.err.println(se);
        }

    }

    /**
     * Handle click events for a view.
     * @param v a view
     */
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.createAccountCreateAccountButton:
               createAccount();
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
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {
            // request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_UPDATE_LOCATION);
        }
        else {
            // already have permission
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1,
                    locationListener);
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
            case REQUEST_LOCATION_PERMISSION_UPDATE_LOCATION:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 1,
                                locationListener);
                    }
                }
                break;
            }
        }
    }

    /**
     * Go to the a specified activity
     * @param c the activity class
     */
    private void goToActivity(Class c) {
        savedValues = getSharedPreferences("Saved Values",MODE_PRIVATE);
        SharedPreferences.Editor editor = savedValues.edit();
        editor.putString("yourUUID",uuid);
        editor.putString("yourName",username);
        editor.putInt("yourStatus",status);
        editor.putString("yourHangoutStatus",hangoutStatus);
        editor.putString("yourLat",latitude);
        editor.putString("yourLong",longitude);
        editor.commit();
        Intent intent = new Intent(getApplicationContext(), c);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
        finish();
    }

    /**
     * Set the text of the error label.
     * @param message the error message.
     */
    private void setErrorMessage(String message) {
        errorLabel.setText(message != null ? message:"");
    }

    /**
     * Creates an account by adding it to the server database.
     */
    private void createAccount() {
        username = editUsername.getText().toString();
        if(validateUsername()) {
            if (!isNetworkAvailable()) {
                setErrorMessage(getString(R.string.create_account_no_connection_error));
            }

            // generate uuid and set default values
            uuid = UUID.randomUUID().toString();
            status = 0;
            hangoutStatus = "0";
            Location currentLocation = locationListener.getLocation();
            if(currentLocation != null) {
                latitude = Double.toString(currentLocation.getLatitude());
                longitude =  Double.toString(currentLocation.getLongitude());
            }
            else {
                latitude = getString(R.string.default_lat);
                longitude = getString(R.string.default_long);
            }
            new AddNewUserToServerDB().execute();
        }
    }

    /**
     * Check if a username is valid. A username is valid if it contains...
     * @return true if the username is valid.
     */
    public boolean validateUsername() {
        if(username.length() <= 0) {
            setErrorMessage(getString(R.string.create_account_no_username_error));
            return false;
        }
        String regExp = "^[A-Za-z][ A-Za-z0-9_]*$";
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(username);
        if(matcher.find()) {
            return true;
        }
        else {
            setErrorMessage(getString(R.string.create_account_invalid_username_error));
            return false;
        }
    }

    /**
     * @return true if this device has some form of internet connectivity.
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // ----- Asynchronous Task Classes -----
    class AddNewUserToServerDB extends AsyncTask<Void, Void, String> {
        /**
         * Task to perform in the background
         * @param params a list of void parameters
         * @return Three types of strings:
         *          1. "200" response code is 200 (successful)
         *          2. The message of the response if the HTTP response code was not 200.
         *          3. "failed" if the request failed.
         */
        @Override
        protected String doInBackground(Void... params ) {
            try {
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody body = RequestBody.create(mediaType, "{" +
                        "\"uuid\":\"" + uuid + "\"," +
                        "\"userName\":\"" + username + "\"," +
                        "\"status\":\"" + status + "\"," +
                        "\"hangoutStatus\":\"" + hangoutStatus + "\"," +
                        "\"latitude\":\"" + latitude + "\"," +
                        "\"longitude\":\"" + longitude + "\"" +
                        "}");
                Request request = new Request.Builder()
                        .url("http://www.3volution.io:4001/api/Users")
                        .post(body)
                        .addHeader("x-ibm-client-id", "default")
                        .addHeader("x-ibm-client-secret", "SECRET")
                        .addHeader("content-type", "application/json")
                        .addHeader("accept", "application/json")
                        .build();
                Response response = client.newCall(request).execute();
                if(response.code() == 200) {
                    return "200";
                }
                else {
                    return response.message();
                }
            }
            catch (IOException e) {
                return "failed";
            }
        }

        /**
         * Actions to perform after the asynchronous request
         * @param message the message returned by the request
         */
        @Override
        protected void onPostExecute(String message) {
            switch(message) {
                case "200":
                    // success, do what you need to.
                    goToActivity(MainActivity.class);
                    break;
                case "failed":
                    setErrorMessage("Error Occurred.");
                    break;
                default:
                    setErrorMessage(message);
                    break;
            }
        }
    }
}