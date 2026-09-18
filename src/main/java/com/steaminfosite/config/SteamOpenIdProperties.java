package com.steaminfosite.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "steam.openid")
public class SteamOpenIdProperties {
    @NotBlank
    private String loginUrl;

    @NotBlank
    private String realm;

    @NotBlank
    private String returnUrl;
}
