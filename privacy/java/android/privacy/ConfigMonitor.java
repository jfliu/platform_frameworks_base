package android.privacy;

import java.io.FileNotFoundException;
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
final class ConfigMonitor {

    private static ConfigMonitor sInstance;
	private static final String TAG = "ConfigMonitor";
	private static final String DATABASE_FILE = PrivacyPersistenceAdapter.DATABASE_FILE;
	private static final String DATABASE_JOURNAL_FILE = PrivacyPersistenceAdapter.DATABASE_JOURNAL_FILE;
	private static final String SETTINGS_DIRECTORY = PrivacyPersistenceAdapter.SETTINGS_DIRECTORY;
	
	private static final int ID_DATABASE_MONITOR = 0;
	private static final int ID_DATABASE_JOURNAL_MONITOR = 1;
	private static final int ID_SETTINGS_MONITOR = 2;
	
	private static final int MONITOR_DELAY = 2000; // Delay before starting watching when triggered, and delay before 
	
	private ConfigMonitorCallback mCallback;
	
	/**
	 * Individual file observer objects: monitor database, journal, and additional configuration folder
	 */
	private FileObserver mDatabaseMonitor;
	private FileObserver mJournalMonitor;
	private FileObserver mFolderMonitor;
	
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
     * Messages for the activities which can be detected
     */
    public static final int MSG_DATABASE_MODIFIED = 1;
    public static final int MSG_DATABASE_DELETED = 2;
    public static final int MSG_DATABASE_MOVED = 3;

    public static final int MSG_DATABASE_JOURNAL_MODIFIED = 4;
    public static final int MSG_DATABASE_JOURNAL_DELETED = 5;
    public static final int MSG_DATABASE_JOURNAL_MOVED = 6;
    
    public static final int MSG_SETTINGS_MODIFIED = 7;
    public static final int MSG_SETTINGS_DELETED = 8;
    public static final int MSG_SETTINGS_MOVED = 9;
    
	
	/**
	 * Using a singleton, so the constructor should only execute once (when
	 * the singleton is created)
	 * TODO: Add exception handling
	 */
	private ConfigMonitor() {
	    PrivacyDebugger.i(TAG,"ConfigMonitor constructor triggered");
	}
	
	private void startMonitors() {
	    PrivacyDebugger.i(TAG,"Starting monitors");
	    if (mDatabaseMonitor == null) {
	        mDatabaseMonitor = new Monitor(DATABASE_FILE, ID_DATABASE_MONITOR);
	    }
	    if (mJournalMonitor == null) {
	        mJournalMonitor = new Monitor(DATABASE_JOURNAL_FILE, ID_DATABASE_JOURNAL_MONITOR);
	    }
	    if (mFolderMonitor == null) {
	        mFolderMonitor = new Monitor(SETTINGS_DIRECTORY, ID_SETTINGS_MONITOR);
	    }

	    // Need to delay the start-up of folder monitors due to delays in inotify notifications being received (or I think that's the issue)
        (new Thread() {
            public void run() {
                try {
                    sleep(MONITOR_DELAY);
                } catch (Exception e) {
                } finally {
                    if (mDatabaseMonitor == null) {
                        mDatabaseMonitor.startWatching();
                    }
                    if (mJournalMonitor == null) {
                        mJournalMonitor.startWatching();
                    }
                    // Need to delay the start-up of folder monitors due to delays in inotify notifications
                    if (mFolderMonitor == null) {
                        mFolderMonitor.startWatching();
                    }
                }
            }
        }).start();
	}
	
	private void stopMonitors() {
	    PrivacyDebugger.i(TAG,"Stopping monitors");
        if (mDatabaseMonitor != null) {
            mDatabaseMonitor.stopWatching();
            mDatabaseMonitor = null;
        }
        if (mJournalMonitor != null) {
            mJournalMonitor.stopWatching();
            mJournalMonitor = null;
        }
        if (mFolderMonitor != null) {
            mFolderMonitor.stopWatching();
            mFolderMonitor = null;
        }
	}
	
	/**
	 * Singleton retriever function to get the config monitor object (which then allows adding to the
	 * list of callbacks, etc)
	 * @return ConfigMonitor the ConfigMonitor singleton
	 */
	synchronized static ConfigMonitor getConfigMonitor() {
	    PrivacyDebugger.i(TAG,"getConfigMonitor");
	    // Check that the monitors are all running correctly
	    if (sInstance == null) {
	        sInstance = new ConfigMonitor();
	    }
	    return sInstance;
	}
	
	/**
	 * Add a callback listener to be notified if the config files
	 * are modified without authorization
	 * @param iface object to do callbacks against
	 */
	void setCallbackListener(ConfigMonitorCallback iface) {
	    synchronized(sInstance) {
	        if (iface != null) {
	            this.startMonitors();
	        } else {
	            this.stopMonitors();
	        }
	        mCallback = iface;
	    }
	    PrivacyDebugger.i(TAG,"Set callback listener on ConfigMonitor");
	}

	ConfigMonitorCallback getCallbackListener() {
	    synchronized(sInstance) {
	        return mCallback;
	    }
	}
	
	public void handleEvent(int observerId, int event, String path) {

	    PrivacyDebugger.w(TAG, "Detected change: observerId " + Integer.toString(observerId) + " : event " + Integer.toString(event) + " : path " + path);
	    if(authorizedWritesInProgress <= 0) {
	        //data was written to a file
	        if ((FileObserver.MODIFY & event) != 0) {
	            PrivacyDebugger.w(TAG, "Detected modification: observeId is " + Integer.toString(observerId));
	            switch (observerId) {
	            case ID_DATABASE_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_DATABASE_MODIFIED);
	                break;
	            case ID_DATABASE_JOURNAL_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_DATABASE_JOURNAL_MODIFIED);
	                break;
	            case ID_SETTINGS_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_SETTINGS_MODIFIED);
	                break;
	            }
	        } else if (((FileObserver.DELETE_SELF | FileObserver.DELETE) & event) != 0) {
	            // File, folder, or a subfile or folder was deleted
	            PrivacyDebugger.w(TAG, "Detected deletion: observerId is " + Integer.toString(observerId));
	            switch (observerId) {
	            case ID_DATABASE_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_DATABASE_DELETED);
	                break;
	            case ID_DATABASE_JOURNAL_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_DATABASE_JOURNAL_DELETED);
	                break;
	            case ID_SETTINGS_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_SETTINGS_DELETED);
	                break;
	            }
	        } else if (((FileObserver.MOVE_SELF | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) & event) != 0) {
	            PrivacyDebugger.w(TAG, "Detected move: observerId is " + Integer.toString(observerId));
	            switch (observerId) {
	            case ID_DATABASE_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_DATABASE_MOVED);
	                break;
	            case ID_DATABASE_JOURNAL_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_DATABASE_JOURNAL_MOVED);
	                break;
	            case ID_SETTINGS_MONITOR:
	                mCallback.onUnauthorizedChange(MSG_SETTINGS_MOVED);
	                break;
	            }
	        }
	    } else {
	        PrivacyDebugger.i(TAG, "user is authorized to modify database, do not inform adapter!");
	    }
	    //If the main file or folder has been deleted or moved, clear the observer since monitoring will stop
	    if (((FileObserver.DELETE_SELF | FileObserver.MOVE_SELF) & event) != 0) {
	        switch (observerId) {
	        case ID_DATABASE_MONITOR:
	            if (mDatabaseMonitor != null) {
	                mDatabaseMonitor.stopWatching();
	                mDatabaseMonitor = null;
	            }
	            break;
	        case ID_DATABASE_JOURNAL_MONITOR:
	            if (mJournalMonitor != null) {
	                mJournalMonitor.stopWatching();
	                mJournalMonitor = null;
	            }
	            break;
	        case ID_SETTINGS_MONITOR:
	            if (mFolderMonitor != null) {
	                mFolderMonitor.stopWatching();
	                mFolderMonitor = null;
	            }
	            break;
	        }
	    }
	}
	
	
	public void handleFinalize(int observerId) {
		// shit-> inform service about that :-/
		try {
		    mCallback.onMonitorFinalize(authorizedWritesInProgress);
		} catch(Exception e) {
			PrivacyDebugger.e(TAG, "can't inform that WatchDog g finalized!", e);
		}
        switch (observerId) {
        case ID_DATABASE_MONITOR:
            mDatabaseMonitor = null;
            break;
        case ID_DATABASE_JOURNAL_MONITOR:
            mJournalMonitor = null;
            break;
        case ID_SETTINGS_MONITOR:
            mFolderMonitor = null;
            break;
        }
        synchronized (sInstance) {
            startMonitors();            
        }
	}
	
	/**
	 * Call this method at the beginning of authorized database accesses
	 */
	synchronized void beginAuthorizedTransaction() {
	    PrivacyDebugger.i(TAG,"beginAuthorizedTransaction");
	    authorizedWritesInProgress++;
	}
	
	/**
	 * Call this method at the end of authorized database accesses
	 */
	synchronized void endAuthorizedTransaction() {
	    PrivacyDebugger.i(TAG,"endAuthorizedTransaction");
	    (new Thread() {
	        public void run() {
	            try {
	                sleep(MONITOR_DELAY);
	            } catch (Exception e) {
	            } finally {
	                authorizedWritesInProgress--;
	            }
	        }
	    }).start();
	}
	
	/**
	 * Converts callBack message to readable string
	 * @param msg callBack message
	 * @return readable string for debug or whatever
	 */
	public String msgToString (int msg) {
		switch(msg) {
			case MSG_DATABASE_MODIFIED:
				return "database modified";
			case MSG_DATABASE_DELETED:
				return "database deleted";
			case MSG_DATABASE_MOVED:
				return "database moved";
            case MSG_DATABASE_JOURNAL_MODIFIED:
                return "database journal modified";
            case MSG_DATABASE_JOURNAL_DELETED:
                return "database journal deleted";
            case MSG_DATABASE_JOURNAL_MOVED:
                return "database journal moved";
            case MSG_SETTINGS_MODIFIED:
                return "settings folder/files modified";
            case MSG_SETTINGS_DELETED:
                return "settings folder/files deleted";
            case MSG_SETTINGS_MOVED:
                return "settings folder/files moved";
			default:
				return "UNKNOWN";
		}
	}
	
	private class Monitor extends FileObserver {
	    private int observerId;
	    
        public Monitor(String path, int observerId) {
            //super(path, FileObserver.DELETE | FileObserver.DELETE_SELF | FileObserver.MODIFY | FileObserver.MOVE_SELF | FileObserver.MOVED_FROM | FileObserver.MOVED_TO );
            super(path, FileObserver.ALL_EVENTS);
            this.observerId = observerId;
        }
        
        @Override
        public void onEvent(int event, String path) {
            PrivacyDebugger.d("ConfigMonitor", "Event " + Integer.toString(event) + " on " + path);
            handleEvent(observerId, event, path);
        }
        
        @Override
        public void finalize () {
            PrivacyDebugger.d("ConfigMonitor", "Finalize on " + Integer.toString(observerId));
            handleFinalize(observerId);
            super.finalize();
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
