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

    /**
     * Generates a random alphanumeric string using upper case letters and digits.
     *
     * @param length number of characters to generate
     * @return random alphanumeric string
     */
    public static String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);
        }
        return builder.toString();
    }

    /**
     * Generates a random e-mail address using the {@code example.com} domain.
     *
     * @return random email
     */
    public static String randomEmail() {
        return String.format(Locale.ROOT, "%s@example.com", randomString(10).toLowerCase(Locale.ROOT));
    }

    /**
     * Generates a random integer between {@code min} and {@code max} inclusive.
     *
     * @param min lower bound (inclusive)
     * @param max upper bound (inclusive)
     * @return random integer between the provided bounds
     */
    public static int randomInt(int min, int max) {
        return RANDOM.nextInt((max - min) + 1) + min;
    }
}
