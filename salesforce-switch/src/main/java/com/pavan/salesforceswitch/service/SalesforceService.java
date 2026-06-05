package com.pavan.salesforceswitch.service;

import com.pavan.salesforceswitch.dto.TokenResponse;
import com.pavan.salesforceswitch.dto.ValidationRuleDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class SalesforceService {

    private final WebClient.Builder webClientBuilder;

    public SalesforceService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Value("${salesforce.client-id}")
    private String clientId;

    @Value("${salesforce.client-secret}")
    private String clientSecret;

    @Value("${salesforce.redirect-uri}")
    private String redirectUri;

    @Value("${salesforce.login-url}")
    private String loginUrl;

    @Value("${salesforce.api-version}")
    private String apiVersion;

    private String accessToken;
    private String instanceUrl;
    private String codeVerifier;

    public String getLoginUrl() {
        codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        return loginUrl + "/services/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&code_challenge=" + codeChallenge
                + "&code_challenge_method=S256";
    }

    public void getToken(String code) {
        WebClient webClient = webClientBuilder.build();

        String body = "grant_type=authorization_code"
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&code_verifier=" + URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8);

        TokenResponse response = webClient.post()
                .uri(loginUrl + "/services/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .block();

        if (response != null) {
            accessToken = response.getAccessToken();
            instanceUrl = response.getInstanceUrl();

            System.out.println("Salesforce login success");
            System.out.println("Instance URL: " + instanceUrl);
        }
    }

    public List<ValidationRuleDto> getValidationRules() {
        if (accessToken == null || instanceUrl == null) {
            throw new RuntimeException("Please login with Salesforce first.");
        }

        String query = "SELECT Id, ValidationName, Active FROM ValidationRule";

        WebClient webClient = webClientBuilder.build();

        Map response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host(instanceUrl.replace("https://", ""))
                        .path("/services/data/" + apiVersion + "/tooling/query")
                        .queryParam("q", query)
                        .build())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException("Salesforce Query Error: " + errorBody))
                )
                .bodyToMono(Map.class)
                .block();

        List<ValidationRuleDto> list = new ArrayList<>();

        if (response != null && response.get("records") != null) {
            List<Map<String, Object>> records =
                    (List<Map<String, Object>>) response.get("records");

            for (Map<String, Object> record : records) {
                ValidationRuleDto dto = new ValidationRuleDto(
                        (String) record.get("Id"),
                        (String) record.get("ValidationName"),
                        (Boolean) record.get("Active"),
                        "Account"
                );

                list.add(dto);
            }
        }

        return list;
    }

    public String updateValidationRule(String id, Boolean active) {
        if (accessToken == null || instanceUrl == null) {
            throw new RuntimeException("Please login with Salesforce first.");
        }

        WebClient webClient = webClientBuilder.build();

        String getUrl = instanceUrl + "/services/data/" + apiVersion
                + "/tooling/sobjects/ValidationRule/" + id;

        Map rule = webClient.get()
                .uri(getUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException("Get Rule Error: " + errorBody))
                )
                .bodyToMono(Map.class)
                .block();

        if (rule == null || rule.get("Metadata") == null) {
            throw new RuntimeException("Validation rule metadata not found");
        }

        Map<String, Object> metadata = (Map<String, Object>) rule.get("Metadata");
        metadata.put("active", active);

        Map<String, Object> body = Map.of("Metadata", metadata);

        webClient.patch()
                .uri(getUrl)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(errorBody -> new RuntimeException("Update Rule Error: " + errorBody))
                )
                .bodyToMono(String.class)
                .block();

        if (active) {
            return "Validation rule activated successfully";
        } else {
            return "Validation rule deactivated successfully";
        }
    }

    private String generateCodeVerifier() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String generateCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating code challenge");
        }
    }
}