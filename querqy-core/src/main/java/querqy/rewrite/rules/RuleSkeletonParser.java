package querqy.rewrite.rules;

import querqy.rewrite.rules.rule.skeleton.RuleSkeleton;

import java.io.IOException;
import java.util.List;

public interface RuleSkeletonParser {
    List<RuleSkeleton> parse() throws IOException;
}
