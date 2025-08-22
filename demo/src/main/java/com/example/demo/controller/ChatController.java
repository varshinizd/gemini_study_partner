package com.example.demo.controller;

import com.example.demo.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*") // Allow CORS for testing
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/upload-pdf")
    public ResponseEntity<String> uploadPdf() {
        try {
            String fileUri = geminiService.uploadPdf();
            return ResponseEntity.ok("PDF uploaded successfully. File URI: " + fileUri);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error uploading PDF: " + e.getMessage());
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