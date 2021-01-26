package querqy.model.convert;

public class QueryBuilderException extends RuntimeException {

    public QueryBuilderException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public QueryBuilderException(final String message) {
        super(message);
    }
}
