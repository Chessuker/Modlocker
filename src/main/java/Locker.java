import java.io.File;

import javax.crypto.SecretKey;

import org.json.JSONObject;

public class Locker {
    private final String name;
    private final SecretKey key;
    private final byte[] salt;
    private final String passwordHash;
    private final MetadataAVLTree metadataTree;
    private final AES aes;

    // Constructor for creating a new locker
    public Locker(String name, String password) throws Exception {
        this.name = name;
        this.aes = new AES();
        this.salt = aes.generateSalt();
        this.key = aes.deriveKeyFromPassword(password, salt);
        this.passwordHash = aes.hashPassword(password, salt);
        this.metadataTree = new MetadataAVLTree(name);
    }

    // Constructor for loading an existing locker
    public Locker(String name, String password, byte[] salt, String passwordHash) throws Exception {
        this.name = name;
        this.aes = new AES();
        this.salt = salt;
        this.passwordHash = passwordHash;
        this.key = aes.deriveKeyFromPassword(password, salt);
        this.metadataTree = new MetadataAVLTree(name);
        // Verify password
        if (!aes.hashPassword(password, salt).equals(passwordHash)) {
            throw new IllegalArgumentException("Invalid password for locker");
        }
    }

    public String getName() {
        return name;
    }

    public SecretKey getKey() {
        return key;
    }

    public byte[] getSalt() {
        return salt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public MetadataAVLTree getMetadataTree() {
        return metadataTree;
    }

    // Function to encrypt a file and store its metadata
    public void encryptFile(File inputFile, File outputFile, JSONObject metadata) throws Exception {
        aes.encryptFile(inputFile, outputFile, key, salt, metadata);
        String hash = metadataTree.computeMetadataHash(metadata);
        metadataTree.insert(hash, outputFile.getAbsolutePath() + ".enc", metadata);
    }

    // Function to retrieve metadata and decrypt a file
    public void decryptFile(File inputFile, File outputFile) throws Exception {
        metadataTree.getOrLoadMetadata(inputFile.getAbsolutePath(), aes);
        aes.decryptFile(inputFile, outputFile, key);
    }

    // Function to read metadata from a file
    public byte[] decryptReadBytes(File inputFile) throws Exception {
        metadataTree.getOrLoadMetadata(inputFile.getAbsolutePath(), aes);
        return aes.decryptReadBytes(inputFile, key);
    }
}