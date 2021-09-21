package querqy.rewrite.rules.rule.skeleton;

import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(staticName = "create")
public class SingleLineParser implements LineParser {

    @Override
    public void parse(String line) {
        throw new UnsupportedOperationException("SingleLineParser has not been implemented so far");
    }

    @Override
    public List<RuleSkeleton> finish() {
        throw new UnsupportedOperationException("SingleLineParser has not been implemented so far");
    }
}
