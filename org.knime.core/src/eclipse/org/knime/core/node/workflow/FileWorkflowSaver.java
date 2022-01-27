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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.knime.core.data.TableBackend;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LockFailedException;
import org.knime.core.util.workflowalizer.AuthorInformation;

/**
 * Recursively walks through the legacy directory structure, generating the workflow defs.
 *
 * TODO remove this comment: Used to implement {@link WorkflowPersistor} and {@link TemplateNodeContainerPersistor}
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class FileWorkflowSaver {

    protected static void saveUIInfoClassName(final NodeSettingsWO settings, final UIInformation info) {
        settings.addString(FileWorkflowPersistor.CFG_UIINFO_CLASS, info != null ? info.getClass().getName() : null);
    }

    protected static void saveUIInfoSettings(final NodeSettingsWO settings, final UIInformation uiInfo) {
        if (uiInfo == null) {
            return;
        }
        // nest into separate sub config
        NodeSettingsWO subConfig = settings.addNodeSettings(FileWorkflowPersistor.CFG_UIINFO_SUB_CONFIG);
        uiInfo.save(subConfig);
    }

    protected static void saveWorkflowName(final NodeSettingsWO settings, final String name) {
        settings.addString("name", name);
    }

    /**
     * Metanode locking information.
     *
     * @param settings
     * @param workflowCipher
     */
    protected static void saveWorkflowCipher(final NodeSettings settings, final WorkflowCipher workflowCipher) {
        if (!workflowCipher.isNullCipher()) {
            NodeSettingsWO cipherSettings = settings.addNodeSettings("cipher");
            workflowCipher.save(cipherSettings);
        }
    }

    /** @since 3.7*/
    protected static void saveAuthorInformation(final AuthorInformation aI, final NodeSettingsWO settings) {
        if (aI != null) {
            final NodeSettingsWO sub = settings.addNodeSettings(FileWorkflowLoader.CFG_AUTHOR_INFORMATION);
            sub.addString("authored-by", aI.getAuthor());
            String authorWhen = aI.getAuthoredDate() == null ? null : FileWorkflowPersistor.formatDate(aI.getAuthoredDate());
            sub.addString("authored-when", authorWhen);
            sub.addString("lastEdited-by", aI.getLastEditor().orElse(null));
            String lastEditWhen = aI.getLastEditDate() == null ? null
                : aI.getLastEditDate().isPresent() ? FileWorkflowPersistor.formatDate(aI.getLastEditDate().get()) : null;
            sub.addString("lastEdited-when", lastEditWhen);
        }
    }

    protected static void saveWorkflowVariables(final WorkflowManager wfm, final NodeSettingsWO settings) {
        List<FlowVariable> vars = wfm.getWorkflowVariables();
        if (!vars.isEmpty()) {
            NodeSettingsWO wfmVarSub = settings.addNodeSettings(FileWorkflowLoader.CFG_WKF_VARIABLES);
            int i = 0;
            for (FlowVariable v : vars) {
                v.save(wfmVarSub.addNodeSettings("Var_" + (i++)));
            }
        }
    }

    protected static void saveCredentials(final WorkflowManager wfm, final NodeSettingsWO settings) {
        CredentialsStore credentialsStore = wfm.getCredentialsStore();
        NodeSettingsWO sub = settings.addNodeSettings(FileWorkflowLoader.CFG_CREDENTIALS);
        synchronized (credentialsStore) {
            for (Credentials c : credentialsStore.getCredentials()) {
                NodeSettingsWO s = sub.addNodeSettings(c.getName());
                c.save(s);
            }
        }
    }

    /** Save the {@link TableBackend} set on workflow projects.
     * @since 4.3
     */
    protected static void saveTableBackend(final WorkflowManager wfm, final NodeSettingsWO settings) {
        wfm.getTableBackendSettings().ifPresent(backendSettings -> backendSettings.saveSettingsTo(settings));
    }

    protected static void saveWorkflowAnnotations(final WorkflowManager manager, final NodeSettingsWO settings) {
        Collection<WorkflowAnnotation> annotations = manager.getWorkflowAnnotations();
        if (annotations.isEmpty()) {
            return;
        }
        NodeSettingsWO annoSettings = settings.addNodeSettings("annotations");
        int i = 0;
        for (Annotation a : annotations) {
            NodeSettingsWO t = annoSettings.addNodeSettings("annotation_" + i);
            a.save(t);
            i += 1;
        }
    }

    /**
     * Save nodes in an own sub-config object as a series of configs.
     *
     * @param settings To save to.
     * @return The sub config where subsequent writing takes place.
     */
    protected static NodeSettingsWO saveSettingsForNodes(final NodeSettingsWO settings) {
        return settings.addNodeSettings(WorkflowPersistor.KEY_NODES);
    }

    /**
     * Save connections in an own sub-config object.
     *
     * @param settings To save to.
     * @return The sub config where subsequent writing takes place.
     */
    protected static NodeSettingsWO saveSettingsForConnections(final NodeSettingsWO settings) {
        return settings.addNodeSettings(WorkflowPersistor.KEY_CONNECTIONS);
    }

    protected static void saveNodeContainer(final NodeSettingsWO settings, final ReferencedFile workflowDirRef,
        final NodeContainer container, final ExecutionMonitor exec, final WorkflowSaveHelper saveHelper)
        throws CanceledExecutionException, IOException, LockFailedException {
        WorkflowManager parent = container.getParent();
        ReferencedFile workingDir = parent.getNodeContainerDirectory();
        boolean isWorkingDir = workflowDirRef.equals(workingDir);

        saveNodeIDSuffix(settings, container);
        int idSuffix = container.getID().getIndex();

        // name of sub-directory container node/sub-workflow settings
        // all chars which are not letter or number are replaced by '_'
        final String containerName = container.getName();
        String nodeDirID =
            FileUtil.getValidFileName(containerName, container instanceof WorkflowManager
                || container instanceof SubNodeContainer ? 12 : -1);
        nodeDirID = nodeDirID.concat(" (#" + idSuffix + ")");

        // try to re-use previous node dir (might be different from calculated
        // one above in case node was renamed between releases)
        if (isWorkingDir && container.getNodeContainerDirectory() != null) {
            ReferencedFile ncDirectory = container.getNodeContainerDirectory();
            nodeDirID = ncDirectory.getFile().getName();
        }

        ReferencedFile nodeDirectoryRef = new ReferencedFile(workflowDirRef, nodeDirID);
        String fileName;
        if (container instanceof WorkflowManager) {
            fileName = FileWorkflowSaver.save((WorkflowManager)container, nodeDirectoryRef, exec, saveHelper);
        } else {
            fileName =  FileSingleNodeContainerPersistor.save(
                (SingleNodeContainer)container, nodeDirectoryRef, exec, saveHelper);
        }
        saveFileLocation(settings, nodeDirID + "/" + fileName);
        saveNodeType(settings, container);

        //save node UI info
        FileWorkflowSaver.saveNodeUIInformation(settings, container.getUIInformation());
    }

    protected static void saveNodeIDSuffix(final NodeSettingsWO settings, final NodeContainer nc) {
        settings.addInt(WorkflowPersistor.KEY_ID, nc.getID().getIndex());
    }

    protected static void saveFileLocation(final NodeSettingsWO settings, final String location) {
        settings.addString("node_settings_file", location);
    }

    protected static void saveNodeType(final NodeSettingsWO settings, final NodeContainer nc) {
        // obsolote since LoadVersion.V2100 - written to help old knime installs to read new workflows
        // treat sub and metanodes the same
        settings.addBoolean("node_is_meta", !(nc instanceof NativeNodeContainer));
        FileWorkflowPersistor.NodeType nodeType;
        if (nc instanceof NativeNodeContainer) {
            nodeType = FileWorkflowPersistor.NodeType.NativeNode;
        } else if (nc instanceof WorkflowManager) {
            nodeType = FileWorkflowPersistor.NodeType.MetaNode;
        } else if (nc instanceof SubNodeContainer) {
            nodeType = FileWorkflowPersistor.NodeType.SubNode;
        } else {
            throw new IllegalArgumentException(
                "Unsupported node container class: " + nc == null ? "<null>" : nc.getClass().getName());
        }
        settings.addString("node_type", nodeType.name()); // added for 2.10Pre
    }

    protected static NodeSettingsWO saveInPortsSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("meta_in_ports");
    }

    protected static NodeSettingsWO saveInPortsEnumSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("port_enum");
    }

    protected static NodeSettingsWO saveInPortSetting(final NodeSettingsWO settings, final int portIndex) {
        return settings.addNodeSettings("inport_" + portIndex);
    }

    /**
     * @since 3.5
     */
    protected static void saveInportsBarUIInfoClassName(final NodeSettingsWO settings, final NodeUIInformation info) {
        settings.addString(FileWorkflowPersistor.CFG_UIINFO_CLASS, info != null ? info.getClass().getName() : null);
    }

    /**
     * @since 3.5
     */
    protected static void saveInportsBarUIInfoSettings(final NodeSettingsWO settings, final NodeUIInformation uiInfo) {
        FileWorkflowSaver.saveNodeUIInformation(settings, uiInfo);
    }

    protected static void saveInPort(final NodeSettingsWO settings, final WorkflowManager wm, final int portIndex) {
        WorkflowInPort inport = wm.getInPort(portIndex);
        settings.addInt("index", portIndex);
        settings.addString("name", inport.getPortName());
        NodeSettingsWO portTypeSettings = settings.addNodeSettings("type");
        inport.getPortType().save(portTypeSettings);
    }

    protected static NodeSettingsWO saveOutPortsSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("meta_out_ports");
    }

    protected static NodeSettingsWO saveOutPortsEnumSetting(final NodeSettingsWO settings) {
        return settings.addNodeSettings("port_enum");
    }

    /**
     * @since 3.5
     */
    protected static void saveOutportsBarUIInfoClassName(final NodeSettingsWO settings, final NodeUIInformation info) {
        settings.addString(FileWorkflowPersistor.CFG_UIINFO_CLASS, info != null ? info.getClass().getName() : null);
    }

    /**
     * @since 3.5
     */
    protected static void saveOutportsBarUIInfoSettings(final NodeSettingsWO settings, final NodeUIInformation uiInfo) {
        FileWorkflowSaver.saveNodeUIInformation(settings, uiInfo);
    }

    protected static NodeSettingsWO saveOutPortSetting(final NodeSettingsWO settings, final int portIndex) {
        return settings.addNodeSettings("outport_" + portIndex);
    }

    protected static void saveOutPort(final NodeSettingsWO settings, final WorkflowManager wm, final int portIndex) {
        WorkflowOutPort outport = wm.getOutPort(portIndex);
        settings.addInt("index", portIndex);
        settings.addString("name", outport.getPortName());
        NodeSettingsWO portTypeSettings = settings.addNodeSettings("type");
        outport.getPortType().save(portTypeSettings);
    }

    protected static void saveConnection(final NodeSettingsWO settings, final ConnectionContainer connection) {
        int sourceID = connection.getSource().getIndex();
        int destID = connection.getDest().getIndex();
        switch (connection.getType()) {
            case WFMIN:
                sourceID = -1;
                break;
            case WFMOUT:
                destID = -1;
                break;
            case WFMTHROUGH:
                sourceID = -1;
                destID = -1;
                break;
            default:
                // all handled above
        }
        settings.addInt("sourceID", sourceID);
        settings.addInt("destID", destID);
        int sourcePort = connection.getSourcePort();
        settings.addInt("sourcePort", sourcePort);
        int targetPort = connection.getDestPort();
        settings.addInt("destPort", targetPort);
        ConnectionUIInformation uiInfo = connection.getUIInfo();
        if (uiInfo != null) {
            //TODO there is actually no need to store the class name - just keep it for now for backwards compatibility
            settings.addString(FileWorkflowPersistor.CFG_UIINFO_CLASS, uiInfo.getClass().getName());
            // nest into separate sub config
            NodeSettingsWO subConfig = settings.addNodeSettings(FileWorkflowPersistor.CFG_UIINFO_SUB_CONFIG);
            int[][] allBendpoints = uiInfo.getAllBendpoints();
            subConfig.addInt(FileWorkflowPersistor.KEY_BENDPOINTS + "_size", allBendpoints.length);
            for (int i = 0; i < allBendpoints.length; i++) {
                subConfig.addIntArray(FileWorkflowPersistor.KEY_BENDPOINTS + "_" + i, allBendpoints[i]);
            }
        }
        if (!connection.isDeletable()) {
            settings.addBoolean("isDeletable", false);
        }
    }

    public static String save(final WorkflowManager wm, final ReferencedFile rawWorkflowDirRef,
        final ExecutionMonitor execMon, final WorkflowSaveHelper saveHelper)
                throws IOException, CanceledExecutionException, LockFailedException {
        final String name = wm.getDirectNCParent().getCipherFileName(WorkflowPersistor.WORKFLOW_FILE);
        NodeSettings preFilledSettings = new NodeSettings(name);
        saveHeader(preFilledSettings);
        wm.getTemplateInformation().save(preFilledSettings);
        saveWizardState(wm, preFilledSettings, saveHelper);
        saveContent(wm, preFilledSettings, rawWorkflowDirRef, execMon, saveHelper);
        return name;
    }

    public static String saveAsTemplate(final WorkflowManager wm, final ReferencedFile rawWorkflowDirRef,
        final ExecutionMonitor execMon, final WorkflowSaveHelper saveHelper)
                throws IOException, CanceledExecutionException, LockFailedException {
        MetaNodeTemplateInformation tI = wm.getTemplateInformation();
        if (!Role.Template.equals(tI.getRole())) {
            throw new IllegalStateException("Cannot save workflow as template (role " + tI.getRole() + ")");
        }
        // as per 2.10 template workflows are also saved under workflow.knime (previously it was all contained in
        // template.knime). The new template.knime file is written elsewhere.
        final String name = wm.getDirectNCParent().getCipherFileName(WorkflowPersistor.WORKFLOW_FILE);
        NodeSettings preFilledSettings = new NodeSettings(name);
        saveContent(wm, preFilledSettings, rawWorkflowDirRef, execMon, saveHelper);
        return name;
    }

    /**
     * @param wm The WFM to save.
     * @param preFilledSettings The settings eventually written to workflow.knime (or workflow.knime.encrypted).
     * For workflows it contains the version number, cipher, template infos etc. The name of the setting defines the
     * output file name (so it's important!)
     * @param rawWorkflowDirRef To save to.
     * @param execMon ...
     * @param saveHelper ...
     * @throws IOException ...
     * @throws CanceledExecutionException ...
     * @throws LockFailedException ...
     */
    private static void saveContent(final WorkflowManager wm, final NodeSettings preFilledSettings,
        final ReferencedFile rawWorkflowDirRef, final ExecutionMonitor execMon, final WorkflowSaveHelper saveHelper)
                throws IOException, CanceledExecutionException, LockFailedException {
        ReferencedFile workflowDirRef = rawWorkflowDirRef;
        Role r = wm.getTemplateInformation().getRole();
        final String fName = preFilledSettings.getKey();
        if (!workflowDirRef.fileLockRootForVM()) {
            throw new LockFailedException("Can't write workflow to \"" + workflowDirRef
                + "\" because the directory can't be locked");
        }
        try {
            final ReferencedFile nodeContainerDirectory = wm.getNodeContainerDirectory();
            final ReferencedFile autoSaveDirectory = wm.getAutoSaveDirectory();
            if (!saveHelper.isAutoSave() && workflowDirRef.equals(nodeContainerDirectory)) {
                if (!nodeContainerDirectory.isDirty()) {
                    return;
                } else {
                    workflowDirRef = nodeContainerDirectory; // update variable assignment to do changes on member
                    // delete "old" node directories if not saving to the working
                    // directory -- do this before saving the nodes (dirs newly created)
                    WorkflowManager.deleteObsoleteNodeDirs(nodeContainerDirectory.getDeletedNodesFileLocations());
                }
            }
            if (saveHelper.isAutoSave() && workflowDirRef.equals(autoSaveDirectory)) {
                if (!autoSaveDirectory.isDirty()) {
                    return;
                } else {
                    workflowDirRef = autoSaveDirectory;
                    WorkflowManager.deleteObsoleteNodeDirs(autoSaveDirectory.getDeletedNodesFileLocations());
                }
            }
            File workflowDir = workflowDirRef.getFile();
            workflowDir.mkdirs();
            if (!workflowDir.isDirectory()) {
                throw new IOException("Unable to create or write directory \": " + workflowDir + "\"");
            }
            saveWorkflowName(preFilledSettings, wm.getNameField());
            saveAuthorInformation(wm.getAuthorInformation(), preFilledSettings);
            saveWorkflowCipher(preFilledSettings, wm.getWorkflowCipher());
            FileNodeContainerMetaPersistor.save(preFilledSettings, wm, workflowDirRef);
            saveWorkflowVariables(wm, preFilledSettings);
            saveCredentials(wm, preFilledSettings);
            saveTableBackend(wm, preFilledSettings);
            saveWorkflowAnnotations(wm, preFilledSettings);

            NodeSettingsWO nodesSettings = saveSettingsForNodes(preFilledSettings);
            Collection<NodeContainer> nodes = wm.getNodeContainers();
            double progRatio = 1.0 / (nodes.size() + 1);

            for (NodeContainer nextNode : nodes) {
                int id = nextNode.getID().getIndex();
                ExecutionMonitor subExec = execMon.createSubProgress(progRatio);
                execMon.setMessage(nextNode.getNameWithID());
                NodeSettingsWO sub = nodesSettings.addNodeSettings("node_" + id);
                NodeContext.pushContext(nextNode);
                try {
                    saveNodeContainer(sub, workflowDirRef, nextNode, subExec, saveHelper);
                } finally {
                    NodeContext.removeLastContext();
                }
                subExec.setProgress(1.0);
            }

            execMon.setMessage("connection information");
            NodeSettingsWO connSettings = saveSettingsForConnections(preFilledSettings);
            int connectionNumber = 0;
            for (ConnectionContainer cc : wm.getConnectionContainers()) {
                NodeSettingsWO nextConnectionConfig = connSettings.addNodeSettings("connection_" + connectionNumber);
                saveConnection(nextConnectionConfig, cc);
                connectionNumber += 1;
            }
            int inCount = wm.getNrInPorts();
            NodeSettingsWO inPortsSetts = inCount > 0 ? saveInPortsSetting(preFilledSettings) : null;
            NodeSettingsWO inPortsSettsEnum = null;
            if (inPortsSetts != null) {
                //TODO actually not neccessary to save the class name
                saveInportsBarUIInfoClassName(inPortsSetts, wm.getInPortsBarUIInfo());
                saveInportsBarUIInfoSettings(inPortsSetts, wm.getInPortsBarUIInfo());
                inPortsSettsEnum = saveInPortsEnumSetting(inPortsSetts);
            }
            for (int i = 0; i < inCount; i++) {
                NodeSettingsWO sPort = saveInPortSetting(inPortsSettsEnum, i);
                saveInPort(sPort, wm, i);
            }
            int outCount = wm.getNrOutPorts();
            NodeSettingsWO outPortsSetts = outCount > 0 ? saveOutPortsSetting(preFilledSettings) : null;
            NodeSettingsWO outPortsSettsEnum = null;
            if (outPortsSetts != null) {
                saveOutportsBarUIInfoClassName(outPortsSetts, wm.getOutPortsBarUIInfo());
                saveOutportsBarUIInfoSettings(outPortsSetts, wm.getOutPortsBarUIInfo());
                outPortsSettsEnum = saveOutPortsEnumSetting(outPortsSetts);
            }
            for (int i = 0; i < outCount; i++) {
                NodeSettingsWO singlePort = saveOutPortSetting(outPortsSettsEnum, i);
                saveOutPort(singlePort, wm, i);
            }
            saveEditorUIInformation(wm, preFilledSettings);

            File workflowFile = new File(workflowDir, fName);
            String toBeDeletedFileName = Role.Template.equals(r) ? WorkflowPersistor.TEMPLATE_FILE : WorkflowPersistor.WORKFLOW_FILE;
            new File(workflowDir, toBeDeletedFileName).delete();
            new File(workflowDir, WorkflowCipher.getCipherFileName(toBeDeletedFileName)).delete();

            OutputStream os = new FileOutputStream(workflowFile);
            os = wm.getDirectNCParent().cipherOutput(os);
            preFilledSettings.saveToXML(os);
            if (saveHelper.isSaveData()) {
                File saveWithDataFile = new File(workflowDir, WorkflowPersistor.SAVED_WITH_DATA_FILE);
                BufferedWriter o = new BufferedWriter(new FileWriter(saveWithDataFile));
                o.write("Do not delete this file!");
                o.newLine();
                o.write("This file serves to indicate that the workflow was written as part of the usual save "
                        + "routine (not exported).");
                o.newLine();
                o.newLine();
                o.write("Workflow was last saved by user ");
                o.write(System.getProperty("user.name"));
                o.write(" on " + new Date());
                o.close();
            }
            if (saveHelper.isAutoSave() && autoSaveDirectory == null) {
                wm.setAutoSaveDirectory(workflowDirRef);
            }
            if (!saveHelper.isAutoSave() && nodeContainerDirectory == null) {
                wm.setNodeContainerDirectory(workflowDirRef);
            }
            NodeContainerState wmState = wm.getNodeContainerState();
            // non remote executions
            boolean isExecutingLocally = wmState.isExecutionInProgress() && !wmState.isExecutingRemotely();
            if (workflowDirRef.equals(nodeContainerDirectory) && !isExecutingLocally) {
                wm.unsetDirty();
            }
            workflowDirRef.setDirty(isExecutingLocally);
            execMon.setProgress(1.0);
        } finally {
            workflowDirRef.fileUnlockRootForVM();
        }
    }

    /** Add version field. */
    static void saveHeader(final NodeSettings settings) {
        settings.addString(WorkflowLoadHelper.CFG_CREATED_BY, KNIMEConstants.VERSION);
        settings.addBoolean(WorkflowLoadHelper.CFG_NIGHTLY, KNIMEConstants.isNightlyBuild());
        settings.addString(WorkflowLoadHelper.CFG_VERSION, FileWorkflowPersistor.getSaveVersion().getVersionString());
    }

    /** Saves the status of the wizard if set so in the save-helper.
     * @param wm ...
     * @param preFilledSettings ...
     * @param saveHelper ...
     */
    static void saveWizardState(final WorkflowManager wm, final NodeSettings preFilledSettings,
        final WorkflowSaveHelper saveHelper) {
        //don't save the wizard state if
        //(1) simply not desired
        //(2) the workflow is or is part of a metanode
        //(3) hasn't been started in wizard execution mode (i.e. not from the web portal)
        if (!saveHelper.isSaveWizardController() || !wm.isProject() || !wm.isInWizardExecution()) {
            return;
        }
        NodeSettingsWO wizardSettings = preFilledSettings.addNodeSettings("wizard");
        final WizardExecutionController wizardController = wm.getWizardExecutionController();
        assert wizardController != null;
        wizardController.save(wizardSettings);
    }

    /**
     * @param settings
     * @since 2.6
     */
    static void saveEditorUIInformation(final WorkflowManager wfm, final NodeSettings settings) {
        EditorUIInformation editorInfo = wfm.getEditorUIInformation();
        if (editorInfo != null) {
            NodeSettingsWO editorConfig = settings.addNodeSettings(FileWorkflowLoader.CFG_EDITOR_INFO);

            editorConfig.addBoolean(FileWorkflowLoader.CFG_EDITOR_SNAP_GRID, editorInfo.getSnapToGrid());
            editorConfig.addBoolean(FileWorkflowLoader.CFG_EDITOR_SHOW_GRID, editorInfo.getShowGrid());
            editorConfig.addInt(FileWorkflowLoader.CFG_EDITOR_X_GRID, editorInfo.getGridX());
            editorConfig.addInt(FileWorkflowLoader.CFG_EDITOR_Y_GRID, editorInfo.getGridY());
            editorConfig.addDouble(FileWorkflowLoader.CFG_EDITOR_ZOOM, editorInfo.getZoomLevel());
            editorConfig.addBoolean(FileWorkflowLoader.CFG_EDITOR_CURVED_CONNECTIONS, editorInfo.getHasCurvedConnections());
            editorConfig.addInt(FileWorkflowLoader.CFG_EDITOR_CONNECTION_WIDTH, editorInfo.getConnectionLineWidth());
        }
    }

    /**
     * Helper to save a {@link NodeUIInformation} object.
     */
    static void saveNodeUIInformation(final NodeSettingsWO settings, final NodeUIInformation nodeUIInfo) {
        //save UI info class name (TODO: for historical reasons, probably not needed anymore)
        settings.addString(FileWorkflowPersistor.CFG_UIINFO_CLASS, nodeUIInfo != null ? nodeUIInfo.getClass().getName() : null);
        //save UI info settings
        //nest into separate sub config
        if (nodeUIInfo != null) {
            NodeSettingsWO subConfig = settings.addNodeSettings(FileWorkflowPersistor.CFG_UIINFO_SUB_CONFIG);
            subConfig.addIntArray(FileWorkflowPersistor.KEY_BOUNDS, nodeUIInfo.getBounds());
        }
    }


}
