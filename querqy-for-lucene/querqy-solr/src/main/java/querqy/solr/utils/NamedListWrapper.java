package querqy.solr.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.util.NamedList;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NamedListWrapper {

    private final NamedList namedList;
    private final String exceptionMessage;

    private NamedListWrapper(final NamedList namedList, final String exceptionMessage) {
        this.namedList = namedList;
        this.exceptionMessage = exceptionMessage;
    }

    public static NamedListWrapper create(final NamedList namedList, final String exceptionMessage) {
        return new NamedListWrapper(namedList, exceptionMessage);
    }

    public NamedListWrapper getNamedListOrElseThrow(String key) {
        Object obj = namedList.get(key);
        if (!(obj instanceof NamedList)) {
            throw new IllegalArgumentException(exceptionMessage);
        }
        return new NamedListWrapper((NamedList) obj, this.exceptionMessage);
    }

    public List<NamedListWrapper> getListOfNamedListsAssertNotEmpty(String key) {
        List list = namedList.getAll(key);
        if (list.isEmpty()) {
            throw new IllegalArgumentException(this.exceptionMessage);
        }

        List<NamedListWrapper> toReturn = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof NamedList) {
                toReturn.add(new NamedListWrapper((NamedList) obj, this.exceptionMessage));
            } else {
                throw new IllegalArgumentException(this.exceptionMessage);
            }
        }

        return toReturn;

    }

    public String getStringOrElseThrow(String key) {
        final Object obj = namedList.get(key);

        if (!(obj instanceof String)) {
            throw new IllegalArgumentException(this.exceptionMessage);
        }

        String objAsString = (String) obj;
        if (StringUtils.isBlank(objAsString)) {
            throw new IllegalArgumentException(this.exceptionMessage);
        }

        return objAsString.trim();
    }

    public int getOrDefaultInteger(String key, int defaultValue) {
        try {
            final Object obj = namedList.get(key);
            if (obj == null) {
                return defaultValue;
            } else if (obj instanceof Integer) {
                return (int) obj;
            } else if (obj instanceof String) {
                return Integer.parseInt((String) obj);
            } else {
                throw new IllegalArgumentException(String.format("Property %s cannot be parsed to integer", key));
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Property %s cannot be parsed to integer", key), e);
        }
    }

    public BigDecimal getOrDefaultBigDecimalAssertNotNegative(String key, float defaultValue) {
        BigDecimal bigDecimal = getOrDefaultBigDecimal(key, defaultValue);
        if (bigDecimal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(String.format("Property %s is not allowed to be negative", key));
        }
        return bigDecimal;
    }

    public BigDecimal getOrDefaultBigDecimal(String key, float defaultValue) {
        try {
            final Object obj = namedList.get(key);
            if (obj == null) {
                return BigDecimal.valueOf(defaultValue);
            } else {
                if (obj instanceof String) {
                    return new BigDecimal((String) obj);
                } else {
                    throw new IllegalArgumentException(String.format("Property %s cannot be parsed to float", key));
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Property %s cannot be parsed to float", key), e);
        }
    }
}
