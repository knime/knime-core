/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
public class BWElimHeadNodeModel extends NodeModel implements
        LoopStartNodeTerminator {
    private int m_iteration;

    private List<String> m_inputColumns = new ArrayList<String>();

    /**
     * Creates a new model with one input and one output port.
     *
     * @param ports the number of in- and output ports
     */
    public BWElimHeadNodeModel(final int ports) {
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
                    createRearranger((BWElimTailNodeModel)getLoopEndNode(),
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
            final BWElimTailNodeModel tail, final DataTableSpec inSpec) {
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
                createRearranger((BWElimTailNodeModel)getLoopEndNode(),
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
