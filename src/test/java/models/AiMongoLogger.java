package models;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

public class AiMongoLogger {

    private static String currentSessionId = null;

    // 🔹 Create or reuse session per test run
    private static String getCurrentSessionId() {
        if (currentSessionId == null) {
            currentSessionId = Instant.now().toString() + "_" + UUID.randomUUID();
        }
        return currentSessionId;
    }

    private static String getMainProjectName() {
        String dir = System.getProperty("user.dir");
        if (dir != null) {
            String[] parts = dir.replace("\\", "/").split("/");
            return parts[parts.length - 1];
        }
        return "DefaultFramework";
    }

    private static String getSubProjectName() {
        String customName = System.getProperty("projectName");
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }

        String className = Thread.currentThread().getStackTrace()[3].getFileName();
        if (className != null && className.endsWith(".java")) {
            return className.replace(".java", "");
        }
        return "DefaultTestClass";
    }

    public static void logExecution(String method, String endpoint, String payload, String response, int status) {
        MongoDatabase db = MongoConnector.connect();
        MongoCollection<Document> col = db.getCollection("ai_executions");

        String mainProject = getMainProjectName();
        String subProject = getSubProjectName();
        String sessionId = getCurrentSessionId();

        Document endpointData = new Document("method", method)
                .append("endpoint", endpoint)
                .append("payload", payload)
                .append("response", response)
                .append("status", status)
                .append("timestamp", Instant.now().toString());

        // 🔸 Try to find the document for project + subproject
        Document existingProject = col.find(
                Filters.and(Filters.eq("project", mainProject), Filters.eq("subproject", subProject))
        ).first();

        if (existingProject != null) {
            // Find if this sessionId exists
            Document existingSession = existingProject.getList("sessions", Document.class)
                    .stream()
                    .filter(s -> sessionId.equals(s.getString("sessionId")))
                    .findFirst()
                    .orElse(null);

            if (existingSession != null) {
                // Add to existing session
                col.updateOne(
                        Filters.and(Filters.eq("project", mainProject),
                                Filters.eq("subproject", subProject),
                                Filters.eq("sessions.sessionId", sessionId)),
                        Updates.push("sessions.$.endpoints", endpointData)
                );
            } else {
                // Create new session
                Document newSession = new Document("sessionId", sessionId)
                        .append("createdAt", Instant.now().toString())
                        .append("endpoints", Arrays.asList(endpointData));
                col.updateOne(
                        Filters.and(Filters.eq("project", mainProject), Filters.eq("subproject", subProject)),
                        Updates.push("sessions", newSession)
                );
            }

        } else {
            // First-time project/subproject
            Document newSession = new Document("sessionId", sessionId)
                    .append("createdAt", Instant.now().toString())
                    .append("endpoints", Arrays.asList(endpointData));

            Document newDoc = new Document("project", mainProject)
                    .append("subproject", subProject)
                    .append("sessions", Arrays.asList(newSession));

            col.insertOne(newDoc);
        }
    }

    public static void logAIHint(String method, String endpoint, String hint) {
        MongoDatabase db = MongoConnector.connect();
        MongoCollection<Document> col = db.getCollection("ai_hints");

        String mainProject = getMainProjectName();
        String subProject = getSubProjectName();

        Document hintData = new Document("method", method)
                .append("endpoint", endpoint)
                .append("hint", hint)
                .append("timestamp", Instant.now().toString());

        Document existingProject = col.find(
                Filters.and(Filters.eq("project", mainProject), Filters.eq("subproject", subProject))
        ).first();

        if (existingProject != null) {
            col.updateOne(
                    Filters.and(Filters.eq("project", mainProject), Filters.eq("subproject", subProject)),
                    Updates.push("hints", hintData)
            );
        } else {
            Document newDoc = new Document("project", mainProject)
                    .append("subproject", subProject)
                    .append("hints", Arrays.asList(hintData));
            col.insertOne(newDoc);
        }
    }
}
