/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodePersistor.LoadNodeModelSettingsFailPolicy;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.ConfigPasswordEntry;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowLoopContext.RestoredFlowLoopContext;
import org.knime.core.node.workflow.SingleNodeContainer.MemoryPolicy;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.LockFailedException;

/**
 *
 * @author wiswedel, University of Konstanz
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public abstract class FileSingleNodeContainerPersistor implements SingleNodeContainerPersistor,
    FromFileNodeContainerPersistor {

    private static final NodeLogger SAVE_LOGGER = NodeLogger.getLogger(FileSingleNodeContainerPersistor.class);
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final LoadVersion m_version;

    /** Meta persistor, only set when used to load a workflow. */
    private final FileNodeContainerMetaPersistor m_metaPersistor;

    private NodeSettingsRO m_nodeSettings;

    private SingleNodeContainerSettings m_sncSettings;

    private boolean m_needsResetAfterLoad;

    private boolean m_isDirtyAfterLoad;

    private List<FlowObject> m_flowObjects;

    private final boolean m_mustWarnOnDataLoadError;

    private final WorkflowDataRepository m_workflowDataRepository;


    /** Load persistor.
     * @param workflowDataRepository TODO
     * @param mustWarnOnDataLoadError TODO*/
    FileSingleNodeContainerPersistor(final ReferencedFile nodeSettingsFile,
        final WorkflowLoadHelper loadHelper, final LoadVersion version,
        final WorkflowDataRepository workflowDataRepository,
        final boolean mustWarnOnDataLoadError) {
        CheckUtils.checkArgumentNotNull(version, "Version must not be null");
        CheckUtils.checkArgumentNotNull(workflowDataRepository, "File store handler repository must not be null");
        m_version = version;
        m_metaPersistor = new FileNodeContainerMetaPersistor(nodeSettingsFile, loadHelper, version);
        m_workflowDataRepository = workflowDataRepository;
        m_mustWarnOnDataLoadError = mustWarnOnDataLoadError;
    }

    LoadVersion getLoadVersion() {
        return m_version;
    }

    protected NodeLogger getLogger() {
        return m_logger;
    }

    /** {@inheritDoc} */
    @Override
    public boolean needsResetAfterLoad() {
        return m_needsResetAfterLoad;
    }

    /** Indicate that node should be reset after load (due to load problems). */
    public void setNeedsResetAfterLoad() {
        m_needsResetAfterLoad = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirtyAfterLoad() {
        return m_isDirtyAfterLoad;
    }

    /** {@inheritDoc} */
    @Override
    public boolean mustComplainIfStateDoesNotMatch() {
        return true;
    }

    /** Mark dirty after (when run into error). */
    public void setDirtyAfterLoad() {
        m_isDirtyAfterLoad = true;
    }

    @Override
    public final FileNodeContainerMetaPersistor getMetaPersistor() {
        return m_metaPersistor;
    }

    /** @return Directory associated with node.
     * @since 2.8 */
    public ReferencedFile getNodeContainerDirectory() {
        return m_metaPersistor.getNodeContainerDirectory();
    }

    public WorkflowLoadHelper getLoadHelper() {
        return m_metaPersistor.getLoadHelper();
    }

    /** {@inheritDoc} */
    @Override
    public SingleNodeContainerSettings getSNCSettings() {
        return m_sncSettings;
    }

    /**
     * @return the nodeSettings
     */
    public NodeSettingsRO getNodeSettings() {
        return m_nodeSettings;
    }

    /** {@inheritDoc} */
    @Override
    public List<FlowObject> getFlowObjects() {
        return m_flowObjects;
    }

    /** {@inheritDoc} */
    @Override
    public void preLoadNodeContainer(final WorkflowPersistor parentPersistor, final NodeSettingsRO parentSettings,
        final LoadResult result) throws InvalidSettingsException, IOException {
        FileNodeContainerMetaPersistor meta = getMetaPersistor();
        final ReferencedFile settingsFileRef = meta.getNodeSettingsFile();
        File settingsFile = settingsFileRef.getFile();
        if (!settingsFile.isFile()) {
            setDirtyAfterLoad();
            throw new IOException("Can't read node file \"" + settingsFile.getAbsolutePath() + "\"");
        }
        NodeSettingsRO settings;
        try {
            InputStream in = new FileInputStream(settingsFile);
            // parentPersitor is null for loaded subnode templates
            in = parentPersistor == null ? in : parentPersistor.decipherInput(in);
            settings = NodeSettings.loadFromXML(new BufferedInputStream(in));
        } catch (IOException ioe) {
            setDirtyAfterLoad();
            throw ioe;
        }

        boolean resetRequired = meta.load(settings, parentSettings, result);
        m_nodeSettings = settings;
        if (resetRequired) {
            setNeedsResetAfterLoad();
            setDirtyAfterLoad();
        }
        if (meta.isDirtyAfterLoad()) {
            setDirtyAfterLoad();
        }

    }

    /** {@inheritDoc} */
    @Override
    public void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec,
        final LoadResult result) throws InvalidSettingsException, CanceledExecutionException, IOException {
        final NodeSettingsRO settingsForNode = loadSettingsForNode(result);
        m_sncSettings = new SingleNodeContainerSettings();
        exec.checkCanceled();
        try {
            m_sncSettings.setMemoryPolicy(loadMemoryPolicySettings(m_nodeSettings));
        } catch (InvalidSettingsException e) {
            String error = "Unable to load SNC settings: " + e.getMessage();
            result.addError(error);
            getLogger().debug(error, e);
            setDirtyAfterLoad();
        }
        NodeSettingsRO modelSettings = null;
        try {
            modelSettings = loadModelSettings(settingsForNode);
        } catch (InvalidSettingsException ise) {
            String error = "Unable to load model settings: " + ise.getMessage();
            result.addError(error);
            getLogger().debug(error, ise);
            setDirtyAfterLoad();
        }
        if (Node.DISALLOW_WEAK_PASSWORDS_IN_NODE_CONFIGURATION && modelSettings != null
            && ConfigPasswordEntry.containsPassword((NodeSettings)modelSettings, false)) {
            result.addWarning(String.format(
                "Node stores passwords in its configuration. These will be lost when saving "
                    + "the workflow in this installation (as per \"%s\" system property)",
                KNIMEConstants.PROPERTY_WEAK_PASSWORDS_IN_SETTINGS_FORBIDDEN));
        }
        try {
            modelSettings = loadNCAndModelSettings(settingsForNode, modelSettings, tblRep, exec, result);
        } catch (InvalidSettingsException ise) {
            String error = "Unable to load node container and wash settings: " + ise.getMessage();
            result.addError(error);
            getLogger().debug(error, ise);
            setDirtyAfterLoad();
        }
        m_sncSettings.setModelSettings(modelSettings);

        try {
            m_sncSettings.setViewSettings(loadViewSettings(settingsForNode));
        } catch (InvalidSettingsException e) {
            String msg = "Unable load view settings: " + e.getMessage();
            result.addError(msg);
            getLogger().debug(msg, e);
            setDirtyAfterLoad();
        }

        try {
            m_sncSettings.setViewVariablesSettings(loadViewVariablesSettings(settingsForNode));
        } catch (InvalidSettingsException e) {
            String msg = "Unable load view variable settings: " + e.getMessage();
            result.addError(msg);
            getLogger().debug(msg, e);
            setDirtyAfterLoad();
        }

        try {
            m_sncSettings.setVariablesSettings(loadVariableSettings(settingsForNode));
        } catch (InvalidSettingsException e) {
            String msg = "Could not load variable settings: " + e.getMessage();
            result.addError(msg);
            setDirtyAfterLoad();
            setNeedsResetAfterLoad();
        }

        try {
            m_flowObjects = loadFlowObjects(m_nodeSettings);
        } catch (Exception e) {
            m_flowObjects = Collections.emptyList();
            String error = "Error loading flow variables: " + e.getMessage();
            getLogger().warn(error, e);
            result.addError(error);
            setDirtyAfterLoad();
            setNeedsResetAfterLoad();
        }
        exec.setProgress(1.0);
    }

    /**
     * Called by {@link #loadNodeContainer(Map, ExecutionMonitor, LoadResult)}. Will instantiate the node and load (and
     * validate) the model settings into it.
     *
     * @param settingsForNode the settings for the node, including the hasContent flag etc (the whole settings.xml)
     * @param modelSettings The settings to load the model settings into (the child "model")
     * @param tblRep Workflow's table repository
     * @param exec ...
     * @param result
     * @return the model settings as stored with the node
     * @throws InvalidSettingsException if, e.g., loading the model settings failed
     * @throws CanceledExecutionException ...
     * @throws IOException ...
     */
    abstract NodeSettingsRO loadNCAndModelSettings(final NodeSettingsRO settingsForNode,
        final NodeSettingsRO modelSettings, final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec,
        final LoadResult result) throws InvalidSettingsException, CanceledExecutionException, IOException;

        /** Loads the settings passed to the NodePersistor class. It includes node settings, variable settings,
     * file store information (but not node factory information). In 2.7 and before it was contained in a separate
     * node.xml, in 2.8+ it's all in one file.
     * @param loadResult ...
     * @return ...
     * @throws IOException ...
     */
    abstract NodeSettingsRO loadSettingsForNode(final LoadResult loadResult) throws IOException;
    /**
     * Called during load by the Node instance to determine whether or not the flow variable output port is to be
     * populated with the FlowVariablePortObjectSpec singleton.
     *
     * @return true if node's state is not idle
     * @since 2.7
     */
    public boolean hasConfiguredState() {
        if (m_metaPersistor == null) {
            throw new IllegalStateException("preloadnodecontainer hasn't been called yet");
        }
        switch (m_metaPersistor.getState()) {
            case IDLE:
            case UNCONFIGURED_MARKEDFOREXEC:
                return false;
            default:
                return true;
        }
    }

    /**
     * Reads sub settings object.
     *
     * @param settings ...
     * @return child settings (here: null)
     * @throws InvalidSettingsException ...
     */
    NodeSettingsRO loadAdditionalFactorySettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // added in v2.6 (during hittisau 2012) without changing the version
        // number (current load version is V250("2.5.0"))
        if (settings.containsKey("factory_settings")) {
            return settings.getNodeSettings("factory_settings");
        }
        return null;
    }

    /**
     * Load file containing node settings.
     *
     * @param settings to load from, used in sub-classes, ignored here.
     * @return pointer to "settings.xml"
     * @throws InvalidSettingsException If that fails for any reason.
     */
    ReferencedFile loadNodeFile(final NodeSettingsRO settings) throws InvalidSettingsException {
        ReferencedFile nodeDir = getMetaPersistor().getNodeContainerDirectory();
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return new ReferencedFile(nodeDir, SETTINGS_FILE_NAME);
        } else {
            return new ReferencedFile(nodeDir, settings.getString("node_file"));
        }
    }

    /**
     * Load configuration of node.
     *
     * @param nodeSettings to load from (used in sub-classes)
     * @return node config
     * @throws InvalidSettingsException if that fails for any reason.
     */
    MemoryPolicy loadMemoryPolicySettings(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V210_Pre)) {
            // in versions before KNIME 1.2.0, there were no misc settings
            // in the dialog, we must use caution here: if they are not present
            // we use the default, i.e. small data are kept in memory
            if (nodeSettings.containsKey(Node.CFG_MISC_SETTINGS)
                && nodeSettings.getNodeSettings(Node.CFG_MISC_SETTINGS).containsKey(SingleNodeContainer.CFG_MEMORY_POLICY)) {
                NodeSettingsRO sub = nodeSettings.getNodeSettings(Node.CFG_MISC_SETTINGS);
                String memoryPolicy =
                    sub.getString(SingleNodeContainer.CFG_MEMORY_POLICY, MemoryPolicy.CacheSmallInMemory.toString());
                if (memoryPolicy == null) {
                    throw new InvalidSettingsException("Can't use null memory policy.");
                }
                try {
                    return MemoryPolicy.valueOf(memoryPolicy);
                } catch (IllegalArgumentException iae) {
                    throw new InvalidSettingsException("Invalid memory policy: " + memoryPolicy);
                }
            } else {
                return MemoryPolicy.CacheSmallInMemory;
            }
        } else {
            // any version after 2.0 saves the snc settings in the settings.xml
            // (previously these settings were saves as part of the node.xml)
            NodeSettingsRO sub = nodeSettings.getNodeSettings(Node.CFG_MISC_SETTINGS);
            String memoryPolicy =
                sub.getString(SingleNodeContainer.CFG_MEMORY_POLICY, MemoryPolicy.CacheSmallInMemory.toString());
            if (memoryPolicy == null) {
                throw new InvalidSettingsException("Can't use null memory policy.");
            }
            try {
                return MemoryPolicy.valueOf(memoryPolicy);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid memory policy: " + memoryPolicy);
            }
        }
    }

    /** The settings passed to the NodeModel's validate and load method.
     * @param settings The whole settings tree.
     * @return the settings child {@link SingleNodeContainer#CFG_MODEL}.
     * @throws InvalidSettingsException ... */
    NodeSettings loadModelSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // settings not present if node never had settings (different since 2.8 -- before the node always had settings,
        // defined by NodeModel#saveSettings -- these settings were never confirmed through validate/load though)
        if (settings.containsKey(SingleNodeContainer.CFG_MODEL)) {
            return (NodeSettings)settings.getNodeSettings(SingleNodeContainer.CFG_MODEL);
        }
        return null;
    }

    private static NodeSettings loadViewSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(SingleNodeContainer.CFG_VIEW)) {
            return (NodeSettings)settings.getNodeSettings(SingleNodeContainer.CFG_VIEW);
        }
        return null;
    }

    private static NodeSettings loadViewVariablesSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(SingleNodeContainer.CFG_VIEW_VARIABLES)) {
            return (NodeSettings)settings.getNodeSettings(SingleNodeContainer.CFG_VIEW_VARIABLES);
        }
        return null;
    }

    /** The variable settings (whatever overwrites user parameters).
     * @param settings The whole settings tree.
     * @return the settings child {@link SingleNodeContainer#CFG_VARIABLES} if present, or null.
     * @throws InvalidSettingsException ... */
    NodeSettings loadVariableSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(SingleNodeContainer.CFG_VARIABLES)) {
            return (NodeSettings)settings.getNodeSettings(SingleNodeContainer.CFG_VARIABLES);
        }
        return null;
    }

    /**
     * Load from variables.
     *
     * @param settings to load from, ignored in this implementation (flow variables added in later versions)
     * @return an empty list.
     * @throws InvalidSettingsException if that fails for any reason.
     */
    List<FlowObject> loadFlowObjects(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return Collections.emptyList();
        }
        List<FlowObject> result = new ArrayList<FlowObject>();
        NodeSettingsRO stackSet;
        if (getLoadVersion().isOlderThan(LoadVersion.V220)) {
            stackSet = settings.getNodeSettings("scope_stack");
        } else {
            stackSet = settings.getNodeSettings("flow_stack");
        }
        for (String key : stackSet.keySet()) {
            NodeSettingsRO sub = stackSet.getNodeSettings(key);
            String type = sub.getString("type");
            if ("variable".equals(type)) {
                FlowVariable v = FlowVariable.load(sub);
                result.add(v);
            } else if ("loopcontext".equals(type)) {
                result.add(new RestoredFlowLoopContext());
                //                int tailID = sub.getInt("tailID");
            } else if ("loopcontext_execute".equals(type)) {
                result.add(new InnerFlowLoopExecuteMarker());
            } else if ("loopcontext_inactive".equals(type)) {
                FlowLoopContext flc = new FlowLoopContext();
                flc.inactiveScope(true);
                result.add(flc);
            } else if ("flowcapturecontext".equals(type)) {
                result.add(new FlowCaptureContext());
            } else if ("flowcapturecontext_inactive".equals(type)) {
                FlowScopeContext slc = new FlowCaptureContext();
                slc.inactiveScope(true);
                result.add(slc);
            } else if ("scopecontext".equals(type)) {
                result.add(new FlowScopeContext());
            } else if ("scopecontext_inactive".equals(type)) {
                FlowScopeContext slc = new FlowScopeContext();
                slc.inactiveScope(true);
                result.add(slc);
            } else {
                throw new InvalidSettingsException("Unknown flow object type: " + type);
            }
        }
        return result;
    }

    boolean shouldFixModelPortOrder() {
        return getLoadVersion().isOlderThan(LoadVersion.V200);
    }

    /** @return the data repository as passed in constructor, not null. */
    WorkflowDataRepository getWorkflowDataRepository() {
        return m_workflowDataRepository;
    }

    /** @return property derived from outer workflow persistor,
     *          see {@link WorkflowPersistor#mustWarnOnDataLoadError()}. */
    public boolean mustWarnOnDataLoadError() {
        return m_mustWarnOnDataLoadError;
    }

    /**
     * @param state State of the node, not null.
     * @param isInactive whether node is inactive - special case where invalid settings are OK.
     * @return policy describing whether failures during configuration load are accepted, not null.
     * @noreference This method is not intended to be referenced by clients.
     */
    public LoadNodeModelSettingsFailPolicy getModelSettingsFailPolicy(
        final InternalNodeContainerState state, final boolean isInactive) {
        if (isInactive) {
            // ignore state for inactive nodes as they can always be executed independent of their settings (bug 6729)
            return LoadNodeModelSettingsFailPolicy.IGNORE;
        }
        switch (state) {
            case IDLE:
                return LoadNodeModelSettingsFailPolicy.IGNORE;
            case EXECUTED:
                return LoadNodeModelSettingsFailPolicy.WARN;
            default:
                return LoadNodeModelSettingsFailPolicy.FAIL;
        }
    }

    /** @noreference This method is not intended to be referenced by clients. */
    protected static String save(final SingleNodeContainer singleNC, final ReferencedFile rawNodeDirRef,
        final ExecutionMonitor exec, final WorkflowSaveHelper saveHelper)
                throws CanceledExecutionException, IOException, LockFailedException {
        String settingsDotXML = singleNC.getDirectNCParent().getCipherFileName(SETTINGS_FILE_NAME);
        ReferencedFile nodeDirRef = rawNodeDirRef;
        ReferencedFile sncWorkingDirRef = singleNC.getNodeContainerDirectory();
        ReferencedFile sncAutoSaveDirRef = singleNC.getAutoSaveDirectory();
        File nodeDir = nodeDirRef.getFile();
        boolean nodeDirExists = nodeDir.exists();
        // the if-checks below also update the nodeDirRef so that we can make changes on that object
        if (!saveHelper.isAutoSave() && nodeDirRef.equals(sncWorkingDirRef)) {
            if (!sncWorkingDirRef.isDirty() && nodeDirExists) {
                return settingsDotXML;
            } else {
                nodeDirRef = sncWorkingDirRef;
            }
        }
        if (saveHelper.isAutoSave() && nodeDirRef.equals(sncAutoSaveDirRef)) {
            if (!sncAutoSaveDirRef.isDirty() && nodeDirExists) {
                return settingsDotXML;
            } else {
                nodeDirRef = sncAutoSaveDirRef;
            }
        }
        boolean nodeDirDeleted = true;
        if (singleNC instanceof NativeNodeContainer) {
            nodeDirDeleted = deleteChildren(nodeDir, SingleNodeContainer.DROP_DIR_NAME);
        }
        nodeDir.mkdirs();
        if (!nodeDir.isDirectory() || !nodeDir.canWrite()) {
            throw new IOException("Unable to write or create directory \"" + nodeDirRef + "\"");
        }
        String debug;
        if (singleNC instanceof NativeNodeContainer && nodeDirExists) {
            if (nodeDirDeleted) {
                debug = "Replaced node directory \"" + nodeDirRef + "\"";
            } else {
                debug = "Failed to replace node directory \"" + nodeDirRef + "\" -- writing into existing directory";
            }
        } else {
            debug = "Created node directory \"" + nodeDirRef + "\"";
        }
        SAVE_LOGGER.debug(debug);

        // get drop directory in "home" (the designated working dir)
        ReferencedFile nodeDropDirInWDRef =
            sncWorkingDirRef == null ? null : new ReferencedFile(sncWorkingDirRef, SingleNodeContainer.DROP_DIR_NAME);

        ReferencedFile nodeDropDirRef = new ReferencedFile(nodeDirRef, SingleNodeContainer.DROP_DIR_NAME);

        // if node container directory is set and we write into a new location
        if (nodeDropDirInWDRef != null && !nodeDropDirRef.equals(nodeDropDirInWDRef)) {

            // this code is executed in either of the two cases:
            // - Node was copy&paste from node with drop folder
            //   (its (freshly copied) drop folder is currently in /tmp)
            // - Node is saved into new location (saveAs) -- need to copy
            //   the drop folder there (either from /tmp or from working dir)
            File dropInSource = nodeDropDirInWDRef.getFile();
            File dropInTarget = new File(nodeDir, SingleNodeContainer.DROP_DIR_NAME);
            if (dropInSource.exists()) {
                FileUtil.copyDir(dropInSource, dropInTarget);
            }
        }
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        saveNodeFileName(singleNC, settings, nodeDirRef); // only to allow 2.7- clients to load 2.8+ workflows
        saveFlowObjectStack(settings, singleNC);
        saveSNCSettings(settings, singleNC);
        FileNodeContainerMetaPersistor.save(settings, singleNC, nodeDirRef);
        if (singleNC instanceof NativeNodeContainer) {
            NativeNodeContainer nativeNC = (NativeNodeContainer)singleNC;
            FileNativeNodeContainerPersistor.save(nativeNC, settings, exec, nodeDirRef,
                saveHelper.isSaveData() && singleNC.getInternalState().equals(InternalNodeContainerState.EXECUTED));
        } else {
            SubNodeContainer subnodeNC = (SubNodeContainer)singleNC;
            FileSubNodeContainerPersistor.save(subnodeNC, settings, exec, nodeDirRef, saveHelper);
        }
        File nodeSettingsXMLFile = new File(nodeDir, settingsDotXML);
        OutputStream os = new FileOutputStream(nodeSettingsXMLFile);
        os = singleNC.getDirectNCParent().cipherOutput(os);
        settings.saveToXML(os);
        if (saveHelper.isAutoSave() && sncAutoSaveDirRef == null) {
            sncAutoSaveDirRef = nodeDirRef;
            singleNC.setAutoSaveDirectory(sncAutoSaveDirRef);
        }
        if (!saveHelper.isAutoSave() && sncWorkingDirRef == null) {
            // set working dir so that we can unset the dirty flag
            sncWorkingDirRef = nodeDirRef;
            singleNC.setNodeContainerDirectory(sncWorkingDirRef);
        }
        nodeDirRef.setDirty(false);
        if (nodeDirRef.equals(sncWorkingDirRef)) {
            singleNC.unsetDirty();
        }
        exec.setProgress(1.0);
        return settingsDotXML;
    }


    protected static ReferencedFile saveNodeFileName(final SingleNodeContainer snc, final NodeSettingsWO settings,
        final ReferencedFile nodeDirectoryRef) {
        // KNIME 2.7- reads from this file. It used to be "node.xml", which was removed in 2.8 and now the settings.xml
        // (contains the settings from this method argument) contains everything. We save the node_file so that
        // old KNIME instances can read new workflows
        String fileName = SETTINGS_FILE_NAME;
        fileName = snc.getDirectNCParent().getCipherFileName(fileName);
        settings.addString("node_file", fileName);
        return new ReferencedFile(nodeDirectoryRef, fileName);
    }

    protected static void saveSNCSettings(final NodeSettingsWO settings, final SingleNodeContainer snc) {
        // if no settings are stored in the SNCSettings, use the ones from the NodeModel as
        // default (node uses default configuration after new instantiation), fixes bug 4364
        NodeSettings temp = new NodeSettings("key_ignored");
        snc.saveSNCSettings(temp, true);
        if (Node.DISALLOW_WEAK_PASSWORDS_IN_NODE_CONFIGURATION && ConfigPasswordEntry.containsPassword(temp, false)) {
            ConfigPasswordEntry.replacePasswordsWithNull(temp);
            NodeContext.pushContext(snc);
            try {
                NodeLogger.getLogger(FileSingleNodeContainerPersistor.class).debugWithFormat("Settings of node \"%s\" "
                        + "contains a password, storing it as null/invalid (as per \"%s\" system property)",
                        snc.getNameWithID(), KNIMEConstants.PROPERTY_WEAK_PASSWORDS_IN_SETTINGS_FORBIDDEN);
            } finally {
                NodeContext.removeLastContext();
            }
        }
        temp.copyTo(settings);
    }

    protected static void saveFlowObjectStack(final NodeSettingsWO settings, final SingleNodeContainer nc) {
        NodeSettingsWO stackSet = settings.addNodeSettings("flow_stack");
        FlowObjectStack stack = nc.getOutgoingFlowObjectStack();
        @SuppressWarnings("unchecked")
        Iterable<FlowObject> myObjs = stack == null //
            ? Collections.emptyList() //
            : stack.getNonLocalFlowObjectsOwnedBy(nc.getID());
        int c = 0;
        for (FlowObject s : myObjs) {
            if (s instanceof FlowVariable) {
                FlowVariable v = (FlowVariable)s;
                NodeSettingsWO sub = stackSet.addNodeSettings("Variable_" + c);
                sub.addString("type", "variable");
                v.save(sub);
            } else if (s instanceof FlowLoopContext) {
                if (!((FlowLoopContext)s).isInactiveScope()) {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Loop_" + c);
                    sub.addString("type", "loopcontext");
                } else {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Inactive_Loop_" + c);
                    sub.addString("type", "loopcontext_inactive");
                }
            } else if (s instanceof InnerFlowLoopExecuteMarker) {
                NodeSettingsWO sub = stackSet.addNodeSettings("Loop_Execute_" + c);
                sub.addString("type", "loopcontext_execute");
            } else if (s instanceof FlowCaptureContext) {
                if (!((FlowScopeContext)s).isInactiveScope()) {
                    NodeSettingsWO sub = stackSet.addNodeSettings("FlowCapture_" + c);
                    sub.addString("type", "flowcapturecontext");
                } else {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Inactive_FlowCapture_" + c);
                    sub.addString("type", "flowcapturecontext_inactive");
                }
            } else if (s instanceof FlowScopeContext) {
                if (!((FlowScopeContext)s).isInactiveScope()) {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Scope_" + c);
                    sub.addString("type", "scopecontext");
                } else {
                    NodeSettingsWO sub = stackSet.addNodeSettings("Inactive_Scope_" + c);
                    sub.addString("type", "scopecontext_inactive");
                }
            } else {
                SAVE_LOGGER.error("Saving of flow objects of type \"" + s.getClass().getSimpleName()
                    + "\" not implemented");
            }
            c += 1;
        }
    }

    /**
     * Delete content of directory, skipping (direct) childs as given in 2nd argument. Use case is: to delete a node
     * directory but skip its drop folder.
     *
     * @param directory The directory whose content is to be deleted
     * @param exclude A list of direct child names that are to be skipped
     * @return false if directory does not exist, true if non-listed children are deleted
     */
    private static boolean deleteChildren(final File directory, final String... exclude) {
        if (!directory.isDirectory()) {
            return false;
        }
        HashSet<String> excludeSet = new HashSet<String>(Arrays.asList(exclude));
        File[] children = directory.listFiles();
        if (children == null) {
            return true;
        }
        boolean success = true;
        for (File f : children) {
            if (!excludeSet.contains(f.getName())) {
                boolean s = FileUtil.deleteRecursively(f);
                success &= s;
            }
        }
        return success;
    }

}
