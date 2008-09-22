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
 *   Apr 28, 2008 (wiswedel): created
 */
package org.knime.base.node.meta.looper.variable;

import org.knime.base.node.flowvariable.tabletovariable.TableToVariableNodeModel;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.LoopStartNode;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class VariablesLoopHeadNodeModel extends TableToVariableNodeModel
    implements LoopStartNode {

    private int m_iteration;
    private int m_maxIterations;
    private RowIterator m_variablesIterator;
    private DataRow m_currentVariables;
    
    /** Two inputs, one output.
     * @param inOutType The type of in- and output port that passes 
     * the data through */
    public VariablesLoopHeadNodeModel(final PortType inOutType) {
        super(inOutType);
    }

    /** {@inheritDoc} */
    @Override
    protected void pushVariables(final DataTableSpec variablesSpec,
            final DataRow currentVariables) {
        super.pushVariables(variablesSpec, currentVariables);
        pushScopeVariableInt("currentIteration", m_iteration);
        pushScopeVariableInt("maxIterations", m_maxIterations);
    }
    
    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable variables = (BufferedDataTable)inData[1];
        if (m_variablesIterator == null) {
            m_variablesIterator = variables.iterator();
            m_maxIterations = variables.getRowCount();
            m_iteration = 0;
        }
        if (!m_variablesIterator.hasNext()) {
            throw new Exception("No more iterations (variables table has "
                    + variables.getRowCount() + " variable sets, i.e. rows)");
        }
        m_currentVariables = m_variablesIterator.next();
        pushVariables(variables.getDataTableSpec(), m_currentVariables);
        m_iteration += 1;
        return new PortObject[]{inData[0]};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return m_iteration >= m_maxIterations;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_variablesIterator = null;
        m_currentVariables = null;
        m_iteration = 0;
        m_maxIterations = 0;
    }

}
