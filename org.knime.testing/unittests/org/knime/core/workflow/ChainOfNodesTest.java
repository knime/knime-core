/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   01.11.2008 (wiswedel): created
 */
package org.knime.core.workflow;

import org.knime.base.node.preproc.filter.column.FilterColumnNodeFactory;
import org.knime.base.node.util.sampledata.SampleDataNodeFactory;
import org.knime.base.node.viz.table.TableNodeFactory;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ChainOfNodesTest extends WorkflowTestCase {
    
    private NodeID m_dataGen;
    private NodeID m_colFilter;
    private NodeID m_tblView;
    
    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        WorkflowManager m = WorkflowManager.ROOT.createAndAddProject();
        m_dataGen = m.createAndAddNode(new SampleDataNodeFactory());
        m_colFilter = m.createAndAddNode(new FilterColumnNodeFactory());
        m_tblView = m.createAndAddNode(new TableNodeFactory());
        setManager(m);
    }
    
    public void testExecuteOneByOne() throws Exception {
        checkState(m_dataGen, State.CONFIGURED);
        checkState(m_colFilter, State.IDLE);
        checkState(m_tblView, State.IDLE);
        getManager().addConnection(m_dataGen, 0, m_colFilter, 0);
        getManager().addConnection(m_colFilter, 0, m_tblView, 0);
        checkState(m_colFilter, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_dataGen);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilter, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_colFilter);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilter, State.EXECUTED);
        checkState(m_tblView, State.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilter, State.EXECUTED);
        checkState(m_tblView, State.EXECUTED);
    }

}
