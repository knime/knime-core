/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.node.view.table;


import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.property.hilite.HiLiteHandler;
import de.unikn.knime.core.node.tableview.TableContentModel;

/** 
 * Node Model for a table view. This class is implemented in first place to
 * comply with the model-view-controller concept. Thus, this implementation does
 * not have any further functionality than its super class
 * <code>NodeModel</code>. The content itself resides in 
 * <code>TableContentModel</code>. 
 * @author Bernd Wiswedel, University of Konstanz
 * @see de.unikn.knime.core.node.tableview.TableContentModel
 */
public class TableNodeModel extends NodeModel {

    /** Index of the input port (only one anyway). */
    protected static final int INPORT = 0;

    /** This model serves as wrapper for a TableContentModel,
     * this will be the "real" data structure. */
    private final TableContentModel m_contModel;

    /** Constructs a new (but empty) model. The model has one input port and
     * no output port.
     */
    public TableNodeModel() {
        super(1, 0);
        // models have empty content
        m_contModel = new TableContentModel();
    }

    /** 
     * Get reference to the underlying content model. This model will be the
     * same for successive calls and never be <code>null</code>.
     * @return Reference to underlying <code>TableContentModel</code>.
     */
    public TableContentModel getContentModel() {
        return m_contModel;
    }

    /** 
     * Called when new data is available. Accesses the input data table and the
     * property handler, sets them in the <code>TableContentModel</code> and 
     * starts event firing to inform listeners. Do not call this method 
     * separately, it is invoked by this model's node.
     * @param data The DataTable at the (single) input port, wrapped in 
     *        a one-dimensional array.
     * @param exec The execution monitor.
     * @return Empty table.
     * 
     * @see NodeModel#execute(DataTable[],ExecutionMonitor)
     */
    protected DataTable[] execute(
            final DataTable[] data, final ExecutionMonitor exec) {
        assert (data != null);
        assert (data.length == 1);
        final DataTable in = data[0];
        assert (in != null);
        HiLiteHandler inProp = getInHiLiteHandler(INPORT);
        m_contModel.setDataTable(in);
        m_contModel.setHiLiteHandler(inProp);
        assert (m_contModel.hasData());
        return new DataTable[0];
    }

    /** 
     * Invoked when data is reset. Removes the data from the underlying
     * <code>TableContentModel</code>. Do not call this method, it is called
     * from the node's <code>reset()</code> method.
     * @see de.unikn.knime.core.node.Node#resetNode()
     */ 
    protected void reset() {
        m_contModel.setDataTable(null);
        assert (!m_contModel.hasData());
    }
    
    /**
     * Returns <code>true</code> as this node is always executable as long
     * as it has an input (otherwise this method is never being called).
     * @see NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
        return new DataTableSpec[0];
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    protected void loadValidatedSettingsFrom(final NodeSettings settings) 
            throws InvalidSettingsException {
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings) {
    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected void validateSettings(final NodeSettings settings) 
            throws InvalidSettingsException {
    }

}
