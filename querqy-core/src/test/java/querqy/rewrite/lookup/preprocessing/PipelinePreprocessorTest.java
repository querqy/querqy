/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2023 Querqy Contributors
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
package querqy.rewrite.lookup.preprocessing;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class PipelinePreprocessorTest {

    @Mock
    LookupPreprocessor preprocessor1;
    @Mock
    LookupPreprocessor preprocessor2;

    @Test
    public void testThat_preprocessorsAreAppliedInOrder_forTwoGivenPreprocessors() {
        when(preprocessor1.process("a")).thenReturn("b");
        when(preprocessor2.process("b")).thenReturn("c");

        final LookupPreprocessor pipeline = PipelinePreprocessor.of(preprocessor1, preprocessor2);
        assertThat(pipeline.process("a")).isEqualTo("c");
    }
}
