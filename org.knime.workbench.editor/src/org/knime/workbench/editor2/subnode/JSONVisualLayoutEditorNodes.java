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
 *   Aug 14, 2018 (awalter): created
 */
package org.knime.workbench.editor2.subnode;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.wizard.ViewHideable;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.layout.LayoutTemplateProvider;
import org.knime.js.core.layout.bs.JSONLayoutViewContent;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A JSON representation of the nodes for the visual layout editor.
 *
 * @author Alison Walter, KNIME.com GmbH, Konstanz, Germany
 */
@JsonAutoDetect
public class JSONVisualLayoutEditorNodes {

    // Map node ID index (as a String for Jackson JSON translation) to JSONNode representation
    private Map<String, JSONNode> m_nodes;

    private long m_length;

    /**
     * @param viewNodes a {@code Map} of {@link NodeIDSuffix} to available {@link WizardNode}s
     * @param subNodeContainer the container node
     * @param wfManager the current workflow manager
     */
    public JSONVisualLayoutEditorNodes(final Map<NodeIDSuffix, ViewHideable> viewNodes,
        final SubNodeContainer subNodeContainer, final WorkflowManager wfManager) {
        if ((viewNodes == null) || viewNodes.isEmpty() || (subNodeContainer == null) || (wfManager == null)) {
            return;
        }

        long maxId = Long.MIN_VALUE;
        m_nodes = new HashMap<>();
        for (final Entry<NodeIDSuffix, ViewHideable> viewNode : viewNodes.entrySet()) {
            final ViewHideable node = viewNode.getValue();
            final NodeID nodeID = viewNode.getKey().prependParent(subNodeContainer.getWorkflowManager().getID());
            final NodeContainer nodeContainer = wfManager.getNodeContainer(nodeID);

            final int nodeIdIndex = nodeContainer.getID().getIndex();
            final String nodeIdIndexString = nodeIdIndex + "";
            final JSONNode jsonNode = new JSONNode(nodeContainer.getName(), nodeContainer.getCustomDescription(),
                getTemplate(node), getIcon(nodeContainer), node.isHideInWizard());
            m_nodes.put(nodeIdIndexString, jsonNode);

            if (nodeIdIndex > maxId) {
                maxId = nodeIdIndex;
            }
        }
        m_length = maxId + 1;
    }

    /**
     * @return the nodes
     */
    public Map<String, JSONNode> getNodes() {
        return m_nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(final Map<String, JSONNode> nodes) {
        m_nodes = nodes;
    }

    /**
     * The largest nodeID index + 1. This is needed by the javascript to convert the map into a keyed array.
     *
     * @return the length
     */
    @JsonIgnore
    public long getLength() {
        return m_length;
    }

    /**
     * @param length the length to set
     */
    @JsonIgnore
    public void setLength(final long length) {
        m_length = length;
    }

    // -- Helper methods--

    private static JSONLayoutViewContent getTemplate(final ViewHideable node) {
        if (node instanceof LayoutTemplateProvider) {
            return ((LayoutTemplateProvider)node).getLayoutTemplate();
        }
        return new JSONLayoutViewContent();
    }

    private static String getIcon(final NodeContainer nodeContainer) {
        URL iconURL = NodeFactory.class.getResource("default.png");
        try {
            iconURL = FileLocator.resolve(nodeContainer.getIcon());
        } catch (final IOException e) {
            // Do nothing
        }
        return iconURL.toString();
    }
}
