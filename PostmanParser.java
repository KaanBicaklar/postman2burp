package burp;

import burp.api.montoya.MontoyaApi;
import com.google.gson.*;
import java.util.ArrayList;
import java.util.List;


public class PostmanParser {
    
    private final Gson gson;
    private final MontoyaApi api;
    
    public PostmanParser(MontoyaApi api) {
        this.gson = new GsonBuilder().create();
        this.api = api;
    }
    

    public List<PostmanRequest> parseCollection(String jsonContent) throws Exception {
        List<PostmanRequest> requests = new ArrayList<>();
        
        try {
            JsonObject collection = gson.fromJson(jsonContent, JsonObject.class);
            

            if (!isValidPostmanCollection(collection)) {
                throw new Exception("Invalid Postman collection format. Missing required 'info' or 'item' fields.");
            }
            

            String collectionName = extractCollectionName(collection);
            api.logging().logToOutput("Parsing collection: " + collectionName);
            

            JsonArray items = collection.getAsJsonArray("item");
            if (items != null) {
                parseItems(items, requests, "");
            }
            
            api.logging().logToOutput("Successfully parsed " + requests.size() + " requests from collection");
            
        } catch (JsonSyntaxException e) {
            throw new Exception("Invalid JSON format: " + e.getMessage());
        } catch (Exception e) {
            api.logging().logToError("Error parsing collection: " + e.getMessage());
            throw new Exception("Error parsing collection: " + e.getMessage());
        }
        
        return requests;
    }
    

    private boolean isValidPostmanCollection(JsonObject collection) {
        return collection.has("info") && collection.has("item");
    }
    

    private String extractCollectionName(JsonObject collection) {
        try {
            JsonObject info = collection.getAsJsonObject("info");
            if (info != null && info.has("name")) {
                return info.get("name").getAsString();
            }
        } catch (Exception e) {
            api.logging().logToError("Could not extract collection name: " + e.getMessage());
        }
        return "Unknown Collection";
    }
    

    private void parseItems(JsonArray items, List<PostmanRequest> requests, String folderPath) {
        for (JsonElement item : items) {
            try {
                JsonObject itemObj = item.getAsJsonObject();
                
                if (itemObj.has("item")) {
                    String folderName = itemObj.has("name") ? itemObj.get("name").getAsString() : "Unnamed Folder";
                    String newFolderPath = folderPath.isEmpty() ? folderName : folderPath + " → " + folderName;
                    
                    api.logging().logToOutput("Processing folder: " + newFolderPath);
                    
                    JsonArray subItems = itemObj.getAsJsonArray("item");
                    parseItems(subItems, requests, newFolderPath);
                    
                } else if (itemObj.has("request")) {
                    PostmanRequest request = parseRequest(itemObj, folderPath);
                    if (request != null) {
                        requests.add(request);
                        api.logging().logToOutput("Parsed request: " + request.getMethod() + " " + request.getEndpoint());
                    }
                }
            } catch (Exception e) {
                api.logging().logToError("Error parsing item: " + e.getMessage());
            }
        }
    }
    

    private PostmanRequest parseRequest(JsonObject item, String folderPath) {
        try {
            String name = item.has("name") ? item.get("name").getAsString() : "Unnamed Request";
            JsonObject requestObj = item.getAsJsonObject("request");
            
            if (requestObj == null) {
                return null;
            }
            
            String method = "GET"; // Default
            if (requestObj.has("method")) {
                method = requestObj.get("method").getAsString();
            }
            
            String url = extractUrl(requestObj);
            if (url == null || url.isEmpty()) {
                api.logging().logToError("Skipping request '" + name + "' - no valid URL found");
                return null;
            }
            
            PostmanRequest request = new PostmanRequest(name, method, url);
            request.setFolderPath(folderPath);
            
            if (item.has("description")) {
                String description = extractDescription(item.get("description"));
                request.setDescription(description);
            }
            
            extractHeaders(requestObj, request);
            
            extractQueryParameters(requestObj, request);
            
            extractBody(requestObj, request);
            
            return request;
            
        } catch (Exception e) {
            api.logging().logToError("Error parsing request: " + e.getMessage());
            return null;
        }
    }
    

    private String extractUrl(JsonObject requestObj) {
        if (!requestObj.has("url")) {
            return null;
        }
        
        JsonElement urlElement = requestObj.get("url");
        
        if (urlElement.isJsonPrimitive()) {
            return urlElement.getAsString();
        } else if (urlElement.isJsonObject()) {
            JsonObject urlObj = urlElement.getAsJsonObject();
            
            if (urlObj.has("raw")) {
                return urlObj.get("raw").getAsString();
            }
            
            StringBuilder urlBuilder = new StringBuilder();
            
            if (urlObj.has("protocol")) {
                urlBuilder.append(urlObj.get("protocol").getAsString()).append("://");
            } else {
                urlBuilder.append("https://");
            }
            
            if (urlObj.has("host")) {
                JsonElement hostElement = urlObj.get("host");
                if (hostElement.isJsonArray()) {
                    JsonArray hostArray = hostElement.getAsJsonArray();
                    for (int i = 0; i < hostArray.size(); i++) {
                        if (i > 0) urlBuilder.append(".");
                        urlBuilder.append(hostArray.get(i).getAsString());
                    }
                } else {
                    urlBuilder.append(hostElement.getAsString());
                }
            }
            
            if (urlObj.has("port")) {
                urlBuilder.append(":").append(urlObj.get("port").getAsString());
            }
            
            if (urlObj.has("path")) {
                JsonElement pathElement = urlObj.get("path");
                if (pathElement.isJsonArray()) {
                    JsonArray pathArray = pathElement.getAsJsonArray();
                    for (JsonElement pathPart : pathArray) {
                        urlBuilder.append("/").append(pathPart.getAsString());
                    }
                } else {
                    String path = pathElement.getAsString();
                    if (!path.startsWith("/")) {
                        urlBuilder.append("/");
                    }
                    urlBuilder.append(path);
                }
            }
            
            if (urlObj.has("query")) {
                JsonArray queryArray = urlObj.getAsJsonArray("query");
                if (queryArray.size() > 0) {
                    urlBuilder.append("?");
                    for (int i = 0; i < queryArray.size(); i++) {
                        if (i > 0) urlBuilder.append("&");
                        JsonObject queryParam = queryArray.get(i).getAsJsonObject();
                        String key = queryParam.has("key") ? queryParam.get("key").getAsString() : "";
                        String value = queryParam.has("value") ? queryParam.get("value").getAsString() : "";
                        
                        boolean disabled = queryParam.has("disabled") && queryParam.get("disabled").getAsBoolean();
                        if (!disabled) {
                            urlBuilder.append(key).append("=").append(value);
                        }
                    }
                }
            }
            
            return urlBuilder.toString();
        }
        
        return null;
    }
    

    private String extractDescription(JsonElement descElement) {
        if (descElement.isJsonPrimitive()) {
            return descElement.getAsString();
        } else if (descElement.isJsonObject()) {
            JsonObject descObj = descElement.getAsJsonObject();
            if (descObj.has("content")) {
                return descObj.get("content").getAsString();
            }
        }
        return "";
    }
    

    private void extractHeaders(JsonObject requestObj, PostmanRequest request) {
        if (!requestObj.has("header")) {
            return;
        }
        
        JsonArray headersArray = requestObj.getAsJsonArray("header");
        for (JsonElement headerElement : headersArray) {
            try {
                JsonObject headerObj = headerElement.getAsJsonObject();
                
                if (headerObj.has("key") && headerObj.has("value")) {
                    String key = headerObj.get("key").getAsString();
                    String value = headerObj.get("value").getAsString();
                    
                    boolean disabled = headerObj.has("disabled") && headerObj.get("disabled").getAsBoolean();
                    if (!disabled) {
                        request.addHeader(key, value);
                    }
                }
            } catch (Exception e) {
                api.logging().logToError("Error parsing header: " + e.getMessage());
            }
        }
    }
    

    private void extractQueryParameters(JsonObject requestObj, PostmanRequest request) {
        if (!requestObj.has("url")) {
            return;
        }
        
        try {
            JsonElement urlElement = requestObj.get("url");
            
            if (urlElement.isJsonObject()) {
                JsonObject urlObj = urlElement.getAsJsonObject();
                
                if (urlObj.has("query")) {
                    JsonArray queryArray = urlObj.getAsJsonArray("query");
                    
                    for (JsonElement queryElement : queryArray) {
                        JsonObject queryParam = queryElement.getAsJsonObject();
                        
                        String key = queryParam.has("key") ? queryParam.get("key").getAsString() : "";
                        String value = queryParam.has("value") ? queryParam.get("value").getAsString() : "";
                        
                        // Only add enabled query params
                        boolean disabled = queryParam.has("disabled") && queryParam.get("disabled").getAsBoolean();
                        if (!disabled && !key.isEmpty()) {
                            request.getQueryParameters().put(key, value);
                        }
                    }
                }
                
                if (urlObj.has("path")) {
                    JsonElement pathElement = urlObj.get("path");
                    if (pathElement.isJsonArray()) {
                        JsonArray pathArray = pathElement.getAsJsonArray();
                        for (JsonElement pathPart : pathArray) {
                            String pathSegment = pathPart.getAsString();
                            if (pathSegment.contains("{{") && pathSegment.contains("}}")) {
                                // Extract variable name from {{variableName}}
                                String varName = pathSegment.substring(
                                    pathSegment.indexOf("{{") + 2,
                                    pathSegment.indexOf("}}")
                                );
                                request.getPathVariables().put(varName, pathSegment);
                            }
                        }
                    }
                }
            }
            
            String url = request.getUrl();
            if (url != null && url.contains("?")) {
                String queryString = url.substring(url.indexOf("?") + 1);
                String[] params = queryString.split("&");
                
                for (String param : params) {
                    if (param.contains("=")) {
                        String[] keyValue = param.split("=", 2);
                        if (keyValue.length == 2) {
                            request.getQueryParameters().put(keyValue[0], keyValue[1]);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            api.logging().logToError("Error parsing query parameters: " + e.getMessage());
        }
    }


    private void extractBody(JsonObject requestObj, PostmanRequest request) {
        if (!requestObj.has("body")) {
            return;
        }
        
        try {
            JsonObject bodyObj = requestObj.getAsJsonObject("body");
            String mode = bodyObj.has("mode") ? bodyObj.get("mode").getAsString() : "";
            
            switch (mode) {
                case "raw":
                    if (bodyObj.has("raw")) {
                        request.setBody(bodyObj.get("raw").getAsString());
                    }
                    break;
                
                case "formdata":
                    if (bodyObj.has("formdata")) {
                        StringBuilder formData = new StringBuilder();
                        JsonArray formdataArray = bodyObj.getAsJsonArray("formdata");
                        for (JsonElement element : formdataArray) {
                            JsonObject formItem = element.getAsJsonObject();
                            if (formItem.has("key") && formItem.has("value")) {
                                boolean disabled = formItem.has("disabled") && formItem.get("disabled").getAsBoolean();
                                if (!disabled) {
                                    formData.append(formItem.get("key").getAsString())
                                           .append("=")
                                           .append(formItem.get("value").getAsString())
                                           .append("&");
                                }
                            }
                        }
                        if (formData.length() > 0) {
                            formData.deleteCharAt(formData.length() - 1); // Remove trailing &
                        }
                        request.setBody(formData.toString());
                    }
                    break;
                
                case "urlencoded":
                    if (bodyObj.has("urlencoded")) {
                        StringBuilder urlencoded = new StringBuilder();
                        JsonArray urlencodedArray = bodyObj.getAsJsonArray("urlencoded");
                        for (JsonElement element : urlencodedArray) {
                            JsonObject formItem = element.getAsJsonObject();
                            if (formItem.has("key") && formItem.has("value")) {
                                boolean disabled = formItem.has("disabled") && formItem.get("disabled").getAsBoolean();
                                if (!disabled) {
                                    urlencoded.append(formItem.get("key").getAsString())
                                             .append("=")
                                             .append(formItem.get("value").getAsString())
                                             .append("&");
                                }
                            }
                        }
                        if (urlencoded.length() > 0) {
                            urlencoded.deleteCharAt(urlencoded.length() - 1); // Remove trailing &
                        }
                        request.setBody(urlencoded.toString());
                    }
                    break;
                    
                default:
                    if (bodyObj.has("raw")) {
                        request.setBody(bodyObj.get("raw").getAsString());
                    }
                    break;
            }
        } catch (Exception e) {
            api.logging().logToError("Error parsing body: " + e.getMessage());
        }
    }
}