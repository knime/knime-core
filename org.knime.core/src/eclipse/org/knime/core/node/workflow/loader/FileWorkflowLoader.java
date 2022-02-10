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
package org.knime.core.node.workflow.loader;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.ConfigBase;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.EditorUIInformation;
import org.knime.core.node.workflow.NodeContainerPersistor;
import org.knime.core.node.workflow.ObsoleteMetaNodeFileWorkflowPersistor;
import org.knime.core.node.workflow.TemplateNodeContainerPersistor;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.NodeFactoryUnknownException;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer.AuthorInformation;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.FlowVariableDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.StyleRangeDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.core.workflow.def.impl.AuthorInformationDefBuilder;
import org.knime.core.workflow.def.impl.ConnectionDefBuilder;
import org.knime.core.workflow.def.impl.FlowVariableDefBuilder;
import org.knime.core.workflow.def.impl.StyleRangeDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowMetadataDefBuilder;


/**
 * Recursively walks through the legacy directory structure, generating the workflow defs.
 *
 * TODO remove this comment: Used to implement {@link WorkflowPersistor} and {@link TemplateNodeContainerPersistor}
 *
 * TODO remove suppresswarnings('javadoc')
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
public class FileWorkflowLoader  {

    // COPIED FROM WorkflowPersistor > > > >

    /** KNIME Node type: native, meta or sub node. */
    private enum NodeType {
            MetaNode("metanode"), NativeNode("node"), SubNode("wrapped node");

        private final String m_shortName;

        /** @param shortName -- toString result */
        private NodeType(final String shortName) {
            m_shortName = shortName;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_shortName;
        }
    }

    /**
     * Nodes that miss an ID attribute are assigned successive negative IDs -1, -2, -3...
     *
     * @see {@link #getIdForMissingNode()}
     */
    private int m_nextIdForMissingNode = -1;

    /**
     * The key under which the bounds to store the {@link ConnectionUIInformation} are registered. *
     *
     * @since 3.5
     */
    public static final String KEY_BENDPOINTS = "extrainfo.conn.bendpoints";

    /** Key for connections. */
    public static final String KEY_CONNECTIONS = "connections";

    /** Key for nodes. */
    public static final String KEY_NODES = "nodes";

    public static final String KEY_UI_INFORMATION = "extraInfoClassName";

    /** Constant for the meta info file name. */
    public static final String METAINFO_FILE = "workflowset.meta";

    /**
     * File used to signal that workflow was saved in usual manner. It will always be present in the workflow directory
     * unless the workflow is exported with the "exclude data" flag being set.
     */
    public static final String SAVED_WITH_DATA_FILE = ".savedWithData";

    /**
     * Identifier for KNIME templates SVG export when saved to disc.
     *
     * @since 2.8
     */
    public static final String SVG_TEMPLATE_FILE = "template.svg";

    /**
     * Identifier for KNIME workflows SVG export when saved to disc.
     *
     * @since 2.8
     */
    public static final String SVG_WORKFLOW_FILE = "workflow.svg";

    /** Identifier for KNIME meta mode templates when saved to disc. */
    public static final String TEMPLATE_FILE = "template.knime";

    // < < < <  COPIED FROM WorkflowPersistor

    /** Identifier for KNIME workflows when saved to disc. */
    public static final String WORKFLOW_FILE = "workflow.knime";

    private static final ConfigBaseRO EMPTY_SETTINGS = new NodeSettings("<<empty>>");

    static final String CFG_AUTHOR_INFORMATION = "authorInformation";

    static final String CFG_EDITOR_CONNECTION_WIDTH = "workflow.editor.connectionWidth";

    static final String CFG_EDITOR_CURVED_CONNECTIONS = "workflow.editor.curvedConnections";


    /** key used to store the editor specific settings (since 2.6). */
    static final String CFG_EDITOR_INFO = "workflow_editor_settings";

    static final String CFG_EDITOR_SHOW_GRID = "workflow.editor.ShowGrid";

    static final String CFG_EDITOR_SNAP_GRID = "workflow.editor.snapToGrid";

    static final String CFG_EDITOR_X_GRID = "workflow.editor.gridX";

    static final String CFG_EDITOR_Y_GRID = "workflow.editor.gridY";

    static final String CFG_EDITOR_ZOOM = "workflow.editor.zoomLevel";

    /** Key for UI info's class name. */
    static final String CFG_UIINFO_CLASS = "ui_classname";

    static final String CFG_UIINFO_SUB_CONFIG = "ui_settings";

    /** Key for workflow variables. */
    static final String CFG_WKF_VARIABLES = "workflow_variables";

    /** Format used to save author/edit infos. */
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    static final LoadVersion VERSION_LATEST = LoadVersion.V4010;

//    private final FileNodeContainerMetaPersistor m_metaPersistor;

    /**
     * Synchronized call to DATE_FORMAT.parse(String).
     *
     * @param s To parse, not null.
     * @return The date.
     * @throws DateTimeParseException if the text cannot be parsed
     */
    private static OffsetDateTime parseDate(final String s) {
        synchronized (DATE_FORMAT) {
            return OffsetDateTime.parse(s, DATE_FORMAT);
        }
    }

    // TODO duplicated code
    /** Parse the version string, return {@link LoadVersion#FUTURE} if it can't be parsed. */
    static LoadVersion parseVersion(final String versionString) {
        var isBeforeV2 = versionString.equals("0.9.0");
        isBeforeV2 |= versionString.equals("1.0");
        isBeforeV2 |= versionString.matches("1\\.[01234]\\.[0-9].*");
        if (isBeforeV2) {
            return LoadVersion.UNKNOWN;
        }
        return LoadVersion.get(versionString).orElse(LoadVersion.FUTURE);
    }

    /**
     * used to check for duplicate connection information in
     * {@link #loadNodeContainer(Map, ExecutionMonitor, org.knime.core.node.workflow.WorkflowPersistor.LoadResult)}
     * {@link #getConnectionSet()}
     * TODO move to builder.addConnection
     */
    private final HashSet<ConnectionDef> m_connectionSet= new HashSet<>();

    private WorkflowLoadHelper m_loadHelper;

    /** The node logger for this class. */
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private boolean m_mustWarnOnDataLoadError;

    private final List<ReferencedFile> m_obsoleteNodeDirectories = new ArrayList<>();

    private final LoadVersion m_versionString;

    private ConfigBaseRO m_workflowSett;

    /**
     * Holds the nodes (TODO move to builder) until build is called. Key is the node ID (suffix?)
     */
    Map<Integer, NodeDef> m_nodesToAdd = new HashMap<>();

    final WorkflowDefBuilder m_workflowDefBuilder = new WorkflowDefBuilder();


    final WorkflowMetadataDefBuilder m_workflowMetadataDefBuilder = new WorkflowMetadataDefBuilder();

    final WorkflowDefBuilder m_workflowProjectDefBuilder = new WorkflowDefBuilder();

//    /**
//     * @return the workflowDir
//     */
//    ReferencedFile getWorkflowKNIMEFile() {
//        // TODO
//        var meta = getMetaPersistor();
//        if (meta == null) {
//            throw new RuntimeException("Persistor not created for loading workflow, meta persistor is null");
//        }
//        return meta.getConfigBaseFile();
//    }

    /**
     * @param workflowFormatVersion
     */
    public FileWorkflowLoader(final LoadVersion workflowFormatVersion) {
        m_versionString = workflowFormatVersion;
    }

    /**
     * Set only once in constructor. Originally from both {@link WorkflowPersistor} and
     * {@link TemplateNodeContainerPersistor}
     */
    public final LoadVersion getLoadVersion() {
        return m_versionString;
    }

    /**
     * Validates the directory argument.
     *
     * @param directory from which to load the workflow.
     * @return the workflow.knime file in the given directory.
     * @throws IOException
     */
    static File getKnimeFile(final File directory) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("Directory must not be null.");
        }
        if (!directory.isDirectory()) {
            throw new IOException("Not a directory: " + directory);
        }
        if (!directory.canRead()) {
            throw new IOException("Cannot read from directory: " + directory);
        }

        var fileName = "workflow.knime"; // == WorkflowPersistor.WORKFLOW_FILE

        // template.knime or workflow.knime
        var dotKNIMERef = new ReferencedFile(new ReferencedFile(directory), fileName);
        var dotKNIME = dotKNIMERef.getFile();

        if (!dotKNIME.isFile()) {
            throw new IOException(String.format("No %s file in directory %s", fileName, directory.getAbsolutePath()));
        }
        return dotKNIME;
    }

    /**
     * @param directory
     * @param workflowDescription
     * @return
     */
    public WorkflowDef load(final File directory, final ConfigBase workflowDescription) {



        var knimeFile = FileWorkflowLoader.getKnimeFile(directory);
//      if (workflowKNIMEFile == null || m_workflowSett == null) {
//          loadResult.setDirtyAfterLoad();
//          throw new IllegalStateException("The method preLoadNodeContainer has either not been called or failed");
//      }


      /* read connections */
      loadConnections(failingNodeIDSet);


      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

      m_workflowDefBuilder//
          .setName(loadWorkflowName(m_workflowSett))//
          .setAuthorInformation(loadAuthorInformation(m_workflowSett, getLoadVersion()))
          .setNodes(loadNodes())//
          .setConnections(loadConnections())//
          .setAnnotations(loadAnnotations(m_workflowSett))//
          // TODO cipher
//          .setCipher(loadCipher())//
          .setWorkflowEditorSettings(loadEditorUIInformation(m_workflowSett));

        // workflow.knime (or template.knime)
      // TODO used to be a ReferencedFile
        var nodeFile = knimeFile.getFile();
        var parentRef = knimeFile.getParent();

        if (parentRef == null) {
            loadResult.setDirtyAfterLoad();
            throw new IOException("Parent directory of file \"" + knimeFile + "\" is not represented by "
                + ReferencedFile.class.getSimpleName() + " object");
        }

        m_mustWarnOnDataLoadError = loadIfMustWarnOnDataLoadError(parentRef.getFile());

        // TODO deciphering
//        ConfigBaseRO subWFSettings;
//        try {
//            InputStream in = new FileInputStream(nodeFile);
//            /**
//             * Security TODO should be different persistors for component, metanode, workflow
//             */
//            if (m_parentPersistor != null) { // real metanode, not a project
//                // the workflow.knime (or template.knime) file is not encrypted
//                // with this metanode's cipher but possibly with a parent
//                // cipher
//                in = m_parentPersistor.decipherInput(in);
//            }
//            in = new BufferedInputStream(in);
//            subWFSettings = NodeSettings.loadFromXML(in);
//        } catch (IOException ioe) {
//            loadResult.setDirtyAfterLoad();
//            throw ioe;
//        }
//        m_workflowSett = subWFSettings;

        // TODO
//        setWorkflowCipher(tryLoadDebug("workflow cipher", WorkflowCipher.NULL_CIPHER,
//            () -> loadWorkflowCipher(getLoadVersion(), m_workflowSett), loadResult));

        setAuthorInformation(tryLoadDebug("workflow author information", AuthorInformation.UNKNOWN,
            () -> loadAuthorInformation(m_workflowSett, loadResult), loadResult));

        setWorkflowVariables(tryLoadDebug("workflow variables", Collections.emptyList(),
            () -> loadWorkflowVariables(m_workflowSett), loadResult));

    }

    /**
     * TODO unify getLoadVersion, static methods, or access via member
     * @return non-null list.
     */
    private List<AnnotationDataDef> loadAnnotations(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V230)) {
            return List.of();
        } else {
            if (!settings.containsKey("annotations")) {
                return Collections.emptyList();
            }
            var annoSettings = settings.getConfigBase("annotations");
            List<AnnotationDataDef> result = new ArrayList<>();
            for (String key : annoSettings.keySet()) {
                var child = annoSettings.getConfigBase(key);
                result.add(loadAnnotationData(child, getLoadVersion()));
            }
            return result;
        }
    }

    /**
     * TODO copied from AnnotationData
     */
    private static AnnotationDataDef loadAnnotationData(final ConfigBaseRO config, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        var builder = new AnnotationDataDefBuilder()//
            .setText(config.getString("text"))//
            .setBgcolor(config.getInt("bgcolor"))//
            .setLocation(CoreToDefUtil.createCoordinate(config.getInt("x-coordinate"), config.getInt("y-coordinate")))//
            .setWidth(config.getInt("width"))//
            .setHeight(config.getInt("height"))//
            .setBorderSize(config.getInt("borderSize", 0)) // default to 0 for backward compatibility
            .setBorderColor(config.getInt("borderColor", 0)) // default for backward compatibility
            .setDefaultFontSize(config.getInt("defFontSize", -1)) // default for backward compatibility
            .setAnnotationVersion(config.getInt("annotation-version", -1)) // default to VERSION_OLD
            .setTextAlignment(
                loadVersion.ordinal() >= LoadVersion.V250.ordinal() ? config.getString("alignment") : "LEFT");

        ConfigBaseRO styleConfigs = config.getConfigBase("styles");
        for (String key : styleConfigs.keySet()) {
            try {
                var def = loadStyleRange(styleConfigs.getConfigBase(key));
                builder.addStyles(def);
            } catch (InvalidSettingsException ex) {
                // TODO
            }
        }
        return builder.build();
    }

    private static StyleRangeDef loadStyleRange(final ConfigBaseRO settings) throws InvalidSettingsException {
        return new StyleRangeDefBuilder()//
            .setStart(settings.getInt("start"))//
            .setLength(settings.getInt("length"))//
            .setFontName(settings.getString("fontname"))//
            .setFontStyle(settings.getInt("fontstyle"))//
            .setFontSize(settings.getInt("fontsize"))//
            .setColor(settings.getInt("fgcolor"))//
            .build();
    }

    /** Originally from {@link TemplateNodeContainerPersistor} */
    public boolean mustComplainIfStateDoesNotMatch() {
        return !getLoadVersion().isOlderThan(LoadVersion.V200);
    }

    /** Originally from {@link TemplateNodeContainerPersistor} */
    public boolean mustWarnOnDataLoadError() {
        return m_mustWarnOnDataLoadError;
    }

    /**
     * @param exec
     * @param loadResult
     * @param failingNodeIDSet
     * @throws CanceledExecutionException
     */
    private void loadConnections(final ExecutionMonitor exec, final LoadResult loadResult,
        final Set<Integer> failingNodeIDSet) throws CanceledExecutionException {
        ConfigBaseRO connections;
        try {
            final ConfigBaseRO set = m_workflowSett;
            connections = set.getConfigBase(KEY_CONNECTIONS);
            if (connections == null) {
                connections = EMPTY_SETTINGS;
            }
        } catch (InvalidSettingsException e) {
            var error = "Can't load workflow connections, config not found: " + e.getMessage();
            getLogger().debug(error, e);
            loadResult.setDirtyAfterLoad();
            loadResult.addError(error);
            connections = EMPTY_SETTINGS;
        }
        for (String connectionKey : connections.keySet()) {
            exec.checkCanceled();
            ConnectionDef c;
            try {
                c = loadConnection(connections.getConfigBase(connectionKey));
            } catch (InvalidSettingsException e) {
                var error = "Can't load connection with internal ID \"" + connectionKey + "\": " + e.getMessage();
                getLogger().debug(error, e);
                loadResult.setDirtyAfterLoad();
                loadResult.addError(error);
                continue;
            }
            int sourceIDSuffix = c.getSourceID();
            NodeContainerPersistor sourceNodePersistor = m_nodeContainerLoaderMap.get(sourceIDSuffix);
            if (sourceNodePersistor == null && sourceIDSuffix != -1) {
                loadResult.setDirtyAfterLoad();
                if (!failingNodeIDSet.contains(sourceIDSuffix)) {
                    loadResult.addError("Unable to load node connection " + c + ", source node does not exist");
                }
                continue;
            }
            // TODO rewrite to accept def
            //            FileWorkflowPersistor.fixSourcePortIfNecessary(sourceNodePersistor, c, getLoadVersion());

            int destIDSuffix = c.getDestID();
            NodeContainerPersistor destNodePersistor = m_nodeContainerLoaderMap.get(destIDSuffix);
            if (destNodePersistor == null && destIDSuffix != -1) {
                loadResult.setDirtyAfterLoad();
                if (!failingNodeIDSet.contains(destIDSuffix)) {
                    loadResult.addError("Unable to load node connection " + c + ", destination node does not exist");
                }
                continue;
            }
            // TODO rewrite to accept def
            //            FileWorkflowPersistor.fixDestPortIfNecessary(destNodePersistor, c, getLoadVersion());

            if (!m_connectionSet.add(c)) {
                loadResult.setDirtyAfterLoad();
                loadResult.addError("Duplicate connection information: " + c);
            }
        }
    }

    private static Map<String, NodeDef> loadNodes(final ConfigBaseRO set) {
        /* read nodes */
        ConfigBaseRO nodes;
        try {
            nodes = set.getConfigBase(KEY_NODES);
        } catch (InvalidSettingsException e) {
//            var error = "Can't load nodes in workflow, config not found: " + e.getMessage();
//            getLogger().debug(error, e);
//            loadResult.addError(error);
//            loadResult.setDirtyAfterLoad();
//            loadResult.setResetRequiredAfterLoad();
            // TODO error handling special
            // stop loading here
//            return;
        }
        // ids of nodes that failed to load. Used to suppress superfluous errors when reading the connections
        Set<Integer> failingNodeIDSet = new HashSet<>();

        final var workflowDirRef = knimeFile.getParent();

        for (String nodeKey : nodes.keySet()) {
            loadNode(workflowDirRef, nodeKey, exec, loadResult, nodes, failingNodeIDSet, missingNodeIDMap, knimeFile);

        }

        //TODO
        return null;
    }

    /**
     * @param workflowDirRef
     * @param nodeKey
     * @param exec
     * @param loadResult
     * @param nodes
     * @param failingNodeIDSet
     * @param missingNodeIDMap
     * @param workflowKNIMEFile
     *
     * @return a node describing what's behind the given node id (might be a metanode, component, or native node).
     *
     * @throws CanceledExecutionException
     */
    private Optional<NodeDef> loadNode(final ReferencedFile workflowDirRef, final String nodeKey,
        final ConfigBaseRO nodes, final Set<Integer> failingNodeIDSet,
        final Map<Integer, NodeFactoryUnknownException> missingNodeIDMap, final ReferencedFile workflowKNIMEFile)
        throws CanceledExecutionException {

        if(!nodes.containsKey(nodeKey)) {
//            var error = "Unable to load settings for node with internal " + "id \"" + nodeKey + "\": " + e.getMessage();
//            getLogger().debug(error, e);
//            loadResult.setDirtyAfterLoad();
//            loadResult.addError(error);
            // maybe this should throw an exception instead of skipping the node
            return Optional.empty();
        }

        ConfigBaseRO nodeSetting = nodes.getConfigBase(nodeKey);

        int nodeIDSuffix = loadNodeId(nodeSetting);

        var nodeType = loadNodeType(nodeSetting);

        ReferencedFile nodeFile;
        try {
            nodeFile = loadNodeFile(nodeSetting, workflowDirRef);
        } catch (InvalidSettingsException e) {
         // TODO
//            var error = "Unable to load settings for node " + "with ID suffix " + nodeIDSuffix + ": " + e.getMessage();
//            getLogger().debug(error, e);
//            loadResult.setDirtyAfterLoad();
//            loadResult.addError(error);
            failingNodeIDSet.add(nodeIDSuffix);
            return Optional.empty();
        }

        switch (nodeType) {
            case MetaNode:
                persistor = createWorkflowLoader(nodeFile);
                break;
            case NativeNode:
                persistor = createNativeNodeContainerPersistorLoad(nodeFile);
                break;
            case SubNode:
                persistor = createSubNodeContainerPersistorLoad(nodeFile);
                break;
            default:
                throw new IllegalStateException("Unknown node type: " + nodeType);
        }

        try {
            var childResult = new LoadResult(nodeType.toString() + " with ID suffix " + nodeIDSuffix);

            /**
             * Recurse
             */
            var childDef = persistor.getLoadResult(this, nodeSetting, childResult);
            m_nodesToAdd.put(nodeIDSuffix, childDef);

            loadResult.addChildError(childResult);
        } catch (Throwable e) {
            var error =
                "Unable to load node with ID suffix " + nodeIDSuffix + " into workflow, skipping it: " + e.getMessage();
            String loadErrorString;
            if (e instanceof NodeFactoryUnknownException) {
                loadErrorString = e.getMessage();
            } else {
                loadErrorString = error;
            }
            if (e instanceof InvalidSettingsException || e instanceof IOException
                || e instanceof NodeFactoryUnknownException) {
                getLogger().debug(error, e);
            } else {
                getLogger().error(error, e);
            }
            loadResult.addError(loadErrorString);
            if (e instanceof NodeFactoryUnknownException) {
                missingNodeIDMap.put(nodeIDSuffix, (NodeFactoryUnknownException)e);
                // don't set dirty
            } else {
                loadResult.setDirtyAfterLoad();
                failingNodeIDSet.add(nodeIDSuffix);
                // node directory is the parent of the settings.xml
                m_obsoleteNodeDirectories.add(nodeFile.getParent());
                return Optional.empty();
            }
        }
        // NodeContainerMetaPersistor

//        var meta = persistor.getMetaPersistor();
//        if (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
//            var randomID = getRandomNodeID();
//            loadResult.setDirtyAfterLoad();
//            loadResult.addError("Duplicate id encountered in workflow: " + nodeIDSuffix + ", uniquifying to random id "
//                + randomID + ", this possibly screws the connections");
//            nodeIDSuffix = randomID;
//        }
//        meta.setNodeIDSuffix(nodeIDSuffix);
//        meta.setUIInfo(nodeUIInfo);
//        if (persistor.isDirtyAfterLoad()) {
//            loadResult.setDirtyAfterLoad();
//        }
//        m_nodeContainerLoaderMap.put(nodeIDSuffix, persistor);
        // TODO
        return null;
    }

    /**
     * @param nodeSetting
     * @return
     */
    private int loadNodeId(final ConfigBaseRO nodeSetting) {
        int nodeIDSuffix;
        try {
            nodeIDSuffix = nodeSetting.getInt("id");
        } catch (InvalidSettingsException e) {
            nodeIDSuffix = getIdForMissingNode();
// TODO
//            var error = "Unable to load node ID (internal id \"" + nodeKey + "\"), trying random number " + nodeIDSuffix
//                + "instead: " + e.getMessage();
//            getLogger().debug(error, e);
//            loadResult.setDirtyAfterLoad();
//            loadResult.addError(error);
        }
        return nodeIDSuffix;
    }

    /**
     * @return
     */
    private int getIdForMissingNode() {
        return m_nextIdForMissingNode--;
    }

    /**
     * @return The load helper as set on the meta persistor. Will be passed on to loaders of contained nodes.
     */
    WorkflowLoadHelper getLoadHelper() {
        return m_loadHelper;
    }

    ConnectionDef loadConnection(final ConfigBaseRO settings) throws InvalidSettingsException {
        var sourceID = settings.getInt("sourceID");
        var destID = loadConnectionDestID(settings);
        var sourcePort = settings.getInt("sourcePort");
        var destPort = loadConnectionDestPort(settings);
        // this attribute is in most cases not present (not saved)
        var isDeletable = settings.getBoolean("isDeletable", true);
        if (sourceID != -1 && sourceID == destID) {
            throw new InvalidSettingsException("Source and Destination must " + "not be equal, id is " + sourceID);
        }

        // TODO load bendpoints - maybe add a builder pattern that builds the list internally instead of having to pass a list all the time
        // e.g., builder.setBendPoints(List<Point> points) -> builder.addBendPoint(Point point)
//        var builder = new ConnectionUISettingsDefBuilder()();
//        try {
//            var uiInfoClass = loadUIInfoClassName(settings);
//            if (uiInfoClass != null) {
//                if (!uiInfoClass.equals(ConnectionUIInformation.class.getName())) {
//                    // TODO
////                    getLogger().debug("Could not load UI information for " + "connection between nodes " + sourceID
////                        + " and " + destID + ": expected " + ConnectionUIInformation.class.getName() + " but got "
////                        + uiInfoClass.getClass().getName());
//                } else {
//                    var builder = ConnectionUIInformation.builder();
//                    // in previous releases, the settings were directly written to the
//                    // top-most node settings object; since 2.0 they are put into a
//                    // separate sub-settings object
//                    var subSettings = getLoadVersion().isOlderThan(LoadVersion.V200) ? settings
//                        : settings.getConfigBase(CFG_UIINFO_SUB_CONFIG);
//                    var size = subSettings.getInt(KEY_BENDPOINTS + "_size");
//                    for (var i = 0; i < size; i++) {
//                        var tmp = subSettings.getIntArray(KEY_BENDPOINTS + "_" + i);
//                        //TODO add bendpoint directly as int array
//                        builder.addBendpoint(tmp[0], tmp[1], i);
//                    }
//                    connUIInfo = builder.build();
//                }
//            }
//        } catch (InvalidSettingsException ise) {
//            // TODO
////            getLogger().debug(
////                "Could not load UI information for connection " + "between nodes " + sourceID + " and " + destID);
//        } catch (Throwable t) {
//         // TODO
////            getLogger().warn(
////                "Exception while loading connection UI " + "information between nodes " + sourceID + " and " + destID,
////                t);
//        }
        return new ConnectionDefBuilder()//
            .setSourceID(sourceID)//
            .setSourcePort(sourcePort)//
            .setDestID(destID)//
            .setDestPort(destPort)//
            .setDeletable(isDeletable)//
            // TODO
//            .setUiSettings(uiSettings)
            .build();
    }

    int loadConnectionDestID(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return settings.getInt("targetID");
        } else {
            return settings.getInt("destID");
        }
    }

    int loadConnectionDestPort(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return settings.getInt("targetPort");
        } else {
            // possibly port index correction in fixDestPort method
            return settings.getInt("destPort");
        }
    }

    /**
     * Load editor information (grid settings, zoom level, et cetera).
     */
    EditorUIInformation loadEditorUIInformation(final ConfigBaseRO settings) throws InvalidSettingsException {
        final var loadVersion = getLoadVersion();
        if (loadVersion.isOlderThan(LoadVersion.V260) || !settings.containsKey(CFG_EDITOR_INFO)) {
            return EditorUIInformation.builder().build();
        }
        var editorCfg = settings.getConfigBase(CFG_EDITOR_INFO);
        var builder = EditorUIInformation.builder()//
            .setSnapToGrid(editorCfg.getBoolean(CFG_EDITOR_SNAP_GRID))//
            .setShowGrid(editorCfg.getBoolean(CFG_EDITOR_SHOW_GRID))//
            .setGridX(editorCfg.getInt(CFG_EDITOR_X_GRID))//
            .setGridY(editorCfg.getInt(CFG_EDITOR_Y_GRID))//
            .setZoomLevel(editorCfg.getDouble(CFG_EDITOR_ZOOM));
        if (editorCfg.containsKey(CFG_EDITOR_CURVED_CONNECTIONS)) {
            builder.setHasCurvedConnections(editorCfg.getBoolean(CFG_EDITOR_CURVED_CONNECTIONS));
        }
        if (editorCfg.containsKey(CFG_EDITOR_CONNECTION_WIDTH)) {
            builder.setConnectionLineWidth(editorCfg.getInt(CFG_EDITOR_CONNECTION_WIDTH));
        }
        return builder.build();
    }

    /**
     * check whether there is a "loaded with no data" file.
     *
     * @param workflowDir ...
     * @return true for old workflows (&lt;2.0) or if there is a {@value WorkflowPersistor#SAVED_WITH_DATA_FILE} file.
     */
    boolean loadIfMustWarnOnDataLoadError(final File workflowDir) {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return true;
        }
        return new File(workflowDir, SAVED_WITH_DATA_FILE).isFile();
    }

    /**
     * The node settings file is typically names settings.xml (for native nodes and components) and workflow.knime for
     * metanodes.
     * @param settings
     * @param workflowDirRef
     * @return
     * @throws InvalidSettingsException
     */
    ReferencedFile loadNodeFile(final ConfigBaseRO settings, final ReferencedFile workflowDirRef)
        throws InvalidSettingsException {
        var fileString = settings.getString("node_settings_file");
        if (fileString == null) {
            throw new InvalidSettingsException("Unable to read settings " + "file for node " + settings.getKey());
        }
        var workflowDir = workflowDirRef.getFile();
        // fileString is something like "File Reader(#1)/settings.xml", thus
        // it contains two levels of the hierarchy. We leave it here to the
        // java.io.File implementation to resolve these levels
        var fullFile = new File(workflowDir, fileString);
        if (!fullFile.isFile() || !fullFile.canRead()) {
            throw new InvalidSettingsException("Unable to read settings " + "file " + fullFile.getAbsolutePath());
        }
        var children = new Stack<String>();
        var workflowDirAbsolute = workflowDir.getAbsoluteFile();
        while (!fullFile.getAbsoluteFile().equals(workflowDirAbsolute)) {
            children.push(fullFile.getName());
            fullFile = fullFile.getParentFile();
        }
        // create a ReferencedFile hierarchy for the settings file
        var result = workflowDirRef;
        while (!children.empty()) {
            result = new ReferencedFile(result, children.pop());
        }
        return result;
    }

    private NodeType loadNodeType(final ConfigBaseRO settings) throws InvalidSettingsException {
        NodeType result;
        try {
            if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
                var factory = settings.getString("factory");
                if (ObsoleteMetaNodeFileWorkflowPersistor.OLD_META_NODES.contains(factory)) {
                    result = NodeType.MetaNode;
                } else {
                    result = NodeType.NativeNode;
                }
            } else if (getLoadVersion().isOlderThan(LoadVersion.V2100Pre)) {
                result = settings.getBoolean("node_is_meta") ? NodeType.MetaNode : NodeType.NativeNode;
            } else {
                final var nodeTypeString = settings.getString("node_type");
                CheckUtils.checkSettingNotNull(nodeTypeString, "node type must not be null");
                try {
                    result = NodeType.valueOf(nodeTypeString);
                } catch (IllegalArgumentException iae) {
                    throw new InvalidSettingsException("Can't parse node type: " + nodeTypeString);
                }
            }
        } catch (InvalidSettingsException e) {
            // TODO
            //            String error =
            //                "Can't retrieve node type for contained node with id suffix " + nodeIDSuffix
            //                    + ", attempting to read ordinary (native) node: " + e.getMessage();
            //            getLogger().debug(error, e);
            //            loadResult.setDirtyAfterLoad();
            //            loadResult.addError(error);
            result = NodeType.NativeNode;
        }
        return result;
    }

    String loadUIInfoClassName(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            if (settings.containsKey(KEY_UI_INFORMATION)) {
                return settings.getString(KEY_UI_INFORMATION);
            }
        } else {
            if (settings.containsKey(CFG_UIINFO_CLASS)) {
                return settings.getString(CFG_UIINFO_CLASS);
            }
        }
        return null;
    }

    // TODO wizard state?
//    /**
//     * @return the wizard state saved in the file or null (often null).
//     * @param settings ...
//     * @throws InvalidSettingsException ...
//     */
//    ConfigBaseRO loadWizardState(final ConfigBaseRO settings) throws InvalidSettingsException {
//        return settings.containsKey("wizard") ? settings.getConfigBase("wizard") : null;
//    }

    // TODO workflow cipher
//    WorkflowCipher loadWorkflowCipher(final LoadVersion loadVersion, final ConfigBaseRO settings)
//        throws InvalidSettingsException {
//        // added in v2.5 - no check necessary
//        if ((getLoadVersion().ordinal() < LoadVersion.V250.ordinal()) || !settings.containsKey("cipher")) {
//            return WorkflowCipher.NULL_CIPHER;
//        }
//        var cipherSettings = settings.getConfigBase("cipher");
//        return WorkflowCipher.load(loadVersion, cipherSettings);
//    }


    String loadWorkflowName(final ConfigBaseRO set) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return "Workflow Manager";
        } else {
            return set.getString("name");
        }
    }

    /**
     * Load workflow variables (not available in 1.3.x flows).
     *
     * @param settings To load from.
     * @return The variables in a list.
     * @throws InvalidSettingsException If any settings-related error occurs.
     */
    List<FlowVariableDef> loadWorkflowVariables(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (getLoadVersion().isOlderThan(LoadVersion.V200)) {
            return List.of();
        }
        if (!settings.containsKey(CFG_WKF_VARIABLES)) {
            return List.of();
        }

        var wfmVarSub = settings.getConfigBase(CFG_WKF_VARIABLES);
        List<FlowVariableDef> result = new ArrayList<>();
        for (String key : wfmVarSub.keySet()) {
            ConfigBaseRO sub = wfmVarSub.getConfigBase(key);
            var def = new FlowVariableDefBuilder()//
            // TODO
//            final String name = CheckUtils.checkSettingNotNull(sub.getString("name"), "name must not be null");
            .setName(sub.getString("name"))//
            // TODO
//            final VariableValue<?> value = VariableType.load(sub);
            .setValue(null)
            // TODO
            .setPropertyClass(null)//
            .build();
            result.add(def);
        }
        return result;
    }

    private static AuthorInformationDef loadAuthorInformation(final ConfigBaseRO settings,
        final LoadVersion loadVersion) throws InvalidSettingsException {

        if (loadVersion.ordinal() >= LoadVersion.V280.ordinal() && settings.containsKey(CFG_AUTHOR_INFORMATION)) {
            final var sub = settings.getConfigBase(CFG_AUTHOR_INFORMATION);
            final var author = sub.getString("authored-by");
            final var authorDateS = sub.getString("authored-when");
            OffsetDateTime authorDate;
            if (authorDateS == null) {
                authorDate = null;
            } else {
                try {
                    authorDate = parseDate(authorDateS);
                } catch (DateTimeParseException e) {
                    authorDate = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
                    // TODO
                    //                    loadResult.addWarning(String.format("Can't parse authored-when-date \"%s\". Replaced with \"%s\".",
                    //                        authorDateS, authorDate.toString()));
                }
            }
            final var editor = sub.getString("lastEdited-by");
            final var editDateS = sub.getString("lastEdited-when");
            OffsetDateTime editDate;
            if (editDateS == null) {
                editDate = null;
            } else {
                try {
                    editDate = parseDate(editDateS);
                } catch (DateTimeParseException e) {
                    editDate = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
                    // TODO
                    //                    loadResult.addWarning(String.format("Can't parse lastEdit-when-date \"%s\". Replaced with \"%s\".",
                    //                        editDateS, editDate.toString()));
                }
            }
            return new AuthorInformationDefBuilder()//
                .setAuthoredBy(author)//
                .setAuthoredWhen(authorDate)//
                .setLastEditedBy(editor)//
                .setLastEditedWhen(editDate)//
                .build();
        } else {
            return new AuthorInformationDefBuilder()//
                .setAuthoredBy("<unknown>")//
                .setAuthoredWhen(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))//
                .setLastEditedBy(null)//
                .setLastEditedWhen(null)//
                .build();
        }
    }

}
