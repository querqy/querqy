package querqy.lucene;

import org.apache.lucene.analysis.util.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

/**
 * A {@link org.apache.lucene.analysis.util.ResourceLoader} which detects GZIP compression by its magic byte signature
 * and decompresses if necessary.
 */
public class GZIPAwareResourceLoader implements ResourceLoader {

    private final ResourceLoader delegate;

    public GZIPAwareResourceLoader(ResourceLoader delegate) {
        this.delegate = delegate;
    }

    public InputStream openResource(String resource) throws IOException {
        InputStream is = delegate.openResource(resource);
        if (is == null) {
            return null;
        } else {
            return detectGZIPAndWrap(is);
        }
    }

    // Wraps the InputStream in a GZIPInputStream if the first two bytes match the GZIP magic bytes
    private static InputStream detectGZIPAndWrap(final InputStream is) throws IOException {
        PushbackInputStream pb = new PushbackInputStream(is, 2);
        byte[] signature = new byte[2];
        int count = 0;
        try {
            while (count < 2) {
                int readCount = is.read(signature, count, 2 - count);
                if (readCount < 0) {
                    return pb;
                }
                count = count + readCount;
            }
        } finally {
            pb.unread(signature, 0, count);
        }
        int head = ((int) signature[0] & 0xff) | ((signature[1] << 8) & 0xff00);
        return GZIPInputStream.GZIP_MAGIC == head ? new GZIPInputStream(pb) : pb;
    }

    @Override
    public <T> Class<? extends T> findClass(String cname, Class<T> expectedType) {
        return delegate.findClass(cname, expectedType);
    }

    @Override
    public <T> T newInstance(String cname, Class<T> expectedType) {
        return delegate.newInstance(cname, expectedType);
    }
}
