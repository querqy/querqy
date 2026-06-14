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
package querqy.lucene.contrib.rewrite.wordbreak;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class MorphologyProvider {
    private static final Map<String, Optional<Morphology>> morphologies = new HashMap<>();
    private static final String DEFAULT_KEY = "default";
    public static final SuffixGroupMorphology DEFAULT = new SuffixGroupMorphology(weight -> new SuffixGroup(null, singletonList(new WordGeneratorAndWeight(NoopWordGenerator.INSTANCE, 1f))));

    static {
        morphologies.put(DEFAULT_KEY, Optional.of(DEFAULT));
        morphologies.put("german", Optional.of(new SuffixGroupMorphology(GermanDecompoundingMorphology::createDecompoundingMorphemes, GermanDecompoundingMorphology::createCompoundingMorphemes)));
    }

    public Optional<Morphology> get(final String name) {
        final String normName = name == null ? "" : name.toLowerCase();
        if (!exists(normName)) {
            throw new IllegalArgumentException(String.format("No such morphology %s", normName));
        }
        return morphologies.get(normName);
    }

    public boolean exists(final String name) {
        final String normName = name == null ? "" : name.toLowerCase();
        return morphologies.containsKey(normName);
    }
}
