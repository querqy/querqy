package querqy.lucene.contrib.rewrite.wordbreak;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import org.apache.lucene.index.Term;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

//@RunWith(MockitoJUnitRunner.class)
public class GermanMorphologyTest {

//
//    Collector collector;
//    SuffixGroup morphemes;
//    ArgumentCaptor<String> leftCaptor;
//
//    @Before
//    public void setUp() {
//
//        collector = mock(Collector.class);
//        when(collector.collect(any(), any(), any(), anyInt(), anyFloat()))
//                .thenReturn(Collector.CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED);
//
//        leftCaptor = ArgumentCaptor.forClass(String.class);
//
//        morphemes = Morphology.GERMAN.createMorphemes(1f);
//    }
//
//
//    @Test
//    public void testPlusNull() {
//
//        final String left = "left";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(3)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("lefte", "leften", "left"));
//
//    }
//
//    @Test
//    public void testPlusS() {
//
//        final String left = "lefts";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(4)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("lefts", "leftse", "leftsen", "left"));
//
//    }
//
//    @Test
//    public void testPlusN() {
//
//        final String left = "leftn";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(4)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("leftn", "leftne", "leftnen", "left"));
//
//    }
//
//    @Test
//    public void testPlusEn() {
//
//        final String left = "leften";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(9)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("leftus", "leftum", "lefton", "lefta", "leften",
//                "leftene", "leftenen", "lefte", "left"));
//
//    }
//
//    @Test
//    public void testPlusNen() {
//
//
//        final String left = "leftnen";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(10)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("leftnus", "leftnum", "leftnon", "leftna", "leftnen",
//                "leftnene", "leftnenen", "leftne", "leftn", "left"));
//
//
//    }
//
//    @Test
//    public void testPlusIen() {
//
//
//        final String left = "leftien";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(10)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder( "leftien",
//                "leftie", "lefti", "leftius", "leftium", "leftia", "leftion", "left", "leftienen", "leftiene"));
//
//
//    }
//
//    @Test
//    public void testPlusA() {
//
//        final String left = "lefta";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(5)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("leftum", "lefton", "leftae", "leftaen", "lefta"));
//
//
//    }
//
//    @Test
//    public void testPlusI() {
//
//        final String left = "lefti";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(4)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("lefte", "lefti", "leftie", "leftien"));
//
//
//    }
//
//    @Test
//    public void testPlusE() {
//
//        final String left = "lefte";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(4)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        // TODO e+e(n) might never occur in the language
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("left", "lefte", "lefteen", "leftee"));
//
//    }
//
//    @Test
//    public void testPlusEr() {
//
//        final String left = "lefter";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(4)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("left", "lefter","leftere", "lefteren"));
//
//    }
//
//    @Test
//    public void testPlusErUmlaut() {
//
//        final String left = "bücher";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(5)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("buch", "büch", "bücher","büchere", "bücheren"));
//
//
//        collector = mock(Collector.class);
//        when(collector.collect(any(), any(), any(), anyInt(), anyFloat())).
//                thenReturn(Collector.CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED);
//
//        leftCaptor = ArgumentCaptor.forClass(String.class);
//
//        final String left2 = "häuser";
//
//        assertTrue(morphemes.collect(left2, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(5)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("haus", "häus", "häuser","häusere", "häuseren"));
//
//    }
//
//    @Test
//    public void testPlusEUmlaut() {
//
//        final String left = "gänse";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(5)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        // TODO: avoid e+e
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("gans", "gäns", "gänse","gänsee", "gänseen"));
//
//
//        collector = mock(Collector.class);
//        when(collector.collect(any(), any(), any(), anyInt(), anyFloat()))
//                .thenReturn(Collector.CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED);
//
//        leftCaptor = ArgumentCaptor.forClass(String.class);
//
//        final String left2 = "läuse";
//
//        assertTrue(morphemes.collect(left2, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(5)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("laus", "läus", "läuse","läusee", "läuseen"));
//
//    }
//
//    @Test
//    public void testMaxEvaluations() {
//
//        final String left = "leftnen";
//        final String right = "right";
//        final Term rightTerm = new Term("f1", "right");
//
//        collector = mock(Collector.class);
//        when(collector.collect(any(), any(), any(), anyInt(), anyFloat()))
//                .thenReturn(Collector.CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED,
//                        Collector.CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED,
//                        Collector.CollectionState.MATCHED_MAX_EVALUATIONS_NOT_REACHED,
//                        Collector.CollectionState.MATCHED_MAX_EVALUATIONS_REACHED
//                );
//
//
//        assertTrue(morphemes.collect(left, 0, right, rightTerm, 10, 1, collector).getMatched().orElse(false));
//        verify(collector, times(4)).collect(leftCaptor.capture(), anyString(), any(), anyInt(), anyFloat());
//        assertThat(leftCaptor.getAllValues(), containsInAnyOrder("leftnen", "leftnene", "leftnenen", "leftne"));
//
//
//    }
}
