/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 Querqy Contributors
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
package querqy.rewrite.logging;

public class MatchLog {

    private final String term;
    private final String type;

    private MatchLog(final String term, final String type) {
        this.term = term;
        this.type = type;
    }

    public String getTerm() {
        return term;
    }

    public String getType() {
        return type;
    }

    public enum MatchType {
        EXACT("exact"), AFFIX("affix"), REGEX("regex");

        private final String typeName;

        MatchType(final String typeName) {
            this.typeName = typeName;
        }

        public String getTypeName() {
            return typeName;
        }
    }

    public static MatchLogBuilder builder() {
        return new MatchLogBuilder();
    }

    public static class MatchLogBuilder {

        private String term;
        private MatchType type;

        public MatchLogBuilder term(final String term) {
            this.term = term;
            return this;
        }

        public MatchLogBuilder type(final MatchType type) {
            this.type = type;
            return this;
        }

        public MatchLog build() {
            return new MatchLog(term, type.getTypeName());
        }
    }

}
