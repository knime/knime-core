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
 *   11 Jul 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.util.urlresolve;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.core.util.hub.ItemVersion;

/**
 * Utility class to provide {@link MethodSource method sources} to parameterized test methods.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class URLMethodSources {

    enum WorkspaceType {
            LOCAL_PATH(Path.of("/Wörk – ßpäce").toAbsolutePath()),
            UNC_PATH(Path.of("//UncMount/Share/Wörk – ßpäce").toAbsolutePath());

        final Path workspace;

        WorkspaceType(final Path absolutePath) {
            workspace = absolutePath;
        }

        private static final Stream<WorkspaceType> supportedByOS() {
            return SystemUtils.IS_OS_WINDOWS ? Arrays.stream(WorkspaceType.values())
                : Stream.of(WorkspaceType.LOCAL_PATH);
        }
    }

    /**
     * <p>
     * A context for resolution is defined by executor and workflow location.
     * </p>
     * This enumerates all relevant combinations for URL resolution, including remote workflow editing, where the user
     * uses an application to view the state of a remote executor.
     */
    enum Context {
        AP_LOCAL((mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(type.workspace.resolve("group/workflow")) //
                .withMountpoint(mountId, type.workspace)) //
            .withLocalLocation() //
            .build())),
        AP_KNWF((mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(type.workspace.resolve("group/workflow"))) //
            .withArchiveLocation(type.workspace.resolve("../Downloads/workflow.knwf").normalize()) //
            .build())),
        AP_HUB((mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(type.workspace.resolve("group/workflow")) //
                .withMountpoint(mountId, type.workspace)) //
            .withHubSpaceLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/Users/John Döë/Private/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID") //
                .withSpace("/Users/John Döë/Private", "*1234") //
                .withWorkflowItemId("*1337")) //
            .build())),
        AP_SERVER((mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(type.workspace.resolve("group/workflow")) //
                .withMountpoint(mountId, type.workspace)) //
            .withServerLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID")) //
            .build())),
        HUB_HUB((mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withHubJobExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(type.workspace.resolve("group/workflow")) //
                .withJobId(UUID.randomUUID()) //
                .withScope("a", "b") //
                .withJobCreator("job creator")) //
            .withHubSpaceLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/Users/John Döë/Private/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID") //
                .withSpace("/Users/John Döë/Private", "*1234") //
                .withWorkflowItemId("*1337")) //
            .build())),
        SERVER_SERVER((mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withServerJobExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(type.workspace.resolve("group/workflow")) //
                .withJobId(UUID.randomUUID())) //
            .withServerLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID")) //
            .build())),
        HUB_HUB_RWE((mountId, type) -> KnimeUrlResolver.getRemoteWorkflowResolver(
            URI.create("knime://" + mountId + "/Users/John%20D%C3%B6%C3%AB/Private/group/workflow"),
            WorkflowContextV2.builder() //
            .withHubJobExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(type.workspace.resolve("group/workflow")) //
                .withJobId(UUID.randomUUID()) //
                .withScope("a", "b") //
                .withJobCreator("job creator") //
                .withRemoteExecutor(mountId, KNIMEConstants.VERSION)) //
            .withHubSpaceLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/Users/John Döë/Private/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID") //
                .withSpace("/Users/John Döë/Private", "*1234") //
                .withWorkflowItemId("*1337")) //
            .build())),
        SERVER_SERVER_RWE((mountId, type) -> KnimeUrlResolver.getRemoteWorkflowResolver(
            URI.create("knime://" + mountId + "/group/workflow"), WorkflowContextV2.builder() //
            .withServerJobExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(type.workspace.resolve("group/workflow")) //
                .withJobId(UUID.randomUUID()) //
                .withRemoteExecutor(mountId, KNIMEConstants.VERSION)) //
            .withServerLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID")) //
            .build())),
        NONE((mountId, type) -> KnimeUrlResolver.getResolver(null));

        private final BiFunction<String, WorkspaceType, KnimeUrlResolver> m_resolver;

        private Context(final BiFunction<String, WorkspaceType, KnimeUrlResolver> resolver) {
            this.m_resolver = resolver;
        }

        /**
         * @param localMountId
         *            <ul>
         *            <li>For {@link #AP_LOCAL} {@link #AP_SERVER} {@link #AP_HUB}: the mountpoint under which the
         *            workflow is located in the Analytics Platform's mount table (for {@link #AP_KNWF} the workflow is
         *            in a temp location).</li>
         *            <li>For {@link #HUB_HUB_RWE} and {@link #SERVER_SERVER_RWE}: denotes the mount point via which the
         *            remote workflow is accessed</li>
         *            </ul>
         * @param type of workspace paths
         * @return
         */
        KnimeUrlResolver getResolver(final String localMountId, final WorkspaceType type) {
            return m_resolver.apply(localMountId, type);
        }

        /**
         * @return a resolver using the <code>mountId</code> as local mount point identifier and
         *         {@link WorkspaceType#LOCAL_PATH}.
         */
        KnimeUrlResolver getResolver() {
            return getResolver("mountId", WorkspaceType.LOCAL_PATH);
        }
    }

    private URLMethodSources() {
        // hidden
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> localApContexts() {
        return WorkspaceType.supportedByOS().map(type -> Arguments.of(Context.AP_LOCAL, "MountID", type, ""));
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> knwfContexts() {
        return WorkspaceType.supportedByOS().map(type -> Arguments.of(Context.AP_KNWF, null, type, null));
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> tempCopyHubContexts() {
        return WorkspaceType.supportedByOS() //
            .flatMap(type -> Stream.of("MountID", "Renamed") //
                .map(mountId -> Arguments.of(Context.AP_HUB, mountId, type, "/Users/John Döë/Private")));
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> tempCopyServerContexts() {
        return WorkspaceType.supportedByOS() //
            .flatMap(type -> Stream.of("MountID", "Renamed") //
                .map(mountId -> Arguments.of(Context.AP_SERVER, mountId, type, "")));
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> hubExecutorContexts() {
        return WorkspaceType.supportedByOS() //
            .map(type -> Arguments.of(Context.HUB_HUB, "MountID", type, "/Users/John Döë/Private"));
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> serverExecutorContexts() {
        return WorkspaceType.supportedByOS() //
            .map(type -> Arguments.of(Context.SERVER_SERVER, "MountID", type, ""));
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> remoteHubExecutorContexts() {
        return WorkspaceType.supportedByOS() //
            .flatMap(type -> Stream.of("MountID", "Renamed") //
                .map(mountId -> Arguments.of(Context.HUB_HUB_RWE, mountId, type, "/Users/John Döë/Private")));
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> remoteServerExecutorContexts() {
        return WorkspaceType.supportedByOS() //
            .flatMap(type -> Stream.of("MountID", "Renamed") //
                .map(mountId -> Arguments.of(Context.SERVER_SERVER_RWE, mountId, type, "")));
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> allNonNullContexts() {
        return Stream.of(localApContexts(), knwfContexts(), tempCopyHubContexts(), tempCopyServerContexts(),
            hubExecutorContexts(), serverExecutorContexts(), remoteHubExecutorContexts(),
            remoteServerExecutorContexts()).flatMap(Function.identity());
    }

    /** @return resolver context, local mount point id, workspace type, space path */
    static Stream<Arguments> allNonRemoteNonNullContexts() {
        return Stream.of(localApContexts(), knwfContexts(), tempCopyHubContexts(), tempCopyServerContexts(),
            hubExecutorContexts(), serverExecutorContexts()).flatMap(Function.identity());
    }

    /** @return without version, with version, both versions, hub item version */
    static Stream<Arguments> mountpointAbsolute() {
        return Stream.of(
            Arguments.of("knime://My-Knime-Hub/Users/john/Private/test.txt",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=3",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=3&spaceVersion=3",
                         ItemVersion.of(3)),
            Arguments.of("knime://My-Knime-Hub/Users/john/Private/test.txt",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?spaceVersion=3",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=3&spaceVersion=3",
                         ItemVersion.of(3)),
            Arguments.of("knime://My-Knime-Hub/Users/john/Private/test.txt",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=most-recent",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=most-recent&spaceVersion=latest",
                         ItemVersion.mostRecent()),
            Arguments.of("knime://My-Knime-Hub/Users/john/Private/test.txt",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=current-state",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=current-state&spaceVersion=-1",
                         ItemVersion.currentState())
        );
    }

    /** @return without version, with version, both versions, hub item version */
    static Stream<Arguments> mountpointRelative() {
        return Stream.of(
            Arguments.of("knime://knime.mountpoint/test.txt",
                         "knime://knime.mountpoint/test.txt?version=3",
                         "knime://knime.mountpoint/test.txt?version=3&spaceVersion=3",
                         ItemVersion.of(3)),
            Arguments.of("knime://knime.mountpoint/test.txt",
                         "knime://knime.mountpoint/test.txt?spaceVersion=3",
                         "knime://knime.mountpoint/test.txt?version=3&spaceVersion=3",
                         ItemVersion.of(3)),
            Arguments.of("knime://knime.mountpoint/test.txt",
                         "knime://knime.mountpoint/test.txt?version=most-recent",
                         "knime://knime.mountpoint/test.txt?version=most-recent&spaceVersion=latest",
                         ItemVersion.mostRecent()),
            Arguments.of("knime://knime.mountpoint/test.txt",
                         "knime://knime.mountpoint/test.txt?version=current-state",
                         "knime://knime.mountpoint/test.txt?version=current-state&spaceVersion=-1",
                         ItemVersion.currentState())
        );
    }

    /** @return without version, with version, both versions, hub item version */
    static Stream<Arguments> spaceRelative() {
        return Stream.of(
            Arguments.of("knime://knime.space/test.txt",
                         "knime://knime.space/test.txt?version=3",
                         "knime://knime.space/test.txt?version=3&spaceVersion=3",
                         ItemVersion.of(3)),
            Arguments.of("knime://knime.space/test.txt",
                         "knime://knime.space/test.txt?spaceVersion=3",
                         "knime://knime.space/test.txt?version=3&spaceVersion=3",
                         ItemVersion.of(3)),
            Arguments.of("knime://knime.space/test.txt",
                         "knime://knime.space/test.txt?version=most-recent",
                         "knime://knime.space/test.txt?version=most-recent&spaceVersion=latest",
                         ItemVersion.mostRecent()),
            Arguments.of("knime://knime.space/test.txt",
                         "knime://knime.space/test.txt?version=current-state",
                         "knime://knime.space/test.txt?version=current-state&spaceVersion=-1",
                         ItemVersion.currentState())
        );
    }

    /** @return without version, with version, both versions, hub item version */
    static Stream<Arguments> workflowRelativeLeavingScope() {
        return Stream.of(
            Arguments.of("knime://knime.workflow/../test.txt",
                         "knime://knime.workflow/../test.txt?version=3",
                         "knime://knime.workflow/../test.txt?version=3&spaceVersion=3",
                         ItemVersion.of(3)),
            Arguments.of("knime://knime.workflow/../test.txt",
                         "knime://knime.workflow/../test.txt?spaceVersion=3",
                         "knime://knime.workflow/../test.txt?version=3&spaceVersion=3",
                         ItemVersion.of(3)),
            Arguments.of("knime://knime.workflow/../test.txt",
                         "knime://knime.workflow/../test.txt?version=most-recent",
                         "knime://knime.workflow/../test.txt?version=most-recent&spaceVersion=latest",
                         ItemVersion.mostRecent()),
            Arguments.of("knime://knime.workflow/../test.txt",
                         "knime://knime.workflow/../test.txt?version=current-state",
                         "knime://knime.workflow/../test.txt?version=current-state&spaceVersion=-1",
                         ItemVersion.currentState())
        );
    }

    // Always unversioned URLs

    /**
     * Workflow-relative in workflow directory; referenced resource does not have its own item version
     *
     * @return without version, with version, both versions, hub item version
     */
    static Stream<Arguments> workflowRelativeInScope() {
        return Stream.of(
            Arguments.of("knime://knime.workflow/test.txt",
                         "knime://knime.workflow/test.txt?version=3",
                         "knime://knime.workflow/test.txt?version=3&spaceVersion=3", null),
            Arguments.of("knime://knime.workflow/test.txt",
                         "knime://knime.workflow/test.txt?spaceVersion=3",
                         "knime://knime.workflow/test.txt?version=3&spaceVersion=3", null),
            Arguments.of("knime://knime.workflow/test.txt",
                         "knime://knime.workflow/test.txt?version=most-recent",
                         "knime://knime.workflow/test.txt?version=most-recent&spaceVersion=latest", null),
            Arguments.of("knime://knime.workflow/test.txt",
                         "knime://knime.workflow/test.txt?version=current-state",
                         "knime://knime.workflow/test.txt?version=current-state&spaceVersion=-1", null)
        );
    }

    /**
     * Node-relative; similar to {@link #workflowRelativeInScope()}, referenced resource does not have its own item
     * version
     *
     * @return without version, with version, both versions, hub item version
     */
    static Stream<Arguments> nodeRelativeInScope() {
        return Stream.of(
            Arguments.of("knime://knime.node/test.txt",
                         "knime://knime.node/test.txt?version=3",
                         "knime://knime.node/test.txt?version=3&spaceVersion=3", null),
            Arguments.of("knime://knime.node/test.txt",
                         "knime://knime.node/test.txt?spaceVersion=3",
                         "knime://knime.node/test.txt?version=3&spaceVersion=3", null),
            Arguments.of("knime://knime.node/test.txt",
                         "knime://knime.node/test.txt?version=most-recent",
                         "knime://knime.node/test.txt?version=most-recent&spaceVersion=latest", null),
            Arguments.of("knime://knime.node/test.txt",
                         "knime://knime.node/test.txt?version=current-state",
                         "knime://knime.node/test.txt?version=current-state&spaceVersion=-1", null)
        );
    }
}
