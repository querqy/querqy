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
package querqy.rewriter.commonrules;

/**
 * @author rene
 *
 */
@Deprecated
public class RuleParseException extends Exception {

   /**
     * 
     */
   private static final long serialVersionUID = 1L;

   /**
     * 
     */
   public RuleParseException() {
   }

   public RuleParseException(final String message) {
       super(message);
   }

   /**
    * @param lineNumber The line number at which the parsing error occurred.
    * @param message The error message.
    */
   public RuleParseException(final int lineNumber, final String message) {
      super("Line " + lineNumber + ": " + message);
   }

   /**
    * @param cause The root cause
    */
   public RuleParseException(final Throwable cause) {
      super(cause);
   }

   /**
    * @param message The error message
    * @param cause The root cause
    */
   public RuleParseException(final String message, final Throwable cause) {
      super(message, cause);
   }

   public RuleParseException(final String message, final Throwable cause, final boolean enableSuppression,
                             final boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

}
