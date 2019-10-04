package querqy.rewrite.contrib;

import querqy.ComparableCharSequence;
import querqy.CompoundCharSequence;
import querqy.LowerCaseCharSequence;
import querqy.model.DisjunctionMaxQuery;
import querqy.model.Term;
import querqy.parser.QuerqyParser;
import querqy.rewrite.commonrules.RuleParseException;
import querqy.trie.TrieMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ReplaceRewriterParser {

    private final InputStreamReader inputStreamReader;
    private final boolean ignoreCase;
    private final QuerqyParser querqyParser;

    // enhance
    private static final String ERROR_MESSAGE = "ReplaceRule not properly configured.";

    private static final String OPERATOR = "=>";
    public static final String TOKEN_SEPARATOR = " ";


    // make delimiter configurable
    private static final String DEFAULT_INPUT_DELIMITER = "\t";


    public ReplaceRewriterParser(final InputStreamReader inputStreamReader,
                                 final boolean ignoreCase,
                                 final QuerqyParser querqyParser) {

        this.inputStreamReader = inputStreamReader;
        this.ignoreCase = ignoreCase;
        this.querqyParser = querqyParser;
    }

    public TrieMap<List<ComparableCharSequence>> parseConfig() throws IOException {

        TrieMap<List<ComparableCharSequence>> trieMap = new TrieMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {

                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                final String[] lineSplit = line.split(OPERATOR);
                if (lineSplit.length != 2) {
                    throw new RuleParseException(ERROR_MESSAGE);
                }

                final String fullInput = lineSplit[0].trim();
                final String output = lineSplit[1].trim();
                final List<String> inputs = Arrays.stream(fullInput.split(DEFAULT_INPUT_DELIMITER))
                        .map(String::trim)
                        .filter(term -> !term.isEmpty())
                        .collect(Collectors.toList());

                if (output.isEmpty() || inputs.isEmpty()) {
                    throw new RuleParseException(ERROR_MESSAGE);
                }

                final List<ComparableCharSequence> outputList = this.querqyParser.parse(output).getClauses().stream()
                        .map(booleanClause -> (DisjunctionMaxQuery) booleanClause)
                        .flatMap(disjunctionMaxQuery -> disjunctionMaxQuery.getTerms().stream())
                        .map(Term::getValue)
                        .collect(Collectors.toCollection(LinkedList::new));

                final List<ComparableCharSequence> inputList = inputs.stream()
                        .map(this.querqyParser::parse)
                        .map(query -> query.getClauses().stream()
                                .map(booleanClause -> (DisjunctionMaxQuery) booleanClause)
                                .map(DisjunctionMaxQuery::getTerms)
                                .flatMap(Collection::stream)
                                .map(Term::getValue)
                                .map(seq -> ignoreCase ? new LowerCaseCharSequence(seq) : seq)
                                .collect(Collectors.toList()))
                        .map(sequenceList -> new CompoundCharSequence(TOKEN_SEPARATOR, sequenceList))
                        .collect(Collectors.toList());

                inputList.forEach(seq -> trieMap.put(seq, outputList));
            }

        } catch (RuleParseException e) {
            throw new IOException(e);
        }

        return trieMap;
    }
}
