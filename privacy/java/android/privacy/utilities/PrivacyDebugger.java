package android.privacy.utilities;

import android.content.Context;
import android.os.Binder;
import android.util.Log;
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
 * Provides Central Privacy Debugging. Use every method with ALL parameters, do not pass null or something else
 * @author CollegeDev (Stefan T.)
 *
 */
public final class PrivacyDebugger {

    private static final String TAG = "PrivacyDebugger";

    private static final String TOGGLE_DEBUGGER_STATE = "android.privacy.TOGGLE_DEBUGGER_STATE";

    private static final int DEBUGGER_ENABLED = 1;
    private static final int DEBUGGER_DISABLED = 2;
    private static final int DEBUGGER_UNKNOWN = -1;

    enum LogLevel { INFO, DEBUG, WARN, ERROR }
    
    private static final String IDENTIFIER = " | OpenPDroid_debug";
    //private static int sDebuggerState = DEBUGGER_UNKNOWN;
    private static int sDebuggerState = DEBUGGER_ENABLED;
    private static boolean enabled = true;

    private PrivacyDebugger () {
        Log.i(TAG,"PrivacyDebugger: constructor triggered");
    }

    /**
     * Used to enabled, disable the debugger. Requires permission: android.privacy.TOGGLE_DEBUGGER_STATE
     * @param state true - enabled , false - disabled
     * @param context
     */
    public static void setDebuggerState(boolean state, Context context) {
        context.enforceCallingPermission(TOGGLE_DEBUGGER_STATE, "Requires TOGGLE_DEBUGGER_STATE");
        if(state) {
            Log.i(TAG, "PrivacyDebugger:setDebuggerState: setting debugger state to enabled");
            sDebuggerState = DEBUGGER_ENABLED;
        } else {
            Log.i(TAG, "PrivacyDebugger:setDebuggerState: setting debugger state to disabled");
            sDebuggerState = DEBUGGER_DISABLED;
        }
        enabled = state;
    }

    /**
     * Tries to get last calling packageName
     * @return packageName or null
     */
    /*private static String getCallingPackage() {
        String[] tmp = ResolveHelper.getCallingPackageName(Binder.getCallingUid());
        if(tmp != null && tmp.length > 0)
            return tmp[0];
        else
            return null;
    }*/


    public static void i(String TAG, String msg) {
        i(TAG, msg, (Throwable)null);
    }

    public static void w(String TAG, String msg) {
        w(TAG, msg, (Throwable)null);
    }

    public static void e(String TAG, String msg) {
        e(TAG, msg, (Throwable)null);
    }

    public static void d(String TAG, String msg) {
        d(TAG, msg, (Throwable)null);
    }

    public static void i(String TAG, String msg, Throwable exception) {
        log(LogLevel.INFO, TAG, msg, exception);
    }

    public static void w(String TAG, String msg, Throwable exception) {
        log(LogLevel.WARN, TAG, msg, exception);
    }

    public static void e(String TAG, String msg, Throwable exception) {
        log(LogLevel.ERROR, TAG, msg, exception);
    }

    public static void d(String TAG, String msg, Throwable exception) {
        log(LogLevel.DEBUG, TAG, msg, exception);
    }

    public static void i(String TAG, String msg, String packageName) {
        i(TAG, msg, packageName, null);
    }

    public static void w(String TAG, String msg, String packageName) {
        w(TAG, msg, packageName, null);
    }

    public static void e(String TAG, String msg, String packageName) {
        e(TAG, msg, packageName, null);
    }

    public static void d(String TAG, String msg, String packageName) {
        d(TAG, msg, packageName, null);
    }

    public static void i(String TAG, String msg, String packageName, Throwable exception) {
        if (msg != null) {
            i(TAG, msg + " - from package: " + packageName, exception);
        }
    }

    public static void w(String TAG, String msg, String packageName, Throwable exception) {
        if (msg != null) {
            w(TAG, msg + " - from package: " + packageName, exception);
        }
    }

    public static void e(String TAG, String msg, String packageName, Throwable exception) {
        if (msg != null) {
            e(TAG, msg + " - from package: " + packageName, exception);
        }
    }
    public static void d(String TAG, String msg, String packageName, Throwable exception) {
        if (msg != null) {
            d(TAG, msg + " - from package: " + packageName, exception);
        }
    }

    private static void log(LogLevel logLevel, String TAG, String msg, Throwable exception) {
        /*
        if (enabled && TAG != null && msg != null) {
            String tmpPackage = getCallingPackage();
            if(tmpPackage != null) {
                if(!exception.equals(helpParam))
                    Log.i(TAG,msg + " - called from package: " + tmpPackage + ". Exception: " + Log.getStackTraceString(exception) + IDENTIFIER);
                else
                    Log.i(TAG,msg + " - called from package: " + tmpPackage + IDENTIFIER);
            } else {
                if(!exception.equals(helpParam))
                    Log.i(TAG,msg + " - called from package: UNKNOWN" + ". Exception: " + Log.getStackTraceString(exception) + IDENTIFIER);
                else
                    Log.i(TAG,msg + " - called from package: UNKNOWN" + IDENTIFIER);
            }
        }*/
        /*
        if (enabled) {
            Log.d("PrivacyDebugger", "Enabled = true");
        } else {
            Log.d("PrivacyDebugger", "Enabled = false");
        }
        
        if (logLevel != null) {
            Log.d("PrivacyDebugger", "Log level: " + logLevel.toString());
        } else {
            Log.d("PrivacyDebugger", "Log level is null");
        }
        (new Throwable()).printStackTrace();
        Log.d("PrivacyDebugger", "Trace", new Throwable());
        if (TAG != null) {
            Log.d("PrivacyDebugger", "Tag != null");
        } else {
            Log.d("PrivacyDebugger", "Tag == null");
        }
        
        if (msg != null) {
            Log.d("PrivacyDebugger", "msg != null");
        } else {
            Log.d("PrivacyDebugger", "msg == null");
        }*/

        if (enabled && TAG != null && msg != null) {
            if (exception != null) {
                switch (logLevel) {
                case INFO:
                    Log.i(TAG, msg, exception);
                    break;
                case WARN:
                    Log.w(TAG, msg, exception);
                    break;
                case ERROR:
                    Log.e(TAG, msg, exception);
                    break;
                case DEBUG:
                    Log.d(TAG, msg, exception);
                    break;
                }
            } else {
                switch (logLevel) {
                case INFO:
                    Log.i(TAG, msg);
                    break;
                case WARN:
                    Log.w(TAG, msg);
                    break;
                case ERROR:
                    Log.e(TAG, msg);
                    break;
                case DEBUG:
                    Log.d(TAG, msg);
                    break;
                }
            }
        }
    }
}
