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
package querqy.rewrite.wordbreak;

import querqy.trie.State;
import querqy.trie.States;
import querqy.trie.TrieMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

/**
 * {@link TermCorpus} implementation backed by a TSV file with lines of the form:
 * <pre>
 *   term TAB docFreq
 * </pre>
 *
 * <p>Co-occurrence information is not available; {@link #isCollationSupported()} returns
 * {@code false} and {@link #coExist} throws {@link UnsupportedOperationException}.
 * This implementation is suitable when the word-break rewriter is configured with
 * {@code verifyCollation=false}.</p>
 *
 * <p>The corpus is loaded eagerly from the supplied {@link Reader} and stored in
 * a {@link TrieMap} for O(k) lookups where k is the term length.</p>
 *
 * <p>Use {@link Builder} to create instances.</p>
 */
public class TsvDfTermCorpus implements TermCorpus {

    private final TrieMap<Integer> trie;
    private final int numDocs;

    private TsvDfTermCorpus(final TrieMap<Integer> trie, final int numDocs) {
        this.trie = trie;
        this.numDocs = numDocs;
    }

    public static Builder builder() {
        return new Builder();
    }

    private Optional<Integer> lookup(final CharSequence term) {
        final States<Integer> states = trie.get(term);
        final State<Integer> state = states.getStateForCompleteSequence();
        return state.isFinal() ? Optional.of(state.getValue()) : Optional.empty();
    }

    @Override
    public boolean isCollationSupported() {
        return false;
    }

    @Override
    public boolean exists(final CharSequence term) {
        return lookup(term).isPresent();
    }

    @Override
    public int docFreq(final CharSequence term) {
        return lookup(term).orElse(0);
    }

    @Override
    public int numDocs() {
        return numDocs;
    }

    public static class Builder {

        private Reader reader;
        private int numDocs = -1;

        private Builder() {}

        /** Source of TSV lines; closed by the caller. */
        public Builder reader(final Reader reader) {
            this.reader = reader;
            return this;
        }

        /**
         * Total number of documents in the corpus, used for decompound scoring.
         * If not set, the value is estimated as {@code numberOfTerms * 100}.
         */
        public Builder numDocs(final int numDocs) {
            if (numDocs <= 0) {
                throw new IllegalArgumentException("numDocs must be > 0, got: " + numDocs);
            }
            this.numDocs = numDocs;
            return this;
        }

        public TsvDfTermCorpus build() throws IOException {
            if (reader == null) {
                throw new IllegalStateException("reader must be set");
            }

            final TrieMap<Integer> trie = new TrieMap<>();
            final BufferedReader br = reader instanceof BufferedReader
                    ? (BufferedReader) reader
                    : new BufferedReader(reader);

            String line;
            int lineNumber = 0;
            int termCount = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                final int tab = line.indexOf('\t');
                if (tab < 0) {
                    throw new IOException("Missing TAB on line " + lineNumber + ": " + line);
                }
                final String term = line.substring(0, tab).trim();
                final int df = Integer.parseInt(line.substring(tab + 1).trim());
                if (df <= 0) {
                    throw new IOException("docFreq must be > 0 on line " + lineNumber + ": " + line);
                }
                trie.put(term, df);
                termCount++;
            }

            final int resolvedNumDocs = numDocs >= 0 ? numDocs : termCount * 100;
            return new TsvDfTermCorpus(trie, resolvedNumDocs);
        }
    }
}
