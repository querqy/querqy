package querqy.modelv2;

import org.junit.Test;

public class TestQuery {

    @Test
    public void test() {
        Query query = new Query.Builder()
                .append("A")
                .append("B")
                .append("C")
                .build();

        System.out.println(query);
    }
}
