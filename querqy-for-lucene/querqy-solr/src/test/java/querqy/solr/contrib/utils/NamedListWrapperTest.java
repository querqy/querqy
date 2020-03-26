package querqy.solr.contrib.utils;

import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NamedListWrapperTest {

    private NamedList<Object> namedList = new NamedList<>();
    private NamedList<Object> nestedNamedListNotEmpty = new NamedList<>();

    private String message = "message";
    private String unknown = "unknown";
    private String keyNamedListExists = "namedList";
    private String keyStringExists = "string";
    private String keyIntExists = "int";
    private String keyBdPositive = "bd";
    private String keyBdNegative = "bdNeg";
    private String keyListOfNamedListsNotEmpty = "nestedNamedListNotEmpty";

    private NamedListWrapper namedListWrapper;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void prepare() {
        namedList.add(keyNamedListExists, namedList);

        namedList.add(keyListOfNamedListsNotEmpty, nestedNamedListNotEmpty);
        nestedNamedListNotEmpty.add("obj", new Object());

        namedList.add(keyStringExists, "string");
        namedList.add(keyIntExists, "8");
        namedList.add(keyBdPositive, "8");
        namedList.add(keyBdNegative, "-8");

        namedListWrapper = NamedListWrapper.create(this.namedList, message);
    }

    @Test
    public void testObjectsExist() {
        assertThat(namedListWrapper.getStringOrElseThrow(keyStringExists)).isInstanceOf(String.class);
        assertThat(namedListWrapper.getOrDefaultInteger(keyIntExists, 0)).isEqualTo(8);
        assertThat(namedListWrapper.getOrDefaultInteger(unknown, 0)).isEqualTo(0);
        assertThat(namedListWrapper.getOrDefaultBigDecimal(keyBdPositive, 0f).doubleValue()).isEqualTo(8);
        assertThat(namedListWrapper.getOrDefaultBigDecimal(unknown, 8f).doubleValue()).isEqualTo(8);
        assertThat(namedListWrapper.getOrDefaultBigDecimalAssertNotNegative(keyBdPositive, 0).doubleValue()).isEqualTo(8);
    }

    @Test
    public void testNoExceptionThrownAndAssertsAreTrue() {
        assertThat(namedListWrapper.getNamedListOrElseThrow(keyNamedListExists)).isInstanceOf(NamedListWrapper.class);
        assertThat(namedListWrapper.getListOfNamedListsAssertNotEmpty(keyListOfNamedListsNotEmpty)).isInstanceOf(List.class);
    }

    @Test
    public void testBigDecimalIsNegative() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(String.format("Property %s is not allowed to be negative", keyBdNegative));
        namedListWrapper.getOrDefaultBigDecimalAssertNotNegative(keyBdNegative, 0);
    }

    @Test
    public void testGetNamedListOrElseThrowNamedListDoesNotExist() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(message);
        namedListWrapper.getNamedListOrElseThrow(unknown);
    }

    @Test
    public void testListOfNamedListsIsEmpty() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(message);
        namedListWrapper.getListOfNamedListsAssertNotEmpty(unknown);
    }
}
