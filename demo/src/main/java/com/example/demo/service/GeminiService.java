package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class GeminiService {

    private final String apiKey;
    private final String model;
    private final String uploadEndpoint;
    private final String generateContentEndpoint;
    private String currentFileUri;
    private final ObjectMapper objectMapper;

    public GeminiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.uploadEndpoint = "https://generativelanguage.googleapis.com/upload/v1beta/files?key=" + apiKey;
        this.generateContentEndpoint = "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key=" + apiKey;
        this.currentFileUri = null;
        this.objectMapper = new ObjectMapper();
    }

    public String uploadPdf(MultipartFile file) throws Exception {
        // Validate file
        if (file.isEmpty()) {
            throw new Exception("No file uploaded or file is empty.");
        }
        if (!file.getContentType().equals("application/pdf")) {
            throw new Exception("Uploaded file is not a PDF. Content-Type: " + file.getContentType());
        }
        System.out.println("Uploading file: " + file.getOriginalFilename() + ", Size: " + file.getSize() + " bytes");

        HttpClient client = HttpClient.newHttpClient();

        // Read PDF
        byte[] fileContent;
        try {
            fileContent = file.getBytes();
            System.out.println("Read " + fileContent.length + " bytes from file");
        } catch (Exception e) {
            throw new Exception("Failed to read uploaded PDF: " + e.getMessage(), e);
        }

        // Create multipart form data
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String multipartFormData = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getOriginalFilename() + "\"\r\n" +
                "Content-Type: application/pdf\r\n\r\n";

        byte[] body = concatenateByteArrays(
                multipartFormData.getBytes(StandardCharsets.UTF_8),
                fileContent,
                ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadEndpoint))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response;
        try {
            System.out.println("Sending request to Gemini API: " + uploadEndpoint);
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new Exception("Failed to upload PDF to Gemini API: " + e.getMessage(), e);
        }

        // Log response
        System.out.println("Upload response status: " + response.statusCode());
        System.out.println("Upload response body: " + response.body());

        if (response.statusCode() == 200) {
            try {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                JsonNode fileNode = jsonNode.get("file");
                if (fileNode != null) {
                    JsonNode uriNode = fileNode.get("uri");
                    if (uriNode != null && uriNode.isTextual()) {
                        currentFileUri = uriNode.asText();
                        System.out.println("File URI stored: " + currentFileUri);
                        return currentFileUri;
                    }
                }
                throw new Exception("File URI not found in response. Response: " + response.body());
            } catch (Exception e) {
                throw new Exception("Failed to parse response JSON: " + e.getMessage() + ". Response: " + response.body(), e);
            }
        } else {
            throw new Exception("Failed to upload file. Status: " + response.statusCode() + ", Response: " + response.body());
        }
    }

    public String generateContentFromPdf(String prompt) throws Exception {
        if (currentFileUri == null) {
            throw new Exception("No PDF uploaded. Please upload the PDF first.");
        }

        // Handle study assistant logic
        String responseText;
        if (prompt.toLowerCase().startsWith("user message: hi") || prompt.toLowerCase().startsWith("user message: hello")) {
            responseText = "Let's study.";
        } else if (prompt.toLowerCase().startsWith("user message: ok") || prompt.toLowerCase().startsWith("user message: okay")) {
            responseText = "Pick a topic to study: [List topics from PDF - placeholder as actual extraction depends on PDF content]";
        } else {
            String requestBody = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "fileData": {
                                "fileUri": "%s",
                                "mimeType": "application/pdf"
                              }
                            },
                            {
                              "text": "%s"
                            }
                          ]
                        }
                      ]
                    }
                    """.formatted(currentFileUri, prompt);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(generateContentEndpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response;
            try {
                System.out.println("Sending content generation request to Gemini API: " + generateContentEndpoint);
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw new Exception("Failed to generate content from Gemini API: " + e.getMessage(), e);
            }

            // Log response
            System.out.println("Generate content response status: " + response.statusCode());
            System.out.println("Generate content response body: " + response.body());

            try {
                JsonNode jsonNode = objectMapper.readTree(response.body());
                JsonNode candidatesNode = jsonNode.path("candidates");
                if (candidatesNode.isArray() && candidatesNode.size() > 0) {
                    JsonNode contentNode = candidatesNode.get(0).path("content").path("parts");
                    if (contentNode.isArray() && contentNode.size() > 0) {
                        StringBuilder allText = new StringBuilder();
                        for (JsonNode part : contentNode) {
                            String text = part.path("text").asText("");
                            if (!text.isEmpty()) {
                                allText.append(text).append(" ");
                            }
                        }
                        String result = allText.toString().trim();
                        responseText = result.isEmpty() ? "Couldn't find relevant information." : result;
                    } else {
                        responseText = "Couldn't find relevant information.";
                    }
                } else {
                    responseText = "Couldn't find relevant information.";
                }
            } catch (Exception e) {
                throw new Exception("Failed to parse content response JSON: " + e.getMessage() + ". Response: " + response.body(), e);
            }
        }

        return responseText;
    }

    public void clearFileUri() {
        System.out.println("Clearing stored file URI: " + currentFileUri);
        this.currentFileUri = null;
    }

    private byte[] concatenateByteArrays(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}