/* 
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   17.05.2006 (Fabian Dill): created
 */
package org.knime.testing.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeStatus;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowException;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.KnimeTestRegistry;

// TODO: check, that the number of correct results corresponds to the number
// of outports of the node under test
// Solve the question about model and data in/out

/**
 * 
 */
public class KnimeTestCase extends TestCase {

    private static final NodeLogger logger =
            NodeLogger.getLogger(KnimeTestCase.class);

    private File m_knimeSettings;

    private WorkflowManager m_manager;

    private List<String> m_owners;
    
    // stores error messages seen during the run
    private TestingAppender m_errorAppender;
    
    /**
     * 
     * @param workflowFile
     */
    public KnimeTestCase(File workflowFile) {
        m_knimeSettings = workflowFile;
        this.setName(workflowFile.getParent());
        m_owners = new LinkedList<String>();
    }

    /**
     * Starts the workflow with the given settings file.
     * 
     * @throws WorkflowException
     * @throws IOException
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws InvalidSettingsException,
            CanceledExecutionException, IOException, WorkflowException {

        // start catching error messages
        m_errorAppender = new TestingAppender(Level.ERROR, Level.ERROR, 100);

        // construct the list of owners
        File ownerFile =
                new File(m_knimeSettings.getParentFile(), 
                        KnimeTestRegistry.OWNER_FILE);
        if (ownerFile.exists()) {
            FileReader fileR = new FileReader(ownerFile);
            BufferedReader r = new BufferedReader(fileR);
            String line = null;
            while ((line = r.readLine()) != null) {
                if (line.trim().length() > 0) { 
                    m_owners.add(line.trim());
                }
            }
        }

        // start here the workflow
        try {
            m_manager =
                    new WorkflowManager(m_knimeSettings,
                            new DefaultNodeProgressMonitor());
        } catch (WorkflowException ex) {
            if (ex.getNextException() != null) {
                throw ex.getNextException();
            } else {
                throw ex;
            }
        }
    }

    /**
     * 
     * @see junit.framework.TestCase#runTest()
     */
    @Override
    public void runTest() {
        logger.info("<Start> Test='"
                + m_knimeSettings.getParentFile().getName()
                + "' --------------------------------------------------------");
        String owner = "";
        if (m_owners.size() > 0) {
            StringBuilder owns = new StringBuilder();
            boolean sep = false;
            for (String o : m_owners) {
                if (sep) {
                    owns.append(",");
                }
                owns.append(o);
                sep = true;
            }
            owner = owns.toString();
        }
        logger.info("TestOwners=" + owner);
        // Collection<NodeView> views = new ArrayList<NodeView>();
        for (NodeContainer nodeCont : m_manager.getNodes()) {
            for (int i = 0; i < nodeCont.getNumViews(); i++) {
                logger.debug("opening view nr. " + i + " for node "
                        + nodeCont.getName());
                nodeCont.showView(i);
            }
        }
        try {
            m_manager.executeAll(true);
            // evaluate the results
            Collection<NodeContainer> nodes = m_manager.getNodes();
            for (NodeContainer node : nodes) {
                logger.debug(node.getName() + " isExecuted == "
                        + node.isExecuted());

                NodeStatus status = node.getStatus();

                if (!node.isExecuted()) {
                    String msg =
                            "\nNode " + node.getName() + " is not executed\n ";
                    if (status != null) {
                        msg += status.getMessage();
                    }
                    Assert.fail(msg);
                } else {
                    if (status != null && (status instanceof NodeStatus.Error)) {
                        String msg =
                                "\nNode " + node.getName()
                                        + " executed with errors: \n ";
                        msg += status.getMessage();
                        Assert.fail(msg);
                    }
                }
            }
        } finally {
            // always close these views.
            for (NodeContainer nodecont : m_manager.getNodes()) {
                nodecont.closeAllViews();
            }
        }
    }

    /**
     * Evaluates the results.
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    public void tearDown() {

        try {
            // disconnect the appender to not catch ny message anymore
            m_errorAppender.disconnect();

            if (m_errorAppender.getMessageCount() > 0) {
                String[] errMsgs = m_errorAppender.getReceivedMessages();
                for (String msg : errMsgs) {
                    logger.error("Got error: " + msg);
                }
                logger.error("Got ERROR messages during run -> FAILING! "
                        + "Check the log file.");
            }

            Assert.assertEquals(m_errorAppender.getMessageCount(), 0);
        } finally {
            logger.info("<End> Test='"
                    + m_knimeSettings.getParentFile().getName()
                    + "' ----------------------------------------------------");

        }
    }

}
