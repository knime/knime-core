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
 *   Feb 16, 2024 (leonard.woerteler): created
 */
package org.knime.core.util.urlresolve;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.Functions;
import org.eclipse.core.runtime.Platform;
import org.junit.Assume;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.URIPathEncoder;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.urlresolve.URLMethodSources.Context;
import org.knime.core.util.urlresolve.URLMethodSources.WorkspaceType;

/**
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
class KnimeUrlResolverTest {

    /**
     * Checks if wrong protocols are rejected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testWrongProtocol() throws Exception {
        final var e = assertThrows(KnimeUrlResolver.getResolver(null)::resolve,
            new URL("http://www.knime.com/"));
        assertTrue("Error should indicate that the protocol is not `knime:`, found '" + e.getMessage() + "'.",
            e.getMessage().contains("not a valid KNIME URL"));
    }

    /**
     * Checks if a missing {@link WorkflowContextV2} is handled correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testNoWorkflowContext() throws Exception {
        final var e = assertThrows(KnimeUrlResolver.getResolver(null)::resolve,
            new URL("knime://knime.workflow/workflow.knime"));
        assertTrue("Error should indicate that no workflow context is set, found '" + e.getMessage() + "'.",
            e.getMessage().contains("No context"));
    }

    /**
     * Checks if workflow-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveWorkflowRelativeLocal() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build());

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toUri().toURL(),
            new URL("knime://knime.workflow"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.resolve("workflow.knime").toUri(),
            new URL("knime://knime.workflow/workflow.knime"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.resolve("..").resolve("test.txt").toUri().normalize(),
            new URL("knime://knime.workflow/../test.txt"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.resolve("..").resolve("With Space+Plus.txt").toUri().normalize(),
            new URL("knime://knime.workflow/../With Space+Plus.txt"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.resolve("..").resolve("With Space+Plus.txt").toUri().normalize(),
            new URL("knime://knime.workflow/../With%20Space+Plus.txt"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.resolve("Double Slash Decoded").toUri(),
            new URL("knime://knime.workflow//Double Slash Decoded"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.resolve("..").resolve("Double Slash Encoded").toUri().normalize(),
            new URL("knime://knime.workflow/..//Double%20Slash%20Encoded"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.resolve("..").resolve("Double Slash Decoded").toUri().normalize(),
            new URL("knime://knime.workflow/..//Double Slash Decoded"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.resolve("Double Slash Encoded").toUri(),
            new URL("knime://knime.workflow//Double%20Slash%20Encoded"));
    }

    /**
     * Checks that workflow-relative knime-URLs do not leave the mount point.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveWorkflowRelativeObeyMountpointRoot() throws Exception {
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build());

        final var e = assertThrows(resolver::resolve, new URL("knime://knime.workflow/../../test.txt"));
        assertTrue("Error should indicate that leaving the mountpoint is not allowed.",
            e.getMessage().contains("Leaving the mount point is not allowed"));
    }

    /**
     * Checks if workflow-relative knime-URLs are resolved correctly to server addresses.
     *
     * @throws Exception if an error occurs
     */
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

        // links inside the workflow must stay local links to the workflow copy
        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            new URI(currentLocation.toUri().toString() + "/workflow.knime").normalize(),
            new URL("knime://knime.workflow/workflow.knime"));

        // path outside the workflow
        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            new URI(baseUri.toString() + "/test%201.txt:data"),
            new URL("knime://knime.workflow/../test 1.txt"));

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toUri().toURL(),
            new URL("knime://knime.workflow"));
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

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
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
     * Checks if mountpoint-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveMountPointRelativeLocal() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build());

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.getParent().resolve("test.txt").toUri(),
            new URL("knime://knime.mountpoint/test.txt"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.getParent().resolve("With Space+Plus.txt").toUri(),
            new URL("knime://knime.mountpoint/With Space+Plus.txt"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.getParent().resolve("With Space+Plus.txt").toUri(),
            new URL("knime://knime.mountpoint/With%20Space+Plus.txt"));
    }


   /**
     * Checks if mountpoint-relative knime-URLs that reside on an UNC drive are resolved correctly (see AP-7427).
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveMountpointRelativeLocalUNC() throws Exception {
        Assume.assumeTrue(Platform.OS_WIN32.equals(Platform.getOS()));

        final File currentLocation = new File("\\\\server\\repo\\workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation.toPath())
                    .withMountpoint("LOCAL", currentLocation.getParentFile().toPath()))
                .withLocalLocation()
                .build());

        final var url = new URL("knime://knime.mountpoint/test.txt");
        final var expectedPath = new File(currentLocation.getParentFile(), "test.txt");
        final var resolved = resolver.resolve(url);
        assertEquals("Unexpected resolved URL", expectedPath.toURI(), resolved.toURI());
        assertEquals("Unexpected resulting file", expectedPath, new File(resolved.toURI()));
    }

    /**
     * Checks that mountpoint-relative knime-URLs do not leave the mount point.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveMountpointRelativeObeyMountpointRoot() throws Exception {
        // original location == current location
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build());

        final var e = assertThrows(resolver::resolve, new URL("knime://knime.mountpoint/../test.txt"));
        assertTrue("Error should indicate that leaving the mountpoint is not allowed.",
            e.getMessage().contains("Leaving the mount point is not allowed"));
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

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            new URI(baseUri.toString() + "/some%20where/outside.txt:data"),
            new URL("knime://knime.mountpoint/some where/outside.txt"));
    }




    /**
     * Checks if workflow-relative knime-URLs that reside on a UNC drive are resolved correctly (see AP-7427).
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveWorkflowRelativeLocalUNC() throws Exception {
        Assume.assumeTrue(Platform.OS_WIN32.equals(Platform.getOS()));

        final File currentLocation = new File("\\\\server\\repo\\workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation.toPath())
                    .withMountpoint("LOCAL", currentLocation.getParentFile().toPath()))
                .withLocalLocation()
                .build());

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            new File(currentLocation, "workflow.knime").toURI(),
            new URL("knime://knime.workflow/workflow.knime"));

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toURI().toURL(),
            new URL("knime://knime.workflow"));

        // path outside the workflow
        final var url = new URL("knime://knime.workflow/../test.txt");
        final var expectedPath = new File(currentLocation.getParentFile(), "test.txt");
        final var resolved = resolver.resolve(url);
        assertEquals("Unexpected resolved URL", expectedPath.toURI(), resolved.toURI());
        assertEquals("Unexpected resulting file", expectedPath, new File(resolved.toURI()));
    }


    /**
     * Check if URLs with a local mount point are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveLocalMountpointURL() throws Exception {
        final var url = new URL("knime://LOCAL/test.txt");
        final var resolved = KnimeUrlResolver.getResolver(null).resolve(url);
        assertEquals("Unexpected protocol", "knime", resolved.getProtocol());
    }

    /**
     * Check if URLs with a remote mount point are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
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

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL pointing to workflow root", resolver,
            currentLocation.toUri().toURL(),
            new URL("knime://knime.workflow"));

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            new URI(URI.create("https://localhost:8080/knime").toString() + "/some%20where/outside.txt:data"),
            new URL("knime://Server/some where/outside.txt"));
    }


    /**
     * Checks if node-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
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
            assertResolvedURIEquals("Unexpected resolved URL", resolver,
                currentLocation.resolve("test.txt").toUri(),
                new URL("knime://knime.node/test.txt"));

            // Check proper handling of potentially encoded paths (see AP-17103).
            assertResolvedURIEquals("Unexpected resolved URL", resolver,
                currentLocation.resolve("With Space+Plus.txt").toUri(),
                new URL("knime://knime.node/With Space+Plus.txt"));

            assertResolvedURIEquals("Unexpected resolved URL", resolver,
                currentLocation.resolve("With Space+Plus.txt").toUri(),
                new URL("knime://knime.node/With%20Space+Plus.txt"));
        });
    }

    /**
     * Checks that node-relative knime-URLs only work on saved workflows.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveNodeRelativeLocalNotSaved() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build());

        withNodeContext(null, () -> {
            final var e = assertThrows(resolver::resolve, new URL("knime://knime.node/test.txt"));
            assertTrue("Error should indicate that the workflow must be saved.",
                e.getMessage().contains("Workflow must be saved"));
        });
    }

    /**
     * Checks if node-relative knime-URLs do not leave the workflow.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveNodeRelativeLocalDontLeaveWorkflow() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build());

        withNodeContext(currentLocation, () -> {
            final var e = assertThrows(resolver::resolve, new URL("knime://knime.node/../test.txt"));
            assertTrue("Error should indicate that leaving the workflow is not allowed.",
                e.getMessage().contains("Leaving the workflow is not allowed"));
        });
    }

    /**
     * Checks if space-relative knime-URLs are resolved correctly with version.
     *
     * @throws Exception if an error occurs
     */
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
        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            URI.create(repoAddressUri.toString() + "/Users/john/Private/boss/test%20small.txt:data"),
            new URL("knime://knime.space/boss/test%20small.txt"));
        final var e = assertThrows(resolver::resolve, new URL("knime://knime.space/test/../../Public/stuff/test.txt"));
        assertTrue("Error should indicate that leaving the Hub space is not allowed.",
            e.getMessage().contains("Leaving the Hub space is not allowed"));
    }

    /**
     * Checks if space-relative knime-URLs are resolved correctly without version.
     *
     * @throws Exception if an error occurs
     */
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

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            URI.create(repoAddressUri.toString() + "/Users/john/Private/boss/test%20small.txt:data"),
            new URL("knime://knime.space/boss/test%20small.txt"));
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

        final var url = new URL("knime://knime.space/../test%20small.txt");
        final var e = assertThrows(resolver::resolve, url);
        assertTrue("Error should indicate that leaving the Hub space is not allowed.",
            e.getMessage().contains("Leaving the Hub space is not allowed for space relative URLs:"));
    }

    /**
     * Checks if space-relative get redirected to mountpoint-relative knime-URLs.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testResolveSpaceRelativeURLToMountPointRelativeLocal() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build());

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            currentLocation.getParent().resolve("test.txt").toUri(),
            new URL("knime://knime.space/test.txt"));
    }

    /**
     * Checks if space-relative get redirected to mountpoint-relative knime-URLs.
     *
     * @throws Exception if an error occurs
     */
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

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
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

    /**
     * Checks if German special characters in a local work flow relative URL are UTF-8 encoded.
     *
     * @throws Exception
     */
    @Test
    void testWorkflowRelativeURLIsEncoded() throws Exception {
        final var mountpointRoot = KNIMEConstants.getKNIMETempPath().resolve("root");
        final var currentLocation = mountpointRoot.resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", mountpointRoot))
                .withLocalLocation()
                .build());

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            // hack to remove unwanted encoding of %!
            new URI(currentLocation.resolve("workfl%C3%B6w.knime").toUri().toString().replace("25", "")),
            new URL("knime://knime.workflow/workflöw.knime"));
    }

    /**
     * Checks if German special characters in a local mount point relative URL are UTF-8 encoded.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testLocalMountPointRelativeURLIsEncoded() throws Exception {
        final var mountpointRoot = KNIMEConstants.getKNIMETempPath().resolve("root");
        final var currentLocation = mountpointRoot.resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", mountpointRoot))
                .withLocalLocation()
                .build());

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            // hack to remove unwanted encoding of %!
            new URI(mountpointRoot.resolve("test%C3%96.txt").toUri().toString().replace("25", "")),
            new URL("knime://knime.mountpoint/testÖ.txt"));
    }

    /**
     * Checks if German special characters in a local node relative URL are UTF-8 encoded.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testLocalNodeRelativeURLIsEncoded() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build());

        withNodeContext(currentLocation, () -> {
            assertResolvedURIEquals("Unexpected resolved URL", resolver,
                // hack to remove unwanted encoding of %!
                new URI(currentLocation.resolve("test%C3%96.txt").toUri().toString().replace("25", "")),
                new URL("knime://knime.node/testÖ.txt"));
        });
    }

    /**
     * Checks if German special characters in a remote mount point resolved URL are encoded.
     *
     * @throws Exception
     */
    @Test
    void testRemoteMountpointURLIsEncoded() throws Exception {
        // Mountpoint is never considered in the old code path, and it makes little sense here IMHO
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID())
                    .withIsRemote(false))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://localhost:8080/knime"))
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Server"))
                .build());

        assertResolvedURIEquals("Unexpected resolved URL", resolver,
            new URI(URI.create("https://localhost:8080/knime").toString() + "/r%C3%B6w0.json:data").normalize(),
            new URL("knime://knime.workflow/../röw0.json"));
    }

    /**
     * Checks if German special characters in a remote work flow relative to server URL are encoded.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testRemoteWorkflowRelativeURLIsEncoded() throws Exception {
        final var baseUri = new URI("http://localhost:8080/knime");

        // original location == current location
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var resolver = KnimeUrlResolver.getResolver(WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID())
                    .withIsRemote(false))
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

    /**
     * Checks if knime-URLs are correctly resolved for temporary copies of workflows.
     *
     * @throws Exception if an error occurs
     */
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
        assertResolvedURLEquals("Unexpected resolved URL", resolver, expectedUrl, url);
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
        assertResolvedURLEquals("Unexpected resolved URL", resolver, expectedUrl, url);
    }

    /**
     * Checks that a workflow-relative URI pointing to a resource within the workflow cannot be resolved.
     *
     * @throws Exception
     */
    @Test
    void testResolveWithinWorkflowRelativeURIFails() throws Exception {
        URL url = new URL("knime://knime.workflow/some where/inside.txt");
        URI mountpointUri = new URI("knime://knime-server-mountpoint/test"
            + "?exec=8443aad7-e59e-4be1-b31b-4b287f5bf466&name=test%2B2019-01-02%2B09.57.19");

        final var resolver = KnimeUrlResolver.getRemoteWorkflowResolver(mountpointUri, null);
        final var ex = assertThrows(resolver::resolve, url);
        assertTrue("Message should talk about resources not being accessible, found '" + ex.getMessage() + "'.",
            ex.getMessage().contains("Workflow relative URL points to a resource within a workflow. Not accessible."));
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#localApContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyHubContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyServerContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#hubExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#serverExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#remoteHubExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#remoteServerExecutorContexts()"
    })
    void neighborWorkflowTest(final Context context, final String localMountId, final WorkspaceType type,
        final String spacePath) throws Exception {

        final var resolver = context.getResolver(localMountId, type);
        final var expected = new HashMap<>(Map.of(
            KnimeUrlType.MOUNTPOINT_RELATIVE, new URL("knime://knime.mountpoint/group/workflow2"),
            KnimeUrlType.HUB_SPACE_RELATIVE, new URL("knime://knime.space/group/workflow2"),
            KnimeUrlType.WORKFLOW_RELATIVE, new URL("knime://knime.workflow/../workflow2"),
            // must include properly encoded space path
            KnimeUrlType.MOUNTPOINT_ABSOLUTE, URIPathEncoder.UTF_8.encodePathSegments(
                new URI(KnimeUrlType.SCHEME, localMountId, spacePath + "/group/workflow2", null)).toURL()));

        for (final var initial : expected.entrySet()) {
            final var initialUrl = initial.getValue();
            final var resolvedUrl = resolver.resolveInternal(initialUrl).orElseThrow();
            Optional.ofNullable(resolvedUrl.path()).ifPresent(p -> assertFalse(p.isAbsolute()));
            Optional.ofNullable(resolvedUrl.pathInsideWorkflow()).ifPresent(p -> assertFalse(p.isAbsolute()));

            final var converted = resolver.changeLinkType(initialUrl);
            for (final var e : expected.entrySet()) {
                final var urlType = e.getKey();
                final var expectedUrl = e.getValue();
                assertEquals("Converting from " + initial.getKey() + " to " + urlType,
                    expectedUrl, converted.get(urlType));
            }
        }
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
        "org.knime.core.util.urlresolve.URLMethodSources#localApContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#knwfContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyHubContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyServerContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#hubExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#serverExecutorContexts()"
    })
    void fileInsideWorkflowTest(final Context context, final String localMountId, final WorkspaceType type,
        final String spacePath) throws Exception {

        final var resolver = context.getResolver(localMountId, type);

        final var path = Path.of((type == WorkspaceType.UNC_PATH ? "//UncMount/Share" : "")
            + "/Wörk – ßpäce/group/workflow/someDir/file.csv").toAbsolutePath();
        final var expectedUrl = URLResolverUtil.toURL(path);

        final var workflowRelative = URI.create("knime://knime.workflow/data/../someDir/file.csv").toURL();
        final var resolvedUrl = resolver.resolveInternal(workflowRelative).orElseThrow();
        if (resolvedUrl.path() != null) {
            assertEquals((spacePath.isEmpty() ? "" : spacePath.substring(1) + "/") + "group/workflow",
                resolvedUrl.path().toString());
        }
        assertEquals("someDir/file.csv", resolvedUrl.pathInsideWorkflow().toString());
        assertEquals(localMountId, resolvedUrl.mountID());
        assertFalse(resolvedUrl.cannotBeRelativized());

        assertResolvedURLEquals("Should resolve to a file in the executor's file system.", resolver,
            expectedUrl, workflowRelative);
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

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#localApContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#knwfContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyHubContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyServerContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#hubExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#serverExecutorContexts()",
    })
    void testRootOfCurrentWorkflow(final Context context, final String localMountId, final WorkspaceType type,
        @SuppressWarnings("unused") final String spacePath) throws Exception {

        final var resolver = context.getResolver(localMountId, type);

        final var path = Path.of((type == WorkspaceType.UNC_PATH ? "//UncMount/Share" : "")
            + "/Wörk – ßpäce/group/workflow").toAbsolutePath();
        final var expectedUrl = URLResolverUtil.toURL(path);

        final var resolved = resolver.resolve(new URL("knime://knime.workflow"));
        assertEquals(expectedUrl, resolved);
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#localApContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#knwfContexts()"
    })
    void testRootOfCurrentWorkflowLocal(final Context context, final String localMountId, final WorkspaceType type,
        @SuppressWarnings("unused") final String spacePath) throws Exception {

        final var resolver = context.getResolver(localMountId, type);

        final var path = Path.of((type == WorkspaceType.UNC_PATH ? "//UncMount/Share" : "")
            + "/Wörk – ßpäce/group/workflow").toAbsolutePath();
        final var expectedUrl = URLResolverUtil.toURL(path);

        final var resolved = resolver.resolve(new URL("knime://knime.workflow/../workflow"));
        assertEquals(expectedUrl, resolved);
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyHubContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyServerContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#remoteHubExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#remoteServerExecutorContexts()"
    })
    void testRootOfCurrentWorkflowMountpoint(final Context context, final String localMountId, final WorkspaceType type,
        final String spacePath) throws Exception {

        final var resolver = context.getResolver(localMountId, type);

        final var expectedUrl = URIPathEncoder.UTF_8.encodePathSegments(
            new URI(KnimeUrlType.SCHEME, localMountId, spacePath + "/group/workflow", null)).toURL();

        final var resolved = resolver.resolve(new URL("knime://knime.workflow/../workflow"));
        assertEquals(expectedUrl, resolved);
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#hubExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#serverExecutorContexts()",
    })
    void testRootOfCurrentWorkflowExecutor(final Context context, final String localMountId, final WorkspaceType type,
        final String spacePath) throws Exception {

        final var resolver = context.getResolver(localMountId, type);

        final var path = "/bla/blubb/repository" + spacePath + "/group/workflow:data";
        final var expectedUrl = URIPathEncoder.UTF_8.encodePathSegments(
            new URI("https", null, "127.0.0.1", 12345,  path, null, null)).toURL();

        final var resolved = resolver.resolve(new URL("knime://knime.workflow/../workflow"));
        assertEquals(expectedUrl, resolved);
    }

    /**
     * Tests that relative KNIME URLs cannot escape the surrounding space.
     * Currently this is not enforced for KNIME Server and for the Remote Workflow Editor.
     *
     * @param context workflow context type
     * @param localMountId local mount ID if known
     * @param type type of the local path of the workspace
     * @param spacePath path from mountpoint root to space root
     * @throws Exception in case of failure
     */
    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#localApContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#knwfContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyHubContexts()",
        //"org.knime.core.util.urlresolve.URLMethodSources#tempCopyServerContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#hubExecutorContexts()",
        //"org.knime.core.util.urlresolve.URLMethodSources#serverExecutorContexts()",
        //"org.knime.core.util.urlresolve.URLMethodSources#remoteHubExecutorContexts()",
        //"org.knime.core.util.urlresolve.URLMethodSources#remoteServerExecutorContexts()"
    })
    void testEscapeSpace(final Context context, final String localMountId, final WorkspaceType type,
        final String spacePath) throws Exception {

        final var resolver = context.getResolver(localMountId, type);
        final var subPath = "/foo/../../test.txt";

        assertThrows(resolver::resolve, new URL("knime://knime.mountpoint" + subPath));
        assertThrows(resolver::resolve, new URL("knime://knime.space" + subPath));
        assertThrows(resolver::resolve, new URL("knime://knime.workflow/../.." + subPath));
    }

    private static void assertResolvedURLEquals(final String message, final KnimeUrlResolver resolver,
        final URL expectedUrl, final URL url) throws IOException {
        assertEquals(message, expectedUrl, resolver.resolve(url));
    }

    private static void assertResolvedURIEquals(final String message, final KnimeUrlResolver resolver,
        final URI expectedUri, final URL url) throws IOException, URISyntaxException {
        assertEquals(message, expectedUri, resolver.resolve(url).toURI());
    }

    private static <T> ResourceAccessException assertThrows(
            final Functions.FailableFunction<T, ?, ResourceAccessException> operation, final T argument) {
        try {
            return fail("Unexpectedly successful: '" + argument + "' -> '" + operation.apply(argument));
        } catch (final ResourceAccessException e) {
            return e;
        }
    }

    private static void withNodeContext(final Path workflowDir,
        final Functions.FailableRunnable<Exception> callback) throws Exception {
        NodeID workflowID = null;
        try {
            final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
                new WorkflowCreationHelper(null));
            workflowID = wfm.getID();
            if (workflowDir != null) {
                wfm.save(workflowDir.toFile(), new ExecutionMonitor(), false);
            }
            NodeContext.pushContext(wfm);
            callback.run();
        } finally {
            WorkflowManager.ROOT.removeProject(workflowID);
            NodeContext.removeLastContext();
        }
    }
}
