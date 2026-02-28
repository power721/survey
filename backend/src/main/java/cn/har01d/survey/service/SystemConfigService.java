package cn.har01d.survey.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.har01d.survey.entity.SystemConfig;
import cn.har01d.survey.repository.SystemConfigRepository;

@Service
public class SystemConfigService {

    // Basic
    public static final String SITE_TITLE = "site.title";
    public static final String SITE_DESCRIPTION = "site.description";
    public static final String SITE_LOGO = "site.logo";
    public static final String SITE_FOOTER = "site.footer";
    public static final String TIMEZONE = "timezone";

    // Registration
    public static final String REGISTER_ENABLED = "register.enabled";

    // OAuth2
    public static final String OAUTH2_ENABLED = "oauth2.enabled";
    public static final String GITHUB_CLIENT_ID = "oauth2.github.client-id";
    public static final String GITHUB_CLIENT_SECRET = "oauth2.github.client-secret";
    public static final String GOOGLE_CLIENT_ID = "oauth2.google.client-id";
    public static final String GOOGLE_CLIENT_SECRET = "oauth2.google.client-secret";

    // File Upload
    public static final String UPLOAD_MAX_SIZE = "upload.max-size";
    public static final String UPLOAD_ALLOWED_EXTENSIONS = "upload.allowed-extensions";

    // Security
    public static final String LOGIN_MAX_ATTEMPTS = "login.max-attempts";
    public static final String JWT_EXPIRATION_MS = "jwt.expiration-ms";

    private final SystemConfigRepository configRepository;

    public SystemConfigService(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public String get(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    public String get(String key) {
        return get(key, "");
    }

    @Transactional
    public void set(String key, String value) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElse(SystemConfig.builder().configKey(key).build());
        config.setConfigValue(value);
        configRepository.save(config);
    }

    public Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        configRepository.findAll().forEach(c -> map.put(c.getConfigKey(), c.getConfigValue()));
        return map;
    }

    @Transactional
    public void updateAll(Map<String, String> configs) {
        configs.forEach(this::set);
    }

    public Map<String, Object> getPublicConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(SITE_TITLE, get(SITE_TITLE, ""));
        config.put(SITE_DESCRIPTION, get(SITE_DESCRIPTION, ""));
        config.put(SITE_LOGO, get(SITE_LOGO, ""));
        config.put(SITE_FOOTER, get(SITE_FOOTER, ""));
        config.put(TIMEZONE, get(TIMEZONE, "Asia/Shanghai"));
        config.put(REGISTER_ENABLED, !"false".equals(get(REGISTER_ENABLED, "true")));
        config.put(OAUTH2_ENABLED, "true".equals(get(OAUTH2_ENABLED, "false")));
        return config;
    }
}
