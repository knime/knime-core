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
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURIEquals;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURLEquals;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertThrows;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;
import org.knime.core.util.urlresolve.URLMethodSources.Context;
import org.knime.core.util.urlresolve.URLMethodSources.WorkspaceType;
/**
 * Tests for {@link AnalyticsPlatformTempCopyUrlResolver}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Manuel Hotz, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
class AnalyticsPlatformTempCopyUrlResolverTest {

    private static AnalyticsPlatformTempCopyUrlResolver m_resolver;

    // before all tests create a new workflow context
    // the workflow context is a singleton, so we need to create a new one for each test
    @BeforeAll
    static void createResolver() {
        final var repoAddressUri = URI.create("https://api.example.com:443/knime/rest/v4/repository");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("My-Knime-Hub", currentLocation.getParent()))
                .withHubSpaceLocation(loc -> loc
                    .withRepositoryAddress(repoAddressUri)
                    .withWorkflowPath("/Users/john/Private/folder/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12"))
                .build();
        m_resolver = new AnalyticsPlatformTempCopyUrlResolver(
            (AnalyticsPlatformExecutorInfo)context.getExecutorInfo(), (RestLocationInfo)context.getLocationInfo(),
            context.getMountpointURI().orElseThrow());
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

    /** Checks if knime-URLs are correctly resolved for temporary copies of workflows. */
    @Test
    void testResolveInTemporaryCopy() throws Exception {
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://localhost:1234/repository"))
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Server"))
                .build());
        assertThat(resolver).isOfAnyClassIn(AnalyticsPlatformTempCopyUrlResolver.class);

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toUri().toURL(),
            new URL("knime://knime.workflow"));

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL outside workflow", resolver,
            new URL("knime://LOCAL/test.txt"),
            new URL("knime://knime.workflow/../test.txt"));

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL inside workflow", resolver,
            currentLocation.resolve("test.txt").toUri().toURL(),
            new URL("knime://knime.workflow/test.txt"));

        assertResolvedURLEquals("Unexpected resolved mountpoint-relative URL", resolver,
            new URL("knime://LOCAL/xxx/test.txt"),
            new URL("knime://knime.mountpoint/xxx/test.txt"));

        assertResolvedURLEquals("Unexpected resolved absolute URL in same mount point", resolver,
            new URL("knime://LOCAL/yyy/test.txt"),
            new URL("knime://LOCAL/yyy/test.txt"));

        assertResolvedURLEquals("Unexpected resolved absolute URL in other mount point", resolver,
            new URL("knime://Some-Other-Server/test.txt"),
            new URL("knime://Some-Other-Server/test.txt"));
    }
    /** Checks if space-relative get redirected to mountpoint-relative knime-URLs. */
    @Test
    void testResolveSpaceRelativeURLInTempCopy() throws Exception {
        final var localMountId = "My-Knime-Hub";
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint(localMountId, currentLocation.getParent()))
                .withHubSpaceLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository"))
                    .withWorkflowPath("/Users/john doe/Private/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("KNIME-Community-Hub")
                    .withSpace("/Users/john doe/Private", "*11")
                    .withWorkflowItemId("*12")
                    .withItemVersion(4))
                .build());

        assertResolvedURIEquals(resolver,
            URI.create("knime://" + localMountId + "/Users/john%20doe/Private/Foo/test.txt"),
            new URL("knime://knime.space/Foo/test.txt"));

        final var ex1 = assertThrows(resolver::resolve, new URL("knime://knime.space/workflow/data/test.txt"));
        assertTrue("'" + ex1.getMessage() + "' should talk about diallowing access.",
            ex1.getMessage().contains("Accessing the current workflow's contents is not allowed"));

        final var ex2 = assertThrows(resolver::resolve,
            new URL("knime://knime.space/Foo/../../OtherSpace/Bar/test.txt"));
        assertTrue("'" + ex2.getMessage() + "' should talk about leaving the space.",
            ex2.getMessage().contains("Leaving the Hub space is not allowed for space relative URLs"));
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#knwfContexts()"
    })
    void neighborWorkflowTestKnwf(final Context context, final String localMountId, final WorkspaceType type,
        @SuppressWarnings("unused") final String spacePath) throws Exception {

        final var resolver = context.getResolver(localMountId, type);
        final var expected = new HashMap<>(Map.of(
            KnimeUrlType.MOUNTPOINT_RELATIVE, new URL("knime://knime.mountpoint/group/workflow2"),
            KnimeUrlType.HUB_SPACE_RELATIVE, new URL("knime://knime.space/group/workflow2"),
            KnimeUrlType.WORKFLOW_RELATIVE, new URL("knime://knime.workflow/../workflow2")));

        for (final var initial : expected.entrySet()) {
            final var e = assertThrows(resolver::changeLinkType, initial.getValue());
            assertTrue("Exception should talk about missing mountpoint: " + e.getMessage(),
                e.getMessage().contains("needs a mountpoint") || e.getMessage().contains("not imported into a space"));
        }
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeInScope()"
    })
    void testResolveWorkflowRelativeIsFile(final URL unversioned)
            throws ResourceAccessException {
        assertThat(m_resolver.resolve(unversioned)).hasProtocol("file");
    }

    @Nested
    class Versioning {
        @ParameterizedTest
        @MethodSource({
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()"
        })
        void testResolveRelativeItemVersion(final URL unversioned, final URL withVersion, final URL bothVersions,
                final HubItemVersion version) throws ResourceAccessException, MalformedURLException {
            var resolvedWithoutVersion = m_resolver.resolve(unversioned);
            for (final var url : new URL[] { withVersion, bothVersions }) {
                var resolved = m_resolver.resolve(url);
                if (version.isVersioned()) {
                    assertNotEquals(resolvedWithoutVersion, resolved, "Version should not be ignored.");
                }
            }
        }

        @ParameterizedTest
        @MethodSource({
            "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeInScope()"
        })
        void testResolveWorkflowRelativeNeverVersioned(final URL unversioned, final URL withVersion, final URL bothVersions)
                throws ResourceAccessException {
            assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(withVersion),
                "Should not be able to resolve with version");
            assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(bothVersions),
                "Should not be able to resolve with versions");
        }

        @ParameterizedTest
        @MethodSource({
            "org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()"
        })
        void testResolveNodeRelativeNeverVersioned(final URL unversioned, final URL withVersion, final URL bothVersions)
                throws ResourceAccessException {
            final var exc = assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(unversioned));
            assertEquals("Workflow must be saved before node-relative URLs can be used", exc.getLocalizedMessage());

            assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(withVersion),
                "Should not be able to resolve with version");
            assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(bothVersions),
                "Should not be able to resolve with versions");
        }
    }
}
