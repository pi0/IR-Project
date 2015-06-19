package ir.pi0.irproject.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
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
        if (p > 1) p = 1;

        clearLine();


        System.out.print(progress_str[(int) ((p_l) * p) % progress_str.length]);
        System.out.printf(" %02.01f%% ~> [ ETA: %s ]", (p * 100),
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

    public static String humanReadableByteCount(long bytes) {
        return humanReadableByteCount((int) bytes, true);
    }

    public static String humanReadableByteCount(int bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    public static Character[] toCharacterArray(String s) {

        if (s == null) {
            return null;
        }

        int len = s.length();
        Character[] array = new Character[len];
        for (int i = 0; i < len; i++) {
            array[i] = new Character(s.charAt(i));
        }

        return array;
    }

    public static Long combineToLong(int a, int b) {
        return (long) a << 32 | b & 0xFFFFFFFFL;
    }

    public static int[] splitLongToInt(long c) {
        int[] r = new int[2];

        r[0] = (int) (c >> 32);
        r[1] = (int) c;

        return r;
    }

    public static boolean deleteRecursive(File path) throws FileNotFoundException {
        if (!path.exists()) throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

    public static void writeInt(Writer w, int v) throws IOException {
        w.write((v >>> 24) & 0xFF);
        w.write((v >>> 16) & 0xFF);
        w.write((v >>> 8) & 0xFF);
        w.write((v) & 0xFF);
    }


    //Class size
    private static final int NR_BITS = Integer.valueOf(System.getProperty("sun.arch.data.model"));
    private static final int BYTE = 8;
    private static final int WORD = NR_BITS / BYTE;
    private static final int MIN_SIZE = 16;

    public static int sizeOf(Class src) {
        //
        // Get the instance fields of src class
        //
        List<Field> instanceFields = new LinkedList<Field>();
        do {
            if (src == Object.class) return MIN_SIZE;
            for (Field f : src.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) == 0) {
                    instanceFields.add(f);
                }
            }
            src = src.getSuperclass();
        } while (instanceFields.isEmpty());
        //
        // Get the field with the maximum offset
        //
        long maxOffset = 0;
        for (Field f : instanceFields) {
            long offset = UNSAFE.objectFieldOffset(f);
            if (offset > maxOffset) maxOffset = offset;
        }
        return (((int) maxOffset / WORD) + 1) * WORD;
    }

    public static final sun.misc.Unsafe UNSAFE;

    static {
        Object theUnsafe = null;
        Exception exception = null;
        try {
            Class<?> uc = Class.forName("sun.misc.Unsafe");
            Field f = uc.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            theUnsafe = f.get(uc);
        } catch (Exception e) {
            exception = e;
        }
        UNSAFE = (sun.misc.Unsafe) theUnsafe;
        if (UNSAFE == null) throw new Error("Could not obtain access to sun.misc.Unsafe", exception);
    }


}
