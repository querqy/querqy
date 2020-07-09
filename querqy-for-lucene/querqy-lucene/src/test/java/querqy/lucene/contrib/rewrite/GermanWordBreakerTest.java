package querqy.lucene.contrib.rewrite;

import static org.hamcrest.Matchers.equalTo;
import static querqy.lucene.rewrite.TestUtil.addNumDocsWithStringField;
import static querqy.lucene.rewrite.TestUtil.addNumDocsWithTextField;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class GermanWordBreakerTest extends LuceneTestCase {


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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 2);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("abcdef", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"ab", "cdef"}),
                    equalTo(new CharSequence[] {"abcd","ef"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 2);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("hundefutter", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"hund", "futter"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 2);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("mattenladen", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"matte", "laden"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 2);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("arbeitsmatten", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"arbeit", "matten"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 1);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("schiller", indexReader, 2, true);
            // We don't care which strategy produces this but let's make sure, we don't crash.
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"s", "chiller"}))
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
        addNumDocsWithTextField("f1", "fans hirt", indexWriter, 20);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {


            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 2);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("fanshirt", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"fan", "shirt"}),
                    equalTo(new CharSequence[] {"fans","hirt"}))
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
    public void testThatHighCollationFrequencyWeighsMoreThanStrategyPrior() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "fan shirt", indexWriter, 1);
        addNumDocsWithTextField("f1", "fans hirt", indexWriter, 10 + (int) (GermanWordBreaker.NORM_PRIOR * GermanWordBreaker.PRIOR_PLUS_S));

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {


            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 2);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("fanshirt", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"fans", "hirt"}),
                    equalTo(new CharSequence[] {"fan","shirt"}))
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
    public void testMinBreakSizeAtLinkingMorphemeS() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "arbeit verträge", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            // minBreakLength must relate to prefix word w/o linking morpheme
            assertTrue(
                    new GermanWordBreaker("f1", true, 1, 7)
                            .breakWord("arbeitsverträge", indexReader, 2, true).isEmpty());

            assertFalse(
                    new GermanWordBreaker("f1", true, 1, 6)
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 2);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("straußenei", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"strauß", "ei"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 3);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("tageszeit", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"tag", "zeit"}))
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
                    new GermanWordBreaker("f1", true, 1, 4)
                            .breakWord("tageszeit", indexReader, 2, true).isEmpty());

            assertFalse(
                    new GermanWordBreaker("f1", true, 1, 3)
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 3);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("geisterstunde", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"geist", "stunde"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 3);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("aphorismensammlung", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"aphorismus", "sammlung"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 3);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("museenverwaltung", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"museum", "verwaltung"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 3);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("madonnenkult", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"madonna", "kult"}))
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
    public void testSplitAtLinkingMorphemeEnRemovingOn() throws IOException {

        final Analyzer analyzer = new WhitespaceAnalyzer();

        final Directory directory = newDirectory();
        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);

        addNumDocsWithTextField("f1", "stadion verbot", indexWriter, 1);

        indexWriter.close();

        try (final IndexReader indexReader = DirectoryReader.open(directory)) {

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 3);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("stadienverbot", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"stadion", "verbot"}))
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

            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 3);
            final List<CharSequence[]> sequences = wordBreaker.breakWord("baumwolltuch", indexReader, 2, true);
            assertThat(sequences, Matchers.contains(
                    equalTo(new CharSequence[] {"baumwolle", "tuch"}))
            );

        } finally {
            try {
                directory.close();
            } catch (final IOException e) {
                //
            }
        }

    }


//    @Test
//    public void testSplitAtLinkingMorphemeEnRemovingA() throws IOException {
//
//        final Analyzer analyzer = new WhitespaceAnalyzer();
//
//        final Directory directory = newDirectory();
//        final RandomIndexWriter indexWriter = new RandomIndexWriter(random(), directory, analyzer);
//
//        addNumDocsWithTextField("f1", "aphrodisiakum verkäufer", indexWriter, 1);
//
//        indexWriter.close();
//
//        try (final IndexReader indexReader = DirectoryReader.open(directory)) {
//
//            final GermanWordBreaker wordBreaker = new GermanWordBreaker("f1", true, 1, 3);
//            final List<CharSequence[]> sequences = wordBreaker.breakWord("aphrodisiakaverkäufer", indexReader, 2, true);
//            assertThat(sequences, Matchers.contains(
//                    equalTo(new CharSequence[] {"aphrodisiakum", "verkäufer"}))
//            );
//
//        } finally {
//            try {
//                directory.close();
//            } catch (final IOException e) {
//                //
//            }
//        }
//
//    }

}