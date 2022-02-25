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
 *   Feb 24, 2022 (hornm): created
 */
package org.knime.core.webui.node;

import java.util.Optional;

import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.Resource;

/**
 * Gives access to the pages and page resources for nodes (i.e. pages that represent, e.g., node dialogs and views).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface PageResourceManager {

    /**
     * @return a unique domain name used to identify page resources of this kind
     */
    String getDomainName();

    /**
     * @param nnc the node to get the page for
     *
     * @return the page for the given node
     * @throws IllegalArgumentException if there is no page given for the node
     */
    Page getPage(NativeNodeContainer nnc);

    /**
     * Provides the URL which serves the page. The full URL is usually only available if the AP is run as desktop
     * application.
     *
     * @param nnc the node which provides the page
     * @return the page url if available, otherwise an empty optional
     */
    Optional<String> getPageUrl(NativeNodeContainer nnc);

    /**
     * Provides the relative path for a page, if available. The relative path is usually only available if the AP is
     * <b>not</b> run as a desktop application (but as an 'executor' as part of the server infrastructure).
     *
     * @param nnc the node which provides the page
     * @return the relative page path
     */
    Optional<String> getPagePath(NativeNodeContainer nnc);

    /**
     * Gives access to page resources. NOTE: Only those resources are available that belong to a page whose path has
     * been requested via {@link #getPagePath(NativeNodeContainer)}.
     *
     * @param resourceId the id of the resource
     * @return the resource or an empty optional if there is no resource for the given id available
     */
    Optional<Resource> getPageResource(String resourceId);

    /**
     * Gives access to page resources via a full URL. NOTE: Only those resources are available that belong to a page
     * whose URL has been requested via {@link #getPageUrl(NativeNodeContainer)}.
     *
     * @param url the resource url
     * @return the resource or an empty optional if there is no resource available at the given URL
     */
    Optional<Resource> getPageResourceFromUrl(String url);

}