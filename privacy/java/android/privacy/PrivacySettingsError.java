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
public final class PrivacySettingsError implements IPrivacySettings {

    private byte mResult;
    
    public PrivacySettingsError(byte type) {
        switch (type) {
            case IPrivacySettings.REAL:
                this.mResult = IPrivacySettings.ERROR_REAL;
                break;
            case IPrivacySettings.CUSTOM:
            case IPrivacySettings.RANDOM:
            case IPrivacySettings.EMPTY:
                this.mResult = IPrivacySettings.ERROR_EMPTY;
        }
    }
    
    @Override
    public byte getSwitchWifiStateSetting() {
        return mResult;
    }

    @Override
    public byte getForceOnlineState() {
        return mResult;
    }

    @Override
    public byte getSendMmsSetting() {
        return mResult;
    }

    @Override
    public byte getSwitchConnectivitySetting() {
        return mResult;
    }

    @Override
    public byte getAndroidIdSetting() {
        return mResult;
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
        return mResult;
    }

    @Override
    public byte getIpTableProtectSetting() {
        return mResult;
    }

    @Override
    public byte getIccAccessSetting() {
        return mResult;
    }

    @Override
    public byte getAddOnManagementSetting() {
        return mResult;
    }

    @Override
    public byte getSmsSendSetting(){
        return mResult;
    }

    @Override
    public byte getPhoneCallSetting(){
        return mResult;
    }

    @Override
    public byte getRecordAudioSetting(){
        return mResult;
    }

    @Override
    public byte getCameraSetting(){
        return mResult;
    }

    @Override
    public byte getDeviceIdSetting() {
        return mResult;
    }

    @Override
    public String getDeviceId() {
        return "";
    }

    @Override
    public byte getLine1NumberSetting() {
        return mResult;
    }

    @Override
    public String getLine1Number() {
        return "";
    }

    @Override
    public byte getLocationGpsSetting() {
        return mResult;
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
        return mResult;
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
        return mResult;
    }

    @Override
    public byte getSimInfoSetting() {
        return mResult;
    }

    @Override
    public byte getSimSerialNumberSetting() {
        return mResult;
    }

    @Override
    public String getSimSerialNumber() {
        return "";
    }

    @Override
    public byte getSubscriberIdSetting() {
        return mResult;
    }

    @Override
    public String getSubscriberId() {
        return "";
    }

    @Override
    public byte getAccountsSetting() {
        return mResult;
    }

    @Override
    public byte getAccountsAuthTokensSetting() {
        return mResult;
    }

    @Override
    public byte getOutgoingCallsSetting() {
        return mResult;
    }

    @Override
    public byte getIncomingCallsSetting() {
        return mResult;
    }

    @Override
    public byte getContactsSetting() {
        return mResult;
    }

    @Override
    public byte getCalendarSetting() {
        return mResult;
    }

    @Override
    public byte getMmsSetting() {
        return mResult;
    }

    @Override
    public byte getSmsSetting() {
        return mResult;
    }

    @Override
    public byte getCallLogSetting() {
        return mResult;
    }

    @Override
    public byte getBookmarksSetting() {
        return mResult;
    }

    @Override
    public byte getSystemLogsSetting() {
        return mResult;
    }

    @Override
    public byte getIntentBootCompletedSetting() {
        return mResult;
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
