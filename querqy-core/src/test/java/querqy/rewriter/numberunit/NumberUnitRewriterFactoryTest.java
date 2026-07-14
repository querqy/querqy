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
package querqy.rewriter.numberunit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class NumberUnitRewriterFactoryTest {

    private static final String MINIMAL_CONFIG =
            "{ \"numberUnitDefinitions\": [ { \"units\": [ { \"term\": \"cm\" } ], " +
            "\"fields\": [ { \"fieldName\": \"weight\" } ] } ] }";

    @Mock
    private NumberUnitQueryCreator numberUnitQueryCreator;

    @Test
    public void testConstructorPassesParsedScaleToQueryCreatorFactory() throws IOException {
        final String config = "{ \"scaleForLinearFunctions\": 7, \"numberUnitDefinitions\": [ " +
                "{ \"units\": [ { \"term\": \"cm\" } ], \"fields\": [ { \"fieldName\": \"weight\" } ] } ] }";

        final AtomicInteger receivedScale = new AtomicInteger(-1);

        new NumberUnitRewriterFactory("rewriter_id", config, scale -> {
            receivedScale.set(scale);
            return numberUnitQueryCreator;
        });

        assertThat(receivedScale.get()).isEqualTo(7);
    }

    @Test
    public void testConstructorThrowsIOExceptionForMalformedJson() {
        assertThatThrownBy(() -> new NumberUnitRewriterFactory("rewriter_id", "not json", scale -> numberUnitQueryCreator))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void testConstructorThrowsIllegalArgumentExceptionForInvalidConfig() {
        final String config = "{ \"numberUnitDefinitions\": [ {} ] }";
        assertThatThrownBy(() -> new NumberUnitRewriterFactory("rewriter_id", config, scale -> numberUnitQueryCreator))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testValidateConfigurationDelegatesToParser() {
        assertThat(NumberUnitRewriterFactory.validateConfiguration(MINIMAL_CONFIG)).isEmpty();
        assertThat(NumberUnitRewriterFactory.validateConfiguration("{ \"numberUnitDefinitions\": [ {} ] }"))
                .isNotEmpty();
    }
}
