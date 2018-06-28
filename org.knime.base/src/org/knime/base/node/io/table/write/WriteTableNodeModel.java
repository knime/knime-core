/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * -------------------------------------------------------------------
 *
 * History
 *   May 19, 2006 (wiswedel): created
 */
package org.knime.base.node.io.table.write;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;


/**
 * NodeModel for the node to write arbitrary tables to a file. It only shows
 * a file chooser dialog.
 * @author wiswedel, University of Konstanz
 */
public class WriteTableNodeModel extends NodeModel {
    /**
     * This is an ugly workaround for AP-8862. {@link DataContainer#writeToStream} first copies the whole table into a
     * temporary file and then starts writing it to the output stream. In case the output stream is to a server the
     * request is opened when the stream is opened but it may take quite some time before the first byte is written.
     * This leads to timeouts on the server side if the input table is larger. Therefore this decorator defers opening
     * the stream until the first byte is written.
     */
    private static class DeferredOpenOutputStream extends OutputStream {
        private final URL m_url;
        private OutputStream m_delegate;

        DeferredOpenOutputStream(final URL url) {
            m_url = url;
        }

        private OutputStream openStream() throws IOException {
            if (m_delegate != null) {
                return m_delegate;
            }

            m_delegate = FileUtil.openOutputConnection(m_url, "PUT").getOutputStream();
            return m_delegate;
        }

        @Override
        public void write(final int b) throws IOException {
            openStream().write(b);
        }

        @Override
        public void write(final byte b[], final int off, final int len) throws IOException {
            openStream().write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            openStream().flush();
        }

        @Override
        public void close() throws IOException {
            openStream().close();
        }
    }

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WriteTableNodeModel.class);

    /** Config identifier for the settings object. */
    static final String CFG_FILENAME = "filename";

    /** Config identifier for overwrite OK. */
    static final String CFG_OVERWRITE_OK = "overwriteOK";

    private final SettingsModelString m_fileName =
        new SettingsModelString(CFG_FILENAME, null);

    private final SettingsModelBoolean m_overwriteOK =
        new SettingsModelBoolean(CFG_OVERWRITE_OK, false);

    /** Creates new NodeModel with one input, no output ports. */
    public WriteTableNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileName.saveSettingsTo(settings);
        m_overwriteOK.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.validateSettings(settings);
        // must not verify overwriteOK (added in v2.1)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.loadSettingsFrom(settings);
        try {
            // property added in v2.1 -- if missing (old flow), set it to true
            m_overwriteOK.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_overwriteOK.setBooleanValue(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        checkFileAccess(m_fileName.getStringValue(), false);
        BufferedDataTable in = inData[0];

        URL url = FileUtil.toURL(m_fileName.getStringValue());
        Path localPath = FileUtil.resolveToPath(url);

        try {
            if (localPath != null) {
                DataContainer.writeToZip(in, localPath.toFile(), exec);
            } else {
                try (OutputStream os = new DeferredOpenOutputStream(url)) {
                    DataContainer.writeToStream(in, os, exec);
                }
            }
            return new BufferedDataTable[0];
        } catch (CanceledExecutionException cee) {
            if (localPath != null) {
                try {
                    Files.delete(localPath);
                    LOGGER.debug("File '" + m_fileName + "' deleted after node has been canceled.");
                } catch (IOException ex) {
                    LOGGER.warn("Unable to delete file '" + m_fileName + "' after cancellation: " + ex.getMessage(),
                        ex);
                }
            }
            throw cee;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkFileAccess(m_fileName.getStringValue(), true);

        return new DataTableSpec[0];
    }

    /**
     * Helper that checks some properties for the file argument.
     *
     * @param fileName The file to check
     * @throws InvalidSettingsException If that fails.
     */
    private void checkFileAccess(final String fileName, final boolean showWarnings)
            throws InvalidSettingsException {
        String warning = CheckUtils.checkDestinationFile(fileName, m_overwriteOK.getBooleanValue());
        if ((warning != null) && showWarnings) {
            setWarningMessage(warning);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }


}
