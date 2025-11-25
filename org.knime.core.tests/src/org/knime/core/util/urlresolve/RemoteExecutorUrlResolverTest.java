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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURLEquals;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.urlresolve.URLMethodSources.Context;
import org.knime.core.util.urlresolve.URLMethodSources.WorkspaceType;

/**
 * Tests for {@link RemoteExecutorUrlResolver}, currently only with a focus on item version handling.
 *
 * @author Manuel Hotz, KNIME AG, Zurich, Switzerland
 */
class RemoteExecutorUrlResolverTest {

    @Nested
    class Restrictions {
        /** Checks that a workflow-relative URI pointing to a resource within the workflow cannot be resolved. */
        @Test
        void testResolveWithinWorkflowRelativeURI() throws Exception {
            URL url = new URL("knime://knime.workflow/some where/inside.txt");
            URI mountpointUri = new URI("knime://knime-server-mountpoint/test"
                + "?exec=8443aad7-e59e-4be1-b31b-4b287f5bf466&name=test%2B2019-01-02%2B09.57.19");

            final var resolver = KnimeUrlResolver.getRemoteWorkflowResolver(mountpointUri, null);
            final var ex = assertThrows(resolver::resolve, url);
            assertTrue("Message should talk about resources not being accessible, found '" + ex.getMessage() + "'.",
                ex.getMessage()
                    .contains("Workflow relative URL points to a resource within a workflow. Not accessible."));
        }

        @ParameterizedTest
        @MethodSource({
            "org.knime.core.util.urlresolve.URLMethodSources#remoteHubExecutorContexts()",
            "org.knime.core.util.urlresolve.URLMethodSources#remoteServerExecutorContexts()"
        })
        void fileInsideRemoteWorkflowTest(final Context context, final String localMountId, final WorkspaceType type,
            @SuppressWarnings("unused") final String spacePath) throws Exception {

            final var resolver = context.getResolver(localMountId, type);
            final var workflowRelative = URI.create("knime://knime.workflow/data/../someDir/file.csv").toURL();
            final var e = assertThrows(resolver::resolve, workflowRelative);
            assertTrue("Should talk about lack of accessibility: '" + e.getMessage() + "'",
                e.getMessage().contains("accessible"));
        }
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
            final var mountpointUri = context.getMountpointURI().orElseThrow();
            final var loc = (HubSpaceLocationInfo)context.getLocationInfo();
            m_resolver = new RemoteExecutorUrlResolver(mountpointUri, loc);
        }

        /**
         * Checks if mountpoint-relative knime-URIs are correctly resolved to server mountpoint URIs.
         *
         * @throws Exception
         */
        @Test
        void testResolveMountpointRelativeURIToServerMountpointURI() throws Exception {
            final var url = new URL("knime://knime.mountpoint/some where/outside.txt");
            final var mountpointUri = new URI("knime://knime-server-mountpoint/test"
                + "?exec=8443aad7-e59e-4be1-b31b-4b287f5bf466&name=test%2B2019-01-02%2B09.57.19");

            final var resolver = KnimeUrlResolver.getRemoteWorkflowResolver(mountpointUri, null);
            final var expectedUrl = new URL("knime://knime-server-mountpoint/some%20where/outside.txt");
            assertResolvedURLEquals(resolver, expectedUrl, url);
        }

        /**
         * Checks if workflow-relative knime-URIs are correctly resolved to server mountpoint URIs.
         *
         * @throws Exception
         */
        @Test
        void testResolveWorkfowRelativeURIToServerMountpointURI() throws Exception {
            final var url = new URL("knime://knime.workflow/../some where/outside.txt");
            final var mountpointUri = new URI("knime://knime-server-mountpoint/test"
                + "?exec=8443aad7-e59e-4be1-b31b-4b287f5bf466&name=test%2B2019-01-02%2B09.57.19");

            final var resolver = KnimeUrlResolver.getRemoteWorkflowResolver(mountpointUri, null);
            final var expectedUrl = new URL("knime://knime-server-mountpoint/some%20where/outside.txt");
            assertResolvedURLEquals(resolver, expectedUrl, url);
        }

        @ParameterizedTest
        @MethodSource({
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointAbsolute()",
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()",
            "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()"
        })
        void testVersionedUrls(final URL unversioned, final URL versioned, final URL bothVersions,
                final ItemVersion version) throws ResourceAccessException {
            final var resolvedPlain = m_resolver.resolve(unversioned);
            for (final var url : new URL[] { versioned, bothVersions }) {
                final var resolved = m_resolver.resolve(url);
                final var isVersioned = version.isVersioned();
                if (isVersioned) {
                    assertNotEquals(resolvedPlain, resolved, "Version should not be ignored");
                    final var fromUrl = URLResolverUtil.parseVersion(resolved.getQuery());
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
            final var mountpointUri = URI.create("knime://My-Knime-Hub/workflow");
            m_resolver = new RemoteExecutorUrlResolver(mountpointUri, null);
        }

        @ParameterizedTest
        @MethodSource({
            // mountpoint-absolute URLs can specify a version on Server
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointAbsolute()",
        })
        void testVersionedUrls(final URL unversioned, final URL versioned, final URL bothVersions,
                final ItemVersion version) throws ResourceAccessException {
            final var resolvedPlain = m_resolver.resolve(unversioned);
            for (final var url : new URL[] { versioned, bothVersions }) {
                final var resolved = m_resolver.resolve(url);
                if (version.isVersioned()) {
                    assertNotEquals(resolvedPlain, resolved, "Version should not be ignored");
                    final var fromUrl = URLResolverUtil.parseVersion(resolved.getQuery());
                    assertEquals(version, fromUrl.orElseThrow(), "Has correct version");
                }

            }
        }

        @ParameterizedTest
        @MethodSource({
            // no Mountpoint-, workflow- and space-relative KNIME URLs with version on Server
            "org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()",
            "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()",
            // Node relative URLs cannot be resolved from pure remote wfs
            "org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()",
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
