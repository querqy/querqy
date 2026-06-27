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
package querqy.rewrite.replace;

import querqy.rewrite.logging.ActionLog;
import querqy.rewrite.logging.InstructionLog;
import querqy.rewrite.logging.MatchLog;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class ReplaceInstruction {

    /**
     * Applies the defined replace rules
     *
     * @param seq              List to replace the tokens in
     * @param start            Startposition of the term in the list
     * @param exclusiveOffset  Endposition of the term in the list without offset.
     * @param wildcardMatch    Wildcard match that should be used to generate a replacement
     * @param actionLogs       Debug information about replaced terms and their replacement
     */
    abstract public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                               final CharSequence wildcardMatch, List<ActionLog> actionLogs);

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset) {
        this.apply(seq, start, exclusiveOffset, "", null);
    }

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                      final CharSequence wildcardMatch) {
        this.apply(seq, start, exclusiveOffset, wildcardMatch, null);
    }

    public void apply(final List<CharSequence> seq, final int start, final int exclusiveOffset,
                      final List<ActionLog> actionLogs) {
        this.apply(seq, start, exclusiveOffset, "", actionLogs);
    }

    /**
     * Removes the term from the seq which is defined by start end exclusiveOffset.
     *
     * @param seq              List to replace the tokens in
     * @param start            Startposition of the term in the list
     * @param exclusiveOffset  Endposition of the term in the list without offset.
     * @param replacementTerms Terms that should be used as replacement
     * @param actionLogs   Debug information about replaced terms and their replacement
     * @param matchType        Information about the type of the rule match (e.g. exact or affix)
     */
    // TODO: this definitely needs to be refactored, but requires more comprehensive refactoring in the replace rewriter
    public void removeTermFromSequence(final List<CharSequence> seq, final int start,
                                       final int exclusiveOffset, List<? extends CharSequence> replacementTerms,
                                       final List<ActionLog> actionLogs,
                                       final MatchLog.MatchType matchType) {
        final List<CharSequence> removedTerms = IntStream.range(0, exclusiveOffset)
                .mapToObj(i -> seq.remove(start)).collect(Collectors.toList());

        final String removedTermsInfo = String.join(" ", removedTerms);
        final String replacementTermsInfo = String.join(" ", replacementTerms);

        if (actionLogs != null) {
            actionLogs.add(
                    ActionLog.builder()
                            .message(String.format("%s => %s", removedTermsInfo, replacementTermsInfo))
                            .match(
                                    MatchLog.builder()
                                            .type(matchType)
                                            .term(removedTermsInfo)
                                            .build()
                            )
                            .instructions(List.of(
                                    InstructionLog.builder()
                                            .type("replace")
                                            .value(replacementTermsInfo)
                                            .build()
                            ))
                            .build()
            );
        }
    }
}
