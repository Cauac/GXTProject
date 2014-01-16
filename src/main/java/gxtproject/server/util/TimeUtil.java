package gxtproject.server.util;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class TimeUtil {

    private static GregorianCalendar calendar = new GregorianCalendar();
    private static SimpleDateFormat dateFormatForDB = new SimpleDateFormat("YYYYMMdd");
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.YYYY");
    public static SimpleDateFormat dateFormatWithSec = new SimpleDateFormat("dd.MM.YYYY hh-mm-ss");
    public static SimpleDateFormat dateFormatYM = new SimpleDateFormat("YYYY.MM");

    public static String getPureDate(String unixTimeString) {
        Long time = Long.parseLong(unixTimeString) * 1000;
        calendar.setTimeInMillis(time);
        return dateFormatForDB.format(calendar.getTime());
    }

    public static String getPureDate() {
        calendar.setTimeInMillis(System.currentTimeMillis());
        return dateFormatForDB.format(calendar.getTime());
    }

    public static String getPureYesterdayDateDotSeparate() {
        calendar.setTimeInMillis(System.currentTimeMillis() - (1000 * 60 * 60 * 24));
        return dateFormat.format(calendar.getTime());
    }
}
