package de.jplitza.deviceadministrator;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

public class DeviceAdmin extends DeviceAdminReceiver {
    static DevicePolicyManager getDPM(Context context) {
        return (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceAdmin.class);
    }
}