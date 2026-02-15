package querqy.rewrite.replace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RegexReplaceRewriterRulesParser {

    protected static final String LINE_FORMAT_ERROR = "Invalid line format. Required: <regex> => <replacement>";
    protected static final String OPERATOR = "=>";

    private final InputStreamReader inputStreamReader;
    private final boolean ignoreCase;


    public RegexReplaceRewriterRulesParser(final InputStreamReader inputStreamReader, final boolean ignoreCase) {
        this.inputStreamReader = inputStreamReader;
        this.ignoreCase = ignoreCase;
    }

    public RegexReplacing parserConfig() throws IOException {

        final RegexReplacing replacing = new RegexReplacing(ignoreCase, null); // FIXME: inject ActionsLog

        try (final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {

                line = line.trim();
                int commentPos = line.indexOf("#");
                if (line.isEmpty() || (commentPos == 0)) {
                    continue;
                }

                if (commentPos > -1) {
                    line = line.substring(0, commentPos).trim();
                }

                final String[] parts = line.split(OPERATOR);
                throwIfTrue((parts.length != 2), LINE_FORMAT_ERROR, line);

                final String pattern = parts[0].trim();
                throwIfTrue(pattern.isEmpty(), LINE_FORMAT_ERROR, line);

                replacing.put(pattern, parts[1].trim());

            }

        }

        return replacing;

    }


    private static void throwIfTrue(final boolean bool, final String message, final String line) throws IOException {
        if (bool) {
            throw new IOException(message + " Found: " + line);
        }
    }
}
