package io.github.buluterol.turkpin.spring;

import io.github.buluterol.turkpin.TurkpinClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Registers a TurkpinClient bean when {@code turkpin.username} is set (Spring Boot 2.7 and 3.x). */
@AutoConfiguration
@EnableConfigurationProperties(TurkpinProperties.class)
@ConditionalOnProperty(prefix = "turkpin", name = "username")
public class TurkpinAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public TurkpinClient turkpinClient(TurkpinProperties props) {
        String url = props.getUrl() != null && !props.getUrl().isEmpty() ? props.getUrl()
                : props.isTest() ? TurkpinClient.TEST_URL : TurkpinClient.LIVE_URL;
        return new TurkpinClient(props.getUsername(), props.getPassword(), url,
                Duration.ofSeconds(props.getTimeout()), null);
    }
}
