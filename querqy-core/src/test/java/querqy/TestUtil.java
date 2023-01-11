package querqy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestUtil {


    public static Reader resource(final String resourceName) {
        return new InputStreamReader(TestUtil.class.getClassLoader().getResourceAsStream(resourceName), UTF_8);
    }

    public static List<String> list(final Reader reader) throws IOException {

        List<String> lines = new ArrayList<>();

        try(BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }

        }

        return lines;
    }
}
