package core.utils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

import org.slf4j.Logger;

import core.log.LoggerUtil;
import core.log.StructuredLog;

/**
 * Random data generator for test fixtures.
 */
public final class RandomDataUtil {

    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final Random RANDOM = new SecureRandom();
    private static final Logger LOGGER = LoggerUtil.getLogger(RandomDataUtil.class);

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
        String result = builder.toString();
        logGeneration("CADENA",
                new String[][] {
                        { "Longitud", String.valueOf(length) },
                        { "Resultado", result }
                });
        return result;
    }

    /**
     * Generates a random e-mail address using the {@code example.com} domain.
     *
     * @return random email
     */
    public static String randomEmail() {
        String localPart = randomString(10).toLowerCase(Locale.ROOT);
        String email = String.format(Locale.ROOT, "%s@example.com", localPart);
        logGeneration("EMAIL", new String[][] {
                { "Usuario", localPart },
                { "Resultado", email }
        });
        return email;
    }

    /**
     * Generates a random integer between {@code min} and {@code max} inclusive.
     *
     * @param min lower bound (inclusive)
     * @param max upper bound (inclusive)
     * @return random integer between the provided bounds
     */
    public static int randomInt(int min, int max) {
        int value = RANDOM.nextInt((max - min) + 1) + min;
        logGeneration("ENTERO", new String[][] {
                { "M\u00EDnimo", String.valueOf(min) },
                { "M\u00E1ximo", String.valueOf(max) },
                { "Resultado", String.valueOf(value) }
        });
        return value;
    }

    private static void logGeneration(String tipo, String[][] pairs) {
        StructuredLog.Block block = StructuredLog.open(LOGGER, "DATOS ALEATORIOS", tipo);
        for (String[] pair : pairs) {
            block.line(pair[0], pair[1]);
        }
        block.close("Generado");
    }
}
