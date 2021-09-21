package querqy.rewrite.commonrules;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;


/**
 * Just as simple wrapper that marks a String as 'input'.
 */
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
@Deprecated
public class InputString {

    @NonNull
    public final String value;

}
