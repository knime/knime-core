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
 *   Oct 15, 2021 (hornm): created
 */
package org.knime.core.webui.page;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.util.FileUtil;

/**
 * Utility methods around {@link Page}s.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public final class PageUtil {

    private static NodeLogger LOGGER = NodeLogger.getLogger(PageUtil.class);

    private static Path uiExtensionsPath;

    /**
     * Determines the page id. The page id is a valid file name!
     *
     * @param nnc the node providing the node view page
     * @param isStaticPage whether it's a static page
     * @param isDialogPage whether it's a page for a node dialog
     * @return the page id
     */
    @SuppressWarnings("java:S2301")
    public static String getPageId(final NativeNodeContainer nnc, final boolean isStaticPage,
        final boolean isDialogPage) {
        String id;
        if (isStaticPage) {
            id = nnc.getNode().getFactory().getClass().getName();
        } else {
            id = nnc.getID().toString().replace(":", "_");
        }
        if (isDialogPage) {
            id = "dialog_" + id;
        }
        return id;
    }

    /**
     * Writes a page (and associated resources) into a temporary directory. Static pages are only written if the
     * respective files don't exist yet. The files are identified by the page-id (see
     * {@link #getPageId(NativeNodeContainer, boolean, boolean)}). A page is regarded static, if the page itself and all
     * associated resources are static.
     *
     * @param nnc the node container to write the view resources for
     * @param page the page to write the view resources for
     * @param isDialogPage whether it's a page of a node dialog
     * @param pageWrittenCallback called when a page has been written (i.e. if it didn't exist, yet)
     *
     * @return the file url to the page
     * @throws IOException if writing the files failed
     * @throws IllegalStateException if the node doesn't have a node view
     */
    public static String writePageResourcesToDiscAndGetFileUrl(final NativeNodeContainer nnc, final Page page,
        final boolean isDialogPage, final Consumer<Path> pageWrittenCallback) throws IOException {
        var pageId = getPageId(nnc, page.isCompletelyStatic(), isDialogPage);
        var pageRootPath = createOrGetUIExtensionsPath().resolve(pageId);

        var pagePath = pageRootPath.resolve(page.getRelativePath());
        if (!Files.exists(pagePath)) {
            writeResource(page, pagePath);
            for (Resource r : page.getContext().values()) {
                writeResource(r, pageRootPath.resolve(r.getRelativePath()));
            }
            if (pageWrittenCallback != null) {
                pageWrittenCallback.accept(pageRootPath);
            }
        }
        return pagePath.toUri().toString();
    }

    /**
     * For testing purposes only.
     */
    public static void clearUIExtensionFiles() {
        if (uiExtensionsPath != null && Files.exists(uiExtensionsPath)) {
            FileUtils.deleteQuietly(uiExtensionsPath.toFile());
            uiExtensionsPath = null;
        }
    }

    private static void writeResource(final Resource r, final Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        try (var in = r.getInputStream()) {
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

    private PageUtil() {
        // utility class
    }
}
