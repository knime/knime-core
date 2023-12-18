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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.HubItemVersion;

/**
 * Tests for {@link RemoteExecutorUrlResolver}, currently only with a focus on item version handling.
 *
 * @author Manuel Hotz, KNIME AG, Zurich, Switzerland
 */
class RemoteExecutorUrlResolverTest {

    private Set<NodeID> m_staticWFMs;

    @BeforeEach
    void setupEach() {
        m_staticWFMs =
                WorkflowManager.ROOT.getNodeContainers().stream().map(NodeContainer::getID).collect(Collectors.toSet());
    }

    @AfterEach
    void popNodeContext() {
        NodeContext.removeLastContext();

        Collection<NodeID> workflows = WorkflowManager.ROOT.getNodeContainers().stream().map(nc -> nc.getID())
                .filter(id -> !m_staticWFMs.contains(id)).collect(Collectors.toList());
        workflows.stream().forEach(id -> WorkflowManager.ROOT.removeProject(id));
    }

    private static void pushNodeContext(final WorkflowContextV2 context) {
        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);
    }

    @Nested
    @DisplayName("Remote Executor w/ HubSpaceLocation")
    class WithHubSpaceLocation {

        private static RemoteExecutorUrlResolver m_resolver;

        // Remember the WFMS that were known before any test ran. Don't touch them on {@link #cleanup()}.
        @BeforeEach
        void createContext() throws URISyntaxException {
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
            final var loc = (HubSpaceLocationInfo)context.getLocationInfo();
            m_resolver = new RemoteExecutorUrlResolver(loc);

            pushNodeContext(context);
        }

        @ParameterizedTest
        @MethodSource({
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointAbsolute()",
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()",
            "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()"
        })
        void testVersionedUrls(final URL unversioned, final URL versioned, final URL bothVersions,
                final HubItemVersion version) throws ResourceAccessException {
            final var resolvedPlain = m_resolver.resolve(unversioned);
            for (final var url : new URL[] { versioned, bothVersions }) {
                final var resolved = m_resolver.resolve(url);
                final var isVersioned = !HubItemVersion.currentState().equals(version);
                if (isVersioned) {
                    assertNotEquals(resolvedPlain, resolved, "Version should not be ignored");
                    final var fromUrl = HubItemVersion.of(resolved);
                    assertEquals(version, fromUrl.orElseThrow(), "Has correct version");
                }
            }
        }

        @ParameterizedTest
        @MethodSource({
            // Node relative URLs cannot be resolved from pure remote wfs
            "org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()"
        })
        void testNoVersioningPossible(@SuppressWarnings("unused") final URL unversioned, final URL versioned,
                final URL bothVersions) {
            assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(versioned),
                    "URL with version should not resolve");
            assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(bothVersions),
                    "URL with versions should not resolve");
        }
    }

    @Nested
    @DisplayName("Remote Executor w/ ServerLocation")
    class WithServerLocation {
        private static RemoteExecutorUrlResolver m_resolver;

        @BeforeEach
        void createContext() throws URISyntaxException {
            // original location == current location
            final var baseUri = new URI("http://localhost:8080/knime");
            final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

            final var context = WorkflowContextV2.builder()
                    .withAnalyticsPlatformExecutor(exec -> exec
                        .withCurrentUserAsUserId()
                        .withLocalWorkflowPath(currentLocation)
                        .withMountpoint("My-Knime-Hub", currentLocation.getParent()))
                    .withServerLocation(loc -> loc
                        .withRepositoryAddress(baseUri)
                        .withWorkflowPath("/workflow")
                        .withAuthenticator(new SimpleTokenAuthenticator("token"))
                        .withDefaultMountId("My-Knime-Hub"))
                    .build();
            final var loc = (ServerLocationInfo)context.getLocationInfo();
            m_resolver = new RemoteExecutorUrlResolver(loc);

            pushNodeContext(context);
        }

        // workflow-relative and mountpoint-absolute URLs can specify a version on Server, but it is ignored later
        @ParameterizedTest
        @MethodSource({
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointAbsolute()",
            "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()",
        })
        void testVersionedUrls(final URL unversioned, final URL versioned, final URL bothVersions,
                final HubItemVersion version) throws ResourceAccessException {
            final var resolvedPlain = m_resolver.resolve(unversioned);
            for (final var url : new URL[] { versioned, bothVersions }) {
                final var resolved = m_resolver.resolve(url);
                if (!HubItemVersion.currentState().equals(version)) {
                    assertNotEquals(resolvedPlain, resolved, "Version should bot be ignored");
                    final var fromUrl = HubItemVersion.of(resolved);
                    assertEquals(version, fromUrl.orElseThrow(), "Has correct version");
                }

            }
        }

        @ParameterizedTest
        @MethodSource({
            // no Mountpoint- and space-relative KNIME URLs with version on Server
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()",
            // Node relative URLs cannot be resolved from pure remote wfs
            "org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()"
        })
        void testNoVersioningPossibleOnServer(@SuppressWarnings("unused") final URL unversioned, final URL versioned,
                final URL bothVersions) {
            assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(versioned),
                    "URL with version should not resolve");
            assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(bothVersions),
                    "URL with versions should not resolve");
        }
    }

}
