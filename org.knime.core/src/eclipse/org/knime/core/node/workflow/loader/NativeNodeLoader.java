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
 *   28 Jan 2022 (Dionysios Stolis): created
 */
package org.knime.core.node.workflow.loader;

import static org.knime.core.node.workflow.loader.LoaderUtils.DEFAULT_CONFIG_MAP;
import static org.knime.core.node.workflow.loader.LoaderUtils.DEFAULT_EMPTY_STRING;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.node.workflow.loader.LoaderUtils.Const;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.FilestoreDef;
import org.knime.core.workflow.def.VendorDef;
import org.knime.core.workflow.def.impl.FallibleNativeNodeDef;
import org.knime.core.workflow.def.impl.FilestoreDefBuilder;
import org.knime.core.workflow.def.impl.NativeNodeDefBuilder;
import org.knime.core.workflow.def.impl.VendorDefBuilder;
import org.knime.core.workflow.loader.FallibleSupplier;

/**
 * Loads the description of a Native Node. Native node are also referred as KNIME Nodes.
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
public final class NativeNodeLoader {

    private NativeNodeLoader() {
    }

    private static final VendorDef DEFAULT_VENDOR_DEF = new VendorDefBuilder().build();

    private static final FilestoreDef DEFAULT_FILE_STORE_DEF = new FilestoreDefBuilder().build();

    /**
     * Maps a regular expression to the a new extension name. For instance, after open sourcing the big data extensions
     * the namespace changed from "com.knime(.features).bigdata.()" to "org.knime(.features).bigdata.()."
     */
    private static final Map<Pattern, String> EXTENSION_RENAME_MAP;

    static {
        // key and value correspond to the two arguments in String#replaceAll
        // most specific matchers should be sorted in first

        Map<Pattern, String> map = new LinkedHashMap<>();
        try {
            map.put(Pattern.compile("^com\\.knime\\.features\\.bigdata\\.feature\\.group"), // NOSONAR
                "org.knime.features.bigdata.connectors.feature.group");
            map.put(Pattern.compile("^com\\.knime\\.features\\.bigdata\\.spark1_3\\.feature\\.group"), // NOSONAR
                "org.knime.features.bigdata.spark.feature.group");
            map.put(Pattern.compile("^com\\.knime\\.features\\.bigdata(.+)"), "org.knime.features.bigdata$1"); // NOSONAR
            map.put(Pattern.compile("^com\\.knime\\.bigdata(.+)"), "org.knime.bigdata$1"); // NOSONAR
            map.put(Pattern.compile("^com\\.knime\\.features\\.personalproductivity(.+)"), // NOSONAR
                "org.knime.features.personalproductivity$1");
            map.put(Pattern.compile("^com\\.knime\\.explorer\\.nodes"), "org.knime.explorer.nodes"); // NOSONAR
        } catch (PatternSyntaxException e) {
            map.clear(); // if one fails, all fail
            // TODO error handling
            //NodeLogger.getLogger(NodeAndBundleInformationPersistor.class).coding(e.getMessage(), e);
        }
        EXTENSION_RENAME_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Loads the properties of a native node into {@link FallibleNativeNodeDefl}, stores the loading exceptions using
     * the {@link FallibleSupplier}
     *
     * @param workflowConfig a read only representation of the workflow.knime
     * @param nodeDirectory a {@link File} of the node folder.
     * @param workflowFormatVersion an {@link LoadVersion}.
     * @return a {@link FallibleNativeNodeDef}
     * @throws IOException when the settings.xml can't be found.
     */
    static FallibleNativeNodeDef load(final ConfigBaseRO workflowConfig, final File nodeDirectory,
        final LoadVersion workflowFormatVersion) throws IOException {
        var nodeConfig = LoaderUtils.readNodeConfigFromFile(nodeDirectory);

        return new NativeNodeDefBuilder()//
            .setFactory(() -> loadFactory(workflowConfig, nodeConfig, workflowFormatVersion), DEFAULT_EMPTY_STRING) //
            .setFactorySettings(() -> loadFactorySettings(nodeConfig), DEFAULT_CONFIG_MAP) //
            .setNodeName(() -> nodeConfig.getString(Const.NODE_NAME_KEY.get()), DEFAULT_EMPTY_STRING) //
            .setBundle(() -> loadBundle(nodeConfig), DEFAULT_VENDOR_DEF) //
            .setFeature(() -> loadFeature(nodeConfig), DEFAULT_VENDOR_DEF) //
            .setNodeCreationConfig(() -> loadCreationConfig(nodeConfig), DEFAULT_CONFIG_MAP)
            .setFilestore(() -> loadFilestore(workflowConfig, workflowFormatVersion), DEFAULT_FILE_STORE_DEF)
            .setConfigurableNode(ConfigurableNodeLoader.load(workflowConfig, nodeConfig, workflowFormatVersion)) //
            .build();

    }

    /**
     * Loads the factory settings from the {@code settings}.
     *
     * @param settings a read only representation of the node's settings.xml.
     * @return a {@link ConfigMapDef} with the factory settings.
     * @throws InvalidSettingsException
     */
    private static ConfigMapDef loadFactorySettings(final ConfigBaseRO settings) throws InvalidSettingsException {
        var factorySettings = settings.containsKey(Const.FACTORY_SETTINGS_KEY.get())
            ? settings.getConfigBase(Const.FACTORY_SETTINGS_KEY.get()) : null;

        return CoreToDefUtil.toConfigMapDef(factorySettings);
    }

    /**
     * Loads the node creation configuration (e.g flow variable port object)
     *
     * @param settings a read only representation of the node's settings.xml.
     * @return a {@link ConfigMapDef} with the creation configs.
     * @throws InvalidSettingsException
     * @since 4.2
     */
    private static ConfigMapDef loadCreationConfig(final ConfigBaseRO settings) throws InvalidSettingsException {
        var nodeCreationSettings = settings.containsKey(Const.NODE_CREATION_CONFIG_KEY.get())
            ? settings.getConfigBase(Const.NODE_CREATION_CONFIG_KEY.get()) : null;
        return CoreToDefUtil.toConfigMapDef(nodeCreationSettings);
    }

    /**
     * Loads the factory name either from {@code workflowConfig} or {@code nodeConfig} according to the
     * {@code workflowFormatVersion}.
     *
     * @param workflowConfig a read only representation of the workflow's workflow.knime file.
     * @param nodeConfig a read only representation of the node's settings.xml file.
     * @param workflowFormatVersion a {@link LoadVersion}.
     * @return the node's factory name.
     * @throws InvalidSettingsException
     * @since 3.5
     */
    private static String loadFactory(final ConfigBaseRO workflowConfig, final ConfigBaseRO nodeConfig,
        final LoadVersion workflowFormatVersion) throws InvalidSettingsException {
        if (workflowFormatVersion.isOlderThan(LoadVersion.V200)) {
            var factoryName = workflowConfig.getString(Const.FACTORY_KEY.get());
            // This is a hack to load old J48 Nodes Model from pre-2.0 workflows
            if ("org.knime.ext.weka.j48_2.WEKAJ48NodeFactory2".equals(factoryName)
                || "org.knime.ext.weka.j48.WEKAJ48NodeFactory".equals(factoryName)) {
                factoryName = "org.knime.ext.weka.knimeJ48.KnimeJ48NodeFactory";
            }
            return factoryName;
        } else {
            return nodeConfig.getString(Const.FACTORY_KEY.get());
        }
    }

    /**
     * Loads the bundle information of a node from the {@code settings}.
     *
     * @param settings a read only representation of the node's settings.xml file.
     * @return a {@link VendorDef}
     */
    private static VendorDef loadBundle(final ConfigBaseRO settings) {
        return new VendorDefBuilder() //
            .setName(settings.getString(Const.NODE_BUNDLE_NAME_KEY.get(), DEFAULT_EMPTY_STRING)) //
            .setSymbolicName(
                fixExtensionName(settings.getString(Const.NODE_BUNDLE_SYMBOLIC_NAME_KEY.get(), DEFAULT_EMPTY_STRING))) //
            .setVendor(settings.getString(Const.NODE_BUNDLE_VENDOR_KEY.get(), DEFAULT_EMPTY_STRING)) //
            .setVersion(settings.getString(Const.NODE_BUNDLE_VERSION_KEY.get(), DEFAULT_EMPTY_STRING)) //
            .build();
    }

    /**
     * Loads the feature information of a node from the {@code settings}.
     *
     * @param settings a read only representation of the node's settings.xml file.
     * @return a {@link VendorDef}
     */
    private static VendorDef loadFeature(final ConfigBaseRO settings) {
        return new VendorDefBuilder() //
            .setName(settings.getString(Const.NODE_FEATURE_NAME_KEY.get(), DEFAULT_EMPTY_STRING)) //
            .setSymbolicName(
                fixExtensionName(settings.getString(Const.NODE_FEATURE_SYMBOLIC_NAME_KEY.get(), DEFAULT_EMPTY_STRING))) //
            .setVendor(settings.getString(Const.NODE_FEATURE_VENDOR_KEY.get(), DEFAULT_EMPTY_STRING)) //
            .setVersion(settings.getString(Const.NODE_FEATURE_VERSION_KEY.get(), DEFAULT_EMPTY_STRING)) //
            .build();
    }

    /**
     * Loads the filestores of a native node from the {@code settings}.
     *
     * @param settings a read only representation of the node's settings.xml file.
     * @param loadVersion a {@link LoadVersion}
     * @return a {@link FilestoreDef}
     * @throws InvalidSettingsException
     */
    private static FilestoreDef loadFilestore(final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V260) || !settings.containsKey(Const.FILESTORES_KEY.get())) {
            return DEFAULT_FILE_STORE_DEF;
        }
        var filestoreSettings = settings.getConfigBase(Const.FILESTORES_KEY.get());
        return new FilestoreDefBuilder()
            .setLocation(() -> filestoreSettings.getString(Const.FILESTORES_LOCATION_KEY.get()), DEFAULT_EMPTY_STRING)
            .setId(() -> filestoreSettings.getString(Const.FILESTORES_ID_KEY.get()), DEFAULT_EMPTY_STRING).build();
    }

    /**
     * If feature or bundle name matches any in {@link #EXTENSION_RENAME_MAP} the string is modified so that it matches
     * the new namespace. Otherwise the argument is returned (also includes null case).
     *
     * @param extName The name of the feature or bundle, e.g. com.knime.bigdata.foo.bar
     * @return the possibly modified name, e.g. org.knime.bigdata.foo.bar
     */
    private static String fixExtensionName(final String extName) {
        if (extName == null) {
            return null;
        }
        for (Map.Entry<Pattern, String> entry : EXTENSION_RENAME_MAP.entrySet()) {
            var p = entry.getKey();
            var matcher = p.matcher(extName);
            if (matcher.matches()) {
                return matcher.replaceAll(entry.getValue());
            }
        }
        return extName;
    }
}
