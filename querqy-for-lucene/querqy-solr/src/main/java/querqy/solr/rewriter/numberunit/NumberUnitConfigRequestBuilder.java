package querqy.solr.rewriter.numberunit;

import querqy.solr.RewriterConfigRequestBuilder;
import querqy.solr.utils.JsonUtil;

import java.util.HashMap;
import java.util.Map;

public class NumberUnitConfigRequestBuilder extends RewriterConfigRequestBuilder {

    private NumberUnitConfigObject numberUnitConfig;

    public NumberUnitConfigRequestBuilder() {
        super(NumberUnitRewriterFactory.class);
    }

    @Override
    public Map<String, Object> buildConfig() {

        if (numberUnitConfig == null) {
            throw new RuntimeException(NumberUnitRewriterFactory.CONF_PROPERTY + " must not be null!");
        }

        final Map<String, Object> config = new HashMap<>();
        config.put(NumberUnitRewriterFactory.CONF_PROPERTY, JsonUtil.toJson(numberUnitConfig));

        return config;
    }

    public NumberUnitConfigRequestBuilder numberUnitConfig(final NumberUnitConfigObject numberUnitConfig) {
        if (numberUnitConfig == null) {
            throw new IllegalArgumentException("numberUnitConfig must not be null");
        }
        this.numberUnitConfig = numberUnitConfig;
        return this;
    }
}
