package io.evolution.downtohang;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


/*
 * The local database, holds all recent users. Users are recent
 * when they have been in the same hangout as you.
 *
 * The database consists of the following:
 * UUID USERNAME STATUS HANGOUT_STATUS LATITUDE LONGITUDE
 */
public class LocalDB {
    // database constants
    public static final String DB_NAME = "recents.db";
    public static final int DB_VERSION = 1;

    // list table constants
    public static final String RECENTS_TABLE = "recent_hangout_users";

    public static final String UUID = "uuid";
    public static final int UUID_COL = 0;
    public static final String USERNAME = "username";
    public static final int USERNAME_COL = 1;
    public static final String STATUS = "status";
    public static final int STATUS_COL = 2;
    public static final String HANGOUT_STATUS = "hangout_status";
    public static final int HANGOUT_STATUS_COL = 3;
    public static final String LATITUDE = "latitude";
    public static final int LATITUDE_COL = 4;
    public static final String LONGITUDE = "longitude";
    public static final int LONGITUDE_COL = 5;

    // Considering a 6th field that has a time stamp we can use to order most recent
    // but that's extraneous

    // other values
    private static final String TEXT_TYPE = " TEXT";
    private static final String REAL_TYPE = " REAL";
    private static final String UNIQUE = " UNIQUE";
    private static final String COMMA_SEP = ",";

    // create table
    public static final String CREATE_FRIENDS_TABLE =
            "CREATE TABLE " + RECENTS_TABLE + " (" +
                     UUID + TEXT_TYPE + UNIQUE + COMMA_SEP +
                    USERNAME + TEXT_TYPE + COMMA_SEP +
                    STATUS + TEXT_TYPE + COMMA_SEP +
                    HANGOUT_STATUS + TEXT_TYPE + COMMA_SEP +
                    LATITUDE + REAL_TYPE + COMMA_SEP +
                    LONGITUDE + REAL_TYPE +
                    ");";

    // drop table
    public static final String DROP_FRIENDS_TABLE = "DROP TABLE IF EXISTS " + RECENTS_TABLE;

    // database object and database helper object
    private SQLiteDatabase db;
    private DBHelper dbHelper;

    /**
     * Create the local database
     * @param context the context
     */
    public LocalDB(Context context) {
        dbHelper = new DBHelper(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Open the database, read only
     */
    private void openReadableDB() {
        db = dbHelper.getReadableDatabase();
    }

    /**
     * Open the database, read and write
     */
    private void openWriteableDB() {
        db = dbHelper.getWritableDatabase();
    }

    /**
     * Close the database
     */
    private void closeDB() {
        if (db != null) {
            db.close();
        }
    }

    /**
     * @return a list of al the users in the local database
     * (If I get around to it, ordered by date)
     */
    public ArrayList<User> getAllUsers() {
        this.openReadableDB();
        Cursor cursor = db.query(RECENTS_TABLE,null,null,null,null,null,null);
        ArrayList<User> friends = new ArrayList<User>();
        while(cursor.moveToNext()) {
            friends.add(getUserFromCursor(cursor));
        }
        if(cursor != null) {
            cursor.close();
        }
        this.closeDB();

        return friends;
    }

    /**
     * Check for a user from the database with the given uuid. If no such user exists, return false.
     * @param uuid the uuid
     * @return true if said user exists.
     */
    public boolean hasUserWithUUID(String uuid) {
        String where = UUID + "=?";
        String[] whereArgs = {uuid};
        openReadableDB();
        Cursor cursor = db.query(RECENTS_TABLE,null,where,whereArgs,null,null,null);
        boolean empty = !cursor.moveToFirst();
        closeDB();
        return empty;
    }

    /**
     * Add a list of recent users
     * @param users a list of users to the database
     * @return true if successful
     */
    public boolean addRecentUsers(List<User> users) {
        for(User user: users) {
            addRecentUser(user);
        }
        return true;
    }

    /**
     * Add a user to the database. If the user is already in the database, update them.
     * @param user a user
     * @return true if successful
     */
    public boolean addRecentUser(User user) {
        if(hasUserWithUUID(user.getUUID())) {
            this.openReadableDB();
            ContentValues cv = new ContentValues();
            cv.put(UUID,user.getUUID());
            cv.put(USERNAME,user.getUsername());
            cv.put(STATUS,user.getStatus());
            cv.put(HANGOUT_STATUS,user.getHangoutStatus());
            cv.put(LATITUDE,user.getLatitude());
            cv.put(LONGITUDE, user.getLongitude());
            this.openWriteableDB();
            db.insert(RECENTS_TABLE, null,cv);
            this.closeDB();
        }
        else {
            updateRecentUser(user); // already in db, update them
        }
        return true;
    }

    /**
     * Update many recent users
     * @param users a list of users
     * @return true if successful
     */
    public boolean updateRecentUsers(List<User> users) {
        for(User user : users) {
            updateRecentUser(user);
        }
        return true;
    }

    /**
     * Update a recent user
     * @param user a user
     * @return true if successful
     */
    public boolean updateRecentUser(User user) {
        this.openReadableDB();
        // WHERE
        String uuid = user.getUUID();
        String where = UUID + "=?";
        String[] whereArgs = {uuid};
        // SET
        ContentValues cv = new ContentValues();
        cv.put(USERNAME,user.getUsername());
        cv.put(STATUS,user.getStatus());
        cv.put(HANGOUT_STATUS,user.getHangoutStatus());
        cv.put(LATITUDE,user.getLatitude());
        cv.put(LONGITUDE,user.getLongitude());
        db.update(RECENTS_TABLE,cv,where,whereArgs);
        this.closeDB();
        return true;
    }


    /**
     * Remove a recent user
     * @param user a user
     * @return true if successful
     */
    public boolean removeRecentUser(User user) {
        this.openReadableDB();
        String uuid = user.getUUID();
        String where = UUID + "= ?";
        String[] whereArgs = { uuid };
        this.openWriteableDB();
        db.delete(RECENTS_TABLE, where, whereArgs);
        this.closeDB();
        return true;
    }

    /**
     * Convert a cursor object to a user
     * @param cursor a cursor object
     * @return a user with all the data given by the cursor.
     */
    private static User getUserFromCursor(Cursor cursor) {
        if(cursor == null || cursor.getCount() == 0) {
            return null;
        }
        else {
            try {
                User user = new User(
                        cursor.getString(UUID_COL),
                        cursor.getString(USERNAME_COL),
                        cursor.getInt(STATUS_COL),
                        cursor.getString(HANGOUT_STATUS_COL),
                        cursor.getDouble(LATITUDE_COL),
                        cursor.getDouble(LONGITUDE_COL)
                );
                return user;
            }
            catch(Exception e) {
                return null;
            }
        }
    }

    /**
     * Helper class
     */
    private static class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                        int version) {
            super(context,name,factory,version);
        }

        /**
         * Create the database
         * @param db an SQLiteDatabase object
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_FRIENDS_TABLE);
            // testing refresh on main
            //db.execSQL("INSERT INTO recent_hangout_users VALUES " +
                    //"(\"06bb9301-5a1f-40a8-807c-92ef60ba6154\",\"sup\",0,\"0\",10,50)");
        }

        /**
         * Update the database
         * @param db an SQLiteDatabase object
         * @param oldVersion the old version number
         * @param newVersion the new version number
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d("RecentUsersDB","Upgrading db from version " + oldVersion + " to " + newVersion);

            db.execSQL(LocalDB.DROP_FRIENDS_TABLE);
            onCreate(db);
        }
    }
}
