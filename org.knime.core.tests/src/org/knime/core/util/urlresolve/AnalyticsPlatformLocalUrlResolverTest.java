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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;

/**
 * Tests for {@link AnalyticsPlatformLocalUrlResolver}, currently only with a focus on item version handling.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Manuel Hotz, KNIME AG, Zurich, Switzerland
 */
class AnalyticsPlatformLocalUrlResolverTest {

    private static AnalyticsPlatformLocalUrlResolver m_resolver;

    private static WorkflowContextV2 m_context;

    // before all tests create a new workflow context
    // the workflow context is a singleton, so we need to create a new one for each test
    @BeforeAll
    static void createResolver() {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        m_context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();
        m_resolver = new AnalyticsPlatformLocalUrlResolver((AnalyticsPlatformExecutorInfo)m_context.getExecutorInfo());
    }

    @BeforeEach
    void createWorkflow() {
        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(m_context));
        NodeContext.pushContext(wfm);
    }

    @AfterEach
    void popNodeContext() {
        NodeContext.removeLastContext();
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
        "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()",
        "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()",
        "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeInScope()"
    })
    void testResolveRelativeItemVersion(final URL unversioned, final URL withVersion, final URL bothVersions,
            @SuppressWarnings("unused") final HubItemVersion version)
                    throws ResourceAccessException, MalformedURLException {
        // given a local workflow
        // when resolving a URL with a version
        for (final var url : new URL[] { withVersion, bothVersions }) {
            var resolved = m_resolver.resolve(url);
            // then the version is ignored, same as when resolving without version
            var resolvedWithoutVersion = m_resolver.resolve(unversioned);
            assertEquals(resolvedWithoutVersion, resolved,
                "Version should be ignored when resolving URLs against LOCAL mount point.");
        }
    }

    @ParameterizedTest
    @MethodSource("org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()")
    void testResolveNodeRelative(final URL unversioned, final URL withVersion, final URL bothVersions)
            throws ResourceAccessException {
        final var exc = assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(unversioned));
        assertEquals("Workflow must be saved before node-relative URLs can be used", exc.getLocalizedMessage());

        for (final var url : new URL[] { withVersion, bothVersions }) {
            final var ex = assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(url));
            assertTrue(ex.getLocalizedMessage().startsWith("Node-relative KNIME URLs cannot specify an item version:"));
        }
    }
}
