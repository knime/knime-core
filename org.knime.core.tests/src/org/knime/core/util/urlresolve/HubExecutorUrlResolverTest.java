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
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURIEquals;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURLEquals;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertThrows;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.HubJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;

/**
 * Tests for {@link HubExecutorUrlResolver}, currently only with a focus on item version handling.
 *
 * @author Manuel Hotz, KNIME AG, Zurich, Switzerland
 */
class HubExecutorUrlResolverTest {

    private static HubExecutorUrlResolver m_resolver;

    // before all tests create a new workflow context
    // the workflow context is a singleton, so we need to create a new one for each test
    @BeforeAll
    static void createResolver() {
        final var repoAddressUri = URI.create("https://api.example.com:443/knime/rest/v4/repository");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withHubJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID())
                    .withScope("test", "test")
                    .withJobCreator("test"))
                .withHubSpaceLocation(loc -> loc
                    .withRepositoryAddress(repoAddressUri)
                    .withWorkflowPath("/Users/john/Private/folder/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12"))
                .build();

        m_resolver = new HubExecutorUrlResolver((HubJobExecutorInfo)context.getExecutorInfo(),
            (HubSpaceLocationInfo)context.getLocationInfo());
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

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#mountpointAbsolute()",
        "org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
        "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()",
        "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()"
    })
    void testResolveWithVersionParameter(final URL unversioned, final URL withItemOrSpaceVersion, final URL withBoth,
            final ItemVersion version) throws ResourceAccessException, MalformedURLException {
        final var resolvedPlain = m_resolver.resolve(unversioned);

            var resolved = m_resolver.resolve(withItemOrSpaceVersion);
            var resolvedWithBoth = m_resolver.resolve(withBoth);
            assertEquals(resolvedWithBoth, resolved,
                "Resolves to the same URL regardless whether one or both params set");
        if (version == null || version.isVersioned()) {
            assertNotEquals(resolvedPlain, resolved, "Should not ignore version parameter");
            assertNotEquals(resolvedPlain, resolvedWithBoth, "Should not ignore version parameter(s)");
            final var fromEither = URLResolverUtil.parseVersion(resolved.getQuery());
            assertEquals(version, fromEither.orElse(null),
                "Has correct version when specifying either query parameter");
            final var fromBoth = URLResolverUtil.parseVersion(resolvedWithBoth.getQuery());
            assertEquals(version, fromBoth.orElse(null), "Has correct version when specifying both query parameters");
        }
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeInScope()"
    })
    void testResolveWorkflowRelative(final URL unversioned, final URL withVersion, final URL withBoth,
        @SuppressWarnings("unused") final ItemVersion version) throws ResourceAccessException {
        assertThat(m_resolver.resolve(unversioned)).hasProtocol("file");

        assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(withVersion),
            "Must not contain version query parameter");
        assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(withBoth),
                "Must not contain version query parameters");
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()"
    })
    void testResolveNodeRelative(final URL unversioned, final URL withVersion, final URL withBoth,
        @SuppressWarnings("unused") final ItemVersion version) {
        final var exc = assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(unversioned));
        org.junit.Assert.assertEquals("Workflow must be saved before node-relative URLs can be used", exc.getLocalizedMessage());

        assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(withVersion),
            "Must not contain version query parameter");
        assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(withBoth),
                "Must not contain version query parameters");
    }

    /** Checks if space-relative knime-URLs are resolved correctly with version. */
    @Test
    void testResolveSpaceRelativeURLWithVersion() throws Exception { // NOSONAR rewrite tests using @ParameterizedTest in JUnit 5
        final var repoAddressUri = URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withHubJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID())
                    .withScope("test", "test")
                    .withJobCreator("test"))
                .withHubSpaceLocation(loc -> loc
                    .withRepositoryAddress(repoAddressUri)
                    .withWorkflowPath("/Users/john/Private/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12")
                    .withItemVersion(4))
                .build());

        // different item, so the workflow's item version is irrelevant
        assertResolvedURIEquals(resolver,
            URI.create(repoAddressUri.toString() + "/Users/john/Private/boss/test%20small.txt:data"),
            new URL("knime://knime.space/boss/test%20small.txt"));
        final var e = assertThrows(resolver::resolve, new URL("knime://knime.space/test/../../Public/stuff/test.txt"));
        assertTrue("Error should indicate that leaving the Hub space is not allowed.",
            e.getMessage().contains("Leaving the Hub space is not allowed"));
    }

    /** Checks if space-relative knime-URLs are resolved correctly without version. */
    @Test
    void testResolveSpaceRelativeURLWithoutVersion() throws Exception {
        final var repoAddressUri = URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withHubJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID())
                    .withScope("test", "test")
                    .withJobCreator("test"))
                .withHubSpaceLocation(loc -> loc
                    .withRepositoryAddress(repoAddressUri)
                    .withWorkflowPath("/Users/john/Private/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12"))
                .build());

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toUri().toURL(),
            new URL("knime://knime.workflow"));

        assertResolvedURIEquals(resolver,
            URI.create(repoAddressUri.toString() + "/Users/john/Private/boss/test%20small.txt:data"),
            new URL("knime://knime.space/boss/test%20small.txt"));
    }


    /**
     * Checks if workflow-relative knime-URLs do not leave the space.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveWorkflowRelativeURLDontLeaveSpace() throws Exception {
        final var repoAddressUri = URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withHubJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID())
                    .withScope("test", "test")
                    .withJobCreator("test"))
                .withHubSpaceLocation(loc -> loc
                    .withRepositoryAddress(repoAddressUri)
                    .withWorkflowPath("/Users/john/Private/folder/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12"))
                .build());
        assertThat(resolver).isOfAnyClassIn(HubExecutorUrlResolver.class);

        assertResolvedURIEquals(resolver,
            URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository/Users/john/Private/test.txt:data"),
            new URL("knime://knime.workflow/../../test.txt"));

        final var eA = assertThrows(resolver::resolve, new URL("knime://knime.workflow/../../../Public/test.txt"));
        assertTrue("Error should indicate that leaving the Hub space is not allowed, found " + eA,
            eA.getMessage().contains("Leaving the Hub space is not allowed for workflow relative URLs:"));

        final var eB = assertThrows(resolver::resolve, new URL("knime://knime.workflow/foo/../../../../test.txt"));
        assertTrue("Error should indicate that leaving the Hub space is not allowed, found " + eB,
            eB.getMessage().contains("Leaving the Hub space is not allowed for workflow relative URLs:"));
    }


    /**
     * Checks if space-relative knime-URLs do not leave the space.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveSpaceRelativeURLDontLeaveSpace() throws Exception {
        final var repoAddressUri = URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withHubJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID())
                    .withScope("test", "test")
                    .withJobCreator("test"))
                .withHubSpaceLocation(loc -> loc
                    .withRepositoryAddress(repoAddressUri)
                    .withWorkflowPath("/Users/john/Private")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12")
                    .withItemVersion(4))
                .build());
        assertThat(resolver).isOfAnyClassIn(HubExecutorUrlResolver.class);

        final var url = new URL("knime://knime.space/../test%20small.txt");
        final var e = assertThrows(resolver::resolve, url);
        assertTrue("Error should indicate that leaving the Hub space is not allowed.",
            e.getMessage().contains("Leaving the Hub space is not allowed for space relative URLs:"));
    }
}
