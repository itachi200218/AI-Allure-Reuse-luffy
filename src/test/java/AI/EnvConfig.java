package AI;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;

/**
 * 🌍 EnvConfig - Loads environment variables from /ENV/.env, /ENV/Tracker.env, or /ENV/Social-media.env.
 */
public class EnvConfig {

    private static final Dotenv dotenv = loadEnv();

    private static Dotenv loadEnv() {
        // Compute absolute path from project root
        String rootPath = new File(System.getProperty("user.dir")).getAbsolutePath();
        String envFolderPath = rootPath + File.separator + "ENV";

        // Paths for all env files
        String mainEnv = envFolderPath + File.separator + ".env";
        String trackerEnv = envFolderPath + File.separator + "Tracker.env";
        String socialMediaEnv = envFolderPath + File.separator + "Social-media.env";

        Dotenv loadedDotenv = null;

        try {
            if (new File(mainEnv).exists()) {
                System.out.println("🔍 Loading environment from: " + mainEnv);
                loadedDotenv = Dotenv.configure()
                        .directory(envFolderPath)
                        .filename(".env")
                        .load();
            }
            else if (new File(trackerEnv).exists()) {
                System.out.println("🔍 Loading environment from: " + trackerEnv);
                loadedDotenv = Dotenv.configure()
                        .directory(envFolderPath)
                        .filename("Tracker.env")
                        .load();
            }
            else if (new File(socialMediaEnv).exists()) {
                System.out.println("🔍 Loading environment from: " + socialMediaEnv);
                loadedDotenv = Dotenv.configure()
                        .directory(envFolderPath)
                        .filename("Social-media.env")
                        .load();
            }
            else {
                System.err.println("⚠️ No .env, Tracker.env, or Social-media.env found inside /ENV folder!");
                loadedDotenv = Dotenv.configure()
                        .ignoreIfMissing()
                        .load();
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading environment: " + e.getMessage());
            loadedDotenv = Dotenv.configure().ignoreIfMissing().load();
        }

        return loadedDotenv;
    }

    public static String get(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isEmpty()) {
            System.err.println("⚠️ Missing or empty env key: " + key);
        }
        return value;
    }
}
