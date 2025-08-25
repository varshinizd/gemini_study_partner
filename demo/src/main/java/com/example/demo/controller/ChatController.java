package com.example.demo.controller;

import com.example.demo.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*") // Allow CORS for testing
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/upload-pdf")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            String fileUri = geminiService.uploadPdf(file);
            return ResponseEntity.ok("PDF uploaded successfully. File URI: " + fileUri);
        } catch (Exception e) {
            e.printStackTrace();
            String message = "Error uploading PDF: " + e.getMessage();
            if (e.getMessage().contains("Status: 413")) {
                message += " (File too large for Gemini API. Try a smaller file, e.g., <20MB.)";
            } else if (e.getMessage().contains("No file uploaded")) {
                message += " (Ensure a valid PDF is selected.)";
            }
            return ResponseEntity.status(500).body(message);
        }
    }

    @PostMapping("/pdf-content")
    public ResponseEntity<String> generatePdfContent(@RequestParam String prompt) {
        try {
            String response = geminiService.generateContentFromPdf(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error generating content: " + e.getMessage());
        }
    }

    @PostMapping("/clear-pdf")
    public ResponseEntity<String> clearPdf() {
        geminiService.clearFileUri();
        return ResponseEntity.ok("Stored PDF cleared.");
    }
}