const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const nodemailer = require("nodemailer");

admin.initializeApp();
const db = admin.firestore();

// 🔑 Gemini setup
const genAI = new GoogleGenerativeAI(
  functions.config().gemini.key
);


// 📧 Email setup (Gmail example)
const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: "commercialsude36@gmail.com",
    pass: "lemzhfvxkqibhnyz"
  }
});

// 🧓 GRANDMA'S GHOSTWRITER — WEEKLY FUNCTION
exports.grandmasGhostwriter =
  functions.pubsub
    .schedule("every sunday 09:00")
    .timeZone("Asia/Kolkata")
    .onRun(async () => {

      // 1️⃣ Fetch last 7 daily summaries
      const snapshot = await db
        .collection("daily_summaries")
        .orderBy("timestamp", "desc")
        .limit(7)
        .get();

      if (snapshot.empty) {
        console.log("No summaries found");
        return null;
      }

      const summaries = snapshot.docs.map(doc => doc.data().text);

      // 2️⃣ Gemini prompt
      const prompt = `
       Write a 100-word heartfelt message from an Indian grandmother (Dadi)
       to her grandson Rohan who lives in Mumbai.

        Tone: warm, emotional, loving.
        Language: Hinglish.
        Voice: grandmother.

        Weekly moments:
        ${summaries.join("\n")}

        End by gently asking him to call.
        Sign as: "- Dadi"
        `;

      const model = genAI.getGenerativeModel({ model: "gemini-pro" });
      const result = await model.generateContent(prompt);
      const letter = result.response.text();

      // 3️⃣ Send email
      await transporter.sendMail({
        from: "YOUR_EMAIL@gmail.com",
        to: "ROHAN_EMAIL@gmail.com",
        subject: "Dadi ki taraf se 💛",
        text: letter
      });

      console.log("Weekly letter sent successfully");
      return null;
    });
