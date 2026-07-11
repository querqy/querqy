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
package querqy.rewriter.wordbreak;

/**
 * Bundles {@link OptionalModifierPosition} with the boost applied to the optional part when it matches, and
 * rejects the combination that would be meaningless: a non-neutral boost while the feature is disabled
 * ({@link OptionalModifierPosition#NONE}).
 *
 * @param position Which part of a decompounded token is optional.
 * @param boost    The boost applied to the optional part's term when it is present. Must be {@code 1.0f} if
 *                 {@code position} is {@link OptionalModifierPosition#NONE}.
 */
public record OptionalModifierConfig(OptionalModifierPosition position, float boost) {

    /** The default configuration: decompounding requires all parts to match, as before this feature existed. */
    public static final OptionalModifierConfig DISABLED = new OptionalModifierConfig(OptionalModifierPosition.NONE, 1f);

    public OptionalModifierConfig {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        if (position == OptionalModifierPosition.NONE && Float.compare(boost, 1f) != 0) {
            throw new IllegalArgumentException(
                    "optionalModifierBoost must be 1.0 when optionalModifierPosition is NONE, got " + boost);
        }
    }
}
