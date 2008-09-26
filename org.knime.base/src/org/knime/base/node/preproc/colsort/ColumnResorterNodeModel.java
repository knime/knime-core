/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2008
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
package org.knime.base.node.preproc.colsort;

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
    
    private boolean m_doNothing = false;
    
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
        // not configured yet -> simply add incoming columns as is
        if (m_newOrder.length == 0) {
            m_doNothing = true;
            setWarningMessage("All columns stay in same order.");
        } else {
            m_doNothing = false;
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
            } else if (incoming.size() < ordered.size()) {
                // check if in orderer are more columns than in incoming
                String[] newOrder = new String[incoming.size()];
                // remove them
                for (int i = 0, j = 0; i < m_newOrder.length; i++) {
                    if (incoming.contains(m_newOrder[i])) {
                        newOrder[j++] = m_newOrder[i];
                    }
                }
                m_newOrder = newOrder;
            }
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

        if (m_doNothing) {
            return new BufferedDataTable[] {inData[0]};
        }
        
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
