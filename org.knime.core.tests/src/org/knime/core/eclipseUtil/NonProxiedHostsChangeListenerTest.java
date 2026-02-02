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
 *   Feb 2, 2026 (lw): created
 */
package org.knime.core.eclipseUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.internal.net.ProxyData;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.knime.core.util.proxy.ExcludedHostsTokenizer;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tests the {@link NonProxiedHostsChangeListener}.
 */
@SuppressWarnings("restriction")
final class NonProxiedHostsChangeListenerTest {

    private ServiceTracker<IProxyService, IProxyService> m_tracker;

    @AfterEach
    void cleanupTracker() {
        if (m_tracker != null) {
            m_tracker.close();
            m_tracker = null;
        }
    }

    @SuppressWarnings("static-method")
    @Test
    void testCompatibilityLayer() throws CoreException {
        // retrieve the Eclipse proxy support via OSGi services
        final var bundle = FrameworkUtil.getBundle(NonProxiedHostsChangeListener.class);
        m_tracker = new ServiceTracker<IProxyService, IProxyService>(bundle.getBundleContext(), //
                "org.eclipse.core.net.proxy.IProxyService", null);
        m_tracker.open();
        final var service = m_tracker.getService();
        assertNotNull(service, "Proxy service must not be null");

        // query and store current configuration
        final var isEnabled = service.isProxiesEnabled();
        final var isSystemEnabled = service.isSystemProxiesEnabled();
        final var proxyData = service.getProxyData();
        final var nonProxiedHosts = service.getNonProxiedHosts();
        try {
            final var newProxies = new IProxyData[]{
                new ProxyData(IProxyData.HTTPS_PROXY_TYPE, "foo", 1234, false, null)
            };
            final var newNonProxiedHosts = ArrayUtils.add(nonProxiedHosts, "bar");

            service.setProxiesEnabled(true);
            service.setSystemProxiesEnabled(false);
            service.setProxyData(newProxies);
            service.setNonProxiedHosts(newNonProxiedHosts);

            final var value = System.getProperty(NonProxiedHostsChangeListener.HTTP_NON_PROXY_HOSTS);
            assertArrayEquals(ExcludedHostsTokenizer.tokenize(value), newNonProxiedHosts, //
                "Non-proxied-hosts change listener should have synced to '%s'"
                    .formatted(NonProxiedHostsChangeListener.HTTP_NON_PROXY_HOSTS));
        } finally {
            // lastly set original configuration
            service.setProxiesEnabled(isEnabled);
            service.setSystemProxiesEnabled(isSystemEnabled);
            service.setProxyData(proxyData);
            service.setNonProxiedHosts(nonProxiedHosts);
        }
    }
}
