package querqy.model.convert.converter;

@FunctionalInterface
public interface MapValueConverter {

    Object toMapValue(final Object builderValue, final MapConverterConfig mapValueConverter);

}
