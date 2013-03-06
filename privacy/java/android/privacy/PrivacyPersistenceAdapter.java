/**
 * Copyright (C) 2012 Svyatoslav Hresyk
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses>.
 */

package android.privacy;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.FileUtils;
import android.privacy.PrivacySettings;
import android.privacy.utilities.PrivacyDebugger;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Map.Entry;

/**
 * Responsible for persisting privacy settings to built-in memory
 * 
 * @author Svyatoslav Hresyk
 * @author CollegeDev (Stefan T.)
 * @author Simeon Morgan <smorgan@digitalfeed.net>
 * {@hide}
 */
public final class PrivacyPersistenceAdapter {

    private static final String TAG = "PrivacyPersistenceAdapter";
    private static final int RETRY_QUERY_COUNT = 5;

    // Used to save settings for access from core libraries
    static final String SETTINGS_DIRECTORY = "/data/system/privacy";
    static final String DATABASE_FILE = "/data/system/privacy.db";
    static final String DATABASE_JOURNAL_FILE = "/data/system/privacy.db-journal";

    private static final int DATABASE_VERSION = 4;
    private static final boolean LOG_LOCKING = false;
    private static final boolean LOG_OPEN_AND_CLOSE = true;
    private static final boolean LOG_CACHE = true;
    public static final int DUMMY_UID = -1;

    /**
     * Number of threads currently reading the database
     */
    private static volatile Integer sDbAccessThreads = 0;
    private static volatile int sDbVersion;
    private static volatile boolean sInitialized; // A flag so that database upgrades etc will only be handled on the first start for any given boot-up 

    /**
     * START DEBUG VARIABLES
     */
    private static volatile boolean autoCloseDb = false;
    /**
     * END DEBUG VARIABLES
     */
    // Used to lock the database during multi-statement operations to prevent
    // internally inconsistent data reads.
    // Multiple locks could be used to improve efficiency (i.e. for different tables)
    private static ReadWriteLock sLock = new ReentrantReadWriteLock();

    private ConfigMonitor mConfigMonitor;

    //By using the same locks for the database and the cache, we can separate read and write locks without extra synchronization 
    private static final HashMap<String, PrivacySettings> sCache = new HashMap<String, PrivacySettings>();

    private static final String TABLE_SETTINGS = "settings";
    private static final String TABLE_MAP = "map";
    private static final String TABLE_ALLOWED_CONTACTS = "allowed_contacts";
    private static final String TABLE_VERSION = "version";

    private static final String CREATE_TABLE_SETTINGS = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " ( " + 
                    " _id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                    " packageName TEXT, " + 
                    " uid INTEGER, " + 
                    " deviceIdSetting INTEGER, " + 
                    " deviceId TEXT, " + 
                    " line1NumberSetting INTEGER, " + 
                    " line1Number TEXT, " + 
                    " locationGpsSetting INTEGER, " + 
                    " locationGpsLat TEXT, " + 
                    " locationGpsLon TEXT, " + 
                    " locationNetworkSetting INTEGER, " + 
                    " locationNetworkLat TEXT, " + 
                    " locationNetworkLon TEXT, " + 
                    " networkInfoSetting INTEGER, " + 
                    " simInfoSetting INTEGER, " + 
                    " simSerialNumberSetting INTEGER, " + 
                    " simSerialNumber TEXT, " + 
                    " subscriberIdSetting INTEGER, " + 
                    " subscriberId TEXT, " + 
                    " accountsSetting INTEGER, " + 
                    " accountsAuthTokensSetting INTEGER, " + 
                    " outgoingCallsSetting INTEGER, " + 
                    " incomingCallsSetting INTEGER, " + 
                    " contactsSetting INTEGER, " + 
                    " calendarSetting INTEGER, " + 
                    " mmsSetting INTEGER, " + 
                    " smsSetting INTEGER, " + 
                    " callLogSetting INTEGER, " + 
                    " bookmarksSetting INTEGER, " + 
                    " systemLogsSetting INTEGER, " + 
                    " externalStorageSetting INTEGER, " + 
                    " cameraSetting INTEGER, " + 
                    " recordAudioSetting INTEGER, " + 
                    " notificationSetting INTEGER, " + 
                    " intentBootCompletedSetting INTEGER," + 
                    " smsSendSetting INTEGER," + 
                    " phoneCallSetting INTEGER," +
                    " ipTableProtectSetting INTEGER," +
                    " iccAccessSetting INTEGER," +
                    " addOnManagementSetting INTEGER," + //this setting indicate if app is managed by addon or not
                    " androidIdSetting INTEGER," +
                    " androidId TEXT," +
                    " wifiInfoSetting INTEGER," +
                    " switchConnectivitySetting INTEGER," +
                    " sendMmsSetting INTEGER," +
                    " forceOnlineState INTEGER," + 
                    " switchWifiStateSetting INTEGER" +
                    ");";


    private static final String CREATE_TABLE_MAP = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_MAP + " ( name TEXT PRIMARY KEY, value TEXT );";

    private static final String CREATE_TABLE_ALLOWED_CONTACTS = 
            "CREATE TABLE IF NOT EXISTS " + TABLE_ALLOWED_CONTACTS + " ( settings_id, contact_id, PRIMARY KEY(settings_id, contact_id) );";

    private static final String INSERT_VERSION = 
            "INSERT OR REPLACE INTO " + TABLE_MAP + " (name, value) " + "VALUES (\"db_version\", " + DATABASE_VERSION + ");";

    private static final String INSERT_ENABLED = 
            "INSERT OR REPLACE INTO " + TABLE_MAP + " (name, value) " + "VALUES (\"enabled\", \"1\");";

    private static final String INSERT_NOTIFICATIONS_ENABLED = 
            "INSERT OR REPLACE INTO " + TABLE_MAP + " (name, value) " + "VALUES (\"notifications_enabled\", \"1\");";

    private static final String[] DATABASE_FIELDS = new String[] { "_id", "packageName", "uid",
        "deviceIdSetting", "deviceId", "line1NumberSetting", "line1Number",
        "locationGpsSetting", "locationGpsLat", "locationGpsLon", "locationNetworkSetting",
        "locationNetworkLat", "locationNetworkLon", "networkInfoSetting", "simInfoSetting",
        "simSerialNumberSetting", "simSerialNumber", "subscriberIdSetting", "subscriberId",
        "accountsSetting", "accountsAuthTokensSetting", "outgoingCallsSetting",
        "incomingCallsSetting", "contactsSetting", "calendarSetting", "mmsSetting",
        "smsSetting", "callLogSetting", "bookmarksSetting", "systemLogsSetting",
        "externalStorageSetting", "cameraSetting", "recordAudioSetting", "notificationSetting",
        "intentBootCompletedSetting", "smsSendSetting", "phoneCallSetting",
        "ipTableProtectSetting", "iccAccessSetting", "addOnManagementSetting",
        "androidIdSetting", "androidId", "wifiInfoSetting", "switchConnectivitySetting",
        "sendMmsSetting", "forceOnlineState", "switchWifiStateSetting" };

    public static final String SETTING_ENABLED = "enabled";
    public static final String SETTING_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String SETTING_DB_VERSION = "db_version";
    public static final String VALUE_TRUE = "1";
    public static final String VALUE_FALSE = "0";

    private SQLiteDatabase mDb;

    private Context mContext;


    /**
     * START DEBUG FUNCTIONS
     */

    void setOpenAndCloseDb(boolean value) {
        autoCloseDb = value;
        if (value) {
            if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:setOpenAndCloseDb: Open and close enabled");
        } else {
            if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:setOpenAndCloseDb: Open and close disabled");
        }
    }

    boolean getOpenAndCloseDb() {
        return this.autoCloseDb;
    }
    
    /**
     * END DEBUG FUNCTIONS
     */


    public PrivacyPersistenceAdapter(Context context) {
        this.mContext = context;
        this.mConfigMonitor = ConfigMonitor.getConfigMonitor();
        if (!sInitialized) {
            initialize();
            fillCache();
            sInitialized = true;
        }
    }

    private void initialize() {
        // create the database and settings directory if we have write
        // permission and they do not exist
        if (new File("/data/system/").canWrite()) { 
            if (!(new File(DATABASE_FILE).exists() && new File(SETTINGS_DIRECTORY).exists())) {
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:constructor: WriteLock: (pre)lock");
                sLock.writeLock().lock();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:constructor: WriteLock: (post)lock");
                try {
                    if (!new File(DATABASE_FILE).exists()) {
                        createDatabase();
                    }
                    if (!new File(SETTINGS_DIRECTORY).exists()) {
                        createSettingsDir();
                    }
                } finally {
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:constructor: WriteLock: (pre)unlock");
                    sLock.writeLock().unlock();
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:constructor: WriteLock: (post)unlock");
                }
            }

            this.mConfigMonitor = ConfigMonitor.getConfigMonitor(); // get again: starts any observers that are not already started (in case dirs were made above)

            // upgrade if needed
            sDbVersion = getDbVersion();

            if (sDbVersion < DATABASE_VERSION) {
                upgradeDatabase();
            }
        }
    }

    private void upgradeDatabase() {
        if (sDbVersion < DATABASE_VERSION) {
            PrivacyDebugger.i(TAG, "PrivacyPersistenceAdapter:upgradeDatabase - upgrading DB from version " + sDbVersion + " to "
                    + DATABASE_VERSION);

            SQLiteDatabase db = null;

            switch (sDbVersion) {
            case 1:
            case 2:
            case 3:
                synchronized (sDbAccessThreads) {
                    sDbAccessThreads++;
                }
                if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Increment DB access threads: now " + Integer.toString(sDbAccessThreads));
                this.mConfigMonitor.beginAuthorizedTransaction();
                try {
                    db = getDatabase();
                    if (db == null && !db.isOpen()) {
                        PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Database not obtained");
                        throw new SQLiteException("Database could not be obtained");
                    }

                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: WriteLock: (pre)lock");
                    sLock.writeLock().lock();
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: WriteLock: (post)lock");

                    try {
                        // check the db version again to make sure that another thread has not already done the upgrade
                        // in the meantime
                        if (sDbVersion < DATABASE_VERSION) {                                
                            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Transaction: (pre)begin");
                            db.beginTransaction();
                            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Transaction: (post)begin");

                            try {
                                db.execSQL("DROP TABLE IF EXISTS " + TABLE_VERSION + ";");
                                db.execSQL(CREATE_TABLE_ALLOWED_CONTACTS);
                                db.execSQL(CREATE_TABLE_MAP);
                                db.execSQL(INSERT_VERSION);
                                db.execSQL(INSERT_ENABLED);
                                db.execSQL(INSERT_NOTIFICATIONS_ENABLED);

                                purgeSettings();

                                // remove uid dirs from the settings directory
                                // TODO: improve handling so that if an exception happens while
                                //      this process is in progress, it doesn't leave the filesystem
                                //      entries in an invalid state
                                File settingsDir = new File(SETTINGS_DIRECTORY);
                                for (File packageDir : settingsDir.listFiles()) {
                                    for (File uidDir : packageDir.listFiles()) {
                                        if (uidDir.isDirectory()) {
                                            File[] settingsFiles = uidDir.listFiles();
                                            // copy the first found (most likely
                                            // the only one) one level up
                                            if (settingsFiles[0] != null) {
                                                File newPath = new File(packageDir + "/"
                                                        + settingsFiles[0].getName());
                                                newPath.delete();
                                                settingsFiles[0].renameTo(newPath);
                                                try {
                                                    deleteRecursive(uidDir);
                                                } catch (Exception e) {
                                                    PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Exception during deleteRecursive for uidDir", e);
                                                }
                                            }
                                        }
                                    }
                                }

                                db.setTransactionSuccessful();
                                sDbVersion = DATABASE_VERSION;
                            } finally {                                
                                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Transaction: (pre)end");
                                db.endTransaction();
                                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Transaction: (post)end");
                            }
                        } else {
                            // The database has been upgraded elsewhere; end the db transaction
                            // and don't make any further changes
                        }

                    } finally {
                        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: WriteLock: (pre)unlock");
                        sLock.writeLock().unlock();
                        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: WriteLock: (post)unlock");
                        this.mConfigMonitor.endAuthorizedTransaction();
                    }
                } catch (SQLException e) {
                    PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: SQLException occurred performing database upgrade", e);
                } finally {
                    closeIdleDatabase();
                }
                break;

            case 4:
                // most current version, do nothing
                PrivacyDebugger.i(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Database is already at the most recent version");
                break;
            }
        }
    }


    private int getDbVersion() {
        String versionString = getValue(SETTING_DB_VERSION);
        if (versionString == null) {
            PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:getDbVersion: getValue returned null; assuming version = 1");
            return 1;
        } else {
            try {
                return Integer.parseInt(versionString);
            } catch (Exception e) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:getDbVersion: failed to parse database version; returning 1");
                return 1;
            }
        }
    }


    public String getValue(String name) {
        SQLiteDatabase db;
        Cursor c;
        String output = null;

        synchronized (sDbAccessThreads) {
            sDbAccessThreads++;
        }       
        if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:getValue: Increment DB access threads: now " + Integer.toString(sDbAccessThreads));

        try {
            db = getDatabase();
            if (db == null || !db.isOpen()) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:getValue: Database not obtained while getting value for name: " + name);
                throw new SQLiteException("Database could not be obtained");
            }

            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:getValue: ReadLock: (pre)lock");
            sLock.readLock().lock();
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:getValue: ReadLock: (post)lock");
            try {

                c = query(db, TABLE_MAP, new String[] { "value" }, "name=?", new String[] { name },
                        null, null, null, null);
                if (c != null && c.getCount() > 0 && c.moveToFirst()) {
                    output = c.getString(c.getColumnIndex("value"));
                    c.close();
                } else {
                    PrivacyDebugger.w(TAG, "PrivacyPersistenceAdapter:getValue: Could not get value for name: " + name);
                }
            } catch (Exception e) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:getValue: Exception occurred while getting value for name: " + name, e);
            } finally {
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:getValue: ReadLock: (pre)unlock");
                sLock.readLock().unlock();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:getValue: ReadLock: (post)unlock");
            }

        } finally {
            closeIdleDatabase();
        }
        return output;
    }

    public boolean setValue(String name, String value) {
        PrivacyDebugger.e(TAG, "setValue - name " + name + " value " + value);
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("value", value);

        boolean success = false;

        SQLiteDatabase db;

        synchronized (sDbAccessThreads) {
            sDbAccessThreads++;
        }
        if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:setValue: Increment DB access threads: now " + Integer.toString(sDbAccessThreads));
        this.mConfigMonitor.beginAuthorizedTransaction();

        try {
            db = getDatabase();
            if (db == null || !db.isOpen()) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:getValue: Database not obtained while getting value for name: " + name);
                throw new SQLiteException("Database could not be obtained");
            }

            // updating the version is atomic, but we need to use a lock
            // to make sure we don't try to get/update the version while the DB is being created or upgraded
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:setValue: WriteLock: (pre)lock");
            sLock.writeLock().lock();
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:setValue: WriteLock: (post)lock");
            try {
                success = db.replace(TABLE_MAP, null, values) != -1;
            } finally {
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:setValue: WriteLock: (pre)unlock");
                sLock.writeLock().unlock();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:setValue: WriteLock: (post)unlock");
            }
        } finally {
            this.mConfigMonitor.endAuthorizedTransaction();
            closeIdleDatabase();
        }

        return success;
    }

    /**
     * Retrieve privacy settings for a single package
     * 
     * @param packageName
     *            package for which to retrieve settings
     * @return privacy settings for the package, or null if no settings exist
     *         for it
     */
    public PrivacySettings getSettings(String packageName) {
        PrivacySettings privacySettings = null;

        if (packageName == null) {
            throw new InvalidParameterException(
                    "PrivacyPersistenceAdapter:getSettings:insufficient application identifier - package name is required");
        }

        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:getSettings: ReadLock: (pre)lock");
        sLock.readLock().lock();
        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:getSettings: ReadLock: (post)lock");
        try {
            return sCache.get(packageName);
        } finally {
            sLock.readLock().unlock();
        }
    }

    /**
     * Read all PrivacySettings from the database and load them into the memory cache
     */
    private void fillCache() {

        SQLiteDatabase db;

        synchronized (sDbAccessThreads) {
            sDbAccessThreads++;
        }
        if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:fillCache: Increment DB access threads: now " + Integer.toString(sDbAccessThreads));
        try {
            // indicate that the DB is being read to prevent closing by other threads
            db = getDatabase();
            if (db == null && !db.isOpen()) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:fillCache: Database not obtained");
                throw new SQLiteException("Database could not be obtained");
            }

            Cursor cursor = null;

            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:fillCache: WriteLock: (pre)lock");
            sLock.readLock().lock();
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:fillCache: WriteLock: (post)lock");
            try {
                cursor = db.query(TABLE_SETTINGS, DATABASE_FIELDS, null, null, null, null, null);
                if (cursor != null && cursor.getCount() != 0 && cursor.moveToFirst()) {
                    {
                        PrivacySettings privacySettings;
                        do {
                            try {
                                //sure, it's optimised by not having to check the column numbers, but is it worth it???
                                privacySettings = new PrivacySettings(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), (byte)cursor.getShort(3), cursor.getString(4), 
                                        (byte)cursor.getShort(5), cursor.getString(6), (byte)cursor.getShort(7), cursor.getString(8), cursor.getString(9), (byte)cursor.getShort(10), 
                                        cursor.getString(11), cursor.getString(12), (byte)cursor.getShort(13), (byte)cursor.getShort(14), (byte)cursor.getShort(15), 
                                        cursor.getString(16), (byte)cursor.getShort(17), cursor.getString(18), (byte)cursor.getShort(19), (byte)cursor.getShort(20), 
                                        (byte)cursor.getShort(21), (byte)cursor.getShort(22), (byte)cursor.getShort(23), (byte)cursor.getShort(24), (byte)cursor.getShort(25), 
                                        (byte)cursor.getShort(26), (byte)cursor.getShort(27), (byte)cursor.getShort(28), (byte)cursor.getShort(29), (byte)cursor.getShort(30), 
                                        (byte)cursor.getShort(31), (byte)cursor.getShort(32), (byte)cursor.getShort(33), (byte)cursor.getShort(34), null, (byte)cursor.getShort(35), (byte)cursor.getShort(36), 
                                        (byte)cursor.getShort(37), (byte)cursor.getShort(38), (byte)cursor.getShort(39), (byte)cursor.getShort(40), cursor.getString(41), (byte)cursor.getShort(42),
                                        (byte)cursor.getShort(43), (byte)cursor.getShort(44), (byte)cursor.getShort(45), (byte)cursor.getShort(46));

                                sCache.put(privacySettings.getPackageName(), privacySettings);
                            } catch (Exception e) {
                                e.printStackTrace();
                                PrivacyDebugger.e(TAG, "Error occurred in getSettingsAll");
                            }
                        } while (cursor.moveToNext());
                    }
                    cursor.close();

                    for (Entry<String, PrivacySettings> entry : sCache.entrySet()) {
                        // get allowed contacts IDs if necessary
                        cursor = query(db, TABLE_ALLOWED_CONTACTS, new String [] { "contact_id" }, "settings_id=?", new String [] { Integer.toString(entry.getValue().get_id()) }, null, null, null, null);

                        if (cursor != null && cursor.getCount() > 0) {
                            int[] allowedContacts = new int[cursor.getCount()];
                            while (cursor.moveToNext()) allowedContacts[cursor.getPosition()] = cursor.getInt(1);
                            entry.getValue().setAllowedContacts(allowedContacts);
                        }
                    }
                }   
            } catch (Exception e) {
                PrivacyDebugger.e(TAG, "fillCache: Exception occurred while getting settings", e);
            } finally {
                if (cursor != null)
                    cursor.close();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:fillCache: ReadLock: (pre)unlock");
                sLock.readLock().unlock();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:fillCache: ReadLock: (post)unlock");
            }
        } finally {
            closeIdleDatabase();
        }
    }

    /**
     * Saves the settings object fields into DB and into plain text files where
     * applicable. The DB changes will not be made persistent if saving settings
     * to plain text files fails.
     * 
     * @param s
     *            settings object
     * @return true if settings were saved successfully, false otherwise
     */
    public boolean saveSettings(PrivacySettings s) {
        boolean result = false;

        String packageName = s.getPackageName();

        if (packageName == null || packageName.isEmpty()) {
            PrivacyDebugger.e(TAG, "saveSettings - either package name is missing");
            throw new InvalidParameterException("PackageName can not be null or empty");
        }

        ContentValues values = new ContentValues();
        values.put("packageName", packageName);
        // values.put("uid", uid);
        values.put("uid", DUMMY_UID);

        values.put("deviceIdSetting", s.getDeviceIdSetting());
        values.put("deviceId", s.getDeviceId());

        values.put("line1NumberSetting", s.getLine1NumberSetting());
        values.put("line1Number", s.getLine1Number());

        values.put("locationGpsSetting", s.getLocationGpsSetting());
        values.put("locationGpsLat", s.getLocationGpsLat());
        values.put("locationGpsLon", s.getLocationGpsLon());

        values.put("locationNetworkSetting", s.getLocationNetworkSetting());
        values.put("locationNetworkLat", s.getLocationNetworkLat());
        values.put("locationNetworkLon", s.getLocationNetworkLon());

        values.put("networkInfoSetting", s.getNetworkInfoSetting());
        values.put("simInfoSetting", s.getSimInfoSetting());

        values.put("simSerialNumberSetting", s.getSimSerialNumberSetting());
        values.put("simSerialNumber", s.getSimSerialNumber());
        values.put("subscriberIdSetting", s.getSubscriberIdSetting());
        values.put("subscriberId", s.getSubscriberId());

        values.put("accountsSetting", s.getAccountsSetting());
        values.put("accountsAuthTokensSetting", s.getAccountsAuthTokensSetting());
        values.put("outgoingCallsSetting", s.getOutgoingCallsSetting());
        values.put("incomingCallsSetting", s.getIncomingCallsSetting());

        values.put("contactsSetting", s.getContactsSetting());
        values.put("calendarSetting", s.getCalendarSetting());
        values.put("mmsSetting", s.getMmsSetting());
        values.put("smsSetting", s.getSmsSetting());
        values.put("callLogSetting", s.getCallLogSetting());
        values.put("bookmarksSetting", s.getBookmarksSetting());
        values.put("systemLogsSetting", s.getSystemLogsSetting());
        values.put("notificationSetting", s.getNotificationSetting());
        values.put("intentBootCompletedSetting", s.getIntentBootCompletedSetting());
        // values.put("externalStorageSetting", s.getExternalStorageSetting());
        values.put("cameraSetting", s.getCameraSetting());
        values.put("recordAudioSetting", s.getRecordAudioSetting());
        values.put("smsSendSetting", s.getSmsSendSetting());
        values.put("phoneCallSetting", s.getPhoneCallSetting());
        values.put("ipTableProtectSetting", s.getIpTableProtectSetting());
        values.put("iccAccessSetting", s.getIccAccessSetting());
        values.put("addOnManagementSetting", s.getAddOnManagementSetting());
        values.put("androidIdSetting", s.getAndroidIdSetting());
        values.put("androidId", s.getAndroidID());
        values.put("wifiInfoSetting", s.getWifiInfoSetting());
        values.put("switchConnectivitySetting", s.getSwitchConnectivitySetting());
        values.put("sendMmsSetting", s.getSendMmsSetting());
        values.put("forceOnlineState", s.getForceOnlineState());
        values.put("switchWifiStateSetting", s.getSwitchWifiStateSetting());

        SQLiteDatabase db = null;
        Cursor cursor = null;

        synchronized(sDbAccessThreads) {
            sDbAccessThreads++;
        }
        if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: Increment DB access threads: now " + sDbAccessThreads.toString());
        this.mConfigMonitor.beginAuthorizedTransaction();
        try {
            db = getDatabase();
            if (db == null || !db.isOpen()) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Database not obtained");
                throw new SQLiteException("Database could not be obtained");
            }
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: WriteLock: (pre)lock");
            sLock.writeLock().lock();
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: WriteLock: (post)lock");
            try {
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: Transaction: (pre)begin");
                db.beginTransaction();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: Transaction: (post)begin");
                try {

                    // save settings to the DB
                    Integer id = s.get_id();

                    if (id != null) { // existing entry -> update
                        if (db.update(TABLE_SETTINGS, values, "_id=?", new String[] { id.toString() }) < 1) {
                            throw new Exception("saveSettings - failed to update database entry");
                        }

                        db.delete(TABLE_ALLOWED_CONTACTS, "settings_id=?", new String[] { id.toString() });
                        int[] allowedContacts = s.getAllowedContacts();
                        if (allowedContacts != null) {
                            ContentValues contactsValues = new ContentValues();
                            for (int i = 0; i < allowedContacts.length; i++) {
                                contactsValues.put("settings_id", id);
                                contactsValues.put("contact_id", allowedContacts[i]);
                                if (db.insert(TABLE_ALLOWED_CONTACTS, null, contactsValues) == -1)
                                    throw new Exception(
                                            "PrivacyPersistenceAdapter:saveSettings: failed to update database entry (contacts)");
                            }
                        }

                    } else { // new entry -> insert if no duplicates exist
                        // PrivacyDebugger.d(TAG,
                        // "saveSettings - new entry; verifying if duplicates exist");
                        cursor = db.query(TABLE_SETTINGS, new String[] { "_id" }, "packageName=?",
                                new String[] { s.getPackageName() }, null, null, null);

                        if (cursor != null) {
                            if (cursor.getCount() == 1) { // exactly one entry
                                // exists -> update
                                // PrivacyDebugger.d(TAG, "saveSettings - updating existing entry");
                                if (db.update(TABLE_SETTINGS, values, "packageName=?",
                                        new String[] { s.getPackageName() }) < 1) {
                                    throw new Exception("saveSettings - failed to update database entry");
                                }

                                if (cursor.moveToFirst()) {
                                    Integer idAlt = cursor.getInt(0); // id of the found
                                    // duplicate entry
                                    db.delete(TABLE_ALLOWED_CONTACTS, "settings_id=?",
                                            new String[] { idAlt.toString() });
                                    int[] allowedContacts = s.getAllowedContacts();
                                    if (allowedContacts != null) {
                                        ContentValues contactsValues = new ContentValues();
                                        for (int i = 0; i < allowedContacts.length; i++) {
                                            contactsValues.put("settings_id", idAlt);
                                            contactsValues.put("contact_id", allowedContacts[i]);
                                            if (db.insert(TABLE_ALLOWED_CONTACTS, null, contactsValues) == -1)
                                                throw new Exception(
                                                        "saveSettings - failed to update database entry (contacts)");
                                        }
                                    }
                                }
                            } else if (cursor.getCount() == 0) { // no entries -> insert
                                // PrivacyDebugger.d(TAG, "saveSettings - inserting new entry");
                                long rowId = db.insert(TABLE_SETTINGS, null, values);
                                if (rowId == -1) {
                                    throw new Exception(
                                            "PrivacyPersistenceAdapter:saveSettings - failed to insert new record into DB");
                                }

                                db.delete(TABLE_ALLOWED_CONTACTS, "settings_id=?",
                                        new String[] { Long.toString(rowId) });
                                int[] allowedContacts = s.getAllowedContacts();
                                if (allowedContacts != null) {
                                    ContentValues contactsValues = new ContentValues();
                                    for (int i = 0; i < allowedContacts.length; i++) {
                                        contactsValues.put("settings_id", rowId);
                                        contactsValues.put("contact_id", allowedContacts[i]);
                                        if (db.insert(TABLE_ALLOWED_CONTACTS, null, contactsValues) == -1)
                                            throw new Exception(
                                                    "PrivacyPersistenceAdapter:saveSettings:failed to update database entry (contacts)");
                                    }
                                }
                            } else {
                                // something went totally wrong and there are
                                // multiple entries for same identifier
                                throw new Exception("PrivacyPersistenceAdapter:saveSettings:duplicate entries in the privacy.db");
                            }
                        } else {
                            // jump to catch block to avoid marking transaction as
                            // successful
                            throw new Exception("PrivacyPersistenceAdapter:saveSettings:cursor is null, database access failed");
                        }
                    }

                    // save settings to plain text file (for access from core libraries)
                    if (!writeExternalPackageSettings(s)) {
                        throw new Exception("PrivacyPersistenceAdapter:saveSettings:failed to write external settings files");
                    }

                    // mark DB transaction successful (commit the changes)
                    db.setTransactionSuccessful();

                    sCache.put(packageName, s);
                    if (LOG_CACHE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: Cache update for" + packageName);
                } finally {
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: Transaction: (pre)end");
                    db.endTransaction(); // we want to transition from set transaction successful to end as fast as possible to avoid errors (see the Android docs)
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: Transaction: (post)end");

                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } finally {
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: WriteLock: (pre)unlock");
                sLock.writeLock().unlock();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: WriteLock: (post)unlock");
                this.mConfigMonitor.endAuthorizedTransaction();
            }
            result = true;
        } catch (Exception e) {
            PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:saveSettings: saving for " + packageName + " failed", e);
        } finally {
            closeIdleDatabase();
        }

        return result;
    }

    /**
     * Write the complete contents of the cache out to the database and file system. Used in case of
     * unauthorized database change/removal. 
     */
    void rebuildFromCache() {
        PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: starting");
        SQLiteDatabase db = null;
        Cursor cursor = null;

        // Get a write lock so nothing can read or write while the DB is being rebuilt
        // In the interim, responses could be provided from the cache (but are not at this stage, because it is a high overhead for a very rare occurance).
        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: WriteLock: (pre)lock");
        sLock.writeLock().lock();
        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: WriteLock: (post)lock");
        try {
            mConfigMonitor.beginAuthorizedTransaction();

            if (mDb != null && mDb.isOpen()) {
                mDb.close(); //close the database, whether or not it is in use
                PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Closed the database");
            } else {
                PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Database already closed");
            }
            
            synchronized(sDbAccessThreads) {
                sDbAccessThreads = 0;
            }
            PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Reset db access threads to 0");

            try {
                deleteRecursive(new File(DATABASE_FILE));
                PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Deleted database file");
            } catch (Exception e) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Error occurred flushing database file");
            }
            try {
                deleteRecursive(new File(DATABASE_JOURNAL_FILE));
                PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Deleted database journal file");
            } catch (Exception e) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:rebuildFromCache: error occurred flushing database journal file");
            }
            try {
                deleteRecursive(new File(SETTINGS_DIRECTORY));
                PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Deleted settings directory");
            } catch (Exception e) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:rebuildFromCache: error occurred flushing settings directory");
            }
            mConfigMonitor.endAuthorizedTransaction();
            PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Executing 'initialize'");
            initialize();

            synchronized(sDbAccessThreads) {
                sDbAccessThreads++;
            }
            if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:rebuildFromCache: Increment DB access threads: now " + sDbAccessThreads.toString());
            this.mConfigMonitor.beginAuthorizedTransaction();
            try {
                db = getDatabase();

                if (db == null || !db.isOpen()) {
                    PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:rebuildFromCache: Database not obtained");
                    throw new SQLiteException("Database could not be obtained");
                } else {
                    PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Got database");
                }
                
                try {
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:rebuildFromCache: Transaction: (pre)begin");
                    db.beginTransaction();
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:rebuildFromCache: Transaction: (post)begin");
                    try {
                        String packageName;
                        PrivacySettings s;
                        ContentValues values = new ContentValues(); // These ContentValues are reused to save object creation, so all values MUST be set each time
                        for (Entry<String, PrivacySettings> entry : sCache.entrySet()) {
                            PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Writing settings for " + entry.getKey());
                            packageName = entry.getKey();
                            s = entry.getValue();
                            if (packageName == null || packageName.isEmpty()) {
                                PrivacyDebugger.e(TAG, "rebuildFromCache - package name is missing");
                                continue;
                            }

                            values.put("packageName", packageName);
                            // values.put("uid", uid);
                            values.put("uid", DUMMY_UID);

                            values.put("deviceIdSetting", s.getDeviceIdSetting());
                            values.put("deviceId", s.getDeviceId());

                            values.put("line1NumberSetting", s.getLine1NumberSetting());
                            values.put("line1Number", s.getLine1Number());

                            values.put("locationGpsSetting", s.getLocationGpsSetting());
                            values.put("locationGpsLat", s.getLocationGpsLat());
                            values.put("locationGpsLon", s.getLocationGpsLon());

                            values.put("locationNetworkSetting", s.getLocationNetworkSetting());
                            values.put("locationNetworkLat", s.getLocationNetworkLat());
                            values.put("locationNetworkLon", s.getLocationNetworkLon());

                            values.put("networkInfoSetting", s.getNetworkInfoSetting());
                            values.put("simInfoSetting", s.getSimInfoSetting());

                            values.put("simSerialNumberSetting", s.getSimSerialNumberSetting());
                            values.put("simSerialNumber", s.getSimSerialNumber());
                            values.put("subscriberIdSetting", s.getSubscriberIdSetting());
                            values.put("subscriberId", s.getSubscriberId());

                            values.put("accountsSetting", s.getAccountsSetting());
                            values.put("accountsAuthTokensSetting", s.getAccountsAuthTokensSetting());
                            values.put("outgoingCallsSetting", s.getOutgoingCallsSetting());
                            values.put("incomingCallsSetting", s.getIncomingCallsSetting());

                            values.put("contactsSetting", s.getContactsSetting());
                            values.put("calendarSetting", s.getCalendarSetting());
                            values.put("mmsSetting", s.getMmsSetting());
                            values.put("smsSetting", s.getSmsSetting());
                            values.put("callLogSetting", s.getCallLogSetting());
                            values.put("bookmarksSetting", s.getBookmarksSetting());
                            values.put("systemLogsSetting", s.getSystemLogsSetting());
                            values.put("notificationSetting", s.getNotificationSetting());
                            values.put("intentBootCompletedSetting", s.getIntentBootCompletedSetting());
                            // values.put("externalStorageSetting", s.getExternalStorageSetting());
                            values.put("cameraSetting", s.getCameraSetting());
                            values.put("recordAudioSetting", s.getRecordAudioSetting());
                            values.put("smsSendSetting", s.getSmsSendSetting());
                            values.put("phoneCallSetting", s.getPhoneCallSetting());
                            values.put("ipTableProtectSetting", s.getIpTableProtectSetting());
                            values.put("iccAccessSetting", s.getIccAccessSetting());
                            values.put("addOnManagementSetting", s.getAddOnManagementSetting());
                            values.put("androidIdSetting", s.getAndroidIdSetting());
                            values.put("androidId", s.getAndroidID());
                            values.put("wifiInfoSetting", s.getWifiInfoSetting());
                            values.put("switchConnectivitySetting", s.getSwitchConnectivitySetting());
                            values.put("sendMmsSetting", s.getSendMmsSetting());
                            values.put("forceOnlineState", s.getForceOnlineState());
                            values.put("switchWifiStateSetting", s.getSwitchWifiStateSetting());

                            // save settings to the DB
                            try {
                                long rowId = db.insert(TABLE_SETTINGS, null, values);
                                if (rowId == -1) {
                                    PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:rebuildFromCache: failed insert entry: " + s.getPackageName());
                                }

                                int[] allowedContacts = s.getAllowedContacts();
                                if (allowedContacts != null) {
                                    ContentValues contactsValues = new ContentValues();
                                    for (int i = 0; i < allowedContacts.length; i++) {
                                        contactsValues.put("settings_id", rowId);
                                        contactsValues.put("contact_id", allowedContacts[i]);
                                        if (db.insert(TABLE_ALLOWED_CONTACTS, null, contactsValues) == -1)
                                            PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: failed to update database entry (6)");
                                    }
                                }

                                // save settings to plain text file (for access from core libraries)
                                if (!writeExternalPackageSettings(s)) {
                                    throw new Exception("PrivacyPersistenceAdapter:saveSettings:failed to write external settings files");
                                }

                            } catch (Exception e) {
                                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Error restoring for package " + s.getPackageName());
                            }
                        }
                    } catch (Exception e) {
                        PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Error restoring entries");

                    } finally {
                        PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: rebuildFromCache: Finished rebuilding from cache");
                        // mark DB transaction successful (commit the changes)
                        db.setTransactionSuccessful();
                        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: Transaction: (pre)end");
                        db.endTransaction(); // we want to transition from set transaction successful to end as fast as possible to avoid errors (see the Android docs)
                        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: Transaction: (post)end");

                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } finally {
                    closeIdleDatabase();
                }
            } catch (Exception e) {
                PrivacyDebugger.d(TAG, "rebuildFromCache: Exception occurred while running rebuildFromCache", e);
            } finally {
                this.mConfigMonitor.endAuthorizedTransaction();
            }
        } finally {
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: WriteLock: (pre)unlock");
            sLock.writeLock().unlock();
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:saveSettings: WriteLock: (post)unlock");
        }
    }

    
    /**
     * This method creates external settings files for access from core libraries
     * 
     * @param settings
     *            Map of setting names and values: setting name is used as filename, and value as content
     * @param packageName
     *            name of package, or null if global setting
     * @return true if file was successful written
     * @throws Exception
     *             if we cannot write settings to directory
     */
    private boolean writeExternalSetting(Map<String, String> settings, String packageName)
            throws Exception {
        // save settings to plain text file (for access from core libraries)
        File settingsFolder;
        if (packageName == null) {
            settingsFolder = new File(SETTINGS_DIRECTORY + File.separator);
        } else {
            settingsFolder = new File(SETTINGS_DIRECTORY + File.separator + packageName + File.separator);
        }
        
        boolean result = false;

        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:writeExternalSettings: WriteLock: (pre)lock");
        sLock.writeLock().lock();
        if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:writeExternalSettings: WriteLock: (post)lock");
        try {
            if (!settingsFolder.exists()) {
                settingsFolder.mkdirs();
                settingsFolder.setReadable(true, false);
                settingsFolder.setExecutable(true, false);
            }
            
            File settingFile;
            for (Entry<String, String> entry : settings.entrySet()) {
                settingFile = new File(settingsettingsFolder.getAbsolutePath() + File.separator + entry.getKey());
                if (!settingFile.exists()) {
                    settingFile.createNewFile();
                    settingFile.setReadable(true, false);
                    settingFile.setExecutable(true, false);
                }
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(
                        settingFile));
                writer.append(entry.getValue());
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            // jump to catch block to avoid marking transaction as successful
            throw new Exception("writeExternalFile - could not write settings to file", e);
        } finally {
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:writeExternalSettings: WriteLock: (pre)unlock");
            sLock.writeLock().unlock();
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:writeExternalSettings: WriteLock: (post)unlock");
        }

        return true;
    }
    
    
    /**
     * This method creates external settings files for access from core libraries
     * 
     * @param settingsName
     *            field name from database
     * @param packageName
     *            name of package
     * @param s
     *            settings from package
     * @return true if file was successful written
     * @throws Exception
     *             if we cannot write settings to directory
     */
    private boolean writeExternalPackageSettings(PrivacySettings s)
            throws Exception {
        Map settingMap = new HashMap<String, String>();
        settingMap.put("systemLogsSetting", s.getSystemLogsSetting() + "");
        settingMap.put("ipTableProtectSetting", s.getIpTableProtectSetting() + "");
        return writeExternalSetting(settingMap, s.getPackageName());
    }

    /**
     * Deletes a settings entry from the DB
     * 
     * @return true if settings were deleted successfully, false otherwise
     */
    public boolean deleteSettings(String packageName) {
        boolean result = true;

        SQLiteDatabase db = null;
        synchronized (sDbAccessThreads) {
            sDbAccessThreads++;
        }
        if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: Increment DB access threads: now " + sDbAccessThreads.toString());
        this.mConfigMonitor.beginAuthorizedTransaction();
        try {            
            db = getDatabase();

            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: WriteLock: (pre)lock");
            sLock.writeLock().lock();
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: WriteLock: (post)lock");
            try {
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: Transaction: (pre)begin");
                db.beginTransaction();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: Transaction: (post)begin");
                // make sure this ends up in a consistent state
                try {
                    // try deleting contacts allowed entries; do not fail if deletion
                    // not possible
                    // TODO: restructure this into a more efficient query (ideally a
                    // single query without a cursor)
                    Cursor c = db.query(TABLE_SETTINGS, new String[] { "_id" }, "packageName=?",
                            new String[] { packageName }, null, null, null);


                    if (c != null && c.getCount() > 0 && c.moveToFirst()) {
                        int id = c.getInt(0);
                        db.delete(TABLE_ALLOWED_CONTACTS, "settings_id=?",
                                new String[] { Integer.toString(id) });
                        c.close();
                    } else {
                        PrivacyDebugger.e(TAG, "deleteSettings - database entry for " + packageName + " not found");
                    }

                    if (db.delete(TABLE_SETTINGS, "packageName=?", new String[] { packageName }) == 0) {
                        PrivacyDebugger.e(TAG, "deleteSettings - database entry for " + packageName + " not found");
                    }

                    // delete settings from plain text file (for access from core
                    // libraries)
                    File settingsPackageDir = new File("/data/system/privacy/" + packageName + "/");
                    File systemLogsSettingFile = new File("/data/system/privacy/" + packageName
                            + "/systemLogsSetting");

                    // delete the setting files
                    systemLogsSettingFile.delete();
                    // delete the parent directories
                    if (settingsPackageDir.list() == null || settingsPackageDir.list().length == 0)
                        settingsPackageDir.delete();

                    db.setTransactionSuccessful();

                    sCache.remove(packageName);
                } finally {
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: Transaction: (pre)end");
                    db.endTransaction();
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: Transaction: (post)end");
                }
            } finally {
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: WriteLock: (pre)unlock");
                sLock.writeLock().unlock();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:deleteSettings: WriteLock: (post)unlock");
                this.mConfigMonitor.endAuthorizedTransaction();
            }
        } catch (SQLiteException e) {
            result = false;
            PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:deleteSettings: failed to open the database, or open a transaction", e);
        } catch (Exception e) {
            result = false;
            PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:deleteSettings - could not delete settings", e);
        } finally {
            closeIdleDatabase();
        }

        return result;
    }

    private Cursor query(SQLiteDatabase db, String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy, String limit)
                    throws Exception {
        Cursor c = null;
        // make sure getting settings does not fail because of
        // IllegalStateException (db already closed)
        boolean success = false;
        for (int i = 0; success == false && i < RETRY_QUERY_COUNT; i++) {
            try {
                if (c != null)
                    c.close();
                c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy,
                        limit);
                success = true;
            } catch (IllegalStateException e) {
                success = false;
                if (db != null && db.isOpen()) {
                    db.close();
                }
                db = getDatabase();
            }
        }
        if (success == false)
            throw new Exception("query - failed to execute query on the DB");
        return c;
    }

    private Cursor rawQuery(SQLiteDatabase db, String sql) throws Exception {
        Cursor c = null;
        // make sure getting settings does not fail because of
        // IllegalStateException (db already closed)
        boolean success = false;
        for (int i = 0; success == false && i < RETRY_QUERY_COUNT; i++) {
            try {
                if (c != null)
                    c.close();
                c = db.rawQuery(sql, null);
                success = true;
            } catch (IllegalStateException e) {
                success = false;
                if (db != null && db.isOpen()) {
                    db.close();
                }
                db = getDatabase();
            }
        }
        if (success == false)
            throw new Exception("query - failed to execute query on the DB");
        return c;
    }

    /**
     * Removes obsolete entries from the DB and file system. Should not be used
     * in methods, which rely on the DB being open after this method has
     * finished. It will close the DB if no other threads has increased the
     * readingThread count.
     * SM: I really want to modify this so that the persistence adapter doesn't need to hold onto a context
     * 
     * @return true if purge was successful, false otherwise.
     */
    public boolean purgeSettings() {
        boolean result = true;

        // get installed apps
        Set<String> apps = new HashSet<String>();
        PackageManager pMan = mContext.getPackageManager();
        List<ApplicationInfo> installedApps = pMan.getInstalledApplications(0);
        for (ApplicationInfo appInfo : installedApps) {
            apps.add(appInfo.packageName);
        }

        SQLiteDatabase db = null;
        synchronized (sDbAccessThreads) {
            sDbAccessThreads++;
        }
        if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: Increment DB access threads: now " + sDbAccessThreads.toString());
        this.mConfigMonitor.beginAuthorizedTransaction();

        try {
            // delete obsolete entries from DB and update outdated entries
            db = getDatabase();
            if (db == null) {
                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:purgeSettings: db could not be obtained");
                return false;
            }

            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: WriteLock: (pre)lock");
            sLock.writeLock().lock();
            if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: WriteLock: (post)lock");
            try {
                Cursor cursor = null;
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: Transaction: (pre)begin");
                db.beginTransaction();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: Transaction: (post)begin");
                try {
                    cursor = query(db, TABLE_SETTINGS, new String[] { "packageName" }, null, null, null, null,
                            null, null);
                    if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                        do {
                            String packageName = cursor.getString(0);
                            if (!apps.contains(packageName)) {
                                db.delete(TABLE_SETTINGS, "packageName = ?", new String [] { packageName });
                            }
                        } while (cursor.moveToNext());
                    }

                    // delete obsolete settings directories
                    File settingsDir = new File(SETTINGS_DIRECTORY);
                    for (File packageDir : settingsDir.listFiles()) {
                        String packageName = packageDir.getName();
                        if (!apps.contains(packageName)) { // remove package dir if no such
                            // app installed
                            try {
                                deleteRecursive(packageDir);
                            } catch (Exception e) {
                                PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:upgradeDatabase: Exception during deleteRecursive for uidDir", e);
                            }
                        }
                    }

                    for (Entry<String, PrivacySettings> entry : sCache.entrySet()) {
                        if (!apps.contains(entry.getKey())) {
                            sCache.remove(entry.getKey());
                        }
                    }


                    db.setTransactionSuccessful();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: Transaction: (pre)end");
                    db.endTransaction();
                    if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: Transaction: (post)end");
                }
            } finally {
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: WriteLock: (pre)unlock");
                sLock.writeLock().unlock();
                if (LOG_LOCKING) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:purgeSettings: WriteLock: (post)unlock");
                this.mConfigMonitor.endAuthorizedTransaction();
            }
        } catch (Exception e) {
            PrivacyDebugger.e(TAG, "PrivacyPersistenceAdapter:purgeSettings - purging DB failed", e);
            result = false;
        } finally {
            closeIdleDatabase();
        }

        return result;
    }


    private boolean deleteRecursive(File fileOrDirectory) throws FileNotFoundException {
        boolean result;
        if (!fileOrDirectory.exists()) throw new FileNotFoundException(fileOrDirectory.getAbsolutePath());
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        }
        return fileOrDirectory.delete();
    }

    private void createDatabase() {
        PrivacyDebugger.i(TAG, "createDatabase - creating privacy database file");
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(DATABASE_FILE, null,
                    SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
            PrivacyDebugger.i(TAG, "createDatabase - creating privacy database");
            db.execSQL(CREATE_TABLE_SETTINGS);
            db.execSQL(CREATE_TABLE_ALLOWED_CONTACTS);
            db.execSQL(CREATE_TABLE_MAP);
            db.execSQL(INSERT_VERSION);
            db.execSQL(INSERT_ENABLED);
            db.execSQL(INSERT_NOTIFICATIONS_ENABLED);
            // PrivacyDebugger.d(TAG, "createDatabase - closing connection to privacy.db");
            if (db != null && db.isOpen()) {
                db.close();
            }
        } catch (SQLException e) {
            PrivacyDebugger.e(TAG, "createDatabase - failed to create privacy database", e);
        }
    }

    private void createSettingsDir() {
        // create settings directory (for settings accessed from core libraries)
        File settingsDir = new File("/data/system/privacy/");
        settingsDir.mkdirs();
        // make it readable for everybody
        settingsDir.setReadable(true, false);
        settingsDir.setExecutable(true, false);
    }

    private synchronized SQLiteDatabase getDatabase() {
        if (mDb == null || !mDb.isOpen() || mDb.isReadOnly()) {
            if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter: getDatabase: Opening privacy database");
            mDb = SQLiteDatabase.openDatabase(DATABASE_FILE, null, SQLiteDatabase.OPEN_READWRITE);
        }   
        return mDb;
    }

    /**
     * If there are no more threads reading the database, close it. Otherwise,
     * reduce the number of reading threads by one
     */
    private void closeIdleDatabase() {

        if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:closeIdleDatabase: About to decrement DB access threads: now " + sDbAccessThreads.toString());
        // only close DB if no other threads are reading
        synchronized (sDbAccessThreads) { //need to lock to make sure something else hasn't beat
            sDbAccessThreads--;
            if (sDbAccessThreads <= 0 && mDb != null && mDb.isOpen()) {
                sDbAccessThreads = 0; //just in case it has gone to a negative due to rebuild from cache while an action was in progress
                if (autoCloseDb) { 
                    if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:closeIdleDatabase: Closing the PDroid database");
                    mDb.close();
                } else {
                    if (LOG_OPEN_AND_CLOSE) PrivacyDebugger.d(TAG, "PrivacyPersistenceAdapter:closeIdleDatabase: Open and close DB disabled: not closing");
                }
            }
        }
    }
}
