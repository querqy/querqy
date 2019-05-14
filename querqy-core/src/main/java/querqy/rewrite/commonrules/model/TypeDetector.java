package querqy.rewrite.commonrules.model;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import org.noggit.JSONParser;
import querqy.rewrite.commonrules.ValidationError;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TypeDetector {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public static Object getTypedObjectFromString(final String value) {
        Object valData = null;
        try {
            JSONParser jsonParser = new JSONParser(value);
            int ev = jsonParser.nextEvent();

            switch (ev) {
                case JSONParser.STRING:
                    final String stringValue = jsonParser.getString();
                    // TODO introduce date type
//                    valData = checkForDate(stringValue);
//                    if (valData == null) {
//                        valData = stringValue;
//                    }
                    return stringValue;
                case JSONParser.LONG:
                    valData = jsonParser.getLong();
                    break;
                case JSONParser.NUMBER:
                    valData = jsonParser.getDouble();
                    break;
                case JSONParser.BOOLEAN:
                    valData = jsonParser.getBoolean();
                    break;
                default:
                    return null;
            }
//            ev = jsonParser.nextEvent();
        } catch (Exception ex) {
            // FIXME: throw exception
            return valData;
        }
        return valData;
    }
/*
    private  static Object checkForDate(String str) {

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss[.SSS]][Z]", Locale.ROOT);
        try {
            return LocalDateTime.parse(str, ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        /*
        // example: +2014-10-23T21:22:33.159Z
        if (str == null || str.length() < 4) {
            return str;
        }

        Calendar cal = Calendar.getInstance(UTC, Locale.ROOT);
        int offset = 0;//a pointer
        int parsedVal = 0;
        try {
            //year & era:
            int lastOffset = str.charAt(str.length() - 1) == 'Z' ? str.length() - 1 : str.length();
            int hyphenIdx = str.indexOf('-', 1);//look past possible leading hyphen
            if (hyphenIdx < 0)
                hyphenIdx = lastOffset;
            int year = Integer.parseInt(str.substring(offset, hyphenIdx));
            cal.set(Calendar.ERA, year <= 0 ? 0 : 1);
            cal.set(Calendar.YEAR, year <= 0 ? -1 * year + 1 : year);
            offset = hyphenIdx + 1;
            if (lastOffset < offset)
                return cal.getTime();

            //NOTE: We aren't validating separator chars, and we unintentionally accept leading +/-.
            // The str.substring()'s hopefully get optimized to be stack-allocated.

            //month:
            parsedVal = parseAndCheckDateParams(str, offset, 1, 12);
            cal.set(Calendar.MONTH, parsedVal - 1);//starts at 0
            offset += 3;
            if (lastOffset < offset)
                return cal.getTime();
            //day:
            checkDateTimeDelimeter(str, offset - 1, '-');

            parsedVal = parseAndCheckDateParams(str, offset, 1, 31);
            cal.set(Calendar.DAY_OF_MONTH, parsedVal);
            offset += 3;
            if (lastOffset < offset)
                return cal.getTime();
            checkDateTimeDelimeter(str, offset - 1, 'T');
            //hour:

            parsedVal = parseAndCheckDateParams(str, offset, 0, 24);
            cal.set(Calendar.HOUR_OF_DAY, parsedVal);
            offset += 3;
            if (lastOffset < offset)
                return cal.getTime();
            checkDateTimeDelimeter(str, offset - 1, ':');
            //minute:

            parsedVal = parseAndCheckDateParams(str, offset, 0, 59);
            cal.set(Calendar.MINUTE, parsedVal);
            offset += 3;
            if (lastOffset < offset)
                return cal.getTime();
            checkDateTimeDelimeter(str, offset - 1, ':');
            //second:

            parsedVal = parseAndCheckDateParams(str, offset, 0, 59);
            cal.set(Calendar.SECOND, parsedVal);
            offset += 3;
            if (lastOffset < offset)
                return cal.getTime();
            checkDateTimeDelimeter(str, offset - 1, '.');
            //ms:

            cal.set(Calendar.MILLISECOND, Integer.parseInt(str.substring(offset, offset + 3)));
            offset += 3;//last one, move to next char
            if (lastOffset == offset)
                return cal.getTime();

        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static void checkDateTimeDelimeter(String str, int offset, char delim) {
        if (str.charAt(offset) != delim) {
            throw new IllegalArgumentException("Invalid delimeter: '" + str.charAt(offset) +
                    "', expecting '" + delim + "'");
        }
    }

    private static  int parseAndCheckDateParams(String str, int offset, int min, int max) {
        int val = Integer.parseInt(str.substring(offset, offset + 2));
        if (val < min || val > max) {
            throw new IllegalArgumentException("Invalid value: " + val + "," +
                    " expecting from " + min + " to " + max + "]");
        }
        return val;
    }*/
}
