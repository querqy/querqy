/**
 * 
 */
package querqy.antlr.rewrite.commonrules;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import querqy.model.Clause.Occur;
import querqy.model.RawQuery;
import querqy.parser.QuerqyParser;
import querqy.parser.QuerqyParserFactory;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.FilterInstruction;
import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.model.Term;

/**
 * @author René Kriegler, @renekrie
 *
 */
public class LineParser {
	
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
			
			List<Term> deleteTerms = parseTermExpression(instructionTerms);
			List<Term> inputTerms = previousInput.getInputTerms();
			for (Term term: deleteTerms) {
                if (Term.findFirstMatch(term, inputTerms) == null) {
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
		
		return new ValidationError("Cannot parse line: " + line);
	}
	
	public static Input parseInput(String s) {
		
		return new Input(parseTermExpression(s));
	
	}
	
	public static List<Term> parseTermExpression(String s) {
		int len = s.length();
		
		if (len == 1) {
			Term term = new Term(new char[] {s.charAt(0)}, 0, 1, null);
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
			return new Term(new char[] {s.charAt(0)}, 0, 1, null);
		}
		
		int pos = s.indexOf(':');
		
		boolean fieldNamesPossible = (pos > 0 && pos < (len -1));

		List<String> fieldNames = fieldNamesPossible ? parseFieldNames(s.substring(0, pos)) : null;
		
		String remaining = fieldNamesPossible ? s.substring(pos + 1).trim() : s;

		return new Term(remaining.toCharArray(), 0, remaining.length(), fieldNames);
		
		
	}
	
	public static List<String> parseFieldNames(String s) {
		
		int len = s.length();
		
		if (len == 1) {
			return Arrays.asList(s);
		}
		
		
		List<String> result = new LinkedList<>();
		
		if (s.charAt(0) == '{' && s.charAt(len - 1) == '}' && (len > 2)) {
			
			String[] parts = s.substring(1, len - 1).split(",");
			for (String part: parts) {
				part = part.trim();
				if (part.length() > 0) {
					result.add(part);
				}
			}
			
		}

		return result;
	
	}
	
	
	

}
