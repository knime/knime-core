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
 *   Sep 28, 2021 (hornm): created
 */
package org.knime.gateway.api.entity;

import java.util.List;
import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.webui.node.NNCWrapper;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.SNCWrapper;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.core.webui.page.PageUtil.PageType;

/**
 * Node view entity containing the info required by the UI (i.e. frontend) to be able display a node view.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class NodeViewEnt extends NodeUIExtensionEnt<NodeWrapper<? extends NodeContainer>> {

    private final NodeInfoEnt m_info;

    private List<String> m_initialSelection;

    /**
     * @param nw the node to create the node view entity for
     * @param initialSelection the initial selection (e.g. a list of row keys or something else), supplied lazily (will
     *            not be called, if the node is not executed)
     * @return a new instance
     */
    public static NodeViewEnt create(final NodeWrapper<? extends NodeContainer> nw, final Supplier<List<String>> initialSelection) {
        if (nw.get().getNodeContainerState().isExecuted() && nw instanceof NNCWrapper) {
            try {
                NodeViewManager.getInstance().updateNodeViewSettings((NativeNodeContainer)nw.get());
                return new NodeViewEnt(nw, initialSelection, NodeViewManager.getInstance(), null);
            } catch (InvalidSettingsException ex) {
                NodeLogger.getLogger(NodeViewEnt.class).error("Failed to update node view settings", ex);
                return new NodeViewEnt(nw, null, null, ex.getMessage());
            }
        } else {
            return new NodeViewEnt(nw, null, null, null);
        }
    }

    /**
     * Creates a new instances without a initial selection and without the underlying node being registered with the
     * selection event source.
     *
     * @param nnc the node to create the node view entity for
     * @return a new instance
     */
    public static NodeViewEnt create(final NativeNodeContainer nnc) {
        return create(NNCWrapper.of(nnc), null);
    }

    /**
     * Creates a new instances without a initial selection and without the underlying node being registered with the
     * selection event source.
     *
     * @param snc the sub node container to create the node view entity for
     * @return a new instance
     */
    public static NodeViewEnt create(final SubNodeContainer snc) {
        return create(SNCWrapper.of(snc), null);
    }

    private NodeViewEnt(final NodeWrapper<? extends NodeContainer> nw, final Supplier<List<String>> initialSelection,
        final NodeViewManager nodeViewManager, final String customErrorMessage) {
        super(nw, nodeViewManager, nodeViewManager, PageType.VIEW);
        CheckUtils.checkArgument(NodeViewManager.hasNodeView(nw.get()), "The provided node doesn't have a node view");
        m_initialSelection = initialSelection == null ? null : initialSelection.get();
        m_info = new NodeInfoEnt(nw.get(), customErrorMessage);
    }

    /**
     * @return additional info for the node providing the view
     */
    public NodeInfoEnt getNodeInfo() {
        return m_info;
    }

    /**
     * @return the initial selection (e.g. a list of row keys)
     */
    public List<String> getInitialSelection() {
        return m_initialSelection;
    }

}
