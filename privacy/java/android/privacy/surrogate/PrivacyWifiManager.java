/**
 * Copyright (C) 2012 Stefan Thiele
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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.IWifiManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.ServiceManager;
import android.privacy.IPrivacySettingsManager;
import android.privacy.PrivacyServiceException;
import android.privacy.IPrivacySettings;
import android.privacy.PrivacySettings;
import android.privacy.PrivacySettingsManager;
import android.util.Log;

/**
 * Provides privacy handling for WifiManager
 * @author CollegeDev
 * {@hide}
 */
public class PrivacyWifiManager extends WifiManager{

    private Context context;
    private PrivacySettingsManager pSetMan;
    private static final String P_TAG = "PrivacyWifiManager";

    private enum PrivacyOutcome { REAL, EMPTY, CUSTOM, RANDOM, ERROR_REAL, ERROR_EMPTY, DEFAULT_REAL, FORCE_ONLINE };


    public PrivacyWifiManager(Context context, IWifiManager service){
        super(context, service);
        this.context = context;
        pSetMan = PrivacySettingsManager.getPrivacyService();
    }

    private PrivacyOutcome getPrivacyOutcome(boolean withForceState) {
        if (pSetMan == null) pSetMan = PrivacySettingsManager.getPrivacyService();
        IPrivacySettings settings = pSetMan.getSettingsSafe(context.getPackageName());
        if (withForceState && settings != null && settings.getForceOnlineState() == IPrivacySettings.REAL) {
            return PrivacyOutcome.FORCE_ONLINE;
        } else if (settings == null) {
            return PrivacyOutcome.REAL;
        } else {
            switch (settings.getWifiInfoSetting()) {
            case IPrivacySettings.REAL:
                return PrivacyOutcome.REAL;
            case IPrivacySettings.EMPTY:
                return PrivacyOutcome.EMPTY;
            case IPrivacySettings.CUSTOM:
                return PrivacyOutcome.CUSTOM;
            case IPrivacySettings.RANDOM:
                return PrivacyOutcome.RANDOM;
            case IPrivacySettings.ERROR_REAL:
                return PrivacyOutcome.ERROR_REAL;
            case IPrivacySettings.DEFAULT_REAL:
                return PrivacyOutcome.DEFAULT_REAL;
            default:
                return PrivacyOutcome.ERROR_EMPTY;
            }
        }
    }

    @Override
    public List<WifiConfiguration> getConfiguredNetworks() {
        switch (getPrivacyOutcome(false)) {
        case REAL:
        case ERROR_REAL:
        case DEFAULT_REAL:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
            return super.getConfiguredNetworks();
        default:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);
            return new ArrayList<WifiConfiguration>(); //create empty list!
        }
    }

    @Override
    public WifiInfo getConnectionInfo() {
        switch (getPrivacyOutcome(false)) {
        case REAL:
        case ERROR_REAL:
        case DEFAULT_REAL:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
            return super.getConnectionInfo();
        default:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
            return new WifiInfo(true);
        }
    }

    @Override
    public List<ScanResult> getScanResults() {
        switch (getPrivacyOutcome(false)) {
        case REAL:
        case ERROR_REAL:
        case DEFAULT_REAL:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
            return super.getScanResults();
        default:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
            return new ArrayList<ScanResult>(); //create empty list!
        }
    }


    @Override
    public int getFrequencyBand() {
        switch (getPrivacyOutcome(false)) {
        case REAL:
        case ERROR_REAL:
        case DEFAULT_REAL:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
            return super.getFrequencyBand();
        default:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
            return -1;
        }
    }


    @Override
    public DhcpInfo getDhcpInfo() {
        switch (getPrivacyOutcome(false)) {
        case REAL:
        case ERROR_REAL:
        case DEFAULT_REAL:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
            return super.getDhcpInfo();
        default:
            pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
            return new DhcpInfo();
        }
    }

    /**
     * @hide
     * @return
     */
     @Override
     public WifiConfiguration getWifiApConfiguration() {
         switch (getPrivacyOutcome(false)) {
         case REAL:
         case ERROR_REAL:
         case DEFAULT_REAL:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
             return super.getWifiApConfiguration();
         default:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
             return new WifiConfiguration();
         }
     }


     @Override
     public String getConfigFile() {
         switch (getPrivacyOutcome(false)) {
         case REAL:
         case ERROR_REAL:
         case DEFAULT_REAL:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
             return super.getConfigFile();
         default:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
             return "";
         }
     }


     //new
     @Override
     public boolean startScan(){
         switch (getPrivacyOutcome(false)) {
         case REAL:
         case ERROR_REAL:
         case DEFAULT_REAL:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
             return super.startScan();
         default:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
             return false;
         }
     }


     @Override
     public boolean startScanActive(){
         switch (getPrivacyOutcome(false)) {
         case REAL:
         case ERROR_REAL:
         case DEFAULT_REAL:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null); 
             return super.startScanActive();
         default:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
             return false;
         }
     }


     @Override
     public boolean setWifiEnabled(boolean enabled){
         switch (getPrivacyOutcome(false)) {
         case REAL:
         case ERROR_REAL:
         case DEFAULT_REAL:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_SWITCH_WIFI_STATE, null); 
             return super.setWifiEnabled(enabled);
         default:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_SWITCH_WIFI_STATE, null);  
             return false;		        
         }
     }

     @Override
     public int getWifiState(){
         switch (getPrivacyOutcome(true)) {
         case FORCE_ONLINE:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
             return WIFI_STATE_ENABLED;
         case REAL:
         case ERROR_REAL:
         case DEFAULT_REAL:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null);  
             return super.getWifiState();
         default:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
             return WIFI_STATE_UNKNOWN;
         }

     }

     @Override
     public boolean isWifiEnabled(){
         switch (getPrivacyOutcome(true)) {
         case FORCE_ONLINE:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
             return true;	            
         case REAL:
         case ERROR_REAL:
         case DEFAULT_REAL:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.REAL, IPrivacySettings.DATA_WIFI_INFO, null);  
             return super.isWifiEnabled();
         default:
             pSetMan.notification(context.getPackageName(), IPrivacySettings.EMPTY, IPrivacySettings.DATA_WIFI_INFO, null);  
             return false;
         }

     }
}
