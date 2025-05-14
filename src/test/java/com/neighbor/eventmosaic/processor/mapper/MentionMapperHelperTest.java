package com.neighbor.eventmosaic.processor.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Тесты для MentionMapperHelper")
class MentionMapperHelperTest {

    private final MentionMapperHelper helper = new MentionMapperHelper();

    @Nested
    @DisplayName("Метод longToOffsetDateTime()")
    class LongToOffsetDateTimeTests {

        @ParameterizedTest(name = "[{index}] {0} -> {1}")
        @CsvSource({
                "20210101123456, 2021-01-01T12:34:56Z",
                "20231231000000, 2023-12-31T00:00:00Z",
                "19990520083015, 1999-05-20T08:30:15Z"
        })
        @DisplayName("должен корректно преобразовывать валидное число YYYYMMDDHHMMSS в OffsetDateTime UTC")
        void shouldConvertValidLongToCorrectOffsetDateTime(Long inputTimestamp, String expectedDateTimeStr) {
            OffsetDateTime expected = OffsetDateTime.parse(expectedDateTimeStr);
            OffsetDateTime actual = helper.longToOffsetDateTime(inputTimestamp);
            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("должен возвращать null для null-ввода")
        void shouldReturnNullWhenInputIsNull() {
            assertNull(helper.longToOffsetDateTime(null));
        }

        @ParameterizedTest(name = "[{index}] Некорректный ввод: {0}")
        @ValueSource(longs = {
                20210101123L,       // Слишком короткий
                202101011234567L,   // Слишком длинный
                20211301123456L,    // Некорректный месяц
                202100101123456L,   // Некорректный день (00)
                20210132123456L,    // Некорректный день (32)
                20210101126000L,    // Некорректная минута (60)
                20210101123460L,    // Некорректная секунда (60)
                0L
        })
        @DisplayName("должен возвращать null для некорректной длины или формата даты/времени")
        void shouldReturnNullWhenInputIsInvalidLengthOrDateTimeFormat(Long invalidInput) {
            assertNull(helper.longToOffsetDateTime(invalidInput));
        }
    }

    @Nested
    @DisplayName("Метод longToElasticIndexDateString()")
    class LongToElasticIndexDateStringTests {

        @ParameterizedTest(name = "[{index}] {0} -> {1}")
        @CsvSource({
                "20210101123456, 2021-01-01", // Полная дата-время
                "20231231000000, 2023-12-31", // Полная дата-время, полночь
                "19990520083015, 1999-05-20", // Другая дата-время
                "20240229,       2024-02-29", // Только дата (валидная, високосный год)
                "20231101,       2023-11-01"  // Только дата
        })
        @DisplayName("должен корректно извлекать YYYY-MM-DD из валидного Long (YYYYMMDD... -> YYYY-MM-DD)")
        void shouldExtractDateStringFromValidLong(Long inputTimestamp, String expectedDateStr) {
            String actual = helper.longToElasticIndexDateString(inputTimestamp);
            assertEquals(expectedDateStr, actual);
        }

        @Test
        @DisplayName("должен возвращать null для null-ввода")
        void shouldReturnNullWhenInputIsNull() {
            assertNull(helper.longToElasticIndexDateString(null));
        }

        @ParameterizedTest(name = "[{index}] Некорректный ввод: {0}")
        @ValueSource(longs = {
                202101L,    // Слишком короткий (меньше 8 символов для YYYYMMDD)
                1234567L,   // Слишком короткий (7 символов)
                0L,         // Нулевое значение
        })
        @DisplayName("должен возвращать null для некорректной длины (<8 символов) или значения")
        void shouldReturnNullWhenInputIsInvalidLengthOrValue(Long invalidInput) {
            assertNull(helper.longToElasticIndexDateString(invalidInput));
        }
    }
}