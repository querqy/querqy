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
package querqy.rewriter.commonrules.model;

import java.util.Objects;

/**
 * An Action represents all Instructions triggered for specific input positions.
 * It references the sequence of query terms that matched the rule. If there is
 * more than one term in a single position it's possible that more than one
 * sequence matched the input. In that case a separate Action could be created
 * for the same position.
 *
 * @author rene
 *
 */
public class Action {

   final Instructions instructions;
   final TermMatches termMatches;

   @Deprecated
   public Action(final Instructions instructions, final TermMatches termMatches, final int startPosition,
                 final int endPosition) {
      this(instructions, termMatches);
   }

   public Action(final Instructions instructions, final TermMatches termMatches) {
      this.instructions = Objects.requireNonNull(instructions, "instructions must not be null");
      this.termMatches = termMatches;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Action action = (Action) o;
      return Objects.equals(instructions, action.instructions) && Objects.equals(termMatches, action.termMatches);
   }

   @Override
   public int hashCode() {
      return Objects.hash(instructions, termMatches);
   }

   @Override
   public String toString() {
      return "Action{" +
              "instructions=" + instructions +
              ", termMatches=" + termMatches +
              '}';
   }

   public Instructions getInstructions() {
      return instructions;
   }

   public TermMatches getTermMatches() {
      return termMatches;
   }

}
