package org.example;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AiUnifiedSearchController {

    @Autowired
    private MongoTemplate mongo;

    // List of collections to search
    private static final List<String> COLLECTIONS = List.of(
            "LuffyFramework",
            "ai_context_logs",
            "ai_executions",
            "ai_hints",
            "ai_reports"
    );

    @GetMapping("/search")
    public Map<String, Object> searchAiCollections(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return Map.of("error", "Search query cannot be empty");
        }

        query = query.trim();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("timestamp", new Date().toString());

        Map<String, List<Document>> resultsMap = new LinkedHashMap<>();

        for (String collectionName : COLLECTIONS) {
            List<Document> docs = searchCollection(collectionName, query);

            // Convert _id to readable string
            docs.forEach(doc -> {
                if (doc.containsKey("_id")) {
                    Object idObj = doc.get("_id");
                    if (idObj instanceof ObjectId) {
                        doc.put("_id", ((ObjectId) idObj).toHexString());
                    } else if (idObj instanceof Document) {
                        Document idDoc = (Document) idObj;
                        if (idDoc.containsKey("date")) {
                            doc.put("_id", idDoc.getDate("date").toInstant().toString());
                        }
                    }
                }
            });

            resultsMap.put(collectionName, docs);
        }

        result.put("results", resultsMap);
        return result;
    }

    private List<Document> searchCollection(String collectionName, String query) {
        try {
            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

            Criteria criteria = new Criteria().orOperator(
                    Criteria.where("type").regex(pattern),
                    Criteria.where("input").regex(pattern),
                    Criteria.where("result").regex(pattern)
            );

            return mongo.find(new Query(criteria), Document.class, collectionName);
        } catch (Exception e) {
            System.err.println("Error searching " + collectionName + ": " + e.getMessage());
            return List.of();
        }
    }
}
