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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
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
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.EncryptionKeySupplier;
import org.knime.core.util.FileUtil;
import org.knime.core.util.KNIMETimer;
import org.knime.core.util.KnimeEncryption;
import org.knime.core.util.LockFailedException;
import org.knime.core.util.MutableBoolean;
import org.knime.core.util.tokenizer.Tokenizer;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 * Simple utility class that takes a workflow, either in a directory or zipped into a single file, executes it and saves
 * the results in the end. If the input was a ZIP file the workflow is zipped back into a file.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BatchExecutor {
    /**
     * Exception that can be thrown by subclasses in
     * {@link BatchExecutor#executeWorkflow(WorkflowManager, WorkflowConfiguration)}.
     *
     * @since 2.7
     */
    public static class BatchException extends Exception {
        private static final long serialVersionUID = -6733538372934618157L;

        private final int m_detailCode;

        /**
         * Creates a new exception.
         *
         * @param message the message
         * @param cause the cause
         * @param detailCode a positive integer, that is used as a return value for process
         */
        public BatchException(final String message, final Exception cause, final int detailCode) {
            super(message, cause);
            m_detailCode = detailCode;
        }

        /**
         * @return the detailCode
         */
        public int getDetailCode() {
            return m_detailCode;
        }
    }

    /**
     * Exception for illegal or broken options.
     *
     * @since 2.7
     */
    public static class IllegalOptionException extends Exception {
        private static final long serialVersionUID = -6274384133109038351L;

        /**
         * Creates a new exception.
         *
         * @param message a message
         */
        public IllegalOptionException(final String message) {
            super(message);
        }
    }

    /**
     * Return code for successful execution: {@value} .
     *
     * @since 2.6
     */
    public static final int EXIT_SUCCESS = 0;

    /**
     * Return code for execution with warnings: {@value} .
     *
     * @since 2.6
     */
    public static final int EXIT_WARN = 1;

    /**
     * Return code for errors before the workflow has been loaded (e.g. wrong parameter): {@value} .
     *
     * @since 2.6
     */
    public static final int EXIT_ERR_PRESTART = 2;

    /**
     * Return code for errors during workflow loading: {@value} .
     *
     * @since 2.6
     */
    public static final int EXIT_ERR_LOAD = 3;

    /**
     * Return code for errors during workflow execution: {@value} .
     *
     * @since 2.6
     */
    public static final int EXIT_ERR_EXECUTION = 4;

    // dummy password constant indicating that the user should be prompted for the master key password
    private static final String PROMPT_FOR_PASSWORD = new String();

    /**
     * List of all configured workflows that should be executed.
     *
     * @since 2.7
     */
    protected final List<WorkflowConfiguration> m_workflows = new ArrayList<WorkflowConfiguration>();

    /**
     * If execution of multiple workflows should be stopped after the first error or if the remaining workflows
     * should be run nevertheless.
     * @since 2.7
     */
    protected boolean m_stopOnError = true;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BatchExecutor.class);

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
        Option(final int[] nodeIDs, final String name, final String value, final String type) {
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
    private static final class BatchExecWorkflowLoadHelper extends WorkflowLoadHelper {
        /**  */
        private final Map<String, Credentials> m_credentialMap;

        /**
         * @param credentialMap
         */
        private BatchExecWorkflowLoadHelper(final Map<String, Credentials> credentialMap) {
            m_credentialMap = credentialMap;
        }

        @Override
        public List<Credentials> loadCredentials(final List<Credentials> credentials) {
            List<Credentials> newCredentials = new ArrayList<Credentials>();
            Console cons = null;
            for (Credentials cred : credentials) {
                String login = null;
                String password = null;
                if (m_credentialMap.containsKey(cred.getName())) {
                    Credentials currCred = m_credentialMap.get(cred.getName());
                    if (currCred != null) {
                        login = currCred.getLogin();
                        password = currCred.getPassword();
                    }
                }
                if (login == null || password == null) {
                    if (cons == null) {
                        if ((cons = System.console()) == null) {
                            System.err.println("No console for credential prompt available");
                            return credentials;
                        }
                    }
                    cons.printf("Enter for credential %s ", cred.getName());
                    if (login == null) {
                        login = cons.readLine("%s", "Enter login: ");
                    }
                    char[] pwd = getPasswordFromConsole(cons);
                    password = new String(pwd);
                }
                newCredentials.add(new Credentials(cred.getName(), login, password));
            }
            return newCredentials;
        }

    }

    /**
     * A load helper for templates contained in the workflow. It will inherit the credentials from a batch workflow load
     * helper.
     */
    private static class BatchExecWorkflowTemplateLoadHelper extends WorkflowLoadHelper {

        private final BatchExecWorkflowLoadHelper m_lH;

        /**
         * Keeps the argument load helper as field to query it for credentials.
         *
         * @param lH Parent
         */
        BatchExecWorkflowTemplateLoadHelper(final BatchExecWorkflowLoadHelper lH) {
            super(true);
            m_lH = lH;
        }

        /** {@inheritDoc} */
        @Override
        public List<Credentials> loadCredentials(final List<Credentials> credentials) {
            return m_lH.loadCredentials(credentials);
        }
    }

    /**
     * @since 2.7
     */
    protected static class WorkflowConfiguration {
        /** If workflow should be saved after execution. */
        public boolean noSave;

        /** If the workflow should be reset prior to execution. */
        public boolean reset;

        /** If the metanode links should be updated prior to execution. */
        public boolean updateMetanodeLinks;

        /** If the workflow should be executed or only loaded. */
        public boolean noExecute;

        /** If the execution should fail if the workflow cannot be loaded sucessfully. */
        public boolean failOnLoadError;

        /** The master key. */
        public String masterKey;

        /** A map with credentials (name => credentials) . */
        public final Map<String, Credentials> credentials = new HashMap<String, Credentials>();

        /** The input workflow, either a directory or zip file. */
        public File inputWorkflow;

        /** The output (zip) file. Either a file or an {@link #outputDir} should be given */
        public File outputFile;

        /** The output directory. Either a directory or an {@link #outputFile} should be given*/
        public File outputDir;

        /** A collection of workflow variables. */
        public final Collection<FlowVariable> flowVariables = new ArrayList<FlowVariable>();

        /** A collection of node options. */
        public final Collection<Option> nodeOptions = new ArrayList<BatchExecutor.Option>();

        /** The (temporary) workflow location which should be used to load the workflow. */
        File workflowLocation;
    }

    /**
     * Creates a new batch executor.
     *
     * @param args the command line arguments
     * @throws IOException if an I/O error occurs
     * @throws CoreException if the preference cannot be read from the specified file
     * @throws IllegalOptionException if a setting is missing or invalid
     * @throws BatchException may be thrown by subclass implementations; the real exception is available via the cause
     *             of the batch exception
     * @since 2.7
     */
    public BatchExecutor(final String[] args) throws IOException, CoreException, IllegalOptionException, BatchException {
        if (args.length == 0) {
            throw new IllegalOptionException("No arguments provided");
        }
        processArguments(args);
        for (WorkflowConfiguration config : m_workflows) {
            checkConfiguration(config);
        }
    }

    /**
     * @since 2.7
     */
    protected BatchExecutor() {
        // only for internal use
    }

    /**
     * This method is called by the constructor in order to process the command line arguments. The default
     * implementation reads the configuration for a single workflow from the arguments using
     * {@link #parseConfigFromArguments(String[])}. Subclasses may override this method to do their own processing. Keep
     * in mind that this method is called from the constructor i.e. instance fields of subclasses are not intialized
     * yet!
     *
     * @param args the command line arguments
     * @throws CoreException if the preference cannot be read from the specified file
     * @throws IllegalOptionException if an option is invalid or missing
     * @throws IOException if an I/O error occurs
     * @throws BatchException may be thrown by subclass implementations; the real exception is available via the cause
     *             of the batch exception
     * @since 2.7
     */
    protected void processArguments(final String[] args) throws IOException, CoreException, IllegalOptionException,
            BatchException {
        parseConfigFromArguments(args);
    }

    /**
     * Reads a workflow configuration from command line arguments.
     *
     * @param args the command line arguments, see {@link #usage()}
     * @return a new workflow configuration
     * @throws CoreException if the preference cannot be read from the specified file
     * @throws IllegalOptionException if an option is invalid or missing
     * @throws FileNotFoundException if the specified preferences file was not found
     * @since 2.7
     */
    protected WorkflowConfiguration parseConfigFromArguments(final String[] args) throws CoreException,
            FileNotFoundException, IllegalOptionException {
        WorkflowConfiguration config = createNewConfiguration();

        for (String s : args) {
            String[] parts = s.split("=", 2);
            handleCommandlineArgument(parts, s, config);
        }

        // yes == is intended here
        setupEncryptionKey(config.masterKey == PROMPT_FOR_PASSWORD, config.masterKey);
        m_workflows.add(config);
        return config;
    }

    /**
     * Handles a single command line argument (pair).
     *
     * @param parts a one or two-element array of the command line arguments. The first part is the argument name, the
     *            second the optional value
     * @param s the complete (un-split) argument
     * @param config the current workflow configuration into which the processed argument must be filled in
     * @throws CoreException if the preference cannot be read from the specified file
     * @throws IllegalOptionException if an option is invalid or missing
     * @throws FileNotFoundException if the specified preferences file was not found
     * @since 2.7
     */
    protected void handleCommandlineArgument(final String[] parts, final String s, final WorkflowConfiguration config)
            throws FileNotFoundException, CoreException, IllegalOptionException {
        if ("-nosave".equals(parts[0])) {
            config.noSave = true;
        } else if ("-reset".equals(parts[0])) {
            config.reset = true;
        } else if ("-updateLinks".equals(parts[0])) {
            config.updateMetanodeLinks = true;
        } else if ("-noexecute".equals(parts[0])) {
            config.noExecute = true;
        } else if ("-failonloaderror".equals(parts[0])) {
            config.failOnLoadError = true;
        } else if ("-masterkey".equals(parts[0])) {
            if (parts.length > 1) {
                if (parts[1].length() == 0) {
                    throw new IllegalOptionException("Master key must not be empty.");
                }
                config.masterKey = parts[1];
            } else {
                config.masterKey = PROMPT_FOR_PASSWORD;
            }
        } else if ("-credential".equals(parts[0])) {
            if (parts.length == 2) {
                if (parts[1].length() == 0) {
                    throw new IllegalOptionException("Credential name must not be empty.");
                }
                String[] credParts = parts[1].split(";", 3);
                String credName = credParts[0].trim();
                if (credName.length() == 0) {
                    throw new IllegalOptionException("Credentials must not be empty.");
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
                    config.credentials.put(credName, new Credentials(credName, credLogin, credPassword));
                } else {
                    config.credentials.put(credName, null);
                }
            } else {
                throw new IllegalOptionException("Couldn't parse -credential argument: " + s);
            }
        } else if ("-preferences".equals(parts[0])) {
            if (parts.length != 2) {
                throw new IllegalOptionException("Couldn't parse -preferences argument: " + s);
            }
            setPreferences(new File(parts[1]));
        } else if ("-workflowFile".equals(parts[0])) {
            if (parts.length != 2) {
                throw new IllegalOptionException("Couldn't parse -workflowFile argument: " + s);
            }
            config.inputWorkflow = new File(parts[1]);
            if (!config.inputWorkflow.isFile()) {
                throw new IllegalOptionException("Workflow file '" + parts[1] + "' is not a file.");
            }
        } else if ("-workflowDir".equals(parts[0])) {
            if (parts.length != 2) {
                throw new IllegalOptionException("Couldn't parse -workflowDir argument: " + s);
            }
            config.inputWorkflow = new File(parts[1]);
            if (!config.inputWorkflow.isDirectory()) {
                throw new IllegalOptionException("Workflow directory '" + parts[1] + "' is not a directory.");
            }
        } else if ("-destFile".equals(parts[0])) {
            if (parts.length != 2) {
                throw new IllegalOptionException("Couldn't parse -destFile argument: " + s);
            }
            config.outputFile = new File(parts[1]);
        } else if ("-destDir".equals(parts[0])) {
            if (parts.length != 2) {
                throw new IllegalOptionException("Couldn't parse -destDir argument: " + s);
            }
            config.outputDir = new File(parts[1]);
        } else if ("-workflow.variable".equals(parts[0])) {
            if (parts.length != 2) {
                throw new IllegalOptionException("Couldn't parse -workflow.variable argument: " + s);
            }
            String[] parts2 = splitWorkflowVariableArg(parts[1]);
            FlowVariable var = null;
            try {
                var = createWorkflowVariable(parts2);
            } catch (Exception e) {
                throw new IllegalOptionException("Couldn't parse -workflow.variable " + "argument: " + s + ": "
                        + e.getMessage());
            }
            config.flowVariables.add(var);
        } else if ("-option".equals(parts[0])) {
            if (parts.length != 2) {
                throw new IllegalOptionException("Couldn't parse -option argument: " + s);
            }
            String[] parts2;
            try {
                parts2 = splitOption(parts[1]);
            } catch (IndexOutOfBoundsException ex) {
                throw new IllegalOptionException("Couldn't parse -option argument: " + s);
            }
            String[] nodeIDPath = parts2[0].split("/");
            int[] nodeIDs = new int[nodeIDPath.length];
            for (int i = 0; i < nodeIDs.length; i++) {
                nodeIDs[i] = Integer.parseInt(nodeIDPath[i]);
            }
            String optionName = parts2[1];
            String value = parts2[2];
            String type = parts2[3];

            config.nodeOptions.add(new Option(nodeIDs, optionName, value, type));
        } else {
            throw new IllegalOptionException("Unknown option '" + parts[0] + "'");
        }
    }

    /**
     * @since 2.7
     */
    protected void usage() {
        System.err.println(getOptionsString() + getPropertiesString() + "\n" + getReturnCodesHelp());
    }

    /**
     * @return a string explaining the options available for the batch executor
     */
    public static String getOptionsString() {
        return "Usage: The following options are available:\n"
                + " -config=...       => XML file containing the configuration for one or more workflows\n" + "   OR\n"
                + " -nosave           => do not save the workflow after execution has finished\n"
                + " -reset            => reset workflow prior to execution\n"
                + " -failonloaderror  => don't execute if there are errors during workflow loading\n"
                + " -updateLinks      => update meta node links to latest version\n"
                + " -credential=name[;login[;password]] => for each credential enter credential\n"
                + "                      name and optional login/password, otherwise its prompted for\n"
                + " -masterkey[=...]  => prompt for master passwort (used in e.g. database nodes),\n"
                + "                      if provided with argument, use argument instead of prompting\n"
                + " -preferences=...  => path to the file containing eclipse/knime preferences,\n"
                + " -workflowFile=... => ZIP file with a ready-to-execute workflow in the root \n"
                + "                      of the ZIP\n"
                + " -workflowDir=...  => directory with a ready-to-execute workflow\n"
                + " -destFile=...     => ZIP file where the executed workflow should be written to\n"
                + "                      if omitted the workflow is only saved in place\n"
                + " -destDir=...      => directory where the executed workflow is saved to\n"
                + "                      if omitted the workflow is only saved in place\n"
                + " -workflow.variable=name,value,type => define or overwrite workflow variable\n"
                + "                      'name' with value 'value' (possibly enclosed by quotes). The\n"
                + "                      'type' must be one of \"String\", \"int\" or \"double\".";
    }

    /**
     * Returns a short description of the defined exit codes.
     *
     * @return a string
     * @since 2.7
     */
    protected String getReturnCodesHelp() {
        return "The following return codes are defined:\n"
                + "\t" + EXIT_SUCCESS + "\tupon successful execution\n"
                + "\t" + EXIT_ERR_PRESTART + "\tif paramaters are wrong or missing\n"
                + "\t" + EXIT_ERR_LOAD + "\twhen an error occurs during loading a workflow\n"
                + "\t" + EXIT_ERR_EXECUTION + "\tif an error during execution occured\n";
    }

    /**
     * @return a string explaining the usage of Java properties for certain KNIME parameters
     */
    public static String getPropertiesString() {
        return "\n" + "Some KNIME settings can also be adjusted by Java properties;\n"
                + "they need to be provided as last option in the command line:\n"
                + " -vmargs -Dorg.knime.core.maxThreads=n => sets the maximum\n"
                + "                  number of threads used by KNIME\n";
    }

    /**
     * Parses the command line and starts up KNIME. It returns whether execution was successful or not.
     *
     * @param args command line arguments, see output of {@link #usage()}
     * @return an exit code, one of {@link #EXIT_SUCCESS}, {@link #EXIT_WARN}, {@link #EXIT_ERR_PRESTART},
     *         {@link #EXIT_ERR_LOAD}, or {@link #EXIT_ERR_EXECUTION}
     *
     */
    public static int mainRun(final String... args) {
        if (args.length == 0) {
            new BatchExecutor().usage();
            return EXIT_SUCCESS;
        }

        try {
            BatchExecutor exec = new BatchExecutor(args);
            return exec.runAll();
        } catch (IOException ex) {
            System.err.println("Error while reading input XML file: " + ex.getMessage());
            return EXIT_ERR_PRESTART;
        } catch (CoreException ex) {
            System.err.println("Error while reading preferences file: " + ex.getMessage());
            return EXIT_ERR_PRESTART;
        } catch (IllegalOptionException ex) {
            System.err.println(ex.getMessage());
            return EXIT_ERR_PRESTART;
        } catch (BatchException ex) {
            System.err.println(ex.getMessage());
            return ex.getDetailCode();
        }
    }

    /**
     * Main method.
     *
     * @param args the command line parameters, see {@link #usage()}
     * @throws Exception if an error occurs
     */
    public static void main(final String[] args) throws Exception {
        BatchExecutor be = new BatchExecutor(args);
        int returnVal = be.runAll();
        System.exit(returnVal);
    }

    /**
     * Loads a single workflow.
     *
     * @param config the workflow configuration
     * @return the workflow manager representing the loaded workflow
     * @throws IOException if an I/O error occurs while loading the workflow
     * @throws InvalidSettingsException if some node or workflow settings are invalid
     * @throws CanceledExecutionException if loading the workflow is canceled by the user (should not happen in batch
     *             mode)
     * @throws UnsupportedWorkflowVersionException if the workflow version is not supported
     * @throws LockFailedException if the workflow cannot be locked
     * @throws IllegalOptionException if a node option is invalid
     * @since 2.7
     */
    protected WorkflowManager loadWorkflow(final WorkflowConfiguration config) throws IOException,
            InvalidSettingsException, CanceledExecutionException, UnsupportedWorkflowVersionException,
            LockFailedException, IllegalOptionException {
        if (config.inputWorkflow.isFile()) {
            File dir = FileUtil.createTempDir("BatchExecutorInput");
            FileUtil.unzip(config.inputWorkflow, dir);
            config.workflowLocation = dir;
        } else {
            config.workflowLocation = config.inputWorkflow;
        }

        // the workflow may be contained in a sub-directory
        // if run on a archived workflow (typical scenario if workflow is
        // exported to a zip using the wizard)
        if (!new File(config.workflowLocation, WorkflowPersistor.WORKFLOW_FILE).exists()) {
            File[] children = config.workflowLocation.listFiles();
            if (children.length == 0) {
                throw new IOException("No workflow directory at " + config.workflowLocation);
            } else {
                config.workflowLocation = config.workflowLocation.listFiles()[0];
            }

        }

        BatchExecWorkflowLoadHelper batchLH = new BatchExecWorkflowLoadHelper(config.credentials);
        WorkflowLoadResult loadResult =
                WorkflowManager.loadProject(config.workflowLocation, new ExecutionMonitor(), batchLH);
        if (config.failOnLoadError && loadResult.hasErrors()) {
            LOGGER.error(loadResult.getFilteredError("", LoadResultEntryType.Error));
            throw new IOException("Error(s) during workflow loading. Check log file for details.");
        }
        WorkflowManager wfm = loadResult.getWorkflowManager();

        BatchExecWorkflowTemplateLoadHelper batchTemplateLH = new BatchExecWorkflowTemplateLoadHelper(batchLH);
        if (config.updateMetanodeLinks) {
            LOGGER.debug("Checking for meta node link updates...");
            updateMetaNodeLinks(wfm, batchTemplateLH, config.failOnLoadError);
            LOGGER.debug("Checking for meta node link updates... done");
        }

        if (!config.flowVariables.isEmpty()) {
            applyWorkflowVariables(wfm, config.reset, config.flowVariables);
        }

        if (config.reset) {
            wfm.resetAndConfigureAll();
            LOGGER.debug("Workflow reset done.");
        }

        setNodeOptions(config.nodeOptions, wfm);
        return wfm;
    }

    /**
     * Executes a workflow.
     *
     * @param wfm the workflow manager
     * @param config the corresponding workflow configuration
     * @return <code>true</code> if the workflow executed successfully, <code>false</code> otherwise
     * @throws CanceledExecutionException if execution has been canceled by the user
     * @throws BatchException may be thrown by subclass implementations; the real exception is available via the cause
     *             of the batch exception
     * @since 2.7
     */
    protected boolean executeWorkflow(final WorkflowManager wfm, final WorkflowConfiguration config)
            throws CanceledExecutionException, BatchException {
        LOGGER.debug("Status of workflow before execution:");
        LOGGER.debug("------------------------------------");
        dumpWorkflowToDebugLog(wfm);
        LOGGER.debug("------------------------------------");
        boolean successful = true;
        final MutableBoolean executionCanceled = new MutableBoolean(false);
        if (!config.noExecute) {
            // get workspace dir
            File wsFile = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
            // file to be checked for
            final File cancelFile = new File(wsFile, ".cancel");
            // create new timer task
            TimerTask task = new TimerTask() {
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
            };
            KNIMETimer.getInstance().schedule(task, 1000, 1000);
            successful = wfm.executeAllAndWaitUntilDone();
            task.cancel();
        }
        if (executionCanceled.booleanValue()) {
            throw new CanceledExecutionException();
        }

        return successful;
    }

    /**
     * Saves the workflow after execution.
     *
     * @param wfm the workflow manager
     * @param config the corresponding workflow configuration
     * @throws IOException if an I/O error occurs while saving th workflow
     * @throws CanceledExecutionException if loading the workflow is canceled by the user (should not happen in batch
     *             mode)
     * @throws LockFailedException if the workflow cannot be locked
     * @since 2.7
     */
    protected void saveWorkflow(final WorkflowManager wfm, final WorkflowConfiguration config) throws IOException,
            CanceledExecutionException, LockFailedException {
        if (!config.noSave) { // save workflow
            // save // in place when no output (file or dir) given
            if ((config.outputDir == null) && (config.outputFile == null)) {
                wfm.save(config.workflowLocation, new ExecutionMonitor(), true);
                LOGGER.debug("Workflow saved: " + config.workflowLocation.getAbsolutePath());
                if (config.inputWorkflow.isFile()) {
                    // if input is a Zip file, overwrite input flow
                    // (Zip) workflow dir contains temp workflow dir
                    FileUtil.zipDir(config.inputWorkflow, config.workflowLocation, 9);
                    LOGGER.info("Saved workflow availabe at: " + config.inputWorkflow.getAbsolutePath());
                }
            } else if (config.outputFile != null) { // save as Zip
                File outputTempDir = FileUtil.createTempDir("BatchExecutorOutput");
                wfm.save(outputTempDir, new ExecutionMonitor(), true);
                LOGGER.debug("Workflow saved: " + outputTempDir.getAbsolutePath());
                // to be saved into new output zip file
                FileUtil.zipDir(config.outputFile, outputTempDir, 9);
                LOGGER.info("Saved workflow availabe at: " + config.outputFile.getAbsolutePath());
            } else if (config.outputDir != null) { // save into dir
                // copy current workflow dir
                wfm.save(config.outputDir, new ExecutionMonitor(), true);
                LOGGER.info("Saved workflow availabe at: " + config.outputDir.getAbsolutePath());
            }
        }
    }

    /**
     * Called from {@link #main(String[])} method. It parses the command line and starts up KNIME. It returns 0 if the
     * execution was run (even with errors) and 1 if the command line could not be parsed (e.g. usage was printed).
     *
     * @return 0 if WorkflowManager (WFM) was executed, 1 otherwise.
     * @since 2.7
     */
    public int runAll() {
        int retVal = EXIT_SUCCESS;

        for (WorkflowConfiguration config : m_workflows) {
            System.out.println("===== Executing workflow " + config.inputWorkflow + " =====");
            int rv = runOne(config);
            if (rv != EXIT_SUCCESS) {
                System.out.println("========= Workflow did not execute sucessfully ============");
                retVal = rv;
                if (m_stopOnError) {
                    break;
                }
            } else {
                System.out.println("============= Workflow executed sucessfully ===============");
            }
        }
        return retVal;
    }

    private int runOne(final WorkflowConfiguration config) {
        long t = System.currentTimeMillis();
        WorkflowManager wfm;
        try {
            wfm = loadWorkflow(config);
        } catch (IOException ex) {
            System.err.println("IO error while loading the workflow");
            return EXIT_ERR_LOAD;
        } catch (InvalidSettingsException ex) {
            System.err.println("Encountered invalid settings while loading the workflow");
            return EXIT_ERR_LOAD;
        } catch (CanceledExecutionException ex) {
            System.err.println("Workflow loading was canceled by user");
            return EXIT_ERR_LOAD;
        } catch (UnsupportedWorkflowVersionException ex) {
            System.err.println("Unsupported workflow version");
            return EXIT_ERR_LOAD;
        } catch (LockFailedException ex) {
            System.err.println("Unsupported workflow version");
            return EXIT_ERR_LOAD;
        } catch (IllegalOptionException ex) {
            System.err.println("Unknown or wrong option");
            return EXIT_ERR_PRESTART;
        }
        boolean sucessful;
        try {
            sucessful = executeWorkflow(wfm, config);
        } catch (CanceledExecutionException ex) {
            System.out.println("Workflow execution canceled");
            LOGGER.warn("Workflow execution canceled");
            return EXIT_ERR_EXECUTION;
        } catch (BatchException ex) {
            System.err.println("Workflow execution failed: " + ex.getMessage());
            LOGGER.error("Workflow execution failed: " + ex.getMessage(), ex.getCause());
            return ex.getDetailCode();
        } finally {
            long elapsedTimeMillis = System.currentTimeMillis() - t;
            String niceTime = StringFormat.formatElapsedTime(elapsedTimeMillis);
            String timeString = "Finished in " + niceTime + " (" + elapsedTimeMillis + "ms)";
            System.out.println(timeString);
            LOGGER.debug("Workflow execution done " + timeString);
            LOGGER.debug("Status of workflow after execution:");
            LOGGER.debug("------------------------------------");
            dumpWorkflowToDebugLog(wfm);
            LOGGER.debug("------------------------------------");
            wfm.shutdown();
        }

        try {
            saveWorkflow(wfm, config);
        } catch (IOException ex) {
            System.err.println("IO error while saving workflow: " + ex.getMessage());
            return EXIT_ERR_EXECUTION;
        } catch (CanceledExecutionException ex) {
            System.err.println("Workflow saving canceled by user");
            return EXIT_ERR_EXECUTION;
        } catch (LockFailedException ex) {
            System.err.println("Failed to lock workflow before saving: " + ex.getMessage());
            return EXIT_ERR_EXECUTION;
        } finally {
            wfm.getParent().removeProject(wfm.getID());
        }
        return sucessful ? EXIT_SUCCESS : EXIT_ERR_EXECUTION;
    }

    /**
     * Update meta node links (recursively).
     *
     * @param wfm The workflow
     * @param failOnLoadError If to fail if there errors updating the links
     * @throws CanceledExecutionException Not actually thrown
     * @throws IOException Special errors during update (not accessible)
     * @throws BatchException
     * @throws LoadException
     */
    private static void updateMetaNodeLinks(final WorkflowManager wfm, final WorkflowLoadHelper lH,
                                            final boolean failOnLoadError) throws IOException,
            CanceledExecutionException {
        // use queue, add meta node children while traversing the node list
        List<NodeID> ncsToCheck = wfm.getLinkedMetaNodes(true);
        int linksChecked = 0;
        int linksUpdated = 0;
        for (NodeID wmID : ncsToCheck) {
            WorkflowManager wm = (WorkflowManager)wfm.findNodeContainer(wmID);
            linksChecked += 1;
            WorkflowManager parent = wm.getParent();
            if (parent.checkUpdateMetaNodeLink(wm.getID(), lH)) {
                MetaNodeLinkUpdateResult loadResult = parent.updateMetaNodeLink(wm.getID(), new ExecutionMonitor(), lH);
                linksUpdated += 1;
                if (failOnLoadError && loadResult.hasErrors()) {
                    LOGGER.error(loadResult.getFilteredError("", LoadResultEntryType.Error));
                    throw new IOException("Error(s) while updating meta node links");
                }
            }
        }
        if (linksChecked == 0) {
            LOGGER.debug("No meta node links in workflow, nothing updated");
        } else {
            LOGGER.debug("Workflow contains " + linksChecked + " meta node link(s), " + linksUpdated + " were updated");
        }
    }

    private static void setNodeOptions(final Collection<Option> options, final WorkflowManager wfm)
            throws InvalidSettingsException, IllegalOptionException {
        for (Option o : options) {
            int[] idPath = o.m_nodeIDs;
            NodeID subID = new NodeID(wfm.getID(), idPath[0]);
            NodeContainer cont = null;
            try {
                cont = wfm.getNodeContainer(subID);
                for (int i = 1; i < idPath.length; i++) {
                    if (cont instanceof WorkflowManager) {
                        WorkflowManager subWM = (WorkflowManager)cont;
                        subID = new NodeID(subID, idPath[i]);
                        cont = subWM.getNodeContainer(subID);
                    } else {
                        cont = null;
                    }
                }
            } catch (IllegalArgumentException ex) {
                // throw by getNodeContainer if no node with the id exists
                cont = null;
            }
            if (cont == null) {
                LOGGER.warn("No node with id " + Arrays.toString(idPath) + " found.");
            } else {
                WorkflowManager parent = cont.getParent();
                NodeSettings settings = new NodeSettings("something");
                parent.saveNodeSettings(cont.getID(), settings);
                NodeSettings model = settings.getNodeSettings(Node.CFG_MODEL);
                String[] splitName = o.m_name.split("/");
                String name = splitName[splitName.length - 1];
                String[] pathElements = new String[splitName.length - 1];
                System.arraycopy(splitName, 0, pathElements, 0, pathElements.length);
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
                } else if ("float".equals(o.m_type) || ("double".equals(o.m_type))) {
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
                    throw new IllegalOptionException("Unknown option type for " + o.m_name + ": " + o.m_type);
                }
                parent.loadNodeSettings(cont.getID(), settings);
            }
        }
    }

    /**
     * Sets the workspace preferences using the given file.
     *
     * @param preferenceFile the preferences file
     * @throws FileNotFoundException if the given file is not a file or does not exist
     * @throws CoreException if applying the preferences fails
     * @since 2.7
     */
    protected static void setPreferences(final File preferenceFile) throws FileNotFoundException, CoreException {
        if (!preferenceFile.isFile()) {
            throw new FileNotFoundException("Preference file '" + preferenceFile.getAbsolutePath() + "' does not exist");
        }

        InputStream in = new BufferedInputStream(new FileInputStream(preferenceFile));
        IPreferencesService prefService = Platform.getPreferencesService();
        IExportedPreferences prefs = prefService.readPreferences(in);
        IPreferenceFilter filter = new IPreferenceFilter() {
            @Override
            public String[] getScopes() {
                return new String[]{InstanceScope.SCOPE, ConfigurationScope.SCOPE, "profile"};
            }

            @Override
            @SuppressWarnings("rawtypes")
            public Map getMapping(final String scope) {
                return null; // this filter is applicable for all nodes
            }
        };
        /*
         * Calling this method with filters and not the applyPreferences without
         * filters is very important! The other method does not merge the
         * preferences but deletes all default values.
         */
        prefService.applyPreferences(prefs, new IPreferenceFilter[]{filter});
    }

    private static void setupEncryptionKey(final boolean isPromptForPassword, String masterKey) {
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
            KnimeEncryption.setEncryptionKeySupplier(new EncryptionKeySupplier() {
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

    /**
     * Splits the argument to -workflow.variable into its sub-components (name, value, type) and returns it as array.
     *
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
        /* return as token= */false,
        /* include in token= */false);
        tokenizer.setSettings(settings);
        ArrayList<String> tokenList = new ArrayList<String>();
        String token;
        while ((token = tokenizer.nextToken()) != null) {
            tokenList.add(token);
        }
        return tokenList.toArray(new String[tokenList.size()]);
    }

    /**
     * Creates a new flow variable from the sub-components of the -workflow.variables commandline argument. If the
     * string array does not meet the requirements (e.g. length = 3), an exception is thrown.
     *
     * @param args The arguments for the variable.
     * @return A new flow variable.
     * @throws IllegalOptionException
     */
    private static FlowVariable createWorkflowVariable(final String[] args) throws IllegalOptionException {
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
            throw new IllegalOptionException("Invalid type for workflow variable " + name + ": " + type);
        }
    }

    /**
     * Injects the workflow variables provided in the last argument into the workflow.
     *
     * @param wfm The workflow, where to inject the variables
     * @param reset Whether to reset the workflow {@link WorkflowManager#addWorkflowVariables(boolean, FlowVariable...)}
     * @param wkfVars The flow variables.
     */
    private static void applyWorkflowVariables(final WorkflowManager wfm, final boolean reset,
                                               final Collection<FlowVariable> wkfVars) {

        /**
         * Check if the names of all passed flow variables are defined for the workflow. Only the name is used for the
         * comparison (not the type) to be consistent with the addWorkflowVariables method in the workflow manager.
         */
        HashSet<String> defined = new HashSet<String>();
        for (FlowVariable defVar : wfm.getWorkflowVariables()) {
            defined.add(defVar.getName());
        }
        HashSet<FlowVariable> unknown = new HashSet<FlowVariable>();
        for (FlowVariable var : wkfVars) {
            if (!defined.contains(var.getName())) {
                unknown.add(var);
            }
        }

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
        wfm.addWorkflowVariables(!reset, wkfVars.toArray(new FlowVariable[wkfVars.size()]));
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

    /**
     * Creates a new workflow configuration object. Subclasses may override this method to create custom workflow
     * configurations.
     *
     * @return a new workflow configuration object
     * @since 2.7
     */
    protected WorkflowConfiguration createNewConfiguration() {
        return new WorkflowConfiguration();
    }

    /**
     * Checks the workflow configuration after all paramaters have been read.
     *
     * @param config a workflow configuration
     * @throws IllegalOptionException if an option is invalid
     * @since 2.7
     */
    protected void checkConfiguration(final WorkflowConfiguration config) throws IllegalOptionException {
        if (config.inputWorkflow == null) {
            throw new IllegalOptionException("No workflow file or directory given.");
        }
    }
}
