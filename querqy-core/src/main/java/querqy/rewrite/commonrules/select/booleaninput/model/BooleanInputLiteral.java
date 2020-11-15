package querqy.rewrite.commonrules.select.booleaninput.model;

import querqy.rewrite.commonrules.model.Input;
import querqy.rewrite.commonrules.select.booleaninput.model.BooleanInput.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BooleanInputLiteral {

    private final List<String> terms;
    private final List<Reference> references = new ArrayList<>();

    private Input input = null;

    public BooleanInputLiteral(final List<String> terms) {
        this.terms = terms;
    }

    public List<String> getTerms() {
        return this.terms;
    }

    public boolean hasInput() {
        return input != null;
    }

    public void setInput(final Input input) {
        this.input = input;
    }

    public Input getInput() {
        return input;
    }

    public void addReference(final BooleanInput.Reference reference) {
        references.add(reference);
    }

    public List<Reference> getReferences() {
        return this.references;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanInputLiteral that = (BooleanInputLiteral) o;
        return Objects.equals(terms, that.terms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terms);
    }
}


