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
 *   Aug 23, 2021 (hornm): created
 */
package org.knime.core.webui.page;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A (html) page of an ui-extension, e.g. a node view, port view or node dialog.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public final class Page implements Resource {

    private final Resource m_pageResource;

    private final Map<String, Resource> m_context;

    private Boolean m_isCompletelyStatic;

    Page(final Resource pageResource, final List<Resource> context) {
        m_pageResource = pageResource;
        m_context = context == null ? Collections.emptyMap()
            : context.stream().collect(Collectors.toMap(Resource::getRelativePath, r -> r));
    }

    /**
     * Additional resources required by the page.
     *
     * @return map from relative path to resources; or an empty map if there a none - never <code>null</code>
     */
    public Map<String, Resource> getContext() {
        return m_context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRelativePath() {
        return m_pageResource.getRelativePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return m_pageResource.getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStatic() {
        return m_pageResource.isStatic();
    }

    /**
     * A page is regarded completely static if the page itself and all associated resources are static.
     *
     * @return <code>true</code> if the page itself and all the associated resources are static (i.e. invariable)
     */
    public boolean isCompletelyStatic() {
        if (m_isCompletelyStatic == null) {
            m_isCompletelyStatic = isStatic() && getContext().values().stream().allMatch(Resource::isStatic);
        }
        return m_isCompletelyStatic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
        return m_pageResource.getType();
    }

    /**
     * Creates a {@link PageBuilder}-instance to create a (static) page (and associated resources) from files.
     *
     * @param clazz a class which is part of the bundle where the references files are located
     * @param basePath the base part (beneath the bundle root)
     * @param relativeFilePath the file to get the page content from
     * @return a new {@link PageBuilder}-instance
     */
    public static FromFilePageBuilder builder(final Class<?> clazz, final String basePath,
        final String relativeFilePath) {
        return new FromFilePageBuilder(clazz, basePath, relativeFilePath);
    }

    /**
     * Creates a {@link PageBuilder}-instance to create a (static) page (and associated resources) from files.
     *
     * @param bundleID the id of the bundle where the references files are located
     * @param basePath the base part (beneath the bundle root)
     * @param relativeFilePath the file to get the page content from
     * @return a new {@link PageBuilder}-instance
     */
    public static FromFilePageBuilder builder(final String bundleID, final String basePath,
        final String relativeFilePath) {
        return new FromFilePageBuilder(bundleID, basePath, relativeFilePath);
    }

    /**
     * Creates a {@link PageBuilder}-instance to create a (dynamic) page (and associated resources) from an
     * {@link InputStream}.
     *
     * @param content the page content supplier for lazy initialization
     * @param relativePath the relative path of the page (including the page resource name itself)
     * @return a new {@link PageBuilder}-instance
     */
    public static PageBuilder builder(final InputStreamSupplier content, final String relativePath) {
        return new PageBuilder(content, relativePath);
    }

    /**
     * Creates a {@link PageBuilder}-instance to create a (dynamic) page (and associated resources) from an
     * {@link InputStream}.
     *
     * @param content the page content supplier for lazy initialization
     * @param relativePath the relative path of the page (including the page resource name itself)
     * @return a new {@link PageBuilder}-instance
     */
    public static PageBuilder builder(final StringSupplier content, final String relativePath) {
        return builder(() -> new ByteArrayInputStream(content.get().getBytes(StandardCharsets.UTF_8)), relativePath);
    }

    /**
     * {@link Supplier} of a {@link String}.
     */
    @FunctionalInterface
    public interface StringSupplier extends Supplier<String> {
        //
    }

    /**
     * {@link Supplier} of a {@link InputStream}.
     */
    @FunctionalInterface
    public interface InputStreamSupplier extends Supplier<InputStream> {
        //
    }

}
