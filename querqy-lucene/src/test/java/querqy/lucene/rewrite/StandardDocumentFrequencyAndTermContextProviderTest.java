package querqy.lucene.rewrite;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import static querqy.lucene.rewrite.TestUtil.addNumDocs;
import static querqy.lucene.rewrite.TestUtil.newTerm;

/**
 * Created by rene on 14/09/2016.
 */
public class StandardDocumentFrequencyAndTermContextProviderTest extends LuceneTestCase {


    @Test
    public void testThatTheTrueDFIsReturned() throws Exception {
        ConstantFieldBoost fieldBoost = new ConstantFieldBoost(1f);

        Analyzer analyzer = new KeywordAnalyzer();

        Directory directory = newDirectory();
        RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocs("f1", "v1", indexWriter, 1);
        addNumDocs("f1", "v5", indexWriter, 5);

        indexWriter.close();


        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = newSearcher(indexReader);

        StandardDocumentFrequencyAndTermContextProvider provider = new StandardDocumentFrequencyAndTermContextProvider();
        ;

        int idx1 = new DependentTermQuery(newTerm("f1", "v5", provider), provider, fieldBoost).tqIndex;
        int idx2 = new DependentTermQuery(newTerm("f1", "v1", provider), provider, fieldBoost).tqIndex;
        DocumentFrequencyAndTermContextProvider.DocumentFrequencyAndTermContext context1
                = provider.getDocumentFrequencyAndTermContext(idx1, indexSearcher);
        DocumentFrequencyAndTermContextProvider.DocumentFrequencyAndTermContext context2
                = provider.getDocumentFrequencyAndTermContext(idx2, indexSearcher);

        assertEquals(5, context1.df);
        assertEquals(1, context2.df);
        assertEquals(5, context1.termContext.docFreq());
        assertEquals(1, context2.termContext.docFreq());

        indexReader.close();
        directory.close();
        analyzer.close();

    }

}
