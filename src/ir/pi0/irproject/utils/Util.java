package ir.pi0.irproject.utils;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import gnu.trove.procedure.TIntProcedure;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private static String[] progress_str;
    private static int p_l = 30;

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

            char[] progressStr = progress_str[l].toCharArray();

            int s_offset = (int) ((p_l / 2.0) - 2);
            for (int i = 0; i < 4; i++) {
                progressStr[i + s_offset] = 'X';
            }
            progress_str[l] = String.copyValueOf(progressStr);

        }


    }

    public static void printProgress(double p, long elapsed, boolean ms, boolean eta, String title) {
        if (p > 1) p = 1;
        if (p < 0) p = 0;

        System.out.print(title + ": ");

        System.out.print(progress_str[(int) ((p_l) * p) % progress_str.length]
                .replace("XXXX", String.format("%02.01f%%", (p * 100))));

        if (eta) {
            System.out.printf(" [ ETA: %s ]",
                    Util.getDurationBreakdown((long) (elapsed * 1.0 * ((1 - p) / p)), ms));
        }
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

    public static boolean deleteRecursive(File path) {
        boolean ret = true;
        if (path.isDirectory())
            for (File f : path.listFiles())
                ret = ret && deleteRecursive(f);
        return ret && path.delete();
    }

    public static void writeInt(Writer w, int v) throws IOException {
        w.write((v >>> 24) & 0xFF);
        w.write((v >>> 16) & 0xFF);
        w.write((v >>> 8) & 0xFF);
        w.write((v) & 0xFF);
    }

    public static int log2(int bits) {
        if (bits == 0)
            return 0; // or throw exception
        return 31 - Integer.numberOfLeadingZeros(bits);
    }


    public static String TIntDoubleHashMapToString(TIntDoubleHashMap m) {
        final StringBuilder sb = new StringBuilder();
        m.forEachEntry(new TIntDoubleProcedure() {
            @Override
            public boolean execute(int i, double v) {
                sb.append(i).append(':').append(v).append(';');
                return true;
            }
        });
        return sb.toString();
    }

    public static TIntDoubleHashMap StringToTIntDoubleHashMap(String s) {
        String[] sp = s.split(";");
        TIntDoubleHashMap m = new TIntDoubleHashMap(sp.length);
        for (String ss : sp) {
            String[] p = ss.split(":");
            if (p.length != 2)
                continue;
            m.put(Integer.parseInt(p[0]), Double.parseDouble(p[1]));
        }
        return m;
    }

    public static TIntList StringToTIntList(String s) {
        String[] sp =s.split(";");
        TIntList l=new TIntArrayList(sp.length);
        for(String ss:sp)
            l.add(Integer.parseInt(ss));
        return l;
    }


    public static String TIntListToString(TIntList l) {
        final StringBuilder sb = new StringBuilder();
        l.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int i) {
                sb.append(i).append(';');
                return true;
            }
        });
        return sb.toString();
    }


//
//    public static int[] unionSets(int[] a,int[] b){
//
//
//
//
//    }


}
