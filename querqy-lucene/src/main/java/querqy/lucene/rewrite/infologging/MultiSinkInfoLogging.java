/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Querqy Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
