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
public final class PrivacySettingsDefaultAllow implements IPrivacySettings {

    @Override
    public byte getSwitchWifiStateSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getForceOnlineState() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getSendMmsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getSwitchConnectivitySetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getAndroidIdSetting() {
        return DEFAULT_REAL;
    }

    /**
     * @return random ID, constant fake id or null
     */
    @Override
    public String getAndroidID() {
        return "q4a5w896ay21dr46"; //we can not pull out empty android id, because we get bootlops then
    }

    @Override
    public byte getWifiInfoSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getIpTableProtectSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getIccAccessSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getAddOnManagementSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getSmsSendSetting(){
        return DEFAULT_REAL;
    }

    @Override
    public byte getPhoneCallSetting(){
        return DEFAULT_REAL;
    }

    @Override
    public byte getRecordAudioSetting(){
        return DEFAULT_REAL;
    }

    @Override
    public byte getCameraSetting(){
        return DEFAULT_REAL;
    }

    @Override
    public byte getDeviceIdSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public String getDeviceId() {
        return "";
    }

    @Override
    public byte getLine1NumberSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public String getLine1Number() {
        return "";
    }

    @Override
    public byte getLocationGpsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public String getLocationGpsLat() {
        return "";
    }

    @Override
    public String getLocationGpsLon() {
        return "";        
    }

    @Override
    public byte getLocationNetworkSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public String getLocationNetworkLat() {
        return "";
    }

    @Override
    public String getLocationNetworkLon() {
        return "";
    }

    @Override
    public byte getNetworkInfoSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getSimInfoSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getSimSerialNumberSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public String getSimSerialNumber() {
        return "";
    }

    @Override
    public byte getSubscriberIdSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public String getSubscriberId() {
        return "";
    }

    @Override
    public byte getAccountsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getAccountsAuthTokensSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getOutgoingCallsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getIncomingCallsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getContactsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getCalendarSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getMmsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getSmsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getCallLogSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getBookmarksSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getSystemLogsSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getIntentBootCompletedSetting() {
        return DEFAULT_REAL;
    }

    @Override
    public byte getNotificationSetting() {
        return SETTING_NOTIFY_ON;
    }

    @Override
    public int[] getAllowedContacts() {
        return null;
    }
}
