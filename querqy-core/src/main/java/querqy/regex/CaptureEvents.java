package querqy.regex;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class CaptureEvents {

    final Map<Integer, Integer> start = new HashMap<>();
    final Map<Integer, Integer> end   = new HashMap<>();

    CaptureEvents copy() {
        final CaptureEvents c = new CaptureEvents();
        c.start.putAll(this.start);
        c.end.putAll(this.end);
        return c;
    }

    @Override
    public boolean equals(Object o) {
//        if (!(o instanceof CaptureEvents other)) return false;
//        return start.equals(other.start) && end.equals(other.end);
        if (!(o instanceof CaptureEvents other)) return false;
        return this.start.equals(other.start) && this.end.equals(other.end);

    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}

