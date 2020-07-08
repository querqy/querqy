package querqy.lucene.contrib.rewrite;

import org.apache.lucene.index.IndexReader;
import querqy.model.Term;

import java.io.IOException;
import java.util.List;

public interface LuceneCompounder {

    List<CompoundTerm> combine(Term[] terms, final IndexReader indexReader, boolean reverse) throws IOException;

    class CompoundTerm {

        public final CharSequence value;
        public final Term[] originalTerms;

        public CompoundTerm(final CharSequence value, final Term[] originalTerms) {
            this.value = value;
            this.originalTerms = originalTerms;
        }

    }
}
