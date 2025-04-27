import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.json.JSONObject;

public class MainUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private MetadataAVLTree metadataAVL;
    private File[] enSelectedFiles;
    private File[] deSelectedFiles;

    public MainUI() {
        setTitle("File Encryption/Decryption with Metadata");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        metadataAVL = new MetadataAVLTree();
        metadataAVL.loadFromFile();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ใช้ CardLayout สำหรับสลับหน้า
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);

        // สร้างหน้า MainMenu
        JPanel mainMenuPanel = createMainMenuPanel();
        cardPanel.add(mainMenuPanel, "MainMenu");

        // สร้างหน้าสำหรับแต่ละหมวดหมู่
        JPanel encryptPanel = createEncryptPanel();
        cardPanel.add(encryptPanel, "Encrypt");

        JPanel decryptPanel = createDecryptPanel();
        cardPanel.add(decryptPanel, "Decrypt");

        JPanel metadataPanel = createMetadataPanel();
        cardPanel.add(metadataPanel, "Metadata");

        JPanel organizationPanel = createOrganizationPanel();
        cardPanel.add(organizationPanel, "Organization");

        // เพิ่ม cardPanel ลงใน JFrame
        add(cardPanel, BorderLayout.CENTER);

        // แสดงหน้า MainMenu เป็นหน้าเริ่มต้น
        cardLayout.show(cardPanel, "MainMenu");

        // บันทึก metadata เมื่อปิดหน้าต่าง
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                metadataAVL.saveToFile();
                System.exit(0);
            }
        });
    }

    // สร้างหน้า MainMenu
    private JPanel createMainMenuPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        panel.setBorder(new EmptyBorder(50, 50, 50, 50));

        JButton encryptButton = createStyledButton("Encrypt");
        encryptButton.addActionListener(e -> cardLayout.show(cardPanel, "Encrypt"));
        panel.add(encryptButton);

        JButton decryptButton = createStyledButton("Decrypt");
        decryptButton.addActionListener(e -> cardLayout.show(cardPanel, "Decrypt"));
        panel.add(decryptButton);

        JButton metadataButton = createStyledButton("Metadata");
        metadataButton.addActionListener(e -> cardLayout.show(cardPanel, "Metadata"));
        panel.add(metadataButton);

        JButton organizationButton = createStyledButton("Organization");
        organizationButton.addActionListener(e -> cardLayout.show(cardPanel, "Organization"));
        panel.add(organizationButton);

        JButton exitButton = createStyledButton("Exit");
        exitButton.addActionListener(e -> {
            metadataAVL.saveToFile();
            System.exit(0);
        });
        panel.add(exitButton);

        setFontForComponents(panel, new Font("Segoe UI", Font.BOLD, 16));
        return panel;
    }

    // สร้างหน้า Encrypt
    private JPanel createEncryptPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setName("EncryptPanel");

        JTextField inputFileField = new JTextField();
        JTextField outputPathField = new JTextField();
        JTextField newNameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JCheckBox showPasswordCheckBox = new JCheckBox("Show");
        JCheckBox deleteOriginalCheckBox = new JCheckBox("Delete original file");
        JTextArea outputArea = new JTextArea(10, 40);
        JLabel statusLabel = new JLabel("Ready");

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Input File:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputFileField.setEditable(false);
        inputPanel.add(inputFileField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        JButton browseInput = createStyledButton("Browse");
        browseInput.addActionListener(new BrowseFileAction(inputFileField, newNameField, true, false));
        inputPanel.add(browseInput, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Output Path:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(outputPathField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        JButton browseOutput = createStyledButton("Browse");
        browseOutput.addActionListener(new BrowseFileAction(outputPathField, newNameField, false, false));
        inputPanel.add(browseOutput, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("New Name (for single file):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        newNameField.setEnabled(false);
        inputPanel.add(newNameField, gbc);
        gbc.weightx = 0;

        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(passwordField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        inputPanel.add(showPasswordCheckBox, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        inputPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(confirmPasswordField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        inputPanel.add(new JLabel("Delete Original:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(deleteOriginalCheckBox, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new WrapLayout());
        JButton encryptFileButton = createStyledButton("Encrypt");
        encryptFileButton.addActionListener(new EncryptAction(inputFileField, outputPathField, newNameField, passwordField, confirmPasswordField, deleteOriginalCheckBox, outputArea, statusLabel));
        buttonPanel.add(encryptFileButton);

        JButton backButton = createStyledButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MainMenu"));
        buttonPanel.add(backButton);

        // Output Area
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        // Status Bar
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Show Password
        showPasswordCheckBox.addActionListener(e -> {
            boolean show = showPasswordCheckBox.isSelected();
            passwordField.setEchoChar(show ? (char) 0 : '•');
            confirmPasswordField.setEchoChar(show ? (char) 0 : '•');
        });

        // Add components
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        panel.add(buttonPanel, BorderLayout.PAGE_END);

        setFontForComponents(panel, new Font("Segoe UI", Font.PLAIN, 14));
        return panel;
    }

    // สร้างหน้า Decrypt
    private JPanel createDecryptPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setName("DecryptPanel");

        JTextField inputFileField = new JTextField();
        JTextField outputPathField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JCheckBox showPasswordCheckBox = new JCheckBox("Show");
        JCheckBox deleteOriginalCheckBox = new JCheckBox("Delete original file");
        JTextArea outputArea = new JTextArea(10, 40);
        JLabel statusLabel = new JLabel("Ready");

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Input File:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputFileField.setEditable(false);
        inputPanel.add(inputFileField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        JButton browseInput = createStyledButton("Browse");
        browseInput.addActionListener(new BrowseFileAction(inputFileField, null, true, true));
        inputPanel.add(browseInput, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Output Path:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(outputPathField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        JButton browseOutput = createStyledButton("Browse");
        browseOutput.addActionListener(new BrowseFileAction(outputPathField, null, false, true));
        inputPanel.add(browseOutput, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(passwordField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        inputPanel.add(showPasswordCheckBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Delete Original:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(deleteOriginalCheckBox, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new WrapLayout());
        JButton decryptButton = createStyledButton("Decrypt");
        decryptButton.addActionListener(new DecryptAction(inputFileField, outputPathField, passwordField, deleteOriginalCheckBox, outputArea, statusLabel));
        buttonPanel.add(decryptButton);

        JButton readButton = createStyledButton("Read");
        readButton.addActionListener(new ReadAction(inputFileField, passwordField, deleteOriginalCheckBox, outputArea, statusLabel));
        buttonPanel.add(readButton);

        JButton backButton = createStyledButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MainMenu"));
        buttonPanel.add(backButton);

        // Output Area
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        // Status Bar
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Show Password
        showPasswordCheckBox.addActionListener(e -> {
            passwordField.setEchoChar(showPasswordCheckBox.isSelected() ? (char) 0 : '•');
        });

        // Add components
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        panel.add(buttonPanel, BorderLayout.PAGE_END);

        setFontForComponents(panel, new Font("Segoe UI", Font.PLAIN, 14));
        return panel;
    }

    // สร้างหน้า Metadata
    private JPanel createMetadataPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JTextField inputFileField = new JTextField();
        JTextArea outputArea = new JTextArea(10, 40);
        JLabel statusLabel = new JLabel("Ready");

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Input File:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputFileField.setToolTipText("Select the file to read metadata");
        inputPanel.add(inputFileField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        JButton browseInput = createStyledButton("Browse");
        browseInput.addActionListener(new BrowseFileAction(inputFileField, null, true, false));
        inputPanel.add(browseInput, gbc);

        // Button Panel
        JPanel buttonPanel = new JPanel(new WrapLayout());
        JButton showMetadataButton = createStyledButton("Show Metadata");
        showMetadataButton.addActionListener(e -> {
            String inputFilePath = inputFileField.getText().trim();
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

        JButton clearMetadataButton = createStyledButton("Clear Metadata");
        clearMetadataButton.addActionListener(e -> {
            try {
                metadataAVL.cleanInvalidMetadata();
                outputArea.append("Cleaned invalid metadata\n");
                statusLabel.setText("Metadata cleaned");
                JOptionPane.showMessageDialog(MainUI.this, "Cleaned invalid metadata", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showError("Cleaning metadata failed: " + ex.getMessage());
            }
        });
        buttonPanel.add(clearMetadataButton);

        JButton backButton = createStyledButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MainMenu"));
        buttonPanel.add(backButton);

        // Output Area
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        // Status Bar
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Add components
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        panel.add(buttonPanel, BorderLayout.PAGE_END);

        setFontForComponents(panel, new Font("Segoe UI", Font.PLAIN, 14));
        return panel;
    }

    // สร้างหน้า Organization
    private JPanel createOrganizationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JTextArea outputArea = new JTextArea(10, 40);
        JLabel statusLabel = new JLabel("Ready");

        // Button Panel
        JPanel buttonPanel = new JPanel(new WrapLayout());
        JButton sortByTimestampButton = createStyledButton("Sort by Timestamp");
        sortByTimestampButton.addActionListener(e -> {
            try {
                List<JSONObject> sortedMetadata = metadataAVL.getSortedByTimestamp();
                displayMetadataList(sortedMetadata, "Metadata Sorted by Timestamp");
                outputArea.append("Displayed metadata sorted by timestamp\n");
                statusLabel.setText("Sorted by timestamp");
            } catch (Exception ex) {
                showError("Failed to sort metadata: " + ex.getMessage());
            }
        });
        buttonPanel.add(sortByTimestampButton);

        JButton sortBySizeButton = createStyledButton("Sort by Size");
        sortBySizeButton.addActionListener(e -> {
            try {
                List<JSONObject> sortedMetadata = metadataAVL.getSortedBySize();
                displayMetadataList(sortedMetadata, "Metadata Sorted by Size");
                outputArea.append("Displayed metadata sorted by size\n");
                statusLabel.setText("Sorted by size");
            } catch (Exception ex) {
                showError("Failed to sort metadata: " + ex.getMessage());
            }
        });
        buttonPanel.add(sortBySizeButton);

        JButton searchRangeButton = createStyledButton("Search Range");
        searchRangeButton.addActionListener(e -> {
            JFrame searchFrame = new JFrame("Search Metadata by Range");
            searchFrame.setSize(400, 300);
            searchFrame.setLocationRelativeTo(MainUI.this);
            JPanel searchPanel = new JPanel(new GridLayout(5, 2, 5, 5));
            JTextField minSizeField = new JTextField();
            JTextField maxSizeField = new JTextField();
            JTextField startTimeField = new JTextField();
            JTextField endTimeField = new JTextField();
            searchPanel.add(new JLabel("Min Size (bytes):"));
            searchPanel.add(minSizeField);
            searchPanel.add(new JLabel("Max Size (bytes):"));
            searchPanel.add(maxSizeField);
            searchPanel.add(new JLabel("Start Timestamp (yyyy-MM-dd'T'HH:mm:ss):"));
            searchPanel.add(startTimeField);
            searchPanel.add(new JLabel("End Timestamp (yyyy-MM-dd'T'HH:mm:ss):"));
            searchPanel.add(endTimeField);
            JButton searchButton = new JButton("Search");
            searchButton.addActionListener(se -> {
                try {
                    List<JSONObject> results = new ArrayList<>();
                    if (!minSizeField.getText().isEmpty() && !maxSizeField.getText().isEmpty()) {
                        long minSize = Long.parseLong(minSizeField.getText());
                        long maxSize = Long.parseLong(maxSizeField.getText());
                        results.addAll(metadataAVL.findBySizeRange(minSize, maxSize));
                    }
                    if (!startTimeField.getText().isEmpty() && !endTimeField.getText().isEmpty()) {
                        results.addAll(metadataAVL.findByTimestampRange(startTimeField.getText(), endTimeField.getText()));
                    }
                    displayMetadataList(results, "Search Results");
                    outputArea.append("Search completed\n");
                } catch (Exception ex) {
                    showError("Search failed: " + ex.getMessage());
                }
            });
            searchPanel.add(searchButton);
            searchFrame.add(searchPanel);
            searchFrame.setVisible(true);
        });
        buttonPanel.add(searchRangeButton);

        JButton complexSearchButton = createStyledButton("Complex Search");
        complexSearchButton.addActionListener(e -> {
            JFrame searchFrame = new JFrame("Complex Metadata Search");
            searchFrame.setSize(400, 300);
            searchFrame.setLocationRelativeTo(MainUI.this);
            JPanel searchPanel = new JPanel(new GridLayout(4, 2, 5, 5));
            JTextField extensionField = new JTextField();
            JTextField minSizeField = new JTextField();
            JTextField maxSizeField = new JTextField();
            searchPanel.add(new JLabel("Extension:"));
            searchPanel.add(extensionField);
            searchPanel.add(new JLabel("Min Size (bytes):"));
            searchPanel.add(minSizeField);
            searchPanel.add(new JLabel("Max Size (bytes):"));
            searchPanel.add(maxSizeField);
            JButton searchButton = new JButton("Search");
            searchButton.addActionListener(se -> {
                try {
                    String extension = extensionField.getText().isEmpty() ? null : extensionField.getText();
                    Long minSize = minSizeField.getText().isEmpty() ? null : Long.parseLong(minSizeField.getText());
                    Long maxSize = maxSizeField.getText().isEmpty() ? null : Long.parseLong(maxSizeField.getText());
                    List<JSONObject> results = metadataAVL.findByComplexCondition(extension, minSize, maxSize);
                    displayMetadataList(results, "Complex Search Results");
                    outputArea.append("Complex search completed\n");
                } catch (Exception ex) {
                    showError("Complex search failed: " + ex.getMessage());
                }
            });
            searchPanel.add(searchButton);
            searchFrame.add(searchPanel);
            searchFrame.setVisible(true);
        });
        buttonPanel.add(complexSearchButton);

        JButton summarizeButton = createStyledButton("Summarize");
        summarizeButton.addActionListener(e -> {
            try {
                Map<String, Integer> summary = metadataAVL.summarizeByExtension();
                JFrame summaryFrame = new JFrame("Metadata Summary");
                summaryFrame.setSize(400, 300);
                JTextArea summaryArea = new JTextArea();
                summaryArea.setEditable(false);
                for (Map.Entry<String, Integer> entry : summary.entrySet()) {
                    summaryArea.append("Extension: " + entry.getKey() + ", Count: " + entry.getValue() + "\n");
                }
                summaryFrame.add(new JScrollPane(summaryArea));
                summaryFrame.setVisible(true);
                outputArea.append("Summary displayed\n");
                statusLabel.setText("Summary displayed");
            } catch (Exception ex) {
                showError("Summary failed: " + ex.getMessage());
            }
        });
        buttonPanel.add(summarizeButton);

        JButton backButton = createStyledButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MainMenu"));
        buttonPanel.add(backButton);

        // Output Area
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        // Status Bar
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Add components
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        panel.add(buttonPanel, BorderLayout.PAGE_END);

        setFontForComponents(panel, new Font("Segoe UI", Font.PLAIN, 14));
        return panel;
    }

    // คลาส Action สำหรับ Encrypt
    private class EncryptAction implements ActionListener {
        private JTextField inputFileField, outputPathField, newNameField;
        private JPasswordField passwordField, confirmPasswordField;
        private JCheckBox deleteOriginalCheckBox;
        private JTextArea outputArea;
        private JLabel statusLabel;
    
        public EncryptAction(JTextField inputFileField, JTextField outputPathField, JTextField newNameField,
                             JPasswordField passwordField, JPasswordField confirmPasswordField,
                             JCheckBox deleteOriginalCheckBox, JTextArea outputArea, JLabel statusLabel) {
            this.inputFileField = inputFileField;
            this.outputPathField = outputPathField;
            this.newNameField = newNameField;
            this.passwordField = passwordField;
            this.confirmPasswordField = confirmPasswordField;
            this.deleteOriginalCheckBox = deleteOriginalCheckBox;
            this.outputArea = outputArea;
            this.statusLabel = statusLabel;
        }
    
        @Override
        public void actionPerformed(ActionEvent e) {
            String inputFileText = inputFileField.getText().trim();
            String outputFilePath = outputPathField.getText().trim();
            String outputFileName = newNameField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            char[] confirmPasswordChars = confirmPasswordField.getPassword();
            String password = new String(passwordChars);
            String confirmPassword = new String(confirmPasswordChars);
    
            try {
                if (inputFileText.isEmpty() || outputFilePath.isEmpty()) {
                    showError("Input file(s) and output path are required");
                    return;
                }
                if (password.length() < 8) {
                    showError("Password must be at least 8 characters long");
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    showError("Passwords do not match");
                    return;
                }
    
                File[] inputFiles;
                boolean isSingleFile = !inputFileText.contains("files selected");
    
                if (isSingleFile) {
                    inputFiles = new File[]{new File(inputFileText)};
                    if (!outputFileName.isEmpty() && !outputFileName.matches("[^<>:\"/\\\\|?*]+")) {
                        showError("Output file name contains invalid characters");
                        return;
                    }
                } else {
                    if (enSelectedFiles == null || enSelectedFiles.length == 0) {
                        showError("No files selected");
                        return;
                    }
                    inputFiles = enSelectedFiles;
                }
    
                AES aes = new AES();
                byte[] salt = aes.generateSalt();
                boolean allSuccessful = true;
    
                for (File inputFile : inputFiles) {
                    String fullOutputPath;
                    if (isSingleFile) {
                        fullOutputPath = outputFileName.isEmpty() ?
                                Paths.get(outputFilePath, inputFile.getName()).toString() :
                                Paths.get(outputFilePath, outputFileName).toString();
                    } else {
                        String fileNameWithoutExt = inputFile.getName().substring(0, inputFile.getName().lastIndexOf('.'));
                        fullOutputPath = Paths.get(outputFilePath, fileNameWithoutExt).toString();
                    }
    
                    File outputEncryptedFile = new File(fullOutputPath + ".enc");
                    if (outputEncryptedFile.exists()) {
                        int overwriteResult = JOptionPane.showConfirmDialog(MainUI.this,
                                "Output file " + outputEncryptedFile.getName() + " already exists. Overwrite?",
                                "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                        if (overwriteResult != JOptionPane.YES_OPTION) {
                            outputArea.append("Skipped encryption for " + inputFile.getName() + "\n");
                            allSuccessful = false;
                            continue;
                        }
                    }
    
                    JSONObject metadata = new JSONObject();
                    metadata.put("extension", inputFile.getName().substring(inputFile.getName().lastIndexOf('.') + 1));
                    metadata.put("originalName", inputFile.getName());
                    metadata.put("size", inputFile.length());
                    metadata.put("timestamp", LocalDateTime.now().toString());
    
                    try {
                        aes.encryptFile(inputFile, new File(fullOutputPath), password, salt, metadata);
                        String hash = metadataAVL.computeMetadataHash(metadata);
                        metadataAVL.insert(hash, fullOutputPath + ".enc", metadata);
                        outputArea.append("Encrypted " + inputFile.getName() + " to " + fullOutputPath + ".enc\n");
    
                        if (deleteOriginalCheckBox.isSelected()) {
                            SecureDelete.secureDelete(inputFile.getAbsolutePath());
                            outputArea.append("Original file " + inputFile.getName() + " securely deleted\n");
                        }
                    } catch (Exception ex) {
                        outputArea.append("Failed to encrypt " + inputFile.getName() + ": " + ex.getMessage() + "\n");
                        allSuccessful = false;
                    }
                }
    
                statusLabel.setText(allSuccessful ? "Encryption completed" : "Encryption completed with errors");
                JOptionPane.showMessageDialog(MainUI.this, allSuccessful ? "All files encrypted successfully" :
                        "Some files failed to encrypt. Check log for details", "Result", JOptionPane.INFORMATION_MESSAGE);
    
            } catch (Exception ex) {
                outputArea.append("Encryption failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Encryption failed");
                showError("Encryption failed: " + ex.getMessage());
            } finally {
                Arrays.fill(passwordChars, '\0');
                Arrays.fill(confirmPasswordChars, '\0');
            }
        }
    }

    // คลาส Action สำหรับ Decrypt
    private class DecryptAction implements ActionListener {
        private JTextField inputFileField, outputPathField;
        private JPasswordField passwordField;
        private JCheckBox deleteOriginalCheckBox;
        private JTextArea outputArea;
        private JLabel statusLabel;

        public DecryptAction(JTextField inputFileField, JTextField outputPathField, JPasswordField passwordField,
                            JCheckBox deleteOriginalCheckBox, JTextArea outputArea, JLabel statusLabel) {
            this.inputFileField = inputFileField;
            this.outputPathField = outputPathField;
            this.passwordField = passwordField;
            this.deleteOriginalCheckBox = deleteOriginalCheckBox;
            this.outputArea = outputArea;
            this.statusLabel = statusLabel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String inputFileText = inputFileField.getText().trim();
            String outputFilePath = outputPathField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);

            try {
                if (inputFileText.isEmpty() || outputFilePath.isEmpty()) {
                    showError("Input file(s) and output path are required");
                    return;
                }
                if (password.length() < 8) {
                    showError("Password must be at least 8 characters long");
                    return;
                }

                File[] inputFiles;
                boolean isSingleFile = !inputFileText.contains("files selected");

                if (isSingleFile) {
                    inputFiles = new File[]{new File(inputFileText)};
                    if (!inputFiles[0].getName().toLowerCase().endsWith(".enc")) {
                        showError("Input file must have .enc extension");
                        return;
                    }
                } else {
                    if (deSelectedFiles == null || deSelectedFiles.length == 0) {
                        showError("No files selected");
                        return;
                    }
                    inputFiles = deSelectedFiles;
                }

                AES aes = new AES();
                boolean allSuccessful = true;

                for (File inputFile : inputFiles) {
                    JSONObject metadata = metadataAVL.getMetadata(inputFile.getAbsolutePath());
                    if (metadata == null) {
                        metadata = aes.readMetadata(inputFile);
                        String hash = metadataAVL.computeMetadataHash(metadata);
                        metadataAVL.insert(hash, inputFile.getAbsolutePath(), metadata);
                    }
                    String originalName = metadata.getString("originalName");

                    String fullOutputPath = Paths.get(outputFilePath, originalName).toString();
                    File outputFile = new File(fullOutputPath);
                    if (outputFile.exists()) {
                        int result = JOptionPane.showConfirmDialog(MainUI.this,
                                "Output file " + outputFile.getName() + " already exists. Overwrite?",
                                "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                        if (result != JOptionPane.YES_OPTION) {
                            outputArea.append("Skipped decryption for " + inputFile.getName() + "\n");
                            allSuccessful = false;
                            continue;
                        }
                    }

                    try {
                        aes.decryptFile(inputFile, outputFile, password);
                        outputArea.append("Decrypted " + inputFile.getName() + " to " + fullOutputPath + "\n");

                        if (deleteOriginalCheckBox.isSelected()) {
                            SecureDelete.secureDelete(inputFile.getAbsolutePath());
                            outputArea.append("Original encrypted file " + inputFile.getName() + " securely deleted\n");
                        }
                    } catch (Exception ex) {
                        outputArea.append("Failed to decrypt " + inputFile.getName() + ": " + ex.getMessage() + "\n");
                        allSuccessful = false;
                    }
                }

                statusLabel.setText(allSuccessful ? "Decryption completed" : "Decryption completed with errors");
                JOptionPane.showMessageDialog(MainUI.this, allSuccessful ? "All files decrypted successfully" :
                        "Some files failed to decrypt. Check log for details", "Result", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                outputArea.append("Decryption failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Decryption failed");
                showError("Decryption failed: " + ex.getMessage());
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    // คลาส Action สำหรับ Read
    private class ReadAction implements ActionListener {
        private JTextField inputFileField;
        private JPasswordField passwordField;
        private JCheckBox deleteOriginalCheckBox;
        private JTextArea outputArea;
        private JLabel statusLabel;

        public ReadAction(JTextField inputFileField, JPasswordField passwordField,
                        JCheckBox deleteOriginalCheckBox, JTextArea outputArea, JLabel statusLabel) {
            this.inputFileField = inputFileField;
            this.passwordField = passwordField;
            this.deleteOriginalCheckBox = deleteOriginalCheckBox;
            this.outputArea = outputArea;
            this.statusLabel = statusLabel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String inputFileText = inputFileField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);

            try {
                if (inputFileText.isEmpty()) {
                    showError("Input file(s) is required");
                    return;
                }
                if (password.length() < 8) {
                    showError("Password must be at least 8 characters long");
                    return;
                }

                File[] inputFiles;
                boolean isSingleFile = !inputFileText.contains("files selected");

                if (isSingleFile) {
                    inputFiles = new File[]{new File(inputFileText)};
                    if (!inputFiles[0].getName().toLowerCase().endsWith(".enc")) {
                        showError("Input file must have .enc extension");
                        return;
                    }
                } else {
                    if (deSelectedFiles == null || deSelectedFiles.length == 0) {
                        showError("No files selected");
                        return;
                    }
                    inputFiles = deSelectedFiles;
                }

                AES aes = new AES();
                boolean allSuccessful = true;

                for (File inputFile : inputFiles) {
                    JSONObject metadata = metadataAVL.getMetadata(inputFile.getAbsolutePath());
                    if (metadata == null) {
                        metadata = aes.readMetadata(inputFile);
                        String hash = metadataAVL.computeMetadataHash(metadata);
                        metadataAVL.insert(hash, inputFile.getAbsolutePath(), metadata);
                    }
                    String extension = metadata.getString("extension");

                    try {
                        byte[] decryptedBytes = aes.decryptReadBytes(inputFile, password);
                        File tempFile = File.createTempFile("decrypted_", "." + extension);
                        tempFile.deleteOnExit();
                        Files.write(tempFile.toPath(), decryptedBytes);
                        Desktop.getDesktop().open(tempFile);
                        outputArea.append("Opened " + inputFile.getName() + "\n");

                        if (deleteOriginalCheckBox.isSelected()) {
                            SecureDelete.secureDelete(inputFile.getAbsolutePath());
                            outputArea.append("Original encrypted file " + inputFile.getName() + " securely deleted\n");
                        }
                    } catch (Exception ex) {
                        outputArea.append("Failed to read " + inputFile.getName() + ": " + ex.getMessage() + "\n");
                        allSuccessful = false;
                    }
                }

                statusLabel.setText(allSuccessful ? "Read completed" : "Read completed with errors");
                JOptionPane.showMessageDialog(MainUI.this, allSuccessful ? "All files read successfully" :
                        "Some files failed to read. Check log for details", "Result", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                outputArea.append("Read failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Read failed");
                showError("Read failed: " + ex.getMessage());
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    // เมธอดช่วยเหลือ
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

    private void displayMetadataList(List<JSONObject> metadataList, String title) {
        JFrame frame = new JFrame(title);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(MainUI.this);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14));
        for (JSONObject metadata : metadataList) {
            area.append("Name: " + metadata.getString("originalName") + "\n");
            area.append("Extension: " + metadata.getString("extension") + "\n");
            area.append("Size: " + metadata.getLong("size") + " bytes\n");
            area.append("Timestamp: " + metadata.getString("timestamp") + "\n");
            area.append("------------------------\n");
        }
        frame.add(new JScrollPane(area));
        frame.setVisible(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(MainUI.this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private class BrowseFileAction implements ActionListener {
        private JTextField targetField;
        private JTextField newNameField; // อาจเป็น null สำหรับหน้า Decrypt
        private boolean isInput;
        private boolean isDecrypt; // บ่งชี้ว่าเป็นหน้า Decrypt หรือไม่

        public BrowseFileAction(JTextField targetField, JTextField newNameField, boolean isInput, boolean isDecrypt) {
            this.targetField = targetField;
            this.newNameField = newNameField;
            this.isInput = isInput;
            this.isDecrypt = isDecrypt;
        }

        public BrowseFileAction(JTextField targetField, boolean isInput, boolean isDecrypt) {
            this(targetField, null, isInput, isDecrypt);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            if (isInput) {
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setMultiSelectionEnabled(true);
                // จำกัดไฟล์ .enc สำหรับหน้า Decrypt
                if (isDecrypt) {
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return f.isDirectory() || f.getName().toLowerCase().endsWith(".enc");
                        }
                        @Override
                        public String getDescription() {
                            return "Encrypted Files (*.enc)";
                        }
                    });
                }
            } else {
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setMultiSelectionEnabled(false);
            }
            int result = fileChooser.showOpenDialog(MainUI.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                if (isInput) {
                    File[] selected = fileChooser.getSelectedFiles();
                    // ตรวจสอบนามสกุล .enc สำหรับหน้า Decrypt
                    if (isDecrypt) {
                        for (File file : selected) {
                            if (!file.getName().toLowerCase().endsWith(".enc")) {
                                showError("All selected files must have .enc extension");
                                return;
                            }
                        }
                        deSelectedFiles = selected; // บันทึกสำหรับหน้า Decrypt
                    } else {
                        enSelectedFiles = selected; // บันทึกสำหรับหน้า Encrypt
                    }
                    if (selected.length > 1) {
                        targetField.setText(selected.length + " files selected");
                        if (newNameField != null) {
                            newNameField.setEnabled(false); // ปิด newNameField สำหรับหลายไฟล์ในหน้า Encrypt
                        }
                    } else {
                        targetField.setText(selected[0].getAbsolutePath());
                        if (newNameField != null) {
                            newNameField.setEnabled(true); // เปิด newNameField สำหรับไฟล์เดียวในหน้า Encrypt
                        }
                    }
                } else {
                    File selectedFile = fileChooser.getSelectedFile();
                    targetField.setText(selectedFile.getAbsolutePath());
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainUI ui = new MainUI();
            ui.setVisible(true);
        });
    }
}