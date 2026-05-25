package app.cookyourbooks.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "cookyourbooks")
public class CookYourBooksProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Gemini gemini = new Gemini();
    private Usda usda = new Usda();

    @Data
    public static class Jwt {
        /** Base64-encoded HMAC secret. Must decode to at least 256 bits. */
        private String secret;
        private String issuer = "cookyourbooks";
        private long accessTokenTtlMinutes = 120;
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }

    @Data
    public static class Gemini {
        private String apiKey;
        private String model = "gemini-2.5-flash";
    }

    @Data
    public static class Usda {
        private String apiKey = "DEMO_KEY";
        private String baseUrl = "https://api.nal.usda.gov/fdc/v1";
    }
}
