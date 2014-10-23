/**
 * 
 */
package querqy.rewrite.commonrules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import querqy.parser.QuerqyParserFactory;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;

/**
 * @author rene
 *
 */
public class SimpleCommonRulesParser {

   static final String EMPTY = "".intern();
   static final String ARROW_OP = "=>";

   final BufferedReader reader;
   final QuerqyParserFactory querqyParserFactory;
   int lineNumber = 0;
   final RulesCollectionBuilder builder;
   Input input = null;
   Instructions instructions;

   public SimpleCommonRulesParser(Reader in, QuerqyParserFactory querqyParserFactory, boolean ignoreCase) {
      this.reader = new BufferedReader(in);
      this.querqyParserFactory = querqyParserFactory;
      builder = new RulesCollectionBuilder(ignoreCase);
      instructions = new Instructions();
   }

   public RulesCollection parse() throws IOException, RuleParseException {
      try {
         lineNumber = 0;
         String line;
         while ((line = reader.readLine()) != null) {
            lineNumber++;
            nextLine(line);
         }
         putRule();
         return builder.build();
      } finally {
         try {
            reader.close();
         } catch (Exception e) {
            // TODO: log
         }
      }
   }

   public void putRule() throws RuleParseException {
      if (input != null) {
         if (instructions.isEmpty()) {
            throw new RuleParseException(lineNumber, "Instruction expected");
         }
         builder.addRule(input, instructions);
         input = null;
         instructions = new Instructions();
      }
   }

   public void nextLine(String line) throws RuleParseException {
      line = stripLine(line);
      if (line.length() > 0) {
         Object lineObject = LineParser.parse(line, input, querqyParserFactory);
         if (lineObject instanceof Input) {
            putRule();
            input = (Input) lineObject;
         } else if (lineObject instanceof ValidationError) {
            throw new RuleParseException(lineNumber, ((ValidationError) lineObject).getMessage());
         } else {
            instructions.add((Instruction) lineObject);
         }

      }
   }

   public String stripLine(String line) {
      line = line.trim();
      if (line.length() > 0) {
         int pos = line.indexOf('#');
         if (pos == 0) {
            return EMPTY;
         }
         if (pos > 0) {
            line = line.substring(0, pos);
         }
      }
      return line;
   }

}
