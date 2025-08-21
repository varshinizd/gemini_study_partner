package com.example.demo.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final String apiKey;
    private final String model;

    public GeminiService(@Value("${gemini.api.key}") String apiKey,
                         @Value("${gemini.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generateResponse(String prompt) {
        try (Client client = new Client()) {
            // Here you can add auth if needed using apiKey
            GenerateContentResponse response = client.models.generateContent(
                    model,
                    prompt,
                    null
            );
            return response.text();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error communicating with Gemini API.";
        }
    }
}
