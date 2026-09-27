package vai.hbtweaks.context.client.keyboard;

import java.util.*;

/**
 * Watches the local chat input to decide whether the local player counts as writing: the text
 * is long enough, is not a command, and was changed recently.
 *
 * @see WritingStatusSender
 */
public class WritingObserver {

    private static long lastWrite = 0;
    private static String lastText = "";

    /** Milliseconds without any edit after which the player no longer counts as writing. */
    private static final long WRITE_PAUSE = 5000;
    /** Minimum text length to count as writing, so a single stray key does not trigger it. */
    private static final int MIN_LENGTH = 5;

    /**
     * Called by ChatScreenMixin on every edit of the chat input.
     *
     * @param text the full content of the chat input
     */
    public static void textChanged(String text) {
        lastWrite = System.currentTimeMillis();
        lastText = text;
        WritingStatusSender.onTextChanged();
    }

    public static String getText() {
        return lastText;
    }

    /**
     * @return true if the current text is long enough and is not a command
     */
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
