/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.linereader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;

/** Model implementation of the line reader node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class LineReaderNodeModel extends NodeModel {

    private NodeLogger LOGGER = NodeLogger.getLogger(LineReaderNodeModel.class);

    private LineReaderConfig m_config;

    /** No input, one output. */
    public LineReaderNodeModel() {
        super(0, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[] {createOutputSpec()};
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec spec = createOutputSpec();
        BufferedDataTableRowOutput output = new BufferedDataTableRowOutput(exec.createDataContainer(spec));
        readLines(new PortInput[0], new PortOutput[] {output}, exec, false);
        return new BufferedDataTable[] {output.getDataTable()};
    }

    private DataTableSpec createOutputSpec() throws InvalidSettingsException {
        if (m_config == null) {
            throw new InvalidSettingsException("No configuration available");
        }
        String colName = m_config.getColumnHeader();
        DataColumnSpecCreator creator =
            new DataColumnSpecCreator(colName, StringCell.TYPE);
        URL url = m_config.getURL();
        String path = url.getPath();
        String name = "";
        if (path != null) {
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash >= 0 && lastSlash + 1 < path.length()) {
                name = path.substring(lastSlash + 1);
            }
        }
        return new DataTableSpec(name, creator.createSpec());
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {

        return new StreamableOperator() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
                readLines(inputs, outputs, exec, true);
            }
        };
    }

    protected void readLines(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec, final boolean isStreamingMode) throws Exception {
        RowOutput output = (RowOutput)outputs[0];
        URL url = m_config.getURL();
        BufferedFileReader fileReader = BufferedFileReader.createNewReader(url);
        long fileSize = fileReader.getFileSize();
        long currentRow = 0;
        final int limitRows = m_config.getLimitRowCount();
        boolean isSkipEmpty = m_config.isSkipEmptyLines();
        String line;
        String rowPrefix = m_config.getRowPrefix();
        try {
            if (!m_config.isOutputOnlyNewLines()) {
                while ((line = fileReader.readLine()) != null) {
                    final long currentRowFinal = currentRow;
                    Supplier<String> progMessage = () -> "Reading row " + (currentRowFinal + 1);
                    if (fileSize > 0) {
                        long numberOfBytesRead = fileReader.getNumberOfBytesRead();
                        double prog = (double)numberOfBytesRead / fileSize;
                        exec.setProgress(prog, progMessage);
                    } else {
                        exec.setMessage(progMessage);
                    }
                    exec.checkCanceled();
                    if (isSkipEmpty && line.trim().length() == 0) {
                        // do not increment currentRow
                        continue;
                    }
                    if (limitRows > 0 && currentRow >= limitRows) {
                        setWarningMessage("Read only " + limitRows
                                + " row(s) due to user settings.");
                        break;
                    }
                    RowKey key = new RowKey(rowPrefix + (currentRow++));
                    DefaultRow row = new DefaultRow(key, new StringCell(line));
                    output.push(row);
                }
            }
        } finally {
            try {
                fileReader.close();
            } catch (IOException ioe2) {
                // ignore
            }

            if (isStreamingMode && m_config.isWatchFile()) {
                TailerListener listener = new LineReaderTailerListener(output, isSkipEmpty, rowPrefix, currentRow);
                Tailer tailer = null;
                try {
                    Semaphore mutex = new Semaphore(1);
                    mutex.acquire();
                    exec.setMessage("Start watching the file");
                    tailer = Tailer.create(new File(url.toURI()), listener, 1000, true);
                    mutex.acquire();
                } finally {
                    output.close();
                    if (tailer != null) {
                        tailer.stop();
                    }
                }
            } else {
                output.close();
            }
        }
    }

    private class LineReaderTailerListener extends TailerListenerAdapter {
        private RowOutput m_output;
        private final boolean m_isSkipEmpty;
        private final String m_rowPrefix;
        private long m_currentRow;
        private LineReaderTailerListener(final RowOutput output, final boolean isSkipEmpty,
            final String rowPrefix, final long currentRow) {
            m_output = output;
            m_isSkipEmpty = isSkipEmpty;
            m_rowPrefix = rowPrefix;
            m_currentRow = currentRow;
        }

        @Override
        public void handle(final String line) {
            if (m_isSkipEmpty && line.trim().length() == 0) {
                return;
            }
            RowKey key = new RowKey(m_rowPrefix + (m_currentRow++));
            DefaultRow row = new DefaultRow(key, new StringCell(line));
            try {
                m_output.push(row);
            } catch (InterruptedException e) {
                LOGGER.debug(e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            m_config.saveConfiguration(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new LineReaderConfig().loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        LineReaderConfig c = new LineReaderConfig();
        c.loadConfigurationInModel(settings);
        m_config = c;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

}
