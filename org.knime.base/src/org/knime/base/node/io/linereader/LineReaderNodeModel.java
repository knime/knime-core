/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.linereader;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/** Model implementation of the line reader node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class LineReaderNodeModel extends NodeModel {

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
        URL url = m_config.getURL();
        BufferedDataContainer container = exec.createDataContainer(spec);
        BufferedFileReader fileReader = BufferedFileReader.createNewReader(url);
        long fileSize = fileReader.getFileSize();
        int currentRow = 0;
        final int limitRows = m_config.getLimitRowCount();
        final boolean isSkipEmpty = m_config.isSkipEmptyLines();
        String line;
        String rowPrefix = m_config.getRowPrefix();
        try {
            while ((line = fileReader.readLine()) != null) {
                String progMessage = "Reading row " + (currentRow + 1);
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
                container.addRowToTable(row);
            }
        } finally {
            container.close();
            try {
                fileReader.close();
            } catch (IOException ioe2) {
                // ignore
            }
        }
        return new BufferedDataTable[] {container.getTable()};
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
