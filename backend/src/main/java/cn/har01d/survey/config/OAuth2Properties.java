package cn.har01d.survey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.oauth2")
@Getter
@Setter
public class OAuth2Properties {
    private ProviderConfig github = new ProviderConfig();
    private ProviderConfig google = new ProviderConfig();
    private String frontendUrl = "http://localhost:5173";

    @Getter
    @Setter
    public static class ProviderConfig {
        private String clientId = "";
        private String clientSecret = "";
    }
}
