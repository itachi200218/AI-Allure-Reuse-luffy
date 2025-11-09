package AI;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import models.MongoConnector;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 💾 AI Memory Saver — Logs both executions & summaries
 * --------------------------------------------------------
 * 🧠 Multi-Project Aware
 * - Each document stores "project" name
 * - saveLearning(): Logs AI interactions
 * - saveExecution(): Logs structured API runs
 * - saveReport(): Logs test summaries + compares last 2 runs of that project
 */
public class AiMemorySaver {

    /**
     * 🟩 Saves each AI interaction (prompt + response)
     * Collection: ai_executions
     */
    public static void saveLearning(String projectName, String prompt, String aiResponse) {
        try {
            MongoDatabase db = MongoConnector.connect();
            MongoCollection<Document> col = db.getCollection("ai_executions");

            Document doc = new Document()
                    .append("project", projectName)
                    .append("type", "learning")
                    .append("prompt", prompt)
                    .append("aiResponse", aiResponse)
                    .append("timestamp", System.currentTimeMillis());

            col.insertOne(doc);
            System.out.println("✅ [" + projectName + "] Saved AI learning.");
        } catch (Exception e) {
            System.err.println("❌ Failed to save AI learning: " + e.getMessage());
        }
    }

    /**
     * 🧩 Saves structured API execution logs per project
     * Collection: ai_executions
     */
    public static void saveExecution(String projectName,
                                     String method,
                                     String endpoint,
                                     String payload,
                                     String response,
                                     int status) {
        try {
            MongoDatabase db = MongoConnector.connect();
            MongoCollection<Document> col = db.getCollection("ai_executions");

            Document doc = new Document()
                    .append("project", projectName)
                    .append("type", "execution")
                    .append("method", method)
                    .append("endpoint", endpoint)
                    .append("payload", payload == null ? "" : payload)
                    .append("response", response == null ? "" : response)
                    .append("status", status)
                    .append("timestamp", System.currentTimeMillis());

            col.insertOne(doc);
            System.out.println("✅ [" + projectName + "] Saved execution: " + method + " " + endpoint + " (" + status + ")");
        } catch (Exception e) {
            System.err.println("❌ Failed to save execution: " + e.getMessage());
        }
    }

    /**
     * 🟦 Saves AI-generated test summary reports per project
     * Collection: ai_reports
     * ✅ Compares last 2 executions of the same project only
     */
    public static void saveReport(String projectName, String testName, String aiSummary, String records) {
        try {
            MongoDatabase db = MongoConnector.connect();
            MongoCollection<Document> execCol = db.getCollection("ai_executions");
            MongoCollection<Document> reportCol = db.getCollection("ai_reports");

            // 🔹 Fetch last two executions for this project
            List<Document> lastTwo = new ArrayList<>();
            try (MongoCursor<Document> cursor = execCol.find(Filters.eq("project", projectName))
                    .sort(Sorts.descending("timestamp"))
                    .limit(2)
                    .iterator()) {
                while (cursor.hasNext()) {
                    lastTwo.add(cursor.next());
                }
            }

            String comparisonText = "⚠️ Not enough execution data for comparison.";
            if (lastTwo.size() == 2) {
                Document current = lastTwo.get(0);
                Document previous = lastTwo.get(1);

                double currentRate = current.getDouble("successRate") != null ? current.getDouble("successRate") : 0.0;
                double previousRate = previous.getDouble("successRate") != null ? previous.getDouble("successRate") : 0.0;
                double diff = currentRate - previousRate;

                comparisonText = String.format("""
                        🤖 AI Comparative Insight — Project: %s
                        • Previous Run Success Rate: %.2f%%
                        • Current Run Success Rate: %.2f%%
                        • Change: %+ .2f%%
                        """, projectName, previousRate, currentRate, diff);
            }

            // 🧾 Build the Mongo report document
            Document doc = new Document()
                    .append("project", projectName)
                    .append("testName", testName)
                    .append("aiSummary", aiSummary)
                    .append("records", records)
                    .append("comparison", comparisonText)
                    .append("timestamp", System.currentTimeMillis());

            reportCol.insertOne(doc);
            System.out.println("✅ [" + projectName + "] Saved AI summary report + comparison.");
        } catch (Exception e) {
            System.err.println("❌ Failed to save AI summary: " + e.getMessage());
        }
    }
}
