package querqy.rewrite.lookup.triemap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import querqy.model.Term;
import querqy.rewrite.lookup.LookupConfig;
import querqy.rewrite.lookup.preprocessing.Preprocessor;
import querqy.trie.TrieMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class TrieMapSequenceLookupTest {

    @Mock Preprocessor preprocessor;
    @Mock TrieMap<String> trieMap;

    @Captor ArgumentCaptor<CharSequence> charSequenceCaptor;

    TrieMapSequenceLookup<String> trieMapSequenceLookup;

    @Before
    public void prepare() {
        trieMapSequenceLookup = new TrieMapSequenceLookup<>(
                trieMap,
                LookupConfig.builder()
                        .ignoreCase(true)
                        .preprocessor(preprocessor)
                        .build()
        );
    }

    @Test
    public void testThat_preprocessedTermIsPassedToMap_forGivenPreprocessor() {
        when(preprocessor.process(any())).thenReturn("b");

        trieMapSequenceLookup.evaluateTerm(term("a"));

        verify(trieMap).get(charSequenceCaptor.capture());
        assertThat(charSequenceCaptor.getValue()).isEqualTo("b");

    }

    private Term term(final String term) {
        return new Term(null, term);
    }

}
