package querqy.rewrite.commonrules.select.booleaninput.model;

public class Reference {
    private BooleanInput booleanInput;
    private final BooleanInputLiteral literal;
    private final int referenceId;

    public Reference(final BooleanInputLiteral literal, final int referenceId) {
        this.literal = literal;
        this.referenceId = referenceId;
        literal.addReference(this);
    }

    public BooleanInput getBooleanInput() {
        return booleanInput;
    }

    public void setBooleanInput(final BooleanInput booleanInput) {
        this.booleanInput = booleanInput;
    }

    public BooleanInputLiteral getLiteral() {
        return literal;
    }

    public int getReferenceId() {
        return referenceId;
    }

}
