package querqy.rewrite.experimental;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import querqy.model.builder.impl.ExpandedQueryBuilder;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;

@Setter
@RequiredArgsConstructor
public class RewrittenQuery {

    private final ExpandedQueryBuilder expandedQueryBuilder;

    private Set<Object> decorations;
    private Map<String, Object> namedDecorations;

    public ExpandedQueryBuilder getQuery() {
        return expandedQueryBuilder;
    }

    public Set<Object> getDecorations() {
        return isNull(decorations) ? Collections.emptySet() : decorations;
    }

    public Map<String, Object> getNamedDecorations() {
        return isNull(namedDecorations) ? Collections.emptyMap() : namedDecorations;
    }

}
