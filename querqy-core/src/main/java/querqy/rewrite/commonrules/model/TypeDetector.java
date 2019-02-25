package querqy.rewrite.commonrules.model;

import org.noggit.JSONParser;
import querqy.rewrite.commonrules.ValidationError;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class TypeDetector {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    public static Object getTypedObjectFromString(String value) {
        Object valData = null;
        try {
            JSONParser jsonParser = new JSONParser(value);
            int ev = jsonParser.nextEvent();

            switch (ev) {
                case JSONParser.STRING:
                    valData = checkForDate(jsonParser.getString());
                    if(valData == null) {
                        valData = jsonParser.getString();
                    }
                    break;
                case JSONParser.LONG:
                    valData = Integer.valueOf((int)(jsonParser.getDouble()));
                    break;
                case JSONParser.NUMBER:
                    valData = Double.valueOf(jsonParser.getDouble());
                    break;
                case JSONParser.BOOLEAN:
                    valData = Boolean.valueOf(jsonParser.getBoolean());
                    break;
                default:
                    return null;
            }
            ev = jsonParser.nextEvent();
        } catch (Exception ex) {
            return valData;
        }
        return valData;
    }

    private  static Object checkForDate(String str) {

        // example: +2014-10-23T21:22:33.159Z
        if (str == null || str.isEmpty())
            return str;
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
                return cal;

            //NOTE: We aren't validating separator chars, and we unintentionally accept leading +/-.
            // The str.substring()'s hopefully get optimized to be stack-allocated.

            //month:
            parsedVal = parseAndCheckDateParams(str, offset, 1, 12);
            cal.set(Calendar.MONTH, parsedVal - 1);//starts at 0
            offset += 3;
            if (lastOffset < offset)
                return cal;
            //day:
            checkDateTimeDelimeter(str, offset - 1, '-');

            parsedVal = parseAndCheckDateParams(str, offset, 1, 31);
            cal.set(Calendar.DAY_OF_MONTH, parsedVal);
            offset += 3;
            if (lastOffset < offset)
                return cal;
            checkDateTimeDelimeter(str, offset - 1, 'T');
            //hour:

            parsedVal = parseAndCheckDateParams(str, offset, 0, 24);
            cal.set(Calendar.HOUR_OF_DAY, parsedVal);
            offset += 3;
            if (lastOffset < offset)
                return cal;
            checkDateTimeDelimeter(str, offset - 1, ':');
            //minute:

            parsedVal = parseAndCheckDateParams(str, offset, 0, 59);
            cal.set(Calendar.MINUTE, parsedVal);
            offset += 3;
            if (lastOffset < offset)
                return cal;
            checkDateTimeDelimeter(str, offset - 1, ':');
            //second:

            parsedVal = parseAndCheckDateParams(str, offset, 0, 59);
            cal.set(Calendar.SECOND, parsedVal);
            offset += 3;
            if (lastOffset < offset)
                return cal;
            checkDateTimeDelimeter(str, offset - 1, '.');
            //ms:

            cal.set(Calendar.MILLISECOND, Integer.parseInt(str.substring(offset, offset + 3)));
            offset += 3;//last one, move to next char
            if (lastOffset == offset)
                return cal;

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
    }
}
