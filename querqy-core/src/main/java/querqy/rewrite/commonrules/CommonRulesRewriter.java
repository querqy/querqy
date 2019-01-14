/**
 * 
 */
package querqy.rewrite.commonrules;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.ExpandedQuery;
import querqy.model.InputSequenceElement;
import querqy.model.Node;
import querqy.model.QuerqyQuery;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.ContextAwareQueryRewriter;
import querqy.rewrite.commonrules.model.*;
import querqy.rewrite.commonrules.model.InputBoundary.Type;

/**
 * @author rene
 *
 */
public class CommonRulesRewriter extends AbstractNodeVisitor<Node> implements ContextAwareQueryRewriter {

    static final InputBoundary LEFT_BOUNDARY = new InputBoundary(Type.LEFT);
    static final InputBoundary RIGHT_BOUNDARY = new InputBoundary(Type.RIGHT);


    protected final RulesCollection rules;
    protected final LinkedList<PositionSequence<Term>> sequencesStack;
    protected ExpandedQuery expandedQuery;
    protected Map<String, Object> context;
    protected SelectionStratedgy selectionStratedgy;

   /**
     * 
     */
   public CommonRulesRewriter(RulesCollection rules,  String ruleSelectionStratedgy) {
      this.rules = rules;
      sequencesStack = new LinkedList<>();
       selectionStratedgy = SelectionStratedgyFactory.getInstance()
               .getSelectionStratedgy(ruleSelectionStratedgy);
   }
   
   @Override
   public ExpandedQuery rewrite(ExpandedQuery query) {
       throw new UnsupportedOperationException("This rewriter needs a query context");
   }

   @Override
   public ExpandedQuery rewrite(ExpandedQuery query, Map<String, Object> context) {

      QuerqyQuery<?> userQuery = query.getUserQuery();
      
      if (userQuery instanceof Query) {
          
         this.expandedQuery = query;
         this.context = context;
         
         sequencesStack.add(new PositionSequence<Term>());
        
         super.visit((BooleanQuery) query.getUserQuery());

         applySequence(sequencesStack.removeLast(), true);
         
      }
      return query;
   }

   @Override
   public Node visit(BooleanQuery booleanQuery) {

      sequencesStack.add(new PositionSequence<Term>());

      super.visit(booleanQuery);

      applySequence(sequencesStack.removeLast(), false);

      return null;
   }
   
   protected void applySequence(PositionSequence<Term> sequence, boolean addBoundaries) {
       
       PositionSequence<InputSequenceElement> sequenceForLookUp = addBoundaries ? addBoundaries(sequence) : termSequenceToInputSequence(sequence);

       boolean isDebug = Boolean.TRUE.equals(context.get(CONTEXT_KEY_DEBUG_ENABLED));
       List<String> actionsDebugInfo = (List<String>) context.get(CONTEXT_KEY_DEBUG_DATA);
       // prepare debug info context object if requested
       if (isDebug && actionsDebugInfo == null) {
           actionsDebugInfo = new LinkedList<>();
           context.put(CONTEXT_KEY_DEBUG_DATA, actionsDebugInfo);
       }

       List<Action> actions = rules.getRewriteActions(sequenceForLookUp);
       actions = selectionStratedgy.selectActions(actions, expandedQuery.getCriterion());

       actions.stream().filter(action -> !CollectionUtils.isEmpty(action.getProperties())
               && action.getProperties().get(0).getPropertyMap() != null
               && action.getProperties().get(0).getPropertyMap().containsKey("id"))
               .forEach(action -> this.expandedQuery
                       .addAppliedRuleId(action.getProperties().get(0).getPropertyMap().get("id")));


       for (Action action : actions) {
           if (isDebug) {
               actionsDebugInfo.add(action.toString());
           }
           for (Instructions instructions : action.getInstructions()) {
              for (Instruction instruction : instructions) {
                 instruction.apply(sequence, action.getTermMatches(), action.getStartPosition(),
                       action.getEndPosition(), expandedQuery, context);
              }
           }
        }
   }
   
   protected PositionSequence<InputSequenceElement> termSequenceToInputSequence(PositionSequence<Term> sequence) {
       PositionSequence<InputSequenceElement> result = new PositionSequence<>();
       for (List<Term> termList : sequence) {
           result.add(Collections.<InputSequenceElement>unmodifiableList(termList));
       }
       return result;
   }
   protected PositionSequence<InputSequenceElement> addBoundaries(PositionSequence<Term> sequence) {
       
       PositionSequence<InputSequenceElement> result = new PositionSequence<>();
       result.nextPosition();
       result.addElement(LEFT_BOUNDARY);
       
       for (List<Term> termList : sequence) {
           result.add(Collections.<InputSequenceElement>unmodifiableList(termList));
       }

       result.nextPosition();
       result.addElement(RIGHT_BOUNDARY);
       return result;
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
