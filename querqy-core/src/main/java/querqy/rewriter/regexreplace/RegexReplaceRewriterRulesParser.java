/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package querqy.rewriter.regexreplace;

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
