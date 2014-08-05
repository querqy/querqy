/**
 * 
 */
package querqy.rewrite.commonrules;

import java.util.LinkedList;

import querqy.model.AbstractNodeVisitor;
import querqy.model.BooleanQuery;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Node;
import querqy.model.Query;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.commonrules.model.Action;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.Instructions;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.rewrite.commonrules.model.TermPositionSequence;

/**
 * @author rene
 *
 */
public class CommonRulesRewriter extends AbstractNodeVisitor<Node> implements QueryRewriter {
    
    protected final RulesCollection rules;
    protected final LinkedList<TermPositionSequence> sequencesStack;

    /**
     * 
     */
    public CommonRulesRewriter(RulesCollection rules) {
        this.rules = rules;
        sequencesStack = new LinkedList<>();
    }

    @Override
    public Query rewrite(Query query) {
        visit(query);
        return query;
    }
    
    @Override
    public Node visit(BooleanQuery booleanQuery) {
        
        sequencesStack.add(new TermPositionSequence());
        
        super.visit(booleanQuery);
        
        TermPositionSequence sequence = sequencesStack.removeLast();
        for (Action action: rules.getRewriteActions(sequence)) {
            for (Instructions instructions: action.getInstructions()) {
                for (Instruction instruction: instructions) {
                    instruction.apply(sequence, action.getMatchedTerms(), action.getStartPosition(), action.getEndPosition());
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
        sequencesStack.getLast().addTerm(term);
        return super.visit(term);
    }

}
