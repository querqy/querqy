package querqy.lucene.contrib.rewrite.wordbreak;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static querqy.lucene.rewrite.TestUtil.addNumDocsWithTextField;

public class GermanWordBreakerTest extends LuceneTestCase {
    private final Morphology GERMAN = new MorphologyProvider().get("GERMAN").get();

    @Test
    public void testWithEmptyIndex() throws IOException {
        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("abcdef", indexReader, 2, true);
            assertNotNull(sequences);
            assertTrue(sequences.isEmpty());

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testWithNoExistentDictField() throws IOException {
        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();

        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);
        addNumDocsWithTextField("f1", "abc def", indexWriter, 4);
        addNumDocsWithTextField("f1", "ab cdef", indexWriter, 10);
        addNumDocsWithTextField("f1", "abcd ef", indexWriter, 5);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f2", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("abcdef", indexReader, 2, true);
            assertNotNull(sequences);
            assertTrue(sequences.isEmpty());

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testNoLinkingMorpheme() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "abc def", indexWriter, 4);
        addNumDocsWithTextField("f1", "ab cdef", indexWriter, 10);
        addNumDocsWithTextField("f1", "abcd ef", indexWriter, 5);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("abcdef", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"ab", "cdef"}),
                    equalTo(new CharSequence[]{"abcd", "ef"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeE() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "hund futter", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("hundefutter", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"hund", "futter"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeN() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "matte laden", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("mattenladen", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"matte", "laden"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeS() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "arbeit matten", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("arbeitsmatten", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"arbeit", "matten"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSingleLetterCandidateS() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "s chiller", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 1, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("schiller", indexReader, 2, true);
            // We don't care which strategy produces this but let's make sure, we don't crash.
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"s", "chiller"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }


    @Test
    public void testNoLinkingMorphemeIsPreferredOverMorphemeSForSameCollationFrequency() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "fan shirt", indexWriter, 20);
        addNumDocsWithTextField("f1", "fan hirt", indexWriter, 20);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {


            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("fanshirt", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"fan", "shirt"}),
                    equalTo(new CharSequence[]{"fan", "hirt"}))
            );


        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testThatHighDfObservatioWeightWeighsMoreThanMorphoSyntaxPrior() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "fan shirt", indexWriter, 1);
        addNumDocsWithTextField("f1", "fan hirt", indexWriter, 10);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {


            // use higher weight for morph-syntax structure first
            org.hamcrest.MatcherAssert.assertThat(
                    new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100, 0.8f).breakWord("fanshirt", indexReader,
                            1, true), contains(equalTo(new CharSequence[]{"fan", "shirt"})));

            // use low weight for morph-syntax structure first
            org.hamcrest.MatcherAssert.assertThat(
                    new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100, 0.1f).breakWord("fanshirt", indexReader,
                            1, true), contains(equalTo(new CharSequence[]{"fan", "hirt"})));


        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testMinBreakSizeAtLinkingMorphemeS() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "arbeit verträge", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            // minBreakLength must relate to prefix word w/o linking morpheme
            assertTrue(
                    new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 7, 100)
                            .breakWord("arbeitsverträge", indexReader, 2, true).isEmpty());

            assertFalse(
                    new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 6, 100)
                            .breakWord("arbeitsverträge", indexReader, 2, true).isEmpty());


        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeEn() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "strauß ei", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("straußenei", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"strauß", "ei"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeNen() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "wöchnerin heim", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("wöchnerinnenheim", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"wöchnerin", "heim"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeIen() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "prinzip reiter", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 2, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("prinzipienreiter", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"prinzip", "reiter"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeEs() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "tag zeit", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("tageszeit", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"tag", "zeit"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testMinBreakSizeAtLinkingMorphemeEs() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "tag zeit", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            // minBreakLength must relate to prefix word w/o linking morpheme
            assertTrue(
                    new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 4, 100)
                            .breakWord("tageszeit", indexReader, 2, true).isEmpty());

            assertFalse(
                    new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100)
                            .breakWord("tageszeit", indexReader, 2, true).isEmpty());


        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }


    @Test
    public void testSplitAtLinkingMorphemeEr() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "geist stunde", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("geisterstunde", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"geist", "stunde"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeUmlautEr() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "buch regal", indexWriter, 1);
        addNumDocsWithTextField("f1", "blatt wald", indexWriter, 1);
        addNumDocsWithTextField("f1", "korn brötchen", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);

            org.hamcrest.MatcherAssert.assertThat(wordBreaker.breakWord("bücherregal", indexReader, 2, true), contains(
                    equalTo(new CharSequence[]{"buch", "regal"}))
            );

            org.hamcrest.MatcherAssert.assertThat(wordBreaker.breakWord("blätterwald", indexReader, 2, true), contains(
                    equalTo(new CharSequence[]{"blatt", "wald"}))
            );

            org.hamcrest.MatcherAssert.assertThat(wordBreaker.breakWord("körnerbrötchen", indexReader, 2, true),
                    contains(equalTo(new CharSequence[]{"korn", "brötchen"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeUmlautE() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "gans klein", indexWriter, 1);
        addNumDocsWithTextField("f1", "laus kamm", indexWriter, 1);
        addNumDocsWithTextField("f1", "korb macher", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);

            org.hamcrest.MatcherAssert.assertThat(wordBreaker.breakWord("gänseklein", indexReader, 2, true), contains(
                    equalTo(new CharSequence[]{"gans", "klein"}))
            );

            org.hamcrest.MatcherAssert.assertThat(wordBreaker.breakWord("läusekamm", indexReader, 2, true), contains(
                    equalTo(new CharSequence[]{"laus", "kamm"}))
            );

            org.hamcrest.MatcherAssert.assertThat(wordBreaker.breakWord("körbemacher", indexReader, 2, true), contains(
                    equalTo(new CharSequence[]{"korb", "macher"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeEnRemovingUs() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "aphorismus sammlung", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("aphorismensammlung", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"aphorismus", "sammlung"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeEnRemovingUm() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "museum verwaltung", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("museenverwaltung", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"museum", "verwaltung"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeEnRemovingA() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "madonna kult", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("madonnenkult", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(equalTo(new CharSequence[]{"madonna", "kult"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeEnRemovingOnPlusEn() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "stadion verbot", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("stadienverbot", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(equalTo(new CharSequence[]{"stadion", "verbot"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeRemovingOnPlusA() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "pharmakon analyse", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("pharmakaanalyse", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"pharmakon", "analyse"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeRemovingEPlusI() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "carabiniere schule", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("carabinierischule", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"carabiniere", "schule"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitRemovingE() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "baumwolle tuch", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("baumwolltuch", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"baumwolle", "tuch"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitRemovingEn() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "süden wind", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("südwind", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"süden", "wind"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    @Test
    public void testSplitAtLinkingMorphemeARemovingUm() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "aphrodisiakum verkäufer", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 1, 3, 100);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("aphrodisiakaverkäufer", indexReader, 2, true);
            org.hamcrest.MatcherAssert.assertThat(sequences, contains(
                    equalTo(new CharSequence[]{"aphrodisiakum", "verkäufer"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }

    //@Test
    public void xtestSpeed() throws Exception {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        final int factor = 1000;

        addNumDocsWithTextField("f1", "abc def", indexWriter, 4 * factor);
        addNumDocsWithTextField("f1", "ab cdef", indexWriter, 10 * factor);
        addNumDocsWithTextField("f1", "abcd ef", indexWriter, 5 * factor);
        addNumDocsWithTextField("f1", "hund futter", indexWriter, factor);
        addNumDocsWithTextField("f1", "baumwolle tuch", indexWriter, factor);
        addNumDocsWithTextField("f1", "stadion verbot", indexWriter, factor);
        addNumDocsWithTextField("f1", "madonna kult", indexWriter, factor);
        addNumDocsWithTextField("f1", "museum verwaltung", indexWriter, factor);
        addNumDocsWithTextField("f1", "aphorismus sammlung", indexWriter, factor);
        addNumDocsWithTextField("f1", "geist stunde", indexWriter, factor);
        addNumDocsWithTextField("f1", "tag zeit", indexWriter, factor);
        addNumDocsWithTextField("f1", "strauß ei", indexWriter, factor);
        addNumDocsWithTextField("f1", "arbeit verträge", indexWriter, factor);
        addNumDocsWithTextField("f1", "fan shirt", indexWriter, factor);
        addNumDocsWithTextField("f1", "fan hirt", indexWriter, 10 + factor * (int) (GermanDecompoundingMorphology.NORM_PRIOR * GermanDecompoundingMorphology.PRIOR_PLUS_S));
        addNumDocsWithTextField("f1", "s chiller", indexWriter, factor);
        addNumDocsWithTextField("f1", "arbeit matten", indexWriter, factor);
        addNumDocsWithTextField("f1", "matte laden", indexWriter, factor);
        addNumDocsWithTextField("f1", "gans fleisch", indexWriter, factor);
        addNumDocsWithTextField("f1", "pharmakon analyse", indexWriter, factor);

        indexWriter.close();

        System.out.println("Done indexing");

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final MorphologicalWordBreaker wordBreaker = new MorphologicalWordBreaker(GERMAN, "f1", true, 50, 1, 100);
            final boolean verifyCollation = true;

            final int maxIterations = 1000;
            wordBreaker.breakWord("abcdef", indexReader, 2, verifyCollation);

            final long t1 = System.currentTimeMillis();
            for (int i = 0; i < maxIterations; i++) {
                wordBreaker.breakWord("abcdef", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("hundefutter", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("baumwolltuch", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("stadienverbot", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("madonnenkult", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("museenverwaltung", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("aphorismensammlung", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("geisterstunde", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("tageszeit", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("straußenei", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("arbeitsverträge", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("fanshirt", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("schiller", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("arbeitsmatten", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("mattenladen", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("gänsefleisch", indexReader, 2, verifyCollation);
                wordBreaker.breakWord("pharmakaanalyse", indexReader, 2, verifyCollation);
            }
            final long t2 = System.currentTimeMillis();
            System.out.println(t2 - t1);
            System.out.println(((t2 - t1) / 15000f));

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }


    }

}