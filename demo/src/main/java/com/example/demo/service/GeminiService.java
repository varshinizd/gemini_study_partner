package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class GeminiService {

    private final String apiKey;
    private final String model;
    private final String pdfPath;
    private final String uploadEndpoint;
    private final String generateContentEndpoint;
    private String currentFileUri;
    private final ObjectMapper objectMapper;

    public GeminiService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.model}") String model,
            @Value("${gemini.pdf.path}") String pdfPath) {
        this.apiKey = apiKey;
        this.model = model;
        this.pdfPath = pdfPath;
        this.uploadEndpoint = "https://generativelanguage.googleapis.com/upload/v1beta/files?key=" + apiKey;
        this.generateContentEndpoint = "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key=" + apiKey;
        this.currentFileUri = null;
        this.objectMapper = new ObjectMapper();
    }

    public String uploadPdf() throws Exception {
        // Validate file
        if (!Files.exists(Paths.get(pdfPath))) {
            throw new Exception("PDF file not found at: " + pdfPath);
        }
        if (!Files.isReadable(Paths.get(pdfPath))) {
            throw new Exception("PDF file is not readable: " + pdfPath);
        }

        HttpClient client = HttpClient.newHttpClient();

        // Read PDF
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(Paths.get(pdfPath));
            if (fileContent.length == 0) {
                throw new Exception("PDF file is empty: " + pdfPath);
            }
        } catch (Exception e) {
            throw new Exception("Failed to read PDF from path: " + pdfPath, e);
        }

        // Create multipart form data
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String multipartFormData = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"devunit1.pdf\"\r\n" +
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
                    return result.isEmpty() ? "No text found in response. Response: " + response.body() : result;
                }
            }
            return "No text found in response. Response: " + response.body();
        } catch (Exception e) {
            throw new Exception("Failed to parse content response JSON: " + e.getMessage() + ". Response: " + response.body(), e);
        }
    }

    public void clearFileUri() {
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