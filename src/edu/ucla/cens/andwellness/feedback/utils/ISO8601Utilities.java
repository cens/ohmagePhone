package edu.ucla.cens.andwellness.feedback.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ISO8601Utilities {
    private static DateFormat m_ISO8601Local =
        new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss");

    public static String formatDateTime()
    {
        return formatDateTime (new Date());
    }

    public static String formatDateTime (Date date)
    {
        if (date == null) {
            return formatDateTime (new Date());
        }

        // format in (almost) ISO8601 format
        String dateStr = m_ISO8601Local.format (date);

        // remap the timezone from 0000 to 00:00 (starts at char 22)
        return dateStr.substring (0, 22)
            + ":" + dateStr.substring (22);
    }
}
