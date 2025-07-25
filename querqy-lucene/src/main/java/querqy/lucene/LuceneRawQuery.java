package querqy.lucene;

import org.apache.lucene.search.Query;
import querqy.model.BooleanParent;
import querqy.model.ParsedRawQuery;
import querqy.model.QuerqyQuery;

public class LuceneRawQuery extends ParsedRawQuery<Query> {
    public LuceneRawQuery(final BooleanParent parent, final Occur occur, final boolean isGenerated, final Query query) {
        super(parent, occur, isGenerated, query);
    }

    @Override
    public QuerqyQuery<BooleanParent> clone(final BooleanParent newParent) {
        return new LuceneRawQuery(newParent, occur, isGenerated(), query);
    }

    @Override
    public QuerqyQuery<BooleanParent> clone(final BooleanParent newParent, final boolean generated) {
        return new LuceneRawQuery(newParent, occur, generated, query);
    }
}
