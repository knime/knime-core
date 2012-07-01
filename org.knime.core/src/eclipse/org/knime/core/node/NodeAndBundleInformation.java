/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 29, 2012 (wiswedel): created
 */
package org.knime.core.node;

import java.util.Dictionary;

import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.node.workflow.WorkflowPersistorVersion200.LoadVersion;
import org.osgi.framework.Bundle;

/** Information object to a node. Contains bundle information and node name.
 * Used in persistor to store extra information.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.6
 */
public final class NodeAndBundleInformation {

    private final String m_bundleSymbolicName;
    private final String m_bundleName;
    private final String m_bundleVendor;
    private final String m_nodeName;
    private final String m_factoryClass;

    /** @param node .. */
    NodeAndBundleInformation(final Node node) {
        final Class<? extends NodeFactory> facClass = node.getFactory().getClass();
        Bundle bundle = OSGIHelper.getBundle(facClass);
        if (bundle != null) {
            Dictionary<String, String> headers = bundle.getHeaders();
            m_bundleSymbolicName = bundle.getSymbolicName();
            m_bundleName = headers.get("Bundle-Name");
            m_bundleVendor = headers.get("Bundle-Vendor");
        } else {
            m_bundleSymbolicName = null;
            m_bundleName = null;
            m_bundleVendor = null;
        }
        m_nodeName = node.getName();
        m_factoryClass = facClass.getName();
    }

    /** Init from string array. All args except the factoryClass may be null.
     * @param factoryClass
     * @param bundleSymbolicName
     * @param bundleName
     * @param bundleVendor
     * @param nodeName
     * @noreference */
    public NodeAndBundleInformation(final String factoryClass,
            final String bundleSymbolicName, final String bundleName,
            final String bundleVendor, final String nodeName) {
        if (factoryClass == null) {
            throw new NullPointerException("factory class must not be null");
        }
        m_bundleSymbolicName = bundleSymbolicName;
        m_bundleName = bundleName;
        m_bundleVendor = bundleVendor;
        m_nodeName = nodeName;
        m_factoryClass = factoryClass;
    }



    /** @return the name */
    public String getBundleName() {
        return m_bundleName;
    }

    /** @return the symbolicName */
    public String getBundleSymbolicName() {
        return m_bundleSymbolicName;
    }

    /** @return the vendor */
    public String getBundleVendor() {
        return m_bundleVendor;
    }

    /** @return the nodeName */
    public String getNodeName() {
        return m_nodeName;
    }

    /** @return the factoryClass, never null.  */
    public String getFactoryClass() {
        return m_factoryClass;
    }

    /** Called by persistor.
     * @param settings ...
     * @noreference This method is not intended to be referenced by clients. */
    public void save(final NodeSettingsWO settings) {
        settings.addString("factory", getFactoryClass());
        // new in 2.6
        settings.addString("node-name", getNodeName());
        settings.addString("node-bundle-name", getBundleName());
        settings.addString("node-bundle-symbolic-name", getBundleSymbolicName());
        settings.addString("node-bundle-vendor", getBundleVendor());
    }

    /** Restores, used in persistor.
     * @param settings ...
     * @param version ...
     * @return bundle info object, at least the factory name will not be null.
     * @throws InvalidSettingsException ...
     * @noreference This method is not intended to be referenced by clients. */
    public static NodeAndBundleInformation load(final NodeSettingsRO settings,
            final LoadVersion version) throws InvalidSettingsException {
        String factoryClass = settings.getString("factory");
        if (factoryClass == null) {
            throw new InvalidSettingsException("Factory class is null");
        }
        String bundleSymbolicName;
        String bundleName;
        String bundleVendor;
        String nodeName;
        if (version.ordinal() < LoadVersion.V260.ordinal()) {
            nodeName = null;
            bundleName = null;
            bundleVendor = null;
            bundleSymbolicName = null;
        } else {
            nodeName = settings.getString("node-name");
            bundleName = settings.getString("node-bundle-name");
            bundleSymbolicName = settings.getString("node-bundle-symbolic-name");
            bundleVendor = settings.getString("node-bundle-vendor");
        }
        return new NodeAndBundleInformation(factoryClass, bundleSymbolicName,
                bundleName, bundleVendor, nodeName);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("class \"%s\" [node name: \"%s\", "
                + "bundle-symbolic-name: %s, bundle-name %s, "
                + "bundle-vendor %s]", getFactoryClass(), getNodeName(),
                getBundleSymbolicName(), getBundleName(), getBundleVendor());
    }

    /**
     * @return ..
     * @noreference This method is not intended to be referenced by clients.
     */
    public String getErrorMessageWhenNodeIsMissing() {
        StringBuilder b = new StringBuilder();
        if (m_nodeName != null) {
            b.append("Node \"").append(m_nodeName).append("\" not available");
        } else {
            b.append("Unable to load factory class \"");
            b.append(m_factoryClass);
            b.append("\"");
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