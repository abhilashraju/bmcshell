package com.ibm.bmcshell;

import java.util.regex.Pattern;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.CommandCatalog;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Replaces Spring Shell's built-in highlighter so that an unrecognised command
 * is shown in yellow (instead of the default red) while the user is typing.
 *
 * Spring Shell's LineReaderAutoConfiguration hard-codes its own anonymous
 * Highlighter — there is no injection point.  Instead we swap it out during
 * @PostConstruct, which runs during context refresh before any ApplicationRunner
 * (including InteractiveShellRunner) is invoked.
 */
@Component
public class CustomHighlighter {

    @Autowired
    private LineReader lineReader;

    @Autowired
    private CommandCatalog commandCatalog;

    @PostConstruct
    public void install() {
        if (lineReader instanceof LineReaderImpl impl) {
            impl.setHighlighter(new ShellHighlighter(commandCatalog));
        }
    }

    /** Mirrors Spring Shell's built-in logic but uses yellow for no-match. */
    private static class ShellHighlighter implements Highlighter {

        private final CommandCatalog commandCatalog;

        ShellHighlighter(CommandCatalog commandCatalog) {
            this.commandCatalog = commandCatalog;
        }

        @Override
        public AttributedString highlight(LineReader reader, String buffer) {
            // Find the longest registered command that is a prefix of the buffer
            String matchedCommand = null;
            int matchedLen = 0;
            for (String cmd : commandCatalog.getRegistrations().keySet()) {
                if (buffer.startsWith(cmd) && cmd.length() > matchedLen) {
                    matchedLen = cmd.length();
                    matchedCommand = cmd;
                }
            }

            if (matchedCommand != null) {
                // Known command: bold command name + plain remainder (same as default)
                return new org.jline.utils.AttributedStringBuilder(buffer.length())
                        .append(matchedCommand, AttributedStyle.BOLD)
                        .append(buffer.substring(matchedLen))
                        .toAttributedString();
            }

            // Unknown command: render in yellow
            return new AttributedString(buffer,
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        }

        @Override
        public void setErrorPattern(Pattern pattern) { /* not used */ }

        @Override
        public void setErrorIndex(int index) { /* not used */ }
    }
}
