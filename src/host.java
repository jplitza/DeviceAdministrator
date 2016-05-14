import java.security.KeyRep;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import net.i2p.crypto.eddsa.*;
import java.util.Base64;

class host {
    static public void main(String args[]) throws java.security.NoSuchAlgorithmException, java.security.spec.InvalidKeySpecException, java.security.InvalidKeyException, java.security.SignatureException {
        EdDSAPublicKey pub;
        EdDSAPrivateKey priv;
        if (args.length < 2) {
            System.err.println("Usage: foo <seqnum> <action> [<privkey> <pubkey>]");
            return;
        }
        else if (args.length < 4) {
            KeyPairGenerator kpg = new KeyPairGenerator();
            KeyPair kp = kpg.generateKeyPair();
            pub = (EdDSAPublicKey) kp.getPublic();
            priv = (EdDSAPrivateKey) kp.getPrivate();
        } else {
            priv = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(args[2])));
            pub = new EdDSAPublicKey(new X509EncodedKeySpec(Base64.getDecoder().decode(args[3])));
        }

        EdDSAEngine sig = new EdDSAEngine();
        byte data[] = {new Integer(args[0]).byteValue(), new Integer(args[1]).byteValue()};
        sig.initSign(priv);

        java.util.Base64.Encoder enc = Base64.getEncoder();
        System.out.println("Private key: " + enc.encodeToString(priv.getEncoded()));
        System.out.println("Public key: " + enc.encodeToString(pub.getEncoded()));
        byte signature[] = sig.signOneShot(data);
        byte result[] = new byte[data.length + signature.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(signature, 0, result, data.length, signature.length);
        System.out.println("Request: " + enc.encodeToString(result));
    }
}
