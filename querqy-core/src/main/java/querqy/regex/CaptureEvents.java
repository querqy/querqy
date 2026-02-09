package querqy.regex;

import java.util.HashMap;
import java.util.Map;

final class CaptureEvents {

    final Map<Integer, Integer> start = new HashMap<>();
    final Map<Integer, Integer> end   = new HashMap<>();

    CaptureEvents copy() {
        final CaptureEvents c = new CaptureEvents();
        c.start.putAll(this.start);
        c.end.putAll(this.end);
        return c;
    }
}

