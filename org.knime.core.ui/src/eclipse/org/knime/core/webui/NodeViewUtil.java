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
package org.knime.core.webui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.webui.NodeView;
import org.knime.core.node.webui.Page;
import org.knime.core.node.webui.Resource;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;

/**
 * Utilities around (web-ui) node views.
 *
 * It contains logic that is shared between the web-ui and the java-ui.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class NodeViewUtil {

    private static final String NODE_VIEW_DEBUG_PATTERN_PROP = "org.knime.ui.debug.node.view.url.factory-class";

    private static final String NODE_VIEW_DEBUG_URL_PROP = "org.knime.ui.debug.node.view.url";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeViewUtil.class);

    private static Path uiExtensionsPath;

    private NodeViewUtil() {
        // utility class
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
     * Creates a {@link Page} from a {@link NodeView}.
     *
     * @param <M> the type of mode the node view has access to
     * @param nodeView the node view to create the page from
     * @param nodeModel the node model instance used to create the page (usually to provide the page data)
     * @return a new page instance
     */
    public static <M extends NodeModel> Page createPage(final NodeView<M> nodeView, final M nodeModel) {
        return nodeView.createPage(new DefaultPageBuilderFactory(), nodeModel);
    }

    /**
     * Writes a page (and associated resources) into a temporary directory. Static pages are only written if the
     * respective files don't exist yet (the files are identified by the {@link NodeView}-class).
     * A page is regarded static, if the page itself and all associated resources are static.
     *
     * @param page the page to write to disc
     * @param nodeViewClass the class of the node view; used as identifier for static pages
     * @param nnc the node container to write the view resources for; used, e.g., to register a state change listener to
     *            remove out-dated files
     *
     * @return the file url to the page
     * @throws IOException if writing the files failed
     */
    public static String writeViewResourcesToDiscAndGetFileUrl(final Page page,
        @SuppressWarnings("rawtypes") final Class<? extends NodeView> nodeViewClass, final NativeNodeContainer nnc)
        throws IOException {
        Path rootPath;
        if (isCompletelyStatic(page)) {
            rootPath = createOrGetUIExtensionsPath().resolve(Integer.toString(nodeViewClass.getName().hashCode()));
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

    /**
     * A page is regarded as static if the page itself and all associated resources are static.
     *
     * @param p the page to check
     * @return <code>true</code> if the page itself and all the associated resources are static (i.e. invariable)
     */
    public static boolean isCompletelyStatic(final Page p) {
        return p.isStatic() && p.getContext().stream().allMatch(Resource::isStatic);
    }

    private static void writeResource(final Resource r, final Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        try (InputStream in = r.getInputStream()) {
            Files.copy(in, targetPath);
            LOGGER.debug("New page resource written: " + targetPath);
        }
    }

    private static Path createOrGetUIExtensionsPath() throws IOException {
        if (uiExtensionsPath == null) {
            uiExtensionsPath = FileUtil.createTempDir("ui_extensions_").toPath();
        }
        return uiExtensionsPath;
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
            if (e.getType() == WorkflowEvent.Type.NODE_REMOVED
                && e.getID().getIndex() == m_nnc.getParent().getProjectWFM().getID().getIndex()) {
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
