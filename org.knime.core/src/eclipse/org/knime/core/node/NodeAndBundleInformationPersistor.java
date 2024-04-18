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
 *
 * History
 *   Jun 29, 2012 (wiswedel): created
 */
package org.knime.core.node;

import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Version;
import org.knime.core.util.workflowalizer.NodeAndBundleInformation;
import org.knime.shared.workflow.def.VendorDef;
import org.osgi.framework.Bundle;

/**
 * Information object to a node. Contains bundle information and node name. Used in persistor to store extra
 * information.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 3.7
 */
public final class NodeAndBundleInformationPersistor extends NodeAndBundleInformation
    implements KNIMEComponentInformation {

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
            map.put(Pattern.compile("^com\\.knime\\.features\\.bigdata\\.feature\\.group"),
                "org.knime.features.bigdata.connectors.feature.group");
            map.put(Pattern.compile("^com\\.knime\\.features\\.bigdata\\.spark1_3\\.feature\\.group"),
                    "org.knime.features.bigdata.spark.feature.group");
            map.put(Pattern.compile("^com\\.knime\\.features\\.bigdata(.+)"), "org.knime.features.bigdata$1");
            map.put(Pattern.compile("^com\\.knime\\.bigdata(.+)"), "org.knime.bigdata$1");
            map.put(Pattern.compile("^org\\.knime\\.features\\.testing\\.core\\.feature\\.group"),
                "org.knime.features.testing.application.feature.group");
            map.put(Pattern.compile("^com\\.knime\\.features\\.personalproductivity(.+)"),
                "org.knime.features.personalproductivity$1");
            map.put(Pattern.compile("^com\\.knime\\.explorer\\.nodes"), "org.knime.explorer.nodes");
        } catch (PatternSyntaxException e) {
            map.clear(); // if one fails, all fail
            NodeLogger.getLogger(NodeAndBundleInformationPersistor.class).coding(e.getMessage(), e);
        }
        EXTENSION_RENAME_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Create a new almost empty information object that only contains the factory class name.
     *
     * @param factoryClass non-<code>null</code> factory class name
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public NodeAndBundleInformationPersistor(final String factoryClass) {
        this(factoryClass, null, null, null, null, null, null, null, null, null);
    }

    /** Init from string array. All args except the factoryClass may be null. */
    private NodeAndBundleInformationPersistor(final String factoryClass, final String bundleSymbolicName,
        final String bundleName, final String bundleVendor, final String nodeName, final Version bundleVersion,
        final String featureSymbolicName, final String featureName, final String featureVendor,
        final Version featureVersion) {
        super(Optional.ofNullable(factoryClass), Optional.ofNullable(bundleSymbolicName),
            Optional.ofNullable(bundleName), Optional.ofNullable(bundleVendor), Optional.ofNullable(nodeName),
            Optional.ofNullable(bundleVersion), Optional.ofNullable(featureSymbolicName),
            Optional.ofNullable(featureName), Optional.ofNullable(featureVendor), Optional.ofNullable(featureVersion));
        if (factoryClass == null) {
            throw new NullPointerException("factory class must not be null");
        }
    }

    /**
     * Returns a non-null string identifying the node. It's either the node's name or the node factory's name.
     *
     * @return non-<code>null</code> node name
     * @since 2.7
     */
    public String getNodeNameNotNull() {
        if (getNodeName().isPresent()) {
            return getNodeName().get();
        }
        final String factoryClass = getFactoryClassNotNull();
        int dotIndex = factoryClass.lastIndexOf('.');
        String name = factoryClass.substring(dotIndex + 1);
        if (name.length() > "NodeFactory".length() && name.endsWith("NodeFactory")) {
            name = name.substring(0, name.length() - "NodeFactory".length());
        } else if (name.length() > "Factory".length() && name.endsWith("Factory")) {
            name = name.substring(0, name.length() - "Factory".length());
        }
        return name;
    }

    /** The value as per {@link #getNodeNameNotNull()}.
     * {@inheritDoc}
     */
    @Override
    public String getComponentName() {
        return getNodeNameNotNull();
    }

    /**
     * Returns the node's factory class name.
     *
     * @return the factory class, never <code>null</code>
     */
    public String getFactoryClassNotNull() {
        if (getFactoryClass() == null || !getFactoryClass().isPresent()) {
            throw new IllegalStateException("Factory class is null");
        }
        return getFactoryClass().get();
    }

    /**
     * Saves the information into the given node settings object. Called by persistor.
     *
     * @param settings a node settings object
     * @noreference This method is not intended to be referenced by clients.
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString("factory", getFactoryClassNotNull());
        // new in 2.6
        settings.addString("node-name", getNodeName().orElse(null));
        settings.addString("node-bundle-name", getBundleName().orElse(null));
        settings.addString("node-bundle-symbolic-name", getBundleSymbolicName().orElse(null));
        settings.addString("node-bundle-vendor", getBundleVendor().orElse(null));
        // new in 2.10
        final Version bundleVersion = getBundleVersion().orElse(Version.EMPTY_VERSION);
        settings.addString("node-bundle-version", bundleVersion.toString());

        // new in 3.0
        settings.addString("node-feature-name", getFeatureName().orElse(null));
        settings.addString("node-feature-symbolic-name", getFeatureSymbolicName().orElse(null));
        settings.addString("node-feature-vendor", getFeatureVendor().orElse(null));
        final Version featureVersion = getFeatureVersion().orElse(Version.EMPTY_VERSION);
        settings.addString("node-feature-version", featureVersion.toString());
    }

    /**
     * Restores the information object from the given node settings object. Used in persistor.
     *
     * @param settings a node settings object
     * @param version the workflow version
     * @return bundle info object, at least the factory name will not be <code>null</code>
     * @throws InvalidSettingsException if the node settings contain invalid entries
     * @noreference This method is not intended to be referenced by clients.
     */
    public static NodeAndBundleInformationPersistor load(final NodeSettingsRO settings,
        final LoadVersion version) throws InvalidSettingsException {
        String factoryClass = settings.getString("factory");
        if (factoryClass == null) {
            throw new InvalidSettingsException("Factory class is null");
        }
        String bundleSymbolicName;
        String bundleName;
        String bundleVendor;
        String nodeName;
        Version bundleVersion;

        String featureSymbolicName;
        String featureName;
        String featureVendor;
        Version featureVersion;

        if (version.ordinal() < LoadVersion.V260.ordinal()) {
            nodeName = null;
            bundleName = null;
            bundleVendor = null;
            bundleSymbolicName = null;
            featureSymbolicName = null;
            featureName = null;
            featureVendor = null;
            featureVersion = null;
        } else {
            nodeName = settings.getString("node-name");
            bundleName = settings.getString("node-bundle-name");
            bundleSymbolicName = fixExtensionName(settings.getString("node-bundle-symbolic-name"));
            bundleVendor = settings.getString("node-bundle-vendor");

            featureSymbolicName = fixExtensionName(settings.getString("node-feature-symbolic-name", null));
            featureName = settings.getString("node-feature-name", null);
            featureVendor = settings.getString("node-feature-vendor", null);

            String v = settings.getString("node-feature-version", "");
            try {
                if (!v.isEmpty()) {
                    featureVersion = new Version(v);
                } else {
                    featureVersion = null;
                }
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid feature version \"" + v + "\"", iae);
            }
        }

        String v = settings.getString("node-bundle-version", "");
        try {
            if (!v.isEmpty()) {
                bundleVersion = new Version(v);
            } else {
                bundleVersion = null;
            }
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Invalid version \"" + v + "\"", iae);
        }

        return new NodeAndBundleInformationPersistor(factoryClass, bundleSymbolicName, bundleName, bundleVendor, nodeName,
            bundleVersion, featureSymbolicName, featureName, featureVendor, featureVersion);
    }

    public static NodeAndBundleInformationPersistor load(final String nodeName, final VendorDef feature,
        final VendorDef bundle, final String factoryName) {
        return new NodeAndBundleInformationPersistor(factoryName, bundle.getSymbolicName(), bundle.getName(),
            bundle.getVendor(), nodeName, new Version(bundle.getVersion()), feature.getSymbolicName(),
            feature.getName(), feature.getVendor(), new Version(feature.getVersion()));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format(
            "class \"%s\" [node name: \"%s\", " + "bundle-symbolic-name: %s, bundle-name: %s, "
                + "bundle-vendor: %s, feature-symbolic-name: %s, feature-name: %s,"
                + "feature-vendor: %s, feature-version: %s]",
            getFactoryClass(), getNodeName().orElse("?"), getBundleSymbolicName().orElse("?"),
            getBundleName().orElse("?"), getBundleVendor().orElse("?"), getFeatureSymbolicName().orElse("?"),
            getFeatureName().orElse("?"), getFeatureVendor().orElse("?"),
            getFeatureVersion().map(v -> v.toString()).orElse("?"));
    }

    /**
     * Returns an error message for reporting that a node implementation is missing. It will show the required
     * factory class and the feature and plug-in in which the node is expected to be.
     *
     * @return an error message
     * @noreference This method is not intended to be referenced by clients.
     */
    public String getErrorMessageWhenNodeIsMissing() {
        StringBuilder b = new StringBuilder(256);
        if (getNodeName().isPresent()) {
            b.append("Node \"").append(getNodeName().get()).append("\" not available");
        } else {
            b.append("Unable to load factory class \"");
            b.append(getFactoryClassNotNull());
            b.append("\"");
        }
        if (getFeatureName().isPresent()) {
            b.append(" from extension \"").append(getFeatureName().get()).append("\"");
        }
        if (getBundleVendor().isPresent()) {
            b.append(" (provided by \"").append(getBundleVendor().get()).append("\"");
            if (getBundleSymbolicName().isPresent()) {
                b.append("; plugin \"").append(getBundleSymbolicName().get());
                b.append("\"");
                if (OSGIHelper.getBundle(getBundleSymbolicName().get()) != null) {
                    b.append(" is installed");
                } else {
                    b.append(" is not installed");
                }
            }
            b.append(")");
        }
        return b.toString();
    }

    /**
     * @param node any node, must not be <code>null</code>
     * @return a new information object based on the given node.
     * @since 2.10
     */
    public static NodeAndBundleInformationPersistor create(final Node node) {
        return create(node.getFactory(), node.getName());
    }

    /**
     * @param factory a node factory, must not be <code>null</code>
     * @return a new information object based on the given node factory.
     * @since 3.0
     */
    public static NodeAndBundleInformationPersistor create(final NodeFactory<? extends NodeModel> factory) {
        return create(factory, null);
    }

    private static NodeAndBundleInformationPersistor create(final NodeFactory<? extends NodeModel> factory,
        final String nodeName) {
        @SuppressWarnings("rawtypes")
        final Class<? extends NodeFactory> facClass = factory.getClass();
        Bundle bundle = null;
        if (factory instanceof BundleNameProvider) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Optional<String> bundleName = ((BundleNameProvider)factory).getBundleName2();
            if (bundleName.isPresent()) {
                bundle = Platform.getBundle(bundleName.get());
            }
        }
        if (bundle == null) {
            bundle = OSGIHelper.getBundle(facClass);
        }

        return create(factory, nodeName, bundle);
    }

    /**
     * @param factory factory of a node, must not be <code>null</code>
     * @param nodeName the node's name, can be <code>null</code>
     * @param bundle the bundle to extract the information from, can be <code>null</code>
     * @return a new information object based on the provided information
     * @since 4.1
    */
    public static NodeAndBundleInformationPersistor create(final NodeFactory<? extends NodeModel> factory,
        final String nodeName, final Bundle bundle) {
        String bundleSymbolicName = null;
        String bundleName = null;
        String bundleVendor = null;
        Version bundleVersion = null;
        String featureSymbolicName = null;
        String featureName = null;
        String featureVendor = null;
        Version featureVersion = null;

        if (bundle != null) {
            Dictionary<String, String> headers = bundle.getHeaders();
            bundleSymbolicName = bundle.getSymbolicName();
            bundleName = headers.get("Bundle-Name");
            bundleVendor = headers.get("Bundle-Vendor");
            bundleVersion = bundle.getVersion() == null ? null : new Version(bundle.getVersion().toString());

            Optional<IInstallableUnit> feature = OSGIHelper.getFeature(bundle);
            featureName = feature.map(f -> f.getProperty(IInstallableUnit.PROP_NAME, null)).orElse(null);
            featureSymbolicName = feature.map(f -> f.getId()).orElse(null);
            featureVendor = feature.map(f -> f.getProperty(IInstallableUnit.PROP_PROVIDER, null)).orElse(null);
            featureVersion = feature.map(f -> new Version(f.getVersion().toString())).orElse(null);
        }
        return new NodeAndBundleInformationPersistor(factory.getClass().getName(), bundleSymbolicName, bundleName,
            bundleVendor, nodeName, bundleVersion, featureSymbolicName, featureName, featureVendor, featureVersion);
    }

    /** If feature or bundle name matches any in {@link #EXTENSION_RENAME_MAP} the string is modified so that
     * it matches the new namespace. Otherwise the argument is returned (also includes null case).
     *
     * @param extName The name of the feature or bundle, e.g. com.knime.bigdata.foo.bar
     * @return the possibly modified name, e.g. org.knime.bigdata.foo.bar
     */
    private static String fixExtensionName(final String extName) {
        if (extName == null) {
            return null;
        }
        for (Map.Entry<Pattern, String> entry : EXTENSION_RENAME_MAP.entrySet()) {
            Pattern p = entry.getKey();
            Matcher matcher = p.matcher(extName);
            if (matcher.matches()) {
                return matcher.replaceAll(entry.getValue());
            }
        }
        return extName;
    }
}
