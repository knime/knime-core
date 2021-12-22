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
 *  NodeDialog, and NodeDialog) and that only interoperate with KNIME through
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
 *   Oct 15, 2021 (hornm): created
 */
package org.knime.core.webui.node.dialog;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.webui.page.PageUtil;

/**
 * Manages (web-ui) node dialog instances and provides associated functionality.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public final class NodeDialogManager {

    private static final String NODE_DIALOG_DEBUG_PATTERN_PROP = "org.knime.ui.dev.node.dialog.url.factory-class";

    private static final String NODE_DIALOG_DEBUG_URL_PROP = "org.knime.ui.dev.node.dialog.url";

    private static NodeDialogManager instance;

    private final Map<NodeID, NodeDialog> m_nodeDialogMap = new HashMap<>();

    /**
     * Returns the singleton instance for this class.
     *
     * @return the singleton instance
     */
    public static synchronized NodeDialogManager getInstance() {
        if (instance == null) {
            instance = new NodeDialogManager();
        }
        return instance;
    }

    private NodeDialogManager() {
        // singleton
    }

    /**
     * @param nc the node to check
     * @return whether the node provides a {@link NodeDialog}
     */
    public static boolean hasNodeDialog(final NodeContainer nc) {
        return nc instanceof NativeNodeContainer
            && ((NativeNodeContainer)nc).getNode().getFactory() instanceof NodeDialogFactory;
    }

    /**
     * Gets the {@link NodeDialog} for a given node container.
     *
     * @param nc the node to create the node dialog from
     * @return a node dialog instance
     * @throws IllegalArgumentException if the passed node does not provide a node dialog
     */
    public NodeDialog getNodeDialog(final NodeContainer nc) {
        if (!hasNodeDialog(nc)) {
            throw new IllegalArgumentException("The node " + nc.getNameWithID() + " doesn't provide a node dialog");
        }
        return m_nodeDialogMap.computeIfAbsent(nc.getID(), id -> createNodeDialog(nc));
    }

    private static NodeDialog createNodeDialog(final NodeContainer nc) {
        var nnc = (NativeNodeContainer)nc;
        NodeDialogFactory fac = (NodeDialogFactory)nnc.getNode().getFactory();
        NodeContext.pushContext(nnc);
        try {
            return fac.createNodeDialog();
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * Provides the URL which serves the node dialog page.
     *
     * @param nnc the node which provides the node dialog
     * @return the page url
     * @throws IllegalStateException if the node doesn't have a node dialog or the url couldn't be retrieved
     */
    public String getNodeDialogPageUrl(final NativeNodeContainer nnc) {
        var debugUrl = getNodeDialogDebugUrl(nnc.getNode().getFactory().getClass());
        if (debugUrl.isPresent()) {
            return debugUrl.get();
        }
        try {
            var page = getNodeDialog(nnc).getPage();
            return PageUtil.writePageResourcesToDiscAndGetFileUrl(nnc, page, true, null);
        } catch (IOException ex) {
            throw new IllegalStateException(
                "Page URL for the view of node '" + nnc.getNameWithID() + "' couldn't be created.", ex);
        }
    }

    private static Optional<String>
        getNodeDialogDebugUrl(@SuppressWarnings("rawtypes") final Class<? extends NodeFactory> nodeFactoryClass) {
        var pattern = System.getProperty(NODE_DIALOG_DEBUG_PATTERN_PROP);
        var url = System.getProperty(NODE_DIALOG_DEBUG_URL_PROP);
        if (url == null) {
            return Optional.empty();
        }
        if (pattern == null || Pattern.matches(pattern, nodeFactoryClass.getName())) {
            return Optional.of(url);
        }
        return Optional.empty();
    }

    /**
     * For testing purposes only.
     */
    void clearCachesAndFiles() {
       m_nodeDialogMap.clear();
       PageUtil.clearUIExtensionFiles();
    }

}
