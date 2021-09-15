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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;
import org.knime.core.webui.data.ApplyDataService;
import org.knime.core.webui.data.DataService;
import org.knime.core.webui.data.InitialDataService;
import org.knime.core.webui.data.text.TextApplyDataService;
import org.knime.core.webui.data.text.TextDataService;
import org.knime.core.webui.data.text.TextInitialDataService;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.Resource;

import com.google.common.collect.MapMaker;

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

    private final Map<NodeContainer, NodeView> m_nodeViewCache = new MapMaker().weakKeys().weakValues().makeMap();

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
    @SuppressWarnings("unchecked")
    public NodeView getNodeView(final NodeContainer nc) {
        if (!hasNodeView(nc)) {
            throw new IllegalArgumentException("The node " + nc.getNameWithID() + " doesn't provide a node view");
        }
        return m_nodeViewCache.computeIfAbsent(nc, k -> {
            NativeNodeContainer nnc = (NativeNodeContainer)nc;
            NodeViewFactory<NodeModel> fac = (NodeViewFactory<NodeModel>)nnc.getNode().getFactory();
            NodeContext.pushContext(nc);
            try {
                return fac.createNodeView(nnc.getNodeModel());
            } finally {
                NodeContext.removeLastContext();
            }
        });
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
        InitialDataService initialDataService = getNodeView(nc).getInitialDataService().orElse(null);
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
        DataService dataService = getNodeView(nc).getDataService().orElse(null);
        if (dataService instanceof TextDataService) {
            return ((TextDataService)dataService).handleRequest(request);
        } else {
            throw new IllegalStateException(
                "The node view provided by node '" + nc.getNameWithID() + "' does not provide a 'data service'");
        }
    }

    /**
     * Helper to call a {@link TextApplyDataService} for a node view.
     *
     * @param nc the node providing the view to call the data service for
     * @param request the data service request representing the data to apply
     * @throws IOException if applying the data failed
     * @throws IllegalStateException if the provided node doesn't provide a node view or the node view does not provide
     *             a data service
     */
    public void callTextApplyDataService(final NodeContainer nc, final String request) throws IOException {
        ApplyDataService applyDataService = getNodeView(nc).getApplyDataService().orElse(null);
        if (applyDataService instanceof TextApplyDataService) {
            ((TextApplyDataService)applyDataService).applyData(request);
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
    public static Optional<String>
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
     * Writes a page (and associated resources) into a temporary directory. Static pages are only written if the
     * respective files don't exist yet (the files are identified by the {@link NodeFactory}-class). A page is regarded
     * static, if the page itself and all associated resources are static.
     *
     * @param nnc the node container to write the view resources for; used, e.g., to register a state change listener to
     *            remove out-dated files
     *
     * @return the file url to the page
     * @throws IOException if writing the files failed
     * @throws IllegalStateException if the node doesn't have a node view
     */
    public String writeNodeViewResourcesToDiscAndGetFileUrl(final NativeNodeContainer nnc) throws IOException {
        Page page = getNodeView(nnc).getPage();
        Path rootPath;
        if (page.isCompletelyStatic()) {
            rootPath = createOrGetUIExtensionsPath()
                .resolve(Integer.toString(nnc.getNode().getFactory().getClass().getName().hashCode()));
        } else {
            String dynamicPageId = Integer.toString(nnc.getID().toString().hashCode());
            rootPath = createOrGetUIExtensionsPath().resolve(dynamicPageId);

            // this makes sure that the dynamic page resource files are deleted on node state change
            // and when the entire workflow project is closed
            ListenerToDeleteResources listener = new ListenerToDeleteResources(rootPath, nnc);
            WorkflowManager.ROOT.addListener(listener);
            nnc.addNodeStateChangeListener(listener);
        }

        Path pagePath = rootPath.resolve(page.getRelativePath());
        if (!Files.exists(pagePath)) {
            writeResource(page, pagePath);
            for (Resource r : page.getContext()) {
                writeResource(r, rootPath.resolve(r.getRelativePath()));
            }
        }
        return pagePath.toUri().toString();
    }

    private static void writeResource(final Resource r, final Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        try (InputStream in = r.getInputStream()) {
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

    private static class ListenerToDeleteResources implements WorkflowListener, NodeStateChangeListener {

        private final Path m_path;

        private final NativeNodeContainer m_nnc;

        ListenerToDeleteResources(final Path path, final NativeNodeContainer nnc) {
            m_path = path;
            m_nnc = nnc;
        }

        @Override
        public void workflowChanged(final WorkflowEvent e) {
            if (e.getType() == WorkflowEvent.Type.NODE_REMOVED && e.getOldValue() instanceof WorkflowManager
                && ((WorkflowManager)e.getOldValue()).getID().getIndex() == m_nnc.getParent().getProjectWFM().getID()
                    .getIndex()) {
                deleteResources(m_path);
                WorkflowManager.ROOT.removeListener(ListenerToDeleteResources.this);
            }
        }

        @Override
        public void stateChanged(final NodeStateEvent state) {
            deleteResources(m_path);
            m_nnc.removeNodeStateChangeListener(ListenerToDeleteResources.this);
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

    }
}
