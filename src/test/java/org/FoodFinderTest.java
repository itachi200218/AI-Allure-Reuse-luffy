package org;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import AI.AiReporter;
import AI.EnvConfig;

/**
 * 🤖 Full Stack Food Finder Admin System (AI-Enhanced)
 * -----------------------------------------------------
 * Features:
 *  - AI Payload Generation
 *  - Auto Token Handling
 *  - MongoDB Self-Learning
 *  - Allure Reporting
 *  - AI Summary Report at End
 */
@Epic("Full Stack Food Finder Admin Management System")
@Feature("AI-Powered API Automation with Allure + MongoDB")
public class FoodFinderTest {

    private Response res;
    private static Map<String, String> headers;
    private static String authToken;
    private static String registeredUser;
    private static String registeredEmail;

    @BeforeClass
    @Description("Setup Base URI and Default Headers")
    public void setup() {
        System.setProperty("projectName", "FoodFinderTest");
        String baseUri = EnvConfig.get("BASE_URL");
        ApiReuse.uri(baseUri);

        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        System.out.println("✅ FoodFinderTest initialized with base URI: " + baseUri);
    }

    // -------------------- AUTH CONTROLLER --------------------

    @Test(priority = 1, description = "POST - Register new user (AI Payload)")
    @Severity(SeverityLevel.CRITICAL)
    public void registerUser() {
        String username = "admin_" + System.currentTimeMillis();
        String email = username + "@foodfinder.com";
        registeredUser = username;
        registeredEmail = email;

        String payload = String.format("""
            {
                "username":"%s",
                "email":"%s",
                "password":"%s"
            }
        """, username, email, EnvConfig.get("DEFAULT_PASSWORD"));

        Allure.step("📤 Request Payload: " + payload);

        ApiReuse api = new ApiReuse(EnvConfig.get("REGISTER_ENDPOINT"), "POST", payload);
        res = ApiReuse.execute(api, headers);

        int code = res.getStatusCode();
        String body = res.getBody().asPrettyString();

        Allure.addAttachment("Response - Register User", "application/json", body, ".json");
        AiReporter.addRecord("🧾 Register User | Code: " + code + " | Username: " + username);

        Assert.assertTrue(code == 200 || code == 201, "❌ Registration failed");
    }

    @Test(priority = 2, description = "POST - Login with valid credentials")
    @Severity(SeverityLevel.BLOCKER)
    public void loginUser() {
        String payload = String.format("""
            {
                "username":"%s",
                "password":"%s"
            }
        """, registeredUser, EnvConfig.get("DEFAULT_PASSWORD"));

        Allure.step("📤 Request Payload: " + payload);

        ApiReuse api = new ApiReuse(EnvConfig.get("LOGIN_ENDPOINT"), "POST", payload);
        res = ApiReuse.execute(api, headers);
        int code = res.getStatusCode();

        String body = res.getBody().asPrettyString();
        Allure.addAttachment("Response - Login User", "application/json", body, ".json");

        Assert.assertEquals(code, 200, "❌ Login failed");
        authToken = res.jsonPath().getString("token");
        headers.put("Authorization", "Bearer " + authToken);
        AiReporter.addRecord("🔑 Logged in successfully | Token stored for reuse");
    }

    @Test(priority = 3, description = "GET - Check Login Status")
    public void checkLogin() {
        executeAndVerify(new ApiReuse(EnvConfig.get("CHECK_ENDPOINT"), "GET", null));
    }

    @Test(priority = 4, description = "GET - Logout User")
    public void logoutUser() {
        executeAndVerify(new ApiReuse(EnvConfig.get("LOGOUT_ENDPOINT"), "GET", null));
    }

    @Test(priority = 5, description = "GET - Fetch All Users (Auth Controller)")
    public void getAllUsersAuth() {
        executeAndVerify(new ApiReuse(EnvConfig.get("ALL_USERS_AUTH_ENDPOINT"), "GET", null));
    }

    @Test(priority = 6, description = "POST - Forgot Username by Email (Fixed Payload)")
    @Severity(SeverityLevel.CRITICAL)
    public void forgotUsername() {
        String payload = String.format("""
        {
            "email": "%s"
        }
    """, registeredEmail);

        Allure.step("📤 Request Payload: " + payload);

        ApiReuse api = new ApiReuse(EnvConfig.get("FORGOT_USERNAME_ENDPOINT"), "POST", payload);
        res = ApiReuse.execute(api, headers);

        int code = res.getStatusCode();
        String body = res.getBody().asPrettyString();

        Allure.addAttachment("Response - Forgot Username", "application/json", body, ".json");
        System.out.println("📧 Forgot Username Response: " + body);
        AiReporter.addRecord("📧 Forgot Username | Code: " + code + " | Email: " + registeredEmail);

        Assert.assertEquals(code, 200, "❌ Forgot Username failed (expected 200).");
    }

    @Test(priority = 7, description = "POST - Reset Password (Fixed Payload)")
    @Severity(SeverityLevel.CRITICAL)
    public void resetPassword() {
        String payload = String.format("""
        {
            "username": "%s",
            "currentPassword": "%s",
            "newPassword": "%s"
        }
    """, registeredUser, EnvConfig.get("DEFAULT_PASSWORD"), EnvConfig.get("NEW_PASSWORD"));

        Allure.step("📤 Request Payload: " + payload);

        ApiReuse api = new ApiReuse(EnvConfig.get("RESET_PASSWORD_ENDPOINT"), "POST", payload);
        res = ApiReuse.execute(api, headers);

        int code = res.getStatusCode();
        String body = res.getBody().asPrettyString();

        Allure.addAttachment("Response - Reset Password", "application/json", body, ".json");
        System.out.println("🔒 Reset Password Response: " + body);

        AiReporter.addRecord("🔒 Reset Password | Code: " + code + " | Username: " + registeredUser);
        Assert.assertEquals(code, 200, "❌ Reset Password failed (expected 200).");
    }

    @Test(priority = 9, description = "GET - Get Profile by Username")
    public void getProfile() {
        executeAndVerify(new ApiReuse(EnvConfig.get("PROFILE_ENDPOINT") + registeredUser, "GET", null));
    }

    // -------------------- ADMIN CONTROLLER --------------------

    @Test(priority = 10, description = "GET - Get All Users (AdminController)")
    public void getAllUsersFromAdminController() {
        executeAndVerify(new ApiReuse(EnvConfig.get("ALL_USERS_ADMIN_ENDPOINT"), "GET", null));
    }

    // -------------------- DASHBOARD CONTROLLER --------------------

    @Test(priority = 11, description = "GET - Dashboard Statistics")
    public void getDashboardStats() {
        executeAndVerify(new ApiReuse(EnvConfig.get("DASHBOARD_STATS_ENDPOINT"), "GET", null));
    }

    // -------------------- GEMINI CONTROLLER --------------------

    @Test(priority = 15, description = "GET - Get All Gemini Users")
    public void getAllGeminiUsers() {
        executeAndVerify(new ApiReuse(EnvConfig.get("GEMINI_USERS_ENDPOINT"), "GET", null));
    }

    // -------------------- USER CONTROLLER --------------------

    @Test(priority = 16, description = "GET - Get All Users (UserController)")
    public void getAllUsersFromUserController() {
        executeAndVerify(new ApiReuse(EnvConfig.get("ALL_USERS_ADMIN_ENDPOINT"), "GET", null));
    }

    @Test(priority = 17, description = "GET - Get User by ID")
    public void getUserById() {
        executeAndVerify(new ApiReuse(EnvConfig.get("USER_BY_ID_ENDPOINT"), "GET", null));
    }

    @Test(priority = 18, description = "DELETE - Delete User by ID")
    public void deleteUser() {
        executeAndVerify(new ApiReuse(EnvConfig.get("DELETE_USER_ENDPOINT"), "DELETE", null));
    }

    // -------------------- STATUS SUMMARY TEST --------------------

    @Test(priority = 98, description = "📊 Check All Endpoint Status Codes")
    @Story("Allure Endpoint Status Overview")
    @Severity(SeverityLevel.NORMAL)
    public void checkAllEndpoints() {
        String[][] endpoints = {
                {"POST", EnvConfig.get("REGISTER_ENDPOINT")},
                {"POST", EnvConfig.get("LOGIN_ENDPOINT")},
                {"GET", EnvConfig.get("CHECK_ENDPOINT")},
                {"GET", EnvConfig.get("LOGOUT_ENDPOINT")},
                {"GET", EnvConfig.get("ALL_USERS_AUTH_ENDPOINT")},
                {"POST", EnvConfig.get("FORGOT_USERNAME_ENDPOINT")},
                {"POST", EnvConfig.get("RESET_PASSWORD_ENDPOINT")},
                {"GET", EnvConfig.get("PROFILE_ENDPOINT") + registeredUser},
                {"GET", EnvConfig.get("ALL_USERS_ADMIN_ENDPOINT")},
                {"GET", EnvConfig.get("DASHBOARD_STATS_ENDPOINT")},
                {"GET", EnvConfig.get("GEMINI_USERS_ENDPOINT")},
                {"GET", EnvConfig.get("USER_BY_ID_ENDPOINT")},
                {"DELETE", EnvConfig.get("DELETE_USER_ENDPOINT")}
        };

        for (String[] e : endpoints) {
            String method = e[0];
            String path = e[1];
            Response r = io.restassured.RestAssured.given().headers(headers).request(method, EnvConfig.get("BASE_URL") + path);
            int status = r.statusCode();
            Allure.step(method + " " + path + " → " + status);
            System.out.println(method + " " + path + " → " + status);
        }
    }

    // -------------------- AI SUMMARY --------------------

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

    @Step("Execute {api.method} request for endpoint: {api.endpoint}")
    public void executeAndVerify(ApiReuse api) {
        res = ApiReuse.execute(api, headers);
        int statusCode = res.getStatusCode();
        String body = res.getBody().asPrettyString();

        Allure.step("➡️ Method: " + api.getMethod());
        Allure.step("📍 Endpoint: " + api.getEndpoint());
        Allure.step("🔢 Status Code: " + statusCode);
        Allure.addAttachment("Response for " + api.getEndpoint(), "application/json", body, ".json");

        System.out.println("➡️ " + api.getMethod() + " " + api.getEndpoint());
        System.out.println("Status Code: " + statusCode);
        System.out.println("Response: " + body);

        AiReporter.addRecord(api.getMethod() + " " + api.getEndpoint() + " | Status: " + statusCode);
        Assert.assertTrue(statusCode == 200 || statusCode == 201 || statusCode == 204,
                "❌ Expected 200/201/204 but got " + statusCode + " for " + api.getEndpoint());
    }
}
