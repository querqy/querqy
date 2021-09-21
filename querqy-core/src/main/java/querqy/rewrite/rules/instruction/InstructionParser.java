package querqy.rewrite.rules.instruction;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import querqy.model.Clause;
import querqy.model.QuerqyQuery;
import querqy.rewrite.commonrules.model.BoostInstruction;
import querqy.rewrite.commonrules.model.DecorateInstruction;
import querqy.rewrite.commonrules.model.DeleteInstruction;
import querqy.rewrite.commonrules.model.FilterInstruction;
import querqy.rewrite.commonrules.model.Instruction;
import querqy.rewrite.commonrules.model.SynonymInstruction;
import querqy.rewrite.commonrules.model.Term;
import querqy.rewrite.rules.RuleParseException;
import querqy.rewrite.rules.instruction.skeleton.InstructionSkeleton;
import querqy.rewrite.rules.query.QuerqyQueryParser;
import querqy.rewrite.rules.query.TermsParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PRIVATE)
public class InstructionParser {

    private final List<Instruction> instructions = new ArrayList<>();

    private final Set<InstructionType> supportedTypes;
    private final QuerqyQueryParser querqyQueryParser;
    private final TermsParser termsParser;

    private final List<Term> inputTerms;
    private final List<InstructionSkeleton> skeletons;

    private InstructionSkeleton skeleton;

    @Builder(builderClassName = "PrototypeBuilder", builderMethodName = "prototypeBuilder")
    protected InstructionParser(@Singular final Set<InstructionType> supportedTypes,
                                final QuerqyQueryParser querqyQueryParser,
                                final TermsParser termsParser) {
        this.supportedTypes = supportedTypes;
        this.querqyQueryParser = querqyQueryParser;
        this.termsParser = termsParser;
        this.inputTerms = Collections.emptyList();
        this.skeletons = Collections.emptyList();
    }

    public InstructionParser with(final List<Term> inputTerms, final List<InstructionSkeleton> skeletons) {
        return of(supportedTypes, querqyQueryParser, termsParser, inputTerms, skeletons);
    }

    public List<Instruction> parse() {
        for (final InstructionSkeleton nextSkeleton : skeletons) {
            skeleton = nextSkeleton;
            parseSkeleton();
        }

        return instructions;
    }

    private void parseSkeleton() {
        assertThatTypeIsSupported(skeleton.getType());

        switch (skeleton.getType()) {
            case SYNONYM:
                parseAsSynonym(); break;

            case UP:
                parseAsBoost(BoostInstruction.BoostDirection.UP); break;

            case DOWN:
                parseAsBoost(BoostInstruction.BoostDirection.DOWN); break;

            case FILTER:
                parseAsFilter(); break;

            case DELETE:
                parseAsDelete(); break;

            case REPLACE:
                parseAsReplace(); break;

            case DECORATE:
                parseAsDecorate(); break;

            default:
                throw new RuleParseException("No parsing implemented for the given instruction type " + skeleton.getType());

        }
    }

    public void assertThatTypeIsSupported(final InstructionType type) {
        if (!supportedTypes.contains(type)) {
            throw new RuleParseException(
                    String.format("Instruction of type %s is not supported", skeleton.getType().name()));
        }
    }

    private void parseAsSynonym() {
        final float param = getParamAsFloat();
        final String value = getValueOrElseThrow();
        final List<Term> terms = termsParser.with(value).parse();

        instructions.add(new SynonymInstruction(terms, param));
    }

    private void parseAsBoost(final BoostInstruction.BoostDirection direction) {
        final float param = getParamAsFloat();
        final String value = getValueOrElseThrow();
        final QuerqyQuery<?> querqyQuery = querqyQueryParser.with(value, Clause.Occur.SHOULD).parse();

        instructions.add(new BoostInstruction(querqyQuery, direction, param));
    }

    private void parseAsFilter() {
        assertThatParamIsNotSet();

        final String value = getValueOrElseThrow();
        final QuerqyQuery<?> querqyQuery = querqyQueryParser.with(value, Clause.Occur.MUST).parse();

        instructions.add(new FilterInstruction(querqyQuery));
    }

    private void parseAsDelete() {
        assertThatParamIsNotSet();
        final Optional<String> optionalValue = skeleton.getValue();

        if (optionalValue.isPresent()) {
            parseAsDeleteWithValue(optionalValue.get());

        } else {
            instructions.add(new DeleteInstruction(inputTerms));
        }
    }

    private void parseAsDeleteWithValue(final String value) {
        final List<Term> deleteTerms = termsParser.with(value).parse();
        validateDeleteTerms(deleteTerms);
        instructions.add(new DeleteInstruction(deleteTerms));
    }

    private void validateDeleteTerms(final List<Term> deleteTerms) {
        for (final Term deleteTerm : deleteTerms) {
            if (deleteTerm.findFirstMatch(inputTerms) == null) {
                throw new RuleParseException("Condition doesn't contain the term to delete: " + deleteTerm);
            }
        }
    }

    private void parseAsReplace() {
        throw new UnsupportedOperationException("Replace instructions cannot be parsed so far");
    }

    private void parseAsDecorate() {
        final String value = getValueOrElseThrow();
        final Optional<String> optionalParam = skeleton.getParameter();

        if (optionalParam.isPresent()) {
            instructions.add(new DecorateInstruction(optionalParam.get(), value));

        } else {
            instructions.add(new DecorateInstruction(value));
        }
    }

    private float getParamAsFloat() {
        try {
            final float param = skeleton.getParameter()
                    .map(Float::parseFloat)
                    .orElse(1.0f);

            assertThatParamAsFloatIsNotNegative(param);
            return param;

        } catch (final NumberFormatException e) {
            throw new RuleParseException(
                    String.format("Instruction of type %s expects a float or nothing as parameter",
                            skeleton.getType().name()));
        }
    }

    private void assertThatParamAsFloatIsNotNegative(final float param) {
        if (param < 0) {
            throw new RuleParseException("Parameter must not be negative: " + param);
        }
    }

    private void assertThatParamIsNotSet() {
        if (skeleton.getParameter().isPresent()) {
            throw new RuleParseException(
                    String.format("Instruction of type %s does not support parameters", skeleton.getType().name()));
        }
    }

    private String getValueOrElseThrow() {
        return skeleton.getValue().orElseThrow(() ->
                new RuleParseException(
                        String.format("Instruction of type %s requires a value", skeleton.getType().name())));
    }

}
