package com.pavan.salesforceswitch.controller;

import com.pavan.salesforceswitch.dto.ValidationRuleDto;
import com.pavan.salesforceswitch.service.SalesforceService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class SalesforceController {

    private final SalesforceService salesforceService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public SalesforceController(SalesforceService salesforceService) {
        this.salesforceService = salesforceService;
    }

    @GetMapping("/auth/login")
    public void login(HttpServletResponse response) throws IOException {
        response.sendRedirect(salesforceService.getLoginUrl());
    }

    @GetMapping("/oauth/callback")
    public void callback(@RequestParam String code, HttpServletResponse response) throws IOException {
        try {
            salesforceService.getToken(code);
            response.sendRedirect(frontendUrl + "?login=success");
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/plain");
            response.getWriter().write("Salesforce Login Error: " + e.getMessage());
        }
    }

    @GetMapping("/api/validation-rules")
    public Object getRules() {
        try {
            List<ValidationRuleDto> rules = salesforceService.getValidationRules();
            return rules;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }

    @PatchMapping("/api/validation-rules/{id}")
    public Map<String, String> updateRule(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> request
    ) {
        try {
            Boolean active = request.get("active");
            String message = salesforceService.updateValidationRule(id, active);
            return Map.of("message", message);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", e.getMessage());
        }
    }
}