package querqy.model;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ParametrizedRawQueryTest {

    @Test
    public void testBuildQueryString() {
        ParametrizedRawQuery rq = new ParametrizedRawQuery(
                null,
                Arrays.asList(
                        new ParametrizedRawQuery.Part("q1 ", ParametrizedRawQuery.Part.Type.QUERY_PART),
                        new ParametrizedRawQuery.Part("p1", ParametrizedRawQuery.Part.Type.PARAMETER),
                        new ParametrizedRawQuery.Part(" q2 ", ParametrizedRawQuery.Part.Type.QUERY_PART),
                        new ParametrizedRawQuery.Part("p2", ParametrizedRawQuery.Part.Type.PARAMETER)
                ),
                Clause.Occur.SHOULD,
                false);

        String queryString = rq.buildQueryString(param -> param.replace("p", "param"));
        assertThat(queryString).isEqualTo("q1 param1 q2 param2");
    }
}
