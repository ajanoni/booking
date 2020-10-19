package com.ajanoni.common;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public final class DateUtil {

    private DateUtil() {
    }

    public static List<LocalDate> getContinuousDates(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toUnmodifiableList());
    }
}
