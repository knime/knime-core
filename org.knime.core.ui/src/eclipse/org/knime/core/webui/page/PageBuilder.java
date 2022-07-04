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
package org.knime.core.webui.page;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builds a {@link Page}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 *
 * @since 4.5
 */
public class PageBuilder {

    final List<Resource> m_resources = new ArrayList<>();

    Map<String, Function<String, Resource>> m_dynamicResources;

    final Resource m_pageResource;

    PageBuilder(final Supplier<InputStream> content, final String relativePath) {
        m_pageResource = new Resource() {

            @Override
            public String getRelativePath() {
                return relativePath;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return content.get();
            }

            @Override
            public boolean isStatic() {
                return false;
            }

            @Override
            public ContentType getContentType() {
                return ContentType.determineType(relativePath);
            }

        };
    }

    /**
     * @param pageResource
     */
    protected PageBuilder(final Resource pageResource) {
        m_pageResource = pageResource;
    }

    /**
     * Adds another resource to the 'context' of a page (such js-resource).
     *
     * @param content the actual content of the resource
     * @param relativePath the relative path to the resource (including the resource name itself)
     * @return this page builder instance
     */
    public PageBuilder addResource(final Supplier<InputStream> content, final String relativePath) {
        m_resources.add(new Resource() {

            @Override
            public String getRelativePath() {
                return relativePath;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return content.get();
            }

            @Override
            public boolean isStatic() {
                return false;
            }

            @Override
            public ContentType getContentType() {
                return ContentType.determineType(relativePath);
            }

        });
        return this;
    }

    /**
     * Allows one to add multiple resources at once with a single function which dynamically maps paths to resources.
     * I.e. no need to define the exact path upfront (apart from a path-prefix).
     *
     * @param supplier the mapping function from relative path to resource content
     * @param relativePathPrefix the path prefix; if there are resources registered with 'overlapping' path prefixes,
     *            the resources with the 'longest' match are being used
     * @return this page builder instance
     */
    public PageBuilder addResources(final Function<String, InputStream> supplier, final String relativePathPrefix) {
        if (m_dynamicResources == null) {
            m_dynamicResources = new HashMap<>();
        }
        m_dynamicResources.put(relativePathPrefix, relativePath -> { // NOSONAR
            var inputStream = supplier.apply(relativePath);
            if (inputStream == null) {
                return null;
            }
            return new Resource() {

                @Override
                public String getRelativePath() {
                    return relativePath;
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return inputStream;
                }

                @Override
                public boolean isStatic() {
                    return false;
                }

                @Override
                public ContentType getContentType() {
                    return ContentType.determineType(relativePath);
                }

            };
        });
        return this;
    }

    /**
     * Adds another resource to the 'context' of a page (such js-resource).
     *
     * @param content the actual content of the resource
     * @param relativePath the relative path to the resource (including the resource name itself)
     * @return this page builder instance
     */
    public PageBuilder addResourceFromString(final Supplier<String> content, final String relativePath) {
        addResource(() -> new ByteArrayInputStream(content.get().getBytes(StandardCharsets.UTF_8)), relativePath);
        return this;
    }

    /**
     * @return a new page instance
     */
    public Page build() {
        return new Page(m_pageResource, m_resources, m_dynamicResources);
    }

}
