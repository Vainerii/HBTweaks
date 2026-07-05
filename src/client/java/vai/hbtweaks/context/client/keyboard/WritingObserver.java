package vai.hbtweaks.context.client.keyboard;

import java.util.*;

public class WritingObserver {

    private static long lastWrite = 0;
    private static String lastText = "";

    private static final long WRITE_PAUSE = 5000;
    private static final int MIN_LENGTH = 5;

    public static void textChanged(String text) {
        lastWrite = System.currentTimeMillis();
        lastText = text;
        WritingStatusSender.onTextChanged();
    }

    public static String getText() {
        return lastText;
    }

    public static boolean isEligible() {
        return lastText.length() >= MIN_LENGTH && !lastText.startsWith("/");
    }

    public static boolean hasRecentChange() {
        return System.currentTimeMillis() - lastWrite < WRITE_PAUSE;
    }

    public static boolean isWriting() {
        return isEligible() && hasRecentChange();
    }

}
