package com.example.demo.controller;

import com.example.demo.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*") // allow CORS for testing
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping
    public String chat(@RequestParam String message) {
        return geminiService.generateResponse(message);
    }
}
