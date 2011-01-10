/* Created on Nov 23, 2006 12:07:08 PM by thor
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------- *
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.util.StringFormat;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.core.util.FileUtil;
import org.knime.core.util.KNIMETimer;
import org.knime.core.util.KnimeEncryption;
import org.knime.core.util.MutableBoolean;
import org.knime.core.util.tokenizer.Tokenizer;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 * Simple utility class that takes a workflow, either in a directory or zipped
 * into a single file, executes it and saves the results in the end. If the
 * input was a ZIP file the workflow is zipped back into a file.
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

    /**
     *
     * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
     */
    private static final class BatchExecWorkflowLoadHelper implements
            WorkflowLoadHelper {
        /**  */
        private final Map<String, Credentials> m_credentialMap;

        /**
         * @param credentialMap
         */
        private BatchExecWorkflowLoadHelper(
                final Map<String, Credentials> credentialMap) {
            m_credentialMap = credentialMap;
        }

        @Override
        public List<Credentials> loadCredentials(
                final List<Credentials> credentials) {
            List<Credentials> newCredentials = new ArrayList<Credentials>();
            Console cons = null;
            for (Credentials cred : credentials) {
                String login = null;
                String password = null;
                if (m_credentialMap.containsKey(cred.getName())) {
                    Credentials currCred = m_credentialMap.get(cred
                            .getName());
                    if (currCred != null) {
                        login = currCred.getLogin();
                        password = currCred.getPassword();
                    }
                }
                if (login == null || password == null) {
                    if (cons == null) {
                        if ((cons = System.console()) == null) {
                            System.err
                            .println("No console for "
                                    + "credential prompt available");
                            return credentials;
                        }
                    }
                    cons.printf("Enter for credential %s ", cred
                            .getName());
                    if (login == null) {
                        login = cons
                        .readLine("%s", "Enter login: ");
                    }
                    char[] pwd = getPasswordFromConsole(cons);
                    password = new String(pwd);
                }
                newCredentials.add(new Credentials(cred.getName(),
                        login, password));
            }
            return newCredentials;
        }

        @Override
        public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(
                final String workflowVersionString) {
            return UnknownKNIMEVersionLoadPolicy.Abort;
        }
    }

    private BatchExecutor() { /**/
    }

    private static void usage() {
        System.err.println(
              "Usage: The following options are available:\n"
            + " -nosave => do not save the workflow after execution has finished\n"
            + " -reset => reset workflow prior to execution\n"
            + " -credential=name[;login[;password]] => for each credential enter credential\n"
            + "                 name and optional login/password, otherwise its prompted for\n"
            + " -masterkey[=...] => prompt for master passwort (used in e.g. database nodes),\n"
            + "                 if provided with argument, use argument instead of prompting\n"
            + " -preferences=... => path to the file containing eclipse/knime preferences,\n"
            + " -workflowFile=... => ZIP file with a ready-to-execute workflow in the root \n"
            + "                  of the ZIP\n"
            + " -workflowDir=... => directory with a ready-to-execute workflow\n"
            + " -destFile=... => ZIP file where the executed workflow should be written to\n"
            + "                  if omitted the workflow is only saved in place\n"
            + " -destDir=... => directory where the executed workflow is saved to\n"
            + "                  if omitted the workflow is only saved in place\n"
            + " -workflow.variable=name,value,type => define or overwrite workflow variable\n"
            + "                  'name' with value 'value' (possibly enclosed by quotes). The\n"
            + "                  'type' must be one of \"String\", \"int\" or \"double\".\n"
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
     * @throws CanceledExecutionException Delegated from WFM
     * @throws InvalidSettingsException Delegated from WFM
     */
    public static void main(final String[] args) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
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
     */
    public static int mainRun(final String[] args) throws IOException,
        InvalidSettingsException, CanceledExecutionException {
        long t = System.currentTimeMillis();
        if (args.length < 1) {
            usage();
            return 1;
        }

        File input = null, output = null;
        boolean noSave = false;
        boolean noExecute = false;
        boolean reset = false;
        boolean isPromptForPassword = false;
        boolean outputZip = false;
        boolean failOnLoadError = false;
        File preferenceFile = null;
        String masterKey = null;
        List<Option> options = new ArrayList<Option>();
        List<FlowVariable> wkfVars = new ArrayList<FlowVariable>();
        final Map<String, Credentials> credentialMap =
            new LinkedHashMap<String, Credentials>();

        for (String s : args) {
            String[] parts = s.split("=", 2);
            if ("-nosave".equals(parts[0])) {
                noSave = true;
            } else if ("-reset".equals(parts[0])) {
                reset = true;
            } else if ("-noexecute".equals(parts[0])) {
                noExecute = true;
            } else if ("-failonloaderror".equals(parts[0])) {
                failOnLoadError = true;
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
            } else if ("-credential".equals(parts[0])) {
                if (parts.length == 2) {
                    if (parts[1].length() == 0) {
                        System.err.println("Credential name must not be empty.");
                        return 1;
                    }
                    String credential = parts[1];
                    String[] credParts = credential.split(";", 3);
                    if (credParts.length > 0) {
                        String credName = credParts[0].trim();
                        if (credName.length() == 0) {
                            System.err.println(
                                "Credentials must not be empty.");
                            return 1;
                        }
                        if (credParts.length > 1) {
                            String credLogin = null;
                            String credPassword = null;
                            if (credParts[1].trim().length() > 0) {
                                credLogin = credParts[1].trim();
                            }
                            if (credParts.length > 2) {
                                credPassword = credParts[2];
                            }
                            credentialMap.put(credName, new Credentials(
                                credName, credLogin, credPassword));
                        } else {
                            credentialMap.put(credName, null);
                        }
                    }
                } else {
                    System.err.println(
                	    "Couldn't parse -credential argument: " + s);
                    return 1;
                }
            } else if ("-preferences".equals(parts[0])) {
                if (parts.length != 2) {
                    System.err.println(
                            "Couldn't parse -preferences argument: " + s);
                    return 1;
                }
                preferenceFile = new File(parts[1]);
                if (!preferenceFile.isFile()) {
                    System.err.println("Preference File '"
                            + parts[1] + "' is not a file.");
                    return 1;
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
                outputZip = true;
            } else if ("-destDir".equals(parts[0])) {
                if (parts.length != 2) {
                    System.err.println(
                            "Couldn't parse -destDir argument: " + s);
                    return 1;
                }
                output = new File(parts[1]);
                outputZip = false;
            } else if ("-workflow.variable".equals(parts[0])) {
                if (parts.length != 2) {
                    System.err.println("Couldn't parse -workflow.variable "
                            + "argument: " + s);
                    return 1;
                }
                String[] parts2 = splitWorkflowVariableArg(parts[1]);
                FlowVariable var = null;
                try {
                    var = createWorkflowVariable(parts2);
                } catch (Exception e) {
                    System.err.println("Couldn't parse -workflow.variable "
                            + "argument: " + s + ": " + e.getMessage());
                    System.exit(1);
                }
                wkfVars.add(var);
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
        if (preferenceFile != null) {
            try {
                setPreferences(preferenceFile);
            } catch (CoreException e) {
                throw new IOException("Unable to import preferences", e);
            }
        }
        setupEncryptionKey(isPromptForPassword, masterKey);

        File workflowDir;
        if (input == null) {
            System.err.println("No input file or directory given.");
            return 1;
        } else if (input.isFile()) {
            File dir = FileUtil.createTempDir("BatchExecutorInput");
            FileUtil.unzip(input, dir);
            workflowDir = dir;
        } else {
            workflowDir = input;
        }

        // the workflow may be contained in a sub-directory
        // if run on a archived workflow (typical scenario if workflow is
        // exported to a zip using the wizard)
        if (!new File(workflowDir, WorkflowPersistor.WORKFLOW_FILE).exists()) {
            workflowDir = workflowDir.listFiles()[0];
        }

        WorkflowLoadResult loadResult;
        try {
            loadResult = WorkflowManager.loadProject(
                    workflowDir, new ExecutionMonitor(),
                    new BatchExecWorkflowLoadHelper(credentialMap));
        } catch (UnsupportedWorkflowVersionException e) {
            System.err.println("Unknown workflow version: " + e.getMessage());
            return 1;
        }
        if (failOnLoadError && loadResult.hasErrors()) {
            System.err.println("Error(s) during workflow loading. "
                    + "Check log file for details.");
            LOGGER.error(
                    loadResult.getFilteredError("", LoadResultEntryType.Error));
            return 1;
        }
        final WorkflowManager wfm = loadResult.getWorkflowManager();

        if (!wkfVars.isEmpty()) {
            applyWorkflowVariables(wfm, reset, wkfVars);
        }

        if (reset) {
            wfm.resetAndConfigureAll();
            LOGGER.debug("Workflow reset done.");
        }

        setNodeOptions(options, wfm);

        LOGGER.debug("Status of workflow before execution:");
        LOGGER.debug("------------------------------------");
        dumpWorkflowToDebugLog(wfm);
        LOGGER.debug("------------------------------------");
        boolean successful = true;
        final MutableBoolean executionCanceled = new MutableBoolean(false);
        if (!noExecute) {
            // get workspace dir
            IWorkspace ws = ResourcesPlugin.getWorkspace();
            URI uri = ws.getRoot().getLocationURI();
            // if exists check for ".cancel" file
            if (uri != null) {
                File wsFile = new File(uri);
                // file to be checked for
                  final File cancelFile = new File(wsFile, ".cancel");
                  // create new timer task
                  KNIMETimer.getInstance().schedule(new TimerTask() {
                      /** {@inheritDoc} */
                      @Override
                      public void run() {
                           if (cancelFile.exists()) {
                               // CANCEL workflow manager
                              wfm.cancelExecution();
                              // delete cancel file
                              cancelFile.delete();
                              executionCanceled.setValue(true);
                              // cancel this timer
                              this.cancel();
                          }
                      }
                  }, 1000, 1000);
            }
            successful = wfm.executeAllAndWaitUntilDone();
        }

        // only save when execution has not been canceled
        if (!executionCanceled.booleanValue()) {
            if (!noSave) { // save workflow
                // save // in place when no output (file or dir) given
                if (output == null) {
                    wfm.save(workflowDir, new ExecutionMonitor(), true);
                    LOGGER.debug("Workflow saved: "
                            + workflowDir.getAbsolutePath());
                    if (input.isFile()) {
                        // if input is a Zip file, overwrite input flow (Zip)
                        // workflow dir contains temp workflow directory
                        FileUtil.zipDir(input, workflowDir, 9);
                        LOGGER.info("Saved workflow availabe at: "
                                + input.getAbsolutePath());
                    }
                } else {
                    if (outputZip) { // save as Zip
                        File outputTempDir =
                            FileUtil.createTempDir("BatchExecutorOutput");
                        wfm.save(outputTempDir, new ExecutionMonitor(), true);
                        LOGGER.debug("Workflow saved: "
                                + outputTempDir.getAbsolutePath());
                        // to be saved into new output zip file
                        FileUtil.zipDir(output, outputTempDir, 9);
                        LOGGER.info("Saved workflow availabe at: "
                                + output.getAbsolutePath());
                    } else { // save into dir
                        // copy current workflow dir
                        wfm.save(output, new ExecutionMonitor(), true);
                        LOGGER.info("Saved workflow availabe at: "
                                + output.getAbsolutePath());
                    }
                }
            }
        }
        // get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis() - t;
        String niceTime = StringFormat.formatElapsedTime(elapsedTimeMillis);
        String timeString = ("Finished in " + niceTime
                + " (" + elapsedTimeMillis + "ms)");
        System.out.println(timeString);
        if (executionCanceled.booleanValue()) {
            LOGGER.debug("Workflow execution canceled after " + timeString);
            LOGGER.debug("Status of workflow after cancelation:");
        } else {
            LOGGER.debug("Workflow execution done " + timeString);
            LOGGER.debug("Status of workflow after execution:");
        }
        LOGGER.debug("------------------------------------");
        dumpWorkflowToDebugLog(wfm);
        LOGGER.debug("------------------------------------");
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
                } else if ("long".equals(o.m_type)) {
                    model.addLong(name, Long.parseLong(o.m_value));
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
                } else if ("LongCell".equals(o.m_type)) {
                    long i = Long.parseLong(o.m_value);
                    model.addDataCell(name, new LongCell(i));
                } else {
                    throw new IllegalArgumentException("Unknown option type '"
                            + o.m_type + "'");
                }
                parent.loadNodeSettings(cont.getID(), settings);
            }
        }
    }

    private static void setPreferences(final File preferenceFile)
        throws IOException, CoreException {
        InputStream in = new BufferedInputStream(
                new FileInputStream(preferenceFile));
        IStatus status = Platform.getPreferencesService().importPreferences(in);
        switch (status.getSeverity()) {
        case IStatus.CANCEL:
            LOGGER.error("Importing preferences was canceled");
            break;
        case IStatus.WARNING:
            LOGGER.warn("Importing preferences raised warning: "
                    + status.getMessage(), status.getException());
            break;
        case IStatus.INFO:
            LOGGER.info("Importing preferences raised an info message: "
                    + status.getMessage(), status.getException());
        case IStatus.OK:
            break;
        default:
            LOGGER.warn("Unknown return status from preference import: "
                    + status.getSeverity());
        }
    }

    private static void setupEncryptionKey(final boolean isPromptForPassword,
            String masterKey) {
        if (isPromptForPassword) {
            Console cons;
            if ((cons = System.console()) == null) {
                System.err.println("No console for password prompt available");
            } else {
                char[] password = getPasswordFromConsole(cons);
                masterKey = new String(password);
            }
        }
        if (masterKey != null) {
            final String encryptionKey = masterKey;
            KnimeEncryption.setEncryptionKeySupplier(
                    new EncryptionKeySupplier() {
                /** {@inheritDoc} */
                @Override
                public String getEncryptionKey() {
                    return encryptionKey;
                }
            });
        }
    }

    private static char[] getPasswordFromConsole(final Console cons) {
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
        return first;
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

    /** Splits the argument to -workflow.variable into its sub-components
     * (name, value, type) and returns it as array.
     * @param arg The string to split
     * @return The components of the string, no validation is done.
     */
    private static String[] splitWorkflowVariableArg(final String arg) {
        Tokenizer tokenizer = new Tokenizer(new StringReader(arg));
        TokenizerSettings settings = new TokenizerSettings();
        settings.addQuotePattern("\"", "\"", '\\');
        settings.addQuotePattern("'", "'", '\\');
        settings.addDelimiterPattern(",",
                /* combine multiple= */false,
                /* return as token= */ false,
                /* include in token= */false);
        tokenizer.setSettings(settings);
        ArrayList<String> tokenList = new ArrayList<String>();
        String token;
        while ((token = tokenizer.nextToken()) != null) {
            tokenList.add(token);
        }
        return tokenList.toArray(new String[tokenList.size()]);
    }

    /** Creates a new flow variable from the sub-components of the
     * -workflow.variables commandline argument. If the string array does not
     * meet the requirements (e.g. length = 3), an exception is thrown.
     * @param args The arguments for the variable.
     * @return A new flow variable.
     */
    private static FlowVariable createWorkflowVariable(final String[] args) {
        if (args.length != 3) {
            throw new IndexOutOfBoundsException("Invalid argument list");
        }
        String name = args[0];
        String value = args[1];
        String type = args[2];
        if ("String".equals(type)) {
            return new FlowVariable(name, value);
        } else if ("int".equals(type)) {
            return new FlowVariable(name, Integer.parseInt(value));
        } else if ("double".equals(type)) {
            return new FlowVariable(name, Double.parseDouble(value));
        } else {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    /** Injects the workflow variables provided in the last argument into the
     * workflow.
     * @param wfm The workflow, where to inject the variables
     * @param reset Whether to reset the workflow
     * {@link WorkflowManager#addWorkflowVariables(boolean, FlowVariable...)}
     * @param wkfVars The flow variables.
     */
    private static void applyWorkflowVariables(final WorkflowManager wfm,
            final boolean reset, final List<FlowVariable> wkfVars) {
        HashSet<FlowVariable> unknown = new HashSet<FlowVariable>(wkfVars);
        unknown.removeAll(wfm.getWorkflowVariables());
        if (!unknown.isEmpty()) {
            StringBuilder str = new StringBuilder("The workflow variable");
            str.append(unknown.size() == 1 ? " is" : "s are");
            str.append(" potentially unused (not defined in workflow):");
            boolean first = true;
            for (FlowVariable f : unknown) {
                str.append(first ? " " : ", ");
                first = false;
                str.append("\"").append(f.getName()).append("\" (");
                str.append(f.getType()).append(")");
            }
            LOGGER.warn(str);
            System.out.println(str);
        }
        for (FlowVariable f : wkfVars) {
            LOGGER.debug("Setting workflow variable " + f);
        }
        wfm.addWorkflowVariables(
                !reset, wkfVars.toArray(new FlowVariable[wkfVars.size()]));
    }

    private static void dumpWorkflowToDebugLog(final WorkflowManager wfm) {
        String str = wfm.printNodeSummary(wfm.getID(), 0);
        BufferedReader reader = new BufferedReader(new StringReader(str));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                LOGGER.debug(line);
            }
            reader.close();
        } catch (IOException e) {
            LOGGER.fatal("IOException while reading string", e);
        }
    }
}
