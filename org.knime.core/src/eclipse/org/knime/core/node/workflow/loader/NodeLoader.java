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

import java.io.IOException;
import java.util.Optional;
import java.util.Random;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
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
 * Responsible for loading the KNIME properties which are used by the Nodes, Components, Metanodes and Workflows
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

    /**
     * @param workflowConfig describes the containing workflow. Contains, e.g., the node's id, description, etc.
     * @param nodeDirectory contains the description of the node and possibly a nested workflow
     * @param workflowFormatVersion implicitly specifies the format of the descriptions
     * @throws IOException
     */
    static NodeDef load(final ConfigBaseRO workflowConfig, final ConfigBaseRO nodeConfig,
        final LoadVersion workflowFormatVersion) {

        return new NodeDefBuilder().setId(() -> loadNodeId(workflowConfig), getRandomNodeID()) //
            .setAnnotation(() -> loadAnnotation(workflowConfig, workflowFormatVersion), DEFAULT_NODE_ANNOTATION) //
            .setCustomDescription(() -> loadCustomDescription(workflowConfig, nodeConfig, workflowFormatVersion), "") //
            .setJobManager(() -> loadJobManager(nodeConfig), DEFAULT_JOB_MANAGER) //
            .setLocks(loadLocks(workflowConfig, workflowFormatVersion)) //
            .setUiInfo(loadUIInfo(workflowConfig, workflowFormatVersion)) //
            .build();
    }

    /**
     * Loads the node's that should be unique within the workflow
     *
     * @param settings a representation of the workflow.knime file.
     * @return a node id
     * @throws InvalidSettingsException
     */
    private static Integer loadNodeId(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            return Optional.ofNullable(settings.getInt("id")).orElse(getRandomNodeID());
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
     * Loads the annotations.
     *
     * @param settings a representation of the workflow.knime file.
     * @param loadVersion
     * @return
     * @throws InvalidSettingsException
     */
    private static NodeAnnotationDef loadAnnotation(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        if (!loadVersion.isOlderThan(LoadVersion.V230) || !settings.containsKey("annotations")) {
            // no credentials in v2.2 and before
            return DEFAULT_NODE_ANNOTATION;
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
                .setLocation(() -> loadCoordinateDef(settings), new CoordinateDefBuilder().build()) //
                .setBorderColor(settings.getInt("borderColor")) //
                .setWidth(settings.getInt("width")) //
                .setHeight(settings.getInt("height")) //
                .setTextAlignment(settings.getString("alignment")) //
                .setBorderSize(settings.getInt("borderSize")) //
                .setDefaultFontSize(settings.getInt("defFontSize")) //
                .setAnnotationVersion(settings.getInt("annotation-version")) //
                .build();
        } catch (InvalidSettingsException e) {
            var errorMessage = "Can't load node annotation: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    private static CoordinateDef loadCoordinateDef(final ConfigBaseRO settings) throws InvalidSettingsException {
        return CoreToDefUtil.createCoordinate(settings.getInt("x-coordinate"), settings.getInt("y-coordinate"));
    }

    /**
     *
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    private static JobManagerDef loadJobManager(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            if (!settings.containsKey("job.manager")) {
                return DEFAULT_JOB_MANAGER;
            }
            var jobManagerSettings = settings.getConfigBase("job.manager");
            return new JobManagerDefBuilder()//
                .setFactory(() -> jobManagerSettings.getString("job.manager.factory.id"), "")//
                .setSettings(CoreToDefUtil.toConfigMapDef(jobManagerSettings.getConfigBase("job.manager.settings")))
                .build();
        } catch (InvalidSettingsException e) { //NOSONAR
            var errorMessage = "Can't restore node execution job manager: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     *
     * @param settings
     * @param loadVersion
     * @return
     */
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

    /**
     *
     * @param parentSettings
     * @param settings
     * @param loadVersion
     * @return
     * @throws InvalidSettingsException
     */
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
            var errorMessage = "Invalid custom description in settings: " + e.getMessage();
            throw new InvalidSettingsException(errorMessage, e);
        }
    }

    /**
     *
     * @param settings
     * @return
     * @throws InvalidSettingsException
     */
    static NodeUIInfoDef loadUIInfo(final ConfigBaseRO settings, final LoadVersion loadVersion) {
        var symbolRelative = loadVersion.ordinal() >= LoadVersion.V230.ordinal();
        return new NodeUIInfoDefBuilder() //
            .setBounds(() -> loadBoundsDef(settings), new BoundsDefBuilder().build()) //
            //TODO What's the key of this? For the metanode loader we set it as null
            .setHasAbsoluteCoordinates(settings.getBoolean("absolute_coordinates", false)) //
            .setSymbolRelative(symbolRelative) //
            .build();
    }

    private static BoundsDef loadBoundsDef(final ConfigBaseRO settings) throws InvalidSettingsException {
        try {
            var bounds = settings.getIntArray("extrainfo.node.bounds");
            if (bounds.length == 0) {
                return new BoundsDefBuilder().build();
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