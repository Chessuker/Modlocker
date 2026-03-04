# Modlocker

Modlocker is a Java-based security application designed to encrypt and decrypt sensitive files while securely managing their metadata. It serves as both a practical security tool and an educational framework for secure software development.

Here's a Presentation: https://www.canva.com/design/DAHC9VN5PXU/WIECbG0sQsZ__Febrzhj1Q/view
---

## 1. Cryptography and Security Algorithms

**Encryption/Decryption:** Utilizes AES-GCM symmetric encryption with 128-bit keys, a 12-byte nonce, and a 128-bit tag for authentication.

**Key Derivation:** Employs PBKDF2 (with HMAC-SHA256 and 65,536 iterations) to securely derive a secret key from a user's password.

**Hashing & Randomization:** Uses SHA-256 for one-way metadata hashing to index files, and `SecureRandom` to generate unique salts, nonces, and overwrite data for each encryption.

---

## 2. File Processing

**Encryption Process:** The program generates a random salt and nonce, derives the key, and encrypts the file. It then stores the file's metadata (original name, extension, size, and timestamp) directly inside the new encrypted file, which is saved with a `.ENC` extension.

**Decryption Process:** The program extracts the metadata, salt, and nonce from the `.ENC` file, verifies its presence in the locker, and derives the key to decrypt the ciphertext back to its original format.

**Read Functionality:** Users can temporarily decrypt and open files in their default application; the temporary file is removed once the user closes the app.

---

## 3. Metadata Management

**AVL Tree Structure:** Instead of a standard database, Modlocker uses a self-balancing binary search tree (AVL Tree) to store metadata and file paths, ensuring efficient O(log N) search operations.

**File Organization:** The "Locker" acts as a container that maps encrypted files' metadata using the AVL Tree, which indexes nodes based on the SHA-256 hash of the metadata content.

**Metadata Operations:** Using inorder traversals, the program supports the following operations:

| Operation     | Description                                                  |
|---------------|--------------------------------------------------------------|
| **Sort**      | Sort files by size or timestamp                              |
| **Filter**    | Filter files by extension, size, or timestamp                |
| **Summarize** | Count files grouped by their extension type                  |
| **Cleanup**   | Delete metadata nodes for invalid or non-existent file paths |

---

## 4. System Architecture

| Component        | Technology                                          |
|------------------|-----------------------------------------------------|
| GUI              | Java Swing                                          |
| File I/O         | Buffered streams for efficient file handling        |
| Data Persistence | `JSONObject` / `JSONArray` for locker serialization |
