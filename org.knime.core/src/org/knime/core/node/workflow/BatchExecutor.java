/* Created on Nov 23, 2006 12:07:08 PM by thor
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.util.FileUtil;

/**
 * Simple utility class that takes a workflow, either in a directory or zipped
 * into a single file, executes it and saves the results in the end. If the
 * input was a ZIP file the workflow is zipped back into a file.
 * 
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class BatchExecutor {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(BatchExecutor.class);

    private static class Option {
        private final int m_nodeID;

        private final String m_name;

        private final String m_value;

        private final String m_type;
        /**
         * Create new <code>Option</code>.
         * @param nodeID node ID
         * @param name name
         * @param value value
         * @param type type
         */
        Option(final int nodeID, final String name, final String value,
                final String type) {
            m_nodeID = nodeID;
            m_name = name;
            m_value = value;
            m_type = type;
        }
    }

    private BatchExecutor() { /**/
    }

    private static void usage() {
        System.err.println(
              "Usage: The following options are available:\n"
            + " -nosave => do not save the workflow after execution has finished\n"
            + " -reset => reset workflow prior to execution\n"
            + " -workflowFile=... => ZIP file with a ready-to-execute workflow in the root of the ZIP\n"
            + " -workflowDir=... => directory with a ready-to-execute workflow\n"
            + " -destFile=... => ZIP file where the executed workflow should be written to\n"
            + "                  if omitted the workflow is only saved in place\n"
            + " -option=nodeID,name,value,type => set the option with name 'name' of the node with\n"
            + "                                   ID 'nodeID' to the given value which has type 'type'\n"
            + "                                   type can be any of the primitive Java types or String\n"
            + "\n"
            + "Some KNIME settings can also be adjusted by Java properties:\n"
            + " -Dorg.knime.core.maxThreads=n => sets the maximum number of threads used by KNIME\n");
    }

    /**
     * Main method.
     * 
     * @param args a workflow directory or a zip input and output file
     * @throws IOException Delegated from WFM
     * @throws WorkflowException Delegated from WFM
     * @throws WorkflowInExecutionException Delegated from WFM
     * @throws CanceledExecutionException Delegated from WFM
     * @throws InvalidSettingsException Delegated from WFM
     */
    public static void main(final String[] args) throws IOException,
            InvalidSettingsException, CanceledExecutionException,
            WorkflowInExecutionException, WorkflowException {
        int returnVal = mainRun(args);
        System.exit(returnVal);
    }
    
    /** Called from {@link #main(String[])} method. It parses the command line
     * and starts up KNIME. It returns 0 if the execution was run (even with
     * errors) and 1 if the command line could not be parsed (e.g. usage was
     * printed).
     * 
     * @param args Command line arguments
     * @return 0 if WorkflowManager (WFM) was executed, 1 otherwise.
     * @throws IOException Delegated from WFM
     * @throws InvalidSettingsException Delegated from WFM
     * @throws CanceledExecutionException Delegated from WFM
     * @throws WorkflowInExecutionException Delegated from WFM
     * @throws WorkflowException Delegated from WFM
     */
    public static int mainRun(final String[] args) throws IOException,
        InvalidSettingsException, CanceledExecutionException,
        WorkflowInExecutionException, WorkflowException {
        long t = System.currentTimeMillis();
        if (args.length < 1) {
            usage();
            return 1;
        }

        File input = null, output = null;
        boolean noSave = false;
        boolean reset = false;
        List<Option> options = new ArrayList<Option>();

        for (String s : args) {
            String[] parts = s.split("=");
            if ("-nosave".equals(parts[0])) {
                noSave = true;
            } else if ("-reset".equals(parts[0])) {
                reset = true;
            } else if ("-workflowFile".equals(parts[0])) {
                input = new File(parts[1]);
                if (!input.isFile()) {
                    System.err.println("Workflow file '" + parts[1]
                            + "' is not a file.");
                    return 1;
                }
            } else if ("-workflowDir".equals(parts[0])) {
                input = new File(parts[1]);
                if (!input.isDirectory()) {
                    System.err.println("Workflow directory '" + parts[1]
                            + "' is not a directory.");
                    return 1;
                }
            } else if ("-destFile".equals(parts[0])) {
                output = new File(parts[1]);
            } else if ("-option".equals(parts[0])) {
                String[] parts2 = parts[1].split("\\,");
                int nodeID = Integer.parseInt(parts2[0]);
                String optionName = parts2[1];
                String value = parts2[2];
                String type = parts2[3];

                options.add(new Option(nodeID, optionName, value, type));
            } else {
                System.err.println("Unknown option '" + parts[0] + "'");
                usage();
                return 1;
            }
        }

        final File workflowDir;
        if (input == null) {
            System.err.println("No input file or directory given.");
            return 1;
        } else if (input.isFile()) {
            File dir = FileUtil.createTempDir("BatchExecutor");
            FileUtil.unzip(input, dir);
            workflowDir = dir;
        } else {
            workflowDir = input;
        }

        final WorkflowManager wfm = new WorkflowManager();
        File workflowFile =
                new File(workflowDir, WorkflowManager.WORKFLOW_FILE);
        if (!workflowFile.exists()) {
            workflowFile =
                    new File(workflowDir.listFiles()[0],
                            WorkflowManager.WORKFLOW_FILE);
        }

        wfm.load(workflowFile, new DefaultNodeProgressMonitor());
        if (reset) {
            wfm.resetAndConfigureAll();
        }

        for (Option o : options) {
            NodeContainer cont = wfm.getNodeContainerById(o.m_nodeID);
            if (cont == null) {
                LOGGER.warn("No node with id " + o.m_nodeID + " found.");
            } else {
                NodeSettings settings = new NodeSettings("something");
                cont.saveSettings(settings);

                if ("int".equals(o.m_type)) {
                    settings.getNodeSettings(Node.CFG_MODEL).addInt(o.m_name,
                            Integer.parseInt(o.m_value));
                } else if ("short".equals(o.m_type)) {
                    settings.getNodeSettings(Node.CFG_MODEL).addShort(o.m_name,
                            Short.parseShort(o.m_value));
                } else if ("byte".equals(o.m_type)) {
                    settings.getNodeSettings(Node.CFG_MODEL).addByte(o.m_name,
                            Byte.parseByte(o.m_value));
                } else if ("boolean".equals(o.m_type)) {
                    settings.getNodeSettings(Node.CFG_MODEL).addBoolean(
                            o.m_name, Boolean.parseBoolean(o.m_value));
                } else if ("char".equals(o.m_type)) {
                    settings.getNodeSettings(Node.CFG_MODEL).addChar(o.m_name,
                            o.m_value.charAt(0));
                } else if ("float".equals(o.m_type)
                        || ("double".equals(o.m_type))) {
                    settings.getNodeSettings(Node.CFG_MODEL).addDouble(
                            o.m_name, Double.parseDouble(o.m_value));
                } else if ("String".equals(o.m_type)) {
                    settings.getNodeSettings(Node.CFG_MODEL).addString(
                            o.m_name, o.m_value);
                } else {
                    throw new IllegalArgumentException("Unknown option type '"
                            + o.m_type + "'");                   
                }
                cont.loadSettings(settings);
            }
        }

        wfm.executeAll(true);
        if (!noSave) {
            wfm.save(workflowFile, new DefaultNodeProgressMonitor());

            if (output != null) {
                FileUtil.zipDir(output, workflowDir, 9);
            }
        }
        System.out.println(
                "Finished in " + (System.currentTimeMillis() - t) + "ms");
        return 0;
    }
}
