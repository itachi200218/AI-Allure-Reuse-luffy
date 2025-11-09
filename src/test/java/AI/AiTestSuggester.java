package AI;

import java.util.List;

/**
 * 🤖 AI-powered Test Case Suggester (Multi-Project Version)
 * ------------------------------------------------------------
 * - Uses past summaries from MongoDB for contextual learning
 * - Generates intelligent new test cases per project
 * - Stores responses under each project's history
 */
public class AiTestSuggester {

    /**
     * Suggest intelligent new or missing test cases for a given project.
     *
     * @param projectName The project this suggestion belongs to.
     * @param endpoints   List of API endpoints to analyze.
     * @return AI-generated suggestions for new test cases.
     */
    public static String suggestTests(String projectName, List<String> endpoints) {
        // 🧠 Learn from this project's past context
        String context = AiSelfLearner.buildPromptContext(projectName);

        StringBuilder sb = new StringBuilder();
        sb.append("💡 Project: ").append(projectName).append("\n\n")
                .append(context).append("\n\n")
                .append("Given these API endpoints, suggest new or missing test cases ")
                .append("(including negative, boundary, authentication, rate-limit, invalid data, and performance cases).\n\n");

        for (String e : endpoints) {
            sb.append("- ").append(e).append("\n");
        }

        sb.append("\nFormat response as:\n")
                .append("[Endpoint] - [Test Case] - [Why it matters].\n")
                .append("Focus on unique and practical scenarios not already covered in previous runs.");

        // 💬 Generate AI suggestions via Gemini
        String result = GeminiAI.generate(sb.toString());

        // 💾 Save learning context for this specific project
        AiMemorySaver.saveLearning(projectName, sb.toString(), result);

        System.out.println("✅ [" + projectName + "] AI test suggestions generated and saved.");
        return result;
    }
}
