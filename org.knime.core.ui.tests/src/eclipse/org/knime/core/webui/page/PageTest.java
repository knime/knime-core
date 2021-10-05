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

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Test;

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
        Page page = Page.builder(BUNDLE_ID, "files", "page.html")
            .addResourceFromString(() -> "resource content", "resource.html").build();
        assertThat(page.isCompletelyStatic(), is(false));
        page = Page.builder(BUNDLE_ID, "files", "page.html").addResourceFile("resource.html").build();
        assertThat(page.isCompletelyStatic(), is(true));
    }

    /**
     * Tests {@link Page#isWebComponent()}.
     */
    @Test
    public void testIsComponent() {
        Page page = Page.builder(BUNDLE_ID, "files", "page.html").build();
        assertThat(page.isWebComponent(), is(false));

        page = Page.builder(BUNDLE_ID, "files", "component.js").build();
        assertThat(page.isWebComponent(), is(true));

        page = Page.builderFromString(() -> "content", "component.js").build();
        assertThat(page.isWebComponent(), is(false));

    }

    /**
     * Test if a page is created for file that doesn't exist.
     */
    @Test
    public void testNonExistingFile() {
        String message = assertThrows(IllegalArgumentException.class,
            () -> Page.builder("org.knime.core.ui.tests", "files", "non-existing-file.html").build()).getMessage();
        assertThat(message, containsString("doesn't exist"));
    }

    /**
     * Tests a page that references an entire directory to define other page-related resources.
     */
    @Test
    public void testCreateResourcesFromDir() {
        Page page = Page.builder(BUNDLE_ID, "files", "page.html").addResourceDirectory("dir").build();
        List<String> context =
            page.getContext().stream().map(r -> r.getRelativePath().toString()).collect(Collectors.toList());
        assertThat(context, Matchers.containsInAnyOrder(format("dir%1$ssubdir%1$sres.html", File.separator),
            format("dir%sres2.js", File.separator), format("dir%sres1.html", File.separator)));
    }

}
