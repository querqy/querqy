/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.BytesRef;

import querqy.lucene.rewrite.cache.CacheKey;
import querqy.lucene.rewrite.cache.TermQueryCache;
import querqy.lucene.rewrite.cache.TermQueryCacheValue;
import querqy.lucene.rewrite.prms.PRMSAndQuery;
import querqy.lucene.rewrite.prms.PRMSDisjunctionMaxQuery;
import querqy.lucene.rewrite.prms.PRMSQuery;
import querqy.lucene.rewrite.prms.PRMSTermQuery;
import querqy.model.Term;
import querqy.rewrite.commonrules.model.PositionSequence;


/**
 * @author rene
 *
 */
public class TermSubQueryBuilder {
    
    private final TermQueryCache termQueryCache;
    private final Analyzer analyzer;

    public TermSubQueryBuilder(final Analyzer analyzer, final TermQueryCache termQueryCache) {
        this.termQueryCache = termQueryCache;
        this.analyzer = analyzer;
    }
    
    public TermSubQueryFactory termToFactory(final String fieldname, final Term sourceTerm, final FieldBoost boost)
            throws IOException {
        
        final CacheKey cacheKey;

        if (termQueryCache != null) {
            
            cacheKey = new CacheKey(fieldname, sourceTerm);
           
            TermQueryCacheValue cacheValue = termQueryCache.get(cacheKey);
            if (cacheValue != null) {
                // The cache references factories with pre-analyzed terms, or cache entries without a
                // query factory if the term does not exist in the index. cacheValue.hasQuery() returns
                // true/false correspondingly.
                // Cache entries don't have a boost factor, it is only added later via the queryFactory.
                return (cacheValue.hasQuery())
                        ? new TermSubQueryFactory(cacheValue, boost, sourceTerm, fieldname)
                        : null;
            } 
            
        } else {
            cacheKey = null;
        }
        
        final LuceneQueryFactoryAndPRMSQuery root;
        TokenStream ts = null;
        try {
           ts = analyzer.tokenStream(fieldname, new CharSequenceReader(sourceTerm));
           final CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
           final PositionIncrementAttribute posIncAttr = ts.addAttribute(PositionIncrementAttribute.class);
           ts.reset();
         
           final PositionSequence<org.apache.lucene.index.Term> sequence = new PositionSequence<>();
           while (ts.incrementToken()) {
              
               final int inc = posIncAttr.getPositionIncrement();
               if (inc > 0 || sequence.isEmpty()) {
                   sequence.nextPosition();
               }

               sequence.addElement(new org.apache.lucene.index.Term(fieldname, new BytesRef(termAttr)));
           }
           
           root = positionSequenceToQueryFactoryAndPRMS(sequence, sourceTerm);

        } finally {
           if (ts != null) {
               try {
                   ts.close();
               } catch (IOException e) {
               }
           }
        }

        putQueryFactoryAndPRMSQueryIntoCache(cacheKey, root);
        
        return root == null ? null : new TermSubQueryFactory(root, boost, sourceTerm, fieldname);
    }

    protected void putQueryFactoryAndPRMSQueryIntoCache(final CacheKey cacheKey, final LuceneQueryFactoryAndPRMSQuery value) {
        if (cacheKey != null && value != null && termQueryCache != null) {
            termQueryCache.put(cacheKey, new TermQueryCacheValue(value));
        }
    }
    
    public LuceneQueryFactoryAndPRMSQuery positionSequenceToQueryFactoryAndPRMS(
            final PositionSequence<org.apache.lucene.index.Term> sequence, final Term sourceTerm) {
        switch (sequence.size()) {
        case 0: return null;
        case 1: 
            final List<org.apache.lucene.index.Term> first = sequence.getFirst();
            return first.isEmpty() ? null: newPosition(sequence.iterator(), null, null, sourceTerm);
            
        default:
            
            return newPosition(sequence.iterator(), null, null, sourceTerm);
            
        }

    }
    
    protected LuceneQueryFactoryAndPRMSQuery newPosition(final Iterator<List<org.apache.lucene.index.Term>> seqIterator,
            final BooleanQueryFactory incomingBq, final List<PRMSQuery> incomingPrmsClauses, final Term sourceTerm) {
        
        final List<org.apache.lucene.index.Term> position = seqIterator.next();
        
        switch (position.size()) {
        case 0: throw new IllegalArgumentException("Sequence must not contain an empty position");
        case 1: {
            
            final org.apache.lucene.index.Term term = position.get(0);
            
            final TermQueryFactory tqf = createTermQueryFactory(term, sourceTerm);
            final PRMSTermQuery prmsTermQuery = new PRMSTermQuery(term);
            
            if (incomingBq != null) {
                
                incomingBq.add(tqf, Occur.MUST);
                incomingPrmsClauses.add(prmsTermQuery);
                if (seqIterator.hasNext()) {
                    newPosition(seqIterator, incomingBq, incomingPrmsClauses, sourceTerm);
                }
                
                return null; // we are not the first position
                
            } else {
                
                if (seqIterator.hasNext()) {
                    final BooleanQueryFactory bq = new BooleanQueryFactory(true);
                    final List<PRMSQuery> prmsClauses = new LinkedList<>();
                    bq.add(tqf, Occur.MUST);
                    prmsClauses.add(prmsTermQuery);
                    newPosition(seqIterator, bq, prmsClauses, sourceTerm);
                    return new LuceneQueryFactoryAndPRMSQuery(bq, new PRMSAndQuery(prmsClauses));
                } else {
                    return new LuceneQueryFactoryAndPRMSQuery(tqf, prmsTermQuery);
                }
                
            }
        }
        default:
            final boolean hasNextPosition = seqIterator.hasNext();
            // the dmq for this position
            final DisjunctionMaxQueryFactory dmq = new DisjunctionMaxQueryFactory(0f);
            final List<PRMSQuery> prmsClauses = new LinkedList<>();
            
            if (!hasNextPosition) {
                
                for (final org.apache.lucene.index.Term term: position) {
                    dmq.add(createTermQueryFactory(term, sourceTerm));
                    prmsClauses.add(new PRMSTermQuery(term));
                }
                
            } else {
                final Iterator<org.apache.lucene.index.Term> posIterator = position.iterator();
                while (posIterator.hasNext()) {
                    
                    final org.apache.lucene.index.Term term = posIterator.next();
                    final TermQueryFactory tqf = createTermQueryFactory(term, sourceTerm);
                    final PRMSTermQuery prmsTermQuery = new PRMSTermQuery(term);
                    
                    if (posIterator.hasNext()) {
                        
                        dmq.add(tqf);
                        prmsClauses.add(prmsTermQuery);
                        
                    } else {
                        final BooleanQueryFactory bq = new BooleanQueryFactory(true);
                        final List<PRMSQuery> bqPrmsClauses = new LinkedList<>();
                        bq.add(tqf, Occur.MUST);
                        bqPrmsClauses.add(prmsTermQuery);
                        newPosition(seqIterator, bq, bqPrmsClauses, sourceTerm);
                        dmq.add(bq);
                        prmsClauses.add(new PRMSAndQuery(bqPrmsClauses));
                    }
                }
            }
             
            return new LuceneQueryFactoryAndPRMSQuery(dmq, new PRMSDisjunctionMaxQuery(prmsClauses));
        }
    
    }
    
    protected TermQueryFactory createTermQueryFactory(final org.apache.lucene.index.Term term, final Term sourceTerm) {
        return new TermQueryFactory(term, sourceTerm);
    }

    // Copied from org.apache.commons.io.input.CharSequenceReader (we don't need the dependency otherwise)
    private static class CharSequenceReader extends Reader {

        private final static int EOF = -1;
        private final CharSequence charSequence;
        private int idx;
        private int mark;

        /**
         * The start index in the character sequence, inclusive.
         * <p>
         * When de-serializing a CharSequenceReader that was serialized before
         * this fields was added, this field will be initialized to 0, which
         * gives the same behavior as before: start reading from the start.
         * </p>
         *
         * @see #start()
         */
        private final int start;

        /**
         * The end index in the character sequence, exclusive.
         * <p>
         * When de-serializing a CharSequenceReader that was serialized before
         * this fields was added, this field will be initialized to {@code null},
         * which gives the same behavior as before: stop reading at the
         * CharSequence's length.
         * If this field was an int instead, it would be initialized to 0 when the
         * CharSequenceReader is de-serialized, causing it to not return any
         * characters at all.
         * </p>
         *
         * @see #end()
         */
        private final Integer end;

        /**
         * Constructs a new instance with the specified character sequence.
         *
         * @param charSequence The character sequence, may be {@code null}
         */
        public CharSequenceReader(final CharSequence charSequence) {
            this(charSequence, 0);
        }

        /**
         * Constructs a new instance with a portion of the specified character sequence.
         * <p>
         * The start index is not strictly enforced to be within the bounds of the
         * character sequence. This allows the character sequence to grow or shrink
         * in size without risking any {@link IndexOutOfBoundsException} to be thrown.
         * Instead, if the character sequence grows smaller than the start index, this
         * instance will act as if all characters have been read.
         * </p>
         *
         * @param charSequence The character sequence, may be {@code null}
         * @param start        The start index in the character sequence, inclusive
         * @throws IllegalArgumentException if the start index is negative
         */
        public CharSequenceReader(final CharSequence charSequence, final int start) {
            this(charSequence, start, Integer.MAX_VALUE);
        }

        /**
         * Constructs a new instance with a portion of the specified character sequence.
         * <p>
         * The start and end indexes are not strictly enforced to be within the bounds
         * of the character sequence. This allows the character sequence to grow or shrink
         * in size without risking any {@link IndexOutOfBoundsException} to be thrown.
         * Instead, if the character sequence grows smaller than the start index, this
         * instance will act as if all characters have been read; if the character sequence
         * grows smaller than the end, this instance will use the actual character sequence
         * length.
         * </p>
         *
         * @param charSequence The character sequence, may be {@code null}
         * @param start        The start index in the character sequence, inclusive
         * @param end          The end index in the character sequence, exclusive
         * @throws IllegalArgumentException if the start index is negative, or if the end index is smaller than the start index
         */
        public CharSequenceReader(final CharSequence charSequence, final int start, final int end) {
            if (start < 0) {
                throw new IllegalArgumentException("Start index is less than zero: " + start);
            }
            if (end < start) {
                throw new IllegalArgumentException("End index is less than start " + start + ": " + end);
            }
            // Don't check the start and end indexes against the CharSequence,
            // to let it grow and shrink without breaking existing behavior.

            this.charSequence = charSequence != null ? charSequence : "";
            this.start = start;
            this.end = end;

            this.idx = start;
            this.mark = start;
        }

        /**
         * Close resets the file back to the start and removes any marked position.
         */
        @Override
        public void close() {
            idx = start;
            mark = start;
        }

        /**
         * Returns the index in the character sequence to end reading at, taking into account its length.
         *
         * @return The end index in the character sequence (exclusive).
         */
        private int end() {
            /*
             * end == null for de-serialized instances that were serialized before start and end were added.
             * Use Integer.MAX_VALUE to get the same behavior as before - use the entire CharSequence.
             */
            return Math.min(charSequence.length(), end == null ? Integer.MAX_VALUE : end);
        }

        /**
         * Mark the current position.
         *
         * @param readAheadLimit ignored
         */
        @Override
        public void mark(final int readAheadLimit) {
            mark = idx;
        }

        /**
         * Mark is supported (returns true).
         *
         * @return {@code true}
         */
        @Override
        public boolean markSupported() {
            return true;
        }

        /**
         * Read a single character.
         *
         * @return the next character from the character sequence
         * or -1 if the end has been reached.
         */
        @Override
        public int read() {
            if (idx >= end()) {
                return EOF;
            }
            return charSequence.charAt(idx++);
        }

        /**
         * Read the specified number of characters into the array.
         *
         * @param array  The array to store the characters in
         * @param offset The starting position in the array to store
         * @param length The maximum number of characters to read
         * @return The number of characters read or -1 if there are
         * no more
         */
        @Override
        public int read(final char[] array, final int offset, final int length) {
            if (idx >= end()) {
                return EOF;
            }
            Objects.requireNonNull(array, "array");
            if (length < 0 || offset < 0 || offset + length > array.length) {
                throw new IndexOutOfBoundsException("Array Size=" + array.length +
                        ", offset=" + offset + ", length=" + length);
            }

            if (charSequence instanceof String) {
                final int count = Math.min(length, end() - idx);
                ((String) charSequence).getChars(idx, idx + count, array, offset);
                idx += count;
                return count;
            }
            if (charSequence instanceof StringBuilder) {
                final int count = Math.min(length, end() - idx);
                ((StringBuilder) charSequence).getChars(idx, idx + count, array, offset);
                idx += count;
                return count;
            }
            if (charSequence instanceof StringBuffer) {
                final int count = Math.min(length, end() - idx);
                ((StringBuffer) charSequence).getChars(idx, idx + count, array, offset);
                idx += count;
                return count;
            }

            int count = 0;
            for (int i = 0; i < length; i++) {
                final int c = read();
                if (c == EOF) {
                    return count;
                }
                array[offset + i] = (char) c;
                count++;
            }
            return count;
        }

        /**
         * Tells whether this stream is ready to be read.
         *
         * @return {@code true} if more characters from the character sequence are available, or {@code false} otherwise.
         */
        @Override
        public boolean ready() {
            return idx < end();
        }

        /**
         * Reset the reader to the last marked position (or the beginning if
         * mark has not been called).
         */
        @Override
        public void reset() {
            idx = mark;
        }

        /**
         * Skip the specified number of characters.
         *
         * @param n The number of characters to skip
         * @return The actual number of characters skipped
         */
        @Override
        public long skip(final long n) {
            if (n < 0) {
                throw new IllegalArgumentException("Number of characters to skip is less than zero: " + n);
            }
            if (idx >= end()) {
                return 0;
            }
            final int dest = (int) Math.min(end(), idx + n);
            final int count = dest - idx;
            idx = dest;
            return count;
        }

        /**
         * Returns the index in the character sequence to start reading from, taking into account its length.
         *
         * @return The start index in the character sequence (inclusive).
         */
        private int start() {
            return Math.min(charSequence.length(), start);
        }

        /**
         * Return a String representation of the underlying
         * character sequence.
         *
         * @return The contents of the character sequence
         */
        @Override
        public String toString() {
            final CharSequence subSequence = charSequence.subSequence(start(), end());
            return subSequence.toString();
        }
    }

}
