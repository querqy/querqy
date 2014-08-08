package querqy.rewrite.lucene;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.junit.Before;
import org.junit.Test;

import querqy.antlr.ANTLRQueryParser;
import querqy.rewrite.QueryRewriter;

public class LuceneQueryBuilderTest extends AbstractLuceneQueryTest {
    
    Analyzer keywordAnalyzer;
    Map<String, Float> searchFields;
    IndexStats dummyIndexStats = new IndexStats() {
        
        @Override
        public int df(Term term) {
            return 10;
        }
    };
    
    @Before
    public void setUp() throws Exception {
        keywordAnalyzer = new KeywordAnalyzer();
        searchFields = new HashMap<>();
        searchFields.put("f1", 1.0f);
        searchFields.put("f11", 1.0f);
        searchFields.put("f12", 1.0f);
        searchFields.put("f13", 1.0f);
        searchFields.put("f14", 1.0f);
        searchFields.put("f15", 1.0f);
        
        searchFields.put("f2", 2.0f);
        searchFields.put("f21", 2.0f);
        searchFields.put("f22", 2.0f);
        searchFields.put("f23", 2.0f);
        searchFields.put("f24", 2.0f);
        searchFields.put("f25", 2.0f);
        
        searchFields.put("f3", 3.0f);
        searchFields.put("f31", 3.0f);
        searchFields.put("f32", 3.0f);
        searchFields.put("f33", 3.0f);
        searchFields.put("f34", 3.0f);
        searchFields.put("f35", 3.0f);
       
    }
    
    Map<String, Float> fields(String...names) {
        Map<String, Float> result = new HashMap<>(names.length);
        for (String name: names) {
            Float value = searchFields.get(name);
            if (value == null) {
                throw new IllegalArgumentException("No such field: " + name);
            }
            result.put(name, value);
        }
        return result;
    }
    
    protected Query build(String input, float tie, String...names) throws IOException {
        LuceneQueryBuilder builder = new LuceneQueryBuilder(null, keywordAnalyzer, fields(names), dummyIndexStats, tie);
        
        ANTLRQueryParser parser = new ANTLRQueryParser();
        querqy.model.Query q = parser.parse(input);
        return builder.createQuery(q);
    }
    
    protected Query buildWithSynonyms(String input, float tie, String...names) throws IOException {
        LuceneQueryBuilder builder = new LuceneQueryBuilder(null, keywordAnalyzer, fields(names), dummyIndexStats, tie);
        
        ANTLRQueryParser parser = new ANTLRQueryParser();
        querqy.model.Query q = parser.parse(input);
        LuceneSynonymsRewriterFactory factory = new LuceneSynonymsRewriterFactory(
                getClass().getClassLoader().getResourceAsStream("synonyms-test.txt"));
        QueryRewriter rewriter = factory.createRewriter(null, null);
        return builder.createQuery(rewriter.rewrite(q));
        
    }

    @Test
    public void test01() throws IOException {
    	float tie = (float) Math.random();
        Query q = build("a", tie, "f1");
        assertThat(q, tq(1f, "f1", "a"));
    }
    
    @Test
    public void test02() throws IOException {
    	float tie = (float) Math.random();
        Query q = build("a", tie, "f1", "f2");
        assertThat(q, dmq(1f, tie,
                            tq(1f, "f1", "a"),
                            tq(2f, "f2", "a")
                ));
    }
    
    @Test
    public void test03() throws Exception {
    	float tie = (float) Math.random();
        Query q = build("a b", tie, "f1");
        assertThat(q, bq(1f,
                            tq(Occur.SHOULD, 1f, "f1", "a"),
                            tq(Occur.SHOULD, 1f, "f1", "b")
                ));
    }
    
    @Test
    public void test04() throws Exception {
    	float tie = (float) Math.random();
        Query q = build("a +b", tie, "f1");
        assertThat(q, bq(1f,  
                            tq(Occur.SHOULD, 1f, "f1", "a"),
                            tq(Occur.MUST, 1f, "f1", "b")
                ));
    }
    
    @Test
    public void test05() throws Exception {
    	float tie = (float) Math.random();
    	Query q = build("-a +b", tie, "f1");
        assertThat(q, bq(1f,  
                tq(Occur.MUST_NOT, 1f, "f1", "a"),
                tq(Occur.MUST, 1f, "f1", "b")
                ));
    }
    
    @Test
    public void test06() throws Exception {
    	float tie = (float) Math.random();
        Query q = build("a b", tie, "f1", "f2");
        assertThat(q, bq(1f,  
                dmq(Occur.SHOULD, 1f, tie,
                        tq(1f, "f1", "a"),
                        tq(2f, "f2", "a")
                ),
                dmq(Occur.SHOULD, 1f, tie,
                        tq(1f, "f1", "b"),
                        tq(2f, "f2", "b")
                )
                ));
    }
    
    @Test
    public void test07() throws Exception {
    	float tie = (float) Math.random();
        Query q = build("+a b", tie, "f1", "f2");
        assertThat(q, bq(1f,  
                dmq(Occur.MUST, 1f, tie,
                        tq(1f, "f1", "a"),
                        tq(2f, "f2", "a")
                        ),
                dmq(Occur.SHOULD, 1f, tie,
                        tq(1f, "f1", "b"),
                        tq(2f, "f2", "b")
                        )
                ));
    }
    
    @Test
    public void test08() throws Exception {
    	float tie = (float) Math.random();
        Query q = build("+a -b", tie, "f1", "f2");
        assertThat(q, bq(1f,  
                dmq(Occur.MUST, 1f, tie,
                        tq(1f, "f1", "a"),
                        tq(2f, "f2", "a")
                        ),
                bq(Occur.MUST_NOT, 1f, 
                        tq(Occur.SHOULD, 1f, "f1", "b"),
                        tq(Occur.SHOULD, 2f, "f2", "b")
                        )
                ));
    }

    
    @Test
    public void test09() throws Exception {
    	float tie = (float) Math.random();
        Query q = build("a -b", tie, "f1", "f2");
        assertThat(q, bq(1f,  
                dmq(Occur.SHOULD, 1f, tie, 
                        tq(1f, "f1", "a"),
                        tq(2f, "f2", "a")
                        ),
                bq(Occur.MUST_NOT, 1f, 
                        tq(Occur.SHOULD, 1f, "f1", "b"),
                        tq(Occur.SHOULD, 2f, "f2", "b")
                        )
                ));
    }
    
    @Test
    public void test10() throws Exception {
    	float tie = (float) Math.random();
        Query q = build("-a -b c", tie, "f1", "f2");
        assertThat(q, bq(1f,  
                dmq(Occur.SHOULD, 1f, tie,
                        tq(1f, "f1", "c"),
                        tq(2f, "f2", "c")
                        ),
                bq(Occur.MUST_NOT, 1f, 
                        tq(Occur.SHOULD, 1f, "f1", "a"),
                        tq(Occur.SHOULD, 2f, "f2", "a")
                        ),
                bq(Occur.MUST_NOT, 1f, 
                         tq(Occur.SHOULD, 1f, "f1", "b"),
                          tq(Occur.SHOULD, 2f, "f2", "b")
                        )
                
                
                
                ));
    }
    
    @Test
    public void test11() throws Exception {
    	float tie = (float) Math.random();
        Query q = build("f2:a", tie, "f1", "f2");
        assertThat(q, tq(2f, "f2", "a"));
    }
    
    @Test
    public void test12() throws Exception {
    	float tie = (float) Math.random();
        // query contains a field that is not contained in the search fields
        Query q = build("x2:a", tie, "f1", "f2");
        
        assertThat(q, dmq(1f, tie,
                tq(1f, "f1", "x2:a"),
                tq(2f, "f2", "x2:a")
    ));
        
        
    }
    
    @Test
    public void test13() throws Exception {
    	float tie = (float) Math.random();
        Query q = buildWithSynonyms("j", tie, "f1");
        assertThat(q, dmq(1f, tie,
                        tq(1f, "f1", "j"),
                        bq(0.5f,
                                tq(Occur.MUST, 1f, "f1", "s"),
                                tq(Occur.MUST, 1f, "f1", "t")
                        ), 
                        tq(1f, "f1", "q")
                    )
                );
    }
    
    
}
