package com.ba.payment.gateway.controller;

import com.ba.payment.gateway.dto.*;
import com.ba.payment.gateway.entity.User;
import com.ba.payment.gateway.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing applications and their payment provider configurations.
 * Enables multi-tenant support where different applications use different payment providers.
 */
@Slf4j
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /**
     * Create a new application for a merchant
     */
    @PostMapping
    public ResponseEntity<ApplicationDTO> createApplication(
            @Valid @RequestBody CreateApplicationRequest request,
            @AuthenticationPrincipal User user) {

        // TODO: Get merchant from authenticated user
        // For now, assuming user has merchant relationship
        Long merchantId = 1L; // Replace with actual merchant lookup

        ApplicationDTO application = applicationService.createApplication(request, merchantId);
        log.info("Application created: {} by user: {}", application.getName(), user.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    /**
     * Get application by ID
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApplicationDTO> getApplication(@PathVariable Long applicationId) {
        ApplicationDTO application = applicationService.getApplication(applicationId);
        return ResponseEntity.ok(application);
    }

    /**
     * Get application by app key
     */
    @GetMapping("/by-key/{appKey}")
    public ResponseEntity<ApplicationDTO> getApplicationByKey(@PathVariable String appKey) {
        ApplicationDTO application = applicationService.getApplicationByKey(appKey);
        return ResponseEntity.ok(application);
    }

    /**
     * Get all applications for a merchant
     */
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<ApplicationDTO>> getApplicationsByMerchant(@PathVariable Long merchantId) {
        List<ApplicationDTO> applications = applicationService.getApplicationsByMerchant(merchantId);
        return ResponseEntity.ok(applications);
    }

    /**
     * Configure a payment provider for an application
     */
    @PostMapping("/{applicationId}/providers")
    public ResponseEntity<ApplicationProviderDTO> configureProvider(
            @PathVariable Long applicationId,
            @Valid @RequestBody ConfigureProviderRequest request,
            @AuthenticationPrincipal User user) {

        ApplicationProviderDTO provider = applicationService.configureProvider(applicationId, request);
        log.info("Provider {} configured for application {} by user: {}",
                provider.getProviderName(), applicationId, user.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(provider);
    }

    /**
     * Get all configured providers for an application
     */
    @GetMapping("/{applicationId}/providers")
    public ResponseEntity<List<ApplicationProviderDTO>> getApplicationProviders(@PathVariable Long applicationId) {
        List<ApplicationProviderDTO> providers = applicationService.getApplicationProviders(applicationId);
        return ResponseEntity.ok(providers);
    }

    /**
     * Deactivate a provider for an application
     */
    @DeleteMapping("/{applicationId}/providers/{providerId}")
    public ResponseEntity<Void> deactivateProvider(
            @PathVariable Long applicationId,
            @PathVariable Long providerId,
            @AuthenticationPrincipal User user) {

        applicationService.deactivateProvider(applicationId, providerId);
        log.info("Provider {} deactivated for application {} by user: {}",
                providerId, applicationId, user.getUsername());

        return ResponseEntity.noContent().build();
    }
}

