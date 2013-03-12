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

package android.privacy.surrogate;

import android.content.Context;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.privacy.IPrivacySettings;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.provider.Browser;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.privacy.utilities.PrivacyDebugger;
import android.util.Log;

/**
 * Provides privacy handling for {@link android.content.ContentResolver}
 * @author Svyatoslav Hresyk 
 * {@hide}
 */
public final class PrivacyContentResolver {

    private static final String TAG = "PrivacyContentResolver";

    private static final String SMS_CONTENT_URI_AUTHORITY = "sms";
    private static final String MMS_CONTENT_URI_AUTHORITY = "mms";
    private static final String MMS_SMS_CONTENT_URI_AUTHORITY = "mms-sms";

    private static PrivacySettingsManager mPrvSvc;

    /**
     * Returns a dummy database cursor if access is restricted by privacy settings
     * @param uri
     * @param context
     * @param realCursor
     */
    public static Cursor enforcePrivacyPermission(Uri uri, String[] projection, Context context, Cursor realCursor) throws RemoteException {
        //    public static Cursor enforcePrivacyPermission(Uri uri, Context context, Cursor realCursor) {
        if (uri != null) {
            String auth = uri.getAuthority();
            String output_label = "[real]";
            Cursor output = realCursor;
            if (auth != null) {
                if (auth.equals(android.provider.Contacts.AUTHORITY) || auth.equals(ContactsContract.AUTHORITY)) {

                    if (mPrvSvc == null) mPrvSvc = PrivacySettingsManager.getPrivacyService(context);
                    int uid = context.getApplicationInfo().uid;
                    IPrivacySettings pSet = mPrvSvc.getSettingsSafe(uid);
                    if (pSet.getContactsSetting() == IPrivacySettings.CUSTOM && 
                            uri.toString().contains(ContactsContract.Contacts.CONTENT_URI.toString())) {

                        boolean idFound = false;
                        if (projection != null) {
                            for (String p : projection) {
                                if (p.equals(ContactsContract.Contacts._ID)) {
                                    idFound = true;
                                    break;
                                }
                            }
                        }

                        if (!idFound) {
                            output = new PrivacyCursor();
                        } else {
                            output = new PrivacyCursor(output, pSet.getAllowedContacts());
                        }
                    } else if (PrivacySettings.getOutcome(pSet.getContactsSetting()) != IPrivacySettings.REAL) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                    }
                    mPrvSvc.notification(uid, pSet.getContactsSetting(), IPrivacySettings.DATA_CONTACTS, null);
                    
                } else if (auth.equals(CalendarContract.AUTHORITY)) {
                    if (mPrvSvc == null) mPrvSvc = PrivacySettingsManager.getPrivacyService(context);
                    int uid = context.getApplicationInfo().uid;
                    IPrivacySettings pSet = mPrvSvc.getSettingsSafe(uid);
                    if (PrivacySettings.getOutcome(pSet.getCalendarSetting()) != IPrivacySettings.REAL) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                    }
                    mPrvSvc.notification(uid, pSet.getCalendarSetting(), IPrivacySettings.DATA_CALENDAR, null);
                    
                } else if (auth.equals(MMS_CONTENT_URI_AUTHORITY)) {
                    if (mPrvSvc == null) mPrvSvc = PrivacySettingsManager.getPrivacyService(context);
                    int uid = context.getApplicationInfo().uid;
                    IPrivacySettings pSet = mPrvSvc.getSettingsSafe(uid);
                    if (PrivacySettings.getOutcome(pSet.getMmsSetting()) != IPrivacySettings.REAL) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                    }
                    mPrvSvc.notification(uid, pSet.getMmsSetting(), IPrivacySettings.DATA_MMS, null);

                } else if (auth.equals(SMS_CONTENT_URI_AUTHORITY)) {
                    if (mPrvSvc == null) mPrvSvc = PrivacySettingsManager.getPrivacyService(context);
                    int uid = context.getApplicationInfo().uid;
                    IPrivacySettings pSet = mPrvSvc.getSettingsSafe(uid);
                    if (PrivacySettings.getOutcome(pSet.getSmsSetting()) != IPrivacySettings.REAL) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                    }
                    mPrvSvc.notification(uid, pSet.getSmsSetting(), IPrivacySettings.DATA_SMS, null);
                    // all messages, sms and mms
                } else if (auth.equals(MMS_SMS_CONTENT_URI_AUTHORITY) || 
                        auth.equals("mms-sms-v2") /* htc specific, accessed by system messages application */) { 

                    // deny access if access to either sms, mms or both is restricted by privacy settings
                    if (mPrvSvc == null) mPrvSvc = PrivacySettingsManager.getPrivacyService(context);
                    int uid = context.getApplicationInfo().uid;
                    IPrivacySettings pSet = mPrvSvc.getSettingsSafe(uid);
                    if (PrivacySettings.getOutcome(pSet.getMmsSetting()) != IPrivacySettings.REAL || PrivacySettings.getOutcome(pSet.getSmsSetting()) != IPrivacySettings.REAL) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                    }
                    mPrvSvc.notification(uid, pSet.getMmsSetting(), IPrivacySettings.DATA_MMS_SMS, null);
                    
                } else if (auth.equals(CallLog.AUTHORITY)) {
                    if (mPrvSvc == null) mPrvSvc = PrivacySettingsManager.getPrivacyService(context);
                    int uid = context.getApplicationInfo().uid;
                    IPrivacySettings pSet = mPrvSvc.getSettingsSafe(uid);
                    if (PrivacySettings.getOutcome(pSet.getCallLogSetting()) != IPrivacySettings.REAL) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                    }
                    mPrvSvc.notification(uid, pSet.getCallLogSetting(), IPrivacySettings.DATA_CALL_LOG, null);

                } else if (auth.equals(Browser.BOOKMARKS_URI.getAuthority())) {
                    if (mPrvSvc == null) mPrvSvc = PrivacySettingsManager.getPrivacyService(context);
                    int uid = context.getApplicationInfo().uid;
                    IPrivacySettings pSet = mPrvSvc.getSettingsSafe(uid);
                    if (PrivacySettings.getOutcome(pSet.getBookmarksSetting()) != IPrivacySettings.REAL) {
                        output_label = "[empty]";
                        output = new PrivacyCursor();
                    }
                    mPrvSvc.notification(uid, pSet.getBookmarksSetting(), IPrivacySettings.DATA_BOOKMARKS, null);
                }
            }
            return output;
        }
        return realCursor;
    }

    private static String arrayToString(String[] array) {
        StringBuffer sb = new StringBuffer();
        if (array != null) for (String bla : array) sb.append("[" + bla + "]");
        else return "";
        return sb.toString();
    }
    /**
     * This method is especially for faking android_id if google wants to read it in their privacy database
     * @author CollegeDev
     * @param uri
     * @param projection
     * @param context
     * @param realCursor
     */
    public static Cursor enforcePrivacyPermission(Uri uri, String[] projection, Context context, Cursor realCursor, boolean google_access) throws RemoteException {
        if (uri != null) {
            String auth = uri.getAuthority();
            String output_label = "[real]";
            Cursor output = realCursor;
            if (auth != null && auth.equals("com.google.android.gsf.gservices")) {
                boolean privacyAllowed = false;
                int uid = context.getApplicationInfo().uid;
                if (mPrvSvc == null) mPrvSvc = PrivacySettingsManager.getPrivacyService(context);
                IPrivacySettings pSet = mPrvSvc.getSettingsSafe(uid);
                if (PrivacySettings.getOutcome(pSet.getSimInfoSetting()) == IPrivacySettings.REAL) {
                    privacyAllowed = true;
                }

                if (privacyAllowed) {
                    PrivacyDebugger.i(TAG,"google is allowed to get real cursor");
                } else {
                    int actual_pos = realCursor.getPosition();
                    int forbidden_position = -1;
                    try{
                        for(int i=0;i<realCursor.getCount();i++){
                            realCursor.moveToNext();
                            if(realCursor.getString(0).equals("android_id")){
                                forbidden_position = realCursor.getPosition();
                                break;
                            }
                        }
                    } catch (Exception e){
                        PrivacyDebugger.e(TAG,"something went wrong while getting blocked permission for android id");
                    } finally{
                        realCursor.moveToPosition(actual_pos);
                        if(forbidden_position == -1) {PrivacyDebugger.i(TAG,"now we return real cursor, because forbidden_pos is -1"); return output;} //give realcursor, because there is no android_id to block
                    }
                    PrivacyDebugger.i(TAG,"now blocking google access to android id and give fake cursor. forbidden_position: " + forbidden_position);
                    output_label = "[fake]";
                    output = new PrivacyCursor(realCursor,forbidden_position);
                }
                mPrvSvc.notification(uid, pSet.getSimInfoSetting(), IPrivacySettings.DATA_NETWORK_INFO_SIM, null);
                
            }
            return output;
        }
        return realCursor;   
    }
}
