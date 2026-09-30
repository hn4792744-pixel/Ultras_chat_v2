package me.uc_hussein.ultraschat.util;

/** Converts Latin letters to small caps while leaving MiniMessage tags and %placeholders% untouched. */
public final class SmallCaps {
    private static final char[] MAP = {
            '\u1D00', '\u0299', '\u1D04', '\u1D05', '\u1D07', '\u0493', '\u0262', '\u029C', '\u026A',
            '\u1D0A', '\u1D0B', '\u029F', '\u1D0D', '\u0274', '\u1D0F', '\u1D18', '\u01EB', '\u0280',
            's', '\u1D1B', '\u1D1C', '\u1D20', '\u1D21', 'x', '\u028F', '\u1D22'
    };

    private SmallCaps() {
    }

    public static char convert(char c) {
        if (c >= 'a' && c <= 'z') {
            return MAP[c - 'a'];
        }
        if (c >= 'A' && c <= 'Z') {
            return MAP[c - 'A'];
        }
        return c;
    }

    public static String convert(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            sb.append(convert(text.charAt(i)));
        }
        return sb.toString();
    }

    /** Converts literal text of a MiniMessage template only. */
    public static String convertTemplate(String t) {
        StringBuilder sb = new StringBuilder(t.length());
        int i = 0;
        while (i < t.length()) {
            char c = t.charAt(i);
            if (c == '<') {
                int end = t.indexOf('>', i);
                if (end > i) {
                    sb.append(t, i, end + 1);
                    i = end + 1;
                    continue;
                }
            } else if (c == '%') {
                int end = t.indexOf('%', i + 1);
                if (end > i + 1 && isToken(t, i + 1, end)) {
                    sb.append(t, i, end + 1);
                    i = end + 1;
                    continue;
                }
            }
            sb.append(convert(c));
            i++;
        }
        return sb.toString();
    }

    private static boolean isToken(String t, int from, int to) {
        for (int i = from; i < to; i++) {
            char c = t.charAt(i);
            if (!(c >= 'a' && c <= 'z') && c != '_' && !(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }
}
