package org.rockend.person_service_.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

// Утилитный класс для получения текущего времени
@Component
@RequiredArgsConstructor
public class DateTimeUtil {

    private final Clock clock;

    public Instant now() {
        return clock.instant();
    }
}
