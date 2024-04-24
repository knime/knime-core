package org.knime.core.node.testurl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Test;

public class TestURLStreamHandlerTest {

    @Test
    public void testCustomUrlConnection() throws IOException {
        final var testUrl = URI.create("knimetest://foo/bar").toURL();

        assertThrows("Should not find connection factory", IOException.class, testUrl::openConnection);

        TestURLStreamHandler.setConnectionFactory(testUrl, MyURLConnection::new);
        final var conn = testUrl.openConnection();
        assertEquals("Connection class should be custom", MyURLConnection.class, conn.getClass());
        conn.connect();
        assertTrue("connect() should have been called", ((MyURLConnection)conn).m_connected);

        TestURLStreamHandler.setConnectionFactory(testUrl, null);
        assertThrows("Should not find connection factory", IOException.class, testUrl::openConnection);
    }

    private static final class MyURLConnection extends URLConnection {

        boolean m_connected;

        MyURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            m_connected = true;
        }
    }
}
