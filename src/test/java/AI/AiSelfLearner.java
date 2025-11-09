package AI;

import com.mongodb.client.*;
import models.MongoConnector;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

/**
 * 🧠 AI Self-Learner (Multi-Project Version)
 * ------------------------------------------------------------
 * - Fetches past summaries from MongoDB
 * - Supports per-project learning contexts
 * - Helps Gemini generate project-specific test suggestions
 */
public class AiSelfLearner {

    /**
     * Fetch latest summaries from all projects (generic).
     */
    public static List<String> getPastSummaries(int limit) {
        MongoDatabase db = MongoConnector.connect();
        MongoCollection<Document> col = db.getCollection("ai_reports");

        List<String> summaries = new ArrayList<>();
        for (Document doc : col.find().sort(new Document("timestamp", -1)).limit(limit)) {
            summaries.add(doc.getString("aiSummary"));
        }
        return summaries;
    }

    /**
     * Fetch past AI summaries only for a specific project.
     */
    public static List<String> getPastSummariesByProject(String projectName, int limit) {
        MongoDatabase db = MongoConnector.connect();
        MongoCollection<Document> col = db.getCollection("ai_reports");

        List<String> summaries = new ArrayList<>();
        for (Document doc : col.find(new Document("projectName", projectName))
                .sort(new Document("timestamp", -1)).limit(limit)) {
            summaries.add(doc.getString("aiSummary"));
        }
        return summaries;
    }

    /**
     * 🔍 Builds context for a specific project’s past insights.
     */
    public static String buildPromptContext(String projectName) {
        List<String> recent = getPastSummariesByProject(projectName, 3);

        StringBuilder ctx = new StringBuilder("📚 Learning from previous AI summaries for project: ")
                .append(projectName).append("\n\n");

        if (recent.isEmpty()) {
            ctx.append("(No past insights found — starting fresh!)\n");
        } else {
            for (String s : recent) {
                ctx.append("- ").append(s).append("\n");
            }
        }

        return ctx.toString();
    }
}
