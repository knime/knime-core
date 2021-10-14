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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.data.text.TextReExecuteDataService;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.Resource;

/**
 * Manages (web-ui) node view instances and provides associated functionality.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public final class NodeViewManager {

    private static final String NODE_VIEW_DEBUG_PATTERN_PROP = "org.knime.ui.debug.node.view.url.factory-class";

    private static final String NODE_VIEW_DEBUG_URL_PROP = "org.knime.ui.debug.node.view.url";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeViewManager.class);

    private static NodeViewManager instance;

    private Path m_uiExtensionsPath;

    private final Map<NodeID, NodeView> m_nodeViewMap = new HashMap<>();

    private final Map<NodeID, NodeCleanUpCallback> m_nodeCleanUpCallbacks = new HashMap<>();

    private final Map<String, Page> m_pageMap = new HashMap<>();

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
        var nodeView = m_nodeViewMap.get(nnc.getID());
        if (nodeView != null) {
            return nodeView;
        }
        return createAndRegisterNodeView(nnc);
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
        var nodeId = nnc.getID();
        m_nodeViewMap.put(nodeId, nodeView);
        var nodeCleanUpCallback = new NodeCleanUpCallback(nnc, () -> {
            m_nodeViewMap.remove(nodeId);
            m_nodeCleanUpCallbacks.remove(nodeId);
        });
        m_nodeCleanUpCallbacks.put(nodeId, nodeCleanUpCallback);
    }

    /**
     * Helper to call a {@link TextInitialDataService} for a node view.
     *
     * @param nc the node providing the view to call the data service for
     * @return the initial data
     * @throws IllegalStateException if the provided node doesn't provide a node view or the node view does not provide
     *             an inital data service
     *
     */
    public String callTextInitialDataService(final NodeContainer nc) {
        var initialDataService = getNodeView(nc).getInitialDataService().orElse(null);
        if (initialDataService instanceof TextInitialDataService) {
            return ((TextInitialDataService)initialDataService).getInitialData();
        } else {
            throw new IllegalStateException("The node view provided by node '" + nc.getNameWithID()
                + "' does not provide a 'initial data service'");
        }
    }

    /**
     * Helper to call a {@link TextDataService} for a node view.
     *
     * @param nc the node providing the view to call the data service for
     * @param request the data service request
     * @return the data service response
     * @throws IllegalStateException if the provided node doesn't provide a node view or the node view does not provide
     *             a data service
     */
    public String callTextDataService(final NodeContainer nc, final String request) {
        var dataService = getNodeView(nc).getDataService().orElse(null);
        if (dataService instanceof TextDataService) {
            return ((TextDataService)dataService).handleRequest(request);
        } else {
            throw new IllegalStateException(
                "The node view provided by node '" + nc.getNameWithID() + "' does not provide a 'data service'");
        }
    }

    /**
     * Helper to call a {@link TextReExecuteDataService} for a node view.
     *
     * @param nc the node providing the view to call the data service for
     * @param request the data service request representing the data to apply
     * @throws IOException if applying the data failed
     * @throws IllegalStateException if the provided node doesn't provide a node view or the node view does not provide
     *             a data service
     */
    public void callTextReExecuteDataService(final NodeContainer nc, final String request) throws IOException {
        var reExecuteDataService = getNodeView(nc).getReExecuteDataService().orElse(null);
        if (reExecuteDataService instanceof TextReExecuteDataService) {
            ((TextReExecuteDataService)reExecuteDataService).reExecute(request);
        } else {
            throw new IllegalStateException(
                "The node view provided by node '" + nc.getNameWithID() + "' does not provide a 'data service'");
        }
    }

    /**
     * Optionally returns a debug url for a view which is controlled by a system property.
     *
     * @param nodeFactoryClass the node factory class to get the node view debug url for
     * @return a debug url or an empty optional of none is set
     */
    private static Optional<String>
        getNodeViewDebugUrl(@SuppressWarnings("rawtypes") final Class<? extends NodeFactory> nodeFactoryClass) {
        String pattern = System.getProperty(NODE_VIEW_DEBUG_PATTERN_PROP);
        String url = System.getProperty(NODE_VIEW_DEBUG_URL_PROP);
        if (url == null) {
            return Optional.empty();
        }
        if (pattern == null || Pattern.matches(pattern, nodeFactoryClass.getName())) {
            return Optional.of(url);
        }
        return Optional.empty();
    }

    /**
     * Provides the URL which serves the node view page.
     * The full URL is usually only available if the AP is run as desktop application.
     *
     * @param nnc the node which provides the node view
     * @return the page url if available, otherwise an empty optional
     * @throws IllegalStateException if the node doesn't have a node view or the node view url couldn't be retrieved
     */
    public Optional<String> getNodeViewPageUrl(final NativeNodeContainer nnc) {
        if (isRunAsDesktopApplication()) {
            var debugUrl = getNodeViewDebugUrl(nnc.getNode().getFactory().getClass());
            if (debugUrl.isPresent()) {
                return debugUrl;
            }
            try {
                return Optional.of(writeNodeViewResourcesToDiscAndGetFileUrl(nnc));
            } catch (IOException ex) {
                throw new IllegalStateException(
                    "Page URL for the view of node '" + nnc.getNameWithID() + "' couldn't be created.", ex);
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Provides the relative path for a node view, if available. The relative path is usually only available if the AP
     * is <b>not</b> run as a desktop application (but as an 'executor' as part of the server infrastructure).
     *
     * @param nnc the node which provides the node view
     * @return the relative page path if available, otherwise an empty optional
     */
    public Optional<String> getNodeViewPagePath(final NativeNodeContainer nnc) {
        if (!isRunAsDesktopApplication()) {
            var page = getNodeView(nnc).getPage();
            var isStaticPage = page.isCompletelyStatic();
            var pageId = getPageId(nnc, isStaticPage);
            registerPage(nnc.getID(), page, pageId);
            return Optional.of(pageId + "/" + page.getRelativePath());
        } else {
            return Optional.empty();
        }
    }

    private void registerPage(final NodeID nodeId, final Page page, final String pageId) {
        m_pageMap.put(pageId, page);
        var nodeCleanUpCallback = m_nodeCleanUpCallbacks.get(nodeId);
        if (nodeCleanUpCallback != null) {
            nodeCleanUpCallback.onCleanUp(() -> m_pageMap.remove(pageId));
        }
    }

    /**
     * Gives access to page resources. NOTE: Only those resources are available that belong to a page whose path has
     * been requested via {@link #getNodeViewPagePath(NativeNodeContainer)}.
     *
     * @param resourceId the id of the resource
     * @return the resource or an empty optional if there is no resource for the given id available
     */
    public Optional<Resource> getNodeViewPageResource(final String resourceId) {
        var split = resourceId.indexOf("/");
        if (split <= 0) {
            return Optional.empty();
        }

        var pageId = resourceId.substring(0, split);
        var page = m_pageMap.get(pageId);
        if (page == null) {
            return Optional.empty();
        }

        var relPath = resourceId.substring(split + 1, resourceId.length());
        if (page.getRelativePath().equals(relPath)) {
            return Optional.of(page);
        } else {
            return Optional.ofNullable(page.getContext().get(relPath));
        }
    }

    private static boolean isRunAsDesktopApplication() {
        return !"true".equals(System.getProperty("java.awt.headless"));
    }

    /**
     * Writes a page (and associated resources) into a temporary directory. Static pages are only written if the
     * respective files don't exist yet (the files are identified by the {@link NodeFactory}-class). A page is regarded
     * static, if the page itself and all associated resources are static.
     *
     * @param nnc the node container to write the view resources for
     *
     * @return the file url to the page
     * @throws IOException if writing the files failed
     * @throws IllegalStateException if the node doesn't have a node view
     */
    private String writeNodeViewResourcesToDiscAndGetFileUrl(final NativeNodeContainer nnc) throws IOException {
        var page = getNodeView(nnc).getPage();
        var isCompletelyStatic = page.isCompletelyStatic();
        var pageId = getPageId(nnc, isCompletelyStatic);
        var rootPath = createOrGetUIExtensionsPath().resolve(pageId);

        var pagePath = rootPath.resolve(page.getRelativePath());
        if (!Files.exists(pagePath)) {
            writeResource(page, pagePath);
            for (Resource r : page.getContext().values()) {
                writeResource(r, rootPath.resolve(r.getRelativePath()));
            }
            var nodeCleanUpCallback = m_nodeCleanUpCallbacks.get(nnc.getID());
            if (nodeCleanUpCallback != null && !isCompletelyStatic) {
                nodeCleanUpCallback.onCleanUp(() -> deleteResources(rootPath));
            }
        }
        return pagePath.toUri().toString();
    }

    /**
     * Determines the page id. The page id is a valid file name!
     *
     * @param nnc the node providing the node view page
     * @param isStaticPage whether it's a static page
     * @return the page id
     */
    @SuppressWarnings("java:S2301")
    public static String getPageId(final NativeNodeContainer nnc, final boolean isStaticPage) {
        if (isStaticPage) {
            return nnc.getNode().getFactory().getClass().getName();
        } else {
            return nnc.getID().toString().replace(":", "_");
        }
    }

    private static void deleteResources(final Path rootPath) {
        if (Files.exists(rootPath)) {
            try {
                FileUtils.deleteDirectory(rootPath.toFile());
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("Directory " + rootPath + " could not be deleted", e);
            }
        }
    }

    private static void writeResource(final Resource r, final Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        try (var in = r.getInputStream()) {
            Files.copy(in, targetPath);
            LOGGER.debug("New page resource written: " + targetPath);
        }
    }

    private Path createOrGetUIExtensionsPath() throws IOException {
        if (m_uiExtensionsPath == null) {
            m_uiExtensionsPath = FileUtil.createTempDir("ui_extensions_").toPath();
        }
        return m_uiExtensionsPath;
    }

    /**
     * For testing purposes only.
     */
    void clearCachesAndFiles() {
        m_nodeViewMap.clear();
        m_nodeCleanUpCallbacks.clear();
        m_pageMap.clear();
        if (m_uiExtensionsPath != null && Files.exists(m_uiExtensionsPath)) {
            FileUtils.deleteQuietly(m_uiExtensionsPath.toFile());
            m_uiExtensionsPath = null;
        }
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
     * For testing purposes only.
     *
     * @return
     */
    int getPageMapSize() {
        return m_pageMap.size();
    }

    /*
     * Helper to clean-up after a node removal, node state change or workflow disposal.
     * Once a clean-up has been triggered, all the registered listeners (on the node and the workflow) are removed which
     * renders the NodeCleanUpCallback-instance useless afterwards.
     */
    private static class NodeCleanUpCallback implements WorkflowListener, NodeStateChangeListener {

        private final List<Runnable> m_onCleanUp = new ArrayList<>();

        private NativeNodeContainer m_nnc;

        NodeCleanUpCallback(final NativeNodeContainer nnc, final Runnable onCleanUp) {
            WorkflowManager.ROOT.addListener(NodeCleanUpCallback.this);
            nnc.addNodeStateChangeListener(NodeCleanUpCallback.this);
            nnc.getParent().addListener(NodeCleanUpCallback.this);
            m_nnc = nnc;
            m_onCleanUp.add(onCleanUp);
        }

        void onCleanUp(final Runnable r) {
            m_onCleanUp.add(r);
        }

        @Override
        public void workflowChanged(final WorkflowEvent e) {
            if (e.getType() == WorkflowEvent.Type.NODE_REMOVED) {
                if (e.getOldValue() instanceof WorkflowManager && ((WorkflowManager)e.getOldValue()).getID()
                    .getIndex() == m_nnc.getParent().getProjectWFM().getID().getIndex()) {
                    // workflow has been closed
                    cleanUp();
                }
                if (e.getOldValue() instanceof NativeNodeContainer
                    && ((NativeNodeContainer)e.getOldValue()).getID().equals(m_nnc.getID())) {
                    // node removed
                    cleanUp();
                }
            }
        }

        @Override
        public void stateChanged(final NodeStateEvent state) {
            cleanUp();
        }

        private void cleanUp() {
            WorkflowManager.ROOT.removeListener(NodeCleanUpCallback.this);
            m_nnc.removeNodeStateChangeListener(NodeCleanUpCallback.this);
            m_nnc.getParent().removeListener(NodeCleanUpCallback.this);
            m_onCleanUp.forEach(Runnable::run);
            m_nnc = null;
            m_onCleanUp.clear();
        }
    }
}
