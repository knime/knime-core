/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 23, 2025 (hornm): created
 */
package org.knime.core.node.agentic.tool;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.dialog.ExternalNodeData;
import org.knime.core.node.port.PortType;
import org.knime.testing.data.filestore.LargeFile;
import org.knime.testing.data.filestore.LargeFileStoreCell;

/**
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class TestNodeModel extends NodeModel {

    static final Map<String, Consumer<BufferedDataTable[]>> onExecuteMap = new HashMap<>();

    private String m_nodeKey = null;

    protected TestNodeModel(final boolean hasInput, final boolean hasOutput) {
        super(hasInput ? 1 : 0, hasOutput ? 1 : 0);
    }

    protected TestNodeModel(final PortType inPort, final PortType outPort) {
        super(inPort == null ? new PortType[0] : new PortType[]{inPort},
            outPort == null ? new PortType[0] : new PortType[]{outPort});
    }

    protected TestNodeModel(final String nodeKey) {
        super(1, 1);
        m_nodeKey = nodeKey;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (getNrOutPorts() == 1) {
            return new DataTableSpec[]{createSpec()};
        }
        return null;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        var onExecute = onExecuteMap.get(m_nodeKey);
        if (onExecute != null) {
            onExecute.accept(inData);
        }

        if (getNrInPorts() != getNrOutPorts() || m_nodeKey != null) {
            return new BufferedDataTable[]{createTable(exec)};
        }
        return inData;
    }

    static BufferedDataTable createTable(final ExecutionContext exec) {
        var dc = exec.createDataContainer(createSpec());
        dc.addRowToTable(new DefaultRow(new RowKey("1"), new StringCell("val1"), new StringCell("val2"),
            createLargeFileStoreCell(exec, "1", 42)));
        dc.addRowToTable(new DefaultRow(new RowKey("2"), new StringCell("val3"), new StringCell("val4"),
            createLargeFileStoreCell(exec, "2", 24)));
        dc.close();
        return dc.getTable();
    }

    private static LargeFileStoreCell createLargeFileStoreCell(final ExecutionContext exec, final String relativePath,
        final long seed) {
        LargeFile lf;
        try {
            final FileStore fs = exec.createFileStore(relativePath);
            lf = LargeFile.create(fs, seed, false);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return new LargeFileStoreCell(lf, seed);
    }

    private static DataTableSpec createSpec() {
        return new DataTableSpec(new DataColumnSpecCreator("col1", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("col2", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("large-file-store", LargeFileStoreCell.TYPE).createSpec());
    }

    public void validateInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        //
    }

    public void setInputData(final ExternalNodeData inputData) throws InvalidSettingsException {
        //
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("nodeKey", m_nodeKey);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_nodeKey = settings.getString("nodeKey");
    }

    @Override
    protected void reset() {
        //
    }

}
