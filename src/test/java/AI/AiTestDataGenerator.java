package AI;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advanced AI Test Data Generator.
 * Generates realistic JSON payloads using GeminiAI with error handling and validation.
 *
 * Example:
 *   String json = AiTestDataGenerator.generatePayload("job payload with company, role, appliedDate, status");
 */
public class AiTestDataGenerator {

    private static final Logger LOGGER = Logger.getLogger(AiTestDataGenerator.class.getName());
    private static final Gson GSON = new Gson();

    public static String generatePayload(String requirement) {
        return generatePayload(requirement, null);
    }

    /**
     * @param requirement A short natural language description of required data.
     * @param schema Optional JSON schema or structure hint (can be null).
     * @return A validated JSON payload string.
     */
    public static String generatePayload(String requirement, String schema) {
        String enhancedPrompt = buildPrompt(requirement, schema);
        String result = tryGenerate(enhancedPrompt, 2); // retry up to 2 times if Gemini fails

        if (isValidJson(result)) {
            return prettyPrintJson(result);
        } else {
            LOGGER.warning("Invalid JSON from Gemini. Returning fallback object.");
            return "{}";
        }
    }

    /** Builds a detailed and structured AI prompt. */
    private static String buildPrompt(String requirement, String schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a single realistic JSON object based on the requirement below.\n")
                .append("Requirement: ").append(requirement).append("\n")
                .append("Include realistic random values (e.g., names, dates, IDs, status fields, etc.).\n")
                .append("Return ONLY valid JSON with no explanation.\n");

        if (schema != null && !schema.isBlank()) {
            sb.append("Schema hint:\n").append(schema).append("\n");
        }

        sb.append("Example: {\"id\": 1, \"name\": \"John Doe\", \"role\": \"Developer\"}");
        return sb.toString();
    }

    /** Tries generating a valid payload with retries. */
    private static String tryGenerate(String prompt, int retries) {
        String result = null;

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                result = GeminiAI.generate(prompt);
                if (isValidJson(result)) return result;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Attempt " + attempt + " failed: " + e.getMessage());
            }

            // wait briefly before retrying
            try {
                Thread.sleep(1000L * attempt);
            } catch (InterruptedException ignored) {}
        }

        return result == null ? "{}" : result;
    }

    /** Checks if the text is valid JSON. */
    private static boolean isValidJson(String text) {
        if (text == null || text.isBlank()) return false;
        try {
            JsonParser.parseString(cleanJson(text));
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /** Removes non-JSON text or explanations before/after JSON braces. */
    private static String cleanJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }

    /** Formats JSON for readability. */
    private static String prettyPrintJson(String json) {
        try {
            JsonElement element = JsonParser.parseString(cleanJson(json));
            return GSON.toJson(element);
        } catch (Exception e) {
            return cleanJson(json);
        }
    }
}
