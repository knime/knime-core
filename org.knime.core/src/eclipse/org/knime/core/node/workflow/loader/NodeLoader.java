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

import static org.knime.core.node.workflow.loader.LoaderUtils.DEFAULT_EMPTY_STRING;

import java.io.File;
import java.util.Optional;
import java.util.Random;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.node.workflow.loader.LoaderUtils.Const;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.BaseNodeDef;
import org.knime.core.workflow.def.BoundsDef;
import org.knime.core.workflow.def.JobManagerDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.NodeLocksDef;
import org.knime.core.workflow.def.NodeUIInfoDef;
import org.knime.core.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.core.workflow.def.impl.BaseNodeDefBuilder;
import org.knime.core.workflow.def.impl.BoundsDefBuilder;
import org.knime.core.workflow.def.impl.FallibleBaseNodeDef;
import org.knime.core.workflow.def.impl.FallibleBoundsDef;
import org.knime.core.workflow.def.impl.JobManagerDefBuilder;
import org.knime.core.workflow.def.impl.NodeAnnotationDefBuilder;
import org.knime.core.workflow.def.impl.NodeLocksDefBuilder;
import org.knime.core.workflow.def.impl.NodeUIInfoDefBuilder;
import org.knime.core.workflow.loader.FallibleSupplier;

/**
 * Loads the description of the Node into a {@link BaseNodeDef}. The BaseNodeDef is the bootstrap node of the
 * NativeNode, Component and MetaNode.
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
final class NodeLoader {

    private NodeLoader() {
    }

    private static final JobManagerDef DEFAULT_JOB_MANAGER = new JobManagerDefBuilder() //
        .setFactory("") //
        .build();

    private static final NodeAnnotationDef DEFAULT_NODE_ANNOTATION = new NodeAnnotationDefBuilder().build();

    private static final BoundsDef DEFAULT_BOUNDS = new BoundsDefBuilder().build();

    /**
     * Loads the properties of the Base node into {@link BaseNodeDef}, stores the loading exceptions using the
     * {@link FallibleSupplier}
     *
     * @param workflowConfig a read only representation of the workflow.knime
     * @param nodeDirectory a {@link File} of the node folder.
     * @param workflowFormatVersion an {@link LoadVersion}.
     * @return a {@link BaseNodeDef}
     */
    static FallibleBaseNodeDef load(final ConfigBaseRO workflowConfig, final ConfigBaseRO nodeConfig,
        final LoadVersion workflowFormatVersion) {

        return new BaseNodeDefBuilder() //
            .setId(() -> loadNodeId(workflowConfig), getRandomNodeID()) //
            .setAnnotation(() -> loadAnnotation(nodeConfig, workflowFormatVersion), DEFAULT_NODE_ANNOTATION) //
            .setCustomDescription(() -> loadCustomDescription(workflowConfig, nodeConfig, workflowFormatVersion),
                DEFAULT_EMPTY_STRING) //
            .setJobManager(() -> loadJobManager(nodeConfig), DEFAULT_JOB_MANAGER) //
            .setLocks(loadLocks(workflowConfig, workflowFormatVersion)) //
            .setUiInfo(loadUIInfo(workflowConfig, workflowFormatVersion)) //
            .build();
    }

    /**
     * Loads the node's id from the {@code settings}.
     *
     * @param settings a read only representation of the workflow.knime file.
     * @return an {@link Integer}
     * @throws InvalidSettingsException
     */
    private static Integer loadNodeId(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            return Optional.ofNullable(settings.getInt(Const.ID_KEY.get())).orElse(getRandomNodeID());
        } catch (InvalidSettingsException e) { //NOSONAR
            var errorMessage =
                String.format("Unable to load node ID (internal id \"%s\"), trying random number", settings.getKey());
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static Integer getRandomNodeID() {
        return 10000 + new Random().nextInt(10000);
    }

    /**
     * Loads the node annotation from the {@code nodeSettings}.
     *
     * @param nodeSettings a read only representation of the settings.xml file.
     * @param workflowFormatVersion a {@link LoadVersion}
     * @return a {@link NodeAnnotationDef}
     * @throws InvalidSettingsException
     */
    private static NodeAnnotationDef loadAnnotation(final ConfigBaseRO nodeSettings,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V250)) {
            var customName = nodeSettings.getString(Const.CUSTOM_NAME_KEY.get(), DEFAULT_EMPTY_STRING);
            var isDefault = customName == null;
            return new NodeAnnotationDefBuilder() //
                .setAnnotationDefault(isDefault) //
                .setData(new AnnotationDataDefBuilder() //
                    .setText(customName) //
                    .build()) //
                .build();
        } else {
            if (nodeSettings.containsKey(Const.NODE_ANNOTATION_KEY.get())) {
                var nodeAnnotationSettings = nodeSettings.getConfigBase(Const.NODE_ANNOTATION_KEY.get());
                var isDefault = nodeAnnotationSettings == null;
                return new NodeAnnotationDefBuilder() //
                    .setAnnotationDefault(isDefault) //
                    .setData(LoaderUtils.loadAnnotation(nodeAnnotationSettings, workflowFormatVersion)) //
                    .build();
            } else {
                return new NodeAnnotationDefBuilder().setAnnotationDefault(true).build();
            }
        }
    }

    /**
     * Loads the job manager settings from the {@code settings}.
     *
     * @param settings a read only representation of the node's settings.xml.
     * @return a {@link JobManagerDef}
     * @throws InvalidSettingsException
     */
    private static JobManagerDef loadJobManager(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            if (!settings.containsKey(Const.JOB_MANAGER_KEY.get())) {
                return DEFAULT_JOB_MANAGER;
            }
            var jobManagerSettings = settings.getConfigBase(Const.JOB_MANAGER_KEY.get());
            return new JobManagerDefBuilder()//
                .setFactory(jobManagerSettings.getString(Const.JOB_MANAGER_FACTORY_ID_KEY.get()))//
                .setSettings(
                    CoreToDefUtil.toConfigMapDef(jobManagerSettings.getConfigBase(Const.JOB_MANAGER_SETTINGS_KEY.get())))
                .build();
        } catch (InvalidSettingsException e) {
            var errorMessage = "Can't restore node execution job manager: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     * Loads the node locks from the {@code settings} according to the {@code workflowFormatVersion}.
     *
     * @param settings a read only representation of the node's settings.xml.
     * @param workflowFormatVersion an {@link LoadVersion}.
     * @return
     */
    private static NodeLocksDef loadLocks(final ConfigBaseRO settings, final LoadVersion workflowFormatVersion) {
        boolean isDeletable;
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            isDeletable = true;
        } else {
            isDeletable = settings.getBoolean(Const.IS_DELETABLE_KEY.get(), true);
        }
        boolean hasResetLock;
        if (workflowFormatVersion.isOlderThan(LoadVersion.V3010)) {
            hasResetLock = false;
        } else {
            hasResetLock = settings.getBoolean(Const.HAS_RESET_LOCK_KEY.get(), false);
        }
        boolean hasConfigureLock;
        if (workflowFormatVersion.isOlderThan(LoadVersion.V3010)) {
            hasConfigureLock = false;
        } else {
            hasConfigureLock = settings.getBoolean(Const.HAS_CONFIGURE_LOCK_KEY.get(), false);
        }

        return new NodeLocksDefBuilder() //
            .setHasConfigureLock(hasConfigureLock) //
            .setHasDeleteLock(isDeletable) //
            .setHasResetLock(hasResetLock) //
            .build();
    }

    /**
     * Loads the custom description either from {@code workflowConfig} or {@code settings} according to the
     * {@code workflowFormatVersion}
     *
     * @param workflowConfig a read only representation of the workflow.knime.
     * @param settings a read only representation of the settings.xml.
     * @param workflowFormatVersion an {@link LoadVersion}.
     * @return a {@link String}
     * @throws InvalidSettingsException
     */
    private static String loadCustomDescription(final ConfigBaseRO workflowConfig, final ConfigBaseRO settings,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            if (!workflowConfig.containsKey(Const.CUSTOM_DESCRIPTION_KEY.get())) {
                return DEFAULT_EMPTY_STRING;
            }
            return workflowConfig.getString(Const.CUSTOM_DESCRIPTION_KEY.get());
        } else {
            // custom description was not saved in v2.5.0 (but again in v2.5.1)
            // see bug 3034
            if (!settings.containsKey(Const.CUSTOM_DESCRIPTION_KEY.get())) {
                return DEFAULT_EMPTY_STRING;
            }
            return settings.getString(Const.CUSTOM_DESCRIPTION_KEY.get());
        }
    }

    /**
     * Loads the node's UI Info from the {@code settings}.
     *
     * @param settings a read only representation of the workflow.knime.
     * @param workflowFormatVersion an {@link LoadVersion}.
     * @return a {@link NodeUIInfoDef}
     */
    static NodeUIInfoDef loadUIInfo(final ConfigBaseRO settings, final LoadVersion workflowFormatVersion) {
        var symbolRelative = workflowFormatVersion.ordinal() >= LoadVersion.V230.ordinal();
        return new NodeUIInfoDefBuilder() //
            .setBounds(() -> loadBoundsDef(settings), DEFAULT_BOUNDS) //
            //TODO What's the key of this? For the metanode loader we set it as null
            .setHasAbsoluteCoordinates(settings.getBoolean("absolute_coordinates", false)) //
            .setSymbolRelative(symbolRelative) //
            .build();
    }

    private static FallibleBoundsDef loadBoundsDef(final ConfigBaseRO settings) throws InvalidSettingsException {
        if (!settings.containsKey(Const.EXTRA_NODE_INFO_BOUNDS_KEY.get())) {
            return (FallibleBoundsDef)DEFAULT_BOUNDS;
        }
        try {
            var bounds = settings.getIntArray(Const.EXTRA_NODE_INFO_BOUNDS_KEY.get());
            if (bounds.length == 0 || bounds.length < 4) {
                return (FallibleBoundsDef)DEFAULT_BOUNDS;
            }
            return new BoundsDefBuilder() //
                .setLocation(CoreToDefUtil.createCoordinate(bounds[0], bounds[1])) //
                .setHeight(bounds[2]) //
                .setWidth(bounds[3]) //
                .build();
        } catch (InvalidSettingsException e) {
            var errorMessage = String.format("Unable to load the UI Bounds for the node id %d: %s",
                settings.getInt("id"), e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     * TODO Not used?
     *
     * @param settings
     * @param loadVersion
     * @return
     * @throws InvalidSettingsException
     */
    private static String loadUIInfoClassName(final ConfigBaseRO settings, final LoadVersion loadVersion)
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
                settings.getInt("id"), e.getMessage());
            throw new InvalidSettingsException(errorMessage, e);
        }
        return null;
    }
}