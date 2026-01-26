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
 *   13.09.2016 (thor): created
 */
package org.knime.core.internal.knimeurl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.auth.SimpleTokenAuthenticator;


/**
 * Testcases for {@link ExplorerURLStreamHandler}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public class ExplorerURLStreamHandlerTest {

    private ExplorerURLStreamHandler m_handler = new ExplorerURLStreamHandler();

    private Set<NodeID> m_staticWFMs;

    /**
     * Remember the WFMS that were known before any test ran. Don't touch them on {@link #cleanup()}.
     */
    @Before
    public void indexWFMsBefore() {
        m_staticWFMs = WorkflowManager.ROOT.getNodeContainers().stream()
                .map(NodeContainer::getID)
                .collect(Collectors.toSet());
    }

    /**
     * Cleanup after each test.
     *
     * @throws Exception if an error occurs
     */
    @After
    public void cleanup() throws Exception {
        while (true) {
            try {
                NodeContext.removeLastContext();
            } catch (IllegalStateException ex) {
                break;
            }
        }

        final var workflows = WorkflowManager.ROOT.getNodeContainers().stream().map(nc -> nc.getID())
            .filter(id -> !m_staticWFMs.contains(id)).collect(Collectors.toList());
        workflows.stream().forEach(id -> WorkflowManager.ROOT.removeProject(id));
    }

    /**
     * Checks if wrong protocols are rejected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testWrongProtocol() throws Exception {
        final var url = new URL("http://www.knime.com/");
        final var e = assertThrows(IOException.class, () -> m_handler.openConnection(url));
        assertTrue("Error should indicate that the protocol is not `knime:`.",
            e.getMessage().contains("Unexpected protocol"));
    }

    /**
     * Checks if a missing {@link NodeContext} is handled correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testNoNodeContext() throws Exception {
        final var url = new URL("knime://knime.workflow/workflow.knime");
        final var e = assertThrows(IOException.class, () -> m_handler.openConnection(url));
        assertTrue("Error should indicate that no node context is set.", e.getMessage().contains("No context"));
    }

    /**
     * Checks if a missing {@link WorkflowContextV2} is handled correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testNoWorkflowContext() throws Exception {
        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(null));
        NodeContext.pushContext(wfm);

        final var url = new URL("knime://knime.workflow/workflow.knime");
        final var e = assertThrows(IOException.class, () -> m_handler.openConnection(url));
        assertTrue("Error should indicate that no workflow context is set.",
            e.getMessage().contains("without a workflow context"));
    }

    /**
     * Checks if workflow-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeLocal() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("workflow.knime").toUri(),
            new URL("knime://knime.workflow/workflow.knime"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("..").resolve("test.txt").toUri().normalize(),
            new URL("knime://knime.workflow/../test.txt"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("..").resolve("With Space+Plus.txt").toUri().normalize(),
            new URL("knime://knime.workflow/../With Space+Plus.txt"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("..").resolve("With Space+Plus.txt").toUri().normalize(),
            new URL("knime://knime.workflow/../With%20Space+Plus.txt"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("Double Slash Decoded").toUri(),
            new URL("knime://knime.workflow//Double Slash Decoded"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("..").resolve("Double Slash Encoded").toUri().normalize(),
            new URL("knime://knime.workflow/..//Double%20Slash%20Encoded"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("..").resolve("Double Slash Decoded").toUri().normalize(),
            new URL("knime://knime.workflow/..//Double Slash Decoded"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("Double Slash Encoded").toUri(),
            new URL("knime://knime.workflow//Double%20Slash%20Encoded"));
    }

    /**
     * Checks that workflow-relative knime-URLs do not leave the mount point.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeObeyMountpointRoot() throws Exception {
        // original location == current location
        final var url = new URL("knime://knime.workflow/../../test.txt");
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        final var e = assertThrows(IOException.class, () -> m_handler.openConnection(url));
        assertTrue("Error should indicate that leaving the mountpoint is not allowed.",
            e.getMessage().contains("Leaving the mount point is not allowed"));
    }

    /**
     * Checks if workflow-relative knime-URLs are resolved correctly to server addresses.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeToServer() throws Exception {
        // original location == current location
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
                    .withDefaultMountId("Testserver"))
                .build();
        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        // links inside the workflow must stay local links to the workflow copy
        assertResolvedURIEquals("Unexpected resolved URL",
            new URI(currentLocation.toUri().toString() + "/workflow.knime").normalize(),
            new URL("knime://knime.workflow/workflow.knime"));

        // path outside the workflow
        assertResolvedURIEquals("Unexpected resolved URL",
            new URI(baseUri.toString() + "/test%201.txt:data"),
            new URL("knime://knime.workflow/../test 1.txt"));
    }

    /**
     * Checks if workflow-relative knime-URLs do not leave the space.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeURLDontLeaveSpace() throws Exception {
        final var repoAddressUri = URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository");
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

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository/Users/john/Private/test.txt:data"),
            new URL("knime://knime.workflow/../../test.txt"));

        final var urlA = new URL("knime://knime.workflow/../../../Public/test.txt");
        final var eA = assertThrows(IOException.class, () -> m_handler.openConnection(urlA));
        assertTrue("Error should indicate that leaving the Hub space is not allowed, found " + eA,
            eA.getMessage().contains("Leaving the Hub space is not allowed for workflow relative URLs:"));

        final var urlB = new URL("knime://knime.workflow/foo/../../../../test.txt");
        final var eB = assertThrows(IOException.class, () -> m_handler.openConnection(urlB));
        assertTrue("Error should indicate that leaving the Hub space is not allowed, found " + eB,
            eB.getMessage().contains("Leaving the Hub space is not allowed for workflow relative URLs:"));
    }

    /**
     * Checks if mountpoint-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveMountPointRelativeLocal() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.getParent().resolve("test.txt").toUri(),
            new URL("knime://knime.mountpoint/test.txt"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.getParent().resolve("With Space+Plus.txt").toUri(),
            new URL("knime://knime.mountpoint/With Space+Plus.txt"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.getParent().resolve("With Space+Plus.txt").toUri(),
            new URL("knime://knime.mountpoint/With%20Space+Plus.txt"));
    }


   /**
     * Checks if mountpoint-relative knime-URLs that reside on an UNC drive are resolved correctly (see AP-7427).
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveMountpointRelativeLocalUNC() throws Exception {
        Assume.assumeTrue(Platform.OS_WIN32.equals(Platform.getOS()));

        final File currentLocation = new File("\\\\server\\repo\\workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation.toPath())
                    .withMountpoint("LOCAL", currentLocation.getParentFile().toPath()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        final var url = new URL("knime://knime.mountpoint/test.txt");
        final var expectedPath = new File(currentLocation.getParentFile(), "test.txt");
        final var conn = m_handler.openConnection(url);
        assertEquals("Unexpected resolved URL", expectedPath.toURI(), conn.getURL().toURI());
        assertEquals("Unexpected resulting file", expectedPath, new File(conn.getURL().toURI()));
    }

    /**
     * Checks that mountpoint-relative knime-URLs do not leave the mount point.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveMountpointRelativeObeyMountpointRoot() throws Exception {
        // original location == current location
        final var url = new URL("knime://knime.mountpoint/../test.txt");
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        final var e = assertThrows(IOException.class, () -> m_handler.openConnection(url));
        assertTrue("Error should indicate that leaving the mountpoint is not allowed.",
            e.getMessage().contains("Leaving the mount point is not allowed"));
    }

    /**
     * Checks if mountpoint-relative knime-URLs are resolved correctly to server addresses.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveMountpointRelativeToServer() throws Exception {
        // original location == current location
        final var baseUri = URI.create("http://localhost:8080/knime");
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
                    .withDefaultMountId("TestServer"))
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            new URI(baseUri.toString() + "/some%20where/outside.txt:data"),
            new URL("knime://knime.mountpoint/some where/outside.txt"));
    }




    /**
     * Checks if workflow-relative knime-URLs that reside on a UNC drive are resolved correctly (see AP-7427).
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveWorkflowRelativeLocalUNC() throws Exception {
        Assume.assumeTrue(Platform.OS_WIN32.equals(Platform.getOS()));

        final File currentLocation = new File("\\\\server\\repo\\workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation.toPath())
                    .withMountpoint("LOCAL", currentLocation.getParentFile().toPath()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            new File(currentLocation, "workflow.knime").toURI(),
            new URL("knime://knime.workflow/workflow.knime"));

        // path outside the workflow
        final var url = new URL("knime://knime.workflow/../test.txt");
        final var expectedPath = new File(currentLocation.getParentFile(), "test.txt");
        final var conn = m_handler.openConnection(url);
        assertEquals("Unexpected resolved URL", expectedPath.toURI(), conn.getURL().toURI());
        assertEquals("Unexpected resulting file", expectedPath, new File(conn.getURL().toURI()));
    }

    /**
     * Check if URLs with a remote mount point are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveRemoteMountpointURL() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://localhost:8080/knime"))
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Server"))
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            new URI(URI.create("https://localhost:8080/knime").toString() + "/some%20where/outside.txt:data"),
            new URL("knime://Server/some where/outside.txt"));
    }


    /**
     * Checks if node-relative knime-URLs are resolved correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveNodeRelativeLocal() throws Exception {
        final var mountpointRoot = KNIMEConstants.getKNIMETempPath().resolve("root");
        final var currentLocation = mountpointRoot.resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", mountpointRoot))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("test.txt").toUri(),
            new URL("knime://knime.node/test.txt"));

        // Check proper handling of potentially encoded paths (see AP-17103).
        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("With Space+Plus.txt").toUri(),
            new URL("knime://knime.node/With Space+Plus.txt"));

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.resolve("With Space+Plus.txt").toUri(),
            new URL("knime://knime.node/With%20Space+Plus.txt"));
    }

    /**
     * Checks that node-relative knime-URLs only work on saved workflows.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveNodeRelativeLocalNotSaved() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        final var url = new URL("knime://knime.node/test.txt");
        final var e = assertThrows(IOException.class, () -> m_handler.openConnection(url));
        assertTrue("Error should indicate that the workflow must be saved.",
            e.getMessage().contains("Workflow must be saved"));
    }

    /**
     * Checks if node-relative knime-URLs do not leave the workflow.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveNodeRelativeLocalDontLeaveWorkflow() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        final var url = new URL("knime://knime.node/../test.txt");
        final var e = assertThrows(IOException.class, () -> m_handler.openConnection(url));
        assertTrue("Error should indicate that leaving the workflow is not allowed.",
            e.getMessage().contains("Leaving the workflow is not allowed"));
    }

    /**
     * Checks if space-relative knime-URLs are resolved correctly with version.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveSpaceRelativeURLWithVersion() throws Exception { // NOSONAR rewrite tests using @ParameterizedTest in JUnit 5
        final var repoAddressUri = URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository");
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
                    .withWorkflowPath("/Users/john/Private/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12")
                    .withItemVersion(4))
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        // different item, so the workflow's item version is irrelevant
        assertResolvedURIEquals("Unexpected resolved URL",
            URI.create(repoAddressUri.toString() + "/Users/john/Private/boss/test%20small.txt:data"),
            new URL("knime://knime.space/boss/test%20small.txt"));
        final var e = assertThrows(IOException.class,
            () -> m_handler.openConnection(new URL("knime://knime.space/test/../../Public/stuff/test.txt")));
        assertTrue("Error should indicate that leaving the Hub space is not allowed.",
            e.getMessage().contains("Leaving the Hub space is not allowed"));
    }

    /**
     * Checks if space-relative knime-URLs are resolved correctly without version.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveSpaceRelativeURLWithoutVersion() throws Exception {
        final var repoAddressUri = URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository");
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
                    .withWorkflowPath("/Users/john/Private/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12"))
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            URI.create(repoAddressUri.toString() + "/Users/john/Private/boss/test%20small.txt:data"),
            new URL("knime://knime.space/boss/test%20small.txt"));
    }

    /**
     * Checks if space-relative knime-URLs do not leave the space.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveSpaceRelativeURLDontLeaveSpace() throws Exception {
        final var repoAddressUri = URI.create("https://api.hub.knime.com:443/knime/rest/v4/repository");
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
                    .withWorkflowPath("/Users/john/Private")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("My-Knime-Hub")
                    .withSpace("/Users/john/Private", "*11")
                    .withWorkflowItemId("*12")
                    .withItemVersion(4))
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        final var url = new URL("knime://knime.space/../test%20small.txt");
        final var e = assertThrows(IOException.class, () -> m_handler.openConnection(url));
        assertTrue("Error should indicate that leaving the Hub space is not allowed.",
            e.getMessage().contains("Leaving the Hub space is not allowed for space relative URLs:"));
    }

    /**
     * Checks if space-relative get redirected to mountpoint-relative knime-URLs.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveSpaceRelativeURLToMountPointRelativeLocal() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            currentLocation.getParent().resolve("test.txt").toUri(),
            new URL("knime://knime.space/test.txt"));
    }

    /**
     * Checks if space-relative get redirected to mountpoint-relative knime-URLs.
     *
     * @throws Exception if an error occurs
     */
    @Test
    @Ignore("Ignored for now because it would require mocking of mountpoint URL services and hub api calls. "
          + "Will be replaced by workfow test.")
    public void testResolveSpaceRelativeURLInTempCopy() throws Exception {
        final var localMountId = "My-Knime-Hub";
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        final var context = WorkflowContextV2.builder()
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
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            URI.create("knime://" + localMountId + "/Users/john%20doe/Private/Foo/test.txt"),
            new URL("knime://knime.space/Foo/test.txt"));

        final var url1 = new URL("knime://knime.space/workflow/data/test.txt");
        final var ex1 = assertThrows(IOException.class, () -> m_handler.openConnection(url1));
        assertTrue("'" + ex1.getMessage() + "' should talk about diallowing access.",
            ex1.getMessage().contains("Accessing the current workflow's contents is not allowed"));

        final var url2 = new URL("knime://knime.space/Foo/../../OtherSpace/Bar/test.txt");
        final var ex2 = assertThrows(IOException.class, () -> m_handler.openConnection(url2));
        assertTrue(ex2.getMessage().contains("Leaving the Hub space is not allowed for space relative URLs"));
    }

    /**
     * Checks if German special characters in a local work flow relative URL are UTF-8 encoded.
     *
     * @throws Exception
     */
    @Test
    public void testWorkflowRelativeURLIsEncoded() throws Exception {
        final var mountpointRoot = KNIMEConstants.getKNIMETempPath().resolve("root");
        final var currentLocation = mountpointRoot.resolve("workflow");
        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", mountpointRoot))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
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
    public void testLocalMountPointRelativeURLIsEncoded() throws Exception {
        final var mountpointRoot = KNIMEConstants.getKNIMETempPath().resolve("root");
        final var currentLocation = mountpointRoot.resolve("workflow");
        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", mountpointRoot))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
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
    public void testLocalNodeRelativeURLIsEncoded() throws Exception {
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withLocalLocation()
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        wfm.save(currentLocation.toFile(), new ExecutionMonitor(), false);
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            // hack to remove unwanted encoding of %!
            new URI(currentLocation.resolve("test%C3%96.txt").toUri().toString().replace("25", "")),
            new URL("knime://knime.node/testÖ.txt"));
    }

    /**
     * Checks if German special characters in a remote mount point resolved URL are encoded.
     *
     * @throws Exception
     */
    @Test
    public void testRemoteMountpointURLIsEncoded() throws Exception {
        // Mountpoint is never considered in the old code path, and it makes little sense here IMHO
        final var currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");
        final var context = WorkflowContextV2.builder()
                .withServerJobExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withJobId(UUID.randomUUID()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://localhost:8080/knime"))
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Server"))
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved URL",
            new URI(URI.create("https://localhost:8080/knime").toString() + "/r%C3%B6w0.json:data").normalize(),
            new URL("knime://knime.workflow/../röw0.json"));
    }

    /**
     * Checks if German special characters in a remote work flow relative to server URL are encoded.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testRemoteWorkflowRelativeURLIsEncoded() throws Exception {
        final var baseUri = new URI("http://localhost:8080/knime");

        // original location == current location
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
                    .withDefaultMountId("Server"))
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURIEquals("Unexpected resolved umlaut URL",
            new URI(baseUri.toString() + "/test%C3%9C.txt:data"),
            new URL("knime://knime.workflow/../testÜ.txt"));
    }

    /**
     * Checks if knime-URLs are correctly resolved for temporary copies of workflows.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testResolveInTemporaryCopy() throws Exception {
        Path currentLocation = KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow");

        final var context = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(currentLocation)
                    .withMountpoint("LOCAL", currentLocation.getParent()))
                .withServerLocation(loc -> loc
                    .withRepositoryAddress(URI.create("https://localhost:1234/repository"))
                    .withWorkflowPath("/workflow")
                    .withAuthenticator(new SimpleTokenAuthenticator("token"))
                    .withDefaultMountId("Server"))
                .build();

        final var wfm = WorkflowManager.ROOT.createAndAddProject("Test" + UUID.randomUUID(),
            new WorkflowCreationHelper(context));
        NodeContext.pushContext(wfm);

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL outside workflow",
            new URL("knime://LOCAL/test.txt"),
            new URL("knime://knime.workflow/../test.txt"));

        assertResolvedURLEquals("Unexpected resolved workflow-relative URL inside workflow",
            currentLocation.resolve("test.txt").toUri().toURL(),
            new URL("knime://knime.workflow/test.txt"));

        assertResolvedURLEquals("Unexpected resolved mountpoint-relative URL",
            new URL("knime://LOCAL/xxx/test.txt"),
            new URL("knime://knime.mountpoint/xxx/test.txt"));

        assertResolvedURLEquals("Unexpected resolved absolute URL in same mount point",
            new URL("knime://LOCAL/yyy/test.txt"),
            new URL("knime://LOCAL/yyy/test.txt"));

        // Ignored for now because it would require mocking of mountpoint URL services and server api calls
        //assertResolvedURLEquals("Unexpected resolved absolute URL in other mount point",
        //    new URL("knime://Some-Other-Server/test.txt"),
        //    new URL("knime://Some-Other-Server/test.txt"));
    }

    private void assertResolvedURLEquals(final String message, final URL expectedUrl, final URL url)
            throws IOException {
        assertEquals(message, expectedUrl, m_handler.openConnection(url).getURL());
    }

    private void assertResolvedURIEquals(final String message, final URI expectedUri, final URL url)
            throws IOException, URISyntaxException {
        assertEquals(message, expectedUri, m_handler.openConnection(url).getURL().toURI());
    }
}
