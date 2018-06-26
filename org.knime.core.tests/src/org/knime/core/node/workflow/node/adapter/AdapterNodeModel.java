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
 *   Oct 26, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow.node.adapter;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Extension of {@link NodeModel} with empty implementations. Meant to be overwritten in sub-classes for custom
 * checks.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class AdapterNodeModel extends NodeModel {

    /** Forwards to super.
     * @param nrInDataPorts ...
     * @param nrOutDataPorts ...
     */
    public AdapterNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts);
    }

    /** Forwards to super.
     * @param inPortTypes ...
     * @param outPortTypes ...
     */
    public AdapterNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (getNrInPorts() == 0 && getNrOutPorts() == 1) {
            // assume simple source node with one table output
            return new DataTableSpec[]{createDefaultOutputSpec()};
        }
        return inSpecs;
    }

    private static final DataTableSpec createDefaultOutputSpec() {
        return new DataTableSpec(
            new DataColumnSpecCreator("String-Column", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Int-Column", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Double-Column", DoubleCell.TYPE).createSpec()
        );
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        if (getNrInPorts() == 0 && getNrOutPorts() == 1) {
            // assume simple source node with one table output
            BufferedDataContainer cnt = exec.createDataContainer(createDefaultOutputSpec());
            cnt.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[] {
                new StringCell("Cell-1.1"), new IntCell(12), new DoubleCell(1.3)}));
            cnt.addRowToTable(new DefaultRow(RowKey.createRowKey(1), new DataCell[] {
                new StringCell("Cell-2.1"), new IntCell(22), new DoubleCell(2.3)}));
            cnt.addRowToTable(new DefaultRow(RowKey.createRowKey(2), new DataCell[] {
                new StringCell("Cell-3.1"), new IntCell(32), new DoubleCell(3.3)}));
            cnt.close();
            return new BufferedDataTable[] {cnt.getTable()};
        }
        return inObjects;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
    }

}
