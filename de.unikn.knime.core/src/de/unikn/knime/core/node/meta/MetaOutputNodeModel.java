/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2004
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
 * History
 *   13.06.2005 (cebron): created
 */
package de.unikn.knime.core.node.meta;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NoSettingsNodeModel;

/**
 * 
 * A model that has a getInDataTable-method instead of providing an output port.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaOutputNodeModel extends NoSettingsNodeModel {

    /*
     * Output DataTable that will be forwarded to the outport
     */
    private DataTable m_outDataTable;

    /*
     * Output DatatableSpec that is coming from the inport
     */
    private DataTableSpec m_outDataTableSpec;

    /**
     * This NodeModel has one input, but no output.
     */
    MetaOutputNodeModel() {
        super(1, 0);
    }

    /**
     * Input <code>DataTableSpec</code>s are saved internally.
     * @see de.unikn.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // assure that a output node has just one input tablespec
        assert (inSpecs.length == 1);
        m_outDataTableSpec = inSpecs[0];
        return new DataTableSpec[]{};
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#execute(DataTable[],
     * ExecutionMonitor)
     */
    protected DataTable[] execute(final DataTable[] inData,
            final ExecutionMonitor exec) throws Exception {

        // assure that a output node has just one input data table
        assert (inData.length == 1);

        m_outDataTable = inData[0];

        // returns an empty DataTable array as the output data is transfered
        // manually to the external nodes of this MetaNode
        return new DataTable[]{};
    }

    /**
     * 
     * @return <code>DataTable</code> at the end of the inner workflow.
     */
    public DataTable getOutDataTable() {
        return m_outDataTable;
    }

    /**
     * 
     * @return the output DataTableSpec at the end of the inner workflow.
     */
    public DataTableSpec getOutTableSpec() {
        return m_outDataTableSpec;
    }

    /**
     * Sets the internal <code>DataTable</code> and <code>DataTableSpec</code> 
     * to null.
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    protected void reset() {
        m_outDataTable = null;
        m_outDataTableSpec = null;
    }
}
