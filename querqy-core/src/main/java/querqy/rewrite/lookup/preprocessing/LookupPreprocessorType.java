package querqy.rewrite.lookup.preprocessing;

public enum LookupPreprocessorType {
    NONE("none"),
    GERMAN("german");

    private final String name;

    LookupPreprocessorType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static LookupPreprocessorType fromString(final String name) {
        for (final LookupPreprocessorType type : LookupPreprocessorType.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalArgumentException("LookupProcessorType " + name + " is not supported");
    }

}
