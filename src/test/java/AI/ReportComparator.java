//package models;
//
//import com.mongodb.client.FindIterable;
//import com.mongodb.client.MongoCollection;
//import com.mongodb.client.MongoCursor;
//import com.mongodb.client.MongoDatabase;
//import org.bson.Document;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class ReportComparator {
//
//    public static void compareLatestReports() {
//        try {
//            MongoDatabase db = MongoConnector.connect();
//            MongoCollection<Document> collection = db.getCollection("ai_reports");
//
//            // Fetch last two reports sorted by timestamp descending
//            FindIterable<Document> iterable = collection.find()
//                    .sort(new Document("timestamp", -1))
//                    .limit(2);
//
//            List<Document> reports = new ArrayList<>();
//            try (MongoCursor<Document> cursor = iterable.iterator()) {
//                while (cursor.hasNext()) {
//                    reports.add(cursor.next());
//                }
//            }
//
//            if (reports.size() < 2) {
//                System.out.println("⚠️ Not enough reports to compare (need at least 2).");
//                return;
//            }
//
//            Document latest = reports.get(0);
//            Document previous = reports.get(1);
//
//            String latestSummary = latest.getString("aiSummary");
//            String previousSummary = previous.getString("aiSummary");
//
//            String latestRecords = latest.getString("records");
//            String previousRecords = previous.getString("records");
//
//            System.out.println("\n==============================");
//            System.out.println("🧠 AI REPORT COMPARISON STARTED");
//            System.out.println("==============================");
//
//            compareSections("AI Summary", previousSummary, latestSummary);
//            compareSections("Records", previousRecords, latestRecords);
//
//            System.out.println("==============================");
//            System.out.println("✅ Comparison completed successfully.");
//            System.out.println("==============================");
//
//        } catch (Exception e) {
//            System.err.println("❌ Error during report comparison: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private static void compareSections(String sectionName, String oldText, String newText) {
//        System.out.println("\n🔍 Comparing " + sectionName + ":\n");
//
//        if (oldText == null || newText == null) {
//            System.out.println("⚠️ One of the " + sectionName + " sections is empty.");
//            return;
//        }
//
//        List<String> oldLines = Arrays.stream(oldText.split("\n"))
//                .map(String::trim)
//                .filter(line -> !line.isEmpty())
//                .collect(Collectors.toList());
//
//        List<String> newLines = Arrays.stream(newText.split("\n"))
//                .map(String::trim)
//                .filter(line -> !line.isEmpty())
//                .collect(Collectors.toList());
//
//        Set<String> added = new HashSet<>(newLines);
//        added.removeAll(oldLines);
//
//        Set<String> removed = new HashSet<>(oldLines);
//        removed.removeAll(newLines);
//
//        if (added.isEmpty() && removed.isEmpty()) {
//            System.out.println("✅ No differences found in " + sectionName + ".");
//        } else {
//            if (!added.isEmpty()) {
//                System.out.println("➕ Added lines in " + sectionName + ":");
//                added.forEach(line -> System.out.println("   + " + line));
//            }
//            if (!removed.isEmpty()) {
//                System.out.println("➖ Removed lines in " + sectionName + ":");
//                removed.forEach(line -> System.out.println("   - " + line));
//            }
//        }
//    }
//
//    public static void main(String[] args) {
//        compareLatestReports();
//    }
//}
