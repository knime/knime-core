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
 *   Aug 27, 2021 (hornm): created
 */
package org.knime.core.node.webui;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Builds a {@link Page}.
 *
 * Pending API!
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @since 4.5
 */
public interface PageBuilder {

    /**
     * Sets the content of the page.
     *
     * @param content the page content provided via a supplier for lazy initialization
     * @param relativePath page-resource path and name
     * @return this page builder instance
     */
    PageBuilder content(Supplier<InputStream> content, String relativePath);

    /**
     * Sets the content of the page.
     *
     * @param content the page content provided via a supplier for lazy initialization
     * @param relativePath page-resource path and name
     * @return this page builder instance
     */
    PageBuilder contentFromString(Supplier<String> content, String relativePath);

    /**
     * Adds another resource to the 'context' of a page (such js-resource).
     *
     * @param content the actual content of the resource
     * @param relativePath the relative path to the resource (including the resource name itself)
     * @return this page builder instance
     */
    PageBuilder addResource(Supplier<InputStream> content, String relativePath);

    /**
     * Adds another resource to the 'context' of a page (such js-resource).
     *
     * @param content the actual content of the resource
     * @param relativePath the relative path to the resource (including the resource name itself)
     * @return this page builder instance
     */
    PageBuilder addResourceFromString(Supplier<String> content, String relativePath);

    /**
     * Allows one to provide data that is used for the initialization of the page. Should only be used for a limited
     * amount of data. Large amounts of data (and binary data) should be fetched using the 'rpc service' framework.
     *
     * @param data supplier for the data
     * @return this page builder instance
     */
    PageBuilder initData(final Supplier<String> data);

    /**
     * @return a new page instance
     */
    Page build();

}
