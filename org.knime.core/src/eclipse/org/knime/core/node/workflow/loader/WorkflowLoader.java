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
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ObsoleteMetaNodeFileWorkflowPersistor;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.node.workflow.loader.LoaderUtils.Const;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.AuthorInformationDef;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.ConnectionUISettingsDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.WorkflowDef;
import org.knime.core.workflow.def.WorkflowUISettingsDef;
import org.knime.core.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.core.workflow.def.impl.AuthorInformationDefBuilder;
import org.knime.core.workflow.def.impl.ConnectionDefBuilder;
import org.knime.core.workflow.def.impl.ConnectionUISettingsDefBuilder;
import org.knime.core.workflow.def.impl.NodeDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowDefBuilder;
import org.knime.core.workflow.def.impl.WorkflowUISettingsDefBuilder;

/**
 * Recursively walks through the legacy directory structure, loading the workflow settings into the {@link WorkflowDef}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("javadoc")
public class WorkflowLoader {

    /** KNIME Node type: native, meta or sub node. */
    enum NodeType {
            METANODE("metanode"), NATIVENODE("nativenode"), COMPONENT("subnode");

        private final String m_name;

        /** @param shortName -- toString result */
        private NodeType(final String name) {
            m_name = name;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_name;
        }
    }

    private static final ConfigBaseRO EMPTY_SETTINGS = new SimpleConfig("<<empty>>");

    private static final String DEFAULT_WORKFLOW_NAME = "Workflow";

    private static final String WORKFLOW_MANAGER = "Workflow Manager";

    private static final String CONNECTION_UI_CLASSNAME = "org.knime.core.node.workflow.ConnectionUIInformation";

    private static final AuthorInformationDef DEFAULT_AUTHOR_INFORMATION = new AuthorInformationDefBuilder()//
        .setAuthoredBy("<unknown>") //
        .setAuthoredWhen(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)) //
        .setLastEditedBy(null) //
        .setLastEditedWhen(null) //
        .build();

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
        final LoadVersion loadVersion) {

        var builder = new WorkflowDefBuilder() //
            .setName(() -> loadName(workflowConfig, loadVersion), DEFAULT_WORKFLOW_NAME)//
            .setAuthorInformation(() -> loadAuthorInformation(workflowConfig, loadVersion), DEFAULT_AUTHOR_INFORMATION)
            .setWorkflowEditorSettings(() -> loadWorkflowUISettings(workflowConfig, loadVersion),
                new WorkflowUISettingsDefBuilder().build());
        setNodes(builder, workflowConfig, workflowDirectory, loadVersion);
        setConnections(builder, workflowConfig, loadVersion);
        setAnnotations(builder, workflowConfig, loadVersion);

        return builder.build();
    }

    private static void setNodes(final WorkflowDefBuilder builder, final ConfigBaseRO workflowConfig,
        final File directory, final LoadVersion loadVersion) {
        try {
            var nodesSettings = workflowConfig.getConfigBase(Const.WORKFLOW_NODES_KEY.get());
            if (nodesSettings != null) {
                nodesSettings.keySet()
                    .forEach(key -> builder.putToNodes(key,
                        () -> loadNode(nodesSettings.getConfigBase(key), workflowConfig, directory, loadVersion),
                        new NodeDefBuilder().build()));
            }
        } catch (InvalidSettingsException ex) {
            builder.setNodes(() -> {
                throw ex;
            }, Map.of());
        }
    }

    private static void setConnections(final WorkflowDefBuilder builder, final ConfigBaseRO workflowConfig,
        final LoadVersion loadVersion) {
        try {
            var connectionsSettings = workflowConfig.getConfigBase(Const.WORKFLOW_CONNECTIONS_KEY.get());
            if (connectionsSettings != null) {
                connectionsSettings.keySet()
                    .forEach(key -> builder.addToConnections(
                        () -> loadConnection(connectionsSettings.getConfigBase(key), loadVersion),
                        new ConnectionDefBuilder().build()));
            }
        } catch (InvalidSettingsException ex) {
            builder.setConnections(() -> {
                throw ex;
            }, List.of());
        }
    }

    private static void setAnnotations(final WorkflowDefBuilder builder, final ConfigBaseRO workflowConfig,
        final LoadVersion loadVersion) {
        if (loadVersion.isOlderThan(LoadVersion.V230)
            || !workflowConfig.containsKey(Const.WORKFLOW_ANNOTATIONS_KEY.get())) {
            builder.setAnnotations(List.of());
        } else {
            try {
                var annoSettings = workflowConfig.getConfigBase(Const.WORKFLOW_ANNOTATIONS_KEY.get());
                if (annoSettings != null) {
                    annoSettings.keySet()
                        .forEach(key -> builder.addToAnnotations(
                            () -> LoaderUtils.loadAnnotation(annoSettings.getConfigBase(key), loadVersion),
                            new AnnotationDataDefBuilder().build()));
                }
            } catch (InvalidSettingsException ex) {
                builder.setConnections(() -> {
                    throw ex;
                }, List.of());
            }
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
    private static NodeDef loadNode(final ConfigBaseRO nodeConfig, final ConfigBaseRO workflowConfig,
        final File workflowDir, final LoadVersion workflowFormatVersion) throws InvalidSettingsException, IOException {

        var settingsFile = LoaderUtils.loadNodeFile(nodeConfig, workflowDir);
        var nodeDirectory = settingsFile.getParentFile();

        switch (loadNodeType(nodeConfig, workflowFormatVersion)) {
            case METANODE:
                return MetaNodeLoader.load(workflowConfig, nodeDirectory, workflowFormatVersion);
            case NATIVENODE:
                return NativeNodeLoader.load(workflowConfig, nodeDirectory, workflowFormatVersion);
            case COMPONENT:
                return ComponentLoader.load(workflowConfig, nodeDirectory, workflowFormatVersion);
            default:
                throw new IllegalStateException("Unknown node type");
        }
    }

    /**
     * @param settings
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     * @return the type of node described by a settings entry (contained in workflow's workflow.knime file).
     * @throws InvalidSettingsException
     */
    private static NodeType loadNodeType(final ConfigBaseRO settings, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            var factory = settings.getString("factory");
            if (ObsoleteMetaNodeFileWorkflowPersistor.OLD_META_NODES.contains(factory)) {
                return NodeType.METANODE;
            } else {
                return NodeType.NATIVENODE;
            }
        } else if (workflowFormatVersion.isOlderThan(LoadVersion.V2100Pre)) {
            return settings.getBoolean("node_is_meta") ? NodeType.METANODE : NodeType.NATIVENODE;
        } else {
            final var nodeType = settings.getString("node_type");
            return getNodeType(nodeType);
        }
    }

    private static NodeType getNodeType(final String nodeType) throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(nodeType, "node type must not be null");
        switch (nodeType.toLowerCase()) {
            case "metanode":
                return NodeType.METANODE;
            case "subnode":
                return NodeType.COMPONENT;
            default:
                return NodeType.NATIVENODE;
        }
    }

    private static Optional<String> loadUIInfoClassName(final ConfigBaseRO settings,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        String className = null;
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            if (settings.containsKey(Const.EXTRA_INFO_CLASS_NAME_KEY.get())) {
                className = settings.getString(Const.EXTRA_INFO_CLASS_NAME_KEY.get());
            }
        } else {
            if (settings.containsKey(Const.UI_CLASSNAME_KEY.get())) {
                className = settings.getString(Const.UI_CLASSNAME_KEY.get());
            }
        }
        return Optional.ofNullable(className == null || className.equals(CONNECTION_UI_CLASSNAME) ? null : className);
    }

    private static String loadName(final ConfigBaseRO set, final LoadVersion workflowFormatVersion)
        throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            return WORKFLOW_MANAGER;
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
     *
     * @param connectionConfig
     * @param loadVersion
     * @return
     * @throws InvalidSettingsException
     */
    private static ConnectionDef loadConnection(final ConfigBaseRO connectionConfig, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        var builder = new ConnectionDefBuilder()//
            .setSourceID(connectionConfig.getInt("sourceID"))//
            .setSourcePort(connectionConfig.getInt("sourcePort")) //
            .setDestID(loadConnectionDestID(connectionConfig, loadVersion))//
            .setDestPort(loadConnectionDestPort(connectionConfig, loadVersion))//
            .setDeletable(connectionConfig.getBoolean("isDeletable", true));
        setConnectionUISettings(builder, connectionConfig, loadVersion);

        return builder.build();
    }

    private static ConnectionUISettingsDef loadConnectionBendPoints(final ConfigBaseRO connectionConfig,
        final LoadVersion loadVersion) {
        var uiBuilder = new ConnectionUISettingsDefBuilder();
        try {
            var subSettings = loadVersion.isOlderThan(LoadVersion.V200) ? connectionConfig
                : connectionConfig.getConfigBase(Const.UI_SETTINGS_KEY.get());
            var size = subSettings.getInt(Const.EXTRA_INFO_CONNECTION_BENDPOINTS_KEY.get() + "_size");
            for (var i = 0; i < size; i++) {
                var tmp = subSettings.getIntArray(Const.EXTRA_INFO_CONNECTION_BENDPOINTS_KEY.get() + "_" + i);
                uiBuilder.addToBendPoints(CoreToDefUtil.createCoordinate(tmp[0], tmp[1]));
            }
        } catch (InvalidSettingsException e) {
            uiBuilder.setBendPoints(() -> {
                throw e;
            }, List.of());
        }
        return uiBuilder.build();
    }

    /**
     * TODO
     *
     * @param connectionConfig the part of the workflow configurations that describes one particular connection
     * @param workflowFormatVersion the version of the workflow format that was used to write the workflow to load
     */
    private static void setConnectionUISettings(final ConnectionDefBuilder builder, final ConfigBaseRO connectionConfig,
        final LoadVersion loadVersion) {
        try {
            var optClassName = loadUIInfoClassName(connectionConfig, loadVersion);
            if (optClassName.isPresent()) {
                builder.setUiSettings(loadConnectionBendPoints(connectionConfig, loadVersion));
            }
        } catch (InvalidSettingsException ex) {
            builder.setUiSettings(() -> {
                throw ex;
            }, null);
        }
    }

    /**
     *
     * Load editor information (grid settings, zoom level, etc).
     *
     * @param workflowConfig
     * @param workflowFormatVersion
     * @return
     * @throws InvalidSettingsException
     */
    private static WorkflowUISettingsDef loadWorkflowUISettings(final ConfigBaseRO workflowConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        var builder = new WorkflowUISettingsDefBuilder();
        if (workflowFormatVersion.isOlderThan(LoadVersion.V260)
            || !workflowConfig.containsKey(Const.WORKFLOW_EDITOR_SETTINGS_KEY.get())) {
            return builder.build();
        }
        var editorCfg = workflowConfig.getConfigBase(Const.WORKFLOW_EDITOR_SETTINGS_KEY.get());
        builder.setSnapToGrid(editorCfg.getBoolean(Const.WORKFLOW_EDITOR_SNAPTOGRID_KEY.get()))//
            .setShowGrid(editorCfg.getBoolean(Const.WORKFLOW_EDITOR_SHOWGRID_KEY.get()))//
            .setGridX(editorCfg.getInt(Const.WORKFLOW_EDITOR_GRID_X_KEY.get()))//
            .setGridY(editorCfg.getInt(Const.WORKFLOW_EDITOR_GIRD_Y_KEY.get()))//
            .setZoomLevel(editorCfg.getDouble(Const.WORKFLOW_EDITOR_ZOOM_LEVEL_KEY.get()));
        if (editorCfg.containsKey(Const.WORKFLOW_EDITOR_CURVED_CONNECTIONS_KEY.get())) {
            builder.setCurvedConnections((editorCfg.getBoolean(Const.WORKFLOW_EDITOR_CURVED_CONNECTIONS_KEY.get())));
        }
        if (editorCfg.containsKey(Const.WORKFLOW_EDITOR_CONNECTION_WIDTH_KEY.get())) {
            builder.setConnectionLineWidth(editorCfg.getInt(Const.WORKFLOW_EDITOR_CONNECTION_WIDTH_KEY.get()));
        }
        return builder.build();
    }

    private static AuthorInformationDef loadAuthorInformation(final ConfigBaseRO settings,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.ordinal() >= LoadVersion.V280.ordinal()
            && settings.containsKey(Const.AUTHOR_INFORMATION_KEY.get())) {
            final var sub = settings.getConfigBase(Const.AUTHOR_INFORMATION_KEY.get());
            if (sub == null) {
                return DEFAULT_AUTHOR_INFORMATION;
            }
            return new AuthorInformationDefBuilder()//
                .setAuthoredBy(() -> loadAuthorSetting(sub, Const.AUTHORED_BY_KEY.get()), "<unknown>")//
                .setAuthoredWhen(() -> loadAuthorInformationDate(sub, Const.AUTHORED_WHEN_KEY.get()),
                    OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))//
                .setLastEditedBy(() -> loadAuthorSetting(sub, Const.LAST_EDITED_BY_KEY.get()), "<unknown>")//
                .setLastEditedWhen(() -> loadAuthorInformationDate(sub, Const.LAST_EDITED_WHEN_KEY.get()),
                    OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC))//
                .build();
        } else {
            return DEFAULT_AUTHOR_INFORMATION;
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
        throws InvalidSettingsException, DateTimeParseException {
        var date = sub.getString(key);
        return date == null ? null : LoaderUtils.parseDate(date);
    }
}
