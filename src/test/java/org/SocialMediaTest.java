package org;

import AI.AiAutoContext;
import AI.AiReporter;
import AI.AiTestDataGenerator;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Epic("AI + API Automation Suite")
@Feature("Social Media API Testing with Gemini AI + Allure Report")
public class SocialMediaTest {

    private static Map<String, String> headers;
    private static String BASE_URI;
    private static String REGISTER_ENDPOINT, LOGIN_ENDPOINT, FEED_GET_ENDPOINT, POST_CREATE_ENDPOINT, POST_DELETE_ENDPOINT;
    private static String PROFILE_ENDPOINT, CHATBOT_ENDPOINT;
    private static String MESSAGE_SEND_ENDPOINT, MESSAGE_HISTORY_ENDPOINT, MESSAGE_DELETE_ALL_ENDPOINT, MESSAGE_DELETE_SPECIFIC_ENDPOINT;

    private static String token = null;
    private static String registeredUser = null;
    private static final String registeredPassword = "Password";
    private static String createdPostId = null;
    private static String messageReceiverId = null;
    private static String sentMessageId = null; // capture actual message ID

    @BeforeClass
    @Step("Initialize API Base URI and common headers from Social-media.env")
    public void setup() {
        System.setProperty("projectName", "SocialMediaTest");

        String rootPath = new File(System.getProperty("user.dir")).getAbsolutePath();
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .filename("Social-media.env")
                .directory(rootPath + File.separator + "ENV")
                .ignoreIfMissing()
                .load();

        BASE_URI = dotenv.get("BASE_URI");
        REGISTER_ENDPOINT = dotenv.get("REGISTER_ENDPOINT");
        LOGIN_ENDPOINT = dotenv.get("LOGIN_ENDPOINT");
        FEED_GET_ENDPOINT = dotenv.get("FEED_GET_ENDPOINT");
        POST_CREATE_ENDPOINT = dotenv.get("POST_CREATE_ENDPOINT");
        POST_DELETE_ENDPOINT = dotenv.get("POST_DELETE_ENDPOINT");
        PROFILE_ENDPOINT = dotenv.get("PROFILE_ENDPOINT");
        CHATBOT_ENDPOINT = dotenv.get("CHATBOT_ENDPOINT");
        MESSAGE_SEND_ENDPOINT = dotenv.get("MESSAGE_SEND_ENDPOINT");
        MESSAGE_HISTORY_ENDPOINT = dotenv.get("MESSAGE_HISTORY_ENDPOINT");
        MESSAGE_DELETE_ALL_ENDPOINT = dotenv.get("MESSAGE_DELETE_ALL_ENDPOINT");
        MESSAGE_DELETE_SPECIFIC_ENDPOINT = dotenv.get("MESSAGE_DELETE_SPECIFIC_ENDPOINT");

        // ✅ Load receiver ID from .env
        messageReceiverId = dotenv.get("RECEIVER_ID");

        ApiReuse.uri(BASE_URI);

        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
    }

    // ---------------- REGISTER ----------------
    @Test(priority = 1)
    public void testRegister() {
        registeredUser = "user_" + System.currentTimeMillis();
        String payload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", registeredUser, registeredPassword);
        Response res = ApiReuse.execute(new ApiReuse(REGISTER_ENDPOINT, "POST", payload), headers);
        AiReporter.addRecord("POST " + REGISTER_ENDPOINT + " → " + res.getStatusCode());
    }

    // ---------------- LOGIN ----------------
    @Test(priority = 2, dependsOnMethods = {"testRegister"})
    public void testLogin() {
        String payload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", registeredUser, registeredPassword);
        Response res = ApiReuse.execute(new ApiReuse(LOGIN_ENDPOINT, "POST", payload), headers);
        token = res.jsonPath().getString("token");
        headers.put("Authorization", "Bearer " + token);
        AiAutoContext.setToken(token);
        AiReporter.addRecord("POST " + LOGIN_ENDPOINT + " → " + res.getStatusCode());
    }

    // ---------------- FEED ----------------
    @Test(priority = 3, dependsOnMethods = {"testLogin"})
    public void testFeed() {
        Response res = ApiReuse.execute(new ApiReuse(FEED_GET_ENDPOINT, "GET", null), headers);
        AiReporter.addRecord("GET " + FEED_GET_ENDPOINT + " → " + res.getStatusCode());
    }

    // ---------------- CREATE POST ----------------
    @Test(priority = 4, dependsOnMethods = {"testLogin"})
    public void testCreatePost() {
        // ✅ Ensure payload has 'content' to avoid 400 error
        String payload = "{\"content\":\"Hello from API Test!\",\"tags\":[\"test\"]}";
        Response res = ApiReuse.execute(new ApiReuse(POST_CREATE_ENDPOINT, "POST", payload), headers);
        createdPostId = res.jsonPath().getString("_id"); // Capture created post ID for deletion
        AiReporter.addRecord("POST " + POST_CREATE_ENDPOINT + " → " + res.getStatusCode());
    }

    // ---------------- DELETE POST ----------------
    @Test(priority = 5, dependsOnMethods = {"testCreatePost"})
    public void testDeletePost() {
        if (createdPostId != null) {
            String endpoint = POST_DELETE_ENDPOINT.replace("{postId}", createdPostId);
            Response res = ApiReuse.execute(new ApiReuse(endpoint, "DELETE", null), headers);
            AiReporter.addRecord("DELETE " + endpoint + " → " + res.getStatusCode());
        }
    }

    // ---------------- PROFILE ----------------
    @Test(priority = 6, dependsOnMethods = {"testLogin"})
    public void testProfile() {
        Response res = ApiReuse.execute(new ApiReuse(PROFILE_ENDPOINT, "GET", null), headers);
        AiReporter.addRecord("GET " + PROFILE_ENDPOINT + " → " + res.getStatusCode());
    }

    // ---------------- CHATBOT ----------------
    @Test(priority = 7, dependsOnMethods = {"testLogin"})
    public void testChatbot() {
        String payload = "{\"message\":\"Hello AI!\"}";
        Response res = ApiReuse.execute(new ApiReuse(CHATBOT_ENDPOINT, "POST", payload), headers);
        AiReporter.addRecord("POST " + CHATBOT_ENDPOINT + " → " + res.getStatusCode());
    }

    // ---------------- MESSAGES ----------------
    // ---------------- MESSAGES ----------------
    @Test(priority = 8, dependsOnMethods = {"testLogin"})
    public void testSendMessage() {
        // Use 'content' instead of 'message' if your API expects it
        String payload = String.format("{\"receiverId\":\"%s\",\"content\":\"Hello!\"}", messageReceiverId);
        Response res = ApiReuse.execute(new ApiReuse(MESSAGE_SEND_ENDPOINT, "POST", payload), headers);

        System.out.println("Send Message Status: " + res.getStatusCode());
        System.out.println("Send Message Body: " + res.getBody().asString());

        if(res.getStatusCode() == 201 || res.getStatusCode() == 200) {
            sentMessageId = res.jsonPath().getString("_id"); // capture actual message ID
        }
        AiReporter.addRecord("POST " + MESSAGE_SEND_ENDPOINT + " → " + res.getStatusCode());
    }

    @Test(priority = 9, dependsOnMethods = {"testSendMessage"})
    public void testMessageHistory() {
        if (messageReceiverId != null) {
            String endpoint = MESSAGE_HISTORY_ENDPOINT.replace("{receiverId}", messageReceiverId);
            Response res = ApiReuse.execute(new ApiReuse(endpoint, "GET", null), headers);
            System.out.println("Message History Status: " + res.getStatusCode());
            System.out.println("Message History Body: " + res.getBody().asString());
            AiReporter.addRecord("GET " + endpoint + " → " + res.getStatusCode());
        }
    }

    @Test(priority = 10, dependsOnMethods = {"testSendMessage"})
    public void testDeleteAllMessages() {
        Response res = ApiReuse.execute(new ApiReuse(MESSAGE_DELETE_ALL_ENDPOINT, "DELETE", null), headers);
        System.out.println("Delete All Messages Status: " + res.getStatusCode());
        System.out.println("Delete All Messages Body: " + res.getBody().asString());
        AiReporter.addRecord("DELETE " + MESSAGE_DELETE_ALL_ENDPOINT + " → " + res.getStatusCode());
    }

    @Test(priority = 11, dependsOnMethods = {"testSendMessage"})
    public void testDeleteSpecificMessage() {
        if (sentMessageId != null) {
            String endpoint = MESSAGE_DELETE_SPECIFIC_ENDPOINT.replace("{messageId}", sentMessageId);
            Response res = ApiReuse.execute(new ApiReuse(endpoint, "DELETE", null), headers);
            System.out.println("Delete Specific Message Status: " + res.getStatusCode());
            System.out.println("Delete Specific Message Body: " + res.getBody().asString());
            AiReporter.addRecord("DELETE " + endpoint + " → " + res.getStatusCode());
        } else {
            System.out.println("No message ID found to delete specific message.");
        }
    }

    // ---------------- AI SUMMARY ----------------
    @Test(priority = 12, dependsOnMethods = {"testLogin"})
    public void testAiSummaryReport() {
        String aiSummary = AiReporter.generateAndSaveSummary();
        AiReporter.clear();
    }
}
