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
package querqy.model;

import querqy.rewrite.RewriteLoggingConfig;
import querqy.rewrite.RewriteChain;
import querqy.rewrite.SearchEngineRequestAdapter;

import java.util.*;

public class EmptySearchEngineRequestAdapter implements SearchEngineRequestAdapter {

    Map<String, Object> context = new HashMap<>();

    @Override
    public RewriteChain getRewriteChain() {
        return null;
    }

    @Override
    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public Optional<String> getRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public String[] getRequestParams(String name) {
        return new String[0];
    }

    @Override
    public Optional<Boolean> getBooleanRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getIntegerRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<Float> getFloatRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getDoubleRequestParam(String name) {
        return Optional.empty();
    }

    @Override
    public boolean isDebugQuery() {
        return false;
    }

    @Override
    public RewriteLoggingConfig getRewriteLoggingConfig() {
        return RewriteLoggingConfig.off();
    }

}