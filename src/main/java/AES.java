import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

public class AES extends PBEKey {
    // private static final int ITERATIONS = 65536;
    // private static final int KEY_LENGTH = 128; // bits
    // private static final int SALT_LENGTH = 16; // bytes
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Override
    public SecretKey deriveKeyFromPassword(String password, byte[] salt) throws Exception {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (salt == null || salt.length != SALT_LENGTH) {
            throw new IllegalArgumentException("Invalid salt length");
        }
        char[] passwordChars = password.toCharArray();
        try {
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            Arrays.fill(passwordChars, '\0');
        }
    }

    @Override
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    @Override
    public String encodeSalt(byte[] salt) {
        return Base64.getEncoder().encodeToString(salt);
    }

    @Override
    public byte[] decodeSalt(String encoded) {
        return Base64.getDecoder().decode(encoded);
    }

    public SecretKey generateKey(String password) throws Exception {
        byte[] salt = generateSalt();
        return deriveKeyFromPassword(password, salt);
    }

    public byte[] nonce() {
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    public void encryptFile(File inputFile, File outputEncryptedFile, String password, byte[] salt, JSONObject metadata) throws Exception {
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException("Input file does not exist or is not a file");
        }
        if (outputEncryptedFile.isDirectory()) {
            throw new IllegalArgumentException("Output path cannot be a directory");
        }

        // Read input file
        byte[] inputBytes = Files.readAllBytes(inputFile.toPath());
        byte[] nonce = nonce();

        // Encrypt
        SecretKey key = deriveKeyFromPassword(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] encryptedBytes = cipher.doFinal(inputBytes);

        // Prepare metadata
        byte[] metadataBytes = metadata.toString().getBytes(StandardCharsets.UTF_8);
        byte[] headerLength = new byte[4];
        int length = metadataBytes.length;
        headerLength[0] = (byte) (length >> 24);
        headerLength[1] = (byte) (length >> 16);
        headerLength[2] = (byte) (length >> 8);
        headerLength[3] = (byte) length;

        // Write to file
        File outputFileWithExtension = new File(outputEncryptedFile.getAbsolutePath() + ".enc");
        try (FileOutputStream fos = new FileOutputStream(outputFileWithExtension)) {
            fos.write(headerLength);      // 4 bytes
            fos.write(metadataBytes);     // Metadata
            fos.write(nonce);            // 12 bytes
            fos.write(salt);             // 16 bytes
            fos.write(encryptedBytes);   // Ciphertext
        }
    }

    public JSONObject readMetadata(File inputFile) throws Exception {
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException("Input file does not exist or is not a file");
        }
        if (!inputFile.getName().endsWith(".enc")) {
            throw new IllegalArgumentException("Input file must have .enc extension");
        }

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // Read header length
            byte[] headerLengthBytes = new byte[4];
            if (fis.read(headerLengthBytes) != 4) {
                throw new IllegalArgumentException("Invalid header length");
            }
            int headerLength = ((headerLengthBytes[0] & 0xFF) << 24) |
                               ((headerLengthBytes[1] & 0xFF) << 16) |
                               ((headerLengthBytes[2] & 0xFF) << 8) |
                               (headerLengthBytes[3] & 0xFF);

            // Read metadata
            byte[] metadataBytes = new byte[headerLength];
            if (fis.read(metadataBytes) != headerLength) {
                throw new IllegalArgumentException("Invalid metadata");
            }
            String metadataJson = new String(metadataBytes, StandardCharsets.UTF_8);
            return new JSONObject(metadataJson);
        }
    }

    public void decryptFile(File inputFile, File outputFile, String password) throws Exception {
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException("Input file does not exist or is not a file");
        }
        if (!inputFile.getName().endsWith(".enc")) {
            throw new IllegalArgumentException("Input file must have .enc extension");
        }
        if (outputFile.isDirectory()) {
            throw new IllegalArgumentException("Output path cannot be a directory");
        }

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // Read header length
            byte[] headerLengthBytes = new byte[4];
            if (fis.read(headerLengthBytes) != 4) {
                throw new IllegalArgumentException("Invalid header length");
            }
            int headerLength = ((headerLengthBytes[0] & 0xFF) << 24) |
                               ((headerLengthBytes[1] & 0xFF) << 16) |
                               ((headerLengthBytes[2] & 0xFF) << 8) |
                               (headerLengthBytes[3] & 0xFF);

            // Skip metadata
            byte[] metadataBytes = new byte[headerLength];
            if (fis.read(metadataBytes) != headerLength) {
                throw new IllegalArgumentException("Invalid metadata");
            }

            // Read nonce, salt, ciphertext
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            byte[] salt = new byte[SALT_LENGTH];
            if (fis.read(nonce) != GCM_NONCE_LENGTH || fis.read(salt) != SALT_LENGTH) {
                throw new IllegalArgumentException("Invalid nonce or salt");
            }

            // Read ciphertext
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] ciphertext = baos.toByteArray();

            // Decrypt
            SecretKey key = deriveKeyFromPassword(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            Files.write(outputFile.toPath(), decryptedBytes);
        }
    }

    public byte[] decryptReadBytes(File inputFile, String password) throws Exception {
        if (!inputFile.exists() || !inputFile.isFile()) {
            throw new IllegalArgumentException("Input file does not exist or is not a file");
        }
        if (!inputFile.getName().endsWith(".enc")) {
            throw new IllegalArgumentException("Input file must have .enc extension");
        }

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // Read header length
            byte[] headerLengthBytes = new byte[4];
            if (fis.read(headerLengthBytes) != 4) {
                throw new IllegalArgumentException("Invalid header length");
            }
            int headerLength = ((headerLengthBytes[0] & 0xFF) << 24) |
                               ((headerLengthBytes[1] & 0xFF) << 16) |
                               ((headerLengthBytes[2] & 0xFF) << 8) |
                               (headerLengthBytes[3] & 0xFF);

            // Skip metadata
            byte[] metadataBytes = new byte[headerLength];
            if (fis.read(metadataBytes) != headerLength) {
                throw new IllegalArgumentException("Invalid metadata");
            }

            // Read nonce, salt, ciphertext
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            byte[] salt = new byte[SALT_LENGTH];
            if (fis.read(nonce) != GCM_NONCE_LENGTH || fis.read(salt) != SALT_LENGTH) {
                throw new IllegalArgumentException("Invalid nonce or salt");
            }

            // Read ciphertext
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] ciphertext = baos.toByteArray();

            // Decrypt
            SecretKey key = deriveKeyFromPassword(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return cipher.doFinal(ciphertext);
        }
    }

    public String decryptRead(File inputFile, String password) throws Exception {
        byte[] decryptedBytes = decryptReadBytes(inputFile, password);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}