/* -------------------------------------------------------------------
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
 * History
 *   Nov 13, 2007 (schweize): created
 */
package org.knime.base.node.colsort;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

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

/**
 * 
 * @author schweizer, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public class ColumnResorterNodeModel extends NodeModel {

//    private static final NodeLogger LOGGER = NodeLogger.
//    getLogger(ColumnResorterNodeModel.class);
    
    // TODO: this node could be much more intelligent:
    // if only one column moved to first or last it doesn't matter if some 
    // columns are missing in configure
    // same holds for lexicographical sorting
    
    /**
     * Settings key for the new order after resorting.
     */
    public static final String CFG_NEW_ORDER = "newOrder";
    
    
    private String[] m_newOrder = new String[] {};
    
    
    
    /**
     * Creates a new ColumnResorterNodeModel.
     */
    public ColumnResorterNodeModel() {
        super(1, 1);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check if ordered columns == input columns
        // input columns.removeAll(ordered) -> size == 0
        Set<String>incoming = new LinkedHashSet<String>();
        for (DataColumnSpec colSpec : inSpecs[0]) {
            incoming.add(colSpec.getName());
        }
        Set<String>ordered = new LinkedHashSet<String>();
        for (String c : m_newOrder) {
            ordered.add(c);
        }
        // ordered.containsall incoming
        if (!ordered.containsAll(incoming)) {
            incoming.removeAll(ordered);
            throw new InvalidSettingsException(
                    "Incoming table contains other columns "
                    + " than configured in the dialog! "
                    + incoming.toString()
                    + " Please re-configure the node.");
        }
        
        DataTableSpec inSpec = inSpecs[0];
        ColumnRearranger r = null;
        try {
            r = createRearranger(inSpec);
        } catch (IllegalArgumentException ia) {
            throw new InvalidSettingsException(ia.getMessage());
        }
      
        return new DataTableSpec[] {r.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        
        //get the original spec and create a rearranger object
        BufferedDataTable in = inData[0];
        DataTableSpec original = in.getDataTableSpec();
            ColumnRearranger rearranger = createRearranger(original);

        return new BufferedDataTable[] {exec.createColumnRearrangeTable(
                in, rearranger, exec)};
        }

    private ColumnRearranger createRearranger(final DataTableSpec original) {
        ColumnRearranger r = new ColumnRearranger(original);
        r.permute(m_newOrder);
        return r;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_newOrder = settings.getStringArray(CFG_NEW_ORDER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addStringArray(CFG_NEW_ORDER, m_newOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getStringArray(CFG_NEW_ORDER);
    }

}
