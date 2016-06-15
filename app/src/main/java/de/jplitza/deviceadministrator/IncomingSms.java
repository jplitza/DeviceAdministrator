package de.jplitza.deviceadministrator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsMessage;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.security.spec.X509EncodedKeySpec;
import net.i2p.crypto.eddsa.*;

public class IncomingSms extends BroadcastReceiver {
    enum Command {
        TEST,
        LOCATE,
        WIPE
    }

    private void activateGps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("allow_location_modechange", false))
            return;

        if (!DeviceAdmin.getDPM(context).isAdminActive(DeviceAdmin.getComponentName(context)))
            return;

        if (!DeviceAdmin.getDPM(context).isDeviceOwnerApp(context.getApplicationContext().getPackageName()))
            return;

        DeviceAdmin.getDPM(context).setSecureSetting(
                DeviceAdmin.getComponentName(context),
                Settings.Secure.LOCATION_MODE,
                Integer.toString(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY)
        );
        Log.d("SmsReceiver", "Forcefully enabled GPS");
    }
    private void getLocation(Context context, String number) throws SecurityException {
        activateGps(context);
        LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationSender(number), null);
    }

    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = prefs.getString("public_key", "");
        if (key.isEmpty())
            return;

        SmsMessage msgs[] = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent);

        if (msgs == null)
            return;

        for (SmsMessage msg : msgs) {

            String senderNum = msg.getDisplayOriginatingAddress();
            String message = msg.getDisplayMessageBody();

            try {
                byte data[] = Base64.decode(message, Base64.DEFAULT);

                EdDSAEngine sig = new EdDSAEngine();
                sig.initVerify(new EdDSAPublicKey(new X509EncodedKeySpec(Base64.decode(key, Base64.DEFAULT))));
                if (!sig.verifyOneShot(data, 0, 2, data, 2, data.length - 2))
                    continue;
                Log.d("SmsReceiver", "Signature valid, command: " + data[1] + ", seqnum: " + data[0]);
                byte seqnum = data[0];
                if (seqnum < Integer.valueOf(prefs.getString("seqnum", "0"))) {
                    Log.d("SmsReceiver", "Sequence number " + seqnum + " is lower than next expected sequence number " + prefs.getString("seqnum", "0"));
                    continue;
                }
                SharedPreferences.Editor e = prefs.edit();
                e.putString("seqnum", Integer.toString(seqnum + 1));
                e.apply();
                try {
                    Command command = Command.values()[data[1]];
                    switch (command) {
                        case TEST:
                            Toast.makeText(
                                    context,
                                    String.format(
                                            context.getResources().getString(R.string.msg_test_successful),
                                            seqnum
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                            break;
                        case LOCATE:
                            if (prefs.getBoolean("allow_location", false))
                                getLocation(context, senderNum);
                            break;
                        case WIPE:
                            if (prefs.getBoolean("allow_wipe", false))
                                DeviceAdmin.getDPM(context).wipeData(0);
                            break;
                    }
                }
                catch(ArrayIndexOutOfBoundsException exc) {
                    Log.w("SmsReceiver", "Valid message but invalid command received: " + data[1]);
                }
            } catch (Exception e) {
                Log.e("SmsReceiver", "Exception", e);

            }
        }
    }
}
