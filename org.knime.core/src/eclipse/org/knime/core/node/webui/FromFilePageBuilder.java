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
 *   Sep 2, 2021 (hornm): created
 */
package org.knime.core.node.webui;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Adds additional methods to the {@link PageBuilder} which allows one to define the page content and associated
 * resources by referencing files.
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
public interface FromFilePageBuilder extends PageBuilder {

    /**
     * Sets the content of the page.
     *
     * @param relativeFilePath the file to get the page content from
     * @return this page builder instance
     */
    FromFilePageBuilder contentFromFile(String relativeFilePath);

    /**
     * Adds another resource file to the 'context' of a page (such as a js-file).
     *
     * @param relativeFilePath the relative path to the file
     * @return this page builder instance
     */
    FromFilePageBuilder addResourceFile(final String relativeFilePath);

    /**
     * Adds all files in the given directory to the 'context' of a page (a directory containing, e.g., js- and
     * css-files).
     *
     * @param relativeDirPath the relative path to the directory
     * @return this page builder instance
     */
    FromFilePageBuilder addResourceDirectory(final String relativeDirPath);

    /*
     * -------------------------------------------------------------------------------------------------
     * Methods overwritten from the parent class in order to narrow down the returned page builder type.
     * -------------------------------------------------------------------------------------------------
     */

    @Override
    FromFilePageBuilder content(Supplier<InputStream> content, String relativePath);

    @Override
    FromFilePageBuilder contentFromString(Supplier<String> content, String relativePath);

    @Override
    FromFilePageBuilder addResource(Supplier<InputStream> content, String relativePath);

    @Override
    FromFilePageBuilder addResourceFromString(Supplier<String> content, String relativePath);

    @Override
    FromFilePageBuilder initData(final Supplier<String> data);

}
