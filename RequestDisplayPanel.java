package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;


public class RequestDisplayPanel extends JPanel {
    
    private MontoyaApi api;
    private JTable requestTable;
    private RequestTableModel tableModel;
    private JTextArea detailsTextArea;
    private JTextArea notesTextArea;
    private JTabbedPane detailsTabPane;
    private JSplitPane splitPane;
    private List<PostmanRequest> requests;
    private JLabel summaryLabel;
    private PostmanRequest currentSelectedRequest;
    
    public RequestDisplayPanel(MontoyaApi api) {
        this.api = api;
        this.requests = new ArrayList<>();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }
    
   
    private void initializeComponents() {
        setLayout(new BorderLayout());
        
        summaryLabel = new JLabel("No requests loaded");
        summaryLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        tableModel = new RequestTableModel();
        requestTable = new JTable(tableModel);
        setupTable();
        
        detailsTextArea = new JTextArea();
        notesTextArea = new JTextArea();
        setupDetailsArea();
        setupNotesArea();
        
        detailsTabPane = new JTabbedPane();
        
        JScrollPane detailsScrollPane = new JScrollPane(detailsTextArea);
        detailsScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane notesScrollPane = new JScrollPane(notesTextArea);
        notesScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        detailsTabPane.addTab("Request Details", detailsScrollPane);
        detailsTabPane.addTab("Notes", notesScrollPane);
        
        JScrollPane tableScrollPane = new JScrollPane(requestTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Requests by Method & Endpoint"));
        
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Request Details & Notes"));
        detailsPanel.add(detailsTabPane, BorderLayout.CENTER);
        
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, detailsPanel);
        splitPane.setDividerLocation(350);
        splitPane.setResizeWeight(0.6);
    }
    
  
    private void setupTable() {
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.setRowHeight(28);
        requestTable.setShowGrid(true);
        requestTable.setGridColor(new Color(240, 240, 240));
        requestTable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        requestTable.setAutoCreateRowSorter(true);
        
        TableColumnModel columnModel = requestTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(120); // Status
        columnModel.getColumn(1).setPreferredWidth(80);  // Method
        columnModel.getColumn(2).setPreferredWidth(200); // Endpoint
        columnModel.getColumn(3).setPreferredWidth(180); // Name
        columnModel.getColumn(4).setPreferredWidth(200); // Notes
        
        columnModel.getColumn(0).setCellRenderer(new StatusCellRenderer());
        columnModel.getColumn(1).setCellRenderer(new MethodCellRenderer());
        columnModel.getColumn(2).setCellRenderer(new EndpointCellRenderer());
        columnModel.getColumn(3).setCellRenderer(new NameCellRenderer());
        columnModel.getColumn(4).setCellRenderer(new NotesCellRenderer());
        
        requestTable.getTableHeader().setBackground(new Color(250, 250, 250));
        requestTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        requestTable.getTableHeader().setBorder(BorderFactory.createLoweredBevelBorder());
        
        requestTable.setDefaultRenderer(Object.class, new AlternatingRowRenderer());
    }
    

    private void setupDetailsArea() {
        detailsTextArea.setEditable(false);
        detailsTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        detailsTextArea.setBackground(new Color(248, 248, 248));
        detailsTextArea.setTabSize(4);
        detailsTextArea.setText("Select a request from the table above to view detailed information...");
        detailsTextArea.setCaretPosition(0);
    }
    

    private void setupNotesArea() {
        notesTextArea.setEditable(true);
        notesTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        notesTextArea.setBackground(Color.WHITE);
        notesTextArea.setTabSize(4);
        notesTextArea.setText("Select a request to add notes...");
        notesTextArea.setCaretPosition(0);
        notesTextArea.setLineWrap(true);
        notesTextArea.setWrapStyleWord(true);
        
        notesTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                RequestDisplayPanel.this.saveNotes();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                RequestDisplayPanel.this.saveNotes();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                RequestDisplayPanel.this.saveNotes();
            }
        });
    }
    

    private void setupLayout() {
        add(summaryLabel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        
       
        updateSummary();
    }
    

    private void setupEventHandlers() {
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = requestTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = requestTable.convertRowIndexToModel(selectedRow);
                    if (modelRow >= 0 && modelRow < requests.size()) {
                        PostmanRequest selectedRequest = requests.get(modelRow);
                        displayRequestDetails(selectedRequest);
                    }
                }
            }
        });
        
        requestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = requestTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        int modelRow = requestTable.convertRowIndexToModel(selectedRow);
                        if (modelRow >= 0 && modelRow < requests.size()) {
                            showCopyMenu(requests.get(modelRow), e.getX(), e.getY());
                        }
                    }
                }
            }
        });
        
        requestTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = requestTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        requestTable.setRowSelectionInterval(row, row);
                        int modelRow = requestTable.convertRowIndexToModel(row);
                        if (modelRow >= 0 && modelRow < requests.size()) {
                            showContextMenu(requests.get(modelRow), e.getX(), e.getY());
                        }
                    }
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = requestTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        requestTable.setRowSelectionInterval(row, row);
                        int modelRow = requestTable.convertRowIndexToModel(row);
                        if (modelRow >= 0 && modelRow < requests.size()) {
                            showContextMenu(requests.get(modelRow), e.getX(), e.getY());
                        }
                    }
                }
            }
        });
    }
    

    public void displayRequests(List<PostmanRequest> newRequests) {
        if (this.requests == null) {
            this.requests = new ArrayList<>();
        }
        
        this.requests.addAll(newRequests);
        
        tableModel.fireTableDataChanged();
        updateSummary();
        
        if (!requests.isEmpty()) {
            if (requestTable.getSelectedRow() < 0) {
                requestTable.setRowSelectionInterval(0, 0);
                displayRequestDetails(requests.get(0));
            }
        } else {
            detailsTextArea.setText("No valid requests found in the imported file.");
        }
    }
    

    private void updateSummary() {
        if (requests.isEmpty()) {
            summaryLabel.setText("📋 No requests loaded");
        } else {
            long getCount = requests.stream().mapToLong(r -> "GET".equals(r.getMethod()) ? 1 : 0).sum();
            long postCount = requests.stream().mapToLong(r -> "POST".equals(r.getMethod()) ? 1 : 0).sum();
            long putCount = requests.stream().mapToLong(r -> "PUT".equals(r.getMethod()) ? 1 : 0).sum();
            long deleteCount = requests.stream().mapToLong(r -> "DELETE".equals(r.getMethod()) ? 1 : 0).sum();
            long otherCount = requests.size() - getCount - postCount - putCount - deleteCount;
            
            StringBuilder summary = new StringBuilder("📊 Total: " + requests.size() + " requests");
            if (getCount > 0) summary.append(" | 📥 GET: ").append(getCount);
            if (postCount > 0) summary.append(" | 📤 POST: ").append(postCount);
            if (putCount > 0) summary.append(" | 🔄 PUT: ").append(putCount);
            if (deleteCount > 0) summary.append(" | 🗑️ DELETE: ").append(deleteCount);
            if (otherCount > 0) summary.append(" | 📄 Other: ").append(otherCount);
            
            summaryLabel.setText(summary.toString());
        }
    }
    

    private void displayRequestDetails(PostmanRequest request) {
        if (request == null) {
            detailsTextArea.setText("No request selected");
            notesTextArea.setText("No request selected");
            currentSelectedRequest = null;
            return;
        }
        
        if (currentSelectedRequest != null && currentSelectedRequest != request) {
            saveCurrentNotes();
        }
        
        currentSelectedRequest = request;
        
        StringBuilder details = new StringBuilder();
        details.append("🔍 REQUEST DETAILS\n");
        details.append("═".repeat(50)).append("\n\n");
        
        details.append("📝 Name: ").append(request.getName()).append("\n");
        details.append("🌐 Method: ").append(request.getMethod()).append("\n");
        details.append("🔗 URL: ").append(request.getUrl()).append("\n");
        details.append("📍 Endpoint: ").append(request.getEndpoint()).append("\n");
        
        if (request.getFolderPath() != null && !request.getFolderPath().isEmpty()) {
            details.append("📁 Folder: ").append(request.getFolderPath()).append("\n");
        }
        
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            details.append("📄 Description: ").append(request.getDescription()).append("\n");
        }
        
        details.append("\n🏷️ HEADERS\n");
        details.append("─".repeat(30)).append("\n");
        String headers = request.getFormattedHeaders();
        if ("No headers".equals(headers)) {
            details.append("No headers defined\n");
        } else {
            details.append(headers).append("\n");
        }
        
        if (request.getBody() != null && !request.getBody().isEmpty()) {
            details.append("\n📦 BODY\n");
            details.append("─".repeat(30)).append("\n");
            details.append(request.getBody()).append("\n");
        }
        
        details.append("\n🔧 CURL COMMAND\n");
        details.append("─".repeat(30)).append("\n");
        details.append(request.getCurlCommand()).append("\n");
        
        detailsTextArea.setText(details.toString());
        detailsTextArea.setCaretPosition(0);
        
        loadNotesForRequest(request);
    }
    

    private void showContextMenu(PostmanRequest request, int x, int y) {
        JPopupMenu popup = new JPopupMenu();
        
        popup.add(new JLabel("🔍 Vulnerability Status"));
        popup.addSeparator();
        
        JMenuItem markSafe = new JMenuItem("🟢 Mark as Safe");
        markSafe.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        markSafe.addActionListener(e -> {
            request.setVulnerable(false);
            tableModel.fireTableDataChanged(); // Refresh table
        });
        popup.add(markSafe);
        
        JMenuItem markVulnerable = new JMenuItem("🔴 Mark as Vulnerable");
        markVulnerable.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        markVulnerable.addActionListener(e -> {
            request.setVulnerable(true);
            tableModel.fireTableDataChanged(); // Refresh table
        });
        popup.add(markVulnerable);
        
        JMenuItem clearStatus = new JMenuItem("⚫ Clear Status");
        clearStatus.addActionListener(e -> {
            request.clearVulnerabilityStatus();
            tableModel.fireTableDataChanged(); // Refresh table
        });
        popup.add(clearStatus);
        
        popup.addSeparator();
        
        popup.add(new JLabel("🚀 Send to Burp Tools"));
        popup.addSeparator();
        
        JMenuItem sendToRepeater = new JMenuItem("🔄 Send to Repeater");
        sendToRepeater.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        sendToRepeater.addActionListener(e -> sendToRepeater(request));
        popup.add(sendToRepeater);
        
        JMenuItem sendToOrganizer = new JMenuItem("� Send to Organizer");
        sendToOrganizer.addActionListener(e -> sendToOrganizer(request));
        popup.add(sendToOrganizer);
        
        popup.addSeparator();
        
        JMenuItem deleteRequest = new JMenuItem("�️ Delete Request");
        deleteRequest.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        deleteRequest.setForeground(Color.RED);
        deleteRequest.addActionListener(e -> deleteRequest(request));
        popup.add(deleteRequest);
        
        popup.addSeparator();
        popup.add(new JLabel("📋 Copy Options"));
        popup.addSeparator();
        
        JMenuItem copyUrl = new JMenuItem("🔗 Copy URL");
        copyUrl.addActionListener(e -> copyToClipboard(request.getUrl()));
        popup.add(copyUrl);
        
        JMenuItem copyCurl = new JMenuItem("🔧 Copy as cURL");
        copyCurl.addActionListener(e -> copyToClipboard(request.getCurlCommand()));
        popup.add(copyCurl);
        
        JMenuItem copyDetails = new JMenuItem("📄 Copy Full Details");
        copyDetails.addActionListener(e -> copyToClipboard(request.getDetailedSummary()));
        popup.add(copyDetails);
        
        popup.show(requestTable, x, y);
    }
    

    private void sendToRepeater(PostmanRequest request) {
        try {
            HttpRequest httpRequest = createHttpRequest(request);
            
            if (httpRequest != null) {
                String tabName = request.getMethod() + " " + request.getEndpoint();
                api.repeater().sendToRepeater(httpRequest, tabName);
                
                JOptionPane.showMessageDialog(
                    this,
                    "Request sent to Repeater successfully!\nTab: " + tabName,
                    "Sent to Repeater",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                api.logging().logToOutput("Sent to Repeater: " + request.getMethod() + " " + request.getUrl());
            }
        } catch (Exception e) {
            showErrorMessage("Failed to send to Repeater: " + e.getMessage());
            api.logging().logToError("Error sending to Repeater: " + e.getMessage());
        }
    }
    

    private void deleteRequest(PostmanRequest request) {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this request?\n\n" +
            request.getMethod() + " " + request.getEndpoint() + "\n" +
            "Name: " + request.getName(),
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            requests.remove(request);
            
            tableModel.fireTableDataChanged();
            updateSummary();
            
            if (requests.isEmpty()) {
                detailsTextArea.setText("No requests available.");
                notesTextArea.setText("Select a request to add notes...");
            } else {
                requestTable.setRowSelectionInterval(0, 0);
                displayRequestDetails(requests.get(0));
            }
            
            api.logging().logToOutput("Deleted request: " + request.getMethod() + " " + request.getUrl());
        }
    }
    

    private void sendToOrganizer(PostmanRequest request) {
        try {
            HttpRequest httpRequest = createHttpRequest(request);
            
            if (httpRequest != null) {
                HttpRequestResponse requestResponse = HttpRequestResponse.httpRequestResponse(
                    httpRequest, 
                    null
                );
                

                api.organizer().sendToOrganizer(requestResponse);
                
                JOptionPane.showMessageDialog(
                    this,
                    "Request sent to Organizer successfully!\nNote: Response will be empty until you send the request.",
                    "Sent to Organizer",
                    JOptionPane.INFORMATION_MESSAGE
                );
                
                api.logging().logToOutput("Sent to Organizer: " + request.getMethod() + " " + request.getUrl());
            }
        } catch (Exception e) {
            showErrorMessage("Failed to send to Organizer: " + e.getMessage());
            api.logging().logToError("Error sending to Organizer: " + e.getMessage());
        }
    }
    

    private HttpRequest createHttpRequest(PostmanRequest request) {
        try {
            StringBuilder httpRequestBuilder = new StringBuilder();
            
            String path = extractPathFromUrl(request.getUrl());
            httpRequestBuilder.append(request.getMethod()).append(" ").append(path).append(" HTTP/1.1\r\n");
            
            String host = extractHostFromUrl(request.getUrl());
            if (host != null) {
                httpRequestBuilder.append("Host: ").append(host).append("\r\n");
            }
            
            for (String key : request.getHeaders().keySet()) {
                String value = request.getHeaders().get(key);
                httpRequestBuilder.append(key).append(": ").append(value).append("\r\n");
            }
            
            if (request.getBody() != null && !request.getBody().isEmpty()) {
                httpRequestBuilder.append("Content-Length: ").append(request.getBody().length()).append("\r\n");
                
                if (!request.getHeaders().containsKey("Content-Type")) {
                    httpRequestBuilder.append("Content-Type: application/json\r\n");
                }
            }
            
            httpRequestBuilder.append("\r\n");
            
            if (request.getBody() != null && !request.getBody().isEmpty()) {
                httpRequestBuilder.append(request.getBody());
            }
            
            return HttpRequest.httpRequest(httpRequestBuilder.toString());
            
        } catch (Exception e) {
            api.logging().logToError("Error creating HTTP request: " + e.getMessage());
            return null;
        }
    }
    

    private String extractHostFromUrl(String url) {
        try {
            String urlWithoutProtocol = url.replaceFirst("^https?://", "");
            int slashIndex = urlWithoutProtocol.indexOf('/');
            
            if (slashIndex == -1) {
                return urlWithoutProtocol;
            } else {
                return urlWithoutProtocol.substring(0, slashIndex);
            }
        } catch (Exception e) {
            return "localhost";
        }
    }
    

    private String extractPathFromUrl(String url) {
        try {
            String urlWithoutProtocol = url.replaceFirst("^https?://", "");
            int slashIndex = urlWithoutProtocol.indexOf('/');
            
            if (slashIndex == -1) {
                return "/";
            } else {
                String path = urlWithoutProtocol.substring(slashIndex);
                return path.isEmpty() ? "/" : path;
            }
        } catch (Exception e) {
            return "/";
        }
    }
    

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
    

    private void showCopyMenu(PostmanRequest request, int x, int y) {
        showContextMenu(request, x, y);
    }
    

    private void copyToClipboard(String text) {
        java.awt.datatransfer.StringSelection stringSelection = 
            new java.awt.datatransfer.StringSelection(text);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        
        JOptionPane.showMessageDialog(
            this,
            "Copied to clipboard!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        );
    }
    

    private class RequestTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Status", "Method", "Endpoint", "Name", "Notes"};
        
        @Override
        public int getRowCount() {
            return requests.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= requests.size()) return "";
            
            PostmanRequest request = requests.get(rowIndex);
            switch (columnIndex) {
                case 0: return request.getVulnerabilityStatus();
                case 1: return request.getMethod();
                case 2: return request.getEndpoint();
                case 3: return request.getName();
                case 4: 
                    String notes = request.getNotes();
                    if (notes == null || notes.trim().isEmpty()) {
                        return "";
                    }
                    String cleanNotes = notes.trim().replaceAll("\\n", " ");
                    return cleanNotes.length() > 50 ? cleanNotes.substring(0, 50) + "..." : cleanNotes;
                default: return "";
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
    

    private class MethodCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                String method = value.toString();
                Color backgroundColor = getMethodColor(method);
                component.setBackground(backgroundColor);
                component.setForeground(Color.WHITE);
                setFont(getFont().deriveFont(Font.BOLD));
            }
            
            setHorizontalAlignment(JLabel.CENTER);
            return component;
        }
        
        private Color getMethodColor(String method) {
            switch (method.toUpperCase()) {
                case "GET": return new Color(40, 167, 69);      // Green
                case "POST": return new Color(0, 123, 255);     // Blue
                case "PUT": return new Color(255, 193, 7);      // Yellow/Orange
                case "PATCH": return new Color(102, 16, 242);   // Purple
                case "DELETE": return new Color(220, 53, 69);   // Red
                case "HEAD": return new Color(108, 117, 125);   // Gray
                case "OPTIONS": return new Color(111, 66, 193); // Purple
                default: return new Color(134, 142, 150);       // Light gray
            }
        }
    }
    

    private class EndpointCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            setToolTipText(value.toString());
            return component;
        }
    }
    

    private class NameCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setToolTipText(value.toString());
            return component;
        }
    }
    

    private class NotesCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
            setForeground(new Color(108, 117, 125));
            
            String notes = value.toString();
            if (notes.isEmpty()) {
                setText("📝 Add notes...");
                setForeground(new Color(180, 180, 180));
            } else {
                setText("📝 " + notes);
            }
            
            setToolTipText(notes.isEmpty() ? "Click to add notes for this request" : notes);
            return component;
        }
    }
    

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            
            String status = value.toString();
            if (status.contains("🔴")) {
                setForeground(new Color(220, 53, 69)); // Red
            } else if (status.contains("🟢")) {
                setForeground(new Color(40, 167, 69)); // Green
            } else {
                setText("⚪ Unknown");
                setForeground(new Color(108, 117, 125)); // Gray
            }
            
            setToolTipText("Right-click to change vulnerability status");
            return component;
        }
    }
    

    private class AlternatingRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                if (row % 2 == 0) {
                    component.setBackground(Color.WHITE);
                } else {
                    component.setBackground(new Color(248, 249, 250));
                }
            }
            
            return component;
        }
    }
    

    private void saveNotes() {
        if (currentSelectedRequest != null) {
            String notes = notesTextArea.getText().trim();
            if (notes.equals("Select a request to add notes...")) {
                notes = "";
            }
            currentSelectedRequest.setNotes(notes);
        }
    }
    

    private void saveCurrentNotes() {
        saveNotes();
    }
    

    private void loadNotesForRequest(PostmanRequest request) {
        if (request != null) {
            String notes = request.getNotes();
            if (notes == null || notes.trim().isEmpty()) {
                notesTextArea.setText("Add notes...");
            } else {
                notesTextArea.setText(notes);
            }
            notesTextArea.setCaretPosition(0);
        }
    }
}