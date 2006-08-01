/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
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

import org.knime.base.data.replace.ReplacedColumnsTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
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
        if (m_column == null
                || !data[INPORT].getDataTableSpec().containsName(m_column)) {
            return new BufferedDataTable[]{null};
        }
        DataTableSpec inSpec = data[INPORT].getDataTableSpec();
        int columnIndex = inSpec.findColumnIndex(m_column);
        // find selected column index
        // create new DataTableSpec with the added size handler
        DataColumnSpec cspec = inSpec.getColumnSpec(m_column);
        DataColumnSpecCreator dtsCont = new DataColumnSpecCreator(cspec);
        // get the domain range for the double size handler
        double minimum = ((DoubleValue)cspec.getDomain().getLowerBound())
                .getDoubleValue();
        double maximum = ((DoubleValue)cspec.getDomain().getUpperBound())
                .getDoubleValue();

        dtsCont.setSizeHandler(new SizeHandler(new SizeModelDouble(minimum,
                maximum)));
        final DataTableSpec newSpec = ReplacedColumnsTable.createTableSpec(
                inSpec, dtsCont.createSpec(), columnIndex);
        // create new Table, which returns new Spec but iterator of original
        // table
        DataTable outTable = new DataTable() {
            public DataTableSpec getDataTableSpec() {
                return newSpec;
            }

            public RowIterator iterator() {
                return data[INPORT].iterator();
            }
        };
        return new BufferedDataTable[]{exec.createBufferedDataTable(outTable,
                exec)};
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
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
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // read settings and write into the map
        m_column = settings.getString(SELECTED_COLUMN, null);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(SELECTED_COLUMN, m_column);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
}
