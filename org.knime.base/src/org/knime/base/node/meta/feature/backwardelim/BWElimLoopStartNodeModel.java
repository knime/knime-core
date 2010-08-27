/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   26.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This class is the model for the backward elimination head node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimLoopStartNodeModel extends NodeModel implements
        LoopStartNodeTerminator {
    private int m_iteration;

    private List<String> m_inputColumns = new ArrayList<String>();

    /**
     * Creates a new model with one input and one output port.
     *
     * @param ports the number of in- and output ports
     */
    public BWElimLoopStartNodeModel(final int ports) {
        super(ports, ports);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        for (int i = 1; i < inSpecs.length; i++) {
            if (!inSpecs[0].equalStructure(inSpecs[i])) {
                throw new InvalidSettingsException("All input tables must "
                        + "have the same structure");
            }
        }

        if (getLoopEndNode() != null) {
            ColumnRearranger crea =
                    createRearranger((BWElimLoopEndNodeModel)getLoopEndNode(),
                            inSpecs[0]);
            DataTableSpec[] outSpecs = new DataTableSpec[inSpecs.length];
            for (int i = 0; i < outSpecs.length; i++) {
                outSpecs[i] = crea.createSpec();
            }

            return outSpecs;
        } else {
            m_inputColumns.clear();
            for (DataColumnSpec cs : inSpecs[0]) {
                m_inputColumns.add(cs.getName());
            }

            return inSpecs;
        }
    }

    /**
     * Creates a column rearranger that filters all columns that are listed in
     * the passed context object.
     *
     * @param ctx the current loop context
     * @param inSpec the input table's spec
     * @return a column rearranger
     */
    private static ColumnRearranger createRearranger(
            final BWElimLoopEndNodeModel tail, final DataTableSpec inSpec) {
        ColumnRearranger crea = new ColumnRearranger(inSpec);
        List<String> remove = new ArrayList<String>(tail.excludedColumns());
        remove.add(tail.includedColumns().get(tail.excludedFeatureIndex()));
        crea.remove(remove.toArray(new String[remove.size()]));
        return crea;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        m_iteration++;
        if (m_iteration == 1) {
            // first iteration is with all columns for reference purposes
            return inData;
        }

        ColumnRearranger crea =
                createRearranger((BWElimLoopEndNodeModel)getLoopEndNode(),
                        inData[0].getDataTableSpec());
        BufferedDataTable[] outTables = new BufferedDataTable[inData.length];
        for (int i = 0; i < outTables.length; i++) {
            outTables[i] =
                    exec.createColumnRearrangeTable(inData[0], crea, exec
                            .createSubProgress(1.0 / outTables.length));
        }

        return outTables;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_iteration = 0;
        m_inputColumns.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * Returns a list with all input columns.
     *
     * @return a list with columns names
     */
    List<String> inputColumns() {
        return m_inputColumns;
    }
}
