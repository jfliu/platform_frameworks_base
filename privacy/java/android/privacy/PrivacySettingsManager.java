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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.privacy.utilities.PrivacyDebugger;

/**
 * Provides API access to the privacy settings
 * @author Svyatoslav Hresyk
 * @author Simeon Morgan <smorgan@digitalfeed.net>
 * TODO: selective contacts access
 * {@hide}
 */
public final class PrivacySettingsManager {

    private static final String TAG = "PrivacySettingsManager";
    public static final String ACTION_PRIVACY_NOTIFICATION = "com.privacy.pdroid.PRIVACY_NOTIFICATION";
    // SM: to delete: public static final String ACTION_PRIVACY_NOTIFICATION_ADDON = "com.privacy.pdroid.PRIVACY_NOTIFICATION_ADDON";
    // SM: to delete: private static final String SERVICE_CLASS = "android.privacy.IPrivacySettingsManager.Stub.Proxy";

    IPrivacySettingsManager service;
    private WeakReference<Context> contextReference = null;
    //private android.privacy.IPrivacySettingsManager.Stub.Proxy service;

    /**
     * @hide - this should be instantiated through Context.getSystemService
     * @param context
     */
    public PrivacySettingsManager(Context context, IPrivacySettingsManager service) {
        try {
            PrivacyDebugger.d(TAG, "PrivacySettingsManager:PrivacySettingsManager: service is of class: " + service.getClass().getCanonicalName());
        } catch (Exception e) {
            PrivacyDebugger.d(TAG, "PrivacySettingsManager:PrivacySettingsManager: Service passed to the constructor is null", e);
        }
        this.contextReference = new WeakReference<Context>(context);
        this.service = service;
    }

    /**
     * @hide - this should be instantiated through Context.getSystemService
     * @param context
     */
    public PrivacySettingsManager(IPrivacySettingsManager service) {
        try {
            PrivacyDebugger.d(TAG, "PrivacySettingsManager:PrivacySettingsManager: service is of class: " + service.getClass().getCanonicalName());
        } catch (Exception e) {
            PrivacyDebugger.d(TAG, "PrivacySettingsManager:PrivacySettingsManager: service class not known: exception happened", e);
        }
        this.service = service;
    }
    
    
    @Deprecated
    public IPrivacySettings getSettings(String packageName, int uid)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        return getSettings(packageName);
    }
    
    
    public IPrivacySettings getSettingsSafe(String packageName) {
        try {
            return getSettings(packageName);
        } catch (PrivacyServiceException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:getSettingsSafe: Exception occured obtaining settings - resorting to onError object", e);
            return getOnErrorObject(packageName);
        }
    }
    
    public IPrivacySettings getSettings(String packageName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();
        try {
            IPrivacySettings settings = service.getSettings(packageName);
            // Providing the 'default' settings should be in the service itself, but I have it here until I work out a good implementation approach
            if (settings == null) {
                return new PrivacySettingsDefaultAllow();
            } else {
                return settings;
            }
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:getSettings: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }


    public boolean saveSettings(PrivacySettings settings)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();
        try {
            return service.saveSettings(settings);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:saveSettings: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }


    public boolean deleteSettings(String packageName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();
        try {
            return service.deleteSettings(packageName);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:deleteSettings: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }


    @Deprecated
    public boolean deleteSettings(String packageName, int uid)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        return deleteSettings(packageName);
    }

    @Deprecated
    public void notification(String packageName, int uid, byte accessMode, String dataType, String output, PrivacySettings pSet) {
        notification(packageName, accessMode, dataType, output);
    }

    @Deprecated
    public void notification(String packageName, byte accessMode, String dataType, String output, PrivacySettings pSet) {
        notification(packageName, accessMode, dataType, output);
    }

    public void notification(String packageName, byte accessMode, String dataType, String output) {
        try {
            this.connectService();
            service.notification(packageName, accessMode, dataType, output);
        } catch (PrivacyServiceException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:notification: Exception occurred connecting to the remote service", e);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:notification: Exception occurred in the remote privacy service", e);
        }
    }

    public void registerObservers()
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            service.registerObservers();
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:registerObservers: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public void addObserver(String packageName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            service.addObserver(packageName);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:addObserver: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public boolean purgeSettings()
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();
        try {
            return service.purgeSettings();
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:purgeSettings: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    @Deprecated
    public double getVersion() {
        return PrivacySettingsManagerService.API_VERSION;
    }

    public double getApiVersion() {
        return PrivacySettingsManagerService.API_VERSION;
    }

    public double getModVersion() {
        return PrivacySettingsManagerService.MOD_VERSION;
    }

    public String getModDetails() {
        return PrivacySettingsManagerService.MOD_DETAILS;
    }

    public boolean setEnabled(boolean enable)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            return service.setEnabled(enable);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:addObserver: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public boolean setNotificationsEnabled(boolean enable)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            return service.setNotificationsEnabled(enable);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:setNotificationsEnabled: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public void setBootCompleted()
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            service.setBootCompleted();
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:setBootCompleted: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public void setDebugFlagInt(String flagName, int value)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            service.setDebugFlagInt(flagName, value);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:setDebugFlagInt: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public Integer getDebugFlagInt(String flagName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            return service.getDebugFlagInt(flagName);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:getDebugFlagInt: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public void setDebugFlagBool(String flagName, boolean value) throws PrivacyServiceDisconnectedException, PrivacyServiceException {
        this.connectService();

        try {
            service.setDebugFlagBool(flagName, value);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:setDebugFlagBool: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public Boolean getDebugFlagBool(String flagName)
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            return service.getDebugFlagBool(flagName);
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:getDebugFlagBoolInt: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    public Map<String, Integer> getDebugFlags()
            throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException, PrivacyServiceException {
        this.connectService();

        try {
            return service.getDebugFlags();
        } catch (RemoteException e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:getDebugFlags: Exception occurred in the remote privacy service", e);
            throw new PrivacyServiceException("Exception occurred in the remote privacy service", e);
        }
    }

    /**
     * Checks that the 
     * @return true if service class name matches expectations, otherwise false
     */
    public boolean isServiceValid() {
        if (!isServiceAvailable()) return false;

        String serviceClass = this.service.getClass().getCanonicalName();
        if (serviceClass.equals("android.privacy.IPrivacySettingsManager.Stub.Proxy") || serviceClass.equals("android.privacy.PrivacySettingsManagerService")) {
            return true;
        } else {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:isServiceValid:PrivacySettingsManagerService is of an incorrect class (" + service.getClass().getCanonicalName() +")");
            (new Throwable()).printStackTrace();
            return false;
        }
    }

    private void isServiceValidOrThrow() throws PrivacyServiceInvalidException {
        if (!this.isServiceValid()) {
            throw new PrivacyServiceInvalidException();
        }
    }

    /**
     * Checks whether the PrivacySettingsManagerService is available. For some reason,
     * occasionally it appears to be null. In this case it should be initialized again.
     * @return true if service is connected, otherwise false
     */
    public boolean isServiceAvailable() {
        if (service == null) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:isServiceAvailable:PrivacySettingsManagerService is null");
            (new Throwable()).printStackTrace();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks whether the PrivacySettingsManagerService is available. For some reason,
     * occasionally it appears to be null. In this case it should be initialized again.
     */
    private void isServiceAvailableOrThrow() throws PrivacyServiceDisconnectedException {
        if (!this.isServiceAvailable()) {
            throw new PrivacyServiceDisconnectedException();
        }
    }

    private void connectService() throws PrivacyServiceDisconnectedException, PrivacyServiceInvalidException {
        if (!isServiceAvailable() || !isServiceValid()) {
            // It's hard to say whether it is worth accomodating the 'static service' case or not: it shouldn't come up because the static service should be persistent
            if (contextReference != null) {
                //Was initialised with a context: do we still have it?
                Context context = contextReference.get();
                if (context != null) {
                    // Still have it: reconnect: this is horrible, but because the getSystemService for the Privacy service returns
                    // a PrivacySettingsManager, there isn't really a way around it.
                    // (Apart from having something like privacySettingsManager.getPrivacySettingsManager() and have the object return either a whole new PrivacySettings if necessary
                    // or itself if it is still valid...
                    PrivacySettingsManager transientPrivacySettingsManager = (PrivacySettingsManager) context.getSystemService("privacy");
                    this.service = transientPrivacySettingsManager.service;
                    transientPrivacySettingsManager = null;
                } else {
                    //Context has gone dead (been cleaned up). Make a non-static connection.
                    PrivacyDebugger.d(TAG, "PrivacySettingsmanager:connectService:switched from a static to non-static instance of the privacy service");
                    this.service = IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy"));
                }
            } else {
                this.service = IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy"));
            }

            if (this.service == null) {
                throw new PrivacyServiceDisconnectedException("Reconnection failed");
            }
        }
    }

    /**
     * Establish a connection to the Privacy service without using a Context
     * @return PrivacySettingsManager connected to the privacy service
     */
    public static PrivacySettingsManager getPrivacyService() {
        return new PrivacySettingsManager(IPrivacySettingsManager.Stub.asInterface(ServiceManager.getService("privacy"))); //we can pass null here
    }

    /**
     * Establish a connection to the Privacy service using context.getSystemService if possible, otherwise without a context
     * @param context  Context to use to connect to the service
     * @return PrivacySettingsManager connected to the privacy service
     */
    public static PrivacySettingsManager getPrivacyService(Context context) {
        try {
            if (context != null) {
                try {
                    PrivacySettingsManager privacySettingsManager = (PrivacySettingsManager) context.getSystemService("privacy");
                    privacySettingsManager.contextReference = new WeakReference<Context>(context);
                    return privacySettingsManager;
                } catch (Exception e) {
                    PrivacyDebugger.w(TAG, "PrivacySettingsManager:getPrivacyService(Context): exception occurred trying to obtain static service. Falling back to non-static service.", e);
                    return getPrivacyService();
                }
            } else {
                return getPrivacyService();
            }
        } catch (Exception e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:getPrivacyService(Context): exception occurred getting privacy service");
            // return a 'disconnected' privacy service manager so that it isn't null
            return new PrivacySettingsManager(null);
        }
    }
    
    private IPrivacySettings getOnErrorObject(String packageName) {
        String onErrorSetting;
        try {
            onErrorSetting = readExternalSetting(PrivacyPersistenceAdapter.ACTION_ON_ERROR_SETTING);
            if (onErrorSetting.equals("allow")) {
                return new PrivacySettingsErrorAllow();
            } else {
                return new PrivacySettingsErrorDeny();
            }
        } catch (Exception e) {
            PrivacyDebugger.e(TAG, "PrivacySettingsManager:getOnErrorObject: exception occurred getting on error setting", e);
            return new PrivacySettingsErrorDeny(); 
        }
    }
    
    private String readExternalSetting(String setting) throws IOException, FileNotFoundException {
        File settingFile = new File(PrivacyPersistenceAdapter.SETTINGS_DIRECTORY + File.separator + setting);
        
        if (!settingFile.exists()) {
            throw new FileNotFoundException("readExternalSetting: Settings file missing for setting: " + setting);
        }
        if (settingFile.isDirectory()) {
            throw new IOException("readExternalSetting: Provided setting name pointed to folder, not file");
        }
        
        try {
            StringWriter writer;
            
            InputStreamReader reader = new InputStreamReader(new FileInputStream(settingFile));
            try {
                writer = new StringWriter();
                char[] buffer = new char[1024];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, count);
                }
                return writer.toString();
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new IOException("readExternalSetting - could not read settings file", e);
        }
    }
}
