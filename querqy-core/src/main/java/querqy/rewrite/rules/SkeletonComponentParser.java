package querqy.rewrite.rules;

public interface SkeletonComponentParser<T> {

    void setContent(final String content);
    boolean isParsable();
    void parse();
    T finish();

}
