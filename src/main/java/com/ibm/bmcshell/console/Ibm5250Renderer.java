package com.ibm.bmcshell.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * IBM i 5250 console renderer.
 *
 * <p>The BMC ibmi console emulator sends screen data as newline-delimited JSON
 * frames over the WebSocket connection. Each frame has the form:
 *
 * <pre>
 * {"rows":["row0","row1",...,"row23"],"inputRow":7,"inputCol":56}
 * </pre>
 *
 * <p>This class accumulates raw bytes, extracts complete JSON frames, parses
 * the row array and cursor position, then renders the 24×80 screen to the
 * terminal using ANSI escape sequences — giving a faithful recreation of the
 * 5250 screen with a border, colour-highlighted input fields, and a cursor
 * indicator.
 *
 * <p>Bytes that do not form a JSON frame (plain ASCII/shell output) are printed
 * verbatim so normal (non-5250) console use is unaffected.
 */
public class Ibm5250Renderer {

    private static final Logger logger = LoggerFactory.getLogger(Ibm5250Renderer.class);

    public static final int ROWS = 24;
    public static final int COLS = 80;

    // ── Raw byte accumulator ─────────────────────────────────────────────────────
    private final StringBuilder rawAccumulator = new StringBuilder(4096);

    // ── Output sink — writes directly to the underlying OS stream ────────────────
    // We use PrintStream so the renderer works whether the caller passes
    // System.out (raw mode) or a PrintWriter-backed stream (normal mode).
    private final PrintStream out;

    /** True after the first 5250 frame has been rendered. */
    private boolean frameRendered = false;

    /** Construct with a raw PrintStream (e.g. System.out — preferred in raw tty mode). */
    public Ibm5250Renderer(PrintStream out) {
        this.out = out;
    }

    /** Convenience constructor that wraps a PrintWriter's underlying stream.
     *  Used when only a JLine terminal writer is available (non-raw mode). */
    public Ibm5250Renderer(PrintWriter writer) {
        // Flush the writer first, then wrap System.out as a safe fallback.
        // In practice ConsoleCommands now always passes System.out directly.
        writer.flush();
        this.out = System.out;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Feed raw bytes from the WebSocket message handler.
     * Detects and renders JSON 5250 frames; everything else is printed verbatim.
     */
    public synchronized void feed(byte[] data) {
        if (data == null || data.length == 0) return;

        String chunk = new String(data, StandardCharsets.UTF_8);
        rawAccumulator.append(chunk);

        processAccumulator();
    }

    /** Reset all state (call when reconnecting). */
    public void reset() {
        rawAccumulator.setLength(0);
        frameRendered = false;
    }

    public boolean isFrameRendered() {
        return frameRendered;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Frame extraction
    // ─────────────────────────────────────────────────────────────────────────────

    // The 5250 frame prefix we look for before attempting any JSON parsing.
    // This avoids treating every '{' in ANSI/VT terminal output as a JSON object.
    private static final String FRAME_PREFIX = "{\"rows\"";

    private void processAccumulator() {
        String buf = rawAccumulator.toString();
        int consumed = 0;

        while (consumed < buf.length()) {
            // Look for the exact 5250 frame prefix, not a bare '{'
            int frameStart = buf.indexOf(FRAME_PREFIX, consumed);

            if (frameStart < 0) {
                // No 5250 frame prefix anywhere — flush everything remaining verbatim
                out.print(buf.substring(consumed));
                out.flush();
                consumed = buf.length();
                break;
            }

            // Flush any bytes that appear before the frame prefix verbatim
            if (frameStart > consumed) {
                out.print(buf.substring(consumed, frameStart));
                out.flush();
                consumed = frameStart;
            }

            // We're now sitting at {"rows"... — find the closing brace
            int depth = 0;
            boolean inString = false;
            boolean escape = false;
            int end = -1;
            for (int i = frameStart; i < buf.length(); i++) {
                char c = buf.charAt(i);
                if (escape)           { escape = false; continue; }
                if (c == '\\' && inString) { escape = true; continue; }
                if (c == '"')         { inString = !inString; continue; }
                if (inString)         continue;
                if (c == '{')         depth++;
                else if (c == '}') {
                    if (--depth == 0) { end = i; break; }
                }
            }

            if (end < 0) {
                // Frame is incomplete — keep the partial frame in the buffer
                // and wait for more data.
                break;
            }

            // Complete frame found — render it
            String json = buf.substring(frameStart, end + 1);
            consumed = end + 1;
            try {
                render5250Frame(json);
            } catch (Exception e) {
                logger.warn("Failed to render 5250 frame: {}", e.getMessage());
                logger.debug("Raw frame: {}", json);
            }
        }

        // Retain only the unconsumed tail (partial frame waiting for more data)
        rawAccumulator.delete(0, consumed);
    }

    /** Quick heuristic: does this JSON string look like a 5250 screen frame? */
    private static boolean looksLike5250Frame(String json) {
        return json.startsWith(FRAME_PREFIX);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Minimal JSON parser (no external dependency)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Parse and render a frame.  We do a lightweight hand-rolled parse to avoid
     * pulling in a JSON library dependency.
     */
    private void render5250Frame(String json) {
        List<String> rows = parseStringArray(json, "rows");
        int inputRow = parseInt(json, "inputRow");
        int inputCol = parseInt(json, "inputCol");

        if (rows.isEmpty()) return;

        frameRendered = true;
        renderScreen(rows, inputRow, inputCol);
    }

    /** Extract a JSON string-array value by key. */
    private static List<String> parseStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        String marker = "\"" + key + "\"";
        int keyIdx = json.indexOf(marker);
        if (keyIdx < 0) return result;

        int arrStart = json.indexOf('[', keyIdx + marker.length());
        if (arrStart < 0) return result;
        int arrEnd = json.indexOf(']', arrStart);
        if (arrEnd < 0) return result;

        String arrayContent = json.substring(arrStart + 1, arrEnd);

        // Split on '","' boundaries, stripping outer quotes
        int i = 0;
        while (i < arrayContent.length()) {
            // Skip whitespace and commas
            while (i < arrayContent.length() &&
                   (arrayContent.charAt(i) == ',' || arrayContent.charAt(i) == ' ')) i++;
            if (i >= arrayContent.length()) break;
            if (arrayContent.charAt(i) != '"') { i++; continue; }
            i++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (i < arrayContent.length()) {
                char c = arrayContent.charAt(i);
                if (c == '\\' && i + 1 < arrayContent.length()) {
                    char next = arrayContent.charAt(i + 1);
                    switch (next) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        default: sb.append(next); break;
                    }
                    i += 2;
                } else if (c == '"') {
                    i++; // closing quote
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            result.add(sb.toString());
        }
        return result;
    }

    /** Extract an integer value by key from a flat JSON object. */
    private static int parseInt(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyIdx = json.indexOf(marker);
        if (keyIdx < 0) return 0;
        int colon = json.indexOf(':', keyIdx + marker.length());
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (NumberFormatException e) { return 0; }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Screen rendering
    // ─────────────────────────────────────────────────────────────────────────────

    private void renderScreen(List<String> rows, int inputRow, int inputCol) {
        StringBuilder sb = new StringBuilder(ROWS * (COLS + 30));

        // Clear screen and move to top-left
        sb.append("\033[H\033[2J");

        // ── Top border ───────────────────────────────────────────────────────────
        sb.append("\033[1;34m╔");            // bold blue
        for (int c = 0; c < COLS; c++) sb.append('═');
        sb.append("╗\033[0m\r\n");

        // ── Rows ─────────────────────────────────────────────────────────────────
        for (int r = 0; r < ROWS; r++) {
            String row = r < rows.size() ? rows.get(r) : "";
            // Pad / truncate to exactly COLS characters
            if (row.length() < COLS) {
                row = row + " ".repeat(COLS - row.length());
            } else if (row.length() > COLS) {
                row = row.substring(0, COLS);
            }

            sb.append("\033[1;34m║\033[0m"); // left border

            if (r == inputRow) {
                // Highlight the input row: text before cursor in normal white,
                // cursor position in reverse video, text after in normal white
                String before = inputCol > 0 ? row.substring(0, Math.min(inputCol, COLS)) : "";
                String atCursor = inputCol < COLS ? String.valueOf(row.charAt(inputCol)) : " ";
                String after = inputCol + 1 < COLS ? row.substring(inputCol + 1) : "";

                sb.append("\033[97m").append(escapeAnsi(before));           // bright white
                sb.append("\033[7;93m").append(escapeAnsi(atCursor));       // reverse + bright yellow (cursor)
                sb.append("\033[0;97m").append(escapeAnsi(after));          // bright white
                sb.append("\033[0m");
            } else if (r == 0 || r == ROWS - 1) {
                // Title / status rows — highlight in bold cyan
                sb.append("\033[1;96m").append(escapeAnsi(row)).append("\033[0m");
            } else {
                // Normal row — detect dot-fill input fields and colour them
                sb.append(colorizeRow(row));
            }

            sb.append("\033[1;34m║\033[0m\r\n"); // right border
        }

        // ── Bottom border ────────────────────────────────────────────────────────
        sb.append("\033[1;34m╚");
        for (int c = 0; c < COLS; c++) sb.append('═');
        sb.append("╝\033[0m\r\n");

        // ── Status line ──────────────────────────────────────────────────────────
        sb.append("\033[90m[IBM i 5250  cursor ")
          .append(inputRow + 1).append(':').append(inputCol + 1)
          .append("  ").append(ROWS).append('×').append(COLS)
          .append("  Alt+C=Ctrl-C  Ctrl+D=disconnect]")
          .append("\033[0m\r\n");

        // ── Place the real terminal cursor at the 5250 input field position ──────
        // +2 on each axis: row 1 = top border, so screen row 0 → terminal row 2;
        // col 1 = left border, so screen col 0 → terminal col 2.
        int termRow = inputRow + 2;
        int termCol = inputCol + 2;
        sb.append(String.format("\033[%d;%dH", termRow, termCol));

        out.print(sb);
        out.flush();
    }

    /**
     * Colour a normal screen row:
     * <ul>
     *   <li>Sequences of dots (". . . . .") are typical 5250 input field markers —
     *       render them in green underline to hint they are editable.</li>
     *   <li>Everything else is rendered in normal white.</li>
     * </ul>
     */
    private static String colorizeRow(String row) {
        // Fast path — no dots
        if (!row.contains(".")) {
            return "\033[97m" + escapeAnsi(row) + "\033[0m";
        }

        StringBuilder sb = new StringBuilder(row.length() + 60);
        boolean inDots = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            boolean isDot = (c == '.');
            if (isDot && !inDots) {
                sb.append("\033[32;4m"); // green + underline for input fields
                inDots = true;
            } else if (!isDot && inDots) {
                sb.append("\033[0;97m"); // back to white
                inDots = false;
            }
            sb.append(escapeAnsi(c));
        }
        if (inDots) sb.append("\033[0m");
        else sb.append("\033[0m");
        return sb.toString();
    }

    /** Escape a raw character for safe ANSI terminal output. */
    private static String escapeAnsi(char c) {
        if (c < 0x20 || c == 0x7F) return " ";   // non-printable → space
        return String.valueOf(c);
    }

    /** Escape a string for safe ANSI terminal output. */
    private static String escapeAnsi(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) sb.append(escapeAnsi(s.charAt(i)));
        return sb.toString();
    }
}
