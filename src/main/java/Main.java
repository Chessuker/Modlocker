
// import javax.swing.*;
// import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
// import java.io.File;

// public class Main {
//     private JFrame frame;
//     private JTextField inputFilePathField;
//     private JTextField outputFilePathField;
//     private JTextField passwordField;
//     private JTextArea outputArea;

//     public Main() {
//         frame = new JFrame("File Encryption/Decryption");
//         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//         frame.setSize(400, 300);
//         frame.setLayout(new GridLayout(5, 2));

//         inputFilePathField = new JTextField();
//         outputFilePathField = new JTextField();
//         passwordField = new JTextField();
//         outputArea = new JTextArea();
//         outputArea.setEditable(false);

//         JButton encryptButton = new JButton("Encrypt");
//         JButton decryptButton = new JButton("Decrypt");

//         encryptButton.addActionListener(new ActionListener() {
//             @Override
//             public void actionPerformed(ActionEvent e) {
//                 handleEncryption();
//             }
//         });

//         decryptButton.addActionListener(new ActionListener() {
//             @Override
//             public void actionPerformed(ActionEvent e) {
//                 handleDecryption();
//             }
//         });

//         frame.add(new JLabel("Input File Path:"));
//         frame.add(inputFilePathField);
//         frame.add(new JLabel("Output File Path:"));
//         frame.add(outputFilePathField);
//         frame.add(new JLabel("Password:"));
//         frame.add(passwordField);
//         frame.add(encryptButton);
//         frame.add(decryptButton);
//         frame.add(new JScrollPane(outputArea));

//         frame.setVisible(true);
//     }

//     private void handleEncryption() {
//         String inputFilePath = inputFilePathField.getText();
//         String outputFilePath = outputFilePathField.getText();
//         String password = passwordField.getText();

//         try {
//             AES aes = new AES();
//             byte[] salt = aes.generateSalt();
//             File inputFile = new File(inputFilePath);
//             File outputEncryptedFile = new File(outputFilePath);
//             aes.encryptFile(inputFile, outputEncryptedFile, password, salt);
//             outputArea.append("File encrypted successfully!\n");
//         } catch (Exception ex) {
//             outputArea.append("Error during encryption: " + ex.getMessage() + "\n");
//         }
//     }

//     private void handleDecryption() {
//         String inputFilePath = inputFilePathField.getText();
//         String outputFilePath = outputFilePathField.getText();
//         String password = passwordField.getText();

//         try {
//             AES aes = new AES();
//             File encryptedFile = new File(inputFilePath);
//             File decryptedFile = new File(outputFilePath);
//             aes.decryptFile(encryptedFile, decryptedFile, password);
//             outputArea.append("File decrypted successfully!\n");
//         } catch (Exception ex) {
//             outputArea.append("Error during decryption: " + ex.getMessage() + "\n");
//         }
//     }

//     public static void main(String[] args) {
//         SwingUtilities.invokeLater(MainUI::new);
//     }
// }