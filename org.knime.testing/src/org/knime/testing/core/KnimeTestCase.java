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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import junit.framework.TestCase;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.util.KNIMETimer;

// TODO: check, that the number of correct results corresponds to the number
// of out-ports of the node under test
// Solve the question about model and data in/out

/**
 *
 */
public class KnimeTestCase extends TestCase {

    private static final String OPTIONS_FILE = "workflow_options";

    private static final String STATUS_FILE = "node_status";

    /**
     * name of the file containing the testowner's email address.
     */
    private static final String OWNER_FILE = "owner";

    /**
     * These guys own all tests without owner file. They will be notified about
     * their failure due to missing owners.
     */
    public final static String REGRESSIONS_OWNER = "peter.ohl@uni-konstanz.de";

    private static final NodeLogger logger =
            NodeLogger.getLogger(KnimeTestCase.class);

    /**
     * The maximum runtime for a single testcase in seconds. After the timeout
     * the workflow will be canceled.
     */
    public static final int TIMEOUT = 300;

    private File m_knimeWorkFlow;

    private WorkflowManager m_manager;

    // if not null, the executed workflow si saved here.
    private final File m_saveLoc;

    // stores error messages seen during the run and the configuration of the
    // test
    private TestingConfig m_testConfig;

    /**
     *
     * @param workflowFile the workflow dir
     * @param saveLoc the dir to save the flow into after execution, or null.
     */
    public KnimeTestCase(final File workflowFile, final File saveLoc) {
        m_knimeWorkFlow = workflowFile;
        m_saveLoc = saveLoc;
        this.setName(workflowFile.getParent());
    }

    /**
     * Starts the workflow with the given settings file.
     *
     * @throws IOException
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     *
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws InvalidSettingsException,
            CanceledExecutionException, IOException {

        // registers itself as appender to the logger:
        m_testConfig = new TestingConfig(100);

        // read in the owners of the test case
        File ownerFile = new File(m_knimeWorkFlow.getParentFile(), OWNER_FILE);
        m_testConfig.setOwners(ownerFile);

        logger.info("<Start> Test='"
                + m_knimeWorkFlow.getParentFile().getName()
                + "' --------------------------------------------------------");
        // be sure to always add an owner to the log file
        String owners = m_testConfig.getOwners();
        if ((owners != null) && (owners.length() > 0)) {
            logger.info("TestOwners=" + owners);
        } else {
            logger.info("TestOwners=" + REGRESSIONS_OWNER);
            // Fail if no owner is set!
            logger.error("No owner set in test '"
                    + m_knimeWorkFlow.getParentFile().getName()
                    + "'. Please create an owner file in the test directory.");
            wrapUp();
            fail();

        }

        logger.debug("Workflow location: " + m_knimeWorkFlow.getParent()
                + " -------------------------");

        // start here the workflow
        try {
            // read in the node status file before loading the workflow.
            // this way autoexecuted nodes are captured, too.
            File statusFile =
                new File(m_knimeWorkFlow.getParentFile(), STATUS_FILE);
            m_testConfig.readNodeStatusFile(statusFile);

            logger.debug("Loading workflow ----------------------------"
                    + "--------------");

            WorkflowLoadResult loadRes = WorkflowManager.loadProject(
                    m_knimeWorkFlow.getParentFile(),
                    new ExecutionMonitor());

            boolean mustReportErrors;
            switch (loadRes.getType()) {
            case Ok:
            case Warning:
                mustReportErrors = false;
                break;
            case DataLoadError:
                mustReportErrors = loadRes.getGUIMustReportDataLoadErrors();
                break;
            default:
                mustReportErrors = true;
            }
            if (mustReportErrors) {
                logger.error(loadRes.getFilteredError(
                        "", LoadResultEntryType.Warning));
            }
            m_manager = loadRes.getWorkflowManager();
            logger.debug("Workflow loaded ----------------------------"
                    + "--------------");

            // remember the executed nodes (before executing) to not
            // complain about their warning status later.
            m_testConfig.registerExecutedNodes(m_manager);

            // construct a list of options (i.e. settings to change in the flow)
            File optionsFile =
                    new File(m_knimeWorkFlow.getParentFile(), OPTIONS_FILE);
            m_testConfig.applySettings(optionsFile, m_manager);

        } catch (IOException ex) {
            String msg = ex.getMessage();
            logger.error("I/O Error during workflow loading:"
                    + (msg == null ? "<no details>" : msg));
            wrapUp();
            fail();
        } catch (InvalidSettingsException ise) {
            String msg = ise.getMessage();
            logger.error("Invalid settings: "
                    + (msg == null ? "<no details>" : msg));
            wrapUp();
            fail();
        } catch (Throwable t) {
            String msg = t.getMessage();
            logger.error("Caught a throwable during test setup: "
                    + t.getClass().getSimpleName() + ", msg: "
                    + (msg == null ? "<no details>" : msg), t);
            wrapUp();
            fail();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runTest() {
        final Set<NodeView<? extends NodeModel>> allViews =
            new HashSet<NodeView<? extends NodeModel>>();

        // Collection<NodeView> views = new ArrayList<NodeView>();
        for (NodeContainer nodeCont : m_manager.getNodeContainers()) {
            for (int i = 0; i < nodeCont.getNrViews(); i++) {
                logger.debug("opening view nr. " + i + " for node "
                        + nodeCont.getName());
                NodeView<? extends NodeModel> view =
                    nodeCont.getView(i);
                // store the view in order to close is after the test finishes
                allViews.add(view);
                // open it now.
                view.createFrame("View #" + i);
            }
        }

        TimerTask timeout = new TimerTask() {
            @Override
            public void run() {
                String error = m_manager.toString();
                // TODO: do we get a cancelExecution() for all nodes?!?
                for (NodeContainer nc : m_manager.getNodeContainers()) {
                    m_manager.cancelExecution(nc);
                }
                logger.error("Workflow canceled after " + TIMEOUT
                        + " seconds, status follows:: ");
                logger.error(error);
            }
        };
        try {
            try {
            KNIMETimer.getInstance().schedule(timeout, TIMEOUT * 1000);

            // execute all nodes.
            logger.info("Executing workflow ----------------------");

            m_manager.executeAllAndWaitUntilDone();

            timeout.cancel();

            } catch (Throwable t) {
                String msg = t.getMessage();
                logger.error("Caught a " + t.getClass().getSimpleName()
                        + " during workflow execution:"
                        + (msg == null ? "<no details>" : msg), t);
                throw t;
            }
            try {
            // evaluate the results
            logger.info("Analyzing executed workflow ----------------------");
            /*
             * 1) make sure all nodes are executed (or nodes not supposed to
             * execute are not executed).
             */
            m_testConfig.checkNodeExecution(m_manager);

            /*
             * 2) check the status (warning and/or error) of the nodes.
             */
            m_testConfig.checkNodeStatus(m_manager);

            /*
             * the above checks only write errors into the log file - thus the
             * next step decides whether the test fails or succeeds:
             *
             * 3) make sure all expected/required messages appeared and no
             * unexpected error message showed up. (We do that always - thus we
             * let it fall through finally.)
             */
            } catch (Throwable t) {
                String msg = t.getMessage();
                logger.error("Caught a " + t.getClass().getSimpleName()
                        + " during workflow analysis:"
                        + (msg == null ? "<no details>" : msg), t);
                throw t;
            }
        } catch (Throwable t) {
            // message was printed already
        } finally {
            timeout.cancel();

            // always close these views.
            for (NodeView<? extends NodeModel> v : allViews) {
                v.closeView();
            }

            wrapUp();
        }
    }

    /**
     * wraps up execution and lets the test fail if (unexpected) error messages
     * occurred.
     */
    private void wrapUp() {

        if (m_saveLoc != null) {
            saveWorkflow();
        }
        try {
            // disconnect the appender to not catch any message anymore
            m_testConfig.disconnect();

            logger.info("Result ---------------------------------------");

            m_testConfig.checkMessages();

        } finally {
            m_testConfig.close();

            logger.info("<End> Test='"
                    + m_knimeWorkFlow.getParentFile().getName()
                    + "' ----------------------------------------------------");
        }
    }

    private void saveWorkflow() {
        logger.info("Saving workflow ------------------------------------");
        boolean doSave = true;
        if (!m_saveLoc.exists()) {
            if (!m_saveLoc.mkdirs()) {
                logger.info("Unable to create dir :"
                        + m_saveLoc + ". Not saving executed workflow.");
                doSave = false;
            }
        } else if (!m_saveLoc.isDirectory()) {
            logger.info("Can only save to a directory. This is none :"
                    + m_saveLoc + ". Not saving executed workflow.");
            doSave = false;
        }
        if (doSave) {
            try {
                m_manager.save(m_saveLoc, new ExecutionMonitor(), true);
                logger.info("Saved workflow to " + m_saveLoc.getAbsolutePath());
                logger.info("Done saving workflow ---------------------------");
            } catch (Throwable t) {
                logger.error("Caught throwable during workflow saving!");
                logger.info(t.getMessage(), t);
                logger.info("No success saving workflow ---------------------");
            }
        } else {
            logger.info("Didn't save workflow -------------------------------");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        m_manager.shutdown();
        m_manager.getParent().removeNode(m_manager.getID());
        // throw the workflow away so that the GC can do its work
        m_manager = null;
    }
}
