package querqy.rewrite.rules.rule.skeleton;

import java.util.List;

public interface LineParser {

    void parse(final String line);
    List<RuleSkeleton> finish();

}
