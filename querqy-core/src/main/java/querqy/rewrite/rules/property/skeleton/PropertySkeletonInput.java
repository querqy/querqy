package querqy.rewrite.rules.property.skeleton;

import lombok.Builder;
import lombok.Getter;

@Builder(builderClassName = "Builder")
@Getter
public class PropertySkeletonInput {

    private boolean isPropertyInitiation;

    private boolean isMultiLineInitiation;
    private boolean isMultiLineFinishing;

    private String rawInput;
    private String strippedInput;

    // make javadoc happy when working with Lombok
    public static class Builder {}

}
