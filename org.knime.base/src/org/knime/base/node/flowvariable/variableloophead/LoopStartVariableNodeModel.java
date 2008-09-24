/*
 * ------------------------------------------------------------------
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
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.flowvariable.variableloophead;

import java.io.IOException;

import org.knime.base.node.flowvariable.tablerowtovariable.TableToVariableNodeModel;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/** Start of loop: pushes variables in input datatable columns
 * onto stack, taking the values from one row per iteration.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class LoopStartVariableNodeModel extends TableToVariableNodeModel
implements LoopStartNodeTerminator {

    // remember which iteration we are in:
    private int m_currentIteration = -1;
    private int m_maxNrIterations = -1;
    
    /** One input, one output.
     */
    protected LoopStartVariableNodeModel() {
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        pushVariables((DataTableSpec)inSpecs[0], null);
        pushScopeVariableInt("maxIterations", 0);
        pushScopeVariableInt("currentIteration", 0);
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }
    
    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inPOs,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inData = (BufferedDataTable)inPOs[0];
        if (m_currentIteration == -1) {
            // first time we see this, initalize counters:
            m_currentIteration = 0;
            m_maxNrIterations = inData.getRowCount();
        } else {
            if (m_currentIteration > m_maxNrIterations) {
                throw new IOException("Loop did not terminate correctly.");
            }
        }
        // ok, not nice: iterate over table until current row is reached
        int i = 0;
        DataRow row = null;
        for (DataRow r : inData) {
            i++;
            row = r;
            if (i > m_currentIteration) {
                break;
            }
        }
        if (row == null) {
            throw new Exception("Not enough rows in input table (odd)!");
        }
        // put values for variables on stack, based on current row
        pushVariables(inData.getDataTableSpec(), row);
        // and add information about loop progress
        pushScopeVariableInt("maxIterations", m_maxNrIterations);
        pushScopeVariableInt("currentIteration", m_currentIteration);
        m_currentIteration++;
        return new PortObject[]{new FlowVariablePortObject()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return m_currentIteration >= m_maxNrIterations;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_currentIteration = -1;
        m_maxNrIterations = -1;
    }

}
