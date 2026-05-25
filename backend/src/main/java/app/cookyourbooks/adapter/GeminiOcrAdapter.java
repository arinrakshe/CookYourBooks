package app.cookyourbooks.adapter;

import app.cookyourbooks.config.CookYourBooksProperties;
import app.cookyourbooks.exception.ExternalServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiOcrAdapter {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";

    private static final String PROMPT = """
        You are extracting a single recipe from the attached image of a cookbook page,
        recipe card, or hand-written note. Return ONLY valid JSON matching this schema:
        {
          "title": string,
          "description": string | null,
          "servings": number | null,
          "steps": [string, ...],
          "ingredients": [
            {
              "rawText": string,
              "name": string | null,
              "quantity": number | null,
              "unit": string | null,
              "preparation": string | null,
              "notes": string | null
            }
          ],
          "notes": string | null
        }
        - "rawText" must echo the original ingredient line verbatim from the source.
        - "unit" should be the canonical code if recognizable (cup, tbsp, tsp, g, kg, oz, lb, ml, l, fl_oz, pinch, dash, clove, slice, can, package, bunch, head, stick, piece, to_taste).
        - "steps" is an ordered list of imperative instructions.
        - If a field is unknown, use null (or [] for arrays). Do not invent content.
        """;

    private final CookYourBooksProperties properties;
    private final ObjectMapper objectMapper;

    public OcrExtractedRecipe extract(byte[] imageBytes, String contentType) {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalServiceException(
                "Gemini API key is not configured (cookyourbooks.gemini.api-key)");
        }
        String model = properties.getGemini().getModel();
        String mimeType = contentType == null || contentType.isBlank()
            ? MediaType.IMAGE_JPEG_VALUE : contentType;
        String encoded = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(
                    Map.of("text", PROMPT),
                    Map.of("inline_data", Map.of(
                        "mime_type", mimeType,
                        "data", encoded))))),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.2));

        try {
            String response = RestClient.create(BASE_URL).post()
                .uri(uri -> uri.path("/v1beta/models/{model}:generateContent")
                    .queryParam("key", apiKey)
                    .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
            return parseResponse(response);
        } catch (RestClientException ex) {
            log.warn("Gemini request failed: {}", ex.getMessage());
            throw new ExternalServiceException("Gemini OCR request failed", ex);
        }
    }

    private OcrExtractedRecipe parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode textNode = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.isNull()) {
                throw new ExternalServiceException("Gemini returned no recipe content");
            }
            String json = textNode.asText();
            return objectMapper.readValue(json, OcrExtractedRecipe.class);
        } catch (JsonProcessingException ex) {
            throw new ExternalServiceException("Gemini returned malformed JSON", ex);
        }
    }
}
