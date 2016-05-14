Device Administrator
====================

This app gives the owner of a device a last resort if he lost the device.
The owner communicates with the lost device via SMS (see [Security](#Security) for more details).
Features currently include:

* Sending current location of the device
    * Forcefully enabling GPS if it is disabled
* Wiping the device

Security
--------

The SMS message that triggers an action is signed by the Ed25519 private key of the owner.
Furthermore, it contains (apart from the command itself) a sequence number to counter replay attacks.
Replies are however neither signed nor encrypted!

Permissions
-----------

The app uses the following permissions:

* `RECEIVE_SMS` and `READ_SMS` to receive commands via SMS,
* `ACCESS_FINE_LOCATION` and `SEND_SMS` to answer with the current location.

Furthermore, it has to be registered as a device administrator in the system settings in order to wipe the device.
Even more, to enable GPS if it isn't enabled, it has to be configured as the device owner via the following command:

    dpm set-device-owner de.jplitza.deviceadministrator/de.jplitza.deviceadministrator.DeviceAdmin

If that doesn't work but the device is rooted, editing the file `/data/system/device_owner.xml` to read the following should work:

```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<device-owner package="de.jplitza.deviceadministrator" />
```
([Source](http://stackoverflow.com/a/27909315))