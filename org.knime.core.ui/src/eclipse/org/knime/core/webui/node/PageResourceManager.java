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

import org.knime.core.webui.page.Page;
import org.knime.core.webui.page.Resource;

/**
 * Gives access to the pages and page resources for nodes (i.e. pages that represent, e.g., node dialogs and views) and
 * node-related ui elements (e.g. port view).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @param <N> the node wrapper this manager operates on
 */
public interface PageResourceManager<N extends NodeWrapper> {

    /**
     * @return a unique domain name used to identify page resources of this kind
     */
    String getDomainName();

    /**
     * @param nodeWrapper the node to get the page for
     *
     * @return the page for the given node
     * @throws IllegalArgumentException if there is no page given for the node
     */
    Page getPage(N nodeWrapper);

    /**
     * @param nodeWrapper the node to get the id for
     * @param page the page to get the id for
     *
     * @return the page for the given node and page
     */
    String getPageId(N nodeWrapper, Page page);

    /**
     * The base url for the page and associated resources. It is usually only available if the AP is run as a desktop
     * application
     *
     * It's <b>not</b> available if run within an 'executor' as part of the server infrastructure - in this case the
     * base url needs to be determined by the frontend.
     *
     * @return the base url or an empty optional if not available
     */
    Optional<String> getBaseUrl();

    /**
     * Optionally returns a debug url for a view (dialog etc.) which is controlled by a system property.
     *
     * @param nodeWrapper the node to get the debug url for
     * @return a debug url or an empty optional if none is set
     */
    Optional<String> getDebugUrl(N nodeWrapper);

    /**
     * Provides the relative path for a page.
     *
     * @param nodeWrapper the node which provides the page
     * @return the relative page path
     */
    String getPagePath(N nodeWrapper);

    /**
     * Gives access to page resources. NOTE: Only those resources are available that belong to a page whose path has
     * been requested via {@link #getPagePath(NodeWrapper)}.
     *
     * @param resourceId the id of the resource
     * @return the resource or an empty optional if there is no resource for the given id available
     */
    Optional<Resource> getPageResource(String resourceId);

    /**
     * Gives access to page resources via a full URL. NOTE: Only those resources are available that belong to a page
     * whose URL has been requested via {@link #getPage(NodeWrapper)}.
     *
     * @param url the resource url
     * @return the resource or an empty optional if there is no resource available at the given URL
     */
    Optional<Resource> getPageResourceFromUrl(String url);

}