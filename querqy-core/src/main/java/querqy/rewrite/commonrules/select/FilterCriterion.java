package querqy.rewrite.commonrules.select;

import querqy.rewrite.commonrules.model.Instructions;

public interface FilterCriterion {

    boolean isValid(final Instructions instructions);

}