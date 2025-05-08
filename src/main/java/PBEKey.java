import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.SecretKey;

public abstract class PBEKey {

    public static final int ITERATIONS = 65536;
    public static final int KEY_LENGTH = 128; // bits
    public static final int SALT_LENGTH = 16; // bytes

    public abstract SecretKey deriveKeyFromPassword(String password, byte[] salt) throws Exception;

    public abstract byte[] generateSalt();

    public abstract String encodeSalt(byte[] salt);

    public abstract byte[] decodeSalt(String encoded);

    public String hashPassword(String password, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(password.getBytes("UTF-8"));
        digest.update(salt);
        return Base64.getEncoder().encodeToString(digest.digest());
    }
}