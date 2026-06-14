/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020 Querqy Contributors
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
package querqy.rewrite.contrib.replace;

import querqy.CompoundCharSequence;
import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.MatchLog;

import java.util.LinkedList;
import java.util.List;

public class WildcardReplaceInstruction extends ReplaceInstruction {

    @FunctionalInterface
    public interface TermCreator {
        CharSequence createTerm(final CharSequence wildcardMatch);
    }

    private final List<TermCreator> termCreators = new LinkedList<>();

    public WildcardReplaceInstruction(final List<? extends CharSequence> replacementTerms) {

        replacementTerms.stream()
                .map(CharSequence::toString)
                .forEach(replacementTerm -> {
                    final int indexWildcardReplacement = replacementTerm.indexOf("$1");
                    if (indexWildcardReplacement < 0) {
                        termCreators.add(0, wildcardMatch -> replacementTerm);

                    } else {

                        final String leftPart = replacementTerm.substring(0, indexWildcardReplacement);
                        final String rightPart = replacementTerm.substring(indexWildcardReplacement + 2);

                        termCreators.add(0, wildcardMatch ->
                                new CompoundCharSequence(null, leftPart, wildcardMatch, rightPart));
                    }
                });
    }

    @Override
    public void apply(final List<CharSequence> seq,
                      final int start,
                      final int exclusiveOffset,
                      final CharSequence wildcardMatch,
                      final List<ActionLog> actionLogs) {
        removeTermFromSequence(seq, start, exclusiveOffset, seq, actionLogs, MatchLog.MatchType.AFFIX);
        termCreators.forEach(termCreator -> seq.add(start, termCreator.createTerm(wildcardMatch)));
    }
}
