
import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Arrays;


public class SecureDelete {

    public static void secureDelete(String filePath, int passes) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File does not exist or is not a regular file: " + filePath);
        }

        long length = file.length();
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        SecureRandom random = new SecureRandom();
        byte[] buffer = new byte[8192]; // Increase buffer size for performance

        try {
            // Perform multiple passes
            for (int pass = 0; pass < passes; pass++) {
                long pos = 0;
                while (pos < length) {
                    random.nextBytes(buffer);
                    int writeSize = (int) Math.min(buffer.length, length - pos);
                    raf.write(buffer, 0, writeSize);
                    pos += writeSize;
                }
                raf.getFD().sync(); // Ensure data is written to disk
            }
        } finally {
            Arrays.fill(buffer, (byte) 0); // Clear buffer
            raf.close();
        }

        Files.delete(Paths.get(filePath));
    }

    public static void secureDelete(String filePath) throws IOException {
        secureDelete(filePath, 3); // Default to 3 passes
    }

    public static void main(String[] args) throws IOException {
        secureDelete("secret.txt");
    }
}