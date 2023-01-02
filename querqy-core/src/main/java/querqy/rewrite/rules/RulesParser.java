package querqy.rewrite.rules;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import querqy.rewrite.commonrules.model.InstructionsSupplier;
import querqy.rewrite.commonrules.model.RulesCollectionBuilder;
import querqy.rewrite.rules.rule.RuleParser;
import querqy.rewrite.rules.rule.skeleton.RuleSkeleton;
import querqy.trie.TrieMap;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class RulesParser {

    private final RuleSkeletonParser ruleSkeletonParser;
    private final RuleParser ruleParser;
    private final RulesCollectionBuilder rulesCollectionBuilder;

    private int ruleOrderNumber = 0;

    @Builder
    private static RulesParser create(final RuleSkeletonParser ruleSkeletonParser,
                                      final RuleParser ruleParser,
                                      final RulesCollectionBuilder rulesCollectionBuilder) {
        return RulesParser.of(ruleSkeletonParser, ruleParser, rulesCollectionBuilder);
    }

    public TrieMap<InstructionsSupplier> parse() throws IOException {
        final List<RuleSkeleton> skeletons = ruleSkeletonParser.parse();
        parseRules(skeletons);

        return createTrieMap();
    }

    private void parseRules(final List<RuleSkeleton> skeletons) {
        for (final RuleSkeleton skeleton : skeletons) {
            ruleParser.parse(skeleton, ruleOrderNumber++);
        }
    }

    private TrieMap<InstructionsSupplier> createTrieMap() {
        ruleParser.finish().forEach(rulesCollectionBuilder::addRule);
        return rulesCollectionBuilder.getTrieMap();
    }
}
