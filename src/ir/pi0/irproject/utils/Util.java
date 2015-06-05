package ir.pi0.irproject.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private static String[] progress_str;
    private static int p_l = 20;

    static {
        progress_str = new String[p_l];
        for (int l = 0; l < p_l; l++) {
            progress_str[l] = "[";
            for (int i = 0; i <= l; i++)
                progress_str[l] += "#";
            for (int i = 0; i < p_l - l - 1; i++)
                progress_str[l] += " ";
//            progress_str[p_l-l]="";
            progress_str[l] += "]";
        }

    }

    public static void printProgress(double p, long elapsed, boolean ms) {
        clearLine();
        System.out.print(progress_str[(int) ((p_l) * p)%progress_str.length]);

        System.out.printf(" %02.01f%% ~> ETA [ %s ]", (p * 100),
                Util.getDurationBreakdown((long) (elapsed * 1.0 * ((1 - p) / p)), ms));
    }

    public static void clearLine() {
        System.out.print("\33[2K\r");
    }

    public static int countMatches(String haystack, Pattern needle) {
        Matcher matcher = needle.matcher(haystack);
        int c = 0;
        while (matcher.find()) c++;
        return c;

    }

    public static String getDurationBreakdown(long millis, boolean print_ms) {
        if (millis < 0)
            millis = 0;

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%02d", hours)).append(":");
        sb.append(String.format("%02d", minutes)).append(":");
        sb.append(String.format("%02d", seconds));

        if (millis > 0 && print_ms)
            sb.append(":").append(String.format("%03d", millis));

        return sb.toString();
    }

    public static String humanReadableByteCount(int bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    public static Character[] toCharacterArray( String s ) {

        if ( s == null ) {
            return null;
        }

        int len = s.length();
        Character[] array = new Character[len];
        for (int i = 0; i < len ; i++) {
            array[i] = new Character(s.charAt(i));
        }

        return array;
    }

}