/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   02.02.2006 (mb): created
 */
package org.knime.base.node.viz.property.size;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.SizeHandler;
import org.knime.core.data.property.SizeModelDouble;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A node model for setting sizes using the double values of a column specified
 * using the <code>SizeManagerNodeDialog</code>.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class SizeManagerNodeModel extends NodeModel {
    /** The selected column. */
    private String m_column;

    /** Keeps port number for the single input port. */
    static final int INPORT = 0;

    /** Keeps port number for the single input port. */
    static final int OUTPORT = 0;

    /** Keeps the selected column. */
    static final String SELECTED_COLUMN = "selected_column";

    /**
     * Creates a new model for mapping sizes. The model has one input and one
     * output.
     */
    SizeManagerNodeModel() {
        super(1, 1);
    }

    /**
     * Is invoked during the node's execution to make the size settings.
     * 
     * @param data the input data array
     * @param exec the execution monitor
     * @return the same input data table whereby the DataTableSpec contains
     *         additional size infos
     * @throws CanceledExecutionException if user canceled execution
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException {
        assert (data != null && data.length == 1 && data[INPORT] != null);
        DataTableSpec inSpec = data[INPORT].getDataTableSpec();
        DataColumnSpec[] newColSpecs = new DataColumnSpec[inSpec
                .getNumColumns()];
        for (int i = 0; i < newColSpecs.length; i++) {
            DataColumnSpec cspec = inSpec.getColumnSpec(i);
            DataColumnSpecCreator dtsCont = new DataColumnSpecCreator(cspec);
            if (cspec.getName().equals(m_column)) {
                // get the domain range for the double size handler
                double minimum = 
                    ((DoubleValue)cspec.getDomain().getLowerBound())
                        .getDoubleValue();
                double maximum = 
                    ((DoubleValue)cspec.getDomain().getUpperBound())
                        .getDoubleValue();

                dtsCont.setSizeHandler(
                        new SizeHandler(new SizeModelDouble(minimum,
                        maximum)));
            }
            newColSpecs[i] = dtsCont.createSpec();
        }
        final DataTableSpec newSpec = new DataTableSpec(newColSpecs);
        BufferedDataTable changedSpecTable = exec.createSpecReplacerTable(
                data[INPORT], newSpec);
        // return original table with SizeHandler
        return new BufferedDataTable[]{changedSpecTable};
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

    /**
     * @param inSpecs the input specs passed to the output port
     * @return the same as the input spec
     * 
     * @throws InvalidSettingsException if a column is not available
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert (inSpecs.length == 1);
        if (m_column == null || !inSpecs[INPORT].containsName(m_column)) {
            throw new InvalidSettingsException("Column " + m_column
                    + " not found.");
        }
        DataColumnSpec cspec = inSpecs[INPORT].getColumnSpec(m_column);
        if (!cspec.getDomain().hasBounds()) {
            throw new InvalidSettingsException("No bounds defined for column: "
                    + m_column);
        }
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // read settings and write into the map
        m_column = settings.getString(SELECTED_COLUMN, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(SELECTED_COLUMN, m_column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
}
