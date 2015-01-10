/**
 * 
 */
package querqy.rewrite.commonrules;

import java.util.LinkedList;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.PositionSequence;
import querqy.rewrite.commonrules.model.RulesCollection;

/**
 * @author rene
 *
 */
public class CommonRulesRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {

   protected final RulesCollection rules;
   protected final LinkedList<PositionSequence<Term>> sequencesStack;
   protected ExpandedQuery expandedQuery;

   /**
     * 
     */
   public CommonRulesRewriter(RulesCollection rules) {
      this.rules = rules;
      sequencesStack = new LinkedList<>();
   }

   @Override
   public ExpandedQuery rewrite(ExpandedQuery query) {

      QuerqyQuery<?> userQuery = query.getUserQuery();
      if (userQuery instanceof Query) {
         this.expandedQuery = query;
         visit((BooleanQuery) query.getUserQuery());
      }
      return query;
   }

   @Override
   public Node visit(BooleanQuery booleanQuery) {

      sequencesStack.add(new PositionSequence<Term>());

      super.visit(booleanQuery);

      PositionSequence<Term> sequence = sequencesStack.removeLast();
      for (Action action : rules.getRewriteActions(sequence)) {
         for (Instructions instructions : action.getInstructions()) {
            for (Instruction instruction : instructions) {
               instruction.apply(sequence, action.getTermMatches(), action.getStartPosition(),
                     action.getEndPosition(), expandedQuery);
            }
         }
      }

      return null;
   }

   @Override
   public Node visit(DisjunctionMaxQuery disjunctionMaxQuery) {
      sequencesStack.getLast().nextPosition();
      return super.visit(disjunctionMaxQuery);
   }

   @Override
   public Node visit(Term term) {
      sequencesStack.getLast().addElement(term);
      return super.visit(term);
   }

}
