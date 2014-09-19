package querqy.lucene.parser;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.input.CharSequenceReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Collect terms of streams.
 * 
 * @author René Kriegler, @renekrie
 */
public class TermCollector {
   /**
    * Analyzes the given string using the given {@link Analyzer} (-chain).
    * 
    * @param input
    *           Input string.
    * @param analyzer
    *           Analyzer.
    * @return All terms from the resulting token stream.
    */
   public static Collection<String> collect(CharSequence input, Analyzer analyzer) {
      Preconditions.checkNotNull(input);
      Preconditions.checkNotNull(analyzer);

      Collection<String> result = Lists.newArrayList();
      try (TokenStream tokenStream = analyzer.tokenStream("querqy", new CharSequenceReader(input))) {
         tokenStream.reset();
         CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

         while (tokenStream.incrementToken()) {
            // Needs to converted to string, because on tokenStream.end()
            // the charTermAttribute will be flushed.
            result.add(charTermAttribute.toString());
         }

         tokenStream.end();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      return result;
   }
}
