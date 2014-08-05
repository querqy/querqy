/**
 * 
 */
package querqy.antlr.rewrite.commonrules;

import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import querqy.antlr.commonrules.SimpleParserBaseVisitor;
import querqy.antlr.commonrules.SimpleParserParser.DeleteInstructionContext;
import querqy.antlr.commonrules.SimpleParserParser.FieldNameContext;
import querqy.antlr.commonrules.SimpleParserParser.InputContext;
import querqy.antlr.commonrules.SimpleParserParser.LineContext;
import querqy.antlr.commonrules.SimpleParserParser.TermExprContext;
import querqy.antlr.commonrules.SimpleParserParser.TermValueContext;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Term;

/**
 * @author rene
 *
 */
public class LineVisitor extends SimpleParserBaseVisitor<Object> {
    
    // TODO: factory?
    enum LineType {INPUT, DELETE, BOOST, FILTER, SYNONYM, UNKNOWN}
    
    final char[] inputChars;
    List<Term> inputTerms;
    List<Term> instructionTerms;
    List<String> fieldNames;
    LineType lineType = LineType.UNKNOWN;
    final Input previousInput;

    /**
     * 
     */
    public LineVisitor(char[] inputChars, Input previousInput) {
        this.inputChars = inputChars;
        this.previousInput = previousInput;
    }
    
    @Override
    public Object visitLine(LineContext ctx) {
        lineType = LineType.UNKNOWN;
        return super.visitLine(ctx);
    }
    
    @Override
    public Object visitInput(InputContext ctx) {
        lineType = LineType.INPUT;
        inputTerms = new LinkedList<>();
        super.visitInput(ctx);
        return new Input(inputTerms);
    }
    
    @Override
    public Object visitDeleteInstruction(DeleteInstructionContext ctx) {
        
        if (previousInput == null) {
            return new ValidationError("Delete instruction without condition");
        }
        
        List<Term> prevInputTerms = previousInput.getInputTerms();
        if (prevInputTerms == null || prevInputTerms.isEmpty()) {
            return new ValidationError("Delete instruction without condition");
        }
        
        lineType = LineType.DELETE;
        instructionTerms = new LinkedList<>();
        
        super.visitDeleteInstruction(ctx);
        
        if (instructionTerms.isEmpty()) {
            // delete all input terms 
           
            
            return new DeleteInstruction(prevInputTerms);
        } else {
            for (Term term: instructionTerms) {
                if (Term.findFirstMatch(term, prevInputTerms) == null) {
                    return new ValidationError("Condition doesn't contain the term to delete: " + term);
                }
            }
            return new DeleteInstruction(instructionTerms);
        }
        
    }
    
    @Override
    public Object visitTermExpr(TermExprContext ctx) {
        fieldNames = new LinkedList<>();
        return super.visitTermExpr(ctx);
    }
    
    @Override
    public Object visitTermValue(TermValueContext ctx) {
        
        Token startToken = ctx.getStart();
        int start =  startToken.getStartIndex();
        int length = 1 + startToken.getStopIndex() - startToken.getStartIndex();
        
        switch (lineType) {
        case INPUT: {
            Term term = new Term(inputChars, start, length, fieldNames);
            inputTerms.add(term);
            return term;
        }
        case DELETE:
            Term term = new Term(inputChars, start, length, fieldNames);
            instructionTerms.add(term);
            return term;
        default:
            throw new RuntimeException("Cannot handle line type: " + lineType);
            
        }
    }
    
    @Override
    public Object visitFieldName(FieldNameContext ctx) {
        Token startToken = ctx.getStart();
        String fieldName = new String(inputChars, startToken.getStartIndex(), 1 + startToken.getStopIndex() - startToken.getStartIndex());
        fieldNames.add(fieldName);
        return fieldName;
    }
    
    public static class ValidationError {
        final String message;
        public ValidationError(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
        
    }

}
