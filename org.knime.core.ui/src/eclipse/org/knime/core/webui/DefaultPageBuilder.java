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
 *   Sep 1, 2021 (hornm): created
 */
package org.knime.core.webui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.webui.Page;
import org.knime.core.node.webui.PageBuilder;
import org.knime.core.node.webui.Resource;

/**
 * The default implementation of the {@link DynamicPageBuilder}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class DefaultPageBuilder implements PageBuilder {

    private Supplier<String> m_data;

    protected final List<Resource> m_resources = new ArrayList<>();

    protected Resource m_pageResource;

    @Override
    public PageBuilder content(final Supplier<InputStream> content, final String relativePath) {
        m_pageResource = new Resource() {

            @Override
            public Path getRelativePath() {
                return Paths.get(relativePath);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return content.get();
            }

            @Override
            public boolean isStatic() {
                return false;
            }

        };
        return this;
    }

    @Override
    public PageBuilder contentFromString(final Supplier<String> content, final String relativePath) {
        return content(() -> new ByteArrayInputStream(content.get().getBytes(StandardCharsets.UTF_8)), relativePath);
    }

    @Override
    public PageBuilder initData(final Supplier<String> data) {
        m_data = data;
        return this;
    }

    @Override
    public Page build() {
        CheckUtils.checkNotNull(m_pageResource, "No page content given");
        return new Page() { // NOSONAR

            @Override
            public Path getRelativePath() {
                return m_pageResource.getRelativePath();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return m_pageResource.getInputStream();
            }

            @Override
            public List<Resource> getContext() {
                return m_resources;
            }

            @Override
            public boolean isStatic() {
                return m_pageResource.isStatic();
            }

            @Override
            public String getInitData() {
                if (m_data != null) {
                    return m_data.get();
                } else {
                    return null;
                }
            }

        };
    }

    @Override
    public PageBuilder addResource(final Supplier<InputStream> content, final String relativePath) {
        m_resources.add(new Resource() {

            @Override
            public Path getRelativePath() {
                return Paths.get(relativePath);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return content.get();
            }

            @Override
            public boolean isStatic() {
                return false;
            }

        });
        return this;
    }

    @Override
    public PageBuilder addResourceFromString(final Supplier<String> content, final String relativePath) {
        addResource(() -> new ByteArrayInputStream(content.get().getBytes(StandardCharsets.UTF_8)), relativePath);
        return this;
    }

}
