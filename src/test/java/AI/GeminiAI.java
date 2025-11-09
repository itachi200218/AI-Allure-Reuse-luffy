package AI;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import models.MongoConnector;
import org.bson.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GeminiAI {

    private static final String API_KEY = EnvConfig.get("OPENROUTER_API_KEY");
    private static final String MODEL = "openai/gpt-4o-mini";
    private static final String URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String generate(String prompt) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "⚠️ OpenRouter API key not set. Please check your .env file.";
        }

        try {
            System.out.println("🔑 Using OpenRouter API Key: " + API_KEY.substring(0, 10) + "**********");
            System.out.println("🧠 Model: " + MODEL);
            System.out.println("💬 Prompt: " + prompt);

            // ✅ Build JSON body
            String body = """
            {
              "model": "%s",
              "messages": [
                {"role": "system", "content": "You are a helpful AI assistant specialized in API test summarization."},
                {"role": "user", "content": "%s"}
              ]
            }
            """.formatted(MODEL, prompt.replace("\"", "\\\""));

            // ✅ Send request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("X-Title", "AI Automation Framework")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // ✅ Handle errors
            if (response.statusCode() == 429) {
                return "⚠️ Rate limit hit. Try again later.";
            }
            if (response.statusCode() / 100 != 2) {
                return "⚠️ API Error " + response.statusCode() + ": " + response.body();
            }

            // ✅ Parse AI response
            JsonNode root = mapper.readTree(response.body());
            String aiResponse = root.path("choices").get(0)
                    .path("message").path("content").asText();

            if (aiResponse == null || aiResponse.isBlank()) {
                return "⚠️ Empty AI response received.";
            }

            // ✅ Save report to MongoDB
            saveToMongo(prompt, aiResponse);

            // ✅ Generate Allure Comparison Report
            try {
                AiSummaryReporter.logSummary(); // <-- new method, no params
            } catch (Exception e) {
                System.err.println("⚠️ AiSummaryReporter failed: " + e.getMessage());
            }

            System.out.println("✅ OpenRouter AI Response Generated Successfully!");
            return aiResponse;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ OpenRouterAI error: " + e.getMessage();
        }
    }

    private static void saveToMongo(String prompt, String aiSummary) {
        try {
            MongoDatabase db = MongoConnector.connect();
            MongoCollection<Document> col = db.getCollection("ai_reports");

            Document doc = new Document("testName", "JobAPI_TestRun")
                    .append("prompt", prompt)
                    .append("aiSummary", aiSummary)
                    .append("timestamp", Instant.now().toEpochMilli());

            col.insertOne(doc);
            System.out.println("✅ AI summary saved to MongoDB successfully.");
        } catch (Exception e) {
            System.out.println("⚠️ Failed to save AI summary to MongoDB: " + e.getMessage());
        }
    }
}
