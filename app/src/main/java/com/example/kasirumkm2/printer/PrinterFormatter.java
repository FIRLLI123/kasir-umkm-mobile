package com.example.kasirumkm2.printer;

public class PrinterFormatter {

    public static final int LINE_CHARS_58MM = 32;

    // ESC/POS Commands
    public static final byte[] ESC_ALIGN_LEFT = new byte[]{0x1b, 0x61, 0x00};
    public static final byte[] ESC_ALIGN_CENTER = new byte[]{0x1b, 0x61, 0x01};
    public static final byte[] ESC_ALIGN_RIGHT = new byte[]{0x1b, 0x61, 0x02};

    public static final byte[] ESC_BOLD_ON = new byte[]{0x1b, 0x45, 0x01};
    public static final byte[] ESC_BOLD_OFF = new byte[]{0x1b, 0x45, 0x00};

    public static final byte[] ESC_DOUBLE_SIZE_ON = new byte[]{0x1d, 0x21, 0x11};
    public static final byte[] ESC_DOUBLE_SIZE_OFF = new byte[]{0x1d, 0x21, 0x00};

    public static final byte[] ESC_FEED_LINE = new byte[]{0x0a};
    public static final byte[] ESC_INIT = new byte[]{0x1b, 0x40};

    /**
     * Format a line with left text and right text aligned to margins
     */
    public static String formatRow(String left, String right) {
        return formatRow(left, right, LINE_CHARS_58MM);
    }

    public static String formatRow(String left, String right, int maxChars) {
        if (left == null) left = "";
        if (right == null) right = "";

        int totalLen = left.length() + right.length();
        if (totalLen >= maxChars) {
            // Truncate left string to fit right string
            int maxLeft = maxChars - right.length() - 3;
            if (maxLeft > 0) {
                left = left.substring(0, maxLeft) + "...";
            } else {
                left = left.substring(0, Math.max(0, maxChars - right.length()));
            }
        }

        StringBuilder sb = new StringBuilder(left);
        int spaces = maxChars - left.length() - right.length();
        for (int i = 0; i < spaces; i++) {
            sb.append(" ");
        }
        sb.append(right);
        return sb.toString();
    }

    /**
     * Get a divider dashed line (e.g., "--------------------------------")
     */
    public static String getDivider() {
        return getDivider(LINE_CHARS_58MM);
    }

    public static String getDivider(int maxChars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxChars; i++) {
            sb.append("-");
        }
        return sb.toString();
    }
}
