/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

import java.util.Dictionary;
import java.util.Optional;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.node.workflow.FileWorkflowPersistor;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Information object to a node. Contains bundle information and node name. Used in persistor to store extra
 * information.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public final class NodeAndBundleInformation {
    private final String m_featureSymbolicName;

    private final String m_featureName;

    private final String m_featureVendor;

    private final Version m_featureVersion;

    private final String m_bundleSymbolicName;

    private final String m_bundleName;

    private final String m_bundleVendor;

    private final String m_nodeName;

    private final String m_factoryClass;

    private final Version m_bundleVersion;

    /**
     * Create a new information object based on the given node.
     *
     * @param node any node, must not be <code>null</code>
     * @since 2.10
     */
    public NodeAndBundleInformation(final Node node) {
        this(node.getFactory(), node.getName());
    }

    /**
     * Create a new information object based on the given node factory.
     *
     * @param factory a node factory, must not be <code>null</code>
     * @since 3.0
     */
    public NodeAndBundleInformation(final NodeFactory<? extends NodeModel> factory) {
        this(factory, null);
    }

    private NodeAndBundleInformation(final NodeFactory<? extends NodeModel> factory, final String nodeName) {
        @SuppressWarnings("rawtypes")
        final Class<? extends NodeFactory> facClass = factory.getClass();
        Bundle bundle = OSGIHelper.getBundle(facClass);
        if (bundle != null) {
            Dictionary<String, String> headers = bundle.getHeaders();
            m_bundleSymbolicName = bundle.getSymbolicName();
            m_bundleName = headers.get("Bundle-Name");
            m_bundleVendor = headers.get("Bundle-Vendor");
            m_bundleVersion = bundle.getVersion();

            Optional<IInstallableUnit> feature = OSGIHelper.getFeature(bundle);
            m_featureName = feature.map(f -> f.getProperty(IInstallableUnit.PROP_NAME, null)).orElse(null);
            m_featureSymbolicName = feature.map(f -> f.getId()).orElse(null);
            m_featureVendor = feature.map(f -> f.getProperty(IInstallableUnit.PROP_PROVIDER, null)).orElse(null);
            m_featureVersion = feature.map(f -> new Version(f.getVersion().toString())).orElse(null);
        } else {
            m_bundleSymbolicName = null;
            m_bundleName = null;
            m_bundleVendor = null;
            m_bundleVersion = null;

            m_featureName = null;
            m_featureSymbolicName = null;
            m_featureVendor = null;
            m_featureVersion = null;
        }
        m_nodeName = nodeName;
        m_factoryClass = facClass.getName();
    }

    /**
     * Create a new almost empty information object that only contains the factory class name.
     *
     * @param factoryClass non-<code>null</code> factory class name
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public NodeAndBundleInformation(final String factoryClass) {
        this(factoryClass, null, null, null, null, null, null, null, null, null);
    }

    /** Init from string array. All args except the factoryClass may be null. */
    private NodeAndBundleInformation(final String factoryClass, final String bundleSymbolicName,
        final String bundleName, final String bundleVendor, final String nodeName, final Version bundleVersion,
        final String featureSymbolicName, final String featureName, final String featureVendor,
        final Version featureVersion) {
        if (factoryClass == null) {
            throw new NullPointerException("factory class must not be null");
        }
        m_bundleSymbolicName = bundleSymbolicName;
        m_bundleName = bundleName;
        m_bundleVendor = bundleVendor;
        m_nodeName = nodeName;
        m_bundleVersion = bundleVersion;
        m_featureName = featureName;
        m_featureSymbolicName = featureSymbolicName;
        m_featureVendor = featureVendor;
        m_featureVersion = featureVersion;
        m_factoryClass = factoryClass;
    }

    /**
     * Returns the bundle's name in which the node is contained. If the bundle is unknown an empty result is returned.
     *
     * @return the bundle's name
     * @since 3.0
     */
    public Optional<String> getBundleName() {
        return Optional.ofNullable(m_bundleName);
    }

    /**
     * Returns the bundle's symbolic name in which the node is contained. If the bundle is unknown an empty result is
     * returned.
     *
     * @return the bundle's symbolic name
     * @since 3.0
     */
    public Optional<String> getBundleSymbolicName() {
        return Optional.ofNullable(m_bundleSymbolicName);
    }

    /**
     * Returns the bundle's vendor in which the node is contained. If the bundle is unknown an empty result is returned.
     *
     * @return the bundle's vendor
     * @since 3.0
     */
    public Optional<String> getBundleVendor() {
        return Optional.ofNullable(m_bundleVendor);
    }

    /**
     * Returns the bundle's version in which the node is contained. If the bundle is unknown an empty result is returned.
     *
     * @return the bundle's version
     * @since 3.0
     */
    public Optional<Version> getBundleVersion() {
        return Optional.ofNullable(m_bundleVersion);
    }

    /**
     * Returns a non-null string identifying the node. It's either the node's name or the node factory's name.
     *
     * @return non-<code>null</code> node name
     * @since 2.7
     */
    public String getNodeNameNotNull() {
        if (m_nodeName != null) {
            return m_nodeName;
        }
        int dotIndex = m_factoryClass.lastIndexOf('.');
        String name = m_factoryClass.substring(dotIndex + 1);
        if (name.length() > "NodeFactory".length() && name.endsWith("NodeFactory")) {
            name = name.substring(0, name.length() - "NodeFactory".length());
        } else if (name.length() > "Factory".length() && name.endsWith("Factory")) {
            name = name.substring(0, name.length() - "Factory".length());
        }
        return name;
    }

    /**
     * Returns the node's name. If the name is unknown an empty result is returned.
     *
     * @return the node's name
     * @since 3.0
     */
    public Optional<String> getNodeName() {
        return Optional.ofNullable(m_nodeName);
    }

    /**
     * Returns the node's factory class name.
     *
     * @return the factory class, never <code>null</code>
     */
    public String getFactoryClass() {
        return m_factoryClass;
    }

    /**
     * Returns the features's name in which the node is contained. If the feature is unknown an empty result is returned.
     *
     * @return the feature's name
     * @since 3.0
     */
    public Optional<String> getFeatureName() {
        return Optional.ofNullable(m_featureName);
    }

    /**
     * Returns the features's symbolic name in which the node is contained. If the feature is unknown an empty result is returned.
     *
     * @return the feature's symbolic name
     * @since 3.0
     */
    public Optional<String> getFeatureSymbolicName() {
        return Optional.ofNullable(m_featureSymbolicName);
    }

    /**
     * Returns the features' vendor in which the node is contained. If the feature is unknown an empty result is returned.
     *
     * @return the feature's vendor
     * @since 3.0
     */
    public Optional<String> getFeatureVendor() {
        return Optional.ofNullable(m_featureVendor);
    }

    /**
     * Returns the features's version in which the node is contained. If the feature is unknown an empty result is returned.
     *
     * @return the feature's version
     * @since 3.0
     */
    public Optional<Version> getFeatureVersion() {
        return Optional.ofNullable(m_featureVersion);
    }

    /**
     * Saves the information into the given node settings object. Called by persistor.
     *
     * @param settings a node settings object
     * @noreference This method is not intended to be referenced by clients.
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString("factory", getFactoryClass());
        // new in 2.6
        settings.addString("node-name", getNodeName().orElse(null));
        settings.addString("node-bundle-name", getBundleName().orElse(null));
        settings.addString("node-bundle-symbolic-name", getBundleSymbolicName().orElse(null));
        settings.addString("node-bundle-vendor", getBundleVendor().orElse(null));
        // new in 2.10
        final Version bundleVersion = getBundleVersion().orElse(Version.emptyVersion);
        settings.addString("node-bundle-version", bundleVersion.toString());

        // new in 3.0
        settings.addString("node-feature-name", getFeatureName().orElse(null));
        settings.addString("node-feature-symbolic-name", getFeatureSymbolicName().orElse(null));
        settings.addString("node-feature-vendor", getFeatureVendor().orElse(null));
        final Version featureVersion = getFeatureVersion().orElse(Version.emptyVersion);
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
    public static NodeAndBundleInformation load(final NodeSettingsRO settings,
        final FileWorkflowPersistor.LoadVersion version) throws InvalidSettingsException {
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

        if (version.ordinal() < FileWorkflowPersistor.LoadVersion.V260.ordinal()) {
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
            bundleSymbolicName = settings.getString("node-bundle-symbolic-name");
            bundleVendor = settings.getString("node-bundle-vendor");

            featureSymbolicName = settings.getString("node-feature-symbolic-name", null);
            featureName = settings.getString("node-feature-name", null);
            featureVendor = settings.getString("node-feature-vendor", null);

            String v = settings.getString("node-feature-version", "");
            try {
                if (!v.isEmpty()) {
                    featureVersion = Version.parseVersion(v);
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
                bundleVersion = Version.parseVersion(v);
            } else {
                bundleVersion = null;
            }
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Invalid version \"" + v + "\"", iae);
        }

        return new NodeAndBundleInformation(factoryClass, bundleSymbolicName, bundleName, bundleVendor, nodeName,
            bundleVersion, featureSymbolicName, featureName, featureVendor, featureVersion);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format(
            "class \"%s\" [node name: \"%s\", " + "bundle-symbolic-name: %s, bundle-name: %s, "
                + "bundle-vendor: %s, feature-symbolic-name: %s, feature-name: %s,"
                + "feature-vendor: %s, feature-version: %s]",
            getFactoryClass(), getNodeName(), getBundleSymbolicName(), getBundleName(), getBundleVendor(),
            getFeatureSymbolicName().orElse("?"), getFeatureName().orElse("?"), getFeatureVendor().orElse("?"),
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
        if (m_nodeName != null) {
            b.append("Node \"").append(m_nodeName).append("\" not available");
        } else {
            b.append("Unable to load factory class \"");
            b.append(m_factoryClass);
            b.append("\"");
        }
        if (m_featureName != null) {
            b.append(" from extension \"").append(m_featureName).append("\"");
        }
        if (m_bundleVendor != null) {
            b.append(" (provided by \"").append(m_bundleVendor).append("\"");
            if (m_bundleSymbolicName != null) {
                b.append("; plugin \"").append(m_bundleSymbolicName);
                b.append("\"");
                if (OSGIHelper.getBundle(m_bundleSymbolicName) != null) {
                    b.append(" is installed");
                } else {
                    b.append(" is not installed");
                }
            }
            b.append(")");
        }
        return b.toString();
    }
}
