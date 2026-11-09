package com.fu.cinema.service;

import com.fu.cinema.exception.BusinessException;
import com.fu.cinema.repository.BookingRepository;
import com.fu.cinema.service.impl.ReportServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ReportValidationTests {
    static Stream<LocalDate[]> invalidPeriods() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        return Stream.of(new LocalDate[]{null, date}, new LocalDate[]{date, null},
                new LocalDate[]{null, null}, new LocalDate[]{date, date.minusDays(1)});
    }

    @ParameterizedTest
    @MethodSource("invalidPeriods")
    void rejectsInvalidDatesBeforeQuerying(LocalDate start, LocalDate end) {
        BookingRepository repository = mock(BookingRepository.class);
        ReportService service = new ReportServiceImpl(repository);
        assertThatThrownBy(() -> service.generate(start, end)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository);
    }
}
