package burp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BulkEditDialog extends JDialog {
    private JTabbedPane tabbedPane;
    private JTextField findField;
    private JTextField replaceField;
    private JCheckBox urlCheckBox;
    private JCheckBox headersCheckBox;
    private JCheckBox bodyCheckBox;
    private JCheckBox regexCheckBox;
    private JTextField headerNameField;
    private JTextField headerValueField;
    private JTextArea previewArea;
    private JButton applyButton;
    private JButton cancelButton;
    private List<PostmanRequest> requests;
    private boolean result = false;
    
    public BulkEditDialog(Frame parent, List<PostmanRequest> selectedRequests) {
        super(parent, "🔧 Bulk Edit Operations", true);
        this.requests = selectedRequests;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(650, 550);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        
        JPanel matchReplacePanel = createMatchReplacePanel();
        tabbedPane.addTab("🔍 Find & Replace", matchReplacePanel);
        
        JPanel headerPanel = createHeaderPanel();
        tabbedPane.addTab("📋 Header Operations", headerPanel);
        
        previewArea = new JTextArea(8, 50);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        previewArea.setBorder(BorderFactory.createTitledBorder("Preview Changes"));
        
        applyButton = new JButton("✅ Apply Changes");
        cancelButton = new JButton("❌ Cancel");
        
        updatePreview();
    }
    
    private JPanel createMatchReplacePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("🔍 Find:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        findField = new JTextField(25);
        panel.add(findField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("🔄 Replace:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        replaceField = new JTextField(25);
        panel.add(replaceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Apply To:"));
        
        urlCheckBox = new JCheckBox("URLs", true);
        headersCheckBox = new JCheckBox("Headers", true);
        bodyCheckBox = new JCheckBox("Body", true);
        regexCheckBox = new JCheckBox("Use Regex", false);
        
        optionsPanel.add(urlCheckBox);
        optionsPanel.add(headersCheckBox);
        optionsPanel.add(bodyCheckBox);
        optionsPanel.add(regexCheckBox);
        
        panel.add(optionsPanel, gbc);
        
        return panel;
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(new JLabel("📋 Add/Update Header:"), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        headerNameField = new JTextField(20);
        panel.add(headerNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Value:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        headerValueField = new JTextField(20);
        panel.add(headerValueField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        JPanel quickHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        quickHeaderPanel.setBorder(BorderFactory.createTitledBorder("Quick Add:"));
        
        JButton authButton = new JButton("🔑 Authorization");
        JButton contentTypeButton = new JButton("📄 Content-Type");
        JButton userAgentButton = new JButton("🌐 User-Agent");
        
        authButton.addActionListener(e -> {
            headerNameField.setText("Authorization");
            headerValueField.setText("Bearer YOUR_TOKEN");
            headerValueField.selectAll();
        });
        
        contentTypeButton.addActionListener(e -> {
            headerNameField.setText("Content-Type");
            headerValueField.setText("application/json");
        });
        
        userAgentButton.addActionListener(e -> {
            headerNameField.setText("User-Agent");
            headerValueField.setText("MonaAPITester/1.0");
        });
        
        quickHeaderPanel.add(authButton);
        quickHeaderPanel.add(contentTypeButton);
        quickHeaderPanel.add(userAgentButton);
        
        panel.add(quickHeaderPanel, gbc);
        
        return panel;
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        add(tabbedPane, BorderLayout.CENTER);
        add(new JScrollPane(previewArea), BorderLayout.SOUTH);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);
        add(buttonPanel, BorderLayout.PAGE_END);
    }
    
    private void setupEventHandlers() {
        findField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        replaceField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        headerNameField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        headerValueField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updatePreview));
        
        urlCheckBox.addActionListener(e -> updatePreview());
        headersCheckBox.addActionListener(e -> updatePreview());
        bodyCheckBox.addActionListener(e -> updatePreview());
        regexCheckBox.addActionListener(e -> updatePreview());
        
        applyButton.addActionListener(e -> {
            if (applyChanges()) {
                result = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> {
            result = false;
            dispose();
        });
    }
    
    private void updatePreview() {
        SwingUtilities.invokeLater(() -> {
            StringBuilder preview = new StringBuilder();
            preview.append("📊 Changes Preview (").append(requests.size()).append(" requests):\n\n");
            
            int changeCount = 0;
            
            String findText = findField.getText().trim();
            String replaceText = replaceField.getText();
            
            if (!findText.isEmpty() && (urlCheckBox.isSelected() || headersCheckBox.isSelected() || bodyCheckBox.isSelected())) {
                preview.append("🔍 Find & Replace: '").append(findText).append("' → '").append(replaceText).append("'\n");
                
                try {
                    Pattern pattern = regexCheckBox.isSelected() ? 
                        Pattern.compile(findText) : 
                        Pattern.compile(Pattern.quote(findText));
                    
                    for (PostmanRequest req : requests) {
                        boolean hasChanges = false;
                        StringBuilder reqChanges = new StringBuilder();
                        
                        if (urlCheckBox.isSelected() && pattern.matcher(req.getUrl()).find()) {
                            hasChanges = true;
                            reqChanges.append("   URL modified\n");
                        }
                        
                        if (headersCheckBox.isSelected() && req.getHeaders() != null) {
                            String headersText = req.getHeaders().toString();
                            if (pattern.matcher(headersText).find()) {
                                hasChanges = true;
                                reqChanges.append("   Headers modified\n");
                            }
                        }
                        
                        if (bodyCheckBox.isSelected() && req.getBody() != null && !req.getBody().isEmpty()) {
                            if (pattern.matcher(req.getBody()).find()) {
                                hasChanges = true;
                                reqChanges.append("   Body modified\n");
                            }
                        }
                        
                        if (hasChanges) {
                            changeCount++;
                            if (changeCount <= 5) {
                                preview.append("  • ").append(req.getMethod()).append(" ").append(req.getName()).append("\n");
                                preview.append(reqChanges);
                            }
                        }
                    }
                    
                    if (changeCount > 5) {
                        preview.append("  ... and ").append(changeCount - 5).append(" more requests\n");
                    }
                    
                } catch (PatternSyntaxException ex) {
                    preview.append("❌ Invalid regex pattern: ").append(ex.getMessage()).append("\n");
                }
                
                preview.append("\n");
            }
            
            String headerName = headerNameField.getText().trim();
            String headerValue = headerValueField.getText().trim();
            
            if (!headerName.isEmpty()) {
                preview.append("📋 Header Operation: Add/Update '").append(headerName).append("' = '").append(headerValue).append("'\n");
                preview.append("  Will be applied to all ").append(requests.size()).append(" selected requests\n\n");
            }
            
            if (changeCount == 0 && headerName.isEmpty()) {
                preview.append("ℹ️ No operations defined. Configure find/replace or header operations above.");
            } else {
                preview.append("✅ Total affected requests: ").append(Math.max(changeCount, headerName.isEmpty() ? 0 : requests.size()));
            }
            
            previewArea.setText(preview.toString());
            previewArea.setCaretPosition(0);
        });
    }
    
    private boolean applyChanges() {
        try {
            boolean hasChanges = false;
            
            String findText = findField.getText().trim();
            String replaceText = replaceField.getText();
            
            if (!findText.isEmpty() && (urlCheckBox.isSelected() || headersCheckBox.isSelected() || bodyCheckBox.isSelected())) {
                Pattern pattern = regexCheckBox.isSelected() ? 
                    Pattern.compile(findText) : 
                    Pattern.compile(Pattern.quote(findText));
                
                for (PostmanRequest req : requests) {
                    if (urlCheckBox.isSelected()) {
                        String newUrl = pattern.matcher(req.getUrl()).replaceAll(replaceText);
                        if (!newUrl.equals(req.getUrl())) {
                            req.setUrl(newUrl);
                            hasChanges = true;
                        }
                    }
                    
                    if (headersCheckBox.isSelected() && req.getHeaders() != null) {
                        for (String key : req.getHeaders().keySet()) {
                            String oldValue = req.getHeaders().get(key);
                            String newValue = pattern.matcher(oldValue).replaceAll(replaceText);
                            if (!newValue.equals(oldValue)) {
                                req.getHeaders().put(key, newValue);
                                hasChanges = true;
                            }
                        }
                    }
                    
                    if (bodyCheckBox.isSelected() && req.getBody() != null) {
                        String newBody = pattern.matcher(req.getBody()).replaceAll(replaceText);
                        if (!newBody.equals(req.getBody())) {
                            req.setBody(newBody);
                            hasChanges = true;
                        }
                    }
                }
            }
            
            String headerName = headerNameField.getText().trim();
            String headerValue = headerValueField.getText().trim();
            
            if (!headerName.isEmpty()) {
                for (PostmanRequest req : requests) {
                    if (req.getHeaders() == null) {
                        req.setHeaders(new java.util.HashMap<>());
                    }
                    req.getHeaders().put(headerName, headerValue);
                    hasChanges = true;
                }
            }
            
            if (!hasChanges) {
                JOptionPane.showMessageDialog(this, 
                    "No changes were defined. Please configure operations and try again.", 
                    "No Operations", 
                    JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            
            return true;
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error applying changes: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    public boolean showDialog() {
        setVisible(true);
        return result;
    }
    
    private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final Runnable callback;
        
        public SimpleDocumentListener(Runnable callback) {
            this.callback = callback;
        }
        
        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
        
        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
        
        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) { callback.run(); }
    }
}
