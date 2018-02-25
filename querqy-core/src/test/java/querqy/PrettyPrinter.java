/**
 * 
 */
package querqy;

import java.io.PrintWriter;
import java.util.Arrays;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Node;
import querqy.model.Query;
import querqy.model.RawQuery;
import querqy.model.Term;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class PrettyPrinter extends AbstractNodeVisitor<Node> {

   int depth = 0;
   final int indendStep;
   final PrintWriter writer;

   public PrettyPrinter(PrintWriter writer, int indendStep) {
      this.indendStep = indendStep;
      this.writer = writer;
   }

   @Override
   public Node visit(Query query) {
      String indend = makeIndend();
      writer.println(indend + "Q (");
      depth++;
      super.visit(query);
      depth--;
      writer.println(indend + ")");
      return null;

   }

   @Override
   public Node visit(BooleanQuery booleanQuery) {
      String indend = makeIndend();
      writer.print(indend);
      writer.println(booleanQuery.getOccur() + "BQ: (");
      depth++;
      super.visit(booleanQuery);
      depth--;
      writer.println(indend + ")");
      return null;

   };

   @Override
   public Node visit(DisjunctionMaxQuery disjunctionMaxQuery) {
      String indend = makeIndend();
      writer.print(indend);
      writer.println(disjunctionMaxQuery.getOccur() + "DMQ: (");
      depth++;
      super.visit(disjunctionMaxQuery);
      depth--;
      writer.println(indend + ")");
      return null;
   };

   @Override
   public Node visit(Term term) {
      String indend = makeIndend();
      writer.print(indend);
      writer.println(term);
      return null;
   };

   String makeIndend() {
      char[] buf = new char[depth * indendStep];
      Arrays.fill(buf, ' ');
      return new String(buf);
   }

   @Override
   public Node visit(RawQuery rawQuery) {
      String indend = makeIndend();
      writer.print(indend);
      writer.println(rawQuery);
      return null;
   }

}
