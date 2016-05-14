package de.jplitza.deviceadministrator;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;

public class LocationSender implements LocationListener {
    final SmsManager sms = SmsManager.getDefault();

    String recipientNumber;

    public LocationSender(String recipientNumber) {
        this.recipientNumber = recipientNumber;
    }

    @Override
    public void onLocationChanged(Location location) {
        String loc = "geo:"+location.getLatitude()+","+location.getLongitude();
        Log.i("LocationSender", "Location obtained: " + loc);
        sms.sendTextMessage(recipientNumber, null, loc, null, null);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderDisabled(String s) {}

    @Override
    public void onProviderEnabled(String s) {}
}
