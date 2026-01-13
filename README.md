# 🌸 Sneh Saathi — A Voice Companion for Elderly Care

**Sneh Saathi** is a warm, voice-first AI companion designed for elderly Indian users, especially those living alone.  
It focuses on **emotional well-being, safety, memory, and family connection**, using simple Hinglish conversations instead of complex interfaces.

---

## 🧠 Problem Statement

Many elderly people:
- Feel lonely and unheard
- Struggle with modern smartphone interfaces
- Are vulnerable to phone scams (OTP, fake KYC calls)
- Forget medicines or daily routines
- Feel disconnected from family members who are busy

**Sneh Saathi** addresses these issues through a **human-like voice companion** that listens patiently and responds with empathy.

---

## 🌟 Key Features

### 1️⃣ Voice-Based Companion (Core)
- Fully voice-driven interaction (no typing required)
- Calm, respectful Hinglish responses
- Always addresses the user as **“Dadi”** and **“Aap”**
- Avoids child-like or disrespectful terms (e.g. *beta*)

---

### 2️⃣ 🛡️ Scam Shield (Safety First)
- Detects scam-related keywords like:
  - OTP
  - Bank / KYC
  - Police threats
- Immediately warns the user **not to share information**
- Encourages contacting family (e.g. grandson Rohan)

**Impact:** Prevents financial and emotional exploitation.

---

### 3️⃣ 🧠 Contextual Memory (Firebase)
- Remembers important, recurring details:
  - Health issues (BP, knee pain)
  - Family references
  - Ongoing concerns
- Uses memory naturally in conversation  
  _“Kal pair ka dard bola tha aapne, aaj thoda theek lag raha hai?”_

---

### 4️⃣ 🕰️ Nostalgia Mode
- Detects when the user talks about the past (“pehle”, “yaad hai”)
- Responds gently without interrogation
- Encourages comforting memories instead of questioning

---

### 5️⃣ 💊 Medication Reminder
- Scheduled daily reminders using **WorkManager**
- Gentle voice reminders (not alarms)
- Example:  
  _“Dadi, baaton baaton mein bhool na jaayein, dawa ka time ho gaya hai.”_

---

### 6️⃣ ✍️ Grandma’s Ghostwriter (WOW Feature)
- Daily conversations are summarized and stored in Firebase
- A weekly script generates a **heartfelt message in Dadi’s voice**
- Sent to family via WhatsApp (demoed using Twilio)

**Example message:**
> “Rohan, is hafte pair thoda dard kar raha tha, par maine kheer banayi. Tumhari yaad aayi. Jab time mile call karna. – Dadi”

This **closes the loneliness loop** by prompting family to reconnect.

---

### 7️⃣ 🤝 Volunteers (Planned – Phase 2)
- Designed as a **human-in-the-loop safety net**
- When emotional distress is detected, trusted volunteers or family can step in
- Ensures AI is not the only support in critical moments

---

## 🧩 Technology Stack

### 📱 Frontend
- Android (Kotlin)
- Jetpack Compose
- Text-to-Speech (Hindi)
- SpeechRecognizer API

### ☁️ Backend / Cloud
- **Firebase Firestore** (memory, summaries)
- Firebase WorkManager (reminders)
- Local Node.js script for Ghostwriter demo

### 🤖 AI
- Groq API (LLM responses)
- Prompt-engineered personality system (elder-safe)

---

## 🟢 Google Technology Usage (Mandatory Requirement)
- **Firebase Firestore**
- Firebase background services
- (Gemini planned for extended deployment)

---

## 🎥 Demo & Links

- **GitHub Repository:**  
  https://github.com/Purjeet979/HackWins

- **Demo Video (3 minutes):**  
 https://drive.google.com/drive/folders/17j_PTlFP8RmSxmHQ0Uu3O9PmVIW0VLa6?usp=sharing

  

## 🧡 Why Sneh Saathi Matters

Sneh Saathi is not just an app — it is:
- A listener
- A protector
- A reminder
- A bridge between generations

It is designed with **dignity, empathy, and safety** at its core.

---

## 👥 Team

- **Developer:** Purjeet  
- **UI / Design:** Soham,Adiya,Parth  
- **Project:** Hackathon Submission

---

> “Technology should not replace humans — it should bring them closer.”  
> **— Sneh Saathi**
