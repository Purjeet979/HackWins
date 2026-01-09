const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const nodemailer = require("nodemailer");

admin.initializeApp();
const db = admin.firestore();

// --- CONFIGURATION ---
// TODO: Replace with your actual API Key from AI Studio
const GEMINI_API_KEY = "AIzaSyDYs_6-JX7xmvhE5AmdVh8NDTm1fkx_bNs"; 
// TODO: Replace with the email you want to send FROM (and your App Password)
const SENDER_EMAIL = "your-email@gmail.com"; 
const SENDER_PASSWORD = "your-app-password"; 

const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);

exports.generateWeeklyLetter = functions.https.onRequest(async (req, res) => {
    // 1. Setup - We use a hardcoded ID for the hackathon demo
    const userId = "dadi_01"; 
    const recipientEmail = "grandson@example.com"; // Change this to your email to test!

    try {
        // 2. Fetch recent conversations from Firestore
        const chatsSnapshot = await db.collection("users").doc(userId)
            .collection("conversations")
            .orderBy("timestamp", "desc")
            .limit(20)
            .get();

        let transcript = "";
        chatsSnapshot.forEach(doc => {
            const data = doc.data();
            transcript += `${data.speaker}: ${data.text}\n`;
        });

        if (!transcript) {
            return res.send("No conversations found yet. Talk to the app first!");
        }

        // 3. Ask Gemini to write the letter
        const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
        const prompt = `
            You are 'Dadi', an elderly Indian grandmother. 
            Read these conversation logs between me and my AI assistant:
            ${transcript}
            
            Write a warm, loving email to my grandson Rohan.
            - Mention specific things I talked about (pain, food, memories).
            - Use Hinglish phrases (beta, khana khaya?).
            - Keep it short (under 100 words).
            - Sign it as 'Dadi'.
        `;

        const result = await model.generateContent(prompt);
        const letterContent = result.response.text();

        // 4. Send the Email
        let transporter = nodemailer.createTransport({
            service: 'gmail',
            auth: { user: SENDER_EMAIL, pass: SENDER_PASSWORD }
        });

        await transporter.sendMail({
            from: `"Dadi" <${SENDER_EMAIL}>`,
            to: recipientEmail,
            subject: "Rohan, how are you beta?",
            text: letterContent
        });

        res.send(`Success! Email sent:\n\n${letterContent}`);

    } catch (error) {
        console.error(error);
        res.status(500).send(error.toString());
    }
});