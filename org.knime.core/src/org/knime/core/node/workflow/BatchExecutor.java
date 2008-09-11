/* Created on Nov 23, 2006 12:07:08 PM by thor
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- *
 */
package org.knime.core.node.workflow;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.util.StringFormat;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.core.util.FileUtil;
import org.knime.core.util.KnimeEncryption;

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
        private final int[] m_nodeIDs;

        private final String m_name;

        private final String m_value;

        private final String m_type;
        /**
         * Create new <code>Option</code>.
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
    }

    private BatchExecutor() { /**/
    }

    private static void usage() {
        System.err.println(
              "Usage: The following options are available:\n"
            + " -nosave => do not save the workflow after execution has finished\n"
            + " -reset => reset workflow prior to execution\n"
            + " -masterkey[=...] => prompt for master passwort (used in e.g. database nodes),\n"
            + "                 if provided with argument, use argument instead of prompting\n"
            + " -workflowFile=... => ZIP file with a ready-to-execute workflow in the root \n"
            + "                  of the ZIP\n"
            + " -workflowDir=... => directory with a ready-to-execute workflow\n"
            + " -destFile=... => ZIP file where the executed workflow should be written to\n"
            + "                  if omitted the workflow is only saved in place\n"
            + " -option=nodeID,name,value,type => set the option with name 'name' of the node\n"
            + "                  with ID 'nodeID' to the given 'value', which has type 'type'.\n"
            + "                  'type' can be any of the primitive Java types, \"String\"\n"
            + "                  or any of \"StringCell\", \"DoubleCell\" or \"IntCell\".\n"
            + "                  If 'name' addresses a nested element (for instance \n"
            + "                  \"rowFilter\" -> \"ColValRowFilterUpperBound\"), the entire\n"
            + "                  path must be given, separated by \"/\".\n"
            + "                  If the node is part of a meta node, provide also the node\n"
            + "                  ids of the parent node(s), e.g. 90/56.\n"
            + "\n"
            + "Some KNIME settings can also be adjusted by Java properties;\n"
            + "they need to be provided as last option in the command line:\n"
            + " -vmargs -Dorg.knime.core.maxThreads=n => sets the maximum\n"
            + "                  number of threads used by KNIME\n");
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
        boolean isPromptForPassword = false;
        String masterKey = null;
        List<Option> options = new ArrayList<Option>();

        for (String s : args) {
            String[] parts = s.split("=", 2);
            if ("-nosave".equals(parts[0])) {
                noSave = true;
            } else if ("-reset".equals(parts[0])) {
                reset = true;
            } else if ("-masterkey".equals(parts[0])) {
                if (parts.length > 1) {
                    if (parts[1].length() == 0) {
                        System.err.println("Master key must not be empty.");
                        return 1;
                    }
                    masterKey = parts[1];
                } else {
                    isPromptForPassword = true;
                }
            } else if ("-workflowFile".equals(parts[0])) {
                if (parts.length != 2) {
                    System.err.println(
                            "Couldn't parse -workflowFile argument: " + s);
                    return 1;
                }
                input = new File(parts[1]);
                if (!input.isFile()) {
                    System.err.println("Workflow file '" + parts[1]
                            + "' is not a file.");
                    return 1;
                }
            } else if ("-workflowDir".equals(parts[0])) {
                if (parts.length != 2) {
                    System.err.println(
                            "Couldn't parse -workflowDir argument: " + s);
                    return 1;
                }
                input = new File(parts[1]);
                if (!input.isDirectory()) {
                    System.err.println("Workflow directory '" + parts[1]
                            + "' is not a directory.");
                    return 1;
                }
            } else if ("-destFile".equals(parts[0])) {
                if (parts.length != 2) {
                    System.err.println(
                            "Couldn't parse -destFile argument: " + s);
                    return 1;
                }
                output = new File(parts[1]);
            } else if ("-option".equals(parts[0])) {
                if (parts.length != 2) {
                    System.err.println(
                            "Couldn't parse -option argument: " + s);
                    return 1;
                }
                String[] parts2;
                try {
                    parts2 = splitOption(parts[1]);
                } catch (IndexOutOfBoundsException ex) {
                    System.err.println(
                            "Couldn't parse -option argument: " + s);
                    return 1;
                }
                String[] nodeIDPath = parts2[0].split("/");
                int[] nodeIDs = new int[nodeIDPath.length];
                for (int i = 0; i < nodeIDs.length; i++) {
                    nodeIDs[i] = Integer.parseInt(nodeIDPath[i]);
                }
                String optionName = parts2[1];
                String value = parts2[2];
                String type = parts2[3];

                options.add(new Option(nodeIDs, optionName, value, type));
            } else {
                System.err.println("Unknown option '" + parts[0] + "'");
                usage();
                return 1;
            }
        }
        setupEncryptionKey(isPromptForPassword, masterKey);

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

        WorkflowLoadResult loadResult = WorkflowManager.loadProject(
                workflowDir, new ExecutionMonitor());
        WorkflowManager wfm = loadResult.getWorkflowManager();

        if (reset) {
            wfm.resetAll();
        }

        setNodeOptions(options, wfm);

        System.out.println(wfm.printNodeSummary(wfm.getID(), 0));
        boolean successful = wfm.executeAllAndWaitUntilDone();
        if (!noSave) {
            wfm.save(workflowDir, new ExecutionMonitor(), true);

            if (output != null) {
                FileUtil.zipDir(output, workflowDir, 9);
            }
        }
        // Get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis() - t;
        String niceTime = StringFormat.formatElapsedTime(elapsedTimeMillis);
        String timeString = ("Finished in " + niceTime
                + " (" + elapsedTimeMillis + "ms)");
        System.out.println(timeString);
        return successful ? 0 : 1;
    }

    private static void setNodeOptions(final List<Option> options,
            final WorkflowManager wfm) throws InvalidSettingsException {
        for (Option o : options) {
            int[] idPath = o.m_nodeIDs;
            NodeID subID = new NodeID(wfm.getID(), idPath[0]);
            NodeContainer cont = wfm.getNodeContainer(subID);
            for (int i = 1; i < idPath.length; i++) {
                if (cont instanceof WorkflowManager) {
                    WorkflowManager subWM = (WorkflowManager)cont;
                    subID = new NodeID(subID, idPath[i]);
                    cont = subWM.getNodeContainer(subID);
                } else {
                    cont = null;
                }
            }
            if (cont == null) {
                LOGGER.warn("No node with id "
                        + Arrays.toString(idPath) + " found.");
            } else {
                WorkflowManager parent = cont.getParent();
                NodeSettings settings = new NodeSettings("something");
                parent.saveNodeSettings(cont.getID(), settings);
                cont.saveSettings(settings);
                NodeSettings model = settings.getNodeSettings(Node.CFG_MODEL);
                String[] splitName = o.m_name.split("/");
                String name = splitName[splitName.length - 1];
                String[] pathElements = new String[splitName.length - 1];
                System.arraycopy(splitName, 0,
                        pathElements, 0, pathElements.length);
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
                parent.loadNodeSettings(cont.getID(), settings);
            }
        }
    }


    private static void setupEncryptionKey(final boolean isPromptForPassword,
            String masterKey) {
        if (isPromptForPassword) {
            Console cons;
            if ((cons = System.console()) == null) {
                System.err.println("No console for password prompt available");
            } else {
                char[] first, second;
                boolean areEqual;
                do {
                    first = cons.readPassword("%s", "Password:");
                    second = cons.readPassword("%s", "Reenter Password:");
                    areEqual = Arrays.equals(first, second);
                    if (!areEqual) {
                        System.out.println("Passwords don't match");
                    }
                } while (!areEqual);
                masterKey = new String(first);
            }
        }
        if (masterKey != null) {
            final String encryptionKey = masterKey;
            KnimeEncryption.setEncryptionKeySupplier(new EncryptionKeySupplier() {
                /** {@inheritDoc} */
                public String getEncryptionKey() {
                    return encryptionKey;
                }
            });
        }
    }

    private static String[] splitOption(String option) {
        String[] res = new String[4];
        int index = option.indexOf(',');
        res[0] = option.substring(0, index);
        option = option.substring(index + 1);

        index = option.indexOf(',');
        res[1] = option.substring(0, index);
        option = option.substring(index + 1);

        index = option.lastIndexOf(',');
        res[2] = option.substring(0, index);
        res[3] = option.substring(index + 1);

        return res;
    }
}
