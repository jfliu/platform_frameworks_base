package android.privacy;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.FileObserver;
import android.privacy.PrivacyPersistenceAdapter;
import android.privacy.utilities.PrivacyDebugger;
/**
 * Copyright (C) 2012-2013 Stefan Thiele (CollegeDev)
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses>.
 */

/**
 * 
 * @author Stefan Thiele (CollegeDev)
 * @author Simeon Morgan <smorgan@digitalfeed.net>
 * Monitors the PDroid-related database and files for unauthorised changes
 */
final class ConfigMonitor extends FileObserver {

    private static final ConfigMonitor instance = new ConfigMonitor();
	private static final String TAG = "ConfigMonitor";
	private static final String WATCH_PATH = PrivacyPersistenceAdapter.DATABASE_FILE;
	private final Set<ConfigMonitorCallback> callbacks;
	
	// Used to track number of current authorized writing programs (rather
	// than just a true-false 'changesInProgress' flag to avoid one PrivacyPersistenceAdapter
	// de-authorising another
	private volatile int authorizedWritesInProgress;
	
	/**
	 * Intent data key for integer-based identifier of the event triggering the intent
	 */
	public static final String MSG_WHAT_INT = "msg_what_i";
	
	/**
	 * Intent data key for a textual message describing the event
	 */
	public static final String MSG_WHAT_STRING = "msg_what_s";
	
	/**
	 * Intent data key: associated data is ArrayList containing the packageNames
	 * for which settings could not be restored
	 * SM: how do we know which did and didn't have settings in the first place...?
	 */
	public static final String MSG_RECOVERY_FAIL_INFO = "rc_info";


    /**
     * MSG will be attached in callBack if someone changed the database
     */
    public static final int MSG_DATABASE_MODIFY = 1;
    /**
     * MSG will be attached in CallBack if someone deleted the database
     */
    public static final int MSG_DATABASE_DELETED = 2;
    /**
     * MSG will be attached in CallBack if someone moved the database
     */
    public static final int MSG_DATABASE_MOVED = 3;
    
	
	/**
	 * Using a singleton, so the constructor should only execute once (when
	 * the singleton is created)
	 */
	private ConfigMonitor() {
	    super(WATCH_PATH);
	}
	
	/**
	 * Singleton retriever function to get the config monitor object (which then allows adding to the
	 * list of callbacks, etc)
	 * @return ConfigMonitor the ConfigMonitor singleton
	 */
	ConfigMonitor getConfigMonitor() {
	    return instance;
	}
	
	/**
	 * Add a callback listener to be notified if the config files
	 * are modified without authorization
	 * @param iface object to do callbacks against
	 */
	void addCallbackListener(ConfigMonitorCallback iface) {
	    synchronized (callbacks) {
    	    callbacks.add(iface);
            PrivacyDebugger.i(TAG,"Added callback listener to watchdog");
    	    if (callbacks.size() == 1) {
        		this.startWatching();
        		PrivacyDebugger.i(TAG,"Commenced listening for config changes");
    	    }
	    }
	}

	/**
	 * Removes a single callback listener from the list of callbacks
	 * @param iface
	 */
	void removeCallbackListener(ConfigMonitorCallback iface) {
	    synchronized (callbacks) {
	        callbacks.remove(iface);
	    }
	}
	
	
	@Override
	public void onEvent(int event, String path) {

	    //data was written to a file
	    if ((FileObserver.MODIFY & event) != 0) {
	    	PrivacyDebugger.w(TAG, "detected database modified");
	    	if(authorizedWritesInProgress <= 0) {
	    		PrivacyDebugger.w(TAG, "inform adapter about modified database");
	    		doChangeCallbacks(MSG_DATABASE_MODIFY);
	    	} else {
	    		PrivacyDebugger.i(TAG, "user is authorized to modify database, do not inform adapter!");
	    	}
	    }
	    //the monitored file or directory was deleted, monitoring effectively stops
	    if ((FileObserver.DELETE_SELF & event) != 0) {
	    	PrivacyDebugger.w(TAG, "detected database deleted");
	    	doChangeCallbacks(MSG_DATABASE_DELETED);
	    }
	    //the monitored file or directory was moved; monitoring continues
	    if ((FileObserver.MOVE_SELF & event) != 0) {
	    	PrivacyDebugger.w(TAG, "detected database moved");
	    	doChangeCallbacks(MSG_DATABASE_MOVED);
	    }
	}
	
	
	/**
	 * Execute 'onUnauthorizedChange' callbacks against all registered callback listeners
	 * TODO: does this need to be synchronized? probably.
	 * @param callbackMessage identifier for the message to be passed
	 */
	private void doChangeCallbacks(int callbackMessage) {
	    synchronized(callbacks) {
            for (ConfigMonitorCallback callback : callbacks) {
                try {
                    callback.onUnauthorizedChange(callbackMessage);
                } catch (Exception e) {}
            }
	    }
	}
	
	
	@Override
	public void finalize () {
		// shit-> inform service about that :-/
		try {
		    synchronized(callbacks) {
	            for (ConfigMonitorCallback callback : callbacks) {
	                try {
	                    callback.onMonitorFinalize(authorizedWritesInProgress);
	                } catch (Exception e) {}
	            } 
		        
		    }
		} catch(Exception e) {
			PrivacyDebugger.e(TAG, "can't inform that WatchDog g finalized!", e);
		}
		try {
			super.finalize();
		} catch(Exception e) {
			//nothing here
		}
	}
	
	/**
	 * Call this method at the beginning of authorized database accesses
	 */
	synchronized void beginAuthorizedTransaction() {
	    authorizedWritesInProgress++;
	}
	
	/**
	 * Call this method at the end of authorized database accesses
	 */
	synchronized void endAuthorizedTransaction() {
	    authorizedWritesInProgress--;
	}
	
	/**
	 * Converts callBack message to readable string
	 * @param msg callBack message
	 * @return readable string for debug or whatever
	 */
	public String msgToString (int msg) {
		switch(msg) {
			case MSG_DATABASE_MODIFY:
				return "database modified";
			case MSG_DATABASE_DELETED:
				return "database deleted";
			case MSG_DATABASE_MOVED:
				return "database moved";
			default:
				return "UNKNOWN";
		}
	}
	
	/**
	 * Callback interface to handle UnauthorizedDatabaseAccesses
	 * @author CollegeDev
	 */
	interface ConfigMonitorCallback {
		
		/**
		 * This method gets a call if someone tries to:
		 * 		- Write to database and modifying permissions
		 * 		- delete the database 
		 * 		- moving the database
		 */
		void onUnauthorizedChange(int MSG_WHAT);
		
		
		/**
		 * This method gets a call if our current monitor gets finalized
		 * -> Monitor is stops watching, we have to initiate a new one
		 * @param authorizedAccessInProgress is the last state of our internal authorizedSave variable
		 */
		void onMonitorFinalize(int authorizedWritesInProgress);
		
	}

}
