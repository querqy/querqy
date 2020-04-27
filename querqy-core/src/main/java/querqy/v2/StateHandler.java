package querqy.v2;

import java.util.Optional;

@FunctionalInterface
public interface StateHandler<T> {

    Optional<T> handleSequence(final StatefulSeqHandler.State state);

}
