import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import org.json.JSONException;
import org.json.JSONObject;

public class MainUI extends JFrame {
    //initializing variables
    private CardLayout cardLayout;
    private JPanel cardPanel;
    private LockerManager lockerManager;
    private Locker currentLocker;
    private File[] enSelectedFiles;
    private File[] deSelectedFiles;
    private JLabel currentLockerLabel;

    public MainUI() {
        //setting up the main UI
        setTitle("ModLocker");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        //setting up the locker manager
        try {
            lockerManager = new LockerManager();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null, "Failed to initialize Locker Manager: " + e.getMessage(),
                    "Initialization Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        //setting up the look and feel of the UI
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        //setting up the main layout
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);

        JPanel mainMenuPanel = createMainMenuPanel();
        cardPanel.add(mainMenuPanel, "MainMenu");

        JPanel lockerPanel = createLockerPanel();
        cardPanel.add(lockerPanel, "Locker");

        JPanel encryptPanel = createEncryptPanel();
        cardPanel.add(encryptPanel, "Encrypt");

        JPanel decryptPanel = createDecryptPanel();
        cardPanel.add(decryptPanel, "Decrypt");

        JPanel organizationPanel = createOrganizationPanel();
        cardPanel.add(organizationPanel, "Organization");

        add(cardPanel, BorderLayout.CENTER);
        cardLayout.show(cardPanel, "MainMenu");

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    // Method to set font for all components in a container
    private JPanel createStandardPanel(Component inputComponent, JTextArea outputArea, JLabel statusLabel, List<JButton> buttons) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JPanel buttonPanel = new JPanel(new WrapLayout());
        buttons.forEach(buttonPanel::add);
        panel.add(inputComponent, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        panel.add(buttonPanel, BorderLayout.PAGE_END);
        setFontForComponents(panel, new Font("Segoe UI", Font.PLAIN, 14));
        return panel;
    }

    //Function to create a main menu panel
    private JPanel createMainMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new java.awt.Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Modlocker - File Encryption/Decryption", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new java.awt.Color(50, 50, 50));

        currentLockerLabel = new JLabel("Current Locker: None", JLabel.CENTER);
        currentLockerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        currentLockerLabel.setForeground(new java.awt.Color(100, 100, 100));

        JButton lockerButton = createStyledButton("Manage Lockers");
        lockerButton.addActionListener(e -> cardLayout.show(cardPanel, "Locker"));

        JButton encryptButton = createStyledButton("Encrypt Files");
        // Check if a locker is selected before allowing encryption
        encryptButton.addActionListener(e -> {
            if (currentLocker == null) {
                showError("Please select a Locker first");
                cardLayout.show(cardPanel, "Locker");
            } else {
                cardLayout.show(cardPanel, "Encrypt");
            }
        });

        JButton decryptButton = createStyledButton("Decrypt Files");
        // Check if a locker is selected before allowing decryption
        decryptButton.addActionListener(e -> {
            if (currentLocker == null) {
                showError("Please select a Locker first");
                cardLayout.show(cardPanel, "Locker");
            } else {
                cardLayout.show(cardPanel, "Decrypt");
            }
        });

        JButton organizationButton = createStyledButton("Organize Files");
        // Check if a locker is selected before allowing organization
        organizationButton.addActionListener(e -> {
            if (currentLocker == null) {
                showError("Please select a Locker first");
                cardLayout.show(cardPanel, "Locker");
            } else {
                cardLayout.show(cardPanel, "Organization");
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridy = 1;
        panel.add(currentLockerLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(lockerButton, gbc);
        gbc.gridx = 1;
        panel.add(encryptButton, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(decryptButton, gbc);
        gbc.gridx = 1;
        panel.add(organizationButton, gbc);

        setFontForComponents(panel, new Font("Segoe UI", Font.PLAIN, 14));
        return panel;
    }

    private JPanel createLockerPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField lockerNameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JCheckBox showPasswordCheckBox = new JCheckBox("Show");
        JComboBox<String> lockerComboBox = new JComboBox<>();
        JTextArea outputArea = new JTextArea(10, 40);
        JLabel statusLabel = new JLabel("Ready");

        for (String lockerName : lockerManager.getLockers().keySet()) {
            lockerComboBox.addItem(lockerName);
        }

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Locker Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(lockerNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(passwordField, gbc);
        gbc.gridx = 2;
        inputPanel.add(showPasswordCheckBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Select Locker:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(lockerComboBox, gbc);

        List<JButton> buttons = new ArrayList<>();
        JButton createLockerButton = createStyledButton("Create Locker");
        createLockerButton.addActionListener(e -> {
            String name = lockerNameField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);
            // Check the password length and if the locker name is empty
            try {
                if (name.isEmpty() || password.isEmpty()) {
                    showError("Locker name and password are required");
                    return;
                }
                if (password.length() < 8) {
                    showError("Password must be at least 8 characters long");
                    return;
                }
                lockerManager.createLocker(name, password);
                lockerComboBox.addItem(name);
                outputArea.append("Created locker: " + name + "\n");
                statusLabel.setText("Locker created");
                lockerNameField.setText("");
                passwordField.setText("");
            } catch (Exception ex) {
                handleError("Locker creation", ex.getMessage(), outputArea, statusLabel);
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        });
        buttons.add(createLockerButton);

        JButton selectLockerButton = createStyledButton("Select Locker");
        selectLockerButton.addActionListener(e -> {
            String name = (String) lockerComboBox.getSelectedItem();
            char[] passwordChars = passwordField.getPassword();
            String password = new String(passwordChars);
            // Check if the locker is selected
            try {
                if (name == null) {
                    showError("No locker selected");
                    return;
                }
                currentLocker = lockerManager.getLocker(name, password);
                outputArea.append("Selected locker: " + name + "\n");
                statusLabel.setText("Locker selected");
                currentLockerLabel.setText("Current Locker: " + name);
                cardLayout.show(cardPanel, "MainMenu");
                passwordField.setText("");
            } catch (Exception ex) {
                handleError("Locker selection", ex.getMessage(), outputArea, statusLabel);
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        });
        buttons.add(selectLockerButton);

        JButton backButton = createStyledButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MainMenu"));
        buttons.add(backButton);

        // Adding action listener to show/hide password
        showPasswordCheckBox.addActionListener(e -> {
            passwordField.setEchoChar(showPasswordCheckBox.isSelected() ? (char) 0 : '•');
        });

        return createStandardPanel(inputPanel, outputArea, statusLabel, buttons);
    }

    // Function to create an encrypt panel
    private JPanel createEncryptPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField inputFileField = new JTextField();
        JTextField outputPathField = new JTextField();
        JCheckBox deleteOriginalCheckBox = new JCheckBox("Delete original file");
        JTextArea outputArea = new JTextArea(10, 40);
        JLabel statusLabel = new JLabel("Ready");

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Input File:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputFileField.setEditable(false);
        inputPanel.add(inputFileField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        JButton browseInput = createStyledButton("Browse");
        browseInput.addActionListener(new BrowseFileAction(inputFileField, null, true, false));
        inputPanel.add(browseInput, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Output Path:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(outputPathField, gbc);
        gbc.weightx = 0;
        gbc.gridx = 2;
        JButton browseOutput = createStyledButton("Browse");
        browseOutput.addActionListener(new BrowseFileAction(outputPathField, null, false, false));
        inputPanel.add(browseOutput, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Delete Original:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(deleteOriginalCheckBox, gbc);

        List<JButton> buttons = new ArrayList<>();
        JButton encryptFileButton = createStyledButton("Encrypt");
        // Add action listener to encrypt the file
        encryptFileButton.addActionListener(new EncryptAction(inputFileField, outputPathField, deleteOriginalCheckBox, outputArea, statusLabel));
        buttons.add(encryptFileButton);

        JButton backButton = createStyledButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MainMenu"));
        buttons.add(backButton);

        return createStandardPanel(inputPanel, outputArea, statusLabel, buttons);
    }

    // Function to create a decrypt panel
    private JPanel createDecryptPanel() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField inputFileField = new JTextField();
        JTextField outputPathField = new JTextField();
        JCheckBox deleteOriginalCheckBox = new JCheckBox("Delete original file");
        JTextArea outputArea = new JTextArea(10, 40);
        JLabel statusLabel = new JLabel("Ready");

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
        inputPanel.add(new JLabel("Delete Original:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(deleteOriginalCheckBox, gbc);

        List<JButton> buttons = new ArrayList<>();
        JButton decryptButton = createStyledButton("Decrypt");
        decryptButton.addActionListener(new DecryptAction(inputFileField, outputPathField, deleteOriginalCheckBox, outputArea, statusLabel));
        buttons.add(decryptButton);

        JButton readButton = createStyledButton("Read");
        readButton.addActionListener(new ReadAction(inputFileField, deleteOriginalCheckBox, outputArea, statusLabel));
        buttons.add(readButton);

        JButton backButton = createStyledButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MainMenu"));
        buttons.add(backButton);

        return createStandardPanel(inputPanel, outputArea, statusLabel, buttons);
    }

    // Function to create an organization panel
    private JPanel createOrganizationPanel() {
        JTextArea outputArea = new JTextArea(10, 40);
        JLabel statusLabel = new JLabel("Ready");
        List<JButton> buttons = new ArrayList<>();

        JButton sortByTimestampButton = createStyledButton("Sort by Timestamp");
        // Add action listener to sort metadata by timestamp
        sortByTimestampButton.addActionListener(e -> {
            try {
                if (currentLocker == null) {
                    showError("No locker selected");
                    return;
                }
                List<JSONObject> sortedMetadata = currentLocker.getMetadataTree().getSortedByTimestamp();
                displayMetadataList(sortedMetadata, "Metadata Sorted by Timestamp");
                outputArea.append("Displayed metadata sorted by timestamp\n");
                statusLabel.setText("Sorted by timestamp");
            } catch (Exception ex) {
                handleError("Sorting metadata", ex.getMessage(), outputArea, statusLabel);
            }
        });
        buttons.add(sortByTimestampButton);

        JButton sortBySizeButton = createStyledButton("Sort by Size");
        // Add action listener to sort metadata by size
        sortBySizeButton.addActionListener(e -> {
            try {
                if (currentLocker == null) {
                    showError("No locker selected");
                    return;
                }
                List<JSONObject> sortedMetadata = currentLocker.getMetadataTree().getSortedBySize();
                displayMetadataList(sortedMetadata, "Metadata Sorted by Size");
                outputArea.append("Displayed metadata sorted by size\n");
                statusLabel.setText("Sorted by size");
            } catch (Exception ex) {
                handleError("Sorting metadata", ex.getMessage(), outputArea, statusLabel);
            }
        });
        buttons.add(sortBySizeButton);

        JButton searchRangeButton = createStyledButton("Filter");
        searchRangeButton.addActionListener(e -> {
            if (currentLocker == null) {
                showError("No locker selected");
                return;
            }
            JFrame searchFrame = new JFrame("Search Metadata");
            searchFrame.setSize(400, 300);
            searchFrame.setLocationRelativeTo(MainUI.this);
            JPanel searchPanel = new JPanel(new GridLayout(6, 2, 5, 5));

            JTextField extensionField = new JTextField();
            JTextField minSizeField = new JTextField();
            JTextField maxSizeField = new JTextField();
            JTextField startDateField = new JTextField();
            JTextField endDateField = new JTextField();

            searchPanel.add(new JLabel("Extension:"));
            searchPanel.add(extensionField);
            searchPanel.add(new JLabel("Min Size (bytes):"));
            searchPanel.add(minSizeField);
            searchPanel.add(new JLabel("Max Size (bytes):"));
            searchPanel.add(maxSizeField);
            searchPanel.add(new JLabel("Start Date (yyyy-MM-dd):"));
            searchPanel.add(startDateField);
            searchPanel.add(new JLabel("End Date (yyyy-MM-dd):"));
            searchPanel.add(endDateField);

            JButton searchButton = new JButton("Search");
            // Add action listener to search metadata by range
            searchButton.addActionListener(se -> {
                try {
                    String extension = extensionField.getText().isEmpty() ? null : extensionField.getText();
                    Long minSize = minSizeField.getText().isEmpty() ? null : Long.valueOf(minSizeField.getText());
                    Long maxSize = maxSizeField.getText().isEmpty() ? null : Long.valueOf(maxSizeField.getText());
                    Date startTime = startDateField.getText().isEmpty() ? null : new java.text.SimpleDateFormat("yyyy-MM-dd").parse(startDateField.getText());
                    Date endTime = endDateField.getText().isEmpty() ? null : new java.text.SimpleDateFormat("yyyy-MM-dd").parse(endDateField.getText());

                    List<JSONObject> results = currentLocker.getMetadataTree().filterMetadata(extension, minSize, maxSize, startTime, endTime);
                    displayMetadataList(results, "Search Results");
                } catch (NumberFormatException ex) {
                    showError("Invalid size format. Please enter a valid number.");
                } catch (java.text.ParseException ex) {
                    showError("Invalid date format. Use yyyy-MM-dd.");
                }
            });
            searchPanel.add(searchButton);

            searchFrame.add(searchPanel);
            searchFrame.setVisible(true);
        });
        buttons.add(searchRangeButton);

        JButton summarizeButton = createStyledButton("Summarize");
        // Add action listener to summarize metadata by extension
        summarizeButton.addActionListener(e -> {
            try {
                if (currentLocker == null) {
                    showError("No locker selected");
                    return;
                }
                Map<String, Integer> summary = currentLocker.getMetadataTree().summarizeByExtension();
                JFrame summaryFrame = new JFrame("Metadata Summary");
                summaryFrame.setSize(400, 300);
                summaryFrame.setLocationRelativeTo(MainUI.this);
                JTextArea summaryArea = new JTextArea();
                summaryArea.setEditable(false);
                for (Map.Entry<String, Integer> entry : summary.entrySet()) {
                    summaryArea.append("Extension: " + entry.getKey() + ", Count: " + entry.getValue() + "\n");
                }
                summaryFrame.add(new JScrollPane(summaryArea));
                summaryFrame.setVisible(true);
                outputArea.append("Summary displayed\n");
                statusLabel.setText("Summary displayed");
            } catch (HeadlessException ex) {
                handleError("Summary", ex.getMessage(), outputArea, statusLabel);
            }
        });
        buttons.add(summarizeButton);

        JButton clearMetadataButton = createStyledButton("Clear Metadata");
        // Add action listener to clear invalid metadata
        clearMetadataButton.addActionListener(e -> {
            try {
                if (currentLocker == null) {
                    showError("No locker selected");
                    return;
                }
                currentLocker.getMetadataTree().cleanInvalidMetadata();
                outputArea.append("Cleaned invalid metadata\n");
                statusLabel.setText("Metadata cleaned");
                JOptionPane.showMessageDialog(MainUI.this, "Cleaned invalid metadata", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (HeadlessException ex) {
                handleError("Cleaning metadata", ex.getMessage(), outputArea, statusLabel);
            }
        });
        buttons.add(clearMetadataButton);

        JButton backButton = createStyledButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "MainMenu"));
        buttons.add(backButton);

        return createStandardPanel(new JPanel(), outputArea, statusLabel, buttons);
    }

    private void handleError(String operation, String message, JTextArea outputArea, JLabel statusLabel) {
        outputArea.append(operation + " failed: " + message + "\n");
        statusLabel.setText(operation + " failed");
        showError(operation + " failed: " + message);
    }
    private class EncryptAction implements ActionListener {
        private JTextField inputFileField, outputPathField, newNameField;
        private JCheckBox deleteOriginalCheckBox;
        private JTextArea outputArea;
        private JLabel statusLabel;

        // Constructor for EncryptAction
        public EncryptAction(JTextField inputFileField, JTextField outputPathField, JCheckBox deleteOriginalCheckBox, JTextArea outputArea, JLabel statusLabel) {
            this.inputFileField = inputFileField;
            this.outputPathField = outputPathField;
            this.deleteOriginalCheckBox = deleteOriginalCheckBox;
            this.outputArea = outputArea;
            this.statusLabel = statusLabel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Get the input file and output path from the text fields
            String inputFileText = inputFileField.getText().trim();
            String outputFilePath = outputPathField.getText().trim();
            String outputFileName = newNameField != null ? newNameField.getText().trim() : "";

            // Check if the locker is selected and if the input file and output path are valid
            try {
                if (currentLocker == null) {
                    showError("No locker selected");
                    return;
                }
                if (inputFileText.isEmpty() || outputFilePath.isEmpty()) {
                    showError("Input file(s) and output path are required");
                    return;
                }

                File[] inputFiles;
                boolean isSingleFile = !inputFileText.contains("files selected");

                // Check if the input file is a single file or multiple files
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

                boolean allSuccessful = true;

                // Loop through each input file and encrypt it
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

                    // Check if the output file already exists and prompt for overwrite
                    File outputEncryptedFile = new File(fullOutputPath);
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

                    // Create metadata for the file
                    JSONObject metadata = new JSONObject();
                    metadata.put("extension", inputFile.getName().substring(inputFile.getName().lastIndexOf('.') + 1));
                    metadata.put("originalName", inputFile.getName());
                    metadata.put("size", inputFile.length());
                    metadata.put("timestamp", LocalDateTime.now().toString());

                    // Encrypt the file using the current locker
                    try {
                        currentLocker.encryptFile(inputFile, new File(fullOutputPath), metadata);
                        outputArea.append("Encrypted " + inputFile.getName() + " to " + fullOutputPath + ".enc\n");

                        if (deleteOriginalCheckBox.isSelected()) {
                            inputFile.delete();
                            outputArea.append("Original file " + inputFile.getName() + " deleted\n");
                        }
                    } catch (Exception ex) {
                        outputArea.append("Failed to encrypt " + inputFile.getName() + ": " + ex.getMessage() + "\n");
                        allSuccessful = false;
                    }
                }

                statusLabel.setText(allSuccessful ? "Encryption completed" : "Encryption completed with errors");
                JOptionPane.showMessageDialog(MainUI.this, allSuccessful ? "All files encrypted successfully" :
                        "Some files failed to encrypt. Check log for details", "Result", JOptionPane.INFORMATION_MESSAGE);

            } catch (HeadlessException | JSONException ex) {
                outputArea.append("Encryption failed: " + ex.getMessage() + "\n");
                statusLabel.setText("Encryption failed");
                showError("Encryption failed: " + ex.getMessage());
            }
        }
    }

    // Function to decrypt files
    private class DecryptAction implements ActionListener {
        private JTextField inputFileField, outputPathField;
        private JCheckBox deleteOriginalCheckBox;
        private JTextArea outputArea;
        private JLabel statusLabel;

        // Constructor for DecryptAction
        public DecryptAction(JTextField inputFileField, JTextField outputPathField,
                            JCheckBox deleteOriginalCheckBox, JTextArea outputArea, JLabel statusLabel) {
            this.inputFileField = inputFileField;
            this.outputPathField = outputPathField;
            this.deleteOriginalCheckBox = deleteOriginalCheckBox;
            this.outputArea = outputArea;
            this.statusLabel = statusLabel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String inputFileText = inputFileField.getText().trim();
            String outputFilePath = outputPathField.getText().trim();

            // Check if the locker is selected and if the input file and output path are valid
            try {
                if (currentLocker == null) {
                    showError("No locker selected");
                    return;
                }
                if (inputFileText.isEmpty() || outputFilePath.isEmpty()) {
                    showError("Input file(s) and output path are required");
                    return;
                }

                File[] inputFiles;
                boolean isSingleFile = !inputFileText.contains("files selected");

                // Check if the input file is a single file or multiple files
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

                boolean allSuccessful = true;

                // Loop through each input file and decrypt it
                for (File inputFile : inputFiles) {
                    JSONObject metadata = currentLocker.getMetadataTree().getMetadata(inputFile.getAbsolutePath());
                    if (metadata == null) { // If metadata is not found, read it from the file
                        String meatdataHash = new AES().hashMetadata(new AES().readMetadata(inputFile));
                        metadata = currentLocker.getMetadataTree().getMetadataByHash(meatdataHash);
                        if (metadata == null) {
                            showError("File is not associated with this Locker");
                            outputArea.append("No metadata found for: " + inputFile.getName() + "\n");
                            allSuccessful = false;
                            continue;
                        }
                    }
                    String originalName = metadata.getString("originalName");

                    // Check if the output file already exists and prompt for overwrite
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

                    // Decrypt the file using the current locker
                    try {
                        currentLocker.decryptFile(inputFile, outputFile);
                        outputArea.append("Decrypted " + inputFile.getName() + " to " + fullOutputPath + "\n");

                        if (deleteOriginalCheckBox.isSelected()) {
                            inputFile.delete();
                            outputArea.append("Original encrypted file " + inputFile.getName() + " deleted\n");
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
            }
        }
    }

    private class ReadAction implements ActionListener {
        private JTextField inputFileField;
        private JCheckBox deleteOriginalCheckBox;
        private JTextArea outputArea;
        private JLabel statusLabel;

        // Constructor for ReadAction
        public ReadAction(JTextField inputFileField, JCheckBox deleteOriginalCheckBox, JTextArea outputArea, JLabel statusLabel) {
            this.inputFileField = inputFileField;
            this.deleteOriginalCheckBox = deleteOriginalCheckBox;
            this.outputArea = outputArea;
            this.statusLabel = statusLabel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String inputFileText = inputFileField.getText().trim();

            // Check if the locker is selected and if the input file is valid
            try {
                if (currentLocker == null) {
                    showError("No locker selected");
                    return;
                }
                if (inputFileText.isEmpty()) {
                    showError("Input file(s) is required");
                    return;
                }

                File[] inputFiles;
                boolean isSingleFile = !inputFileText.contains("files selected");

                // Check if the input file is a single file or multiple files
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

                boolean allSuccessful = true;

                // Loop through each input file and read it
                for (File inputFile : inputFiles) {
                    JSONObject metadata = currentLocker.getMetadataTree().getMetadata(inputFile.getAbsolutePath());
                    if (metadata == null) { // If metadata is not found, read it from the file
                        String meatdataHash = new AES().hashMetadata(new AES().readMetadata(inputFile));
                        metadata = currentLocker.getMetadataTree().getMetadataByHash(meatdataHash);
                        if (metadata == null) {
                            showError("File is not associated with this Locker");
                            outputArea.append("No metadata found for: " + inputFile.getName() + "\n");
                            allSuccessful = false;
                            continue;
                        }
                    }
                    String extension = metadata.getString("extension");

                    // Check if the output file already exists and prompt for overwrite
                    try {
                        byte[] decryptedBytes = currentLocker.decryptReadBytes(inputFile);
                        File tempFile = File.createTempFile("decrypted_", "." + extension);
                        tempFile.deleteOnExit();    // Ensure the temp file is deleted on exit
                        Files.write(tempFile.toPath(), decryptedBytes);
                        java.awt.Desktop.getDesktop().open(tempFile); // Open the decrypted file with the default application
                        outputArea.append("Opened " + inputFile.getName() + "\n");

                        if (deleteOriginalCheckBox.isSelected()) {
                            inputFile.delete();
                            outputArea.append("Original encrypted file " + inputFile.getName() + " deleted\n");
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
            }
        }
    }

    // Function to create a browse file action
    private class BrowseFileAction implements ActionListener {
        private JTextField textField;
        private JTextField newNameField;
        private boolean isInput;
        private boolean isDecrypt;

        // Constructor for BrowseFileAction
        public BrowseFileAction(JTextField textField, JTextField newNameField, boolean isInput, boolean isDecrypt) {
            this.textField = textField;
            this.newNameField = newNameField;
            this.isInput = isInput;
            this.isDecrypt = isDecrypt;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Open a file chooser dialog to select files or directories
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(isInput ? JFileChooser.FILES_AND_DIRECTORIES : JFileChooser.DIRECTORIES_ONLY);
            if (isInput) {
                fileChooser.setMultiSelectionEnabled(true);

                if (isDecrypt) {
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Encrypted Files (*.enc)", "enc"));
                } else {
                    fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt"));
                    fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Document Files (*.docx, *.pdf)", "docx", "pdf"));
                    fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files (*.png, *.jpg)", "png", "jpg"));
                    fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Video Files (*.mov, *.mp4)", "mov", "mp4"));
                    fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Audio Files (*.wav, *.mp3)", "wav", "mp3"));
                }
            }

            int result = fileChooser.showOpenDialog(MainUI.this);
            // Check if the user selected a file or directory
            if (result == JFileChooser.APPROVE_OPTION) {
                if (isInput) {
                    File[] selectedFiles = fileChooser.getSelectedFiles();
                    if (selectedFiles.length == 0) {
                        File singleFile = fileChooser.getSelectedFile();
                        if (isDecrypt && !singleFile.getName().toLowerCase().endsWith(".enc")) {
                            showError("Input file must have .enc extension");
                            return;
                        }
                        textField.setText(singleFile.getAbsolutePath());
                        if (newNameField != null) {
                            newNameField.setEnabled(true);
                        }
                        if (isDecrypt) {
                            deSelectedFiles = new File[]{singleFile};
                        } else {
                            enSelectedFiles = new File[]{singleFile};
                        }
                    } else {
                        List<File> validFiles = new ArrayList<>();
                        for (File file : selectedFiles) {
                            if (!isDecrypt || file.getName().toLowerCase().endsWith(".enc")) {
                                validFiles.add(file);
                            }
                        }
                        if (validFiles.isEmpty()) {
                            showError("No valid files selected");
                            return;
                        }
                        textField.setText(validFiles.size() + " files selected");
                        if (newNameField != null) {
                            newNameField.setEnabled(false);
                            newNameField.setText("");
                        }
                        if (isDecrypt) {
                            deSelectedFiles = validFiles.toArray(File[]::new);
                        } else {
                            enSelectedFiles = validFiles.toArray(File[]::new);
                        }
                    }
                } else {
                    textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        }
    }

    private void displayMetadataList(List<JSONObject> metadataList, String title) {
        // Create a new JFrame to display the metadata
        JFrame frame = new JFrame(title);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(MainUI.this);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14));
        for (JSONObject metadata : metadataList) {
            area.append("Extension: " + metadata.getString("extension") + "\n");
            area.append("Original Name: " + metadata.getString("originalName") + "\n");
            area.append("Size: " + metadata.getLong("size") + " bytes\n");
            area.append("Timestamp: " + metadata.getString("timestamp") + "\n");
            area.append("----------------------------------------\n");
        }
        frame.add(new JScrollPane(area));
        frame.setVisible(true);
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

    private void showError(String message) {
        JOptionPane.showMessageDialog(MainUI.this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainUI().setVisible(true)); // Start the application
    }

    private static class WrapLayout extends java.awt.FlowLayout {
        public WrapLayout() {
            super(CENTER, 10, 10);
        }

        @Override
        public java.awt.Dimension preferredLayoutSize(Container target) {
            return computeSize(target, true);
        }

        @Override
        public java.awt.Dimension minimumLayoutSize(Container target) {
            return computeSize(target, false);
        }

        private java.awt.Dimension computeSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int hgap = getHgap();
                int vgap = getVgap();
                int width = target.getWidth();
                if (width == 0) {
                    width = Integer.MAX_VALUE;
                }
                Insets insets = target.getInsets();
                int maxWidth = width - (insets.left + insets.right + hgap * 2);
                int x = 0, y = 0, rowHeight = 0, maxHeight = 0;
                int count = target.getComponentCount();

                for (int i = 0; i < count; i++) {
                    Component c = target.getComponent(i);
                    if (c.isVisible()) {
                        java.awt.Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                        if (x == 0 || x + d.width <= maxWidth) {
                            x += d.width + hgap;
                            rowHeight = Math.max(rowHeight, d.height);
                        } else {
                            maxHeight += rowHeight + vgap;
                            x = d.width + hgap;
                            rowHeight = d.height;
                        }
                    }
                }
                maxHeight += rowHeight + vgap;
                return new java.awt.Dimension(width, maxHeight + insets.top + insets.bottom);
            }
        }
    }
}