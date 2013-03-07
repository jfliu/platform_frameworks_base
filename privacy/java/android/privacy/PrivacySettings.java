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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * Holds privacy settings for access to all private data types for a single application
 * @author Svyatoslav Hresyk
 * @author Simeon Morgan <smorgan@digitalfeed.net>
 * @author Stefan Thiele (CollegeDev)
 * {@hide} 
 */
public final class PrivacySettings implements IPrivacySettings, Parcelable {


     // Database entry ID
    private final Integer _id;

    // Application identifiers
    private String packageName;
    private int uid;

    //
    // Privacy settings
    //

    private byte deviceIdSetting;
    private String deviceId;

    // Phone and Voice Mailbox Number
    private byte line1NumberSetting; 
    private String line1Number;

    private byte locationGpsSetting;
    private String locationGpsLat;
    private String locationGpsLon;
    private byte locationNetworkSetting;
    private String locationNetworkLat;
    private String locationNetworkLon;

    // CountryIso, Operator Code, Operator Name
    private byte networkInfoSetting;
    private byte simInfoSetting;

    private byte simSerialNumberSetting;
    private String simSerialNumber;
    private byte subscriberIdSetting;
    private String subscriberId;

    private byte accountsSetting;
    private byte accountsAuthTokensSetting;
    private byte outgoingCallsSetting;
    private byte incomingCallsSetting;

    private byte contactsSetting;
    private byte calendarSetting;
    private byte mmsSetting;
    private byte smsSetting;
    private byte callLogSetting;
    private byte bookmarksSetting; // browser bookmarks and history

    private byte systemLogsSetting;

    private byte notificationSetting;

    private byte intentBootCompletedSetting;
    //    private byte externalStorageSetting;
    private byte cameraSetting;
    private byte recordAudioSetting;
    private byte smsSendSetting;
    private byte phoneCallSetting;

    private byte ipTableProtectSetting;
    private byte iccAccessSetting;
    private byte addOnManagementSetting;

    private byte androidIdSetting;
    private String androidID;

    private byte wifiInfoSetting;

    private byte switchConnectivitySetting;

    private byte sendMmsSetting;

    private byte forceOnlineState; //used to fake online state

    private byte switchWifiStateSetting;


    private int[] allowedContacts;

    /**
     * Constructor to set all Values REAL
     * @param _id id in database
     * @param packageName	packagename of the app
     * @param uid uid of application
     * {@hide}
     */
    public PrivacySettings(Integer _id, String packageName, int uid) {
        this._id = _id;

        this.packageName = packageName;
        this.uid = uid;

        this.deviceIdSetting = REAL;
        this.deviceId = null;
        this.line1NumberSetting = REAL;
        this.line1Number = null;
        this.locationGpsSetting = REAL;
        this.locationGpsLat = null;
        this.locationGpsLon = null;
        this.locationNetworkSetting = REAL;
        this.locationNetworkLat = null;
        this.locationNetworkLon = null;
        this.networkInfoSetting = REAL;
        this.simInfoSetting = REAL;
        this.simSerialNumberSetting = REAL;
        this.simSerialNumber = null;
        this.subscriberIdSetting = REAL;
        this.subscriberId = null;
        this.accountsSetting = REAL;
        this.accountsAuthTokensSetting = REAL;
        this.outgoingCallsSetting = REAL;
        this.incomingCallsSetting = REAL;
        this.contactsSetting = REAL;
        this.calendarSetting = REAL;
        this.mmsSetting = REAL;
        this.smsSetting = REAL;
        this.callLogSetting = REAL;
        this.bookmarksSetting = REAL;
        this.systemLogsSetting = REAL;
        this.notificationSetting = SETTING_NOTIFY_OFF;
        this.intentBootCompletedSetting = REAL;
        //        this.externalStorageSetting = REAL;
        this.cameraSetting = REAL; 
        this.recordAudioSetting = REAL;
        this.allowedContacts = null;
        this.smsSendSetting = REAL;
        this.phoneCallSetting = REAL;
        this.ipTableProtectSetting = REAL;
        this.iccAccessSetting = REAL;
        this.addOnManagementSetting = EMPTY;
        this.androidIdSetting = REAL;
        this.androidID = null;
        this.wifiInfoSetting = REAL;
        this.switchConnectivitySetting = REAL;
        this.sendMmsSetting = REAL;
        this.forceOnlineState = EMPTY;
        this.switchWifiStateSetting = REAL;
    }

    /**
     * Constructor for two possibilities:<br>
     * 1. pass allEmpty = true for set all values to empty
     * 2. pass allEmpty = false for set all possible values to RANDOM 
     * @param _id id in database
     * @param packageName packagename of application
     * @param uid the uid of application
     * @param allEmpty see description above
     * {@hide}
     */
    public PrivacySettings(Integer _id, String packageName, int uid, boolean allEmpty) {
        this._id = _id;

        this.packageName = packageName;
        this.uid = uid;
        if(allEmpty){
            this.deviceIdSetting = EMPTY;
            this.deviceId = null;
            this.line1NumberSetting = EMPTY;
            this.line1Number = null;
            this.locationGpsSetting = EMPTY;
            this.locationGpsLat = null;
            this.locationGpsLon = null;
            this.locationNetworkSetting = EMPTY;
            this.locationNetworkLat = null;
            this.locationNetworkLon = null;
            this.networkInfoSetting = EMPTY;
            this.simInfoSetting = EMPTY;
            this.simSerialNumberSetting = EMPTY;
            this.simSerialNumber = null;
            this.subscriberIdSetting = EMPTY;
            this.subscriberId = null;
            this.accountsSetting = EMPTY;
            this.accountsAuthTokensSetting = EMPTY;
            this.outgoingCallsSetting = EMPTY;
            this.incomingCallsSetting = EMPTY;
            this.contactsSetting = EMPTY;
            this.calendarSetting = EMPTY;
            this.mmsSetting = EMPTY;
            this.smsSetting = EMPTY;
            this.callLogSetting = EMPTY;
            this.bookmarksSetting = EMPTY;
            this.systemLogsSetting = EMPTY;
            this.notificationSetting = SETTING_NOTIFY_OFF;
            this.intentBootCompletedSetting = EMPTY;
            //        this.externalStorageSetting = REAL;
            this.cameraSetting = EMPTY;
            this.recordAudioSetting = EMPTY;
            this.allowedContacts = null;
            this.smsSendSetting = EMPTY;
            this.phoneCallSetting = EMPTY;
            this.ipTableProtectSetting = EMPTY;
            this.iccAccessSetting = EMPTY;
            this.addOnManagementSetting = EMPTY;
            this.androidIdSetting = EMPTY;
            this.androidID = null;
            this.wifiInfoSetting = EMPTY;
            this.switchConnectivitySetting = EMPTY;
            this.sendMmsSetting = EMPTY;
            this.forceOnlineState = REAL;
            this.switchWifiStateSetting = EMPTY;
        } else {
            this.deviceIdSetting = RANDOM;
            this.deviceId = null;
            this.line1NumberSetting = RANDOM;
            this.line1Number = null;
            this.locationGpsSetting = RANDOM;
            this.locationGpsLat = null;
            this.locationGpsLon = null;
            this.locationNetworkSetting = RANDOM;
            this.locationNetworkLat = null;
            this.locationNetworkLon = null;
            this.networkInfoSetting = EMPTY;
            this.simInfoSetting = EMPTY;
            this.simSerialNumberSetting = RANDOM;
            this.simSerialNumber = null;
            this.subscriberIdSetting = RANDOM;
            this.subscriberId = null;
            this.accountsSetting = EMPTY;
            this.accountsAuthTokensSetting = EMPTY;
            this.outgoingCallsSetting = EMPTY;
            this.incomingCallsSetting = EMPTY;
            this.contactsSetting = EMPTY;
            this.calendarSetting = EMPTY;
            this.mmsSetting = EMPTY;
            this.smsSetting = EMPTY;
            this.callLogSetting = EMPTY;
            this.bookmarksSetting = EMPTY;
            this.systemLogsSetting = EMPTY;
            this.notificationSetting = SETTING_NOTIFY_OFF;
            this.intentBootCompletedSetting = EMPTY;
            //        this.externalStorageSetting = REAL;
            this.cameraSetting = EMPTY;
            this.recordAudioSetting = EMPTY;
            this.allowedContacts = null;
            this.smsSendSetting = EMPTY;
            this.phoneCallSetting = EMPTY;
            this.ipTableProtectSetting = EMPTY;
            this.iccAccessSetting = EMPTY;
            this.addOnManagementSetting = EMPTY;
            this.androidIdSetting = RANDOM;
            this.androidID = null;
            this.wifiInfoSetting = EMPTY;
            this.switchConnectivitySetting = EMPTY;
            this.sendMmsSetting = EMPTY;
            this.forceOnlineState = REAL;
            this.switchWifiStateSetting = EMPTY;
        }
    }


    public PrivacySettings(Integer id, String packageName, int uid, byte deviceIdSetting, String deviceId,
            byte line1NumberSetting, String line1Number, byte locationGpsSetting, String locationGpsLat,
            String locationGpsLon, byte locationNetworkSetting, String locationNetworkLat, 
            String locationNetworkLon, byte networkInfoSetting, byte simInfoSetting, byte simSerialNumberSetting,
            String simSerialNumber, byte subscriberIdSetting, String subscriberId, byte accountsSetting, 
            byte accountsAuthTokensSetting, byte outgoingCallsSetting, byte incomingCallsSetting, byte contactsSetting,
            byte calendarSetting, byte mmsSetting, byte smsSetting, byte callLogSetting, byte bookmarksSetting, 
            byte systemLogsSetting, byte externalStorageSetting, byte cameraSetting, byte recordAudioSetting, 
            byte notificationSetting, byte intentBootCompletedSetting, int[] allowedContacts, byte smsSendSetting, byte phoneCallSetting, byte ipTableProtectSetting,
            byte iccAccessSetting, byte addOnManagementSetting, byte androidIdSetting, String androidID, byte wifiInfoSetting, byte switchConnectivitySetting, byte sendMmsSetting,
            byte forceOnlineState, byte switchWifiStateSetting) {
        this._id = id;

        this.packageName = packageName;
        this.uid = uid;

        this.deviceIdSetting = deviceIdSetting;
        this.deviceId = deviceId;
        this.line1NumberSetting = line1NumberSetting;
        this.line1Number = line1Number;
        this.locationGpsSetting = locationGpsSetting;
        this.locationGpsLat = locationGpsLat;
        this.locationGpsLon = locationGpsLon;
        this.locationNetworkSetting = locationNetworkSetting;
        this.locationNetworkLat = locationNetworkLat;
        this.locationNetworkLon = locationNetworkLon;
        this.networkInfoSetting = networkInfoSetting;
        this.simInfoSetting = simInfoSetting;
        this.simSerialNumberSetting = simSerialNumberSetting;
        this.simSerialNumber = simSerialNumber;
        this.subscriberIdSetting = subscriberIdSetting;
        this.subscriberId = subscriberId;
        this.accountsSetting = accountsSetting;
        this.accountsAuthTokensSetting = accountsAuthTokensSetting;
        this.outgoingCallsSetting = outgoingCallsSetting;
        this.incomingCallsSetting = incomingCallsSetting;
        this.contactsSetting = contactsSetting;
        this.calendarSetting = calendarSetting;
        this.mmsSetting = mmsSetting;
        this.smsSetting = smsSetting;
        this.callLogSetting = callLogSetting;
        this.bookmarksSetting = bookmarksSetting;
        this.systemLogsSetting = systemLogsSetting;
        this.notificationSetting = notificationSetting;
        this.intentBootCompletedSetting = intentBootCompletedSetting;
        //        this.externalStorageSetting = externalStorageSetting;
        this.cameraSetting = cameraSetting;
        this.recordAudioSetting = recordAudioSetting;
        this.allowedContacts = allowedContacts;
        this.smsSendSetting = smsSendSetting;
        this.phoneCallSetting = phoneCallSetting;
        this.ipTableProtectSetting = ipTableProtectSetting;
        this.iccAccessSetting = iccAccessSetting;
        this.addOnManagementSetting = addOnManagementSetting;
        this.androidIdSetting = androidIdSetting;
        this.androidID = androidID;
        this.wifiInfoSetting = wifiInfoSetting;
        this.switchConnectivitySetting = switchConnectivitySetting;
        this.sendMmsSetting = sendMmsSetting;
        this.forceOnlineState = forceOnlineState;
        this.switchWifiStateSetting = switchWifiStateSetting;
    }

    @Override
    public byte getSwitchWifiStateSetting() {
        return switchWifiStateSetting;
    }

    public void setSwitchWifiStateSetting(byte switchWifiStateSetting) {
        this.switchWifiStateSetting = switchWifiStateSetting;
    }

    @Override
    public byte getForceOnlineState() {
        return forceOnlineState;
    }

    public void setForceOnlineState(byte forceOnlineState) {
        this.forceOnlineState = forceOnlineState;
    }

    @Override
    public byte getSendMmsSetting() {
        return sendMmsSetting;
    }

    public void setSendMmsSetting(byte sendMmsSetting) {
        this.sendMmsSetting = sendMmsSetting;
    }

    @Override
    public byte getSwitchConnectivitySetting() {
        return switchConnectivitySetting;
    }

    public void setSwitchConnectivitySetting(byte switchConnectivitySetting) {
        this.switchConnectivitySetting = switchConnectivitySetting;
    }

    @Override
    public byte getAndroidIdSetting() {
        return androidIdSetting;
    }

    public void setAndroidIdSetting(byte androidIdSetting) {
        this.androidIdSetting = androidIdSetting;
    }

    /**
     * @return random ID, constant fake id or null
     */
    @Override
    public String getAndroidID() {
        if(androidIdSetting == EMPTY) return "q4a5w896ay21dr46"; //we can not pull out empty android id, because we get bootlops then
        if(androidIdSetting == RANDOM) {
            Random value = new Random();
            StringBuilder localBuilder = new StringBuilder();
            for(int i = 0; i < ID_PATTERN.length; i++)
                localBuilder.append(ID_PATTERN[value.nextInt(ID_PATTERN.length-1)]);
            return localBuilder.toString();
        }
        return androidID;
    }

    @Override
    public byte getWifiInfoSetting() {
        return wifiInfoSetting;
    }

    public void setWifiInfoSetting(byte wifiInfoSetting) {
        this.wifiInfoSetting = wifiInfoSetting;
    }

    public void setAndroidID(String androidID) {
        this.androidID = androidID;
    }

    @Override
    public byte getIpTableProtectSetting() {
        return ipTableProtectSetting;
    }

    public void setIpTableProtectSetting(byte ipTableProtectSetting) {
        this.ipTableProtectSetting = ipTableProtectSetting;
    }

    @Override
    public byte getIccAccessSetting() {
        return iccAccessSetting;
    }

    public void setIccAccessSetting(byte iccAccessSetting) {
        this.iccAccessSetting = iccAccessSetting;
    }

    @Override
    public byte getAddOnManagementSetting() {
        return addOnManagementSetting;
    }

    public void setAddOnManagementSetting(byte addOnManagementSetting) {
        this.addOnManagementSetting = addOnManagementSetting;
    }

    @Override
    public byte getSmsSendSetting(){
        return smsSendSetting;
    }

    public void setSmsSendSetting(byte smsSendSetting){
        this.smsSendSetting = smsSendSetting;
    }

    @Override
    public byte getPhoneCallSetting(){
        return phoneCallSetting;
    }

    public void setPhoneCallSetting(byte phoneCallSetting){
        this.phoneCallSetting = phoneCallSetting;
    }

    @Override
    public byte getRecordAudioSetting(){
        return recordAudioSetting;
    }

    public void setRecordAudioSetting(byte recordAudioSetting){
        this.recordAudioSetting = recordAudioSetting;
    }

    @Override
    public byte getCameraSetting(){
        return cameraSetting;
    }

    public void setCameraSetting(byte cameraSetting){
        this.cameraSetting = cameraSetting;
    }

    public Integer get_id() {
        return _id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    @Override
    public byte getDeviceIdSetting() {
        return deviceIdSetting;
    }

    public void setDeviceIdSetting(byte deviceIdSetting) {
        this.deviceIdSetting = deviceIdSetting;
    }

    @Override
    public String getDeviceId() {
        if (deviceIdSetting == EMPTY) return "";
        if (deviceIdSetting == RANDOM) {
            Random rnd = new Random();
            String rndId = Math.abs(rnd.nextLong()) + "";
            if(rndId.length() > 15)
                return rndId.substring(0, 15);
            else{
                for(int i = rndId.length(); i <= 16; i++)
                    rndId += rnd.nextInt(9);
                return rndId.substring(0, 15);
            }
            //return rndId.substring(0, 15);
        }
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public byte getLine1NumberSetting() {
        return line1NumberSetting;
    }

    public void setLine1NumberSetting(byte line1NumberSetting) {
        this.line1NumberSetting = line1NumberSetting;
    }

    @Override
    public String getLine1Number() {
        if (line1NumberSetting == EMPTY) return "";
        if (line1NumberSetting == RANDOM) {
            Random rnd = new Random();
            String rndId = "+" + Math.abs(rnd.nextLong()) + "";
            if(rndId.length() > 13)
                return rndId.substring(0, 13);
            else{
                for(int i = rndId.length(); i <= 14; i++)
                    rndId += rnd.nextInt(9);
                return rndId.substring(0, 13);
            }
            //return rndId.substring(0, 13);
        }
        return line1Number;
    }

    public void setLine1Number(String line1Number) {
        this.line1Number = line1Number;
    }

    @Override
    public byte getLocationGpsSetting() {
        return locationGpsSetting;
    }

    public void setLocationGpsSetting(byte locationGpsSetting) {
        this.locationGpsSetting = locationGpsSetting;
    }

    @Override
    public String getLocationGpsLat() {
        if (locationGpsSetting == EMPTY) return "";
        if (locationGpsSetting == RANDOM) return getRandomLat();
        return locationGpsLat;
    }

    public void setLocationGpsLat(String locationGpsLat) {
        this.locationGpsLat = locationGpsLat;
    }

    @Override
    public String getLocationGpsLon() {
        if (locationGpsSetting == EMPTY) return "";        
        if (locationGpsSetting == RANDOM) return getRandomLon();
        return locationGpsLon;
    }

    public void setLocationGpsLon(String locationGpsLon) {
        this.locationGpsLon = locationGpsLon;
    }

    @Override
    public byte getLocationNetworkSetting() {
        return locationNetworkSetting;
    }

    public void setLocationNetworkSetting(byte locationNetworkSetting) {
        this.locationNetworkSetting = locationNetworkSetting;
    }

    @Override
    public String getLocationNetworkLat() {
        if (locationNetworkSetting == EMPTY) return "";
        if (locationNetworkSetting == RANDOM) return getRandomLat();  
        return locationNetworkLat;
    }

    public void setLocationNetworkLat(String locationNetworkLat) {
        this.locationNetworkLat = locationNetworkLat;
    }

    @Override
    public String getLocationNetworkLon() {
        if (locationNetworkSetting == EMPTY) return "";
        if (locationNetworkSetting == RANDOM) return getRandomLon();
        return locationNetworkLon;
    }

    public void setLocationNetworkLon(String locationNetworkLon) {
        this.locationNetworkLon = locationNetworkLon;
    }

    @Override
    public byte getNetworkInfoSetting() {
        return networkInfoSetting;
    }

    public void setNetworkInfoSetting(byte networkInfoSetting) {
        this.networkInfoSetting = networkInfoSetting;
    }

    @Override
    public byte getSimInfoSetting() {
        return simInfoSetting;
    }

    public void setSimInfoSetting(byte simInfoSetting) {
        this.simInfoSetting = simInfoSetting;
    }

    @Override
    public byte getSimSerialNumberSetting() {
        return simSerialNumberSetting;
    }

    public void setSimSerialNumberSetting(byte simSerialNumberSetting) {
        this.simSerialNumberSetting = simSerialNumberSetting;
    }

    @Override
    public String getSimSerialNumber() {
        if (simSerialNumberSetting == EMPTY) return "";
        if (simSerialNumberSetting == RANDOM) {
            Random rnd = new Random();
            return Math.abs(rnd.nextLong()) + "";
        }
        return simSerialNumber;
    }

    public void setSimSerialNumber(String simSerialNumber) {
        this.simSerialNumber = simSerialNumber;
    }

    @Override
    public byte getSubscriberIdSetting() {
        return subscriberIdSetting;
    }

    public void setSubscriberIdSetting(byte subscriberIdSetting) {
        this.subscriberIdSetting = subscriberIdSetting;
    }

    @Override
    public String getSubscriberId() {
        if (subscriberIdSetting == EMPTY) return "";
        if (subscriberIdSetting == RANDOM) {
            Random rnd = new Random();
            String rndId = Math.abs(rnd.nextLong()) + "";
            if(rndId.length() > 15)
                return rndId.substring(0, 15);
            else{
                for(int i = rndId.length(); i <= 16; i++)
                    rndId += rnd.nextInt(9);
                return rndId.substring(0, 15);
            }
        }
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    @Override
    public byte getAccountsSetting() {
        return accountsSetting;
    }

    public void setAccountsSetting(byte accountsSetting) {
        this.accountsSetting = accountsSetting;
    }

    @Override
    public byte getAccountsAuthTokensSetting() {
        return accountsAuthTokensSetting;
    }

    public void setAccountsAuthTokensSetting(byte accountsAuthTokensSetting) {
        this.accountsAuthTokensSetting = accountsAuthTokensSetting;
    }

    @Override
    public byte getOutgoingCallsSetting() {
        return outgoingCallsSetting;
    }

    public void setOutgoingCallsSetting(byte outgoingCallsSetting) {
        this.outgoingCallsSetting = outgoingCallsSetting;
    }

    @Override
    public byte getIncomingCallsSetting() {
        return incomingCallsSetting;
    }

    public void setIncomingCallsSetting(byte incomingCallsSetting) {
        this.incomingCallsSetting = incomingCallsSetting;
    }

    @Override
    public byte getContactsSetting() {
        return contactsSetting;
    }

    public void setContactsSetting(byte contactsSetting) {
        this.contactsSetting = contactsSetting;
    }

    @Override
    public byte getCalendarSetting() {
        return calendarSetting;
    }

    public void setCalendarSetting(byte calendarSetting) {
        this.calendarSetting = calendarSetting;
    }

    @Override
    public byte getMmsSetting() {
        return mmsSetting;
    }

    public void setMmsSetting(byte mmsSetting) {
        this.mmsSetting = mmsSetting;
    }

    @Override
    public byte getSmsSetting() {
        return smsSetting;
    }

    public void setSmsSetting(byte smsSetting) {
        this.smsSetting = smsSetting;
    }

    @Override
    public byte getCallLogSetting() {
        return callLogSetting;
    }

    public void setCallLogSetting(byte callLogSetting) {
        this.callLogSetting = callLogSetting;
    }

    @Override
    public byte getBookmarksSetting() {
        return bookmarksSetting;
    }

    public void setBookmarksSetting(byte bookmarksSetting) {
        this.bookmarksSetting = bookmarksSetting;
    }

    @Override
    public byte getSystemLogsSetting() {
        return systemLogsSetting;
    }

    public void setSystemLogsSetting(byte systemLogsSetting) {
        this.systemLogsSetting = systemLogsSetting;
    }

    @Override
    public byte getIntentBootCompletedSetting() {
        return intentBootCompletedSetting;
    }

    public void setIntentBootCompletedSetting(byte intentBootCompletedSetting) {
        this.intentBootCompletedSetting = intentBootCompletedSetting;
    }

    @Override
    public byte getNotificationSetting() {
        return notificationSetting;
    }

    public void setNotificationSetting(byte notificationSetting) {
        this.notificationSetting = notificationSetting;
    }

    @Override
    public int[] getAllowedContacts() {
        return allowedContacts;
    }

    public void setAllowedContacts(int[] allowedContacts) {
        this.allowedContacts = allowedContacts;
    }
    
    /**
     * Converts the full range of setting responses (including error_real, etc) to the smaller range of outcomes (e.g. error_real -> real)
     * @param setting
     * @return
     */
    public static byte getOutcome(byte setting) {
        switch (setting) {
        case ERROR_REAL:
        case DEFAULT_REAL:
            return REAL;
        case ERROR_EMPTY:
            return EMPTY;
        default:
            return setting;
        }
    }
    

    @Override
    public String toString() {
        return "PrivacySettings [_id=" + _id + ", accountsAuthTokensSetting=" + accountsAuthTokensSetting
                + ", accountsSetting=" + accountsSetting + ", bookmarksSetting=" + bookmarksSetting
                + ", calendarSetting=" + calendarSetting + ", callLogSetting=" + callLogSetting + ", contactsSetting="
                + contactsSetting + ", deviceId=" + deviceId + ", deviceIdSetting=" + deviceIdSetting
                + ", incomingCallsSetting=" + incomingCallsSetting + ", intentBootCompletedSetting="
                + intentBootCompletedSetting + ", line1Number=" + line1Number + ", line1NumberSetting="
                + line1NumberSetting + ", locationGpsLat=" + locationGpsLat + ", locationGpsLon=" + locationGpsLon
                + ", locationGpsSetting=" + locationGpsSetting + ", locationNetworkLat=" + locationNetworkLat
                + ", locationNetworkLon=" + locationNetworkLon + ", locationNetworkSetting=" + locationNetworkSetting
                + ", mmsSetting=" + mmsSetting + ", networkInfoSetting=" + networkInfoSetting
                + ", notificationSetting=" + notificationSetting + ", outgoingCallsSetting=" + outgoingCallsSetting
                + ", packageName=" + packageName + ", simInfoSetting=" + simInfoSetting + ", simSerialNumber="
                + simSerialNumber + ", simSerialNumberSetting=" + simSerialNumberSetting + ", smsSetting=" + smsSetting
                + ", subscriberId=" + subscriberId + ", subscriberIdSetting=" + subscriberIdSetting
                + ", systemLogsSetting=" + systemLogsSetting + ", uid=" + uid + ", phoneCallSetting=" + phoneCallSetting 
                + ", smsSendSetting=" + smsSendSetting + ", recordAudioSetting=" + recordAudioSetting + ", cameraSetting=" 
                + cameraSetting + ", ipTableProtectSetting=" + ipTableProtectSetting + ", iccAccessSetting=" + iccAccessSetting 
                + ", addOnManagementSetting=" + addOnManagementSetting + ", android ID=" + androidID + ", androidIdSetting="
                + androidIdSetting + ", wifiInfoSetting=" + wifiInfoSetting + ", switchConnectivitySetting=" + switchConnectivitySetting 
                + ", sendMmsSetting=" + sendMmsSetting + ", forceOnlineState=" + forceOnlineState + ", switchWifiStateSetting=" 
                + switchWifiStateSetting + "]";
    }

    /**
     * Util methods
     */

    private String getRandomLat() {
        BigDecimal latitude;
        double lat = Math.random() * 180;
        if (lat > 90) latitude = new BigDecimal(lat - 90);
        else latitude = new BigDecimal(-lat);
        return latitude.setScale(6, BigDecimal.ROUND_HALF_UP) + "";
    }

    private String getRandomLon() {
        BigDecimal longitude;
        double lon = Math.random() * 360;
        if (lon > 180) longitude = new BigDecimal(lon - 180);
        else longitude = new BigDecimal(-lon);
        return longitude.setScale(6, BigDecimal.ROUND_HALF_UP) + "";
    }

    /**
     * Parcelable implementation
     */

    public static final Parcelable.Creator<PrivacySettings> CREATOR = new
            Parcelable.Creator<PrivacySettings>() {
        public PrivacySettings createFromParcel(Parcel in) {
            return new PrivacySettings(in);
        }

        public PrivacySettings[] newArray(int size) {
            return new PrivacySettings[size];
        }
    };

    public PrivacySettings(Parcel in) {
        int _id = in.readInt();
        this._id = (_id == -1) ? null : _id;

        this.packageName = in.readString();
        this.uid = in.readInt();

        this.deviceIdSetting = in.readByte();
        this.deviceId = in.readString();
        this.line1NumberSetting = in.readByte();
        this.line1Number = in.readString();
        this.locationGpsSetting = in.readByte();
        this.locationGpsLat = in.readString();
        this.locationGpsLon = in.readString();
        this.locationNetworkSetting = in.readByte();
        this.locationNetworkLat = in.readString();
        this.locationNetworkLon = in.readString();
        this.networkInfoSetting = in.readByte();
        this.simInfoSetting = in.readByte();
        this.simSerialNumberSetting = in.readByte();
        this.simSerialNumber = in.readString();
        this.subscriberIdSetting = in.readByte();
        this.subscriberId = in.readString();
        this.accountsSetting = in.readByte();
        this.accountsAuthTokensSetting = in.readByte();
        this.outgoingCallsSetting = in.readByte();
        this.incomingCallsSetting = in.readByte();
        this.contactsSetting = in.readByte();
        this.calendarSetting = in.readByte();
        this.mmsSetting = in.readByte();
        this.smsSetting = in.readByte();
        this.callLogSetting = in.readByte();
        this.bookmarksSetting = in.readByte();
        this.systemLogsSetting = in.readByte();
        this.notificationSetting = in.readByte();
        this.intentBootCompletedSetting = in.readByte();
        //        this.externalStorageSetting = in.readByte();
        this.cameraSetting = in.readByte();
        this.recordAudioSetting = in.readByte();
        //        int[] buffer = in.createIntArray();
        //        if (buffer != null && buffer.length > 0) {
        //            in.readIntArray(buffer);
        //            int count = 0;
        //            for (int i = 0; i < buffer.length; i++) if (buffer[i] != 0) count++; else break;
        //            this.allowedContacts = new int[count];
        //            System.arraycopy(buffer, 0, allowedContacts, 0, count);
        //        } // else it will be null

        this.allowedContacts = in.createIntArray();
        this.smsSendSetting = in.readByte();
        this.phoneCallSetting = in.readByte();
        this.ipTableProtectSetting = in.readByte();
        this.iccAccessSetting = in.readByte();
        this.addOnManagementSetting = in.readByte();
        this.androidIdSetting = in.readByte();
        this.androidID = in.readString();
        this.wifiInfoSetting = in.readByte();
        this.switchConnectivitySetting = in.readByte();
        this.sendMmsSetting = in.readByte();
        this.forceOnlineState = in.readByte();
        this.switchWifiStateSetting = in.readByte();

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt((_id == null) ? -1 : _id);

        dest.writeString(packageName);
        dest.writeInt(uid);

        dest.writeByte(deviceIdSetting);
        dest.writeString(deviceId);
        dest.writeByte(line1NumberSetting);
        dest.writeString(line1Number);
        dest.writeByte(locationGpsSetting);
        dest.writeString(locationGpsLat);
        dest.writeString(locationGpsLon);
        dest.writeByte(locationNetworkSetting);
        dest.writeString(locationNetworkLat);
        dest.writeString(locationNetworkLon);
        dest.writeByte(networkInfoSetting);
        dest.writeByte(simInfoSetting);
        dest.writeByte(simSerialNumberSetting);
        dest.writeString(simSerialNumber);
        dest.writeByte(subscriberIdSetting);
        dest.writeString(subscriberId);
        dest.writeByte(accountsSetting);
        dest.writeByte(accountsAuthTokensSetting);
        dest.writeByte(outgoingCallsSetting);
        dest.writeByte(incomingCallsSetting);
        dest.writeByte(contactsSetting);
        dest.writeByte(calendarSetting);
        dest.writeByte(mmsSetting);
        dest.writeByte(smsSetting);
        dest.writeByte(callLogSetting);
        dest.writeByte(bookmarksSetting);
        dest.writeByte(systemLogsSetting);
        dest.writeByte(notificationSetting);
        dest.writeByte(intentBootCompletedSetting);
        //        dest.writeByte(externalStorageSetting);
        dest.writeByte(cameraSetting);
        dest.writeByte(recordAudioSetting);
        dest.writeIntArray(allowedContacts);
        dest.writeByte(smsSendSetting);
        dest.writeByte(phoneCallSetting);
        dest.writeByte(ipTableProtectSetting);
        dest.writeByte(iccAccessSetting);
        dest.writeByte(addOnManagementSetting);
        dest.writeByte(androidIdSetting);
        dest.writeString(androidID);
        dest.writeByte(wifiInfoSetting);
        dest.writeByte(switchConnectivitySetting);
        dest.writeByte(sendMmsSetting);
        dest.writeByte(forceOnlineState);
        dest.writeByte(switchWifiStateSetting);
    }

    @Override
    public int describeContents() {
        return 0;
    }


}
