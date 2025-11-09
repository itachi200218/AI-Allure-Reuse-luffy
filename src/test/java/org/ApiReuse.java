package org;

import AI.AiAutoContext;
import AI.AiReporter;
import AI.GeminiAI;
import models.AiMongoLogger;
import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * 🤖 AI + MongoDB Enhanced API Executor
 * -------------------------------------------------
 * Features:
 *  - Smart HTTP method inference
 *  - AI-generated payloads (only when needed)
 *  - Auto token injection from Mongo context
 *  - Failure hinting via GeminiAI
 *  - MongoDB logging for each execution
 * -------------------------------------------------
 */
public class ApiReuse {

    private final String endpoint;
    private final String method;
    private final String payload;

    public ApiReuse(String endpoint, String method, String payload) {
        this.endpoint = endpoint;
        this.method = method;
        this.payload = payload;
    }

    public String getEndpoint() { return endpoint; }
    public String getMethod()   { return method;   }
    public String getPayload()  { return payload;  }

    // ---------- BASE URI ----------
    public static void uri(String baseUri) {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.baseURI = baseUri;
    }

    // ---------- MAIN EXECUTION ----------
    public static Response execute(ApiReuse api, Map<String, String> headers) {

        // 🚨 Validate project name before running any test
        String projectName = System.getProperty("projectName");
        if (projectName == null || projectName.isBlank()) {
            throw new RuntimeException("""
                ❌ Missing required project name.
                Please set it in your @BeforeClass method like this:

                    System.setProperty("projectName", "<YourProjectName>");

                Example:
                    System.setProperty("projectName", "FoodFinderTest");
                """);
        }

        // 🧠 Smart HTTP method selection
        String method = (api.getMethod() == null || api.getMethod().isEmpty())
                ? inferHttpMethod(api.getEndpoint())
                : api.getMethod().toUpperCase();

        // 🧠 Auto token injection (skip for login/register)
        String finalEndpoint = AiAutoContext.inferEndpoint(api.getEndpoint());
        if (!finalEndpoint.contains("/login") && !finalEndpoint.contains("/register")) {
            String token = AiAutoContext.getToken();
            if (token != null && !"no-token".equalsIgnoreCase(token)) {
                headers.put("Authorization", "Bearer " + token);
            }
        }

        // 🧠 Infer endpoint and generate payload if needed
        String finalPayload;
        if (api.getPayload() == null || api.getPayload().isEmpty()) {
            if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("DELETE")) {
                finalPayload = null;
            } else {
                finalPayload = AiAutoContext.smartPayload(method, finalEndpoint);
            }
        } else {
            finalPayload = api.getPayload();
        }

        // ---------- Build & Send ----------
        RequestSpecification req = RestAssured.given().headers(headers);

        // ✅ Only send body for non-GET/DELETE
        if (finalPayload != null && !finalPayload.isEmpty()
                && !method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("DELETE")) {
            req.body(finalPayload);
        }

        Response res;
        try {
            res = req.request(Method.valueOf(method), finalEndpoint)
                    .then()
                    .extract()
                    .response();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("❌ Invalid HTTP method '" + method +
                    "' for endpoint " + finalEndpoint, e);
        }

        int status = res.getStatusCode();
        String body = sanitize(res.asString());

        // ---------- Logging ----------
        AiReporter.addRecord(method + " " + finalEndpoint + " → " + status);
        AiMongoLogger.logExecution(method, finalEndpoint, finalPayload, body, status);

        // 🔑 Auto-save token after login response
        if (finalEndpoint.contains("/login") && body.contains("token")) {
            String extracted = body.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
            AiAutoContext.setToken(extracted);   // store token for next requests
            System.out.println("🔑 Token captured and stored in MongoDB.");
        }

        // ⚠️ Ask Gemini for hint on failure
        if (status >= 400) {
            String prompt = """
                Provide a one-line cause and fix for this failed API request:
                Method: %s
                Endpoint: %s
                Request: %s
                Status: %d
                Response: %s
                """.formatted(method, finalEndpoint, sanitize(finalPayload), status, body);

            String hint = GeminiAI.generate(prompt);
            AiReporter.addRecord("AI_HINT: " + hint);
            AiMongoLogger.logAIHint(method, finalEndpoint, hint);
        }

        return res;
    }

    // ---------- Infer HTTP method ----------
    private static String inferHttpMethod(String endpoint) {
        if (endpoint == null) return "GET";
        String s = endpoint.toLowerCase();
        if (s.contains("create") || s.contains("add") || s.contains("register")) return "POST";
        if (s.contains("update") || s.contains("edit") || s.contains("change"))  return "PUT";
        if (s.contains("delete") || s.contains("remove"))                       return "DELETE";
        return "GET";
    }

    // ---------- Sanitize output ----------
    private static String sanitize(String input) {
        if (input == null) return "";
        String out = input.replaceAll("\\r?\\n", " ");
        out = out.replaceAll("(?i)(\"?token\"?\\s*:\\s*\")[^\"]+\"", "\"token\":\"***\"");
        out = out.replaceAll("(?i)(\"?password\"?\\s*:\\s*\")[^\"]+\"", "\"password\":\"***\"");
        if (out.length() > 2000) out = out.substring(0, 2000) + "...(truncated)";
        return out;
    }
}
