Device Administrator
====================

This app gives the owner of a device a last resort if he lost the device.
The owner communicates with the lost device via SMS (see [Security](#security) for more details).
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

* `RECEIVE_SMS` and `READ_SMS` to receive commands via SMS (necessary),
* `ACCESS_FINE_LOCATION` and `SEND_SMS` to answer with the current location (optional).

Furthermore, it has to be registered as a device administrator in the system settings in order to wipe the device (optional).
Even more, to enable GPS if it isn't enabled, it has to be configured as a device administrator *and* as the device owner, the latter via the following command (works only if there are no accounts configured yet):

    dpm set-device-owner de.jplitza.deviceadministrator/de.jplitza.deviceadministrator.DeviceAdmin

If that doesn't work but the device is rooted, manually editing the file `/data/system/device_owner.xml` to read the following should work:

```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<device-owner package="de.jplitza.deviceadministrator" />
```
([Source](http://stackoverflow.com/a/27909315))

Generating keypair/commands
---------------------------

This currently has to be done on a PC and is very cumbersome. Plans are to include this in the app in the future.

In a terminal:
```sh
# change to src/ directory
cd src/
# compile the host component of the app (only has to be done once)
javac -extdir . host.java
# generate keypair
CLASSPATH=eddsa-0.1.0.jar:. java host
```
This will output a private and a public key in Base64 format.
**The private key isn't stored anywhere**, you need to save it somewhere yourself!
Afterwards, you can copy the public key into the app on your phone, for example by generating a QR code with the public key in it.

To generate a command, invoke the program like so:
```sh
CLASSPATH=eddsa-0.1.0.jar:. java host $PRIVKEY $SEQNUM $COMMAND
```
where `$PRIVKEY` is the private key that you copied somewhere save (right?), `$SEQNUM` is a sequence number higher than the last one your phone received, and `$COMMAND` is one of the following:

 Code | Command
------|---------
 0    | Test, will only spawn a message on the phone when received.
 1    | Locate
 2    | Wipe