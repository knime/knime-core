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
import java.util.List;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ObsoleteMetaNodeFileWorkflowPersistor;
import org.knime.core.node.workflow.TemplateNodeContainerPersistor;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.ConnectionUISettingsDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.StyleRangeDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowUISettingsDef;
import org.knime.core.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.core.workflow.def.impl.AuthorInformationDefBuilder;
import org.knime.core.workflow.def.impl.ConnectionDefBuilder;
import org.knime.core.workflow.def.impl.ConnectionUISettingsDefBuilder;
import org.knime.core.workflow.def.impl.StyleRangeDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowUISettingsDefBuilder;

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
public class WorkflowLoader {

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

    public enum Const {
            /** @see ConnectionUISettingsDef#getBendPoints() */
            KEY_BENDPOINTS("extrainfo.conn.bendpoints"),
            /** @see WorkflowDef#getConnections() */
            KEY_CONNECTIONS("connections"),
            /** @see WorkflowDef#getNodes() */
            KEY_NODES("nodes"),
            /** */
            KEY_UI_INFORMATION("extraInfoClassName"),
            /**
             * The relative path of the file that contains the node configuration. For example
             * {@code 2_1 Building (#1444)/settings.xml}
             */
            KEY_NODE_SETTINGS_FILE("node_settings_file"),
            /** Constant for the meta info file name. */
            METAINFO_FILE_NAME("workflowset.meta"),
            /** Identifier for KNIME templates SVG export when saved to disk. */
            SVG_TEMPLATE_FILE("template.svg"),
            /** Identifier for KNIME workflows SVG export when saved to disk. */
            SVG_WORKFLOW_FILE_NAME("workflow.svg"),
            /** Identifier for KNIME meta mode templates when saved to disk. */
            TEMPLATE_FILE_NAME("template.knime"),
            /** Identifier for KNIME workflows when saved to disk. */
            WORKFLOW_FILE_NAME("workflow.knime"),
            /** @see WorkflowDef#getAuthorInformation() */
            CFG_AUTHOR_INFORMATION("authorInformation"),
            /** */
            CFG_EDITOR_CONNECTION_WIDTH("workflow.editor.connectionWidth"),
            /** */
            CFG_EDITOR_CURVED_CONNECTIONS("workflow.editor.curvedConnections"),
            /** key used to store the editor specific settings (since 2.6). */
            CFG_EDITOR_INFO("workflow_editor_settings"),
            /** */
            CFG_EDITOR_SHOW_GRID("workflow.editor.ShowGrid"),
            /** */
            CFG_EDITOR_SNAP_GRID("workflow.editor.snapToGrid"),
            /** */
            CFG_EDITOR_X_GRID("workflow.editor.gridX"),
            /** */
            CFG_EDITOR_Y_GRID("workflow.editor.gridY"),
            /** */
            CFG_EDITOR_ZOOM("workflow.editor.zoomLevel"),
            /** Key for UI info's class name. */
            CFG_UIINFO_CLASS("ui_classname"),
            /** */
            CFG_UIINFO_SUB_CONFIG("ui_settings");

        /**
         * @param string
         */
        Const(final String string) {
            m_key = string;
        }

        /**
         * @return the key
         */
        public String get() {
            return m_key;
        }

        final String m_key;
    }

    private static final ConfigBaseRO EMPTY_SETTINGS = new SimpleConfig("<<empty>>");

    /** Format used to save author/edit infos. */
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

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

    /**
     * Also validates the directory argument.
     *
     * @param workflowDirectory from which to load the workflow.
     * @return the workflow.knime file in the given directory.
     */
    static File getWorkflowDotKnimeFile(final File workflowDirectory) throws IOException {
        if (workflowDirectory == null) {
            throw new IllegalArgumentException("Directory must not be null.");
        }
        if (!workflowDirectory.isDirectory()) {
            throw new IOException("Not a directory: " + workflowDirectory);
        }
        if (!workflowDirectory.canRead()) {
            throw new IOException("Cannot read from directory: " + workflowDirectory);
        }

        // template.knime or workflow.knime
        // TODO ReferencedFile usage
        var dotKNIMERef = new ReferencedFile(new ReferencedFile(workflowDirectory), Const.WORKFLOW_FILE_NAME.get());
        var dotKNIME = dotKNIMERef.getFile();

        if (!dotKNIME.isFile()) {
            throw new IOException(String.format("No %s file in directory %s", Const.WORKFLOW_FILE_NAME.get(),
                workflowDirectory.getAbsolutePath()));
        }
        return dotKNIME;
    }

    /**
     * Parses the file (usually workflow.knime) that describes the workflow.
     *
     * @param workflowDirectory containing the workflow
     */
    public static ConfigBaseRO parseWorkflowConfig(final File workflowDirectory) throws IOException {
        var workflowDotKnime = WorkflowLoader.getWorkflowDotKnimeFile(workflowDirectory);
        return SimpleConfig.parseConfig(workflowDotKnime.getAbsolutePath(), workflowDotKnime);
    }

    /**
     * @param nodeDirectory the directory that contains the node configuration and possibly the contained subworkflow
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    public static WorkflowDef load(final File nodeDirectory, final LoadVersion workflowFormatVersion)
        throws IOException, InvalidSettingsException {
        var workflowConfig = parseWorkflowConfig(nodeDirectory);
        return load(nodeDirectory, workflowConfig, workflowFormatVersion);
    }

    /**
     * @param workflowDirectory the directory that contains the workflow
     * @param workflowConfig the contents of the workflow.knime file in the workflow directory - since the
     *            {@link RootWorkflowLoader} has to parse the contents and we don't want to do it twice, we allow
     *            passing it in here.
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    public static WorkflowDef load(final File workflowDirectory, final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException, IOException {

        var builder = new WorkflowDefBuilder();

        // TODO error handling
        //      if (workflowKNIMEFile == null || m_workflowSett == null) {
        //          loadResult.setDirtyAfterLoad();
        //          throw new IllegalStateException("The method preLoadNodeContainer has either not been called or failed");
        //      }

        builder//
            .setName(loadWorkflowName(workflowConfig, workflowFormatVersion))//
            .setAuthorInformation(loadAuthorInformationDef(workflowConfig, workflowFormatVersion))
            .setAnnotations(loadAnnotationDefs(workflowConfig, workflowFormatVersion))//
            // TODO cipher
            //          .setCipher(loadCipher())//
            .setWorkflowEditorSettings(loadEditorUIInformationDef(workflowConfig, workflowFormatVersion));

        // TODO name methods consistently
        // TODO javadoc
        // nodes
        // TODO
        var nodesConfig = loadNodesConfig(workflowConfig);
        for (String nodeKey : nodesConfig.keySet()) {
            builder.putNodes(nodeKey,
                loadNodeDef(nodesConfig, nodeKey, workflowConfig, workflowDirectory, workflowFormatVersion));
        }

        // connections
        //
        var connectionsConfig = loadConnectionsConfig(workflowConfig, workflowFormatVersion);
        for (String connectionKey : connectionsConfig.keySet()) {
            var connection = loadConnectionDef(connectionsConfig.getConfigBase(connectionKey), workflowFormatVersion);
            builder.addConnections(connection);
        }

        return builder.build();

        // TODO error handling
        //      var nodeFile = knimeFile.getFile();
        //        var parentRef = knimeFile.getParent();
        //        if (parentRef == null) {
        //            loadResult.setDirtyAfterLoad();
        //            throw new IOException("Parent directory of file \"" + knimeFile + "\" is not represented by "
        //                + ReferencedFile.class.getSimpleName() + " object");
        //        }

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
    //        if ((workflowFormatVersion.ordinal() < LoadVersion.V250.ordinal()) || !settings.containsKey("cipher")) {
    //            return WorkflowCipher.NULL_CIPHER;
    //        }
    //        var cipherSettings = settings.getConfigBase("cipher");
    //        return WorkflowCipher.load(loadVersion, cipherSettings);
    //    }

    /**
     * The node settings file is typically named settings.xml (for native nodes and components) and workflow.knime for
     * meta nodes. However, the actual name is stored in the parent workflow's entry that describes the node.
     *
     * @param workflowNodeConfig the configuration tree in the workflow description (workflow.knime) that describes the
     *            node
     * @param workflowDir the directory that contains the node directory
     * @return
     * @throws InvalidSettingsException
     */
    private static File loadNodeFile(final ConfigBaseRO workflowNodeConfig, final File workflowDir)
        throws InvalidSettingsException {
        // relative path to node configuration file
        var fileString = workflowNodeConfig.getString(Const.KEY_NODE_SETTINGS_FILE.get());
        if (fileString == null) {
            throw new InvalidSettingsException(
                "Unable to read settings " + "file for node " + workflowNodeConfig.getKey());
        }
        // fileString is something like "File Reader(#1)/settings.xml", thus
        // it contains two levels of the hierarchy. We leave it here to the
        // java.io.File implementation to resolve these levels
        var nodeFile = new File(workflowDir, fileString);
        if (!nodeFile.isFile() || !nodeFile.canRead()) {
            throw new InvalidSettingsException("Unable to read settings " + "file " + nodeFile.getAbsolutePath());
        }
        return nodeFile;
    }

    /**
     * @param settings
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     * @return the type of node described by a settings entry (contained in workflow's workflow.knime file).
     * @throws InvalidSettingsException
     */
    private static NodeType loadNodeType(final ConfigBaseRO settings, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException {
        NodeType result;
        try {
            if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
                var factory = settings.getString("factory");
                if (ObsoleteMetaNodeFileWorkflowPersistor.OLD_META_NODES.contains(factory)) {
                    result = NodeType.MetaNode;
                } else {
                    result = NodeType.NativeNode;
                }
            } else if (workflowFormatVersion.isOlderThan(LoadVersion.V2100Pre)) {
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
            // TODO error handling
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

    private static String loadUIInfoClassName(final ConfigBaseRO settings, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            if (settings.containsKey(Const.KEY_UI_INFORMATION.get())) {
                return settings.getString(Const.KEY_UI_INFORMATION.get());
            }
        } else {
            if (settings.containsKey(Const.CFG_UIINFO_CLASS.get())) {
                return settings.getString(Const.CFG_UIINFO_CLASS.get());
            }
        }
        return null;
    }

    private static String loadWorkflowName(final ConfigBaseRO set, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            return "Workflow Manager";
        } else {
            return set.getString("name");
        }
    }

    private static int loadConnectionDestID(final ConfigBaseRO settings, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            return settings.getInt("targetID");
        } else {
            return settings.getInt("destID");
        }
    }

    private static int loadConnectionDestPort(final ConfigBaseRO settings, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            return settings.getInt("targetPort");
        } else {
            // possibly port index correction in fixDestPort method
            return settings.getInt("destPort");
        }
    }

    /**
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    private static ConfigBaseRO loadNodesConfig(final ConfigBaseRO workflowConfig) {
        try {
            return workflowConfig.getConfigBase(Const.KEY_NODES.get());
        } catch (InvalidSettingsException e) {
            // TODO error handling
            //            var error = "Can't load nodes in workflow, config not found: " + e.getMessage();
            //            getLogger().debug(error, e);
            //            loadResult.addError(error);
            //            loadResult.setDirtyAfterLoad();
            //            loadResult.setResetRequiredAfterLoad();
            // TODO error handling special
            // stop loading here
            return EMPTY_SETTINGS;
        }
    }

    /**
     * @param workflowConfig
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    private static ConfigBaseRO loadConnectionsConfig(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) {
        ConfigBaseRO connectionSettings;
        try {
            connectionSettings = workflowConfig.getConfigBase(Const.KEY_CONNECTIONS.get());
            if (connectionSettings == null) {
                connectionSettings = EMPTY_SETTINGS;
            }
            return connectionSettings;
        } catch (InvalidSettingsException e) {
            // TODO error handling
            //            var error = "Can't load workflow connections, config not found: " + e.getMessage();
            //            getLogger().debug(error, e);
            //            loadResult.setDirtyAfterLoad();
            //            loadResult.addError(error);
            return EMPTY_SETTINGS;
        }
    }


    /**
     * @param workflowConfig
     * @param workflowFormatVersion
     */
    private static List<AnnotationDataDef> loadAnnotationDefs(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V230)) {
            return List.of();
        } else {
            if (!workflowConfig.containsKey("annotations")) {
                return Collections.emptyList();
            }
            var annoSettings = workflowConfig.getConfigBase("annotations");
            List<AnnotationDataDef> result = new ArrayList<>();
            for (String key : annoSettings.keySet()) {
                var child = annoSettings.getConfigBase(key);
                result.add(loadAnnotationDef(child, workflowFormatVersion));
            }
            return result;
        }
    }

    /**
     * @param annotationConfig
     * @param workflowFormatVersion
     */
    private static AnnotationDataDef loadAnnotationDef(final ConfigBaseRO annotationConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        var builder = new AnnotationDataDefBuilder()//
            .setText(annotationConfig.getString("text"))//
            .setBgcolor(annotationConfig.getInt("bgcolor"))//
            .setLocation(CoreToDefUtil.createCoordinate(annotationConfig.getInt("x-coordinate"),
                annotationConfig.getInt("y-coordinate")))//
            .setWidth(
                annotationConfig.getInt("width"))//
            .setHeight(annotationConfig.getInt("height"))//
            .setBorderSize(annotationConfig.getInt("borderSize", 0)) // default to 0 for backward compatibility
            .setBorderColor(annotationConfig.getInt("borderColor", 0)) // default for backward compatibility
            .setDefaultFontSize(annotationConfig.getInt("defFontSize", -1)) // default for backward compatibility
            .setAnnotationVersion(annotationConfig.getInt("annotation-version", -1)) // default to VERSION_OLD
            .setTextAlignment(
                workflowFormatVersion.ordinal() >= LoadVersion.V250.ordinal() ? annotationConfig.getString("alignment") : "LEFT");

        ConfigBaseRO styleConfigs = annotationConfig.getConfigBase("styles");
        for (String key : styleConfigs.keySet()) {
            try {
                var def = loadStyleRangeDef(styleConfigs.getConfigBase(key));
                builder.addStyles(def);
            } catch (InvalidSettingsException ex) {
                // TODO
            }
        }
        return builder.build();
    }

    /**
     * @param styleConfig
     */
    private static StyleRangeDef loadStyleRangeDef(final ConfigBaseRO styleConfig) throws InvalidSettingsException {
        return new StyleRangeDefBuilder()//
            .setStart(styleConfig.getInt("start"))//
            .setLength(styleConfig.getInt("length"))//
            .setFontName(styleConfig.getString("fontname"))//
            .setFontStyle(styleConfig.getInt("fontstyle"))//
            .setFontSize(styleConfig.getInt("fontsize"))//
            .setColor(styleConfig.getInt("fgcolor"))//
            .build();
    }

    /**
     * @param nodesConfig the part of the workflow configuration that describes the nodes contained in the workflow
     * @param nodeKey the key that gives access to the part of nodesConfig that describes a particular node
     * @param workflowConfig
     * @param workflowDir
     * @param workflowFormatVersion
     * @return a node describing what's behind the given node id (might be a metanode, component, or native node).
     */
    private static NodeDef loadNodeDef(final ConfigBaseRO nodesConfig, final String nodeKey,
        final ConfigBaseRO workflowConfig, final File workflowDir, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException, IOException {

        if (!nodesConfig.containsKey(nodeKey)) {
            // TODO error handling
            var error = "Unable to load settings for node with internal id " + nodeKey;
            //            getLogger().debug(error, e);
            //            loadResult.setDirtyAfterLoad();
            //            loadResult.addError(error);
            throw new InvalidSettingsException(error);
        }

        // contains the information about the node that is in the workflow description
        ConfigBaseRO workflowNodeConfig = nodesConfig.getConfigBase(nodeKey);

        var nodeType = loadNodeType(workflowNodeConfig, workflowFormatVersion);

        //        Path.of(nodeConfigFile)
        // TODO maybe this can be moved
        var settingsFile = loadNodeFile(workflowNodeConfig, workflowDir);
        var nodeDirectory = settingsFile.getParentFile();

        // TODO error handling
        //
        // var childResult = new LoadResult(nodeType.toString() + " with ID suffix " + nodeIDSuffix);
        // var childDef = persistor.getLoadResult(this, nodeSetting, childResult);
        // loadResult.addChildError(childResult);
        switch (nodeType) {
            case MetaNode:
                return new MetaNodeLoader().load(workflowConfig, nodeDirectory, workflowFormatVersion).getNodeDef();
            case NativeNode:
                return new NativeNodeLoader().load(workflowConfig, nodeDirectory, workflowFormatVersion).getNodeDef();
            case SubNode:
                return new ComponentLoader().load(workflowConfig, nodeDirectory, workflowFormatVersion).getNodeDef();
            default:
                throw new IllegalStateException("Unknown node type: " + nodeType);
        }

        // TODO error handling
        // catch {
        //            var error =
        //                "Unable to load node with ID suffix " + nodeIDSuffix + " into workflow, skipping it: " + e.getMessage();
        //            String loadErrorString;
        //            if (e instanceof NodeFactoryUnknownException) {
        //                loadErrorString = e.getMessage();
        //            } else {
        //                loadErrorString = error;
        //            }
        //            if (e instanceof InvalidSettingsException || e instanceof IOException
        //                || e instanceof NodeFactoryUnknownException) {
        //                getLogger().debug(error, e);
        //            } else {
        //                getLogger().error(error, e);
        //            }
        //            loadResult.addError(loadErrorString);
        //            if (e instanceof NodeFactoryUnknownException) {
        //                missingNodeIDMap.put(nodeIDSuffix, (NodeFactoryUnknownException)e);
        //                // don't set dirty
        //            } else {
        //                loadResult.setDirtyAfterLoad();
        //                failingNodeIDSet.add(nodeIDSuffix);
        //                // node directory is the parent of the settings.xml
        //                m_obsoleteNodeDirectories.add(nodeFile.getParent());
        //                return Optional.empty();
        //            }
        //        }
    }

    /**
     * @param connectionConfig the part of the workflow configurations that describes one particular connection
     * @param workflowFormatVersion
     */
    private static ConnectionDef loadConnectionDef(final ConfigBaseRO connectionConfig, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException {

        //        try {
        var sourceID = connectionConfig.getInt("sourceID");
        var destID = loadConnectionDestID(connectionConfig, workflowFormatVersion);
        var sourcePort = connectionConfig.getInt("sourcePort");
        var destPort = loadConnectionDestPort(connectionConfig, workflowFormatVersion);
        // this attribute is in most cases not present (not saved)
        var isDeletable = connectionConfig.getBoolean("isDeletable", true);
        if (sourceID != -1 && sourceID == destID) {
            throw new InvalidSettingsException("Source and Destination must not be equal, id is " + sourceID);
        }

        return new ConnectionDefBuilder()//
            .setSourceID(sourceID)//
            .setSourcePort(sourcePort)//
            .setDestID(destID)//
            .setDestPort(destPort)//
            .setDeletable(isDeletable)//
            .setUiSettings(loadConnectionUISettingsDef(connectionConfig, workflowFormatVersion)).build();
        //        } catch (InvalidSettingsException e) {
        //            // TODO error handling
        //            var error = "Can't load connection with internal ID \"" + connectionKey + "\": " + e.getMessage();
        //            getLogger().debug(error, e);
        //            loadResult.setDirtyAfterLoad();
        //            loadResult.addError(error);
        //        }
    }

    /**
     * @param connectionConfig the part of the workflow configurations that describes one particular connection
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    private static ConnectionUISettingsDef loadConnectionUISettingsDef(final ConfigBaseRO connectionConfig,
        final LoadVersion workflowFormatVersion) {
        var builder = new ConnectionUISettingsDefBuilder();
        try {
            var uiInfoClass = loadUIInfoClassName(connectionConfig, workflowFormatVersion);
            if (uiInfoClass != null) {
                if (!uiInfoClass.equals("org.knime.core.node.workflow.ConnectionUIInformation")) {
                    // TODO
                    //                    getLogger().debug("Could not load UI information for " + "connection between nodes " + sourceID
                    //                        + " and " + destID + ": expected " + ConnectionUIInformation.class.getName() + " but got "
                    //                        + uiInfoClass.getClass().getName());
                } else {
                    // in previous releases, the settings were directly written to the
                    // top-most node settings object; since 2.0 they are put into a
                    // separate sub-settings object
                    var subSettings = workflowFormatVersion.isOlderThan(LoadVersion.V200) ? connectionConfig
                        : connectionConfig.getConfigBase(Const.CFG_UIINFO_SUB_CONFIG.get());
                    var size = subSettings.getInt(Const.KEY_BENDPOINTS.get() + "_size");
                    for (var i = 0; i < size; i++) {
                        var tmp = subSettings.getIntArray(Const.KEY_BENDPOINTS.get() + "_" + i);
                        //TODO add bendpoint directly as int array
                        builder.addBendPoints(CoreToDefUtil.createCoordinate(tmp[0], tmp[1]));
                    }
                }
            }
        } catch (Throwable t) {
            // TODO error handling
            //            getLogger().warn(
            //                "Exception while loading connection UI " + "information between nodes " + sourceID + " and " + destID,
            //                t);
        }
        return builder.build();
    }

    /**
     * Load editor information (grid settings, zoom level, et cetera).
     *
     * @param workflowConfig
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    private static WorkflowUISettingsDef loadEditorUIInformationDef(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        var builder = new WorkflowUISettingsDefBuilder();
        if (workflowFormatVersion.isOlderThan(LoadVersion.V260) || !workflowConfig.containsKey(Const.CFG_EDITOR_INFO.get())) {
            return builder.build();
        }
        var editorCfg = workflowConfig.getConfigBase(Const.CFG_EDITOR_INFO.get());
        builder.setSnapToGrid(editorCfg.getBoolean(Const.CFG_EDITOR_SNAP_GRID.get()))//
            .setShowGrid(editorCfg.getBoolean(Const.CFG_EDITOR_SHOW_GRID.get()))//
            .setGridX(editorCfg.getInt(Const.CFG_EDITOR_X_GRID.get()))//
            .setGridY(editorCfg.getInt(Const.CFG_EDITOR_Y_GRID.get()))//
            .setZoomLevel(editorCfg.getDouble(Const.CFG_EDITOR_ZOOM.get()));
        if (editorCfg.containsKey(Const.CFG_EDITOR_CURVED_CONNECTIONS.get())) {
            builder.setCurvedConnections((editorCfg.getBoolean(Const.CFG_EDITOR_CURVED_CONNECTIONS.get())));
        }
        if (editorCfg.containsKey(Const.CFG_EDITOR_CONNECTION_WIDTH.get())) {
            builder.setConnectionLineWidth(editorCfg.getInt(Const.CFG_EDITOR_CONNECTION_WIDTH.get()));
        }
        return builder.build();
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
    //        if ((workflowFormatVersion.ordinal() < LoadVersion.V250.ordinal()) || !settings.containsKey("cipher")) {
    //            return WorkflowCipher.NULL_CIPHER;
    //        }
    //        var cipherSettings = settings.getConfigBase("cipher");
    //        return WorkflowCipher.load(loadVersion, cipherSettings);
    //    }

    private static AuthorInformationDef loadAuthorInformationDef(final ConfigBaseRO settings,
        final LoadVersion loadVersion) throws InvalidSettingsException {

        if (loadVersion.ordinal() >= LoadVersion.V280.ordinal()
            && settings.containsKey(Const.CFG_AUTHOR_INFORMATION.get())) {
            final var sub = settings.getConfigBase(Const.CFG_AUTHOR_INFORMATION.get());
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
    //        if ((workflowFormatVersion.ordinal() < LoadVersion.V250.ordinal()) || !settings.containsKey("cipher")) {
    //            return WorkflowCipher.NULL_CIPHER;
    //        }
    //        var cipherSettings = settings.getConfigBase("cipher");
    //        return WorkflowCipher.load(loadVersion, cipherSettings);
    //    }

}
