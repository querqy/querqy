package querqy.model.builder.converter;

@FunctionalInterface
public interface MapValueConverter {

    Object toMapValue(final Object builderValue, final MapConverter mapValueConverter);

}
