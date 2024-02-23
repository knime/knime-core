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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.Functions;
import org.junit.Assume;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.URIPathEncoder;
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
    @ParameterizedTest
    @EnumSource(value = URLMethodSources.Context.class)
    void testWrongProtocol(final Context context) throws Exception {
        final var resolver = context.getResolver();
        final var e = org.junit.jupiter.api.Assertions.assertThrows(ResourceAccessException.class,
            () -> resolver.resolve(new URL("http://www.knime.com/")));
        assertThat(e).as("Error should indicate that the protocol is not `knime:`")
            .hasMessageContaining("not a valid KNIME URL");
    }

    /**
     * Context paths provide the path to the space (relative to the mount point) and the workflow (relative to the
     * space).
     * <ul>
     * <li>For {@link ContextlessUrlResolver}s, there is no workflow location.</li>
     * <li>For {@link AnalyticsPlatformTempCopyUrlResolver}s, there is no logical mount point until the workflow is
     * saved.</li>
     * </ul>
     */
    @ParameterizedTest
    @EnumSource(value = Context.class, names = {"NONE", "AP_KNWF"})
    void testContextPathsAreUndefined(final Context context) throws Exception {
        assertThat(context.getResolver().getContextPaths()).isEmpty();
    }

    /**
     * Checks if mountpoint-relative knime-URLs that reside on an UNC drive are resolved correctly (see AP-7427).
     */
    @ParameterizedTest
    @MethodSource("org.knime.core.util.urlresolve.URLMethodSources#allNonRemoteNonNullContexts()")
    void testResolveWorkflowRelativeUNC(final Context context, final String localMountId, final WorkspaceType type)
        throws Exception {
        Assume.assumeTrue(type == WorkspaceType.UNC_PATH);

        final var resolver = context.getResolver(localMountId, WorkspaceType.UNC_PATH);

        final var url = new URL("knime://knime.workflow/test.txt");
        final var expectedPath = type.workspace.resolve("group/workflow/test.txt");
        final var resolved = resolver.resolve(url);
        assertEquals("Unexpected resolved URL", URLResolverUtil.toURL(expectedPath), resolved);
        assertEquals("Unexpected resulting file", expectedPath.toFile(), new File(resolved.toURI()));
    }

    /** Checks if German special characters in a local workflow relative URL are UTF-8 encoded. */
    @ParameterizedTest
    @MethodSource("org.knime.core.util.urlresolve.URLMethodSources#allNonNullContexts()")
    void testMountpointSpaceRelative(final Context context, final String localMountId, final WorkspaceType type)
            throws Exception {
        final var resolver = context.getResolver(localMountId, type);

        // has no mount point
        Assume.assumeFalse(context == Context.AP_KNWF);

        assertThat(resolver.resolve(new URL("knime://knime.mountpoint/testÖ.txt"))).asString() //
            .doesNotContain("Ö").contains("test%C3%96.txt");

        assertThat(resolver.resolve(new URL("knime://knime.space/testÖ.txt"))).asString() //
            .doesNotContain("Ö").contains("test%C3%96.txt");

        assertThat(resolver.resolve(new URL("knime://knime.workflow/../testÖ.txt"))).asString() //
            .doesNotContain("Ö").contains("test%C3%96.txt");

        Assume.assumeFalse(Set.of(Context.HUB_HUB_RWE, Context.SERVER_SERVER_RWE).contains(context));

        assertThat(resolver.resolve(new URL("knime://knime.workflow/testÖ.txt"))).asString() //
            .doesNotContain("Ö").contains("test%C3%96.txt");
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

        final var expectedRelative = new HashMap<>(Map.of(
            KnimeUrlType.MOUNTPOINT_RELATIVE, new URL("knime://knime.mountpoint/group/workflow2"),
            KnimeUrlType.HUB_SPACE_RELATIVE, new URL("knime://knime.space/group/workflow2"),
            KnimeUrlType.WORKFLOW_RELATIVE, new URL("knime://knime.workflow/../workflow2")));

        // make sure that all possible versions of absolute URLs are present
        final var pathInMountpoint = spacePath + "/group/workflow2";
        final var absoluteDefaultId = URIPathEncoder.UTF_8.encodePathSegments(
            new URI(KnimeUrlType.SCHEME, "MountID", pathInMountpoint, null)).toURL();
        final var absoluteRenamedId = URIPathEncoder.UTF_8.encodePathSegments(
            new URI(KnimeUrlType.SCHEME, localMountId, pathInMountpoint, null)).toURL();

        final var allInputs = new HashSet<>(expectedRelative.values());
        allInputs.add(absoluteRenamedId);
        // the KS RWE resolver only uses the (local) mountpoint URL, so it doesn't know the remote default mount ID
        if (context != Context.SERVER_SERVER_RWE) {
            allInputs.add(absoluteDefaultId);
        }

        for (final var initialUrl : allInputs) {
            final var resolvedUrl = resolver.resolveInternal(initialUrl).orElseThrow();
            Optional.ofNullable(resolvedUrl.path()).ifPresent(p -> assertFalse(p.isAbsolute()));
            Optional.ofNullable(resolvedUrl.pathInsideWorkflow()).ifPresent(p -> assertFalse(p.isAbsolute()));

            final var converted = resolver.changeLinkType(initialUrl);
            for (final var e : expectedRelative.entrySet()) {
                final var urlType = e.getKey();
                final var expectedUrl = e.getValue();
                assertThat(converted.get(urlType)).as("Converting " + initialUrl + " to " + urlType)
                    .isEqualTo(expectedUrl);
            }

            final var inputType = KnimeUrlType.getType(initialUrl).orElseThrow();
            // If an absolute URL is the input, it should come out unchanged
            final var expectedAbsUrl = inputType == KnimeUrlType.MOUNTPOINT_ABSOLUTE ? initialUrl
                // on Server executors we ignore the default mount ID for historical reasons
                : context == Context.SERVER_SERVER_RWE ? absoluteRenamedId
                    // in all other cases the URL should work in the workflow's location (using the default mount ID)
                    : absoluteDefaultId;

            assertThat(converted.get(KnimeUrlType.MOUNTPOINT_ABSOLUTE)) //
                .as("Converting " + initialUrl + " to MOUNTPOINT_ABSOLUTE") //
                .isEqualTo(expectedAbsUrl);
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
        assertTrue(resolvedUrl.canBeRelativized());

        assertResolvedURLEquals("Should resolve to a file in the executor's file system.", resolver,
            expectedUrl, workflowRelative);
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
    void testRootOfCurrentWorkflow(final Context context, final String localMountId, final WorkspaceType type)
            throws Exception {

        final var resolver = context.getResolver(localMountId, type);

        final var path = Path.of((type == WorkspaceType.UNC_PATH ? "//UncMount/Share" : "")
            + "/Wörk – ßpäce/group/workflow").toAbsolutePath();
        final var expectedUrl = URLResolverUtil.toURL(path);

        final var resolved = resolver.resolve(new URL("knime://knime.workflow"));
        assertEquals(expectedUrl, resolved);
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyHubContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#hubExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#remoteHubExecutorContexts()"
    })
    void testDifferentSpaceNotRelativizable(final Context context, final String localMountId, final WorkspaceType type)
            throws Exception {
        final var urlSame = new URL("knime://MountID/Users/John%20D%C3%B6%C3%AB/Private/workflow2");
        final var resolvedSame = context.getResolver(localMountId, type).resolveInternal(urlSame).orElseThrow();
        assertThat(resolvedSame.canBeRelativized()).as("canBeRelativized").isTrue();

        final var urlOther = new URL("knime://MountID/Users/John%20D%C3%B6%C3%AB/OtherSpace/workflow2");
        final var resolvedOther = context.getResolver(localMountId, type).resolveInternal(urlOther).orElseThrow();
        assertThat(resolvedOther.canBeRelativized()).as("canBeRelativized").isFalse();
    }

    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#localApContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#knwfContexts()"
    })
    void testRootOfCurrentWorkflowLocal(final Context context, final String localMountId, final WorkspaceType type)
            throws Exception {

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
        assertThat(expectedUrl).isEqualTo(resolved);
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
        assertThat(expectedUrl).isEqualTo(resolved);
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

    /**
     *
     */
    @ParameterizedTest
    @MethodSource({
        "org.knime.core.util.urlresolve.URLMethodSources#localApContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#knwfContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyHubContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#tempCopyServerContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#hubExecutorContexts()",
        "org.knime.core.util.urlresolve.URLMethodSources#serverExecutorContexts()"
    })
    void testNodeRelativeEscapeWorkflow(final Context context, final String localMountId, final WorkspaceType type)
        throws Exception {

        final var resolver = context.getResolver(localMountId, type);

        withNodeContext(KNIMEConstants.getKNIMETempPath().resolve("root").resolve("workflow"), () -> {
            final var e = assertThrows(resolver::resolve, new URL("knime://knime.node/foo/../../test.txt"));
            assertThat(e).hasMessageContaining("Leaving the workflow is not allowed");
        });

    }

    static void assertResolvedURLEquals(final String message, final KnimeUrlResolver resolver,
        final URL expectedUrl, final URL url) throws IOException {
        assertThat(resolver.resolve(url)).as(message).isEqualTo(expectedUrl);
    }

    static void assertResolvedURLEquals(final KnimeUrlResolver resolver,
        final URL expectedUrl, final URL url) throws IOException {
        assertThat(resolver.resolve(url)).as("Unexpected resolved URL").isEqualTo(expectedUrl);
    }

    static void assertResolvedURIEquals(final String message, final KnimeUrlResolver resolver,
        final URI expectedUri, final URL url) throws IOException, URISyntaxException {
        assertThat(resolver.resolve(url).toURI()).as(message).isEqualTo(expectedUri);
    }

    static void assertResolvedURIEquals(final KnimeUrlResolver resolver,
        final URI expectedUri, final URL url) throws IOException, URISyntaxException {
        assertThat(resolver.resolve(url).toURI()).as("Unexpected resolved URI").isEqualTo(expectedUri);
    }

    static <T> ResourceAccessException assertThrows(
            final Functions.FailableFunction<T, ?, ResourceAccessException> operation, final T argument) {
        try {
            return fail("Unexpectedly successful: '" + argument + "' -> '" + operation.apply(argument));
        } catch (final ResourceAccessException e) {
            return e;
        }
    }

    static void withNodeContext(final Path workflowDir,
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
