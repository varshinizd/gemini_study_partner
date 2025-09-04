📘 Study Notes Chatbot (RAG-style with Gemini API)
🚀 Project Overview

This project implements a Retrieval-Augmented Generation (RAG)-style chatbot powered by the open-source Gemini API endpoint.
The chatbot helps students focus on studying by:

Answering questions strictly from the uploaded notes/files.

Ignoring irrelevant/distracting queries using a prompt pipeline that keeps responses aligned with study material.

Providing a smooth learning experience by acting as a study companion.

✨ Features

📂 Upload Notes → Students can upload their study materials (PDFs, text, etc.).

🧠 RAG-style Question Answering → The chatbot retrieves relevant content from notes and uses Gemini API to answer.

🎯 Distraction Filtering → Handles off-topic questions with a custom prompt pipeline to maintain study focus.

⚡ Lightweight & Flexible → Easy to adapt for different subjects and file types.

🛠️ Tech Stack

Backend: Java + Spring Boot (or your backend choice)

Frontend: JSP / HTML / CSS / JavaScript (React optional)

API: Gemini API Endpoint

Architecture: Retrieval-Augmented Generation (RAG)

🔧 How It Works

Upload Notes: User uploads subject-specific files.

Classification: The system identifies the relevant subject/folder.

Search & Retrieval: Extracts and searches content from the uploaded files.

Gemini Prompt Pipeline:

If answer exists → Responds with content from notes.

If not found → Returns fallback Gemini API response.

If off-topic → Politely redirects user back to study context.

🚀 Setup & Installation

Clone the repository:

git clone https://github.com/your-username/study-notes-chatbot.git
cd study-notes-chatbot


Configure Gemini API endpoint in the backend service.

Run the backend (Spring Boot):

./mvn spring-boot:run


Launch the frontend (HTML).

Upload notes and start chatting!

📌 Future Enhancements

🔍 Support for advanced search (vector DB + embeddings).

📝 Quiz generator based on uploaded notes.

📊 Student progress dashboard.
