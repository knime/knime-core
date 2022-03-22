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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ObsoleteMetaNodeFileWorkflowPersistor;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Pair;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.ConnectionUISettingsDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowUISettingsDef;
import org.knime.core.workflow.def.impl.AuthorInformationDefBuilder;
import org.knime.core.workflow.def.impl.ConnectionDefBuilder;
import org.knime.core.workflow.def.impl.ConnectionUISettingsDefBuilder;
import org.knime.core.workflow.def.impl.FallibleConnectionDef;
import org.knime.core.workflow.def.impl.FallibleNodeDef;
import org.knime.core.workflow.def.impl.NodeDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowUISettingsDefBuilder;
import org.knime.core.workflow.loader.LoadException;

/**
 * Recursively walks through the legacy directory structure, generating the workflow defs.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
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
            NODES_KEY("nodes"),
            /** */
            KEY_UI_INFORMATION("extraInfoClassName"),
            /**
             * The relative path of the file that contains the node configuration. For example
             * {@code 2_1 Building (#1444)/settings.xml}
             */
            /** Constant for the meta info file name. */
            METAINFO_FILE_NAME("workflowset.meta"),
            /** Identifier for KNIME templates SVG export when saved to disk. */
            SVG_TEMPLATE_FILE("template.svg"),
            /** Identifier for KNIME workflows SVG export when saved to disk. */
            SVG_WORKFLOW_FILE_NAME("workflow.svg"),
            /** Identifier for KNIME meta mode templates when saved to disk. */
            TEMPLATE_FILE_NAME("template.knime"),

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

    private static final AuthorInformationDef DEFAULT_AUTHOR_INFORMATION = new AuthorInformationDefBuilder()//
        .setAuthoredBy("<unknown>") //
        .setAuthoredWhen(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) //
        .setLastEditedBy(null) //
        .setLastEditedWhen(null) //
        .build();

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
     * @param nodeDirectory the directory that contains the node configuration and possibly the contained subworkflow
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    public static WorkflowDef load(final File nodeDirectory, final LoadVersion workflowFormatVersion)
        throws IOException {
        var workflowConfig = LoaderUtils.parseWorkflowConfig(nodeDirectory);
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
        final LoadVersion workflowFormatVersion) {

        return new WorkflowDefBuilder() //
            .setName(() -> loadName(workflowConfig, workflowFormatVersion), "Workflow")//
            .setAuthorInformation(() -> loadAuthorInformationDef(workflowConfig, workflowFormatVersion),
                DEFAULT_AUTHOR_INFORMATION)
            .setAnnotations(() -> loadAnnotationDefs(workflowConfig, workflowFormatVersion), List.of())//
            .setNodes(() -> loadNodesDef(workflowConfig, workflowDirectory, workflowFormatVersion), Map.of()) //
            .setConnections(() -> loadConnectionsDef(workflowConfig, workflowFormatVersion), List.of()) //
            .setWorkflowEditorSettings(() -> loadEditorUIInformationDef(workflowConfig, workflowFormatVersion),
                new WorkflowUISettingsDefBuilder().build()) //
            .build();
    }

    /**
     *
     * @param workflowConfig
     * @param workflowDirectory
     * @param workflowFormatVersion
     * @return
     * @throws InvalidSettingsException
     */
    private static Map<String, NodeDef> loadNodesDef(final ConfigBaseRO workflowConfig, final File workflowDirectory,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        var nodesSettings = workflowConfig.getConfigBase(Const.NODES_KEY.get());
        return nodesSettings.keySet().stream().map(key -> {
            try {
                var nodeSetting = nodesSettings.getConfigBase(key);
                var nodeDef = loadNodeDef(nodeSetting, key, workflowConfig, workflowDirectory, workflowFormatVersion);
                return Pair.create(key, nodeDef);
            } catch (InvalidSettingsException | IOException e) {
                var exception = new LoadException(WorkflowDef.Attribute.NODES_ELEMENTS, e);
                return Pair.create(key, new FallibleNodeDef(new NodeDefBuilder().build(), exception));
            }
        }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    /**
     *
     * @param workflowConfig
     * @param workflowFormatVersion
     * @return
     * @throws InvalidSettingsException
     */
    private static List<ConnectionDef> loadConnectionsDef(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        var connectionsSettings = workflowConfig.getConfigBase(Const.KEY_CONNECTIONS.get());
        return connectionsSettings.keySet().stream().map(key -> {
            try {
                var connectionSetting = connectionsSettings.getConfigBase(key);
                return loadConnectionDef(connectionSetting, workflowFormatVersion);
            } catch (InvalidSettingsException e) {
                var exception = new LoadException(WorkflowDef.Attribute.CONNECTIONS_ELEMENTS, e);
                return new FallibleConnectionDef(new ConnectionDefBuilder().build(), exception);
            }
        }).collect(Collectors.toList());

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
                    // TODO might be error prone?
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

    private static String loadName(final ConfigBaseRO set, final LoadVersion workflowFormatVersion)
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
     * @param workflowConfig
     * @param workflowFormatVersion
     */
    private static List<AnnotationDataDef> loadAnnotationDefs(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V230) || !workflowConfig.containsKey("annotations")) {
            return List.of();
        } else {
            var annoSettings = workflowConfig.getConfigBase("annotations");
            List<AnnotationDataDef> result = new ArrayList<>();
            for (String key : annoSettings.keySet()) {
                var child = annoSettings.getConfigBase(key);
                result.add(LoaderUtils.loadAnnotationDef(child, workflowFormatVersion));
            }
            return result;
        }
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

        // contains the information about the node that is in the workflow description
        var workflowNodeConfig = nodesConfig.getConfigBase(nodeKey);
        var settingsFile = LoaderUtils.loadNodeFile(workflowNodeConfig, workflowDir);
        var nodeDirectory = settingsFile.getParentFile();

        var nodeType = loadNodeType(workflowNodeConfig, workflowFormatVersion);
        switch (nodeType) {
            case MetaNode:
                return MetaNodeLoader.load(workflowConfig, nodeDirectory, workflowFormatVersion);
            case NativeNode:
                return NativeNodeLoader.load(workflowConfig, nodeDirectory, workflowFormatVersion);
            case SubNode:
                return ComponentLoader.load(workflowConfig, nodeDirectory, workflowFormatVersion);
            default:
                throw new IllegalStateException("Unknown node type: " + nodeType);
        }
    }

    /**
     *
     * @param connectionConfig
     * @param workflowFormatVersion
     * @return
     */
    private static ConnectionDef loadConnectionDef(final ConfigBaseRO connectionConfig,
        final LoadVersion workflowFormatVersion) {
        //TODO Should we check this later?
        //        if (sourceID != -1 && sourceID == destID) {
        //            throw new InvalidSettingsException("Source and Destination must not be equal, id is " + sourceID);
        //        }
        return new ConnectionDefBuilder()//
            .setSourceID(() -> connectionConfig.getInt("sourceID"), 0)//
            .setSourcePort(() -> connectionConfig.getInt("sourcePort"), 0) //
            .setDestID(() -> loadConnectionDestID(connectionConfig, workflowFormatVersion), 0)//
            .setDestPort(() -> loadConnectionDestPort(connectionConfig, workflowFormatVersion), 0)//
            .setDeletable(connectionConfig.getBoolean("isDeletable", true))//
            .setUiSettings(loadConnectionUISettingsDef(connectionConfig, workflowFormatVersion)) //
            .build();
    }

    /**
     * FIXME Should we have the ui info class name in the def.yaml?
     *
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
                        builder.addToBendPoints(CoreToDefUtil.createCoordinate(tmp[0], tmp[1]));
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
     * Load editor information (grid settings, zoom level, etc).
     *
     * @param workflowConfig
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    private static WorkflowUISettingsDef loadEditorUIInformationDef(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        var builder = new WorkflowUISettingsDefBuilder();
        if (workflowFormatVersion.isOlderThan(LoadVersion.V260)
            || !workflowConfig.containsKey(Const.CFG_EDITOR_INFO.get())) {
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

    private static AuthorInformationDef loadAuthorInformationDef(final ConfigBaseRO settings,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        try {
            if (loadVersion.ordinal() >= LoadVersion.V280.ordinal()
                && settings.containsKey(Const.CFG_AUTHOR_INFORMATION.get())) {
                final var sub = settings.getConfigBase(Const.CFG_AUTHOR_INFORMATION.get());
                if (sub == null) {
                    return DEFAULT_AUTHOR_INFORMATION;
                }
                return new AuthorInformationDefBuilder()//
                    .setAuthoredBy(() -> loadAuthorSetting(sub, "authored-by"), "<unknown>")//
                    .setAuthoredWhen(() -> loadAuthorInformationDate(sub, "authored-when"),
                        OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))//
                    .setLastEditedBy(() -> loadAuthorSetting(sub, "lastEdited-by"), "<unknown>")//
                    .setLastEditedWhen(() -> loadAuthorInformationDate(sub, "lastEdited-when"),
                        OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))//
                    .build();
            } else {
                return DEFAULT_AUTHOR_INFORMATION;
            }
        } catch (InvalidSettingsException e) {
            var errorMessage = "Unable to load workflow author information: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static String loadAuthorSetting(final ConfigBaseRO sub, final String key) throws InvalidSettingsException {
        try {
            return sub.getString(key);
        } catch (InvalidSettingsException e) {
            var errorMessage = String.format("Unable to load the %s: %s", key, e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static OffsetDateTime loadAuthorInformationDate(final ConfigBaseRO sub, final String key)
        throws InvalidSettingsException {
        try {
            var date = sub.getString(key);
            return date == null ? null : parseDate(date);
        } catch (InvalidSettingsException e) {
            var errorMessage = String.format("Unable to load the %s: %s", key, e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        } catch (DateTimeParseException e) {
            //TODO Should we warn for this?
            return OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
        }
    }
}
