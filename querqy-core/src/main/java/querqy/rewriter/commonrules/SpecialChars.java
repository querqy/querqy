/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2026 Querqy Contributors
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
package querqy.rewriter.commonrules;

/**
 * Characters that have special syntactic meaning in the Common Rules format (rule input patterns and rule
 * file comments), and can therefore be escaped - see {@link EscapeUtil}.
 */
public final class SpecialChars {

    public static final char ESCAPE = '\\';
    public static final char BOUNDARY = '"';
    public static final char WILDCARD = '*';
    public static final char COMMENT_START = '#';

    private SpecialChars() {
    }

}
