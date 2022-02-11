/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   31 Jan 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.loader;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ObsoleteMetaNodeFileWorkflowPersistor;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.workflowalizer.NodeMetadata.NodeType;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.BoundsDef;
import org.knime.core.workflow.def.CoordinateDef;
import org.knime.core.workflow.def.JobManagerDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeDef;
import org.knime.core.workflow.def.NodeLocksDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.core.workflow.def.impl.BoundsDefBuilder;
import org.knime.core.workflow.def.impl.CoordinateDefBuilder;
import org.knime.core.workflow.def.impl.JobManagerDefBuilder;
import org.knime.core.workflow.def.impl.NodeAnnotationDefBuilder;
import org.knime.core.workflow.def.impl.NodeDefBuilder;
import org.knime.core.workflow.def.impl.NodeLocksDefBuilder;
import org.knime.core.workflow.def.impl.NodeUIInfoDefBuilder;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
class NodeLoader {

    private final NodeDefBuilder m_nodeBuilder;

    private Optional<Integer> m_nodeId;

    // Can be component, native, metanode builder
    NodeLoader(final NodeDefBuilder nodeBuilder) {
        m_nodeBuilder = nodeBuilder;
        m_nodeId = Optional.empty();
    }

    /**
     * @param parentSettings
     * @param settings
     * @param singleNode
     */
    void load(final ConfigBaseRO parentSettings, final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {

        // the load methods should throw specific error messages
        m_nodeId = loadNodeId(parentSettings);
        m_nodeBuilder.setId(m_nodeId.get()) //
            .setAnnotation(loadAnnotation(parentSettings, loadVersion)) //
            .setCustomDescription(loadCustomDescription(parentSettings, settings, loadVersion)) //
            .setJobManager(loadJobManager(settings)) //
            .setLocks(loadLocks(parentSettings, loadVersion)) //
            .setNodeType(loadType(parentSettings, loadVersion)) //
            .setUiInfo(loadUIInfo(parentSettings));
    }

    private static Optional<Integer> loadNodeId(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            return Optional.ofNullable(settings.getInt("id"));
        } catch (InvalidSettingsException e) {
            var random = getRandomNodeID();
            var errorMessage = String.format("Unable to load node ID (internal id \"%s\"), trying random number %d",
                settings.getKey(), random);
            //  Feed the error message to the new loadResult
            return Optional.of(random);
        }
    }

    private static Integer getRandomNodeID() {
        return 10000 + (int)(Math.random() * 10000);
        //TODO The old functionality could check if the node container map contained the generated random int
        // the new one doesn't have access to the node ids of the other workflow's node.

        // Old functionality
        // some number between 10k and 20k, hopefully unique.
        //        int nodeIDSuffix = 10000 + (int)(Math.random() * 10000);
        //        while (m_nodeContainerLoaderMap.containsKey(nodeIDSuffix)) {
        //            nodeIDSuffix += 1;
        //        }
        //        return nodeIDSuffix;
    }

    private static NodeAnnotationDef loadAnnotation(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        if (!loadVersion.isOlderThan(LoadVersion.V230) || !settings.containsKey("annotations")) {
            // no credentials in v2.2 and before
            return new NodeAnnotationDefBuilder().build();
        }
        return new NodeAnnotationDefBuilder() //
            .setAnnotationDefault(false) //
            .setData(loadAnnotationDataDef(settings)) //
            .build();
    }

    private static AnnotationDataDef loadAnnotationDataDef(final ConfigBaseRO settings)
        throws InvalidSettingsException {
        try {
            return new AnnotationDataDefBuilder() //
                .setText(settings.getString("text")) //
                .setBgcolor(settings.getInt("bgcolor")) //
                .setLocation(loadCoordinateDef(settings)) //
                .setBorderColor(settings.getInt("borderColor")) //
                .setWidth(settings.getInt("width")) //
                .setHeight(settings.getInt("height")) //
                .setAlignment(settings.getString("alignment")) //
                .setBorderSize(settings.getInt("borderSize")) //
                .setDefFontSize(settings.getInt("defFontSize")) //
                .setAnnotationVersion(settings.getInt("annotation-version")) //
                .build();
        } catch (InvalidSettingsException e) {
            // TODO Proper exception handling
            return new AnnotationDataDefBuilder().build();
        }
    }

    private static CoordinateDef loadCoordinateDef(final ConfigBaseRO settings) throws InvalidSettingsException {
        return CoreToDefUtil.createCoordinate(settings.getInt("x-coordinate"), settings.getInt("y-coordinate"));
    }

    private static JobManagerDef loadJobManager(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            if (!settings.containsKey("job.manager")) {
                return new JobManagerDefBuilder().build();
            }
            var jobManagerSettings = settings.getConfigBase("job.manager");
            var factoryId = jobManagerSettings.getString("job.manager.factory.id");
            return new JobManagerDefBuilder()//
                .setFactory(factoryId)//
                .setSettings(CoreToDefUtil.toConfigMapDef(jobManagerSettings.getConfigBase("job.manager.settings")))
                .build();
        } catch (InvalidSettingsException e) {
            // TODO proper error handling
        }
        return new JobManagerDefBuilder().build();

    }

    private static NodeLocksDef loadLocks(final ConfigBaseRO settings, final LoadVersion loadVersion) {
        var hasDeleteLock = loadVersion.isOlderThan(LoadVersion.V200) ? true : settings.getBoolean("isDeletable", true);
        var hasResetLock =
            loadVersion.isOlderThan(LoadVersion.V3010) ? false : settings.getBoolean("hasResetLock", false);
        var hasConfigureLock =
            loadVersion.isOlderThan(LoadVersion.V3010) ? false : settings.getBoolean("hasConfigureLock", false);

        return new NodeLocksDefBuilder() //
            .setHasConfigureLock(hasConfigureLock) //
            .setHasDeleteLock(hasDeleteLock) //
            .setHasResetLock(hasResetLock) //
            .build();
    }

    private String loadType(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        try {
            return loadNodeType(settings, loadVersion).toString();
        } catch (InvalidSettingsException e) {
            // Freed the error to the new load result
            var errorMessage = String.format(
                "Can't retrieve node type for contained node with id suffix %d, attempting to read ordinary (native) node: %s",
                m_nodeId.get(), e.getMessage());
            return "NativeNode";

//            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static String loadCustomDescription(final ConfigBaseRO parentSettings, final ConfigBaseRO settings,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        try {
            if (loadVersion.isOlderThan(LoadVersion.V200)) {
                if (!parentSettings.containsKey("customDescription")) {
                    return null;
                }
                return parentSettings.getString("customDescription");
            } else {
                // custom description was not saved in v2.5.0 (but again in v2.5.1)
                // see bug 3034
                if (!settings.containsKey("customDescription")) {
                    return null;
                }
                return settings.getString("customDescription");
            }
        } catch (InvalidSettingsException e) {
            // TODO Throw proper exception
            // var errorMessage = String.format("Can't load custom description for the node %d", m_nodeId.get());
            // throw new InvalidSettingsException(errorMessage, e);
        }
        return null;

    }

    private static NodeType loadNodeType(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V200)) {
            var factory = settings.getString("factory");
            if (ObsoleteMetaNodeFileWorkflowPersistor.OLD_META_NODES.contains(factory)) {
                return NodeType.METANODE;
            } else {
                return NodeType.NATIVE_NODE;
            }
        } else if (loadVersion.isOlderThan(LoadVersion.V2100Pre)) {
            return settings.getBoolean("node_is_meta") ? NodeType.METANODE : NodeType.NATIVE_NODE;
        } else {
            final var nodeTypeString = settings.getString("node_type");
            CheckUtils.checkSettingNotNull(nodeTypeString, "node type must not be null");
            try {
                return NodeType.valueOf(nodeTypeString);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Can't parse node type: " + nodeTypeString);
            }
        }
    }

    private static NodeUIInfoDef loadUIInfo(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            return new NodeUIInfoDefBuilder() //
                .setBounds(loadBoundsDef(settings)) //
                .setHasAbsoluteCoordinates(null) //
                .setSymbolRelative(null) //
                .build();
        } catch (InvalidSettingsException e) {
            // var errorMessage = String.format("Error meessage for the ui info", null);
            // throw new InvalidSettingsException(errorMessage, e);

        }
        return new NodeUIInfoDefBuilder().build();
    }

    private static BoundsDef loadBoundsDef(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            var bounds = settings.getIntArray("extrainfo.node.bounds");
            return new BoundsDefBuilder() //
                .setLocation(CoreToDefUtil.createCoordinate(bounds[0], bounds[1])) //
                .setHeight(bounds[2]) //
                .setWidth(bounds[3]) //
                .build();
        } catch (Exception e) {
            // TODO throw proper exception
        }
        return new BoundsDefBuilder().build();
    }

    private String loadUIInfoClassName(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        try {
            if (loadVersion.isOlderThan(LoadVersion.V200)) {
                if (settings.containsKey(WorkflowPersistor.KEY_UI_INFORMATION)) {
                    return settings.getString(WorkflowPersistor.KEY_UI_INFORMATION);
                }
            } else {
                if (settings.containsKey("ui_classname")) {
                    return settings.getString("ui_classname");
                }
            }
        } catch (InvalidSettingsException e) {
            var errorMessage = String.format(
                "Unable to load UI information class name to node with ID suffix %s, no UI information available: %s",
                m_nodeId.get(), e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
        return null;
    }

    /**
     * @return the nodeBuilder
     */
    NodeDefBuilder getNodeBuilder() {
        return m_nodeBuilder;
    }

    NodeDef getNodeDef() {
        return m_nodeBuilder.build();
    }

}