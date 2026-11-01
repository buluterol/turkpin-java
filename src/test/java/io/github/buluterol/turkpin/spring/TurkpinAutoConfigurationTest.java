package io.github.buluterol.turkpin.spring;

import static org.junit.jupiter.api.Assertions.*;

import io.github.buluterol.turkpin.TurkpinClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TurkpinAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TurkpinAutoConfiguration.class));

    @Test
    void createsClientFromProperties() {
        runner.withPropertyValues("turkpin.username=dealer@example.com", "turkpin.password=secret", "turkpin.test=true")
                .run(ctx -> {
                    assertEquals(TurkpinClient.TEST_URL, ctx.getBean(TurkpinClient.class).getUrl());
                });
    }

    @Test
    void defaultsToLive() {
        runner.withPropertyValues("turkpin.username=u", "turkpin.password=p")
                .run(ctx -> assertEquals(TurkpinClient.LIVE_URL, ctx.getBean(TurkpinClient.class).getUrl()));
    }

    @Test
    void skippedWithoutUsername() {
        runner.run(ctx -> assertFalse(ctx.containsBean("turkpinClient")));
    }

    @Test
    void userBeanWins() {
        TurkpinClient own = new TurkpinClient("x", "y", true);
        runner.withPropertyValues("turkpin.username=u", "turkpin.password=p")
                .withBean(TurkpinClient.class, () -> own)
                .run(ctx -> assertSame(own, ctx.getBean(TurkpinClient.class)));
    }
}
