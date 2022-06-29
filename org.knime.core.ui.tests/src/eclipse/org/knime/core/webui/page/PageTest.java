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
 *   Sep 15, 2021 (hornm): created
 */
package org.knime.core.webui.page;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.junit.Test;
import org.knime.core.webui.page.Resource.ContentType;

/**
 * Tests {@link Page}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class PageTest {

    /**
     * The bundle id of this test fragment.
     */
    public static final String BUNDLE_ID = "org.knime.core.ui.tests";

    /**
     * Tests {@link Page#isCompletelyStatic()}.
     */
    @Test
    public void testIsCompletelyStaticPage() {
        var page = Page.builder(BUNDLE_ID, "files", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        assertThat(page.isCompletelyStatic(), is(false));
        page = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        assertThat(page.isCompletelyStatic(), is(true));
    }

    /**
     * Tests {@link Page#getContentType()}.
     */
    @Test
    public void testIsComponent() {
        var page = Page.builder(BUNDLE_ID, "files", "page.html").build();
        assertThat(page.getContentType(), is(ContentType.HTML));

        page = Page.builder(BUNDLE_ID, "files", "component.umd.min.js").build();
        assertThat(page.getContentType(), is(ContentType.VUE_COMPONENT_LIB));

        var page2 = Page.builder(() -> "content", "component.blub").build();
        assertThrows(IllegalArgumentException.class, page2::getContentType);
    }

    /**
     * Tests a page that references an entire directory to define other page-related resources.
     */
    @Test
    public void testCreateResourcesFromDir() {
        var page = Page.builder(BUNDLE_ID, "files", "page.html").addResourceDirectory("dir").build();
        assertThat(page.getResource("dir/subdir/res.html").isPresent(), is(true));
        assertThat(page.getResource("dir/res2.umd.min.js").isPresent(), is(true));
        assertThat(page.getResource("dir/res1.html").isPresent(), is(true));
        assertThat(page.getResource("path/to/non/existent/resource.html").isEmpty(), is(true));
    }

    /**
     * Tests page resources added via {@link PageBuilder#addResources(Function, String)}.
     *
     * @throws IOException
     */
    @Test
    public void testCreateResourcesWithDynamicPaths() throws IOException {
        Function<String, InputStream> resourceSupplier = relativePath -> {
            if (relativePath.equals("null")) {
                return null;
            } else if (relativePath.equals("path/to/a/resource")) {
                return stringToInputStream("resource supplier - known path");
            } else {
                return stringToInputStream("resource supplier - another path");
            }
        };
        Function<String, InputStream> resourceSupplier2 =
            relativePath -> stringToInputStream("resource supplier 2 - " + relativePath);

        var page = Page.builder(BUNDLE_ID, "files", "page.html") //
            .addResources(resourceSupplier, "path/prefix") //
            .addResources(resourceSupplier2, "path/prefix/2") //
            .build();
        assertThat(page.isCompletelyStatic(), is(false));

        assertThat(page.getResource("path/prefix/null").isEmpty(), is(true));
        assertThat(resourceToString(page.getResource("path/prefix/path/to/a/resource").get()),
            is("resource supplier - known path"));
        assertThat(resourceToString(page.getResource("path/prefix/path/to/a/resource2").get()),
            is("resource supplier - another path"));
        assertThat(resourceToString(page.getResource("path/prefix/2/path/to/another/resource").get()),
            is("resource supplier 2 - path/to/another/resource"));
        assertThat(page.getResource("path/to/nonexisting/resource").isEmpty(), is(true));
    }

    private static InputStream stringToInputStream(final String s) {
        return new ByteArrayInputStream((s).getBytes(StandardCharsets.UTF_8));
    }

    private static String resourceToString(final Resource r) throws IOException {
        try (var is = r.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
