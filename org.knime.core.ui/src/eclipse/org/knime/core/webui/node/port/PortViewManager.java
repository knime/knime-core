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
 *   Jul 18, 2022 (hornm): created
 */
package org.knime.core.webui.node.port;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.DataServiceProvider;
import org.knime.core.webui.node.AbstractNodeUIManager;
import org.knime.core.webui.node.NodePortWrapper;
import org.knime.core.webui.node.util.NodeCleanUpCallback;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.PageUtil.PageType;

/**
 * Manages (web-ui) port view instances and provides associated functionality.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class PortViewManager extends AbstractNodeUIManager<NodePortWrapper> {

    private static PortViewManager instance;

    private static Map<PortType, PortViewFactory<?>> portViewFactoryMap = new HashMap<>();

    private final Map<NodePortWrapper, PortView> m_portViewMap = new WeakHashMap<>();

    /**
     * Allows one to register a {@link PortViewFactory} and consequently to associate a {@link PortView} with a certain
     * {@link PortType}.
     *
     * Pending API: most likely to be removed as soon as the port view API is integrated with the
     * {@link PortObject}/{@link PortType} API.
     *
     * @param portType
     * @param portViewFactory
     */
    public static void registerPortViewFactory(final PortType portType,
        final PortViewFactory<? extends PortObject> portViewFactory) {
        portViewFactoryMap.put(portType, portViewFactory);
    }

    /**
     * Returns the singleton instance for this class.
     *
     * @return the singleton instance
     */
    public static synchronized PortViewManager getInstance() {
        if (instance == null) {
            instance = new PortViewManager();
        }
        return instance;
    }

    /**
     * @param portType the port type to check
     * @return {@code true} if the given port type provides a {@link PortView}; otherwise {@code false}
     */
    public static boolean hasPortView(final PortType portType) {
        return portViewFactoryMap.containsKey(portType);
    }

    private PortViewManager() {
        // singleton
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page getPage(final NodePortWrapper nodePortWrapper) {
        return getPortView(nodePortWrapper).getPage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataServiceProvider getDataServiceProvider(final NodePortWrapper nodePortWrapper) {
        return getPortView(nodePortWrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PageType getPageType() {
        return PageType.PORT;
    }

    /**
     * Gets the {@link PortView} for the given combination of node container and port index. The port view will be
     * either retrieved from a cache or newly created if it hasn't been accessed, yet.
     *
     * @param nodePortWrapper
     * @return a (new) port view instance
     * @throws NoSuchElementException if there is no port view for the given node-port combination
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    PortView getPortView(final NodePortWrapper nodePortWrapper) {
        var portView = m_portViewMap.get(nodePortWrapper); // NOSONAR
        if (portView != null) {
            return portView;
        }
        var nc = nodePortWrapper.get();
        var portIdx = nodePortWrapper.getPortIdx();
        var outPort = nc.getOutPort(portIdx);
        var portType = outPort.getPortType();

        PortViewFactory factory = portViewFactoryMap.get(portType); // NOSONAR
        if (factory != null) {
            NodeContext.pushContext(nc);
            try {
                portView = factory.createPortView(outPort.getPortObject());
                m_portViewMap.put(nodePortWrapper, portView);
                NodeCleanUpCallback.builder(nc, () -> m_portViewMap.remove(nodePortWrapper))
                    .cleanUpOnNodeStateChange(true).build();
                return portView;
            } finally {
                NodeContext.removeLastContext();
            }
        } else {
            throw new NoSuchElementException("No port view available");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPageId(final NodePortWrapper w, final Page p) {
        return getPortView(w).getPageId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldCleanUpPageAndDataServicesOnNodeStateChange() {
        return true;
    }

    /**
     * For testing purposes only.
     */
    int getPortViewMapSize() {
        return m_portViewMap.size();
    }

}
