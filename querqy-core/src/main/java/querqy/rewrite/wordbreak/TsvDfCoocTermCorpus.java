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

import querqy.BloomFilter;
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
 *   term TAB docFreq TAB bloomHex
 * </pre>
 * where {@code bloomHex} is a lowercase hex string encoding the bits of a
 * {@link BloomFilter} that represents co-occurrence of the term with other terms.
 *
 * <p>The corpus is loaded eagerly from the supplied {@link Reader} and stored in
 * a {@link TrieMap} for O(k) lookups where k is the term length.</p>
 *
 * <p>Use {@link Builder} to create instances.</p>
 */
public class TsvDfCoocTermCorpus implements TermCorpus {

    private record TermEntry(int docFreq, BloomFilter bloomFilter) {}

    private final TrieMap<TermEntry> trie;
    private final int numDocs;

    private TsvDfCoocTermCorpus(final TrieMap<TermEntry> trie, final int numDocs) {
        this.trie = trie;
        this.numDocs = numDocs;
    }

    public static Builder builder() {
        return new Builder();
    }

    private Optional<TermEntry> lookup(final CharSequence term) {
        final States<TermEntry> states = trie.get(term);
        final State<TermEntry> state = states.getStateForCompleteSequence();
        return state.isFinal() ? Optional.of(state.getValue()) : Optional.empty();
    }

    @Override
    public boolean exists(final CharSequence term) {
        return lookup(term).isPresent();
    }

    @Override
    public int docFreq(final CharSequence term) {
        return lookup(term).map(TermEntry::docFreq).orElse(0);
    }

    @Override
    public int numDocs() {
        return numDocs;
    }

    @Override
    public boolean isCollationSupported() {
        return true;
    }

    @Override
    public boolean coExist(final CharSequence term1, final CharSequence term2) {
        return lookup(term1).map(e -> e.bloomFilter().contains(term2)).orElse(false);
    }

    public static class Builder {

        private Reader reader;
        private int hashFunctions = -1;
        private int numDocs = -1;

        private Builder() {}

        /** Source of TSV lines; closed by the caller. */
        public Builder reader(final Reader reader) {
            this.reader = reader;
            return this;
        }

        /** Number of hash functions used when building the bloom filters. */
        public Builder hashFunctions(final int hashFunctions) {
            this.hashFunctions = hashFunctions;
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

        public TsvDfCoocTermCorpus build() throws IOException {
            if (reader == null) {
                throw new IllegalStateException("reader must be set");
            }
            if (hashFunctions < 1) {
                throw new IllegalStateException("hashFunctions must be set to a value >= 1");
            }

            final TrieMap<TermEntry> trie = new TrieMap<>();
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
                final int tab1 = line.indexOf('\t');
                if (tab1 < 0) {
                    throw new IOException("Missing first TAB on line " + lineNumber + ": " + line);
                }
                final int tab2 = line.indexOf('\t', tab1 + 1);
                if (tab2 < 0) {
                    throw new IOException("Missing second TAB on line " + lineNumber + ": " + line);
                }
                final String term = line.substring(0, tab1).trim();
                final int df = Integer.parseInt(line.substring(tab1 + 1, tab2).trim());
                if (df <= 0) {
                    throw new IOException("docFreq must be > 0 on line " + lineNumber + ": " + line);
                }
                final String hex = line.substring(tab2 + 1).trim();
                trie.put(term, new TermEntry(df, BloomFilter.fromHex(hex, hashFunctions)));
                termCount++;
            }

            final int resolvedNumDocs = numDocs >= 0 ? numDocs : termCount * 100;
            return new TsvDfCoocTermCorpus(trie, resolvedNumDocs);
        }
    }
}
