package org.knime.core.node.testurl;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

import org.osgi.service.url.AbstractURLStreamHandlerService;

public final class TestURLStreamHandler extends AbstractURLStreamHandlerService {

    public static final String PROTOCOL = "knimetest";

    private static final ThreadLocal<Map<URL, Function<URL, URLConnection>>> FACTORIES = new ThreadLocal<>() {
        @Override
        protected Map<URL, Function<URL, URLConnection>> initialValue() {
            return new HashMap<>();
        }
    };

    public static void setConnectionFactory(final URL url, final Function<URL, URLConnection> factory) {
        final var factoryMap = FACTORIES.get();
        if (factory != null) {
            factoryMap.put(url, factory);
        } else {
            factoryMap.remove(url);
        }
    }

    @Override
    public URLConnection openConnection(final URL u) throws IOException {
        final var factory = FACTORIES.get().get(u);
        if (factory == null) {
            throw new IOException("No connection factory registered for URL " + u);
        }
        return factory.apply(u);
    }
}
