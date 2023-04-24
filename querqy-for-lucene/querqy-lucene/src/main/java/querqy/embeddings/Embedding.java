package querqy.embeddings;

import java.util.List;

public class Embedding {

    private float[] vector;

    private String commaSeparatedString;

    private Embedding(final float[] vector) {
        if (vector == null || vector.length < 1) {
            throw new IllegalArgumentException("vector must not be null or empty");
        }
        this.vector = vector;
    }
    private Embedding(final String commaSeparatedString) {
        if (commaSeparatedString == null) {
            throw new IllegalArgumentException("commaSeparatedString must not be null");
        }
        this.commaSeparatedString = commaSeparatedString;
    }

    public static Embedding of(final String commaSeparatedString) {
        return new Embedding(commaSeparatedString);
    }

    public static Embedding of(final float[] vector) {
        return new Embedding(vector);
    }

    public static Embedding of(final List<Number> vector) {
        float[] vec = new float[vector.size()];
        for (int i = 0; i < vec.length; i++) {
            vec[i] = vector.get(i).floatValue();
        }
        return new Embedding(vec);
    }

    public String asCommaSeparatedString() {
        if (commaSeparatedString == null) {
            final StringBuilder sb = new StringBuilder(vector.length * 12).append(vector[0]);
            for (int i = 1; i < vector.length; i++) {
                sb.append(",").append(vector[i]);
            }
            commaSeparatedString = sb.toString();
        }
        return commaSeparatedString;
    }

    public float[] asVector() {
        if (vector == null) {
            final String[] splits = commaSeparatedString.split(",");
            vector = new float[splits.length];
            for (int i = 0; i < splits.length; i++) {
                vector[i] = Float.parseFloat(splits[i].trim());
            }
        }
        return vector;
    }


}
