package bot.utilites;

import org.telegram.telegrambots.logging.BotLogger;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.logging.Level;

/**
 * Util class.
 */
public class Util {

    /**
     * Hack to hide Netty`s warn message (for Java 9) and change default
     * logging level for BotLogger.
     */
    public static void disableWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
        BotLogger.setLevel(Level.WARNING);
    }
}
