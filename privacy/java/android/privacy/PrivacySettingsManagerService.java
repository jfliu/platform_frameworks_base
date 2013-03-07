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

import android.R.integer;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.privacy.PrivacyPersistenceAdapter;
import android.privacy.utilities.PrivacyDebugger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * PrivacySettingsManager's counterpart running in the system process, which
 * allows write access to /data/
 * 
 * @author Svyatoslav Hresyk
 * @author Simeon Morgan <smorgan@digitalfeed.net>
 * @author Stefan Thiele (CollegeDev)
 * 
 * TODO: add selective contact access management API
 * 
 *         {@hide}
 */
public final class PrivacySettingsManagerService extends IPrivacySettingsManager.Stub {

    private static final String TAG = "PrivacySettingsManagerService";
    private static final String WRITE_PRIVACY_SETTINGS = "android.privacy.WRITE_PRIVACY_SETTINGS";
    private static final String READ_PRIVACY_SETTINGS = "android.privacy.READ_PRIVACY_SETTINGS";

    private static boolean mSendNotifications = true; 
    private PrivacyPersistenceAdapter mPersistenceAdapter;
    
    private Context mContext;

    public static PrivacyFileObserver sObserver;
    private ConfigMonitor mConfigMonitor;
    //private ConfigMonitorCallbackHandler mConfigMonitorCallback = new ConfigMonitorCallbackHandler(); // NOTE: threading?

    private boolean mEnabled;
    private boolean mNotificationsEnabled;
    private boolean mBootCompleted;

    static final double API_VERSION = 1.51;
    static final double MOD_VERSION = 1.0;
    static final String MOD_DETAILS = "OpenPDroid 1.0 by FFU5y, Mateor, wbedard; forked from PDroid 2.0\n" +
    		"PDroid 2.0 by CollegeDev; forked from PDroid\n" +
    		"PDroid by Syvat's\n" +
    		"Additional contributions by Pastime1971";

    /**
     * @hide - this should be instantiated through Context.getSystemService
     * @param context
     */
    public PrivacySettingsManagerService(Context context) {
        super();
        PrivacyDebugger.i(TAG,
                "PrivacySettingsManagerService - initializing for package: "
                        + context.getPackageName() + " UID: " + Binder.getCallingUid());
        this.mContext = context;

        mPersistenceAdapter = new PrivacyPersistenceAdapter(context);
        sObserver = new PrivacyFileObserver("/data/system/privacy", this);

        mEnabled = mPersistenceAdapter.getValue(PrivacyPersistenceAdapter.SETTING_ENABLED).equals(
                PrivacyPersistenceAdapter.VALUE_TRUE);
        mNotificationsEnabled = mPersistenceAdapter.getValue(
                PrivacyPersistenceAdapter.SETTING_NOTIFICATIONS_ENABLED).equals(
                PrivacyPersistenceAdapter.VALUE_TRUE);
        mBootCompleted = false;
        this.mConfigMonitor = ConfigMonitor.getConfigMonitor();
        synchronized (this.mConfigMonitor) {
            if (this.mConfigMonitor.getCallbackListener() == null) {
                this.mConfigMonitor.setCallbackListener(new ConfigMonitorCallbackHandler());
            }
        }
    }

    public PrivacySettings getSettings(String packageName) {
        // PrivacyDebugger.d(TAG, "getSettings - " + packageName);
        if (mEnabled || mContext.getPackageName().equals("com.privacy.pdroid")
                || mContext.getPackageName().equals("com.privacy.pdroid.Addon")
                || mContext.getPackageName().equals("com.android.privacy.pdroid.extension"))
            // we have to add our addon package here, to get real settings
            return mPersistenceAdapter.getSettings(packageName);
        else
            return null;
    }

    public boolean saveSettings(PrivacySettings settings) throws RemoteException {
        PrivacyDebugger.d(TAG, "saveSettings - checking if caller (UID: " + Binder.getCallingUid()
                + ") has sufficient permissions");
        // Why are we letting the system delete package settings??
        if (Binder.getCallingUid() != 1000) {
            checkCallerCanWriteOrThrow();
        }
        
        PrivacyDebugger.d(TAG, "saveSettings - " + settings);
        boolean result = mPersistenceAdapter.saveSettings(settings);
        if (result == true)
            sObserver.addObserver(settings.getPackageName());
        return result;
    }

    public boolean deleteSettings(String packageName) throws RemoteException {
        // Why are we letting the system delete package settings??
        if (Binder.getCallingUid() != 1000) {
            checkCallerCanWriteOrThrow();
        }

        boolean result = mPersistenceAdapter.deleteSettings(packageName);
        // update observer if directory exists
        String observePath = PrivacyPersistenceAdapter.SETTINGS_DIRECTORY + "/" + packageName;
        if (new File(observePath).exists() && result == true) {
            sObserver.addObserver(observePath);
        } else if (result == true) {
            sObserver.children.remove(observePath);
        }
        return result;
    }
    
    public boolean setGlobalSetting(String setting, String value) throws RemoteException {
        // Why are we letting the system do anything at all?
        if (Binder.getCallingUid() != 1000) {
            checkCallerCanWriteOrThrow();
        }
        return mPersistenceAdapter.setGlobalSetting(setting, value);
    }
    
    public void notification(final String packageName, final byte accessMode,
            final String dataType, final String output) {
        if (mBootCompleted && mNotificationsEnabled && mSendNotifications) {
            Intent intent = new Intent();
            intent.setAction(PrivacySettingsManager.ACTION_PRIVACY_NOTIFICATION);
            intent.putExtra("packageName", packageName);
            intent.putExtra("uid", PrivacyPersistenceAdapter.DUMMY_UID);
            intent.putExtra("accessMode", accessMode);
            intent.putExtra("dataType", dataType);
            intent.putExtra("output", output);
            mContext.sendBroadcast(intent);
        }
    }

    public void registerObservers() throws RemoteException {
        checkCallerCanWriteOrThrow();
        sObserver = new PrivacyFileObserver("/data/system/privacy", this);
    }

    public void addObserver(String packageName) throws RemoteException {
        checkCallerCanWriteOrThrow();
        sObserver.addObserver(packageName);
    }

    public boolean purgeSettings() {
        return mPersistenceAdapter.purgeSettings();
    }

    public void setBootCompleted() {
        try {
            StackTraceElement[] elements = new Throwable().getStackTrace();
            String callerClassName = elements[1].getClassName();
            PrivacyDebugger.d(TAG, "PrivacySettingsManagerService:setBootCompleted: called by " + callerClassName);
        } catch (Exception e) {
            PrivacyDebugger.d(TAG, "PrivacySettingsManagerService:setBootCompleted: Exception while obtaining caller class name");
        }
        mBootCompleted = true;
    }

    public boolean setNotificationsEnabled(boolean enable) throws RemoteException {
        checkCallerCanWriteOrThrow();
        String value = enable ? PrivacyPersistenceAdapter.VALUE_TRUE
                : PrivacyPersistenceAdapter.VALUE_FALSE;
        if (mPersistenceAdapter.setValue(PrivacyPersistenceAdapter.SETTING_NOTIFICATIONS_ENABLED,
                value)) {
            this.mNotificationsEnabled = true;
            this.mBootCompleted = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Enables or disables PDroid protection. If 'enabled' = true, PDroid will
     * return valid settings. Otherwise it will return 'null', which allows all.
     * Setting to 'enabled' has immediate effects; setting to 'disabled' has no effect until next reboot.
     * @param newIsEnabled 
     * @return new 'enabled' state.
     */
    public boolean setEnabled(boolean newIsEnabled) throws RemoteException {
        checkCallerCanWriteOrThrow();
        String value = newIsEnabled ? PrivacyPersistenceAdapter.VALUE_TRUE
                : PrivacyPersistenceAdapter.VALUE_FALSE;
        if (mPersistenceAdapter.setValue(PrivacyPersistenceAdapter.SETTING_ENABLED, value)) {
            this.mEnabled = true;
            return true;
        } else {
            return false;
        }
    }
    
        /**
     * Check the caller of the service has privileges to write to it
	 * Throw an exception if not. 
	 */
	private void checkCallerCanWriteOrThrow() throws RemoteException {
		mContext.enforceCallingPermission(WRITE_PRIVACY_SETTINGS,
				"Requires WRITE_PRIVACY_SETTINGS");
		//for future:
		// if not allowed then throw
		//			throw new SecurityException("Attempted to write without sufficient priviliges");

	}
	
	/**
	 * Check that the caller of the service has privileges to write to it.
	 * @return true if caller can write, false otherwise.
	 */
	private boolean checkCallerCanWriteSettings() throws RemoteException {
		try {
			checkCallerCanWriteOrThrow();
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	/**
	 * Check the caller of the service has privileges to read from it
	 * Throw an exception if not. 
	 */
	private void checkCallerCanReadOrThrow() {
		if (Binder.getCallingUid() == 1000) {
			return;
		}
		mContext.enforceCallingPermission(READ_PRIVACY_SETTINGS,
				"Requires READ_PRIVACY_SETTINGS");
		//for future:
		// if not allowed then throw
		//			throw new SecurityException("Attempted to read without sufficient priviliges");

	}
	
	/**
	 * Check that the caller of the service has privileges to read from it.
	 * @return true if caller can read, false otherwise.
	 */
	private boolean checkCallerCanReadSettings() {
		try {
			checkCallerCanReadOrThrow();
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}
	
	
	public static final String DEBUG_FLAG_SEND_NOTIFICATIONS = "sendNotifications";
	public static final String DEBUG_FLAG_OPEN_AND_CLOSE_DB = "openAndCloseDb";
	
	public static final int DEBUG_FLAG_TYPE_INTEGER = 0;
	public static final int DEBUG_FLAG_TYPE_BOOLEAN = 1;
	
    public void setDebugFlagInt(String flagName, int value) throws RemoteException {
        checkCallerCanWriteOrThrow();
        // There are currently no debug flags which are integers
        throw new RemoteException();
    }
    
    public int getDebugFlagInt(String flagName) throws RemoteException {
        checkCallerCanWriteOrThrow();
        throw new RemoteException();
    }
    
    public void setDebugFlagBool(String flagName, boolean value) throws RemoteException {
        checkCallerCanWriteOrThrow();
        if (flagName.equals(DEBUG_FLAG_OPEN_AND_CLOSE_DB)) {
            this.mPersistenceAdapter.setOpenAndCloseDb(value);
        } else if (flagName.equals(DEBUG_FLAG_SEND_NOTIFICATIONS)) {
            this.mSendNotifications = value;
        } else {
            throw new RemoteException();
        }
    }
    
    public boolean getDebugFlagBool(String flagName) throws RemoteException {
        checkCallerCanWriteOrThrow();
        if (flagName.equals(DEBUG_FLAG_OPEN_AND_CLOSE_DB)) {
            return this.mPersistenceAdapter.getOpenAndCloseDb();
        } else if (flagName.equals(DEBUG_FLAG_SEND_NOTIFICATIONS)) {
            return this.mSendNotifications;
        } else {
            throw new RemoteException();
        }
	}
    
    public Map getDebugFlags() {
        Map<String, Integer> debugFlags = new HashMap<String, Integer>();
        debugFlags.put(DEBUG_FLAG_OPEN_AND_CLOSE_DB, DEBUG_FLAG_TYPE_BOOLEAN);
        debugFlags.put(DEBUG_FLAG_SEND_NOTIFICATIONS, DEBUG_FLAG_TYPE_BOOLEAN);
        return debugFlags;
    }
    
    class ConfigMonitorCallbackHandler implements ConfigMonitor.ConfigMonitorCallback {

        @Override
        public void onUnauthorizedChange(int MSG_WHAT) {
            PrivacyDebugger.i(TAG,
                    "Config monitor: unauthorized change");
            if (mPersistenceAdapter == null) {
                PrivacyDebugger.d(TAG, "PrivacySettingsManagerService: onUnauthorizedChange: getting new PrivacyPersistenceAdapter");
                mPersistenceAdapter = new PrivacyPersistenceAdapter(mContext);  
            } else {
                PrivacyDebugger.d(TAG, "PrivacySettingsManagerService: onUnauthorizedChange: Already have a PrivacyPersistenceAdapter");
            }
            PrivacyDebugger.d(TAG, "PrivacySettingsManagerService: onUnauthorizedChange: Executing rebuildFromCache");
            mPersistenceAdapter.rebuildFromCache();
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onMonitorFinalize(int authorizedWritesInProgress) {
            // TODO Auto-generated method stub
            PrivacyDebugger.i(TAG,
                    "Config monitor: finalize");
            
        }
        
    }
}
