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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURIEquals;
import static org.knime.core.util.urlresolve.KnimeUrlResolverTest.assertResolvedURLEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.contextv2.ServerJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;
/**
 * Tests for {@link ServerExecutorUrlResolver}, currently only with a focus on item version handling.
 *
 * @author Manuel Hotz, KNIME AG, Zurich, Switzerland
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
class ServerExecutorUrlResolverTest {

    private static ServerExecutorUrlResolver m_resolver;

    // before all tests create a new workflow context
    // the workflow context is a singleton, so we need to create a new one for each test
    @BeforeAll
    static void createResolver() throws URISyntaxException {
        final var baseUri = new URI("http://localhost:8080/knime");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(baseUri)
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub"))
                .build();

        m_resolver = new ServerExecutorUrlResolver((ServerJobExecutorInfo)context.getExecutorInfo(),
            (ServerLocationInfo)context.getLocationInfo());
    }

    /** Check if URLs with a remote mount point are resolved correctly. */
    @Test
    void testResolveRemoteMountpointURL() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://localhost:8080/knime"))
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Server"))
                .build());
        assertThat(resolver).isOfAnyClassIn(ServerExecutorUrlResolver.class);

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toUri().toURL(),
            new URL("knime://knime.workflow"));

        assertResolvedURIEquals(resolver,
            new URI(URI.create("https://localhost:8080/knime").toString() + "/some%20where/outside.txt:data"),
            new URL("knime://Server/some where/outside.txt"));
    }

    /** Checks if workflow-relative knime-URLs are resolved correctly to server addresses. */
    @Test
    void testResolveWorkflowRelativeToServer() throws Exception {
        // original location == current location
        final var baseUri = new URI("http://localhost:8080/knime");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(baseUri)
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Testserver"))
                .build());
        assertThat(resolver).isOfAnyClassIn(ServerExecutorUrlResolver.class);

        // links inside the workflow must stay local links to the workflow copy
        assertResolvedURIEquals(resolver,
            new URI(currentLocation.toUri().toString() + "/workflow.knime").normalize(),
            new URL("knime://knime.workflow/workflow.knime"));

        // path outside the workflow
        assertResolvedURIEquals(resolver,
            new URI(baseUri.toString() + "/test%201.txt:data"),
            new URL("knime://knime.workflow/../test 1.txt"));

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toUri().toURL(),
            new URL("knime://knime.workflow"));
    }


    /** Checks if German special characters in a remote work flow relative to server URL are encoded. */
    @Test
    void testRemoteWorkflowRelativeURLIsEncoded() throws Exception {
        final var baseUri = new URI("http://localhost:8080/knime");

        // original location == current location
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(baseUri)
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Server"))
                .build());

        assertResolvedURIEquals("Unexpected resolved umlaut URL", resolver,
            new URI(baseUri.toString() + "/test%C3%9C.txt:data"),
            new URL("knime://knime.workflow/../testÜ.txt"));
    }

    /** Checks if German special characters in a remote mount point resolved URL are encoded. */
    @Test
    void testRemoteMountpointURLIsEncoded() throws Exception {
        // Mountpoint is never considered in the old code path, and it makes little sense here IMHO
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://localhost:8080/knime"))
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Server"))
                .build());

        assertResolvedURIEquals(resolver,
            new URI(URI.create("https://localhost:8080/knime").toString() + "/r%C3%B6w0.json:data").normalize(),
            new URL("knime://knime.workflow/../röw0.json"));
    }

    /**
     * Checks if mountpoint-relative knime-URLs are resolved correctly to server addresses.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveMountpointRelativeToServer() throws Exception {
        // original location == current location
        final var baseUri = URI.create("http://localhost:8080/knime");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(baseUri)
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("TestServer"))
                .build());
        assertThat(resolver).isOfAnyClassIn(ServerExecutorUrlResolver.class);

        assertResolvedURIEquals(resolver,
            new URI(baseUri.toString() + "/some%20where/outside.txt:data"),
            new URL("knime://knime.mountpoint/some where/outside.txt"));
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeInScope()",
        "org.knime.core.util.urlresolve.URLMethodSources#mountpointAbsolute()",
        "org.knime.core.util.urlresolve.URLMethodSources#mountpointRelative()",
        "org.knime.core.util.urlresolve.URLMethodSources#workflowRelativeLeavingScope()",
        "org.knime.core.util.urlresolve.URLMethodSources#spaceRelative()"
    })
    void testResolveForbidden(@SuppressWarnings("unused") final URL unversioned, final URL withVersion,
            final URL withBoth, @SuppressWarnings("unused") final ItemVersion version) {
        for (final var url : new URL[] { withVersion, withBoth }) {
            final var ex = assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(url),
                    "Must not contain version query parameter");
            assertEquals("KNIME URLs on a KNIME Server cannot specify an item version.", ex.getMessage());
        }
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#nodeRelativeInScope()"
    })
    void testResolveNodeRelativeNoItemVersion(@SuppressWarnings("unused") final URL unversioned, final URL withVersion,
            final URL withBoth, @SuppressWarnings("unused") final ItemVersion version) {
        for (final var url : new URL[] { withVersion, withBoth }) {
            final var ex = assertThrows(ResourceAccessException.class, () -> m_resolver.resolve(url),
                    "Must not contain version query parameter");
            assertTrue(ex.getMessage().startsWith("Node-relative KNIME URLs cannot specify an item version"),
                "Must not contain item version");
        }
    }
}
