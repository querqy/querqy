/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2014 Querqy Contributors
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
package querqy.rewrite;

/**
 * Thrown when a rule (its input, an instruction, a property, or a boolean expression) cannot be parsed. Used
 * across rewriters that parse rules from text (e.g. the Common Rules and Replace rewriters), not tied to any
 * one of them - see also {@link TemplateParseException}, which lives at this same level for the same reason.
 */
public class RuleParseException extends RuntimeException {

    public RuleParseException(final String message) {
        super(message);
    }

    public RuleParseException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
