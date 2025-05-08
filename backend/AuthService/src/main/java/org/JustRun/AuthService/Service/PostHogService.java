package org.JustRun.AuthService.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PostHogService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${posthog.api.url}")
    private String posthogApiUrl;

    @Value("${posthog.api.key}")
    private String posthogApiKey;

    public void trackEvent(String distinctId, String eventName, Map<String, Object> properties) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("api_key", posthogApiKey);
        requestBody.put("event", eventName);
        requestBody.put("distinct_id", distinctId);
        requestBody.put("properties", properties);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        restTemplate.postForObject(posthogApiUrl, request, String.class);
    }
}
