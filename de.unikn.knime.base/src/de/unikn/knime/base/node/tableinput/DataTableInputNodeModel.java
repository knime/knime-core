/* Created on Jun 9, 2006 1:53:05 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.base.node.tableinput;

import java.io.File;
import java.io.IOException;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionContext;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NoSettingsNodeModel;

/**
 * This class represents a silly node model that can be given a data table (and
 * its spec) which is then output upon execute of the node. This class is only
 * useful if you can get your hand on the model of course.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class DataTableInputNodeModel extends NoSettingsNodeModel {
    private DataTableSpec m_spec;

    private BufferedDataTable m_datatable;

    /**
     * Creates a new data table input model with no input ports and one data
     * output node.
     */
    public DataTableInputNodeModel() {
        super(0, 1);
    }

    /**
     * Does nothing but return the data table set by
     * {@link #setBufferedDataTable(BufferedDataTable)}.
     * 
     * @param inData the input data table array
     * @param exec the execution monitor
     * @return the datatable set by
     *         {@link #setBufferedDataTable(BufferedDataTable)}
     * @throws Exception actually, no exception is thrown
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        return new BufferedDataTable[]{m_datatable};
    }

    /**
     * Does nothing but return the data table spec set by
     * {@link #setDataTableSpec(DataTableSpec)}.
     * 
     * @param inSpecs the input specs
     * @return the datatable spec set by
     *         {@link #setDataTableSpec(DataTableSpec)}
     * @throws InvalidSettingsException actually, no exception is thrown
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{m_spec};
    }

    /**
     * Sets the data table spec that should be returned by
     * {@link #configure(DataTableSpec[])}.
     * 
     * @param spec the spec
     */
    public void setDataTableSpec(final DataTableSpec spec) {
        m_spec = spec;
    }

    /**
     * Sets the data table that should be returned by
     * {@link #execute(BufferedDataTable[], ExecutionContext)}.
     * 
     * @param table the data table
     */
    public void setBufferedDataTable(final BufferedDataTable table) {
        m_datatable = table;
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel #loadInternals(File,
     *      ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel #saveInternals(File,
     *      ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }
}
