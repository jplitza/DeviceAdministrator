import java.security.KeyRep;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import net.i2p.crypto.eddsa.*;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import java.util.Base64;

class host {
    static private Base64.Encoder enc;

    static private void usage() {
        System.err.println("Usage: java host [<privkey> [<seqnum> <action>]]");
        System.err.println();
        System.err.println("If invoked without arguments, generates a new keypair and outputs it.");
        System.err.println("If invoked with only the private key, outputs the corresponding public key.");
    }

    static private String base64(byte[] src) {
        if (enc == null)
            enc = Base64.getEncoder();
        return enc.encodeToString(src);
    }

    static public void main(String args[])
        throws java.security.NoSuchAlgorithmException,
               java.security.spec.InvalidKeySpecException,
               java.security.InvalidKeyException,
               java.security.SignatureException {
        EdDSAPublicKey pub;
        EdDSAPrivateKey priv;

        if (args.length == 1 && args[0].equals("-h")) {
            usage();
            return;
        }

        if (args.length == 0) {
            // generate a new keypair, output it and exit
            KeyPairGenerator kpg = new KeyPairGenerator();
            KeyPair kp = kpg.generateKeyPair();
            pub = (EdDSAPublicKey) kp.getPublic();
            priv = (EdDSAPrivateKey) kp.getPrivate();
            System.out.println("Private key: " + base64(priv.getEncoded()));
            System.out.println("Public key: " + base64(pub.getEncoded()));
            return;
        } else if (args.length == 3 || args.length == 1) {
            // read private key from command line, compute corresponding
            // public key and output the latter
            priv = new EdDSAPrivateKey(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(args[0]))
            );
            pub = new EdDSAPublicKey(
                new EdDSAPublicKeySpec(priv.getA(), priv.getParams())
            );
            System.out.println("Public key: " + base64(pub.getEncoded()));
            if (args.length == 1)
                return;
        } else {
            usage();
            return;
        }

        EdDSAEngine sig = new EdDSAEngine();
        byte data[] = {
            Byte.valueOf(args[1]),
            Byte.valueOf(args[2])
        };
        sig.initSign(priv);

        byte signature[] = sig.signOneShot(data);
        byte result[] = new byte[data.length + signature.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(signature, 0, result, data.length, signature.length);
        System.out.println("Request: " + base64(result));
    }
}
