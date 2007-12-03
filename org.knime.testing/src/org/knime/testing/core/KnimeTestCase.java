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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeStatus;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowException;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.KNIMETimer;

// TODO: check, that the number of correct results corresponds to the number
// of out-ports of the node under test
// Solve the question about model and data in/out

/**
 * 
 */
public class KnimeTestCase extends TestCase {

    private static final String OPTIONS_FILE = "workflow_options";

    /*
     * if this pattern occurs in a value of an option, it's replaced with the
     * directory of the options file
     */
    private static final String LOCATION = "$$location$$";
    
    private static final String LOCATIONURL = "$$locationURL$$";

    private static final NodeLogger logger =
            NodeLogger.getLogger(KnimeTestCase.class);

    /**
     * The message inserted if the test fails due to error messages (analyzers
     * will parse for it).
     */
    public static final String ERR_FAIL_MSG = "Got ERRORs during run";

    /**
     * The message inserted if the test fails due to excpetions (analyzers will
     * parse for it).
     */
    public static final String EXCEPT_FAIL_MSG = "Got EXCEPTIONs during run";

    /**
     * The maximum runtime for a single testcase in seconds. After the timeout
     * the workflow will be canceled.
     */
    public static final int TIMEOUT = 300;

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
        m_errorAppender = new TestingAppender(Level.WARN, Level.ERROR, 100);

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
            logger.info("TestOwners=" + owner);
        } else {
            // Tests with no owner set are failing now!
            logger.error("No owner set in test '"
                    + m_knimeSettings.getParentFile().getName()
                    + "'. Please create an owner file in the test directory.");
            wrapUp();
            fail();

        }


        // start here the workflow
        try {
            m_manager =
                    new WorkflowManager(m_knimeSettings,
                            new DefaultNodeProgressMonitor());

            // construct a list of options (i.e. settings to change in the flow)
            File optionsFile =
                    new File(m_knimeSettings.getParentFile(), OPTIONS_FILE);
            if (optionsFile.exists()) {
                applyChanges(optionsFile, m_manager);
            }

        } catch (WorkflowException ex) {
            String msg = ex.getMessage();
            logger.error("Error during workflow loading:"
                    + (msg == null ? "<no details>" : msg));
            wrapUp();
            fail();
        } catch (InvalidSettingsException ise) {
            String msg = ise.getMessage();
            logger.error("Invalid settings in options file:"
                    + (msg == null ? "<no details>" : msg));
            wrapUp();
            fail();
        } catch (Throwable t) {
            String msg = t.getMessage();
            logger.error("Caught a throwable during workflow loading:"
                    + (msg == null ? "<no details>" : msg));
            wrapUp();
            fail();
        }
    }

    /**
     * {@inheritDoc}
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

        TimerTask timeout = new TimerTask() {
            @Override
            public void run() {
                m_manager.cancelExecution();
                logger.error("Workflow canceled after " + TIMEOUT + " seconds");
            }
        };
        try {
            KNIMETimer.getInstance().schedule(timeout, TIMEOUT * 1000);
            m_manager.executeAll(true);
            timeout.cancel();
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
                    // make sure to log the reason for failure.
                    logger.error(msg);
                    Assert.fail(msg);
                } else {
                    if (status != null && (status instanceof NodeStatus.Error)) {
                        String msg =
                                "\nNode " + node.getName()
                                        + " executed with errors: \n ";
                        msg += status.getMessage();
                        // make sure to log the reason for failure.
                        logger.error(msg);
                        Assert.fail(msg);
                    }
                }
            }
        } catch (Throwable t) {
            String msg = t.getMessage();
            logger.error("Caught a throwable during workflow loading:"
                    + (msg == null ? "<no details>" : msg));
        } finally {
            timeout.cancel();
            // always close these views.
            for (NodeContainer nodecont : m_manager.getNodes()) {
                nodecont.closeAllViews();
            }

            // we have a method wrapUp instead of tearDown(), because tearDown
            // is not reliably called. We always call wrapUp.
            wrapUp();
        }
    }

    /**
     * Evaluates the results.
     */
    private void wrapUp() {

        try {
            // disconnect the appender to not catch ny message anymore
            m_errorAppender.disconnect();

            boolean testFails = false;

            if (m_errorAppender.getMessageCount() > 0) {
                String[] errMsgs = m_errorAppender.getReceivedMessages();
                for (String msg : errMsgs) {
                    logger.error("Got error: " + msg);
                }
                logger.error(ERR_FAIL_MSG + " -> FAILING! "
                        + "Check the log file.");
                testFails = true;
            }
            if (m_errorAppender.getExceptionsCount() > 0) {
                String[] excMsgs = m_errorAppender.getExceptions();
                for (String e : excMsgs) {
                    logger.error("Got exception: " + e);
                }
                logger.error(EXCEPT_FAIL_MSG + " -> FAILING! "
                        + "Check the log file.");
                testFails = true;
            }
            if (testFails) {
                fail("Failing due to errors or exceptions in the log file.");
            }
            m_manager = null; // throw it away so that the GC can do its work
        } finally {
            m_errorAppender.close();
            logger.info("<End> Test='"
                    + m_knimeSettings.getParentFile().getName()
                    + "' ----------------------------------------------------");

        }
    }

    /**
     * reads in each line of the options file, parses it, replaces $$location$$
     * in each value with the location of the options file and applies the
     * changes to the workflow settings.
     */
    private void applyChanges(final File optionsFile, final WorkflowManager wfm)
            throws FileNotFoundException, InvalidSettingsException, IOException {

        BufferedReader optfile =
                new BufferedReader(new FileReader(optionsFile));

        String location = optionsFile.getParent();
        if (location == null) {
            throw new FileNotFoundException(
                    "Couldn't determine location of options file.");
        }
        location = location.replace('\\', '/');
        String locURL = new File(location).toURI().toString();
        if (locURL.endsWith("/")) {
            locURL = locURL.substring(0, locURL.length() - 1);
        }
        List<Option> options = new LinkedList<Option>();

        String line;
        while ((line = optfile.readLine()) != null) {
            try {
                String[] parts = line.split("\\,");
                String[] nodeIDPath = parts[0].split("/");
                int[] nodeIDs = new int[nodeIDPath.length];
                for (int i = 0; i < nodeIDs.length; i++) {
                    nodeIDs[i] = Integer.parseInt(nodeIDPath[i]);
                }
                String optionName = parts[1];
                String value = parts[2];
                String type = parts[3];

                // locationURL is part of location! Make sure to test it first!
                if (value.contains(LOCATIONURL)) {
                    value = value.replace(LOCATIONURL, locURL);
                } else if (value.contains(LOCATION)) {
                    value = value.replace(LOCATION, location);
                }
                options.add(new Option(nodeIDs, optionName, value, type));
            } catch (ArrayIndexOutOfBoundsException aobe) {
                throw new InvalidSettingsException("Invalid options line, "
                        + "in File '" + optionsFile + "', line: " + line);
            }
        }
        if (options.size() <= 0) {
            throw new InvalidSettingsException(
                    "Options file with no settings to change. "
                            + "Please delete the file or edit it (in the "
                            + "workflow dir file '" + OPTIONS_FILE + "').");
        }

        applySettingsModifications(options, wfm);

    }

    private void applySettingsModifications(final List<Option> options,
            final WorkflowManager wfm) throws InvalidSettingsException {

        for (Option o : options) {
            int[] idPath = o.m_nodeIDs;
            NodeContainer cont = wfm.getNodeContainerById(idPath[0]);
            for (int i = 1; i < idPath.length; i++) {
                cont =
                        cont.getEmbeddedWorkflowManager().getNodeContainerById(
                                idPath[i]);
            }
            if (cont == null) {
                logger.error("Can't modify settings: No node with id "
                        + Arrays.toString(idPath) + " found.");
            } else {
                NodeSettings settings = new NodeSettings("something");
                cont.saveSettings(settings);
                NodeSettings model = settings.getNodeSettings(Node.CFG_MODEL);
                String[] splitName = o.m_name.split("/");
                String name = splitName[splitName.length - 1];
                String[] pathElements = new String[splitName.length - 1];
                System.arraycopy(splitName, 0, pathElements, 0,
                        pathElements.length);
                for (String s : pathElements) {
                    model = model.getNodeSettings(s);
                }

                if ("int".equals(o.m_type)) {
                    model.addInt(name, Integer.parseInt(o.m_value));
                } else if ("short".equals(o.m_type)) {
                    model.addShort(name, Short.parseShort(o.m_value));
                } else if ("byte".equals(o.m_type)) {
                    model.addByte(name, Byte.parseByte(o.m_value));
                } else if ("boolean".equals(o.m_type)) {
                    model.addBoolean(name, Boolean.parseBoolean(o.m_value));
                } else if ("char".equals(o.m_type)) {
                    model.addChar(name, o.m_value.charAt(0));
                } else if ("float".equals(o.m_type)
                        || ("double".equals(o.m_type))) {
                    model.addDouble(name, Double.parseDouble(o.m_value));
                } else if ("String".equals(o.m_type)) {
                    model.addString(name, o.m_value);
                } else if ("StringCell".equals(o.m_type)) {
                    model.addDataCell(name, new StringCell(o.m_value));
                } else if ("DoubleCell".equals(o.m_type)) {
                    double d = Double.parseDouble(o.m_value);
                    model.addDataCell(name, new DoubleCell(d));
                } else if ("IntCell".equals(o.m_type)) {
                    int i = Integer.parseInt(o.m_value);
                    model.addDataCell(name, new IntCell(i));
                } else {
                    throw new IllegalArgumentException("Unknown option type '"
                            + o.m_type + "'");
                }
                cont.loadSettings(settings);

                logger.info("Applied settings change: " + o);
            }
        }

    }

    private static class Option {
        private final int[] m_nodeIDs;

        private final String m_name;

        private final String m_value;

        private final String m_type;

        /**
         * Create new <code>Option</code>.
         * 
         * @param nodeIDs node IDs, mostly one element, more for nested flows
         * @param name name
         * @param value value
         * @param type type
         */
        Option(final int[] nodeIDs, final String name, final String value,
                final String type) {
            m_nodeIDs = nodeIDs;
            m_name = name;
            m_value = value;
            m_type = type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Node#");
            boolean first = true;
            for (int id : m_nodeIDs) {
                if (!first) {
                    result.append(".");
                }
                result.append(id);
                first = false;
            }
            result.append(": ");
            result.append(m_name);
            result.append(" = ");
            result.append(m_value);
            return result.toString();
        }
    }

}
