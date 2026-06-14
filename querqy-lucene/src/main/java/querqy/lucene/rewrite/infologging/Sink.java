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

/**
 * Info logging messages can be written to various types of Sinks.
 */
public interface Sink {

    /**
     * <p>Log a message. If the implementation wants to cache the message during the request, it should use the
     * request context via {@link SearchEngineRequestAdapter#getContext()} for caching and flush the output when
     * {@link #endOfRequest(SearchEngineRequestAdapter)} is called.</p>
     * <p>Sink objects can be shared across requests.</p>
     *
     * @param message The message to be logged
     * @param rewriterId The rewriter for which the event should be logged
     * @param searchEngineRequestAdapter Provides the context for collecting the log output
     */
    void log(Object message, String rewriterId, SearchEngineRequestAdapter searchEngineRequestAdapter);

    /**
     * <p>Signals the end of a request. Messages that were cached via the
     * {@link SearchEngineRequestAdapter#getContext()} should now be flushed.</p>
     *
     * @param searchEngineRequestAdapter Provides the context for collecting the log output
     */
    void endOfRequest(SearchEngineRequestAdapter searchEngineRequestAdapter);


}
