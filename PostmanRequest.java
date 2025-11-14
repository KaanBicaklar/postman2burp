package burp;

import java.util.HashMap;
import java.util.Map;

public class PostmanRequest {
    
    private String name;
    private String method;
    private String url;
    private String endpoint;
    private Map<String, String> headers;
    private String body;
    private String description;
    private String displayName;
    private String folderPath;
    private Map<String, String> queryParameters;
    private Map<String, String> pathVariables;
    private String notes;
    private boolean isVulnerable;
    private String vulnerabilityStatus;
    
    public PostmanRequest() {
        this.headers = new HashMap<>();
        this.queryParameters = new HashMap<>();
        this.pathVariables = new HashMap<>();
        this.method = "GET";
        this.folderPath = "";
        this.vulnerabilityStatus = "";
        this.isVulnerable = false;
    }
    
    public PostmanRequest(String name, String method, String url) {
        this();
        this.name = name;
        this.method = method != null ? method.toUpperCase() : "GET";
        this.url = url;
        this.endpoint = extractEndpoint(url);
        this.displayName = generateDisplayName();
    }
    
    private String extractEndpoint(String fullUrl) {
        if (fullUrl == null || fullUrl.isEmpty()) {
            return "/";
        }
        
        try {
            String urlWithoutProtocol = fullUrl.replaceFirst("^https?://", "");
            int firstSlashIndex = urlWithoutProtocol.indexOf('/');
            
            if (firstSlashIndex == -1) {
                return "/";
            }
            
            String endpoint = urlWithoutProtocol.substring(firstSlashIndex);
            
            int queryIndex = endpoint.indexOf('?');
            if (queryIndex != -1) {
                endpoint = endpoint.substring(0, queryIndex);
            }
            
            endpoint = endpoint.replaceAll("\\{\\{[^}]+\\}\\}", "{var}");
            
            return endpoint;
            
        } catch (Exception e) {
            return "/";
        }
    }
    
    private String generateDisplayName() {
        StringBuilder sb = new StringBuilder();
        
        String methodIcon = getMethodIcon(method);
        sb.append(methodIcon).append(" ");
        sb.append(method != null ? method : "GET");
        sb.append(" ");
        
        sb.append(endpoint != null ? endpoint : "/");
        
        if (name != null && !name.isEmpty()) {
            sb.append(" - ").append(name);
        }
        
        if (folderPath != null && !folderPath.isEmpty()) {
            sb.append(" (").append(folderPath).append(")");
        }
        
        return sb.toString();
    }
    
    private String getMethodIcon(String method) {
        if (method == null) return "📄";
        
        switch (method.toUpperCase()) {
            case "GET": return "📥";
            case "POST": return "📤";
            case "PUT": return "🔄";
            case "PATCH": return "🔧";
            case "DELETE": return "🗑️";
            case "HEAD": return "👁️";
            case "OPTIONS": return "⚙️";
            default: return "📄";
        }
    }
    
    public void addHeader(String key, String value) {
        if (key != null && value != null) {
            String processedValue = replacePostmanVariables(value);
            headers.put(key, processedValue);
        }
    }
    
    public void addQueryParameter(String key, String value) {
        if (key != null && value != null) {
            String processedValue = replacePostmanVariables(value);
            queryParameters.put(key, processedValue);
        }
    }
    
    public void addPathVariable(String key, String value) {
        if (key != null && value != null) {
            String processedValue = replacePostmanVariables(value);
            pathVariables.put(key, processedValue);
        }
    }
    
    private String replacePostmanVariables(String input) {
        if (input == null) return null;
        return input;
    }
    
    public String getFormattedHeaders() {
        if (headers.isEmpty()) {
            return "No headers";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        return sb.toString().trim();
    }
    
    public String getFormattedQueryParameters() {
        if (queryParameters.isEmpty()) {
            return "No query parameters";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        
        return sb.toString();
    }
    
    public String getCompleteUrl() {
        if (queryParameters.isEmpty()) {
            return url;
        }
        
        String baseUrl = url;
        
        int queryIndex = baseUrl.indexOf('?');
        if (queryIndex != -1) {
            baseUrl = baseUrl.substring(0, queryIndex);
        }
        
        return baseUrl + "?" + getFormattedQueryParameters();
    }
    
    public String getRequestInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Method: ").append(method).append("\n");
        info.append("URL: ").append(getCompleteUrl()).append("\n");
        info.append("Endpoint: ").append(endpoint).append("\n");
        
        if (!queryParameters.isEmpty()) {
            info.append("Query Params: ").append(getFormattedQueryParameters()).append("\n");
        }
        
        if (folderPath != null && !folderPath.isEmpty()) {
            info.append("Folder: ").append(folderPath).append("\n");
        }
        
        if (description != null && !description.isEmpty()) {
            info.append("Description: ").append(description).append("\n");
        }
        
        return info.toString();
    }
    
    public String getDetailedSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== ").append(displayName).append(" ===\n\n");
        summary.append(getRequestInfo()).append("\n");
        summary.append("Headers:\n").append(getFormattedHeaders()).append("\n");
        
        if (body != null && !body.isEmpty()) {
            String processedBody = replacePostmanVariables(body);
            summary.append("\nBody:\n").append(processedBody);
        }
        
        return summary.toString();
    }
    
    public String getCurlCommand() {
        StringBuilder curl = new StringBuilder("curl -X ").append(method);
        
        for (Map.Entry<String, String> header : headers.entrySet()) {
            curl.append(" -H \"").append(header.getKey()).append(": ").append(header.getValue()).append("\"");
        }
        
        if (body != null && !body.isEmpty()) {
            String processedBody = replacePostmanVariables(body);
            curl.append(" -d '").append(processedBody.replace("'", "'\"'\"'")).append("'");
        }
        
        curl.append(" \"").append(getCompleteUrl()).append("\"");
        
        return curl.toString();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.displayName = generateDisplayName();
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method != null ? method.toUpperCase() : "GET";
        this.displayName = generateDisplayName();
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
        this.endpoint = extractEndpoint(url);
        this.displayName = generateDisplayName();
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getFolderPath() {
        return folderPath;
    }
    
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
        this.displayName = generateDisplayName();
    }
    
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }
    
    public void setQueryParameters(Map<String, String> queryParameters) {
        this.queryParameters = queryParameters != null ? queryParameters : new HashMap<>();
    }
    
    public Map<String, String> getPathVariables() {
        return pathVariables;
    }
    
    public void setPathVariables(Map<String, String> pathVariables) {
        this.pathVariables = pathVariables != null ? pathVariables : new HashMap<>();
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public boolean isVulnerable() {
        return isVulnerable;
    }
    
    public void setVulnerable(boolean vulnerable) {
        isVulnerable = vulnerable;
        if (vulnerable) {
            vulnerabilityStatus = "🔴 Vulnerable";
        } else {
            vulnerabilityStatus = "🟢 Safe";
        }
    }
    
    public String getVulnerabilityStatus() {
        return vulnerabilityStatus != null ? vulnerabilityStatus : "";
    }
    
    public void setVulnerabilityStatus(String status) {
        this.vulnerabilityStatus = status;
        this.isVulnerable = "🔴 Vulnerable".equals(status);
    }
    
    public void clearVulnerabilityStatus() {
        this.vulnerabilityStatus = "";
        this.isVulnerable = false;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
