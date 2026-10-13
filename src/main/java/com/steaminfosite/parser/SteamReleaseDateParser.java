package com.steaminfosite.parser;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SteamReleaseDateParser {
    private static final List<DateTimeFormatter> DATE_FORMATTERS =
            List.of(
                    formatter("d MMMM uuuu"),
                    formatter("d MMM uuuu"),
                    formatter("d MMM, uuuu"),
                    formatter("MMM d, uuuu"),
                    formatter("d MMMM, uuuu"),
                    formatter("MMMM d, uuuu"),
                    formatter("uuuu-MM-dd")
            );

    private static final List<DateTimeFormatter> MONTH_FORMATTERS =
            List.of(
                    formatter("MMM uuuu"),
                    formatter("MMMM uuuu"),
                    formatter("MMM, uuuu"),
                    formatter("MMMM, uuuu"),
                    formatter("uuuu-MM")
            );

    private static final Pattern QUARTER_PATTERN =
            Pattern.compile(
                    "^Q([1-4])\\s+(\\d{4})$",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern YEAR_PATTERN = Pattern.compile("^\\d{4}$");
    //연 월 일이 모두 있는 정상 날짜만 반환
    public LocalDate parseExactDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        String value = rawDate
                .replace('\u00A0', ' ')
                .trim()
                .replaceAll("\\s+", " ");

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                //다른 날짜 형식으로 확인
            }
        }

        return null;
    }

    public LocalDate parseForSort(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        String value = rawDate.replace('\u00A0', ' ')
                .trim().replaceAll("\\s+", " ");
        //정확한 날짜가 있으면 그대로 정렬에 사용
        LocalDate exactDate = parseExactDate(value);

        if (exactDate != null) {
            return exactDate;
        }
        //월까지만 제공된 경우 해당 월의 마지막 날
        for (DateTimeFormatter formatter : MONTH_FORMATTERS) {
            try {
                return YearMonth.parse(value, formatter)
                        .atEndOfMonth();
            } catch (DateTimeParseException ignored) {
                //다른 월 형식으로 확인
            }
        }
        //분기까지만 제공된 경우 해당 분기의 마지막 날
        Matcher quarterMatcher = QUARTER_PATTERN.matcher(value);

        if (quarterMatcher.matches()) {
            int quarter = Integer.parseInt(quarterMatcher.group(1));
            int year = Integer.parseInt(quarterMatcher.group(2));

            return YearMonth.of(year, quarter * 3).atEndOfMonth();
        }
        //연도만 제공된 경우 해당 연도의 마지막 날
        if (YEAR_PATTERN.matcher(value).matches()) {
            int year = Integer.parseInt(value);
            return LocalDate.of(year, 12, 31);
        }
        //Coming soon, To be announced, 해석할 수 없는 값
        return null;
    }

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH)
                .withResolverStyle(ResolverStyle.STRICT);
    }
}