
package org.ohmage.db.utils;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;

public class ISO8601Utilities {
    /**
     * Prints a nice human readable format
     */
    private static DateTimeFormatter printer = DateTimeFormat
            .forPattern("MMMM d',' yyyy 'at' h:mma");

    /**
     * Parses objects from ISO8601 format
     */
    private static DateTimeFormatter parser = ISODateTimeFormat.dateTime();

    /**
     * Parses objects from ISO8601 format, but doesn't convert times in other
     * timezones to local time
     */
    private static DateTimeFormatter parserWithOffset = parser.withOffsetParsed();

    /**
     * Prints an ISO8601 formatted timestamp in a human readable way. Note: does
     * not convert timestamp to local time.
     * 
     * @param string
     * @return a string like 'March 20, 2013 at 2:28pm'
     */
    public static String print(String string) {
        return print(parse(string));
    }

    /**
     * Prints a {@link DateTime} in a human readable way.
     * 
     * @param dateTime
     * @return a string like 'March 20, 2013 at 2:28pm'
     */
    public static String print(DateTime dateTime) {
        return printer.print(dateTime).replace("AM", "am").replace("PM", "pm");
    }

    /**
     * Parses an ISO8601 formatted timestamp to a {@link DateTime}. Note: does
     * not convert timestamp to local time.
     * 
     * @param string
     * @return a {@link DateTime}
     */
    public static DateTime parse(String string) {
        return parserWithOffset.parseDateTime(string);
    }

    /**
     * Formats a date to an ISO8601 formatted timestamp
     * 
     * @param time
     * @return an ISO8601 formatted timestamp
     */
    public static String format(Date time) {
        return parser.print(time.getTime());
    }
}
