/**
 * 
 */
package querqy.lucene.rewrite;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.input.CharSequenceReader;
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
                return (cacheValue.hasQuery()) ? new TermSubQueryFactory(cacheValue, boost) : null;
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
           
           root = positionSequenceToQueryFactoryAndPRMS(sequence);

        } finally {
           if (ts != null) {
               try {
                   ts.close();
               } catch (IOException e) {
               }
           }
        }

        putQueryFactoryAndPRMSQueryIntoCache(cacheKey, root);
        
        return root == null ? null : new TermSubQueryFactory(root, boost);
    }

    protected void putQueryFactoryAndPRMSQueryIntoCache(final CacheKey cacheKey, final LuceneQueryFactoryAndPRMSQuery value) {
        if (value != null && cacheKey != null && termQueryCache != null) {
            termQueryCache.put(cacheKey, new TermQueryCacheValue(value));
        }
    }
    
    public LuceneQueryFactoryAndPRMSQuery positionSequenceToQueryFactoryAndPRMS(PositionSequence<org.apache.lucene.index.Term> sequence) {
        switch (sequence.size()) {
        case 0: return null;
        case 1: 
            final List<org.apache.lucene.index.Term> first = sequence.getFirst();
            return first.isEmpty() ? null: newPosition(sequence.iterator(), null, null);
            
        default:
            
            return newPosition(sequence.iterator(), null, null);
            
        }

    }
    
    protected LuceneQueryFactoryAndPRMSQuery newPosition(final Iterator<List<org.apache.lucene.index.Term>> seqIterator,
            final BooleanQueryFactory incomingBq, final List<PRMSQuery> incomingPrmsClauses) {
        
        final List<org.apache.lucene.index.Term> position = seqIterator.next();
        
        switch (position.size()) {
        case 0: throw new IllegalArgumentException("Sequence must not contain an empty position");
        case 1: {
            
            final org.apache.lucene.index.Term term = position.get(0);
            
            final TermQueryFactory tqf = createTermQueryFactory(term);
            final PRMSTermQuery prmsTermQuery = new PRMSTermQuery(term);
            
            if (incomingBq != null) {
                
                incomingBq.add(tqf, Occur.MUST);
                incomingPrmsClauses.add(prmsTermQuery);
                if (seqIterator.hasNext()) {
                    newPosition(seqIterator, incomingBq, incomingPrmsClauses);
                }
                
                return null; // we are not the first position
                
            } else {
                
                if (seqIterator.hasNext()) {
                    final BooleanQueryFactory bq = new BooleanQueryFactory(true);
                    final List<PRMSQuery> prmsClauses = new LinkedList<>();
                    bq.add(tqf, Occur.MUST);
                    prmsClauses.add(prmsTermQuery);
                    newPosition(seqIterator, bq, prmsClauses);
                    return new LuceneQueryFactoryAndPRMSQuery(bq, new PRMSAndQuery(prmsClauses));
                } else {
                    return new LuceneQueryFactoryAndPRMSQuery(tqf, prmsTermQuery);
                }
                
            }
        }
        default:
            final boolean hasNextPosition = seqIterator.hasNext();
            // the dmq for this position
            final DisjunctionMaxQueryFactory dmq = new DisjunctionMaxQueryFactory();
            final List<PRMSQuery> prmsClauses = new LinkedList<>();
            
            if (!hasNextPosition) {
                
                for (final org.apache.lucene.index.Term term: position) {
                    dmq.add(createTermQueryFactory(term));
                    prmsClauses.add(new PRMSTermQuery(term));
                }
                
            } else {
                final Iterator<org.apache.lucene.index.Term> posIterator = position.iterator();
                while (posIterator.hasNext()) {
                    
                    final org.apache.lucene.index.Term term = posIterator.next();
                    final TermQueryFactory tqf = createTermQueryFactory(term);
                    final PRMSTermQuery prmsTermQuery = new PRMSTermQuery(term);
                    
                    if (posIterator.hasNext()) {
                        
                        dmq.add(tqf);
                        prmsClauses.add(prmsTermQuery);
                        
                    } else {
                        final BooleanQueryFactory bq = new BooleanQueryFactory(true);
                        final List<PRMSQuery> bqPrmsClauses = new LinkedList<>();
                        bq.add(tqf, Occur.MUST);
                        bqPrmsClauses.add(prmsTermQuery);
                        newPosition(seqIterator, bq, bqPrmsClauses);
                        dmq.add(bq);
                        prmsClauses.add(new PRMSAndQuery(bqPrmsClauses));
                    }
                }
            }
             
            return new LuceneQueryFactoryAndPRMSQuery(dmq, new PRMSDisjunctionMaxQuery(prmsClauses));
        }
    
    }
    
    protected TermQueryFactory createTermQueryFactory(
            org.apache.lucene.index.Term term) {
        return new TermQueryFactory(term);
    }
   
}
