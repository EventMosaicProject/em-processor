package com.neighbor.eventmosaic.processor.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Тесты для EventMapperHelper")
class EventMapperHelperTest {

    private final EventMapperHelper helper = new EventMapperHelper();

    @Nested
    @DisplayName("Метод integerToElasticIndexDateString()")
    class IntegerToElasticIndexDateStringTests {

        @ParameterizedTest(name = "[{index}] {0} -> {1}")
        @CsvSource({
                "20210101, 2021-01-01",
                "20231231, 2023-12-31",
                "19990520, 1999-05-20"
        })
        @DisplayName("должен корректно преобразовывать валидное число YYYYMMDD в строку YYYY-MM-DD")
        void shouldConvertValidIntegerToCorrectDateString(Integer input, String expected) {
            String actual = helper.integerToElasticIndexDateString(input);
            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("должен возвращать null для null-ввода")
        void shouldReturnNullWhenInputIsNull() {
            assertNull(helper.integerToElasticIndexDateString(null));
        }

        @ParameterizedTest(name = "[{index}] Некорректный ввод: {0}")
        @ValueSource(ints = {202101, 0, 123, -20210101})
        @DisplayName("должен возвращать null для некорректной длины или значения")
        void shouldReturnNullWhenInputIsInvalidLengthOrValue(Integer invalidInput) {
            assertNull(helper.integerToElasticIndexDateString(invalidInput));
        }
    }

    @Nested
    @DisplayName("Метод integerToOffsetDate()")
    class IntegerToOffsetDateTests {

        @Test
        @DisplayName("должен корректно преобразовывать валидное число YYYYMMDD в OffsetDateTime (начало дня UTC)")
        void shouldConvertValidIntegerToCorrectOffsetDateTime() {
            Integer day = 20210101;
            OffsetDateTime expected = OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime actual = helper.integerToOffsetDate(day);
            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("должен корректно преобразовывать другое валидное число YYYYMMDD")
        void shouldConvertAnotherValidIntegerToCorrectOffsetDateTime() {
            Integer day = 20231231;
            OffsetDateTime expected = OffsetDateTime.of(2023, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime actual = helper.integerToOffsetDate(day);
            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("должен возвращать null для null-ввода")
        void shouldReturnNullWhenInputIsNull() {
            assertNull(helper.integerToOffsetDate(null));
        }

        @ParameterizedTest(name = "[{index}] Некорректный ввод: {0}")
        @ValueSource(ints = {202101, 20211301, 0, 1234567}) // Некорректная длина, некорректный месяц
        @DisplayName("должен возвращать null для некорректной длины или формата")
        void shouldReturnNullWhenInputIsInvalidLengthOrFormat(Integer invalidInput) {
            assertNull(helper.integerToOffsetDate(invalidInput));
        }
    }

    @Nested
    @DisplayName("Метод longToOffsetDateTime()")
    class LongToOffsetDateTimeTests {

        @Test
        @DisplayName("должен корректно преобразовывать валидное число YYYYMMDDHHMMSS в OffsetDateTime UTC")
        void shouldConvertValidLongToCorrectOffsetDateTime() {
            Long timestamp = 20210101123456L;
            OffsetDateTime expected = OffsetDateTime.of(2021, 1, 1, 12, 34, 56, 0, ZoneOffset.UTC);
            OffsetDateTime actual = helper.longToOffsetDateTime(timestamp);
            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("должен корректно преобразовывать другое валидное число YYYYMMDDHHMMSS (полночь)")
        void shouldConvertAnotherValidLongToCorrectOffsetDateTime() {
            Long timestamp = 20231231000000L;
            OffsetDateTime expected = OffsetDateTime.of(2023, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);
            OffsetDateTime actual = helper.longToOffsetDateTime(timestamp);
            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("должен возвращать null для null-ввода")
        void shouldReturnNullWhenInputIsNull() {
            assertNull(helper.longToOffsetDateTime(null));
        }

        @ParameterizedTest(name = "[{index}] Некорректный ввод: {0}")
        @ValueSource(longs = {202101011234L, 202101011234567L, 20211301123456L, 0L}) // Слишком короткий, слишком длинный, некорректный месяц
        @DisplayName("должен возвращать null для некорректной длины или формата")
        void shouldReturnNullWhenInputIsInvalidLengthOrFormat(Long invalidInput) {
            assertNull(helper.longToOffsetDateTime(invalidInput));
        }
    }
}