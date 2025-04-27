import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

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
        saveToFile();
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
            saveToFile();
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

    // ดึง metadata ที่เรียงลำดับตาม timestamp
    public List<JSONObject> getSortedByTimestamp() {
        List<JSONObject> result = new ArrayList<>();
        inOrderTraversal(root, result);
        result.sort((a, b) -> a.getString("timestamp").compareTo(b.getString("timestamp")));
        return result;
    }

    // ดึง metadata ที่เรียงลำดับตามขนาด
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

    // ค้นหาแบบช่วงตามขนาด
    public List<JSONObject> findBySizeRange(long minSize, long maxSize) {
        List<JSONObject> result = new ArrayList<>();
        findBySizeRangeRec(root, minSize, maxSize, result);
        return result;
    }

    private void findBySizeRangeRec(Node node, long minSize, long maxSize, List<JSONObject> result) {
        if (node == null) return;
        long size = node.metadata.getLong("size");
        if (size > minSize) findBySizeRangeRec(node.left, minSize, maxSize, result);
        if (size >= minSize && size <= maxSize) result.add(node.metadata);
        if (size < maxSize) findBySizeRangeRec(node.right, minSize, maxSize, result);
    }

    // ค้นหาแบบช่วงตาม timestamp
    public List<JSONObject> findByTimestampRange(String startTime, String endTime) {
        List<JSONObject> result = new ArrayList<>();
        findByTimestampRangeRec(root, startTime, endTime, result);
        return result;
    }

    private void findByTimestampRangeRec(Node node, String startTime, String endTime, List<JSONObject> result) {
        if (node == null) return;
        String timestamp = node.metadata.getString("timestamp");
        if (timestamp.compareTo(startTime) > 0) findByTimestampRangeRec(node.left, startTime, endTime, result);
        if (timestamp.compareTo(startTime) >= 0 && timestamp.compareTo(endTime) <= 0) result.add(node.metadata);
        if (timestamp.compareTo(endTime) < 0) findByTimestampRangeRec(node.right, startTime, endTime, result);
    }

    // ค้นหาด้วยเงื่อนไขที่ซับซ้อน (เช่น นามสกุลและขนาด)
    public List<JSONObject> findByComplexCondition(String extension, Long minSize, Long maxSize) {
        List<JSONObject> result = new ArrayList<>();
        findByComplexConditionRec(root, extension, minSize, maxSize, result);
        return result;
    }

    private void findByComplexConditionRec(Node node, String extension, Long minSize, Long maxSize, List<JSONObject> result) {
        if (node == null) return;
        findByComplexConditionRec(node.left, extension, minSize, maxSize, result);
        JSONObject metadata = node.metadata;
        boolean matches = true;
        if (extension != null && !metadata.getString("extension").equalsIgnoreCase(extension)) {
            matches = false;
        }
        if (minSize != null && metadata.getLong("size") < minSize) {
            matches = false;
        }
        if (maxSize != null && metadata.getLong("size") > maxSize) {
            matches = false;
        }
        if (matches) result.add(metadata);
        findByComplexConditionRec(node.right, extension, minSize, maxSize, result);
    }

    // สรุปข้อมูลตามนามสกุล
    public Map<String, Integer> summarizeByExtension() {
        Map<String, Integer> summary = new HashMap<>();
        summarizeByExtensionRec(root, summary);
        return summary;
    }

    private void summarizeByExtensionRec(Node node, Map<String, Integer> summary) {
        if (node == null) return;
        summarizeByExtensionRec(node.left, summary);
        String extension = node.metadata.getString("extension");
        summary.put(extension, summary.getOrDefault(extension, 0) + 1);
        summarizeByExtensionRec(node.right, summary);
    }

    // ทำความสะอาด metadata ของไฟล์ที่ถูกลบหรือย้าย
    public void cleanInvalidMetadata() {
        List<String> hashesToRemove = new ArrayList<>();
        collectInvalidMetadataRec(root, hashesToRemove);
        for (String hash : hashesToRemove) {
            root = delete(root, hash);
        }
        saveToFile();
    }

    private void collectInvalidMetadataRec(Node node, List<String> hashesToRemove) {
        if (node == null) return;
        collectInvalidMetadataRec(node.left, hashesToRemove);
        if (!new File(node.filePath).exists()) {
            hashesToRemove.add(node.hash);
        }
        collectInvalidMetadataRec(node.right, hashesToRemove);
    }

    // เมธอดรีเคอร์ซีฟสำหรับลบโหนด
    private Node delete(Node node, String hash) {
        if (node == null) return null;
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
        try {
            JSONArray jsonArray = new JSONArray();
            saveToJson(root, jsonArray);
            Files.writeString(Paths.get(METADATA_FILE), jsonArray.toString(2));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save metadata: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to load metadata: " + e.getMessage(), e);
        }
    }
}