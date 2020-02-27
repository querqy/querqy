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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReplaceRewriterParser {

    private final InputStreamReader inputStreamReader;
    private final boolean ignoreCase;
    private final String inputDelimiter;
    private final QuerqyParser querqyParser;

    private static final String ERROR_MESSAGE = "ReplaceRule not properly configured. Each non-empty line must either " +
            "start with # or contain a rule, e. g. a => b";

    private static final String OPERATOR = "=>";
    public static final String TOKEN_SEPARATOR = " ";

    public ReplaceRewriterParser(final InputStreamReader inputStreamReader,
                                 final boolean ignoreCase,
                                 final String inputDelimiter,
                                 final QuerqyParser querqyParser) {

        this.inputStreamReader = inputStreamReader;
        this.ignoreCase = ignoreCase;
        this.inputDelimiter = inputDelimiter;
        this.querqyParser = querqyParser;
    }

    public TrieMap<List<CharSequence>> parseConfig() throws IOException {

        final TrieMap<List<CharSequence>> trieMap = new TrieMap<>();
        final Set<CharSequence> checkForDuplicateInput = new HashSet<>();

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
                final List<String> inputs = Arrays.stream(fullInput.split(this.inputDelimiter))
                        .map(String::trim)
                        .filter(term -> !term.isEmpty())
                        .collect(Collectors.toList());

                if (output.isEmpty() || inputs.isEmpty()) {
                    throw new RuleParseException(ERROR_MESSAGE);
                }

                final List<CharSequence> outputList = this.querqyParser.parse(output).getClauses().stream()
                        .map(booleanClause -> (DisjunctionMaxQuery) booleanClause)
                        .flatMap(disjunctionMaxQuery -> disjunctionMaxQuery.getTerms().stream())
                        .map(Term::getValue)
                        .collect(Collectors.toCollection(LinkedList::new));

                final List<CharSequence> inputList = inputs.stream()
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

                for (CharSequence seq : inputList) {
                    if (checkForDuplicateInput.contains(seq)) {
                        throw new RuleParseException(String.format("Duplicate input: %s", seq));
                    } else {
                        checkForDuplicateInput.add(seq);
                    }
                    trieMap.put(seq, outputList);
                }
            }

        } catch (RuleParseException e) {
            throw new IOException(e);
        }

        return trieMap;
    }
}
