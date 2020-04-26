package querqy.v2;

import java.util.Optional;

@FunctionalInterface
public interface SequenceHandler<T> {

    Optional<T> handleSequence(final StateSeqExtractor.State state);

}
