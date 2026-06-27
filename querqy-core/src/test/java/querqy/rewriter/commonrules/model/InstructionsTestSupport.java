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
package querqy.rewriter.commonrules.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface InstructionsTestSupport {

    static Instructions instructions(final int ord, final String name, final Object value) {
        final Map<String, Object> props = new HashMap<>(1);
        props.put(name, value);
        return new Instructions(ord, Integer.toString(ord), Collections.emptyList(), new InstructionsProperties(props));
    }

    static Instructions instructions(final int ord, final Collection<Instruction> instructions) {
        return new Instructions(ord, Integer.toString(ord), instructions,
                new InstructionsProperties(Collections.emptyMap()));
    }

    static Instructions instructions(final int ord) {
        return new Instructions(ord, Integer.toString(ord), Collections.emptyList(),
                new InstructionsProperties(Collections.emptyMap()));
    }

}
