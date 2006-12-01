/* Created on Nov 23, 2006 12:07:08 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
         * TODO Thorsten?
         * @param nodeID
         * @param name
         * @param value
         * @param type
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
        System.err
                .println("Usage: "
                        + BatchExecutor.class.getName()
                        + " OPTIONS\n"
                        + "where OPTIONS can be:\n"
                        + " -nosave => do not save the workflow after execution has finished\n"
                        + " -workflowFile=... => ZIP file with a ready-to-execute workflow in the root of the ZIP\n"
                        + " -workflowDir=... => directory with a ready-to-execute workflow\n"
                        + " -destFile=... => ZIP file where the executed workflow should be written to\n"
                        + "                  if omitted the workflow is only saved in place\n"
                        + " -option=nodeID,name,value,type => set the option with name 'name' of the node with\n"
                        + "                                   ID 'nodeID' to the given value which has type 'type'\n"
                        + "                                   type can be any of the primitive Java types or String");
    }

    /**
     * Main method.
     * 
     * @param args a workflow directory or a zip input and output file
     * @throws IOException bum
     * @throws WorkflowException crash
     * @throws WorkflowInExecutionException bomb
     * @throws CanceledExecutionException ploing
     * @throws InvalidSettingsException shit
     */
    public static void main(final String[] args) throws IOException,
            InvalidSettingsException, CanceledExecutionException,
            WorkflowInExecutionException, WorkflowException {
        long t = System.currentTimeMillis();
        if (args.length < 1) {
            usage();
            System.exit(1);
        }

        File input = null, output = null;
        boolean noSave = false;
        List<Option> options = new ArrayList<Option>();

        for (String s : args) {
            String[] parts = s.split("=");
            if ("-nosave".equals(parts[0])) {
                noSave = true;
            } else if ("-workflowFile".equals(parts[0])) {
                input = new File(parts[1]);
                if (!input.isFile()) {
                    System.err.println("Workflow file '" + parts[1]
                            + "' is not a file.");
                    System.exit(1);
                }
            } else if ("-workflowDir".equals(parts[0])) {
                input = new File(parts[1]);
                if (!input.isDirectory()) {
                    System.err.println("Workflow directory '" + parts[1]
                            + "' is not a directory.");
                    System.exit(1);
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
                System.exit(1);
            }
        }

        final File workflowDir;
        if (input == null) {
            System.err.println("No input file or directory given.");
            System.exit(1);
            workflowDir = null;
        } else if (input.isFile()) {
            File dir = FileUtil.createTempDir("BatchExecutor");
            FileUtil.unzip(input, dir);
            workflowDir = dir;
        } else {
            workflowDir = input;
        }

        final WorkflowManager wfm = new WorkflowManager();
        File workflowFile = new File(input, WorkflowManager.WORKFLOW_FILE);
        wfm.load(workflowFile, new DefaultNodeProgressMonitor());

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
        System.out.println("Finished in " + (System.currentTimeMillis() - t)
                + "ms");
    }
}
