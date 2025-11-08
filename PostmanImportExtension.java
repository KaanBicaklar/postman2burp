package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PostmanImportExtension implements BurpExtension {
    
    private MontoyaApi api;
    private JPanel mainPanel;
    private RequestDisplayPanel requestDisplayPanel;
    private PostmanParser postmanParser;
    
    private static final String EXTENSION_NAME = "MonaPostman";
    
    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.postmanParser = new PostmanParser(api);
        
        api.extension().setName(EXTENSION_NAME);
        
        SwingUtilities.invokeLater(() -> {
            initializeGUI();
            api.userInterface().registerSuiteTab(EXTENSION_NAME, mainPanel);
        });
        
        api.logging().logToOutput("Postman Import Extension loaded successfully!");
        api.logging().logToOutput("Ready to import Postman collections with endpoint and method organization");
    }
    
    private void initializeGUI() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        requestDisplayPanel = new RequestDisplayPanel(api);
        JScrollPane scrollPane = new JScrollPane(requestDisplayPanel);
        scrollPane.setPreferredSize(new Dimension(900, 650));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Imported Requests"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createTitledBorder("Import Postman Collection"));
        headerPanel.setPreferredSize(new Dimension(0, 80));
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton importButton = new JButton("📁 Select Postman JSON Files");
        importButton.setPreferredSize(new Dimension(240, 35));
        importButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        importButton.setBackground(new Color(0, 123, 255));
        importButton.setForeground(Color.WHITE);
        importButton.setFocusPainted(false);
        
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importPostmanFiles();
            }
        });
        
        JLabel instructionLabel = new JLabel("Select one or more Postman collection JSON files to import and organize requests");
        instructionLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        instructionLabel.setForeground(Color.GRAY);
        
        leftPanel.add(importButton);
        leftPanel.add(Box.createHorizontalStrut(15));
        leftPanel.add(instructionLabel);
        
        headerPanel.add(leftPanel, BorderLayout.CENTER);
        
        return headerPanel;
    }
    
    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.setPreferredSize(new Dimension(0, 30));
        
        JLabel statusLabel = new JLabel("🔄 Ready to import Postman collections");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusPanel.add(statusLabel);
        
        return statusPanel;
    }
    
    private void importPostmanFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Postman Collection Files");
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".json");
            }
            
            @Override
            public String getDescription() {
                return "JSON Files (*.json)";
            }
        });
        
        int result = fileChooser.showOpenDialog(mainPanel);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            
            if (selectedFiles.length == 0) {
                return;
            }
            
            updateStatusLabel("📂 Processing " + selectedFiles.length + " file(s)...");
            
            SwingUtilities.invokeLater(() -> {
                List<PostmanRequest> allRequests = new ArrayList<>();
                int successfulFiles = 0;
                StringBuilder errorMessages = new StringBuilder();
                
                for (File selectedFile : selectedFiles) {
                    try {
                        String jsonContent = new String(Files.readAllBytes(Paths.get(selectedFile.getAbsolutePath())));
                        
                        List<PostmanRequest> requests = postmanParser.parseCollection(jsonContent);
                        
                        if (requests.size() > 0) {
                            String filePrefix = selectedFile.getName().replaceAll("\\.[^.]+$", "");
                            for (PostmanRequest request : requests) {
                                String currentFolder = request.getFolderPath();
                                if (currentFolder == null || currentFolder.isEmpty()) {
                                    request.setFolderPath(filePrefix);
                                } else {
                                    request.setFolderPath(filePrefix + "/" + currentFolder);
                                }
                            }
                            
                            allRequests.addAll(requests);
                            successfulFiles++;
                            
                            api.logging().logToOutput("Successfully imported " + requests.size() + " requests from " + selectedFile.getName());
                        } else {
                            errorMessages.append("No requests found in ").append(selectedFile.getName()).append("\n");
                        }
                        
                    } catch (IOException ex) {
                        String errorMsg = "Error reading " + selectedFile.getName() + ": " + ex.getMessage();
                        errorMessages.append(errorMsg).append("\n");
                        api.logging().logToError(errorMsg);
                    } catch (Exception ex) {
                        String errorMsg = "Error parsing " + selectedFile.getName() + ": " + ex.getMessage();
                        errorMessages.append(errorMsg).append("\n");
                        api.logging().logToError(errorMsg);
                    }
                }
                
                if (allRequests.size() > 0) {
                    requestDisplayPanel.displayRequests(allRequests);
                }
                
                String statusMessage;
                if (successfulFiles > 0) {
                    statusMessage = "✅ Successfully imported " + allRequests.size() + " requests from " + successfulFiles + "/" + selectedFiles.length + " files";
                    updateStatusLabel(statusMessage);
                    
                    String message = "Successfully imported " + allRequests.size() + " requests from " + successfulFiles + " files!\n" +
                        "Requests are organized by HTTP method and endpoint.";
                    
                    if (errorMessages.length() > 0) {
                        message += "\n\nSome files had issues:\n" + errorMessages.toString();
                    }
                    
                    JOptionPane.showMessageDialog(
                        mainPanel,
                        message,
                        "Import Results",
                        errorMessages.length() > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE
                    );
                } else {
                    statusMessage = "❌ No valid requests found in any selected files";
                    updateStatusLabel(statusMessage);
                    
                    JOptionPane.showMessageDialog(
                        mainPanel,
                        "No valid requests found in any of the selected files.\n" +
                        "Please check if the files are valid Postman collections.\n\n" +
                        "Errors:\n" + errorMessages.toString(),
                        "Import Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            });
        }
    }
    
    private void updateStatusLabel(String message) {
        JPanel statusPanel = (JPanel) mainPanel.getComponent(2);
        JLabel statusLabel = (JLabel) statusPanel.getComponent(0);
        statusLabel.setText(message);
        statusPanel.repaint();
    }
    
    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(
            mainPanel,
            message,
            "Import Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}