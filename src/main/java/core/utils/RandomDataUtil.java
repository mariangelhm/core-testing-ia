package core.utils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

/**
 * Random data generator for test fixtures.
 */
public final class RandomDataUtil {

    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final Random RANDOM = new SecureRandom();

    private RandomDataUtil() {
    }

    public static String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);
        }
        return builder.toString();
    }

    public static String randomEmail() {
        return String.format(Locale.ROOT, "%s@example.com", randomString(10).toLowerCase(Locale.ROOT));
    }

    public static int randomInt(int min, int max) {
        return RANDOM.nextInt((max - min) + 1) + min;
    }
}
