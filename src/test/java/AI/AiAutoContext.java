package AI;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import models.MongoConnector;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AiAutoContext {

    // ======================
    // TOKEN MANAGEMENT
    // ======================
    public static String getToken() {
        MongoDatabase db = MongoConnector.connect();
        MongoCollection<Document> col = db.getCollection("ai_context");
        Document doc = col.find().first();
        return doc != null ? doc.getString("token") : "no-token";
    }

    public static void setToken(String token) {
        MongoDatabase db = MongoConnector.connect();
        MongoCollection<Document> col = db.getCollection("ai_context");
        col.drop(); // Always keep latest token
        col.insertOne(new Document("token", token));
    }

    // ======================
    // ENDPOINT INFERENCE
    // ======================
    public static String inferEndpoint(String endpoint) {
        if (endpoint == null) return "";

        if (endpoint.contains("{id}")) {
            int randomId = new Random().nextInt(10) + 1;
            endpoint = endpoint.replace("{id}", String.valueOf(randomId));
        }
        if (endpoint.contains("{userId}")) {
            int userId = new Random().nextInt(100) + 1000;
            endpoint = endpoint.replace("{userId}", String.valueOf(userId));
        }
        if (endpoint.contains("{page}")) {
            endpoint = endpoint.replace("{page}", String.valueOf(new Random().nextInt(3) + 1));
        }

        return endpoint;
    }

    // ======================
    // SMART PAYLOAD GENERATION (AI)
    // ======================
    public static String smartPayload(String method, String endpoint) {
        String prompt = "Generate a valid JSON payload for an HTTP " + method +
                " request to endpoint: " + endpoint +
                ". Include realistic test data and structure it properly.";

        // Call Gemini and auto-store the result
        String aiResponse = GeminiAI.generate(prompt);
        storeContext("payload_prompt", prompt, aiResponse);
        return aiResponse;
    }

    // ======================
    // SMART QUERY PARAMETER GENERATION
    // ======================
    public static Map<String, String> smartQueryParams(String endpoint) {
        Map<String, String> params = new HashMap<>();

        if (endpoint.contains("search")) {
            params.put("q", "sample");
        } else if (endpoint.contains("filter")) {
            params.put("status", "active");
        } else {
            params.put("limit", "10");
            params.put("page", "1");
        }

        storeContext("query_params", endpoint, params.toString());
        return params;
    }

    // ======================
    // AUTO REQUEST BUILDER
    // ======================
    public static Map<String, Object> autoBuildRequest(String method, String endpoint) {
        Map<String, Object> request = new HashMap<>();
        String token = getToken();
        String finalEndpoint = inferEndpoint(endpoint);

        request.put("token", token);
        request.put("endpoint", finalEndpoint);
        request.put("payload", smartPayload(method, finalEndpoint));
        request.put("queryParams", smartQueryParams(finalEndpoint));

        storeContext("auto_request", endpoint, request.toString());
        return request;
    }

    // ======================
    // STORE CONTEXT (MONGO)
    // ======================
    private static void storeContext(String type, String input, String result) {
        try {
            MongoDatabase db = MongoConnector.connect();
            MongoCollection<Document> col = db.getCollection("ai_context_logs");

            Document log = new Document("type", type)
                    .append("input", input)
                    .append("result", result)
                    .append("timestamp", System.currentTimeMillis());

            col.insertOne(log);
        } catch (Exception e) {
            System.out.println("⚠️ Failed to store AI context log: " + e.getMessage());
        }
    }

    // ======================
    // AI DECISION MAKER
    // ======================
    public static String decideAction(String context) {
        String prompt = "Given this context: " + context +
                ", decide which HTTP method (GET, POST, PUT, DELETE) is most appropriate.";
        String aiResponse = GeminiAI.generate(prompt);
        storeContext("decision", context, aiResponse);
        return aiResponse;
    }
}
