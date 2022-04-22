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
 *   Aug 24, 2021 (hornm): created
 */
package org.knime.core.webui.node.view;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.DataServiceProvider;
import org.knime.core.webui.data.rpc.json.impl.ObjectMapperUtil;
import org.knime.core.webui.node.AbstractNodeUIManager;
import org.knime.core.webui.node.util.NodeCleanUpCallback;
import org.knime.core.webui.node.view.selection.SelectionTranslationService;
import org.knime.core.webui.node.view.selection.TextSelectionTranslationService;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.PageUtil.PageType;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Manages (web-ui) node view instances and provides associated functionality.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 *
 * @since 4.5
 */
public final class NodeViewManager extends AbstractNodeUIManager {

    private static NodeViewManager instance;

    private final Map<NodeContainer, NodeView> m_nodeViewMap = new WeakHashMap<>();

    private final Map<NodeContainer, SelectionTranslationService> m_selectionServices = new WeakHashMap<>();

    /**
     * Returns the singleton instance for this class.
     *
     * @return the singleton instance
     */
    public static synchronized NodeViewManager getInstance() {
        if (instance == null) {
            instance = new NodeViewManager();
        }
        return instance;
    }

    private NodeViewManager() {
        // singleton
    }

    /**
     * @param nc the node container to check
     * @return whether the node container provides a {@link NodeView}
     */
    public static boolean hasNodeView(final NodeContainer nc) {
        return nc instanceof NativeNodeContainer
            && ((NativeNodeContainer)nc).getNode().getFactory() instanceof NodeViewFactory;
    }

    /**
     * Gets the {@link NodeView} for given node container or creates it if it hasn't been created, yet.
     *
     * @param nc the node container create the node view from
     * @return a new node view instance
     * @throws IllegalArgumentException if the passed node container does not provide a node view
     */
    public NodeView getNodeView(final NodeContainer nc) {
        if (!hasNodeView(nc)) {
            throw new IllegalArgumentException("The node " + nc.getNameWithID() + " doesn't provide a node view");
        }
        var nnc = (NativeNodeContainer)nc;
        var nodeView = m_nodeViewMap.get(nnc);
        if (nodeView != null) {
            return nodeView;
        }
        return createAndRegisterNodeView(nnc);
    }

    /**
     * Helper to call the {@link TextSelectionTranslationService}.
     *
     * @param nc the node to call the data service for
     * @param request the selection request representing the selection to apply
     * @return the result of the translation, i.e., an array of row keys
     * @throws IOException if applying the selection failed
     */
    public List<String> callTextSelectionTranslationService(final NodeContainer nc, final String request)
        throws IOException {
        var service =
            getSelectionTranslationService(nc).filter(TextSelectionTranslationService.class::isInstance).orElse(null);
        if (service != null) {
            return ((TextSelectionTranslationService)service).translate(request);
        } else {
            return ObjectMapperUtil.getInstance().getObjectMapper().readValue(request,
                new TypeReference<List<String>>() {
                });
        }
    }

    private Optional<SelectionTranslationService> getSelectionTranslationService(final NodeContainer nc) {
        return Optional.ofNullable(m_selectionServices.computeIfAbsent(nc,
            k -> getNodeView(nc).createSelectionTranslationService().orElse(null)));
    }

    /**
     * Updates the view settings of a already created node view (i.e. a node view that has already been requested via
     * {@link #getNodeView(NodeContainer)} at least once).
     *
     * Updating the view settings means to get the current node settings from the node and provide them to the view (via
     * {@link NodeView#loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)}.
     *
     * NOTE: The settings (values) being passed to the node view are already combined with upstream flow variables (in
     * case settings are overwritten by flow variables).
     *
     * @param nc the node container to update the node view for
     * @throws InvalidSettingsException if settings couldn't be updated
     * @throws IllegalArgumentException if the passed node container does not provide a node view
     */
    public void updateNodeViewSettings(final NodeContainer nc) throws InvalidSettingsException {
        var nodeView = getNodeView(nc);
        var viewSettings = ((NativeNodeContainer)nc).getViewSettingsUsingFlowObjectStack();
        if (viewSettings.isPresent()) {
            nodeView.loadValidatedSettingsFrom(viewSettings.get());
        }
    }

    private NodeView createAndRegisterNodeView(final NativeNodeContainer nnc) {
        @SuppressWarnings("unchecked")
        NodeViewFactory<NodeModel> fac = (NodeViewFactory<NodeModel>)nnc.getNode().getFactory();
        NodeContext.pushContext(nnc);
        try {
            var nodeView = fac.createNodeView(nnc.getNodeModel());
            registerNodeView(nnc, nodeView);
            return nodeView;
        } finally {
            NodeContext.removeLastContext();
        }
    }

    private void registerNodeView(final NativeNodeContainer nnc, final NodeView nodeView) {
        if (m_nodeViewMap.put(nnc, nodeView) == null) {
            new NodeCleanUpCallback(nnc, () -> m_nodeViewMap.remove(nnc));
        }
    }

    /**
     * For testing purposes only.
     */
    void clearCaches() {
        m_nodeViewMap.clear();
        m_selectionServices.clear();
        clearPageMap();
    }

    /**
     * For testing purposes only.
     *
     * @return
     */
    int getNodeViewMapSize() {
        return m_nodeViewMap.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataServiceProvider getDataServiceProvider(final NodeContainer nc) {
        return getNodeView(nc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page getPage(final NativeNodeContainer nnc) {
        return getNodeView(nnc).getPage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PageType getPageType() {
        return PageType.VIEW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final int getPageMapSize() {
        return super.getPageMapSize();
    }

}
