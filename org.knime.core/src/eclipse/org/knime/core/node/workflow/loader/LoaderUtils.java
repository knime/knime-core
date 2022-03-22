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
 *   9 Mar 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.loader;

import java.io.File;
import java.io.IOException;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.StyleRangeDef;
import org.knime.core.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.core.workflow.def.impl.ConfigMapDefBuilder;
import org.knime.core.workflow.def.impl.StyleRangeDefBuilder;

/**
 * //TODO We can add all the read from file methods for the files workflow.knime, settings.xml, template.knime.
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 */
final class LoaderUtils {

    private LoaderUtils() {}

    enum Const {
        /** @see NativeNodeLoader#load */
        NODE_NAME_KEY("node-name"),
        /** @see NativeNodeLoader#loadBundle */
        NODE_BUNDLE_NAME_KEY("node-bundle-name"), //
        NODE_BUNDLE_SYMBOLIC_NAME_KEY("node-bundle-symbolic-name"), //
        NODE_BUNDLE_VENDOR_KEY("node-bundle-vendor"), //
        NODE_BUNDLE_VERSION_KEY("node-bundle-version"),
        /** @see NativeNodeLoader#loadFeature */
        NODE_FEATURE_NAME_KEY("node-feature-name"), //
        NODE_FEATURE_SYMBOLIC_NAME_KEY("node-feature-symbolic-name"), //
        NODE_FEATURE_VENDOR_KEY("node-feature-vendor"), //
        NODE_FEATURE_VERSION_KEY("node-feature-version"),
        /** @see NativeNodeLoader#loadCreationConfig */
        NODE_CREATION_CONFIG_KEY("node_creation_config"),
        /** @see NativeNodeLoader#loadFactory */
        FACTORY_KEY("factory"),
        /** @see NativeNodeLoader#loadFactorySettings */
        FACTORY_SETTINGS_KEY("factory_settings"),
        /** @see NativeNodeLoader#loadFilestore */
        FILESTORES_KEY("filestores"),
        FILESTORES_LOCATION_KEY("file_store_location"),
        FILESTORES_ID_KEY("file_store_id"),

        /** @see MetaNodeLoader#loadInPorts */
        META_IN_PORTS_KEY("meta_in_ports"),
        /** @see MetaNodeLoader#loadOutPorts */
        META_OUT_PORTS_KEY("meta_out_ports"),
        /** @see MetaNodeLoader#loadPortsSettingsEnum */
        PORT_ENUM_KEY("port_enum"),
        /** @see MetaNodeLoader#loadNodeUIInformation */
        UI_SETTINGS_KEY("ui_settings"),

        /** @see MetaNodeLoader#loadPort */
        /** @see ComponentLoader#loadPort */
        PORT_INDEX_KEY("index"), //
        PORT_NAME_KEY("name"), //
        PORT_TYPE_KEY("type"), //
        PORT_OBJECT_CLASS_KEY("object_class"),

        /** @see ComponentLoader#loadMetadata */
        DESCRIPTION_KEY("description"), //
        METADATA_KEY("metadata"), //
        METADATA_NAME_KEY("name"), //
        INPORTS_KEY("inports"), //
        OUTPORTS_KEY("outports"),
        /** @see ComponentLoader#loadTemplateLink */
        WORKFLOW_TEMPLATE_INFORMATION_KEY("workflow_template_information"),
        /** @see ComponentLoader#loadVirtualInNodeId */
        VIRTUAL_IN_ID_KEY("virtual-in-ID"),
        /** @see ComponentLoader#loadVirtualInNodeId */
        VIRTUAL_OUT_ID_KEY("virtual-out-ID"),
        /** @see ComponentLoader#loadIcon */
        ICON_KEY("icon"),


        /** @see ConfigurableNodeLoader#loadInternalNodeSubSettings */
        INTERNAL_NODE_SUBSETTINGS("internal_node_subsettings"),
        /** @see ConfigurableNodeLoader#loadVariableSettings */
        VARIABLES_KEY("variables"),
        /** @see ConfigurableNodeLoader#loadModelSettings */
        MODEL_KEY("model"),
        /** @see ConfigurableNodeLoader#loadFlowStackObjects */
        SCOPE_STACK_KEY("scope_stack"),
        FLOW_STACK_KEY("flow_stack"),
        /** @see ConfigurableNodeLoader#loadFlowObjectDef */
        TYPE_KEY("type"),
        VARIABLE("variable"),
        /** @see ConfigurableNodeLoader#loadFlowContextDef */
        INACTIVE("_INACTIVE"),
        /** @see ConfigurableNodeLoader#loadFlowContextType */
        LOOP("LOOP"),
        FLOW("FLOW"),
        SCOPE("SCOPE"),

        /** @see NodeLoader#loadNodeId */
        ID_KEY("id"),
        /** @see NodeLoader#loadAnnotation */
        CUSTOM_NAME_KEY("customName"),  //
        NODE_ANNOTATION_KEY("nodeAnnotation"),
        /** @see NodeLoader#loadJobManager */
        JOB_MANAGER_KEY("job.manager"), //
        JOB_MANAGER_FACTORY_ID_KEY("job.manager.factory.id"),
        JOB_MANAGER_SETTINGS_KEY("job.manager.settings"),
        /** @see NodeLoader#loadLocks */
        IS_DELETABLE_KEY("isDeletable"), //
        HAS_RESET_LOCK_KEY("hasResetLock"), //
        HAS_CONFIGURE_LOCK_KEY("hasConfigureLock"),
        /** @see NodeLoader#loadCustomDescription */
        CUSTOM_DESCRIPTION_KEY("customDescription"),
        /** @see NodeLoader#loadBoundsDef */
        EXTRA_NODE_INFO_BOUNDS_KEY("extrainfo.node.bounds"),

        /** @see LoaderUtils#readWorkflowConfigFromFile */
        WORKFLOW_FILE_NAME("workflow.knime"),
        /** @see LoaderUtils#loadNodeFile */
        NODE_SETTINGS_FILE("node_settings_file"),
        /** @see LoaderUtils#readNodeConfigFromFile(File)*/
        NODE_SETTINGS_FILE_NAME("settings.xml");

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

    static final int DEFAULT_NEGATIVE_INDEX = -1;

    static final String DEFAULT_EMPTY_STRING = "";

    static final ConfigMapDef DEFAULT_CONFIG_MAP = new ConfigMapDefBuilder().build();

    static ConfigBaseRO readNodeConfigFromFile(final File nodeDirectory) throws IOException {
        var nodeSettingsFile = new File(nodeDirectory, Const.NODE_SETTINGS_FILE_NAME.get());
        try {
            return SimpleConfig.parseConfig(nodeSettingsFile.getAbsolutePath(), nodeSettingsFile);
        } catch (IOException e) {
            throw new IOException("Cannot load the " + Const.NODE_SETTINGS_FILE_NAME.get(), e);
        }
    }

    static ConfigBaseRO readWorkflowConfigFromFile(final File nodeDirectory) throws IOException {
        var workflowSettingsFile = new File(nodeDirectory, Const.WORKFLOW_FILE_NAME.get());
        try {
            return SimpleConfig.parseConfig(workflowSettingsFile.getAbsolutePath(), workflowSettingsFile);
        } catch (IOException e) {
            throw new IOException("Cannot load the " + Const.WORKFLOW_FILE_NAME.get(), e);
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
    static ConfigBaseRO parseWorkflowConfig(final File workflowDirectory) throws IOException {
        var workflowDotKnime = LoaderUtils.getWorkflowDotKnimeFile(workflowDirectory);
        return SimpleConfig.parseConfig(workflowDotKnime.getAbsolutePath(), workflowDotKnime);
    }


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
    static File loadNodeFile(final ConfigBaseRO workflowNodeConfig, final File workflowDir)
        throws InvalidSettingsException {
        // relative path to node configuration file
        var fileString = workflowNodeConfig.getString(Const.NODE_SETTINGS_FILE.get());
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
     * @param annotationConfig
     * @param workflowFormatVersion
     */
    static AnnotationDataDef loadAnnotationDef(final ConfigBaseRO annotationConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        var builder = new AnnotationDataDefBuilder()//
            .setText(annotationConfig.getString("text"))//
            .setBgcolor(annotationConfig.getInt("bgcolor"))//
            .setLocation(CoreToDefUtil.createCoordinate(annotationConfig.getInt("x-coordinate"),
                annotationConfig.getInt("y-coordinate")))//
            .setWidth(annotationConfig.getInt("width"))//
            .setHeight(annotationConfig.getInt("height"))//
            .setBorderSize(annotationConfig.getInt("borderSize", 0)) // default to 0 for backward compatibility
            .setBorderColor(annotationConfig.getInt("borderColor", 0)) // default for backward compatibility
            .setDefaultFontSize(annotationConfig.getInt("defFontSize", -1)) // default for backward compatibility
            .setAnnotationVersion(annotationConfig.getInt("annotation-version", -1)) // default to VERSION_OLD
            .setTextAlignment(workflowFormatVersion.ordinal() >= LoadVersion.V250.ordinal()
                ? annotationConfig.getString("alignment") : "LEFT");

        ConfigBaseRO styleConfigs = annotationConfig.getConfigBase("styles");
        for (String key : styleConfigs.keySet()) {
            builder.addToStyles(() -> loadStyleRangeDef(styleConfigs.getConfigBase(key)),
                new StyleRangeDefBuilder().build());
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
}
