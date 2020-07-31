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
 *   Jul 28, 2020 (hornm): created
 */
package org.knime.core.node.rpc;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Utility class that collects the {@link RpcTransportFactory} registered via an extension point. The extension is
 * provided by org.knime.core.ui.
 *
 * @author Martin Horn, KNIME AG, Zurich, Switzerland
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
final class RpcTransportRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(RpcTransportRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.RpcTransportFactory";

    /**
     * The cached instance, as collected by {@link #collectRpcMessageTransportFactory()}.
     */
    private static RpcTransportFactory m_rpcTransportFactory;

    private RpcTransportRegistry() {
        // utility
    }

    /**
     * Returns a delegate to org.knime.core.ui NodeContainerUI, which in turn is implemented by the gateway's
     * AbstractEntityProxyNodeContainer which uses the node service to send a remote procedure call.
     *
     * @return the implementation provided by org.knime.core.ui that is used to send remote procedure calls to a node
     *         model on a remote machine.
     */
    static RpcTransportFactory getRpcMessageTransportFactory() {
        if(m_rpcTransportFactory == null) {
            m_rpcTransportFactory = collectRpcMessageTransportFactory();
        }
        return m_rpcTransportFactory;
    }

    /**
     *
     * @return the factory provided by org.knime.core.ui (delegating message transport to NodeContainerUI#doRpc)
     */
    private static RpcTransportFactory collectRpcMessageTransportFactory() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

        // find all extensions implementing the RpcTransportFactory extension point
        List<RpcTransportFactory> factoryList = Stream.of(point.getExtensions())
                .flatMap(ext -> Stream.of(ext.getConfigurationElements()))
                .map(RpcTransportRegistry::readFactory)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (factoryList.isEmpty() || factoryList.size() > 1) {
            throw new IllegalStateException(
                String.format("%s factories registered via RpcTransportFactory extension point, should be exactly 1.\n"
                    + "Factory list: %s", factoryList.size(), factoryList));
        }
        return factoryList.get(0);
    }

    /**
     * Extracts the {@link RpcTransportFactory} from an eclipse extension.
     * @param cfe an eclipse extension
     * @return null, if the extension does not implement the {@link RpcTransportFactory} extension point, the factory
     *         instance provided by the extension point otherwise.
     */
    private static RpcTransportFactory readFactory(final IConfigurationElement cfe) {
        try {
            RpcTransportFactory ext = (RpcTransportFactory)cfe.createExecutableExtension("factoryClass");
            LOGGER.debugWithFormat("Added RpcTransportFactory '%s' from '%s'", ext.getClass().getName(),
                cfe.getContributor().getName());
            return ext;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Looking for an implementation of the RpcTransportFactory extension point,\n"
                + "but could not process extension %s: %s", cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

}
