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
public final class PrivacySettingsAllow implements IPrivacySettings {

    @Override
    public byte getSwitchWifiStateSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getForceOnlineState() {
        return ERROR_REAL;
    }

    @Override
    public byte getSendMmsSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getSwitchConnectivitySetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getAndroidIdSetting() {
        return ERROR_REAL;
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
        return ERROR_REAL;
    }

    @Override
    public byte getIpTableProtectSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getIccAccessSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getAddOnManagementSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getSmsSendSetting(){
        return ERROR_REAL;
    }

    @Override
    public byte getPhoneCallSetting(){
        return ERROR_REAL;
    }

    @Override
    public byte getRecordAudioSetting(){
        return ERROR_REAL;
    }

    @Override
    public byte getCameraSetting(){
        return ERROR_REAL;
    }

    @Override
    public byte getDeviceIdSetting() {
        return ERROR_REAL;
    }

    @Override
    public String getDeviceId() {
        return "";
    }

    @Override
    public byte getLine1NumberSetting() {
        return ERROR_REAL;
    }

    @Override
    public String getLine1Number() {
        return "";
    }

    @Override
    public byte getLocationGpsSetting() {
        return ERROR_REAL;
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
        return ERROR_REAL;
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
        return ERROR_REAL;
    }

    @Override
    public byte getSimInfoSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getSimSerialNumberSetting() {
        return ERROR_REAL;
    }

    @Override
    public String getSimSerialNumber() {
        return "";
    }

    @Override
    public byte getSubscriberIdSetting() {
        return ERROR_REAL;
    }

    @Override
    public String getSubscriberId() {
        return "";
    }

    @Override
    public byte getAccountsSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getAccountsAuthTokensSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getOutgoingCallsSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getIncomingCallsSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getContactsSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getCalendarSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getMmsSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getSmsSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getCallLogSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getBookmarksSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getSystemLogsSetting() {
        return ERROR_REAL;
    }

    @Override
    public byte getIntentBootCompletedSetting() {
        return ERROR_REAL;
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
