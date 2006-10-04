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
 *   17.05.2006 (Fabian Dill): created
 */
package org.knime.testing.core;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import junit.framework.Assert;
import junit.framework.TestCase;

// TODO: check, that the number of correct results corresponds to the number
// of outports of the node under test
// Solve the question about model and data in/out

/**
 * 
 */
public class KnimeTestCase extends TestCase {

    private static final NodeLogger logger = NodeLogger
            .getLogger(KnimeTestCase.class);

    private File m_knimeSettings;

    private WorkflowManager m_manager;

    /**
     * 
     * @param workflowFile
     */
    public KnimeTestCase(File workflowFile) {
        m_knimeSettings = workflowFile;
        this.setName(workflowFile.getParent());
    }

    /**
     * Starts the workflow with the given settings file.
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() {
        // start here the workflow
        try {
            m_manager = new WorkflowManager(m_knimeSettings,
                    new DefaultNodeProgressMonitor());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidSettingsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CanceledExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 
     * @see junit.framework.TestCase#runTest()
     */
    @Override
    public void runTest() {
        // Collection<NodeView> views = new ArrayList<NodeView>();
        for (NodeContainer nodeCont : m_manager.getNodes()) {
            for (int i = 0; i < nodeCont.getNumViews(); i++) {
                logger.debug("opening view nr. " + i + " for node "
                        + nodeCont.getName());
                nodeCont.showView(i);
            }
        }
        m_manager.executeAll(true);
        // evaluate the results
        Collection<NodeContainer> nodes = m_manager.getNodes();
        for (NodeContainer node : nodes) {
            logger.debug(node.getName() + " is executed " + node.isExecuted());
            try {
                Assert.assertEquals(
                        "\nNode" + node.getName() + " is not executed\n "
                                + node.getStatus() != null ? node.getStatus()
                                .getMessage() : "", true, node.isExecuted());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (NodeContainer nodecont : m_manager.getNodes()) {
            nodecont.closeAllViews();
        }
    }

    /**
     * Evaluates the results.
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    public void tearDown() {
    }

}
