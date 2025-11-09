package org;

import AI.AiReporter;
import AI.EnvConfig;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 🍽️ Full Stack Food Finder API Automation (AI-Enhanced)
 * -------------------------------------------------------
 * ✅ AI Payload Generation (Register/Login)
 * ✅ MongoDB Execution Logging
 * ✅ Gemini AI Hints + Auto Summary
 * ✅ Allure Reporting with Endpoint Status Overview
 * ✅ Uses .env Configuration for Base URI + Endpoints
 * -------------------------------------------------------
 */
@Epic("AI Automation Framework – Food Finder APIs")
@Feature("Functional API Verification & AI Summary Reporting")
public class UserAppTest {

    private Map<String, String> headers;
    private Response res;
    private String registeredUser = "test_user";
    private String authToken;

    // ========================================================
    // 🧩 SETUP
    // ========================================================

    @BeforeClass
    @Description("Setup Base URI and Default Headers")
    public void setup() {
        System.setProperty("projectName", "UserAppTest");
        String baseUri = EnvConfig.get("USER_APP_BASE_URI");
        ApiReuse.uri(baseUri);

        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        System.out.println("✅ UserAppTest initialized with base URI: " + baseUri);
    }


    // ========================================================
    // 🔐 AUTH CONTROLLER
    // ========================================================

    @Test(priority = 1, description = "POST - Login with valid credentials")
    @Severity(SeverityLevel.BLOCKER)
    public void loginUser() {
        String loginEndpoint = EnvConfig.get("USER_LOGIN_ENDPOINT");

        String payload = String.format("""
            {
                "username": "%s",
                "password": "%s"
            }
        """, registeredUser, EnvConfig.get("DEFAULT_PASSWORD"));

        Allure.step("📤 Request Payload: " + payload);
        ApiReuse api = new ApiReuse(loginEndpoint, "POST", payload);
        res = ApiReuse.execute(api, headers);

        int code = res.getStatusCode();
        String body = res.getBody().asPrettyString();
        Allure.addAttachment("Response - Login User", "application/json", body, ".json");

        Assert.assertEquals(code, 200, "❌ Login failed");
        authToken = res.jsonPath().getString("token");
        headers.put("Authorization", "Bearer " + authToken);
        AiReporter.addRecord("🔑 Logged in successfully | Token stored for reuse");
    }

    // ========================================================
    // 🍳 RECIPE CONTROLLER
    // ========================================================

    @Test(description = "GET - Fetch Non-Veg Recipes")
    public void getNonVegRecipes() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_GET_RECIPES_ENDPOINT") + "?category=Non-Veg", "GET", null));
    }

    @Test(description = "GET - Fetch Veg Recipes")
    public void getVegRecipes() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_GET_RECIPES_ENDPOINT") + "?category=Veg", "GET", null));
    }

    @Test(description = "GET - Fetch Recipe Detail by ID (2)")
    public void getRecipeDetail2() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_GET_RECIPE_DETAIL_ENDPOINT") + "?id=2", "GET", null));
    }

    @Test(description = "GET - Fetch Recipe by Name 'Chicken Biryani'")
    public void getRecipeByName() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_GET_RECIPE_BY_NAME_ENDPOINT") + "/Chicken%20Biryani", "GET", null));
    }

    // ========================================================
    // 🔍 SUGGESTIONS CONTROLLER
    // ========================================================

    @Test(description = "GET - Suggestions for query 'chi'")
    public void getSuggestionChi() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_GET_SUGGESTIONS_ENDPOINT") + "?query=chi", "GET", null));
    }

    @Test(description = "GET - Suggestions for query 'veg recipe'")
    public void getSuggestionVegRecipe() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_GET_SUGGESTIONS_ENDPOINT") + "?query=veg recipe", "GET", null));
    }

    @Test(description = "GET - Suggestions for query 'fish'")
    public void getSuggestionFish() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_GET_SUGGESTIONS_ENDPOINT") + "?query=fish", "GET", null));
    }

    // ========================================================
    // 🤖 AI & CHATBOT CONTROLLER
    // ========================================================

    @Test(description = "POST - Chatbot API (Prompt: hi)")
    public void chatbotAPI() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_CHATBOT_API_ENDPOINT"), "POST", "{\"prompt\":\"hi\"}"));
    }

    @Test(description = "POST - AI Search (Query: egg)")
    public void aiSearchAPI() {
        executeAndVerify(new ApiReuse(EnvConfig.get("FOOD_AI_SEARCH_ENDPOINT"), "POST", "{\"query\":\"egg\"}"));
    }

    // ========================================================
    // 📊 ENDPOINT HEALTH CHECK
    // ========================================================

    @Test(priority = 98, description = "📡 Check All Endpoint Status Codes")
    @Story("Allure Endpoint Status Overview")
    @Severity(SeverityLevel.NORMAL)
    public void checkAllEndpoints() {
        String base = EnvConfig.get("FOOD_FINDER_BASE_URI");

        String[][] endpoints = {
                {"GET", EnvConfig.get("FOOD_GET_RECIPES_ENDPOINT") + "?category=Veg"},
                {"GET", EnvConfig.get("FOOD_GET_RECIPES_ENDPOINT") + "?category=Non-Veg"},
                {"GET", EnvConfig.get("FOOD_GET_RECIPE_DETAIL_ENDPOINT") + "?id=1"},
                {"GET", EnvConfig.get("FOOD_GET_RECIPE_BY_NAME_ENDPOINT") + "/Paneer%20Butter%20Masala"},
                {"GET", EnvConfig.get("FOOD_GET_SUGGESTIONS_ENDPOINT") + "?query=chi"},
                {"GET", EnvConfig.get("FOOD_GET_SUGGESTIONS_ENDPOINT") + "?query=veg"},
                {"POST", EnvConfig.get("FOOD_CHATBOT_API_ENDPOINT")},
                {"POST", EnvConfig.get("FOOD_AI_SEARCH_ENDPOINT")}
        };

        System.out.println("\n📡 Checking All Endpoint Statuses...");
        for (String[] e : endpoints) {
            String method = e[0];
            String path = e[1];
            String fullUrl = base + path;

            try {
                Response r = io.restassured.RestAssured.given().headers(headers).request(method, fullUrl);
                int status = r.statusCode();
                Allure.step(method + " " + path + " → " + status);
                System.out.println(method + " " + path + " → " + status);
                AiReporter.addRecord("Endpoint: " + method + " " + path + " | Status: " + status);
            } catch (Exception ex) {
                Allure.step("⚠️ Failed to connect to " + path + " (" + ex.getMessage() + ")");
                AiReporter.addRecord("❌ Error: " + method + " " + path + " | " + ex.getMessage());
                System.out.println("❌ " + method + " " + path + " failed: " + ex.getMessage());
            }
        }
        System.out.println("✅ Endpoint health check completed.\n");
    }

    // ========================================================
    // 🧠 AI SUMMARY REPORT
    // ========================================================

    @Test(priority = 99, description = "🧠 Generate AI Summary Report")
    @Story("AI Summary + Insights")
    @Severity(SeverityLevel.MINOR)
    public void generateAiSummaryReport() {
        Allure.step("🧠 Generating AI Summary Report...");
        String aiSummary = AiReporter.generateAndSaveSummary();

        Allure.addAttachment("AI Summary Report", aiSummary);
        System.out.println("\n================ AI SUMMARY REPORT ================\n");
        System.out.println(aiSummary);
        System.out.println("===================================================\n");

        AiReporter.clear();
        Allure.step("🧹 Cleared AI records after summary generation.");
    }

    // ========================================================
    // ⚙️ CORE EXECUTOR (Reusable)
    // ========================================================

    @Step("Execute {api.method} request for endpoint: {api.endpoint}")
    public void executeAndVerify(ApiReuse api) {
        res = ApiReuse.execute(api, headers);
        int statusCode = res.getStatusCode();
        String body = res.getBody().asPrettyString();

        Allure.step("➡️ Method: " + api.getMethod());
        Allure.step("📍 Endpoint: " + api.getEndpoint());
        Allure.step("🔢 Status Code: " + statusCode);
        Allure.addAttachment("Response for " + api.getEndpoint(), "application/json", body, ".json");

        System.out.println("\n➡️ " + api.getMethod() + " " + api.getEndpoint());
        System.out.println("Status Code: " + statusCode);
        System.out.println("Response: " + body);

        AiReporter.addRecord(api.getMethod() + " " + api.getEndpoint() + " | Status: " + statusCode);
        Assert.assertTrue(statusCode == 200 || statusCode == 201,
                "❌ Expected 200/201 but got " + statusCode + " for " + api.getEndpoint());
    }
}
