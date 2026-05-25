package app.cookyourbooks.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CookYourBooksProperties.class)
public class PropertiesConfig {
}
