/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   16.09.2016 (thor): created
 */
package org.knime.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

/**
 * Decorator for a {@link HttpURLConnection} that is used for writing data. The default implementation doesn't send
 * a request for small amounts of data until the input stream is opened or the request status is queried. Therfore
 * this decorator returns a custom output stream through {@link #getOutputStream()} that queries the HTTP status code
 * when the stream is closed. All other operations are simply delegate to the original instances.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class HttpURLConnectionDecorator extends HttpURLConnection {
    private class OutputStreamDecorator extends OutputStream {
        private final OutputStream m_osDelegate;

        OutputStreamDecorator(final OutputStream delegate) {
            m_osDelegate = delegate;
        }

        @Override
        public void write(final byte[] b) throws IOException {
            m_osDelegate.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            m_osDelegate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            m_osDelegate.flush();
        }

        @Override
        public void close() throws IOException {
            m_osDelegate.close();
            int statusCode =  getResponseCode();
            if ((statusCode < 200) || (statusCode >= 300)) {
                throw new IOException("Server returned error " + statusCode + ": " + getResponseMessage());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int b) throws IOException {
            m_osDelegate.write(b);
        }
    }

    private final HttpURLConnection m_delegate;

    HttpURLConnectionDecorator(final HttpURLConnection delegate) {
        super(delegate.getURL());
        m_delegate = delegate;
    }

    @Override
    public String getHeaderFieldKey(final int n) {
        return m_delegate.getHeaderFieldKey(n);
    }

    @Override
    public void setFixedLengthStreamingMode(final int contentLength) {
        m_delegate.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setFixedLengthStreamingMode(final long contentLength) {
        m_delegate.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setChunkedStreamingMode(final int chunklen) {
        m_delegate.setChunkedStreamingMode(chunklen);
    }

    @Override
    public String getHeaderField(final int n) {
        return m_delegate.getHeaderField(n);
    }

    @Override
    public void connect() throws IOException {
        m_delegate.connect();
    }

    @Override
    public void setInstanceFollowRedirects(final boolean followRedirects) {
        m_delegate.setInstanceFollowRedirects(followRedirects);
    }

    @Override
    public void setConnectTimeout(final int timeout) {
        m_delegate.setConnectTimeout(timeout);
    }

    @Override
    public boolean getInstanceFollowRedirects() {
        return m_delegate.getInstanceFollowRedirects();
    }

    @Override
    public void setRequestMethod(final String rMethod) throws ProtocolException {
        m_delegate.setRequestMethod(rMethod);
    }

    @Override
    public int getConnectTimeout() {
        return m_delegate.getConnectTimeout();
    }

    @Override
    public void setReadTimeout(final int timeout) {
        m_delegate.setReadTimeout(timeout);
    }

    @Override
    public int getReadTimeout() {
        return m_delegate.getReadTimeout();
    }

    @Override
    public String getRequestMethod() {
        return m_delegate.getRequestMethod();
    }

    @Override
    public int getResponseCode() throws IOException {
        return m_delegate.getResponseCode();
    }

    @Override
    public URL getURL() {
        return m_delegate.getURL();
    }

    @Override
    public int getContentLength() {
        return m_delegate.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return m_delegate.getContentLengthLong();
    }

    @Override
    public String getContentType() {
        return m_delegate.getContentType();
    }

    @Override
    public String getContentEncoding() {
        return m_delegate.getContentEncoding();
    }

    @Override
    public long getExpiration() {
        return m_delegate.getExpiration();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return m_delegate.getResponseMessage();
    }

    @Override
    public long getDate() {
        return m_delegate.getDate();
    }

    @Override
    public long getHeaderFieldDate(final String name, final long Default) {
        return m_delegate.getHeaderFieldDate(name, Default);
    }

    @Override
    public long getLastModified() {
        return m_delegate.getLastModified();
    }

    @Override
    public void disconnect() {
        m_delegate.disconnect();
    }

    @Override
    public String getHeaderField(final String name) {
        return m_delegate.getHeaderField(name);
    }

    @Override
    public boolean usingProxy() {
        return m_delegate.usingProxy();
    }

    @Override
    public Permission getPermission() throws IOException {
        return m_delegate.getPermission();
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return m_delegate.getHeaderFields();
    }

    @Override
    public int getHeaderFieldInt(final String name, final int Default) {
        return m_delegate.getHeaderFieldInt(name, Default);
    }

    @Override
    public InputStream getErrorStream() {
        return m_delegate.getErrorStream();
    }

    @Override
    public long getHeaderFieldLong(final String name, final long Default) {
        return m_delegate.getHeaderFieldLong(name, Default);
    }

    @Override
    public Object getContent() throws IOException {
        return m_delegate.getContent();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getContent(final Class[] classes) throws IOException {
        return m_delegate.getContent(classes);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return m_delegate.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new OutputStreamDecorator(m_delegate.getOutputStream());
    }

    @Override
    public String toString() {
        return m_delegate.toString();
    }

    @Override
    public void setDoInput(final boolean doinput) {
        m_delegate.setDoInput(doinput);
    }

    @Override
    public boolean getDoInput() {
        return m_delegate.getDoInput();
    }

    @Override
    public void setDoOutput(final boolean dooutput) {
        m_delegate.setDoOutput(dooutput);
    }

    @Override
    public boolean getDoOutput() {
        return m_delegate.getDoOutput();
    }

    @Override
    public void setAllowUserInteraction(final boolean allowuserinteraction) {
        m_delegate.setAllowUserInteraction(allowuserinteraction);
    }

    @Override
    public boolean getAllowUserInteraction() {
        return m_delegate.getAllowUserInteraction();
    }

    @Override
    public void setUseCaches(final boolean usecaches) {
        m_delegate.setUseCaches(usecaches);
    }

    @Override
    public boolean getUseCaches() {
        return m_delegate.getUseCaches();
    }

    @Override
    public void setIfModifiedSince(final long ifmodifiedsince) {
        m_delegate.setIfModifiedSince(ifmodifiedsince);
    }

    @Override
    public long getIfModifiedSince() {
        return m_delegate.getIfModifiedSince();
    }

    @Override
    public boolean getDefaultUseCaches() {
        return m_delegate.getDefaultUseCaches();
    }

    @Override
    public void setDefaultUseCaches(final boolean defaultusecaches) {
        m_delegate.setDefaultUseCaches(defaultusecaches);
    }

    @Override
    public void setRequestProperty(final String key, final String value) {
        m_delegate.setRequestProperty(key, value);
    }

    @Override
    public void addRequestProperty(final String key, final String value) {
        m_delegate.addRequestProperty(key, value);
    }

    @Override
    public String getRequestProperty(final String key) {
        return m_delegate.getRequestProperty(key);
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return m_delegate.getRequestProperties();
    }
}
