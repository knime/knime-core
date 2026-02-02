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
 *   Jan 30, 2026 (lw): created
 */
package org.knime.core.eclipseUtil;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.internal.net.ProxyChangeEvent;
import org.eclipse.core.internal.net.ProxyManager;
import org.eclipse.core.internal.net.ProxyType;
import org.eclipse.core.net.proxy.IProxyChangeEvent;
import org.eclipse.core.net.proxy.IProxyChangeListener;
import org.eclipse.core.net.proxy.IProxyData;
import org.knime.core.node.NodeLogger;

/**
 * Compatibility layer for the Eclipse proxy support to comply with the Java networking properties.
 * <p>
 * See "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/doc-files/net-properties.html".
 * </p>
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class NonProxiedHostsChangeListener implements IProxyChangeListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NonProxiedHostsChangeListener.class);

    // package scope for tests
    static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";

    /**
     * Flag indicating whether {@link #installChangeListener()} has already been invoked.
     */
    private static boolean isInstalledGlobally;

    /**
     * Default, private constructor (only called to instantiate listener).
     */
    private NonProxiedHostsChangeListener(final ProxyManager manager) {
        final var initializeEvent = new ProxyChangeEvent(IProxyChangeEvent.NONPROXIED_HOSTS_CHANGED,
            /*Old, non-proxied hosts are not relevant.*/ null, //
            manager.getNonProxiedHosts(), //
            manager.getProxyData(), //
            /*No changes in proxy data.*/ null);

        this.proxyInfoChanged(initializeEvent);
    }

    /**
     * Sets this class as additional changed-listener. It will call {@link #proxyInfoChanged(IProxyChangeEvent)}
     * to set {@code "http.nonProxyHosts"} on whatever Eclipse has set {@code "https.nonProxyHosts"} to.
     * <p>
     * See "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/doc-files/net-properties.html".
     * </p>
     *
     * @noreference This method is not intended to be referenced by clients.
     */
    static synchronized void installChangeListener() {
        if (isInstalledGlobally) {
            return;
        }
        final var service = ProxyManager.getProxyManager();
        if (service instanceof ProxyManager manager) {
            manager.addProxyChangeListener(new NonProxiedHostsChangeListener(manager));
            isInstalledGlobally = true;
        } else {
            LOGGER.error(String.format("Could not install non-proxied-hosts change listener, " //
                + "could not retrieve the %s instance: %s", ProxyManager.class, service));
        }
    }

    private static boolean hasDataOfType(final IProxyData[] data, final String type) {
        if (data == null || type == null) {
            return false;
        }
        return Arrays.stream(data) //
            .filter(d -> StringUtils.isNotBlank(d.getHost())) //
            .anyMatch(d -> type.equalsIgnoreCase(d.getType()));
    }

    @Override
    public void proxyInfoChanged(final IProxyChangeEvent event) {
        final var nonProxiedHostsChangeWithHttps = event.getChangeType() == IProxyChangeEvent.NONPROXIED_HOSTS_CHANGED
            && hasDataOfType(event.getOldProxyData(), IProxyData.HTTPS_PROXY_TYPE);
        final var nonProxiedHostsWithHttpsChange = ArrayUtils.isNotEmpty(event.getNonProxiedHosts())
            && hasDataOfType(event.getChangedProxyData(), IProxyData.HTTPS_PROXY_TYPE);

        // The Eclipse proxy support (from within `org.eclipse.core.net`) incorrectly
        // sets non-proxied-hosts for HTTPS with the "https" prefix, instead of "http".
        if (nonProxiedHostsChangeWithHttps || nonProxiedHostsWithHttpsChange) {
            // Note that the non-proxied-hosts from the Eclipse proxy support apply to
            // all proxy protocols at once, no need to join with HTTP entries.
            System.setProperty(HTTP_NON_PROXY_HOSTS,
                ProxyType.convertHostsToPropertyString(event.getNonProxiedHosts()));
        }
    }
}
