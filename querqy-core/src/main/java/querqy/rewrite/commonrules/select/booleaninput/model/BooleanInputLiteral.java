package querqy.rewrite.commonrules.select.booleaninput.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BooleanInputLiteral {

    private final List<String> terms;
    private final List<Reference> references = new ArrayList<>();

    public BooleanInputLiteral(final List<String> terms) {
        this.terms = terms;
    }

    public List<String> getTerms() {
        return this.terms;
    }

    public void addReference(final Reference reference) {
        references.add(reference);
    }

    public List<Reference> getReferences() {
        return this.references;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BooleanInputLiteral that = (BooleanInputLiteral) o;
        return Objects.equals(terms, that.terms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terms);
    }
}


