/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
package querqy.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;
import querqy.rewrite.wordbreak.TermCorpus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

/**
 * {@link TermCorpus} implementation backed by a Lucene index field.
 * The {@code dictionaryField} and any case-normalization are encapsulated here;
 * callers pass plain {@link CharSequence} values that are already normalized.
 */
public class LuceneTermCorpus implements TermCorpus {

    private final Supplier<IndexReader> indexReaderSupplier;
    private final String dictionaryField;

    public LuceneTermCorpus(final Supplier<IndexReader> indexReaderSupplier, final String dictionaryField) {
        this.indexReaderSupplier = indexReaderSupplier;
        this.dictionaryField = dictionaryField;
    }

    private IndexReader reader() {
        return indexReaderSupplier.get();
    }

    private Term toTerm(final CharSequence value) {
        return new Term(dictionaryField, new BytesRef(value));
    }

    @Override
    public boolean isCollationSupported() {
        return true;
    }

    @Override
    public boolean exists(final CharSequence term) {
        return docFreq(term) > 0;
    }

    @Override
    public int docFreq(final CharSequence term) {
        try {
            return reader().docFreq(toTerm(term));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int numDocs() {
        return reader().numDocs();
    }

    @Override
    public boolean coExist(final CharSequence term1, final CharSequence term2) {
        final Term t1 = toTerm(term1);
        final Term t2 = toTerm(term2);
        try {
            return hasMinMatches(1, t1, t2);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean hasMinMatches(final int minCount, final Term term1, final Term term2)
            throws IOException {

        final IndexSearcher searcher = new IndexSearcher(reader());
        final IndexReaderContext topReaderContext = searcher.getTopReaderContext();
        final IndexReader indexReader = topReaderContext.reader();
        // TODO: deleted documents?
        final int numDocs = indexReader.numDocs();
        if (minCount > numDocs) {
            return false;
        }

        final int df1 = indexReader.docFreq(term1);
        if (minCount > df1) {
            return false;
        }

        final int df2 = indexReader.docFreq(term2);
        if (minCount > df2) {
            return false;
        }

        int count = 0;

        for (final LeafReaderContext context : topReaderContext.leaves()) {

            final Terms terms1 = context.reader().terms(term1.field());

            if (terms1 != null) {

                final Terms terms2 = context.reader().terms(term2.field());
                if (terms2 != null) {

                    final TermsEnum termsEnum1 = terms1.iterator();
                    if (!termsEnum1.seekExact(term1.bytes())) {
                        continue;
                    }

                    final TermsEnum termsEnum2 = terms2.iterator();
                    if (!termsEnum2.seekExact(term2.bytes())) {
                        continue;
                    }

                    final PostingsEnum postings1 = termsEnum1.postings(null, PostingsEnum.NONE);
                    final PostingsEnum postings2 = termsEnum2.postings(null, PostingsEnum.NONE);

                    int doc1 = postings1.nextDoc();
                    while (doc1 != DocIdSetIterator.NO_MORE_DOCS) {
                        int doc2 = postings2.advance(doc1);
                        if (doc2 == DocIdSetIterator.NO_MORE_DOCS) {
                            break;
                        }
                        if (doc2 == doc1) {
                            count++;
                            if (count >= minCount) {
                                return true;
                            }
                        } else if (doc2 > doc1) {
                            doc1 = postings1.advance(doc2);
                            if (doc2 == doc1) {
                                count++;
                                if (count >= minCount) {
                                    return true;
                                }
                            }
                        }
                    }

                }

            }

        }

        return false;

    }

}
