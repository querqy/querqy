/**
 *
 */
package querqy.rewrite.commonrules;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import querqy.model.ExpandedQuery;
import querqy.model.Term;
import querqy.rewrite.QueryRewriter;
import querqy.rewrite.RewriterFactory;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.RulesCollection;
import querqy.utils.Constants;

/**
 * @author René Kriegler, @renekrie
 */
public class SimpleCommonRulesRewriterFactory implements RewriterFactory {

    final RulesCollection rules;
    final String ruleSelectionStratedgy;

    /**
     * @param reader
     * @param querqyParserFactory
     * @param ignoreCase
     * @throws IOException
     */
    public SimpleCommonRulesRewriterFactory(final Reader reader, final QuerqyParserFactory querqyParserFactory,
                                            final boolean ignoreCase) throws IOException {
        try {
            ruleSelectionStratedgy = Constants.DEFAULT_SELECTION_STRATEDGY;
            rules = new SimpleCommonRulesParser(reader, querqyParserFactory, ignoreCase).parse();
        } catch (final RuleParseException e) {
            throw new IOException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // TODO: log
            }
        }
    }

    /**
     *
     * @param reader
     * @param querqyParserFactory
     * @param ignoreCase
     * @throws IOException
     */
    public SimpleCommonRulesRewriterFactory(final Reader reader, final QuerqyParserFactory querqyParserFactory,
                                            final boolean ignoreCase, final String ruleSelectionStratedgy)
            throws IOException {
        try {
            this.ruleSelectionStratedgy = ruleSelectionStratedgy;
            rules = new SimpleCommonRulesParser(reader, querqyParserFactory, ignoreCase).parse();
        } catch (final RuleParseException e) {
            throw new IOException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // TODO: log
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * querqy.rewrite.RewriterFactory#createRewriter(querqy.model.ExpandedQuery,
     * java.util.Map)
     */
    @Override
    public QueryRewriter createRewriter(final ExpandedQuery input, final Map<String, ?> context) {
        return new CommonRulesRewriter(rules, ruleSelectionStratedgy);
    }

    @Override
    public Set<Term> getGenerableTerms() {
        // REVISIT: return Iterator? Limit number of results?
        final Set<Term> result = new HashSet<Term>();
        for (final Instruction instruction : rules.getInstructions()) {
            result.addAll(instruction.getGenerableTerms());
        }
        return result;
    }

}
