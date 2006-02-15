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
 * A <code>NodeModel</code> that has a <code>setInDataTable</code> and
 * <code>setInTableSpec</code>-method instead of providing an input port.
 * 
 * @author cebron, University of Konstanz
 */
public class MetaInputNodeModel extends NoSettingsNodeModel {
    
    /*
     * Input Datatable that will be forwarded to the outport
     */
    private DataTable m_inDataTable;
    
    /*
     * Input DatatableSpec that will be forwarded to the outport
     */
    private DataTableSpec m_inDataTableSpec;
    
    /**
     * No input, but one output. 
     */
    MetaInputNodeModel() {
        super(0, 1);
    }

    /**
     * If an input <code>DataTableSpec</code> has been set, it will be 
     * forwarded to the outport of this node.
     *
     * @see de.unikn.knime.core.node.NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        if (m_inDataTableSpec != null) {
            return new DataTableSpec[] {m_inDataTableSpec};
        }
        
        // if there is none at the moment
        throw new InvalidSettingsException("No Input Spec available");
    }

    /**
     * If the input <code>DataTable</code> has been set, it will be returned
     * during execution.
     * 
     * @see de.unikn.knime.core.node.NodeModel#execute(DataTable[],
     *      ExecutionMonitor)
     */
    protected DataTable[] execute(final DataTable[] inData,
                                  final ExecutionMonitor exec)
            throws Exception {
        return new DataTable[] {m_inDataTable};
    }

    /**
     * Set the input <code>DataTable</code>.
     * @param dt the input <code>DataTable</code>
     * @throws NullPointerException if a null <code>DataTable</code> is passed
     * as an argument.
     */
    public void setInDataTable(final DataTable dt) {
        if (dt == null) {
            throw new NullPointerException("Input DataTable must not be null!");
        }
        m_inDataTable = dt;
    } 
    
    /**
     * Set the input <code>DataTableSpec</code>.
     * @param spec the input <code>DataTableSpec</code>
     * @throws NullPointerException if a null <code>DataTableSpec</code> is 
     * passed as an argument.
     */
    public void setInTableSpec(final DataTableSpec spec) {
        if (spec == null) {
            throw new NullPointerException("Input DataTableSpec" 
                    + " must not be null!");
        }
        m_inDataTableSpec = spec;
    } 
    
    /**
     * Input <code>DataTable</code> is set to null.
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    protected void reset() {
        m_inDataTable = null;
    }
}
