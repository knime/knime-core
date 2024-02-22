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
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURIEquals;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURLEquals;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.withNodeContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.UUID;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assume;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;

/**
 * Tests for {@link AnalyticsPlatformLocalUrlResolver}. For more generic tests see {@link KnimeUrlResolverTest}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Manuel Hotz, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
class AnalyticsPlatformLocalUrlResolverTest {

    private static AnalyticsPlatformLocalUrlResolver m_resolver;

    private static Path m_currentLocation;

    // before all tests create a new workflow context
    // the workflow context is a singleton, so we need to create a new one for each test
    @BeforeAll
    static void createResolver() {
        m_currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        m_resolver = new AnalyticsPlatformLocalUrlResolver(AnalyticsPlatformExecutorInfo.builder() //
            .withCurrentUserAsUserId() //
            .withLocalWorkflowPath(m_currentLocation) //
            .withMountpoint("LOCAL", m_currentLocation.getParent()) //
            .build());
    }

    private WorkflowManager wfm;

    @BeforeEach
    void createWorkflow() {
        wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(), new WorkflowCreationHelper());
        NodeContext.pushContext(wfm);
    }

    @AfterEach
    void popNodeContext() {
        WorkflowManager.ROOT.removeProject(wfm.getID());
        NodeContext.removeLastContext();
        wfm = null;
    }

    @Nested
    @DisplayName("Reference points: workflow relative, mountpoint/space relative, node relative")
    class ReferencePoints {

        /** Checks if workflow-relative knime-URLs are resolved correctly. */
        @Test
        void testResolveWorkflowRelativeLocal() throws Exception {
            assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", m_resolver,
                m_currentLocation.toUri().toURL(), new URL("knime://knime.workflow"));

            assertResolvedURIEquals(m_resolver, m_currentLocation.resolve("workflow.knime").toUri(),
                new URL("knime://knime.workflow/workflow.knime"));

            assertResolvedURIEquals(m_resolver, m_currentLocation.resolve("..").resolve("test.txt").toUri().normalize(),
                new URL("knime://knime.workflow/../test.txt"));

            assertResolvedURIEquals(m_resolver,
                m_currentLocation.resolve("..").resolve("With Space+Plus.txt").toUri().normalize(),
                new URL("knime://knime.workflow/../With Space+Plus.txt"));

            assertResolvedURIEquals(m_resolver,
                m_currentLocation.resolve("..").resolve("With Space+Plus.txt").toUri().normalize(),
                new URL("knime://knime.workflow/../With%20Space+Plus.txt"));

            assertResolvedURIEquals(m_resolver, m_currentLocation.resolve("Double Slash Decoded").toUri(),
                new URL("knime://knime.workflow//Double Slash Decoded"));

            assertResolvedURIEquals(m_resolver,
                m_currentLocation.resolve("..").resolve("Double Slash Encoded").toUri().normalize(),
                new URL("knime://knime.workflow/..//Double%20Slash%20Encoded"));

            assertResolvedURIEquals(m_resolver,
                m_currentLocation.resolve("..").resolve("Double Slash Decoded").toUri().normalize(),
                new URL("knime://knime.workflow/..//Double Slash Decoded"));

            assertResolvedURIEquals(m_resolver, m_currentLocation.resolve("Double Slash Encoded").toUri(),
                new URL("knime://knime.workflow//Double%20Slash%20Encoded"));
        }

        /** Checks if mountpoint/space-relative knime-URLs are resolved correctly. */
        @ParameterizedTest
        @ValueSource(strings = {"knime.mountpoint", "knime.space"})
        void testResolveMountPointAndSpaceRelative(final String mountpointOrSpace) throws Exception {
            assertResolvedURIEquals(m_resolver,
                m_currentLocation.getParent().resolve("test.txt").toUri(),
                new URL(MessageFormat.format("knime://{0}/test.txt", mountpointOrSpace)));

            assertResolvedURIEquals(m_resolver,
                m_currentLocation.getParent().resolve("With Space+Plus.txt").toUri(),
                new URL(MessageFormat.format("knime://{0}/With Space+Plus.txt", mountpointOrSpace)));

            assertResolvedURIEquals(m_resolver,
                m_currentLocation.getParent().resolve("With Space+Plus.txt").toUri(),
                new URL(MessageFormat.format("knime://{0}/With%20Space+Plus.txt", mountpointOrSpace)));
        }

        /** Checks if node-relative knime-URLs are resolved correctly. */
        @Test
        void testResolveNodeRelativeLocal() throws Exception {
            final var mountpointRoot = KNIMEConstants.getKNIMETempPath().resolve("root");
            final var currentLocation = mountpointRoot.resolve("workflow");

            final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                    .withAnalyticsPlatformExecutor(exec -> exec
                        .withCurrentUserAsUserId()
                        .withLocalWorkflowPath(currentLocation)
                        .withMountpoint("LOCAL", mountpointRoot))
                    .withLocalLocation()
                    .build());

            withNodeContext(currentLocation, () -> {
                assertResolvedURIEquals(resolver,
                    currentLocation.resolve("test.txt").toUri(),
                    new URL("knime://knime.node/test.txt"));

                // Check proper handling of potentially encoded paths (see AP-17103).
                assertResolvedURIEquals(resolver,
                    currentLocation.resolve("With Space+Plus.txt").toUri(),
                    new URL("knime://knime.node/With Space+Plus.txt"));

                assertResolvedURIEquals(resolver,
                    currentLocation.resolve("With Space+Plus.txt").toUri(),
                    new URL("knime://knime.node/With%20Space+Plus.txt"));
            });
        }

        /** Checks that node-relative knime-URLs only work on saved workflows. */
        @ParameterizedTest
        @MethodSource("org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()")
        void testResolveNodeRelativeLocalNotSaved(final URL unversioned, final URL withVersion, final URL bothVersions,
            @SuppressWarnings("unused") final HubItemVersion version) throws Exception {
            withNodeContext(null, () -> {
                final var exc = assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(unversioned));
                assertEquals("Workflow must be saved before node-relative URLs can be used", exc.getLocalizedMessage());
            });
        }

    }

    /** Checks if workflow-relative knime-URLs that reside on a UNC drive are resolved correctly (see AP-7427). */
    @Test
    void testResolveMountpointRelativeLocalUNC() throws Exception {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);

        final File currentLocation = new File("\\\\server\\repo\\workflow");

        final var resolver =
            KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec ->  exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation.toPath())
                    .withMountpoint("LOCAL", currentLocation.getParentFile().toPath()))
                .withLocalLocation()
                .build());

        assertResolvedURIEquals(
            resolver, new File(currentLocation, "workflow.knime").toURI(),
            new URL("knime://knime.workflow/workflow.knime"));

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toURI().toURL(), new URL("knime://knime.workflow"));

        // path outside the workflow
        final var url = new URL("knime://knime.workflow/../test.txt");
        final var expectedPath = new File(currentLocation.getParentFile(), "test.txt");
        final var resolved = resolver.resolve(url);
        assertEquals("Unexpected resolved URL", expectedPath.toURI(), resolved.toURI());
        assertEquals("Unexpected resulting file", expectedPath, new File(resolved.toURI()));
    }

    @Nested
    class Versioning {
        @ParameterizedTest
        @MethodSource({"org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()",
            "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeInScope()"})
        void testResolveRelativeIgnoresItemVersion(final URL unversioned, final URL withVersion, final URL bothVersions,
            @SuppressWarnings("unused") final HubItemVersion version)
            throws ResourceAccessException, MalformedURLException {
            // given a local workflow
            // when resolving a URL with a version
            for (final var url : new URL[]{withVersion, bothVersions}) {
                var resolved = m_resolver.resolve(url);
                // then the version is ignored, same as when resolving without version
                var resolvedWithoutVersion = m_resolver.resolve(unversioned);
                assertThat(resolvedWithoutVersion)
                    .as("Version should be ignored when resolving URLs against LOCAL mount point.").isEqualTo(resolved);
            }
        }

        @ParameterizedTest
        @MethodSource("org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()")
        void testResolveNodeRelative(final URL unversioned, final URL withVersion, final URL bothVersions)
            throws ResourceAccessException {
            for (final var url : new URL[]{withVersion, bothVersions}) {
                final var ex = assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(url));
                assertThat(ex.getLocalizedMessage())
                    .startsWith("Node-relative KNIME URLs cannot specify an item version:");
            }
        }
    }
}
