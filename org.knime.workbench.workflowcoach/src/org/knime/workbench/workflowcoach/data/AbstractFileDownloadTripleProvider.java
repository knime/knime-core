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
 *   Apr 14, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeFrequencies;
import org.knime.core.node.NodeTriple;

/**
 * A node triple provider that downloads the nodes triples from a url and stores it to a file.
 *
 * @author Martin Horn, University of Konstanz
 */
public abstract class AbstractFileDownloadTripleProvider implements UpdatableNodeTripleProvider {

    private static final int TIMEOUT = 10000; //10 seconds

    private final String m_url;

    private final Path m_file;

    /**
     * Creates a new triple provider.
     *
     * @param url the url to download the file from
     * @param fileName the file name to store the downloaded nodes triples to - file name only, not a path!
     *
     */
    protected AbstractFileDownloadTripleProvider(final String url, final String fileName) {
        m_url = url;
        m_file = Paths.get(KNIMEConstants.getKNIMEHomeDir(), fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<NodeTriple> getNodeTriples() throws IOException {
        return NodeFrequencies.from(Files.newInputStream(m_file)).getFrequencies().stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateRequired() {
        return !Files.exists(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upate() throws Exception {
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(TIMEOUT);
        GetMethod method = new GetMethod(m_url);
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        if (Files.exists(m_file)) {
            String lastModified = getHttpDateFormat().format(Date.from(Files.getLastModifiedTime(m_file).toInstant()));
            method.setRequestHeader("If-Modified-Since", lastModified);
        }
        method.setRequestHeader("Accept-Encoding", "gzip");
        int statusCode = client.executeMethod(method);
        if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
            return;
        }
        if (statusCode != HttpStatus.SC_OK) {
            throw new HttpException("Cannot access server node recommendation file: " + method.getStatusLine());
        }

        //download and store the file
        try (InputStream in = getInputStream(method); OutputStream out = Files.newOutputStream(m_file)) {
            IOUtils.copy(in, out);
        } finally {
            method.releaseConnection();
        }
    }

    private static InputStream getInputStream(final GetMethod method) throws IOException {
        InputStream in = method.getResponseBodyAsStream();
        Header encoding = method.getResponseHeader("Content-Encoding");
        if (encoding != null && encoding.getValue().equals("gzip")) {
            in = new GZIPInputStream(in);
        }
        return in;
    }

    private static SimpleDateFormat getHttpDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        TimeZone tZone = TimeZone.getTimeZone("GMT");
        dateFormat.setTimeZone(tZone);
        return dateFormat;
    }
}
