package de.jplitza.deviceadministrator;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.security.spec.X509EncodedKeySpec;
import net.i2p.crypto.eddsa.*;

public class IncomingSms extends BroadcastReceiver {
    static enum Command {
        TEST,
        LOCATE,
        WIPE
    };

    private void activateGps(Context context) {
        DevicePolicyManager mDPM = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDPM.setSecureSetting(getComponentName(context), Settings.Secure.LOCATION_MODE, new Integer(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY).toString());
    }
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceAdmin.class);
    }
    private void getLocation(Context context, String number) throws SecurityException {
        activateGps(context);
        LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationSender(number), null);
    }

    public void onReceive(Context context, Intent intent) {

        SmsMessage msgs[] = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent);

        if (msgs == null)
            return;

        for (SmsMessage msg : msgs) {

            String senderNum = msg.getDisplayOriginatingAddress();
            String message = msg.getDisplayMessageBody();
            Log.i("SmsReceiver", "senderNum: " + senderNum + "; message: " + message);

            try {
                byte data[] = Base64.decode(message, Base64.DEFAULT);

                EdDSAEngine sig = new EdDSAEngine();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                sig.initVerify(new EdDSAPublicKey(new X509EncodedKeySpec(Base64.decode(
                        prefs.getString("public_key", ""),
                        Base64.DEFAULT
                )
                )));
                if (!sig.verifyOneShot(data, 0, 2, data, 2, data.length - 2))
                    continue;
                Log.i("SmsReceiver", "Signature valid, command: " + data[1] + ", seqnum: " + data[0]);
                byte seqnum = data[0];
                Command command = Command.values()[data[1]];
                if (seqnum < new Integer(prefs.getString("seqnum", "0")))
                    continue;
                switch (command) {
                    case TEST:
                        Toast.makeText(context, "DevAdmin: Test with seqnum " + seqnum + " received.", Toast.LENGTH_LONG).show();
                        break;
                    case LOCATE:
                        getLocation(context, senderNum);
                        break;
                    case WIPE:
                        // TODO
                        break;
                }
                SharedPreferences.Editor e = prefs.edit();
                e.putString("seqnum", new Integer(seqnum + 1).toString());
                e.apply();
            } catch (Exception e) {
                Log.e("SmsReceiver", "Exception", e);

            }
        }
    }
}
