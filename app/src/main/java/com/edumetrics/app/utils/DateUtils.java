package com.edumetrics.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_TIME =
            new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public static String getCurrentDate() {
        return DATE_FORMAT.format(new Date());
    }

    public static String getCurrentTime() {
        return TIME_FORMAT.format(new Date());
    }

    public static String formatDateForDisplay(String date) {
        try {
            Date parsed = DATE_FORMAT.parse(date);
            return DISPLAY_FORMAT.format(parsed);
        } catch (Exception e) {
            return date;
        }
    }

    public static String formatTimestamp(long timestamp) {
        return DISPLAY_DATE_TIME.format(new Date(timestamp));
    }

    public static String getDateNDaysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return DATE_FORMAT.format(cal.getTime());
    }

    public static boolean isWithinMinutes(long timestamp, int minutes) {
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - timestamp);
        // Allow up to 30 minutes for clock drift and practical session time.
        return diff <= (30 * 60 * 1000L);
    }

    public static String formatDate(String format, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(date);
    }
}
