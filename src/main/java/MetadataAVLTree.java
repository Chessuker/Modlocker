import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MetadataAVLTree {
    // Define AVL tree 
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
    private final String metadataFile;

    // Constructor
    public MetadataAVLTree(String lockerName) {
        this.metadataFile = lockerName + "_metadata.json";
        loadFromFile();
    }

    public static String computeMetadataHash(JSONObject metadata) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] metadataBytes = metadata.toString().getBytes(StandardCharsets.UTF_8);
        digest.update(metadataBytes);
        return Base64.getEncoder().encodeToString(digest.digest());
    }

    public JSONObject getOrLoadMetadata(String filePath, AES aes) throws Exception {
        JSONObject metadata = getMetadata(filePath);
        if (metadata == null) {
            metadata = aes.readMetadata(new File(filePath));
            String hash = computeMetadataHash(metadata);
            insert(hash, filePath, metadata);
        }
        return metadata;
    }

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
        // check if parameters are valid
        if (hash == null || hash.isEmpty()) {
            throw new IllegalArgumentException("Hash cannot be null");
        }
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        root = insertRec(root, hash, filePath, metadata); // Insert into AVL tree
        saveToFile(); // Save to file
    }

    private Node insertRec(Node node, String hash, String filePath, JSONObject metadata) {
        if (node == null) {
            return new Node(hash, filePath, metadata);
        }
        // Insert by comparing hash values
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

        // balance the tree
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

    // Get metadata by file path
    public JSONObject getMetadata(String filePath) throws Exception {
        // get metadata hash from the file
        String hash = computeMetadataHashFromFile(filePath);
        // check if the hash is valid
        Node node = searchRec(root, hash);
        if (node != null && !node.filePath.equals(filePath)) {
            node.filePath = filePath;
            saveToFile();
        }
        return node != null ? node.metadata : null;
    }

    // Get metadata by hash
    public JSONObject getMetadataByHash(String hash) throws Exception {
        Node node = searchRec(root, hash);
        return node != null ? node.metadata : null;
    }

    private Node searchRec(Node node, String hash) {
        if (node == null || node.hash.equals(hash)) {
            return node;
        }
        int cmp = hash.compareTo(node.hash);
        return cmp < 0 ? searchRec(node.left, hash) : searchRec(node.right, hash);
    }

    private String computeMetadataHashFromFile(String filePath) throws Exception {
        AES aes = new AES();
        JSONObject metadata = aes.readMetadata(new File(filePath));
        return computeMetadataHash(metadata);
    }

    public List<JSONObject> getSortedByTimestamp() {
        List<JSONObject> result = new ArrayList<>();
        inOrderTraversal(root, result);
        result.sort((a, b) -> a.getString("timestamp").compareTo(b.getString("timestamp")));
        return result;
    }

    public List<JSONObject> getSortedBySize() {
        List<JSONObject> result = new ArrayList<>();
        inOrderTraversal(root, result);
        result.sort((a, b) -> Long.compare(a.getLong("size"), b.getLong("size")));
        return result;
    }

    private void inOrderTraversal(Node node, List<JSONObject> result) {
        if (node == null) return;
        inOrderTraversal(node.left, result);
        result.add(node.metadata);
        inOrderTraversal(node.right, result);
    }

    public List<JSONObject> filterMetadata(String extension, Long minSize, Long maxSize, Date startTime, Date endTime) {
        List<JSONObject> result = new ArrayList<>();
        filterMetadataRec(root, extension, minSize, maxSize, startTime, endTime, result);
        return result;
    }
    
    private void filterMetadataRec(Node node, String extension, Long minSize, Long maxSize, Date startTime, Date endTime, List<JSONObject> result) {
        if (node == null) return;
    
        // In-order traversal to check all nodes
        filterMetadataRec(node.left, extension, minSize, maxSize, startTime, endTime, result);
    
        JSONObject metadata = node.metadata;
        boolean matches = true;
    
        // Check extension condition
        if (extension != null && !metadata.getString("extension").equalsIgnoreCase(extension)) {
            matches = false;
        }
    
        // Check size range condition
        long size = metadata.getLong("size");
        if (minSize != null && size < minSize) {
            matches = false;
        }
        if (maxSize != null && size > maxSize) {
            matches = false;
        }
    
        // Check timestamp range condition
        try {
            String timestampStr = metadata.getString("timestamp");
            Date timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(timestampStr);
    
            if (startTime != null && timestamp.before(startTime)) {
                matches = false;
            }
            if (endTime != null && timestamp.after(endTime)) {
                matches = false;
            }
        } catch (java.text.ParseException e) {
            matches = false; // Skip invalid timestamps
        }
    
        // Add metadata if all conditions match
        if (matches) {
            result.add(metadata);
        }
    
        filterMetadataRec(node.right, extension, minSize, maxSize, startTime, endTime, result);
    }
    
    // Summarize metadata by file extension
    public Map<String, Integer> summarizeByExtension() {
        Map<String, Integer> summary = new HashMap<>();
        summarizeByExtensionRec(root, summary);
        return summary;
    }

    private void summarizeByExtensionRec(Node node, Map<String, Integer> summary) {
        if (node == null) return;
        // In-order traversal to check all nodes
        summarizeByExtensionRec(node.left, summary);
        // Get the file extension from metadata
        String extension = node.metadata.getString("extension");
        // Update the summary map with the file extension count
        summary.put(extension, summary.getOrDefault(extension, 0) + 1);
        summarizeByExtensionRec(node.right, summary);
    }

    public void cleanInvalidMetadata() {
        List<String> hashesToRemove = new ArrayList<>();
        // Collect hashes of invalid metadata
        collectInvalidMetadataRec(root, hashesToRemove);
        // Remove invalid metadata from the AVL tree
        for (String hash : hashesToRemove) {
            root = delete(root, hash);
        }
        saveToFile();
    }

    private void collectInvalidMetadataRec(Node node, List<String> hashesToRemove) {
        if (node == null) return;
        // In-order traversal to check all nodes
        collectInvalidMetadataRec(node.left, hashesToRemove);
        // Check if the file exists
        if (!new File(node.filePath).exists()) {
            hashesToRemove.add(node.hash);
        }
        collectInvalidMetadataRec(node.right, hashesToRemove);
    }

    private Node delete(Node node, String hash) {
        if (node == null) return null;
        // Find the node to delete
        int cmp = hash.compareTo(node.hash);
        if (cmp < 0) {
            node.left = delete(node.left, hash);
        } else if (cmp > 0) {
            node.right = delete(node.right, hash);
        } else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            Node minNode = findMin(node.right);
            node.hash = minNode.hash;
            node.filePath = minNode.filePath;
            node.metadata = minNode.metadata;
            node.right = delete(node.right, minNode.hash);
        }
        // Update height and balance the tree
        node.height = Math.max(height(node.left), height(node.right)) + 1;
        int balance = balanceFactor(node);
        if (balance > 1 && balanceFactor(node.left) >= 0) {
            return rightRotate(node);
        }
        if (balance > 1 && balanceFactor(node.left) < 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }
        if (balance < -1 && balanceFactor(node.right) <= 0) {
            return leftRotate(node);
        }
        if (balance < -1 && balanceFactor(node.right) > 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }
        return node;
    }

    private Node findMin(Node node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    public void saveToFile() {
        // Save the AVL tree to a JSON file
        try {
            JSONArray jsonArray = new JSONArray();
            saveToJson(root, jsonArray);
            Files.writeString(Paths.get(metadataFile), jsonArray.toString(2));
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to save metadata: " + e.getMessage(), e);
        }
    }

    private void saveToJson(Node node, JSONArray jsonArray) {
        if (node == null) return;
        // In-order traversal to save all nodes
        saveToJson(node.left, jsonArray);
        JSONObject json = new JSONObject();
        // Add metadata to JSON object
        json.put("hash", node.hash);
        json.put("filePath", node.filePath);
        json.put("metadata", node.metadata);
        jsonArray.put(json);
        saveToJson(node.right, jsonArray);
    }

    public void loadFromFile() {
        // Load the AVL tree from a JSON file
        try {
            File file = new File(metadataFile);
            //  Check if the file exists
            if (file.exists()) {
                // Read the file content
                String content = Files.readString(Paths.get(metadataFile));
                JSONArray jsonArray = new JSONArray(content);
                root = null;
                // Parse the JSON array and insert into the AVL tree
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    String hash = json.getString("hash");
                    String filePath = json.getString("filePath");
                    JSONObject metadata = json.getJSONObject("metadata");
                    insert(hash, filePath, metadata);
                }
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to load metadata: " + e.getMessage(), e);
        }
    }
}
