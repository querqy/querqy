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
package querqy.lucene;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.lucene.util.ResourceLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

@RunWith(MockitoJUnitRunner.class)
public class GZIPAwareResourceLoaderTest {

    @Mock
    ResourceLoader resourceLoader;

    @Test
    public void testOpenEmptyResource() throws IOException {

        when(resourceLoader.openResource("some_name")).thenReturn(new ByteArrayInputStream(new byte[] {}));

        final GZIPAwareResourceLoader loader = new GZIPAwareResourceLoader(resourceLoader);
        final InputStream inputStream = loader.openResource("some_name");
        inputStream.close();

        verify(resourceLoader, times(1)).openResource("some_name");

    }

    @Test
    public void testZippedResource() throws IOException {

        when(resourceLoader.openResource("some_name")).thenReturn(getClass().getClassLoader()
                .getResourceAsStream("rules.txt.gz"));

        final GZIPAwareResourceLoader loader = new GZIPAwareResourceLoader(resourceLoader);
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(loader.openResource("some_name"), UTF_8))) {
            assertEquals("HELLO QUERQY!", reader.readLine());
            assertNull(reader.readLine());
        }

        verify(resourceLoader, times(1)).openResource("some_name");

    }

    @Test
    public void testUnzippedResource() throws IOException {

        when(resourceLoader.openResource("some_name")).thenReturn(new ByteArrayInputStream("HELLO, I wasn't zipped!"
                .getBytes(Charset.defaultCharset())));

        final GZIPAwareResourceLoader loader = new GZIPAwareResourceLoader(resourceLoader);
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(loader.openResource("some_name"), UTF_8))) {

            assertEquals("HELLO, I wasn't zipped!", reader.readLine());
            assertNull(reader.readLine());
        }

        verify(resourceLoader, times(1)).openResource("some_name");

    }


}
