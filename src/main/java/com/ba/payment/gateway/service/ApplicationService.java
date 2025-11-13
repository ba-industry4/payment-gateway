package com.ba.payment.gateway.service;

import com.ba.payment.gateway.dto.*;
import com.ba.payment.gateway.entity.*;
import com.ba.payment.gateway.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing applications and their payment provider configurations.
 * Allows different applications to use different payment providers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationProviderRepository applicationProviderRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final MerchantRepository merchantRepository;

    @Transactional
    public ApplicationDTO createApplication(CreateApplicationRequest request, Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        String appKey = generateUniqueAppKey();

        Application application = Application.builder()
                .merchant(merchant)
                .name(request.getName())
                .description(request.getDescription())
                .appType(request.getAppType())
                .appKey(appKey)
                .webhookUrl(request.getWebhookUrl())
                .callbackUrl(request.getCallbackUrl())
                .allowedOrigins(request.getAllowedOrigins())
                .active(true)
                .settings(request.getSettings())
                .build();

        application = applicationRepository.save(application);
        log.info("Created application: {} with key: {}", application.getName(), application.getAppKey());

        return convertToDTO(application);
    }

    @Transactional
    public ApplicationProviderDTO configureProvider(Long applicationId, ConfigureProviderRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        PaymentProvider provider = paymentProviderRepository.findById(request.getPaymentProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment provider not found"));

        // Check if configuration already exists
        ApplicationProvider appProvider = applicationProviderRepository
                .findByApplicationAndPaymentProvider(application, provider)
                .orElse(ApplicationProvider.builder()
                        .application(application)
                        .paymentProvider(provider)
                        .build());

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            applicationProviderRepository.findByApplicationAndIsDefaultTrueAndActiveTrue(application)
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        applicationProviderRepository.save(existing);
                    });
        }

        appProvider.setPriority(request.getPriority());
        appProvider.setIsDefault(request.getIsDefault());
        appProvider.setActive(true);
        appProvider.setProviderConfig(request.getProviderConfig());
        appProvider.setSupportedCurrencies(request.getSupportedCurrencies());
        appProvider.setMinAmount(request.getMinAmount());
        appProvider.setMaxAmount(request.getMaxAmount());

        appProvider = applicationProviderRepository.save(appProvider);
        log.info("Configured provider {} for application {}", provider.getName(), application.getName());

        return convertToProviderDTO(appProvider);
    }

    @Transactional(readOnly = true)
    public ApplicationDTO getApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        return convertToDTO(application);
    }

    @Transactional(readOnly = true)
    public ApplicationDTO getApplicationByKey(String appKey) {
        Application application = applicationRepository.findByAppKeyAndActiveTrue(appKey)
                .orElseThrow(() -> new IllegalArgumentException("Application not found or inactive"));
        return convertToDTO(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> getApplicationsByMerchant(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        return applicationRepository.findByMerchant(merchant).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationProviderDTO> getApplicationProviders(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        return applicationProviderRepository.findByApplicationAndActiveTrue(application).stream()
                .map(this::convertToProviderDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApplicationProvider getDefaultProvider(Application application) {
        return applicationProviderRepository.findByApplicationAndIsDefaultTrueAndActiveTrue(application)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No default payment provider configured for application: " + application.getName()));
    }

    @Transactional(readOnly = true)
    public ApplicationProvider getProviderByName(Application application, String providerName) {
        return applicationProviderRepository.findByApplicationAndProviderName(application, providerName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment provider " + providerName + " not configured for application: " + application.getName()));
    }

    @Transactional(readOnly = true)
    public List<ApplicationProvider> getActiveProvidersByPriority(Application application) {
        return applicationProviderRepository.findActiveProvidersByApplicationOrderedByPriority(application);
    }

    @Transactional
    public void deactivateProvider(Long applicationId, Long providerId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        PaymentProvider provider = paymentProviderRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider not found"));

        ApplicationProvider appProvider = applicationProviderRepository
                .findByApplicationAndPaymentProvider(application, provider)
                .orElseThrow(() -> new IllegalArgumentException("Provider configuration not found"));

        appProvider.setActive(false);
        applicationProviderRepository.save(appProvider);
        log.info("Deactivated provider {} for application {}", provider.getName(), application.getName());
    }

    private String generateUniqueAppKey() {
        String appKey;
        do {
            appKey = "app_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        } while (applicationRepository.existsByAppKey(appKey));
        return appKey;
    }

    private ApplicationDTO convertToDTO(Application application) {
        List<ApplicationProviderDTO> providers = applicationProviderRepository
                .findByApplicationAndActiveTrue(application).stream()
                .map(this::convertToProviderDTO)
                .collect(Collectors.toList());

        return ApplicationDTO.builder()
                .id(application.getId())
                .name(application.getName())
                .description(application.getDescription())
                .appType(application.getAppType())
                .appKey(application.getAppKey())
                .webhookUrl(application.getWebhookUrl())
                .callbackUrl(application.getCallbackUrl())
                .allowedOrigins(application.getAllowedOrigins())
                .active(application.getActive())
                .settings(application.getSettings())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .providers(providers)
                .build();
    }

    private ApplicationProviderDTO convertToProviderDTO(ApplicationProvider appProvider) {
        return ApplicationProviderDTO.builder()
                .id(appProvider.getId())
                .applicationId(appProvider.getApplication().getId())
                .paymentProviderId(appProvider.getPaymentProvider().getId())
                .providerName(appProvider.getPaymentProvider().getName())
                .providerType(appProvider.getPaymentProvider().getType())
                .priority(appProvider.getPriority())
                .isDefault(appProvider.getIsDefault())
                .active(appProvider.getActive())
                .providerConfig(appProvider.getProviderConfig())
                .supportedCurrencies(appProvider.getSupportedCurrencies())
                .minAmount(appProvider.getMinAmount())
                .maxAmount(appProvider.getMaxAmount())
                .build();
    }
}

