package org.JustRun.AuthService.Controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.JustRun.AuthService.Service.AuthService;
import org.JustRun.AuthService.Service.PostHogService;
import org.JustRun.AuthService.dto.AuthRequest;
import org.JustRun.AuthService.dto.AuthResponse;
import org.JustRun.AuthService.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final PostHogService postHogService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
//        return ResponseEntity.ok(authService.register(request));
        AuthResponse response = authService.register(request);
        trackAuthEvent(request.getUsername(), "user_registered", "Registration success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
//        return ResponseEntity.ok(authService.authenticate(request));
        AuthResponse response = authService.authenticate(request);
        trackAuthEvent(request.getUsername(), "user_logged_in", "Login success");
        return ResponseEntity.ok(response);
    }

    private void trackAuthEvent(String username, String eventName, String status) {
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put("username", username);
        eventProperties.put("status", status);

        // Track event using PostHog
        postHogService.trackEvent(username, eventName, eventProperties);
        System.out.println("[Analytics] Event '" + eventName + "' for username '" + username + "' sent to PostHog with status '" + status + "'.");
    }

}
