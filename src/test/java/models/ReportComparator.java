package models;

import com.mongodb.client.*;
import io.qameta.allure.Allure;
import org.bson.Document;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🤖 AI Project-Level Execution Comparator (Enhanced Edition)
 * --------------------------------------------------------------------
 * - Groups by {project → subproject}
 * - Supports session-based structure (with endpoints inside each session)
 * - Compares last 2 runs per subproject
 * - Shows success % delta, endpoint counts, new/removed endpoints
 * - Adds session-level comparison and global summary
 */
public class ReportComparator {

    public static void compareLatestExecutions() {
        StringBuilder report = new StringBuilder();
        try {
            MongoDatabase db = MongoConnector.connect();
            MongoCollection<Document> collection = db.getCollection("ai_executions");
            List<Document> docs = collection.find().into(new ArrayList<>());
            if (docs.isEmpty()) {
                report.append("⚠️ No execution data found in MongoDB.\n");
                attachToAllure(report);
                return;
            }

            // 🔹 Group by Project → Subproject
            Map<String, Map<String, List<Document>>> grouped =
                    docs.stream().collect(Collectors.groupingBy(
                            d -> d.getString("project") != null ? d.getString("project") : "UnknownProject",
                            Collectors.groupingBy(d -> d.getString("subproject") != null ? d.getString("subproject") : "UnknownSubproject")
                    ));

            report.append("🧠 **AI Project-Wise Comparison Report**\n\n");

            Map<String, Double> projectPassRates = new LinkedHashMap<>();
            Map<String, Integer> projectEndpointTotals = new LinkedHashMap<>();

            for (String project : grouped.keySet()) {
                report.append("🏗️ **Project:** ").append(project).append("\n");
                Map<String, List<Document>> subprojects = grouped.get(project);
                int totalProjectEndpoints = 0;

                for (String sub : subprojects.keySet()) {
                    List<Document> runs = subprojects.get(sub);

                    // 🧩 Handle session-based documents
                    List<Document> flattenedRuns = new ArrayList<>();
                    for (Document d : runs) {
                        List<Document> sessions = (List<Document>) d.get("sessions");
                        if (sessions != null && !sessions.isEmpty()) {
                            for (Document s : sessions) {
                                Document copy = new Document();
                                copy.put("timestamp", s.getString("createdAt"));
                                copy.put("endpoints", s.get("endpoints"));
                                flattenedRuns.add(copy);
                            }
                        } else {
                            flattenedRuns.add(d);
                        }
                    }

                    flattenedRuns.sort(Comparator.comparing(
                            d -> d.getString("timestamp"), Comparator.reverseOrder()));

                    report.append("   🔹 Subproject: ").append(sub).append("\n");

                    if (flattenedRuns.size() < 2) {
                        report.append("      ⚠️ Only one run — no comparison possible.\n\n");
                        continue;
                    }

                    List<Document> latest = flattenedRuns.get(0).getList("endpoints", Document.class, new ArrayList<>());
                    List<Document> previous = flattenedRuns.get(1).getList("endpoints", Document.class, new ArrayList<>());

                    double latestRate = calcSuccessRate(latest);
                    double prevRate = calcSuccessRate(previous);
                    double delta = latestRate - prevRate;

                    int latestCount = latest.size();
                    int prevCount = previous.size();
                    int diffCount = latestCount - prevCount;

                    report.append(String.format("      📊 Success Rate: %.2f%% → %.2f%% (Δ %.2f%%) %s\n",
                            prevRate, latestRate, delta, trendIcon(delta)));

                    report.append(String.format("      🧩 Endpoints Count: Prev=%d | Latest=%d (%s%d)\n",
                            prevCount, latestCount,
                            diffCount > 0 ? "+" : "", diffCount));

                    compareEndpoints(previous, latest, report);

                    totalProjectEndpoints += latestCount;
                    report.append("\n");
                }

                // 📈 Average project pass rate
                double avgProjectRate = grouped.get(project).values().stream()
                        .mapToDouble(list -> {
                            Document latestDoc = list.get(0);
                            List<Document> sessions = (List<Document>) latestDoc.get("sessions");
                            List<Document> endpoints;
                            if (sessions != null && !sessions.isEmpty()) {
                                endpoints = sessions.get(0).getList("endpoints", Document.class, new ArrayList<>());
                            } else {
                                endpoints = latestDoc.getList("endpoints", Document.class, new ArrayList<>());
                            }
                            return calcSuccessRate(endpoints);
                        })
                        .average().orElse(0.0);

                projectPassRates.put(project, avgProjectRate);
                projectEndpointTotals.put(project, totalProjectEndpoints);

                report.append(String.format("   🧮 Total Endpoints in Project '%s': %d\n", project, totalProjectEndpoints));
                report.append("---\n");
            }

            // 🌍 Global Summary
            report.append("\n🌍 **Global Project Comparison Summary**\n");
            projectPassRates.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(e -> {
                        int totalEndpoints = projectEndpointTotals.getOrDefault(e.getKey(), 0);
                        report.append(String.format("🏁 %s — Avg Success Rate: %.2f%% | Endpoints: %d\n",
                                e.getKey(), e.getValue(), totalEndpoints));
                    });

        } catch (Exception e) {
            report.append("❌ Error: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        } finally {
            attachToAllure(report);
        }
    }

    // ✅ Calculates success rate for a combined endpoint list
    private static double calcSuccessRate(List<Document> endpoints) {
        if (endpoints == null || endpoints.isEmpty()) return 0.0;
        long success = endpoints.stream()
                .filter(d -> {
                    int status = d.getInteger("status", 0);
                    return status >= 200 && status < 300;
                }).count();
        return (success * 100.0 / endpoints.size());
    }

    private static void compareEndpoints(List<Document> prev, List<Document> latest, StringBuilder report) {
        Set<String> prevSet = prev.stream()
                .map(d -> d.getString("method") + " " + d.getString("endpoint"))
                .collect(Collectors.toSet());
        Set<String> currSet = latest.stream()
                .map(d -> d.getString("method") + " " + d.getString("endpoint"))
                .collect(Collectors.toSet());

        Set<String> added = new HashSet<>(currSet);
        added.removeAll(prevSet);
        Set<String> removed = new HashSet<>(prevSet);
        removed.removeAll(currSet);

        if (!added.isEmpty())
            report.append("      ➕ Added Endpoints (").append(added.size()).append("): ").append(added).append("\n");
        if (!removed.isEmpty())
            report.append("      ➖ Removed Endpoints (").append(removed.size()).append("): ").append(removed).append("\n");

        Set<String> newFails = findNewFailures(prev, latest);
        if (!newFails.isEmpty())
            report.append("      ❌ New Failing Endpoints: ").append(newFails).append("\n");
        else
            report.append("      ✅ No new failures.\n");
    }

    private static Set<String> findNewFailures(List<Document> prev, List<Document> latest) {
        Set<String> prevFails = prev.stream()
                .filter(d -> d.getInteger("status", 0) >= 400)
                .map(d -> d.getString("method") + " " + d.getString("endpoint"))
                .collect(Collectors.toSet());
        Set<String> currFails = latest.stream()
                .filter(d -> d.getInteger("status", 0) >= 400)
                .map(d -> d.getString("method") + " " + d.getString("endpoint"))
                .collect(Collectors.toSet());
        currFails.removeAll(prevFails);
        return currFails;
    }

    private static String trendIcon(double delta) {
        if (delta > 0) return "⬆️";
        if (delta < 0) return "⬇️";
        return "➡️";
    }

    private static void attachToAllure(StringBuilder report) {
        Allure.addAttachment("AI Project Comparison Report",
                new ByteArrayInputStream(report.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println(report);
    }

    public static void main(String[] args) {
        compareLatestExecutions();
    }
}
