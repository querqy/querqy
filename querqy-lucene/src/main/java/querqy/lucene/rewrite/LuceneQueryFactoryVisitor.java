package querqy.lucene.rewrite;

public abstract class LuceneQueryFactoryVisitor<R> {

    public R visit(final BooleanQueryFactory factory) {
        factory.getClauses().forEach(clause -> clause.queryFactory.accept(this));
        return null;
    }

    public R visit(final DisjunctionMaxQueryFactory factory) {
        factory.disjuncts.forEach(disjunct -> disjunct.accept(this));
        return null;
    }

    public R visit(final TermSubQueryFactory factory) {
        return factory.root.accept(this);
    }



    public R visit(final TermQueryFactory factory) {
        return null;
    }



    public R visit(final NeverMatchQueryFactory factory) {
        return null;
    }



}
