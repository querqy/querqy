package querqy.lucene.rewrite.infologging;

import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MultiSinkInfoLogging implements InfoLogging {

    private final Map<String, List<Sink>> mappings;
    private final Set<Sink> allSinks;

    public MultiSinkInfoLogging(final Map<String, List<Sink>> mappings) {
        this.mappings = mappings;
        allSinks = mappings.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
    }


    @Override
    public void log(final Object message, final String rewriterId,
                    final SearchEngineRequestAdapter searchEngineRequestAdapter) {

        final List<Sink> sinks = mappings.get(rewriterId);
        if (sinks != null) {
            for (final Sink sink : sinks) {
                sink.log(message, rewriterId, searchEngineRequestAdapter);
            }
        }

    }

    @Override
    public void endOfRequest(final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        allSinks.forEach(sink -> sink.endOfRequest(searchEngineRequestAdapter));
    }

    @Override
    public boolean isLoggingEnabledForRewriter(final String rewriterId) {
        final List<Sink> sinks = mappings.get(rewriterId);
        return sinks != null && !sinks.isEmpty();
    }


}
