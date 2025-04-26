import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

public class MetadataAVLTree {
    private class Node {
        String hash;
        String filePath;
        JSONObject metadata;
        Node left, right;
        int height;

        Node(String hash, String filePath, JSONObject metadata) {
            this.hash = hash;
            this.filePath = filePath;
            this.metadata = metadata;
            this.height = 1;
        }
    }

    private Node root;
    private static final String METADATA_FILE = "metadata.json";

    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    private int balanceFactor(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;
        x.right = y;
        y.left = T2;
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        return x;
    }

    private Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;
        y.left = x;
        x.right = T2;
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        return y;
    }

    public void insert(String hash, String filePath, JSONObject metadata) {
        root = insertRec(root, hash, filePath, metadata);
        saveToFile(); // บันทึกทุกครั้งที่แทรก
    }

    private Node insertRec(Node node, String hash, String filePath, JSONObject metadata) {
        if (node == null) {
            return new Node(hash, filePath, metadata);
        }
        int cmp = hash.compareTo(node.hash);
        if (cmp < 0) {
            node.left = insertRec(node.left, hash, filePath, metadata);
        } else if (cmp > 0) {
            node.right = insertRec(node.right, hash, filePath, metadata);
        } else {
            node.filePath = filePath;
            node.metadata = metadata;
            return node;
        }

        node.height = Math.max(height(node.left), height(node.right)) + 1;
        int balance = balanceFactor(node);

        if (balance > 1 && hash.compareTo(node.left.hash) < 0) {
            return rightRotate(node);
        }
        if (balance < -1 && hash.compareTo(node.right.hash) > 0) {
            return leftRotate(node);
        }
        if (balance > 1 && hash.compareTo(node.left.hash) > 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        if (balance < -1 && hash.compareTo(node.right.hash) < 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    public JSONObject getMetadata(String filePath) throws Exception {
        String hash = computeMetadataHashFromFile(filePath);
        Node node = searchRec(root, hash);
        if (node != null && !node.filePath.equals(filePath)) {
            node.filePath = filePath;
            saveToFile(); // อัปเดตไฟล์เมื่อ filePath เปลี่ยน
        }
        return node != null ? node.metadata : null;
    }

    private Node searchRec(Node node, String hash) {
        if (node == null || node.hash.equals(hash)) {
            return node;
        }
        int cmp = hash.compareTo(node.hash);
        return cmp < 0 ? searchRec(node.left, hash) : searchRec(node.right, hash);
    }

    public String computeMetadataHash(JSONObject metadata) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String metadataStr = metadata.toString();
        digest.update(metadataStr.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(digest.digest());
    }

    private String computeMetadataHashFromFile(String filePath) throws Exception {
        AES aes = new AES();
        JSONObject metadata = aes.readMetadata(new File(filePath));
        return computeMetadataHash(metadata);
    }

    public void saveToFile() {
        try {
            JSONArray jsonArray = new JSONArray();
            saveToJson(root, jsonArray);
            Files.writeString(Paths.get(METADATA_FILE), jsonArray.toString(2));
        } catch (Exception e) {
            System.err.println("Failed to save metadata: " + e.getMessage());
        }
    }

    private void saveToJson(Node node, JSONArray jsonArray) {
        if (node == null) return;
        saveToJson(node.left, jsonArray);
        JSONObject json = new JSONObject();
        json.put("hash", node.hash);
        json.put("filePath", node.filePath);
        json.put("metadata", node.metadata);
        jsonArray.put(json);
        saveToJson(node.right, jsonArray);
    }

    public void loadFromFile() {
        try {
            File file = new File(METADATA_FILE);
            if (file.exists()) {
                String content = Files.readString(Paths.get(METADATA_FILE));
                JSONArray jsonArray = new JSONArray(content);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    String hash = json.getString("hash");
                    String filePath = json.getString("filePath");
                    JSONObject metadata = json.getJSONObject("metadata");
                    insert(hash, filePath, metadata);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load metadata: " + e.getMessage());
        }
    }
}