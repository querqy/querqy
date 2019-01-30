/**
 *
 */
package querqy.rewrite.commonrules;

import org.apache.commons.collections4.CollectionUtils;
import querqy.model.*;
import querqy.model.Term;
import querqy.rewrite.ContextAwareQueryRewriter;
import querqy.rewrite.SearchEngineRequestAdapter;
import querqy.rewrite.commonrules.model.*;
import querqy.rewrite.commonrules.model.InputBoundary.Type;
import querqy.utils.Constants;

import java.util.*;

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
    protected SearchEngineRequestAdapter searchEngineRequestAdapter;
    protected SelectionStratedgy selectionStratedgy;

   /**
     *
     */
   public CommonRulesRewriter(RulesCollection rules) {
      this.rules = rules;
      sequencesStack = new LinkedList<>();
      selectionStratedgy = SelectionStratedgyFactory.getInstance().getSelectionStratedgy(Constants.DEFAULT_SELECTION_STRATEDGY);
   }

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
        throw new UnsupportedOperationException("This rewriter needs a query adapter");
    }

   @Override
   public ExpandedQuery rewrite(ExpandedQuery query, SearchEngineRequestAdapter searchEngineRequestAdapter) {

      QuerqyQuery<?> userQuery = query.getUserQuery();

      if (userQuery instanceof Query) {

         this.expandedQuery = query;
         this.searchEngineRequestAdapter = searchEngineRequestAdapter;

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

       boolean isDebug = Boolean.TRUE.equals(searchEngineRequestAdapter.getContext().get(CONTEXT_KEY_DEBUG_ENABLED));
       List<String> actionsDebugInfo = (List<String>) searchEngineRequestAdapter.getContext().get(CONTEXT_KEY_DEBUG_DATA);
       // prepare debug info context object if requested
       if (isDebug && actionsDebugInfo == null) {
           actionsDebugInfo = new LinkedList<>();
           searchEngineRequestAdapter.getContext().put(CONTEXT_KEY_DEBUG_DATA, actionsDebugInfo);
       }

       List<Action> actions = rules.getRewriteActions(sequenceForLookUp);
       actions = selectionStratedgy.selectActions(actions, retrieveCriterionFromRequest());
       List<String> appliedRules = new ArrayList<>();
       actions.stream().filter(action -> !CollectionUtils.isEmpty(action.getProperties())
               && action.getProperties().get(0).getPropertyMap() != null
               && action.getProperties().get(0).getPropertyMap().containsKey("id"))
               .forEach(action -> appliedRules
                       .add(action.getProperties().get(0).getPropertyMap().get("id")));

       searchEngineRequestAdapter.setAppliedRules(appliedRules);

       for (Action action : actions) {
           if (isDebug) {
               actionsDebugInfo.add(action.toString());
           }
           for (Instructions instructions : action.getInstructions()) {
              for (Instruction instruction : instructions) {
                 instruction.apply(sequence, action.getTermMatches(), action.getStartPosition(),
                       action.getEndPosition(), expandedQuery, searchEngineRequestAdapter);
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

    public Criterion retrieveCriterionFromRequest() {
        Optional<String> sort = searchEngineRequestAdapter.getRequestParam("rules.criteria.sort");
        Optional<String> size = searchEngineRequestAdapter.getRequestParam("rules.criteria.size");
        String[] filters = searchEngineRequestAdapter.getRequestParams("rules.criteria.filter");

        Criterion criterion = new Criterion();

        sort.ifPresent(sortStr -> {
            String[] sortCriteria = sortStr.split("\\s+");
            if (sortCriteria.length == 2) {
                criterion.add(new SortCriteria(sortCriteria[0], sortCriteria[1]));
            }
        });

        if (size.isPresent()) {
            criterion.add(new SelectionCriteria(Integer.valueOf(size.get())));
        } else {
            criterion.add(new SelectionCriteria(-1));
        }

        if (filters != null) {
            Arrays.asList(filters).parallelStream().forEach(filterStr -> {
                String[] filterArr = filterStr.split(":");
                if (filterArr.length == 2) {
                    criterion
                            .add(new FilterCriteria(filterArr[0].trim(), filterArr[1].trim()));
                }
            });
        }

        return criterion;
    }
}
