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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.knime.core.node.dialog.DialogNode;
import org.knime.core.node.wizard.ViewHideable;
import org.knime.core.node.wizard.WizardNode;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.js.core.layout.bs.JSONLayoutContent;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * A JSON representation of the nodes for the visual layout editor.
 *
 * @author Alison Walter, KNIME.com GmbH, Konstanz, Germany
 */
@JsonAutoDetect
public class JSONVisualLayoutEditorNodes {

    private List<JSONNode> m_nodes;

    /**
     * @param viewNodes a {@code Map} of {@link NodeIDSuffix} to available {@link WizardNode}s
     * @param layouts a {@code Map} of {@link NodeIDSuffix} to {@link JSONLayoutContent}
     * @param subNodeContainer the container node
     * @param wfManager the current workflow manager
     */
    public JSONVisualLayoutEditorNodes(final Map<NodeIDSuffix, ViewHideable> viewNodes,
        final Map<NodeIDSuffix, JSONLayoutContent> layouts, final SubNodeContainer subNodeContainer,
        final WorkflowManager wfManager) {
        if (viewNodes == null || viewNodes.isEmpty() || subNodeContainer == null || wfManager == null) {
            return;
        }
        m_nodes = new ArrayList<>();
        for (final Entry<NodeIDSuffix, ViewHideable> viewNode : viewNodes.entrySet()) {
            final ViewHideable node = viewNode.getValue();
            final NodeID nodeID = viewNode.getKey().prependParent(subNodeContainer.getWorkflowManager().getID());
            final NodeContainer nodeContainer = wfManager.getNodeContainer(nodeID);
            final JSONNode jsonNode = new JSONNode(nodeContainer.getID().getIndex(), nodeContainer.getName(),
                nodeContainer.getCustomDescription(), layouts.get(viewNode.getKey()), getIcon(nodeContainer),
                node.isHideInWizard(), getType(node));
            m_nodes.add(jsonNode);
        }
    }

    /**
     * @return the nodes
     */
    public List<JSONNode> getNodes() {
        return m_nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(final List<JSONNode> nodes) {
        m_nodes = nodes;
    }

    // -- Helper methods--

    private static String getType(final ViewHideable node) {
        final boolean isWizardNode = node instanceof WizardNode;
        if (isWizardNode) {
            if (node instanceof DialogNode) {
                return "quickform";
            }
            return "view";
        }
        if (node instanceof SubNodeContainer) {
            return "nestedLayout";
        }
        throw new IllegalArgumentException("Node is not view, subnode, or quickform: " + node.getClass());
    }

    private static String getIcon(final NodeContainer nodeContainer) {
        if (nodeContainer == null) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png"));
        }
        String iconBase64 = "";
        if (nodeContainer instanceof SubNodeContainer) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout_16.png"));
        }
        try {
            final URL url = FileLocator.resolve(nodeContainer.getIcon());
            final String mimeType = URLConnection.guessContentTypeFromName(url.getFile());
            byte[] imageBytes = null;
            try (InputStream s = url.openStream()) {
                imageBytes = IOUtils.toByteArray(s);
            }
            iconBase64 = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (final IOException e) {
            // Do nothing
        }

        if (iconBase64.isEmpty()) {
            return createIcon(ImageRepository.getIconImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/layout/missing.png"));
        }
        return iconBase64;
    }

    private static String createIcon(final Image i) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[]{i.getImageData()};
        loader.save(out, SWT.IMAGE_PNG);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
