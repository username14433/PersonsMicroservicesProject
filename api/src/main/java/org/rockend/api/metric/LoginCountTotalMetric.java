package org.rockend.api.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
    Данный компонент позволяет нам хранить счётчик успешных операций login().
    Использует Counter из Micrometer
 */

@Component
public class LoginCountTotalMetric {
    public static final String LOGIN_COUNT_TOTAL_METRIC = "individual_app_login_count_total_metric";

    private final Counter counter;

    public LoginCountTotalMetric(MeterRegistry registry) {
        counter = Counter.builder(LOGIN_COUNT_TOTAL_METRIC).register(registry);
    }

    public void incrementLoginCount() {
        counter.increment();
    }
}
