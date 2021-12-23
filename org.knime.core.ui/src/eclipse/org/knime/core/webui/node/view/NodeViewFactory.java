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
 *   Aug 31, 2021 (hornm): created
 */
package org.knime.core.webui.node.view;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.wizard.page.WizardPageContribution;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;

/**
 * Implemented by {@link NodeFactory}s to register a node view.
 *
 * Pending API - needs to be integrated with {@link NodeFactory} eventually.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <T> the node model this node view will have access to
 *
 * @since 4.5
 */
public interface NodeViewFactory<T extends NodeModel> extends WizardPageContribution {

    /**
     * Creates a new node view instance. It is guaranteed that a {@link NodeContext} is available when the method is
     * called.
     *
     * @param nodeModel the node model to create the view for
     * @return a new node view instance
     */
    // TODO node view instances should only be created once per node instance?
    // ... and, e.g., call 'getPage' repeatedly on node state change (in case of dynamic pages only)
    NodeView createNodeView(T nodeModel);

    /**
     * {@inheritDoc}
     */
    @Override
    default Optional<String> validateViewValue(final NativeNodeContainer nnc, final String value) throws IOException {
        var nodeView = NodeViewManager.getInstance().getNodeView(nnc);
        var service = nodeView.getApplyDataService().orElse(null);
        if (service instanceof TextReExecuteDataService) {
            return ((TextReExecuteDataService)service).validateData(value);
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void loadViewValue(final NativeNodeContainer nnc, final String value) throws IOException {
        var nodeView = NodeViewManager.getInstance().getNodeView(nnc);
        var service = nodeView.getApplyDataService().orElse(null);
        if (service instanceof TextReExecuteDataService) {
            ((TextReExecuteDataService)service).applyData(value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Optional<String> getInitialViewValue(final NativeNodeContainer nnc) {
        var nodeView = NodeViewManager.getInstance().getNodeView(nnc);
        InitialDataService service = nodeView.getInitialDataService().orElse(null);
        if (service instanceof TextInitialDataService) {
            return Optional.of(((TextInitialDataService)service).getInitialData());
        }
        return Optional.empty();
    }
}
