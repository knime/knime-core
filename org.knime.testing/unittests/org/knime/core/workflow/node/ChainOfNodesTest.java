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
package org.knime.core.workflow.node;

import junit.framework.TestCase;

import org.knime.base.node.preproc.filter.column.FilterColumnNodeFactory;
import org.knime.base.node.util.sampledata.SampleDataNodeFactory;
import org.knime.base.node.viz.table.TableNodeFactory;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ChainOfNodesTest extends TestCase {
    
    public void testExecuteOneByOne() throws Exception {
        final Object semaphore = new Object();
        WorkflowManager m = WorkflowManager.ROOT.createAndAddProject();
        m.addNodeStateChangeListener(new NodeStateChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final NodeStateEvent state) {
                synchronized (semaphore) {
                    semaphore.notifyAll();
                }
            }
        });
        NodeID dataGen = m.createAndAddNode(new SampleDataNodeFactory());
        NodeID colFilter = m.createAndAddNode(new FilterColumnNodeFactory());
        NodeID tblView = m.createAndAddNode(new TableNodeFactory());
        assertTrue(Util.checkState(m, dataGen, State.CONFIGURED));
        assertTrue(Util.checkState(m, colFilter, State.IDLE));
        assertTrue(Util.checkState(m, tblView, State.IDLE));
        m.addConnection(dataGen, 0, colFilter, 0);
        m.addConnection(colFilter, 0, tblView, 0);
        assertTrue(Util.checkState(m, colFilter, State.CONFIGURED));
        assertTrue(Util.checkState(m, tblView, State.CONFIGURED));
        
        synchronized (semaphore) {
            m.executeUpToHere(dataGen);
            do {
                semaphore.wait();
            } while (m.getState().executionInProgress());
        }
        assertTrue(Util.checkState(m, dataGen, State.EXECUTED));
        assertTrue(Util.checkState(m, colFilter, State.CONFIGURED));
        assertTrue(Util.checkState(m, tblView, State.CONFIGURED));
        
        synchronized (semaphore) {
            m.executeUpToHere(colFilter);
            do {
                semaphore.wait();
            } while (m.getState().executionInProgress());
        }
        assertTrue(Util.checkState(m, dataGen, State.EXECUTED));
        assertTrue(Util.checkState(m, colFilter, State.EXECUTED));
        assertTrue(Util.checkState(m, tblView, State.CONFIGURED));
        
        synchronized (semaphore) {
            m.executeUpToHere(tblView);
            do {
                semaphore.wait();
            } while (m.getState().executionInProgress());
        }
        assertTrue(Util.checkState(m, dataGen, State.EXECUTED));
        assertTrue(Util.checkState(m, colFilter, State.EXECUTED));
        assertTrue(Util.checkState(m, tblView, State.EXECUTED));
    }

}
