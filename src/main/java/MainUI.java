import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.time.LocalDateTime;
import org.json.JSONObject;

public class MainUI extends JFrame {
    private JTextField inputFilePathField;
    private JTextField outputFilePathField;
    private JTextField outputFileNameField;
    private JPasswordField passwordField;
    private JTextArea outputArea;
    private JCheckBox showPasswordCheckBox;
    private JCheckBox deleteOriginalCheckBox;
    private JLabel statusLabel;
    private MetadataAVLTree metadataAVL;

    public MainUI() {
        setTitle("File Encryption/Decryption with Metadata");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        metadataAVL = new MetadataAVLTree();
        metadataAVL.loadFromFile();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Main Panel with GridBagLayout
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Input File Path
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Input File:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputFilePathField = new JTextField();
        inputFilePathField.setToolTipText("Select the file to encrypt, decrypt, or read");
        mainPanel.add(inputFilePathField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseInputButton = createStyledButton("Browse");
        browseInputButton.addActionListener(new BrowseFileAction(inputFilePathField, true));
        mainPanel.add(browseInputButton, gbc);

        // Output Path
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("Output Path:"), gbc);
        gbc.gridx = 1;
        outputFilePathField = new JTextField();
        outputFilePathField.setToolTipText("Select the destination folder for the output file");
        mainPanel.add(outputFilePathField, gbc);
        gbc.gridx = 2;
        JButton browseOutputButton = createStyledButton("Browse");
        browseOutputButton.addActionListener(new BrowseFileAction(outputFilePathField, false));
        mainPanel.add(browseOutputButton, gbc);

        // Output File Name
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Output File Name:"), gbc);
        gbc.gridx = 1;
        outputFileNameField = new JTextField();
        outputFileNameField.setToolTipText("Enter the name for the output file (optional, defaults to original name)");
        mainPanel.add(outputFileNameField, gbc);
        gbc.gridx = 2;
        mainPanel.add(new JLabel(), gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField();
        passwordField.setToolTipText("Enter a password (minimum 8 characters)");
        mainPanel.add(passwordField, gbc);
        gbc.gridx = 2;
        showPasswordCheckBox = new JCheckBox("Show");
        showPasswordCheckBox.addActionListener(e -> {
            passwordField.setEchoChar(showPasswordCheckBox.isSelected() ? (char) 0 : '•');
        });
        mainPanel.add(showPasswordCheckBox, gbc);

        // Delete Original
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(new JLabel("Delete Original:"), gbc);
        gbc.gridx = 1;
        deleteOriginalCheckBox = new JCheckBox("Delete original file after operation");
        deleteOriginalCheckBox.setToolTipText("Securely delete the input file after encryption or decryption");
        mainPanel.add(deleteOriginalCheckBox, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton encryptButton = createStyledButton("Encrypt");
        encryptButton.setBackground(new Color(50, 150, 50));
        encryptButton.setForeground(Color.BLACK);
        encryptButton.addActionListener(new EncryptAction());
        buttonPanel.add(encryptButton);

        JButton decryptButton = createStyledButton("Decrypt");
        decryptButton.setBackground(new Color(50, 50, 150));
        decryptButton.setForeground(Color.BLACK);
        decryptButton.addActionListener(new DecryptAction());
        buttonPanel.add(decryptButton);

        JButton readButton = createStyledButton("Read");
        readButton.setBackground(new Color(150, 100, 50));
        readButton.setForeground(Color.BLACK);
        readButton.addActionListener(new ReadAction());
        buttonPanel.add(readButton);

        JButton showMetadataButton = createStyledButton("Show Metadata");
        showMetadataButton.setBackground(new Color(100, 100, 200));
        showMetadataButton.setForeground(Color.BLACK);
        showMetadataButton.addActionListener(e -> {
            String inputFilePath = inputFilePathField.getText().trim();
            if (inputFilePath.isEmpty()) {
                showError("Please select an input file");
                return;
            }
            try {
                JSONObject metadata = metadataAVL.getMetadata(inputFilePath);
                if (metadata == null) {
                    AES aes = new AES();
                    metadata = aes.readMetadata(new File(inputFilePath));
                    String hash = metadataAVL.computeMetadataHash(metadata);
                    metadataAVL.insert(hash, inputFilePath, metadata);
                }
                JFrame metadataFrame = new JFrame("File Metadata");
                metadataFrame.setSize(400, 300);
                metadataFrame.setLocationRelativeTo(MainUI.this);
                JTextArea metadataArea = new JTextArea();
                metadataArea.setEditable(false);
                metadataArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
                metadataArea.append("Extension: " + metadata.getString("extension") + "\n");
                metadataArea.append("Original Name: " + metadata.getString("originalName") + "\n");
                metadataArea.append("Size: " + metadata.getLong("size") + " bytes\n");
                metadataArea.append("Timestamp: " + metadata.getString("timestamp") + "\n");
                metadataFrame.add(new JScrollPane(metadataArea));
                metadataFrame.setVisible(true);
                outputArea.append("Metadata displayed for: " + inputFilePath + "\n");
                statusLabel.setText("Metadata displayed");
            } catch (Exception ex) {
                outputArea.append("Failed to read metadata: " + ex.getMessage() + "\n");
                statusLabel.setText("Metadata read failed");
                showError("Failed to read metadata: " + ex.getMessage());
            }
        });
        buttonPanel.add(showMetadataButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        mainPanel.add(buttonPanel, gbc);

        // Output Area
        outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        // Status Bar
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Add components to frame
        add(mainPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Set modern font
        setFontForComponents(mainPanel, new Font("Segoe UI", Font.PLAIN, 14));
        setFontForComponents(buttonPanel, new Font("Segoe UI", Font.BOLD, 14));

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                metadataAVL.saveToFile();
                System.exit(0);
            }
        });
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void setFontForComponents(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof Container) {
                setFontForComponents((Container) comp, font);
            }
            comp.setFont(font);
        }
    }

    private class BrowseFileAction implements ActionListener {
        private JTextField targetField;
        private boolean isInput;

        public BrowseFileAction(JTextField targetField, boolean isInput) {
            this.targetField = targetField;
            this.isInput = isInput;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            if (isInput) {
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            } else {
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }
            int result = fileChooser.showOpenDialog(MainUI.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                targetField.setText(selectedFile.getAbsolutePath());
                statusLabel.setText("Selected: " + selectedFile.getName());
            } else {
                statusLabel.setText("Selection cancelled");
            }
        }
    }

    private class EncryptAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String inputFilePath = inputFilePathField.getText().trim();
            String outputFilePath = outputFilePathField.getText().trim();
            String outputFileName = outputFileNameField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);

            try {
                if (inputFilePath.isEmpty() || outputFilePath.isEmpty()) {
                    showError("Input path and output path are required");
                    return;
                }
                if (password.length() < 8) {
                    showError("Password must be at least 8 characters long");
                    return;
                }
                if (!outputFileName.isEmpty() && !outputFileName.matches("[a-zA-Z0-9._-]+")) {
                    showError("Output file name must contain only letters, numbers, dots, or hyphens");
                    return;
                }

                String fullOutputPath = outputFileName.isEmpty() ?
                        Paths.get(outputFilePath, new File(inputFilePath).getName()).toString() :
                        Paths.get(outputFilePath, outputFileName).toString();
                File outputEncryptedFile = new File(fullOutputPath + ".enc");
                if (outputEncryptedFile.exists()) {
                    int result = JOptionPane.showConfirmDialog(MainUI.this,
                            "Output file already exists. Overwrite?", "Confirm Overwrite",
                            JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                AES aes = new AES();
                byte[] salt = aes.generateSalt();
                File inputFile = new File(inputFilePath);
                JSONObject metadata = new JSONObject();
                metadata.put("extension", inputFile.getName().substring(inputFile.getName().lastIndexOf('.') + 1));
                metadata.put("originalName", inputFile.getName());
                metadata.put("size", inputFile.length());
                metadata.put("timestamp", LocalDateTime.now().toString());

                aes.encryptFile(inputFile, new File(fullOutputPath), password, salt, metadata);
                String hash = metadataAVL.computeMetadataHash(metadata);
                metadataAVL.insert(hash, fullOutputPath + ".enc", metadata);
                String message = "File encrypted successfully to " + fullOutputPath + ".enc";
                outputArea.append(message + "\n");
                statusLabel.setText("Encryption successful");

                if (deleteOriginalCheckBox.isSelected()) {
                    SecureDelete.secureDelete(inputFilePath);
                    outputArea.append("Original file securely deleted\n");
                    statusLabel.setText("Encryption and deletion successful");
                }

                JOptionPane.showMessageDialog(MainUI.this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                outputArea.append("Encryption failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Encryption failed");
                showError("Encryption failed: " + ex.getMessage());
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    private class DecryptAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String inputFilePath = inputFilePathField.getText().trim();
            String outputFilePath = outputFilePathField.getText().trim();
            String outputFileName = outputFileNameField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);

            try {
                if (inputFilePath.isEmpty() || outputFilePath.isEmpty()) {
                    showError("Input path and output path are required");
                    return;
                }
                if (password.length() < 8) {
                    showError("Password must be at least 8 characters long");
                    return;
                }
                if (!inputFilePath.endsWith(".enc")) {
                    showError("Input file must have .enc extension");
                    return;
                }
                if (!outputFileName.isEmpty() && !outputFileName.matches("[a-zA-Z0-9._-]+")) {
                    showError("Output file name must contain only letters, numbers, dots, or hyphens");
                    return;
                }

                JSONObject metadata = metadataAVL.getMetadata(inputFilePath);
                if (metadata == null) {
                    AES aes = new AES();
                    metadata = aes.readMetadata(new File(inputFilePath));
                    String hash = metadataAVL.computeMetadataHash(metadata);
                    metadataAVL.insert(hash, inputFilePath, metadata);
                }
                String extension = metadata.getString("extension");
                String originalName = metadata.getString("originalName");

                String fullOutputPath;
                if (outputFileName.isEmpty()) {
                    fullOutputPath = Paths.get(outputFilePath, originalName).toString();
                } else {
                    fullOutputPath = Paths.get(outputFilePath, outputFileName + "." + extension).toString();
                }

                File outputFile = new File(fullOutputPath);
                if (outputFile.exists()) {
                    int result = JOptionPane.showConfirmDialog(MainUI.this,
                            "Output file already exists. Overwrite?", "Confirm Overwrite",
                            JOptionPane.YES_NO_OPTION);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                AES aes = new AES();
                File encryptedFile = new File(inputFilePath);
                aes.decryptFile(encryptedFile, outputFile, password);
                String message = "File decrypted successfully to " + fullOutputPath;
                outputArea.append(message + "\n");
                statusLabel.setText("Decryption successful");

                if (deleteOriginalCheckBox.isSelected()) {
                    SecureDelete.secureDelete(inputFilePath);
                    outputArea.append("Original encrypted file securely deleted\n");
                    statusLabel.setText("Decryption and deletion successful");
                }

                JOptionPane.showMessageDialog(MainUI.this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                outputArea.append("Decryption failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Decryption failed");
                showError("Decryption failed: " + ex.getMessage());
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    private class ReadAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String inputFilePath = inputFilePathField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);

            try {
                if (inputFilePath.isEmpty()) {
                    showError("Input path is required");
                    return;
                }
                if (password.length() < 8) {
                    showError("Password must be at least 8 characters long");
                    return;
                }
                if (!inputFilePath.endsWith(".enc")) {
                    showError("Input file must have .enc extension");
                    return;
                }

                JSONObject metadata = metadataAVL.getMetadata(inputFilePath);
                if (metadata == null) {
                    AES aes = new AES();
                    metadata = aes.readMetadata(new File(inputFilePath));
                    String hash = metadataAVL.computeMetadataHash(metadata);
                    metadataAVL.insert(hash, inputFilePath, metadata);
                }
                String extension = metadata.getString("extension");

                AES aes = new AES();
                File encryptedFile = new File(inputFilePath);
                byte[] decryptedBytes = aes.decryptReadBytes(encryptedFile, password);

                File tempFile = File.createTempFile("decrypted_", "." + extension);
                tempFile.deleteOnExit();
                Files.write(tempFile.toPath(), decryptedBytes);
                Desktop.getDesktop().open(tempFile);

                outputArea.append("File decrypted and opened successfully\n");
                statusLabel.setText("Read successful");

                if (deleteOriginalCheckBox.isSelected()) {
                    SecureDelete.secureDelete(inputFilePath);
                    outputArea.append("Original encrypted file securely deleted\n");
                    statusLabel.setText("Read and deletion successful");
                }
            } catch (Exception ex) {
                outputArea.append("Read failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Read failed");
                showError("Read failed: " + ex.getMessage());
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainUI ui = new MainUI();
            ui.setVisible(true);
        });
    }
}