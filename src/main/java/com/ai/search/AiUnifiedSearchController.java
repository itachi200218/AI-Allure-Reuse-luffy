//package com.ai.search;
//
//import org.bson.Document;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.*;
//
//@RestController
//@RequestMapping("/api")
//@CrossOrigin(origins = "*")
//public class AiUnifiedSearchController {
//
//    @Autowired
//    private MongoTemplate mongo;
//
//    @GetMapping("/search")
//    public Map<String, Object> unifiedSearch(@RequestParam("q") String query) {
//
//        if (query == null || query.trim().isEmpty()) {
//            return Map.of("error", "Search query cannot be empty");
//        }
//
//        Map<String, Object> result = new LinkedHashMap<>();
//
//        result.put("query", query);
//        result.put("timestamp", new Date().toString());
//        result.put("results", Map.of(
//                "ai_context_logs", searchInCollection("ai_context_logs", query),
//                "ai_executions", searchInCollection("ai_executions", query),
//                "ai_hints", searchInCollection("ai_hints", query),
//                "ai_reports", searchInCollection("ai_reports", query)
//        ));
//
//        return result;
//    }
//
//    private List<Document> searchInCollection(String collection, String query) {
//        List<Document> found = new ArrayList<>();
//
//        try {
//            Criteria criteria = new Criteria().orOperator(
//                    Criteria.where("project").regex(query, "i"),
//                    Criteria.where("subproject").regex(query, "i"),
//                    Criteria.where("endpoints.endpoint").regex(query, "i"),
//                    Criteria.where("endpoints.status").is(parseIntIfNumber(query)),
//                    Criteria.where("summary").regex(query, "i"),
//                    Criteria.where("error").regex(query, "i"),
//                    Criteria.where("details").regex(query, "i"),
//                    Criteria.where("message").regex(query, "i")
//            );
//
//            Query mongoQuery = new Query(criteria);
//            found = mongo.find(mongoQuery, Document.class, collection);
//
//        } catch (Exception e) {
//            System.err.println("Error searching in " + collection + ": " + e.getMessage());
//        }
//
//        return found;
//    }
//
//    private Object parseIntIfNumber(String value) {
//        try {
//            return Integer.parseInt(value);
//        } catch (NumberFormatException e) {
//            return null;
//        }
//    }
//}
