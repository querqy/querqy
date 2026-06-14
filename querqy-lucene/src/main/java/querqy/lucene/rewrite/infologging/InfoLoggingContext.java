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
 * Per-request access to InfoLogging
 */
public class InfoLoggingContext {

    private final InfoLogging infoLogging;
    private final SearchEngineRequestAdapter searchEngineRequestAdapter;

    private String rewriterId = null;

    public InfoLoggingContext(final InfoLogging infoLogging,
                              final SearchEngineRequestAdapter searchEngineRequestAdapter) {
        this.infoLogging = infoLogging;
        this.searchEngineRequestAdapter = searchEngineRequestAdapter;
    }

    public String getRewriterId() {
        return rewriterId;
    }

    public void setRewriterId(final String rewriterId) {
        this.rewriterId = rewriterId;
    }

    public void log(final Object message) {
        infoLogging.log(message, rewriterId, searchEngineRequestAdapter);
    }

    public void endOfRequest() {
        infoLogging.endOfRequest(searchEngineRequestAdapter);
    }

    public boolean isEnabledForRewriter() {
        return infoLogging.isLoggingEnabledForRewriter(rewriterId);
    }



}
