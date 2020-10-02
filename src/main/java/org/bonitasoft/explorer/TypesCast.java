package org.bonitasoft.explorer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TypesCast {

    /**
     * to share this date format in all the page
     */
    public final static SimpleDateFormat sdfCompleteDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public static String getString(Object value, String defaultValue) {
        try {
            return (String) value;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    public static String getStringNullIsEmpty(Object value, String defaultValue) {
        try {
            String valueSt=(String) value;
            if (valueSt != null && valueSt.trim().isEmpty())
                valueSt=null;
            return valueSt;
                    
        } catch (Exception e) {
            return defaultValue;
        }
    }
    public static Boolean getBoolean(Object value, Boolean defaultValue) {
        try {
            return Boolean.valueOf(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static Integer getInteger(Object value, Integer defaultValue) {
        try {
            return Integer.valueOf(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * default toolbox method
     * 
     * @param value
     * @param defaultValue
     * @return
     */
    public static Long getLong(Object value, Long defaultValue) {
        try {
            return Long.valueOf(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 2020-10-01T00:33:00.000Z
    private final static SimpleDateFormat sdfHtml5 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static Long getHtml5ToLongDate(String dateSt, Long defaultDate) {
        if (dateSt==null)
            return defaultDate;
        try
        {
            Date date = sdfHtml5.parse(dateSt);
            return date.getTime();
        }
        catch(Exception e) 
        {
            return defaultDate;
        }
    }
    
    public static Long getLongDateFromYear( int year ) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR,0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }
    
    
    public static String getHumanDate( Date date) {
        if (date == null)
            return "";
        return sdf.format(date);
    }
    public static String getHumanDuration(long durationInMsn, boolean withMs) {
        String result = "";
        long timeRes = durationInMsn;
        long nbDays = timeRes / (1000 * 60 * 60 * 24);
        if (nbDays > 1)
            result += nbDays + " days ";

        timeRes = timeRes - nbDays * (1000 * 60 * 60 * 24);

        long nbHours = timeRes / (1000 * 60 * 60);
        result += String.format("%02d", nbHours) + ":";
        timeRes = timeRes - nbHours * (1000 * 60 * 60);

        long nbMinutes = timeRes / (1000 * 60);
        result += String.format("%02d", nbMinutes) + ":";
        timeRes = timeRes - nbMinutes * (1000 * 60);

        long nbSecond = timeRes / (1000);
        result += String.format("%02d", nbSecond) + " ";
        timeRes = timeRes - nbSecond * (1000);

        if (withMs)
            result += String.format("%03d", timeRes);
        return result;
    }
}
