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
 *   6 Jul 2023 (carlwitt): created
 */
package org.knime.core.util.urlresolve;
import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertThrows;

import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;

/**
 * Tests for {@link ContextlessUrlResolver} For more generic tests see {@link KnimeUrlResolverTest}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Manuel Hotz, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
class ContextlessUrlResolverTest {

    private static ContextlessUrlResolver m_resolver;

    // before all tests create a new workflow context
    // the workflow context is a singleton, so we need to create a new one for each test
    @BeforeAll
    static void createResolver() {
        m_resolver = (ContextlessUrlResolver)KnimeUrlResolver.getResolver(null);
    }

    /**
     * Checks if a missing {@link WorkflowContextV2} is handled correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testNoWorkflowContext() throws Exception {
        final var e = assertThrows(m_resolver::resolve, new URL("knime://knime.workflow/workflow.knime"));
        assertThat(e).hasMessageContaining("No context");
    }

    /** Check if URLs with a local mount point are resolved correctly. */
    @Test
    void testResolveLocalMountpointURL() throws Exception {
        assertThat(m_resolver.resolve(new URL("knime://LOCAL/test.txt"))).hasProtocol("knime");
    }
}
