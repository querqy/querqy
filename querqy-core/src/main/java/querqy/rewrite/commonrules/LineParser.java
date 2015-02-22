/**
 * 
 */
package querqy.rewrite.commonrules;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import querqy.model.Clause.Occur;
import querqy.model.RawQuery;
import querqy.parser.QuerqyParser;
import querqy.parser.QuerqyParserFactory;
import querqy.rewrite.commonrules.model.BoostInstruction;
import querqy.rewrite.commonrules.model.BoostInstruction.BoostDirection;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.FilterInstruction;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.PrefixTerm;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.Term;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class LineParser {
    
    static final char BOUNDARY = '=';
    static final char WILDCARD = '*';
	
	public static Object parse(String line, Input previousInput, QuerqyParserFactory querqyParserFactory) {
		
	
		if (line.endsWith("=>")) {
			if (line.length() == 2) {
				return new ValidationError("Empty input");
			}
			return parseInput(line.substring(0, line.length() - 2));
		}
		
		if (previousInput == null) {
			return new ValidationError("Missing input for instruction");
		}
		
		String lcLine = line.toLowerCase();
		
		if (lcLine.startsWith("delete")) {
			
			if (lcLine.length() == 6) {
				return new DeleteInstruction(previousInput.getInputTerms());
			}
			
			String instructionTerms = line.substring(6).trim();
			if (instructionTerms.charAt(0) != ':') {
				return new ValidationError("Cannot parse line: " + line);
			}
			
			if (instructionTerms.length() == 1) {
				return new DeleteInstruction(previousInput.getInputTerms());
			}
			
			instructionTerms = instructionTerms.substring(1).trim();
			Object expr = parseTermExpression(instructionTerms);
			if (expr instanceof ValidationError) {
			    return new ValidationError("Cannot parse line: " + line +" : " + ((ValidationError)expr).getMessage());
			}
			@SuppressWarnings("unchecked")
            List<Term> deleteTerms = (List<Term>) expr;
			List<Term> inputTerms = previousInput.getInputTerms();
			for (Term term: deleteTerms) {
                if (term.findFirstMatch(inputTerms) == null) {
                    return new ValidationError("Condition doesn't contain the term to delete: " + term);
                }
            }
			
			return new DeleteInstruction(deleteTerms);
			
		}
		
		if (lcLine.startsWith("filter")) {
		
			if (lcLine.length() == 6) {
				return new ValidationError("Cannot parse line: " + line);
			}
			
			String filterString = line.substring(6).trim();
			if (filterString.charAt(0) != ':') {
				return new ValidationError("Cannot parse line: " + line);
			}
			
			filterString = filterString.substring(1).trim();
			if (filterString.length() == 0) {
				return new ValidationError("Cannot parse line: " + line);
			}
			
			if (filterString.charAt(0) == '*') {
				if (filterString.length() == 1) {
					return new ValidationError("Missing raw query after * in line: " + line);
				}
				String rawQuery = filterString.substring(1).trim();
				return new FilterInstruction(new RawQuery(null, rawQuery, Occur.MUST, false));
			} else if (querqyParserFactory == null) {
				return new ValidationError("No querqy parser factory to parse filter query. Prefix '*' you want to pass this line as a raw query String to your search engine. Line: " + line);
			} else {
				QuerqyParser parser = querqyParserFactory.createParser();
				return new FilterInstruction(parser.parse(filterString));
			}
		}
		
		if (lcLine.startsWith("down")) {
		    return parseBoostInstruction(line, lcLine, 4, BoostDirection.DOWN, querqyParserFactory);
		}
		
		if (lcLine.startsWith("up")) {
            return parseBoostInstruction(line, lcLine, 2, BoostDirection.UP, querqyParserFactory);
        }
		
		if (lcLine.startsWith("synonym")) {
		    
		    if (lcLine.length() == 7) {
                return new ValidationError("Cannot parse line: " + line);
            }
            
            String synonymString = line.substring(7).trim();
            if (synonymString.charAt(0) != ':') {
                return new ValidationError("Cannot parse line: " + line);
            }
            
            synonymString = synonymString.substring(1).trim();
            if (synonymString.length() == 0) {
                return new ValidationError("Cannot parse line: " + line);
            }
            
            List<Term> synonymTerms = new LinkedList<>();
            for (String token: synonymString.split("\\s+")) {
                if (token.length() > 0) {
                    Term term = parseTerm(token);
                    if (term.getMaxPlaceHolderRef() > 1) {
                        return new ValidationError("Max. wild card reference is 1: " + line);
                    }
                    synonymTerms.add(term);
                }
            }
            if (synonymTerms.isEmpty()) {
                // should never happen
                return new ValidationError("Cannot parse line: " + line);
            } else {
                return new SynonymInstruction(synonymTerms);
            }
            
		}
		
		return new ValidationError("Cannot parse line: " + line);
		
	}
	
	
	
	public static Object parseBoostInstruction(String line, String lcLine, int lengthPredicate, BoostDirection direction, QuerqyParserFactory querqyParserFactory) {
	    
	    if (lcLine.length() == lengthPredicate) {
            return new ValidationError("Cannot parse line: " + line);
        }
        
	    String boostLine = line.substring(lengthPredicate).trim();
        char ch = boostLine.charAt(0);
        if ((ch != '(' && ch != ':')) {
            return new ValidationError("Cannot parse line, '(' or ':' expected: " + line);
        }
        
        boostLine = boostLine.substring(1).trim();
        if (boostLine.length() == 1) {
            return new ValidationError("Query expected: " + line);
        }
        
        float boost = 1f;
        if (ch == '(') {
            int pos = boostLine.indexOf(')');
            if (pos < 1 || (pos == boostLine.length() - 1)) {
                return new ValidationError("Cannot parse line: " + line);
            }
            boost = Float.parseFloat(boostLine.substring(0, pos));
            boostLine = boostLine.substring(pos + 1).trim();
            if (boostLine.charAt(0) != ':') {
                return new ValidationError("Query expected: " + line);
            }
            boostLine = boostLine.substring(1).trim();
        }
        
        if (boostLine.length() == 0) {
            return new ValidationError("Query expected: " + line);
        }
        
        if (boostLine.charAt(0) == '*') {
            
            if (boostLine.length() == 1) {
                return new ValidationError("Missing raw query after * in line: " + line);
            }
            
            String rawQuery = boostLine.substring(1).trim();
            return new BoostInstruction(
                    new RawQuery(null, rawQuery, Occur.SHOULD, false), 
                    direction, boost);
        
        } else if (querqyParserFactory == null) {
        
            return new ValidationError("No querqy parser factory to parse filter query. Prefix '*' you want to pass this line as a raw query String to your search engine. Line: " + line);
        
        } else {
            QuerqyParser parser = querqyParserFactory.createParser();
            return new BoostInstruction(parser.parse(boostLine), direction, boost);
        }
	}
	
	@SuppressWarnings("unchecked")
    public static Object parseInput(String s) {
	    
	    boolean requiresLeftBoundary = false;
	    boolean requiresRightBoundary = false;
	    
	    s = s.trim();
	    if (s.length() > 0 && s.charAt(0) == BOUNDARY) {
	        requiresLeftBoundary = true;
	        s = s.substring(1).trim();
	    }
	
	    if (s.length() > 0 && s.charAt(s.length() - 1) == BOUNDARY) {
	        requiresRightBoundary = true;
	        s = s.substring(0, s.length() - 1).trim();
	    }
	    
	    int pos = s.indexOf('*');
	    if (pos > -1 && pos < (s.length() -1)) {
	        return new ValidationError("* is only allowed at the end of the input: " + s);
	    }
	    Object expr = parseTermExpression(s);
	    return (expr instanceof ValidationError) ? expr : new Input((List<Term>) expr, requiresLeftBoundary, requiresRightBoundary);
	
	}
	
	static Object parseTermExpression(String s) {
	    
		int len = s.length();
		
		if (len == 1) {
		    char ch = s.charAt(0);
            if (ch == WILDCARD) {
                return new ValidationError("Missing prefix for wildcard " + WILDCARD);
            }
			Term term = new Term(new char[] {ch}, 0, 1, null);
			return Arrays.asList(term);
		}
		
		
		List<Term> terms = new LinkedList<>();
		
		
		for (String part : s.split("\\s+")) {
			if (part.length() > 0) {
				terms.add(parseTerm(part));
			}
		}
		
		return terms;
		
	}
	
	public static Term parseTerm(String s) {
		
		int len = s.length();
		
		if (len == 1) {
		    char ch = s.charAt(0);
		    if (ch == WILDCARD) {
		        throw new IllegalArgumentException("Missing prefix for wildcard " + WILDCARD);
		    }
			return new Term(new char[] {ch}, 0, 1, null);
		}
		
		int pos = s.indexOf(':');
		
		boolean fieldNamesPossible = (pos > 0 && pos < (len -1));

		List<String> fieldNames = fieldNamesPossible ? parseFieldNames(s.substring(0, pos)) : null;
		
		String remaining = fieldNamesPossible ? s.substring(pos + 1).trim() : s;
		if (fieldNamesPossible && remaining.length() == 1 && remaining.charAt(0) == WILDCARD) {
		    throw new IllegalArgumentException("Missing prefix for wildcard " + WILDCARD);
		}

		return (remaining.charAt(remaining.length() - 1) == WILDCARD) 
		        ? new PrefixTerm(remaining.toCharArray(), 0, remaining.length() - 1, fieldNames)
		        : new Term(remaining.toCharArray(), 0, remaining.length(), fieldNames);
		
		
	}
	
	public static List<String> parseFieldNames(String s) {
		
		int len = s.length();
		
		if (len == 1) {
			return Arrays.asList(s);
		}
		
		
		List<String> result = new LinkedList<>();
		
		if (s.charAt(0) == '{' && s.charAt(len - 1) == '}') {
			if (len > 2) {
    			String[] parts = s.substring(1, len - 1).split(",");
    			for (String part: parts) {
    				part = part.trim();
    				if (part.length() > 0) {
    					result.add(part);
    				}
    			}
			}
			
		} else {
		    result.add(s);
		}

		return result;
	
	}

}