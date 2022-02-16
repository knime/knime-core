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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.util.LoadVersion;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.NativeNodeDef;
import org.knime.core.workflow.def.VendorDef;
import org.knime.core.workflow.def.impl.NativeNodeDefBuilder;
import org.knime.core.workflow.def.impl.VendorDefBuilder;

/**
 *
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
public class NativeNodeLoader extends SingleNodeLoader {

    private static final String NODE_NAME_KEY = "node-name";

    private static final String NODE_BUNDLE_NAME_KEY = "node-bundle-name";

    private static final String NODE_BUNDLE_SYMBOLIC_NAME_KEY = "node-bundle-symbolic-name";

    private static final String NODE_BUNDLE_VENDOR_KEY = "node-bundle-vendor";

    private static final String NODE_BUNDLE_VERSION_KEY = "node-bundle-version";

    private static final String NODE_FEATURE_NAME_KEY = "node-feature-name";

    private static final String NODE_FEATURE_SYMBOLIC_NAME_KEY = "node-feature-symbolic-name";

    private static final String NODE_FEATURE_VENDOR_KEY = "node-feature-vendor";

    private static final String NODE_FEATURE_VERSION_KEY = "node-feature-version";

    private static final String NODE_CREATION_CONFIG_KEY = "node_creation_config";

    private static final String FACTORY_KEY = "factory";

    private static final String FACTORY_SETTINGS_KEY = "factory_settings";

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
            NodeLogger.getLogger(NodeAndBundleInformationPersistor.class).coding(e.getMessage(), e);
        }
        EXTENSION_RENAME_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Constructor
     */
    public NativeNodeLoader() {
        super(new NativeNodeDefBuilder());
    }

    /**
     * @param parentSettings
     * @param settings
     * @param loadVersion
     * @throws InvalidSettingsException
     */
    @Override
    public NativeNodeLoader load(final ConfigBaseRO parentSettings, final ConfigBaseRO settings, final LoadVersion loadVersion)
        throws InvalidSettingsException {
        super.load(parentSettings, settings, loadVersion);

        // The load method should throw specific error messages
        getNodeBuilder().setFactory(loadFactory(parentSettings, settings, loadVersion)) //
            .setFactorySettings(loadFactorySettings(settings)) //
            .setNodeName(settings.getString(NODE_NAME_KEY)) //
            .setBundle(loadBundle(settings)) //
            .setFeature(loadFeature(settings)) //
            .setNodeCreationConfig(loadCreationConfig(settings));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    NativeNodeDefBuilder getNodeBuilder() {
        return (NativeNodeDefBuilder)super.getNodeBuilder();
    }

    private static ConfigMapDef loadFactorySettings(final ConfigBaseRO settings) throws InvalidSettingsException {
        var factorySettings =
            settings.containsKey(FACTORY_SETTINGS_KEY) ? settings.getConfigBase(FACTORY_SETTINGS_KEY) : null;

        return CoreToDefUtil.toConfigMapDef(factorySettings);
    }

    private static ConfigMapDef loadCreationConfig(final ConfigBaseRO settings) throws InvalidSettingsException {
        var nodeCreationSettings =
            settings.containsKey(NODE_CREATION_CONFIG_KEY) ? settings.getConfigBase(NODE_CREATION_CONFIG_KEY) : null;
        return CoreToDefUtil.toConfigMapDef(nodeCreationSettings);
    }

    private static String loadFactory(final ConfigBaseRO parentSettings, final ConfigBaseRO settings,
        final LoadVersion loadVersion) throws InvalidSettingsException {
        if (loadVersion.isOlderThan(LoadVersion.V200)) {
            var factoryName = parentSettings.getString(FACTORY_KEY);
            // This is a hack to load old J48 Nodes Model from pre-2.0 workflows
            if ("org.knime.ext.weka.j48_2.WEKAJ48NodeFactory2".equals(factoryName)
                || "org.knime.ext.weka.j48.WEKAJ48NodeFactory".equals(factoryName)) {
                factoryName = "org.knime.ext.weka.knimeJ48.KnimeJ48NodeFactory";
            }
            return factoryName;
        } else {
            return settings.getString(FACTORY_KEY);
        }

    }

    private static VendorDef loadBundle(final ConfigBaseRO settings) throws InvalidSettingsException {
        return new VendorDefBuilder() //
            .setName(settings.getString(NODE_BUNDLE_NAME_KEY, "")) //
            .setSymbolicName(fixExtensionName(settings.getString(NODE_BUNDLE_SYMBOLIC_NAME_KEY))) //
            .setVendor(settings.getString(NODE_BUNDLE_VENDOR_KEY, "")) //
            .setVersion(settings.getString(NODE_BUNDLE_VERSION_KEY, "")) //
            .build();
    }

    private static VendorDef loadFeature(final ConfigBaseRO settings) throws InvalidSettingsException {
        return new VendorDefBuilder() //
            .setName(settings.getString(NODE_FEATURE_NAME_KEY, "")) //
            .setSymbolicName(fixExtensionName(settings.getString(NODE_FEATURE_SYMBOLIC_NAME_KEY))) //
            .setVendor(settings.getString(NODE_FEATURE_VENDOR_KEY, "")) //
            .setVersion(settings.getString(NODE_FEATURE_VERSION_KEY, "")) //
            .build();
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

    /**
     * {@inheritDoc}
     */
    @Override
    NativeNodeDef getNodeDef() {
        return getNodeBuilder().build();
    }
}
