import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class LockerManager {
    private Map<String, JSONObject> lockerData;
    private final AES aes;

    // Constructor for LockerManager
    public LockerManager() {
        aes = new AES();
        lockerData = new HashMap<>();
        loadLockers();
    }

    // Function to create a new locker
    public void createLocker(String name, String password) throws Exception {
        if (lockerData.containsKey(name)) {
            throw new IllegalArgumentException("Locker already exists");
        }
        byte[] salt = aes.generateSalt();
        String passwordHash = aes.hashPassword(password, salt);
        JSONObject lockerJson = new JSONObject();
        lockerJson.put("salt", aes.encodeSalt(salt));
        lockerJson.put("passwordHash", passwordHash);
        lockerData.put(name, lockerJson);
        saveLockers();
    }

    // Function to get a locker by name and password
    public Locker getLocker(String name, String password) throws Exception {
        JSONObject lockerJson = lockerData.get(name);
        if (lockerJson == null) {
            throw new IllegalArgumentException("Locker does not exist");
        }
        String encodedSalt = lockerJson.getString("salt");
        byte[] salt = aes.decodeSalt(encodedSalt);
        String storedHash = lockerJson.getString("passwordHash");
        String computedHash = aes.hashPassword(password, salt);
        if (!computedHash.equals(storedHash)) {
            throw new IllegalArgumentException("Invalid password for locker");
        }
        return new Locker(name, password, salt, storedHash);
    }

    public Map<String, JSONObject> getLockers() {
        return lockerData;
    }

    // Function to save all lockers to a file
    private void saveLockers() {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, JSONObject> entry : lockerData.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }
        try {
            Files.writeString(new File("lockers.json").toPath(), json.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save lockers: " + e.getMessage(), e);
        }
    }

    // Function to load lockers from a file
    private void loadLockers() {
        File file = new File("lockers.json");
        if (!file.exists()) return;
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject json = new JSONObject(content);
            for (String name : json.keySet()) {
                JSONObject lockerJson = json.getJSONObject(name);
                if (!lockerJson.has("salt") || !lockerJson.has("passwordHash")) {
                    continue; // ข้าม Locker ที่ข้อมูลไม่สมบูรณ์
                }
                lockerData.put(name, lockerJson);
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException("Failed to load lockers: " + e.getMessage(), e);
        }
    }
}