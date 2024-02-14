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
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.auth.SimpleTokenAuthenticator;
import org.knime.core.util.hub.HubItemVersion;

/**
 * Utility class to provide {@link MethodSource method sources} to parameterized test methods.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class URLMethodSources {

    enum WorkspaceType {
        LOCAL_PATH,
        UNC_PATH
    }

    private static final Path WORKSPACE = Path.of("/Wörk – ßpäce").toAbsolutePath();

    private static final Path WORKSPACE_UNC = Path.of("//UncMount/Share/Wörk – ßpäce").toAbsolutePath();

    private static final Path getWorkspace(final WorkspaceType type) {
        return type == WorkspaceType.LOCAL_PATH ? WORKSPACE : WORKSPACE_UNC;
    }

    private static final Stream<WorkspaceType> availableWorkspaceTypes() {
        return SystemUtils.IS_OS_WINDOWS ? Arrays.stream(WorkspaceType.values()) : Stream.of(WorkspaceType.LOCAL_PATH);
    }

    private static final Map<String, BiFunction<String, WorkspaceType, KnimeUrlResolver>> CONTEXTS = Map.of(
        "local+local", (mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(getWorkspace(type).resolve("group/workflow")) //
                .withMountpoint(mountId, getWorkspace(type))) //
            .withLocalLocation() //
            .build()),

        "local+knwf", (mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(getWorkspace(type).resolve("group/workflow"))) //
            .withArchiveLocation(getWorkspace(type).resolve("../Downloads/workflow.knwf").normalize()) //
            .build()),

        "local+hub", (mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(getWorkspace(type).resolve("group/workflow")) //
                .withMountpoint(mountId, getWorkspace(type))) //
            .withHubSpaceLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/Users/John Döë/Private/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID") //
                .withSpace("/Users/John Döë/Private", "*1234") //
                .withWorkflowItemId("*1337")) //
            .build()),

        "local+server", (mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withAnalyticsPlatformExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(getWorkspace(type).resolve("group/workflow")) //
                .withMountpoint(mountId, getWorkspace(type))) //
            .withServerLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID")) //
            .build()),

        "hub+hub", (mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withHubJobExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(getWorkspace(type).resolve("group/workflow")) //
                .withJobId(UUID.randomUUID()) //
                .withScope("a", "b") //
                .withJobCreator("job creator") //
                .withIsRemote(false)) //
            .withHubSpaceLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/Users/John Döë/Private/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID") //
                .withSpace("/Users/John Döë/Private", "*1234") //
                .withWorkflowItemId("*1337")) //
            .build()),

        "server+server", (mountId, type) -> KnimeUrlResolver.getResolver(WorkflowContextV2.builder() //
            .withServerJobExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(getWorkspace(type).resolve("group/workflow")) //
                .withJobId(UUID.randomUUID()) //
                .withIsRemote(false)) //
            .withServerLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID")) //
            .build()),

        "hub+hub+rwe", (mountId, type) -> KnimeUrlResolver.getRemoteWorkflowResolver(
            URI.create("knime://" + mountId + "/Users/John%20D%C3%B6%C3%AB/Private/group/workflow"),
            WorkflowContextV2.builder() //
            .withHubJobExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(getWorkspace(type).resolve("group/workflow")) //
                .withJobId(UUID.randomUUID()) //
                .withScope("a", "b") //
                .withJobCreator("job creator") //
                .withIsRemote(true)) //
            .withHubSpaceLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/Users/John Döë/Private/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID") //
                .withSpace("/Users/John Döë/Private", "*1234") //
                .withWorkflowItemId("*1337")) //
            .build()),

        "server+server+rwe", (mountId, type) -> KnimeUrlResolver.getRemoteWorkflowResolver(
            URI.create("knime://" + mountId + "/group/workflow"), WorkflowContextV2.builder() //
            .withServerJobExecutor(exec -> exec //
                .withUserId("user") //
                .withLocalWorkflowPath(getWorkspace(type).resolve("group/workflow")) //
                .withJobId(UUID.randomUUID()) //
                .withIsRemote(true)) //
            .withServerLocation(loc -> loc //
                .withRepositoryAddress(URI.create("https://127.0.0.1:12345/bla/blubb/repository")) //
                .withWorkflowPath("/group/workflow") //
                .withAuthenticator(new SimpleTokenAuthenticator("token")) //
                .withDefaultMountId("MountID")) //
            .build())
    );


    private URLMethodSources() {
        // hidden
    }

    static KnimeUrlResolver getResolver(final String context, final String localMountId, final WorkspaceType type) {
        return CONTEXTS.get(context).apply(localMountId, type);
    }

    static Stream<Arguments> localApContexts() {
        return availableWorkspaceTypes().map(type -> Arguments.of("local+local", "MountID", type, ""));
    }

    static Stream<Arguments> knwfContexts() {
        return availableWorkspaceTypes().map(type -> Arguments.of("local+knwf", null, type, null));
    }

    static Stream<Arguments> tempCopyHubContexts() {
        return availableWorkspaceTypes() //
                .flatMap(type -> Stream.of("MountID", "Renamed") //
                    .map(mountId -> Arguments.of("local+hub", mountId, type, "/Users/John Döë/Private")));
    }

    static Stream<Arguments> tempCopyServerContexts() {
        return availableWorkspaceTypes() //
                .flatMap(type -> Stream.of("MountID", "Renamed") //
                    .map(mountId -> Arguments.of("local+server", mountId, type, "")));
    }

    static Stream<Arguments> hubExecutorContexts() {
        return availableWorkspaceTypes() //
                .map(type -> Arguments.of("hub+hub", "MountID", type, "/Users/John Döë/Private"));
    }

    static Stream<Arguments> serverExecutorContexts() {
        return availableWorkspaceTypes() //
                .map(type -> Arguments.of("server+server", "MountID", type, ""));
    }

    static Stream<Arguments> remoteHubExecutorContexts() {
        return availableWorkspaceTypes() //
                .flatMap(type -> Stream.of("MountID", "Renamed") //
                    .map(mountId -> Arguments.of("hub+hub+rwe", mountId, type, "/Users/John Döë/Private")));
    }

    static Stream<Arguments> remoteServerExecutorContexts() {
        return availableWorkspaceTypes() //
                .flatMap(type -> Stream.of("MountID", "Renamed") //
                    .map(mountId -> Arguments.of("server+server+rwe", mountId, type, "")));
    }

    static Stream<Arguments> mountpointAbsolute() {
        return Stream.of(
            Arguments.of("knime://My-Knime-Hub/Users/john/Private/test.txt",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=3",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=3&spaceVersion=3",
                         HubItemVersion.of(3)),
            Arguments.of("knime://My-Knime-Hub/Users/john/Private/test.txt",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?spaceVersion=3",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=3&spaceVersion=3",
                         HubItemVersion.of(3)),
            Arguments.of("knime://My-Knime-Hub/Users/john/Private/test.txt",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=most-recent",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=most-recent&spaceVersion=latest",
                         HubItemVersion.latestVersion()),
            Arguments.of("knime://My-Knime-Hub/Users/john/Private/test.txt",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=current-state",
                         "knime://My-Knime-Hub/Users/john/Private/test.txt?version=current-state&spaceVersion=-1",
                         HubItemVersion.currentState())
        );
    }

    static Stream<Arguments> mountpointRelative() {
        return Stream.of(
            Arguments.of("knime://knime.mountpoint/test.txt",
                         "knime://knime.mountpoint/test.txt?version=3",
                         "knime://knime.mountpoint/test.txt?version=3&spaceVersion=3",
                         HubItemVersion.of(3)),
            Arguments.of("knime://knime.mountpoint/test.txt",
                         "knime://knime.mountpoint/test.txt?spaceVersion=3",
                         "knime://knime.mountpoint/test.txt?version=3&spaceVersion=3",
                         HubItemVersion.of(3)),
            Arguments.of("knime://knime.mountpoint/test.txt",
                         "knime://knime.mountpoint/test.txt?version=most-recent",
                         "knime://knime.mountpoint/test.txt?version=most-recent&spaceVersion=latest",
                         HubItemVersion.latestVersion()),
            Arguments.of("knime://knime.mountpoint/test.txt",
                         "knime://knime.mountpoint/test.txt?version=current-state",
                         "knime://knime.mountpoint/test.txt?version=current-state&spaceVersion=-1",
                         HubItemVersion.currentState())
        );
    }

    static Stream<Arguments> spaceRelative() {
        return Stream.of(
            Arguments.of("knime://knime.space/test.txt",
                         "knime://knime.space/test.txt?version=3",
                         "knime://knime.space/test.txt?version=3&spaceVersion=3",
                         HubItemVersion.of(3)),
            Arguments.of("knime://knime.space/test.txt",
                         "knime://knime.space/test.txt?spaceVersion=3",
                         "knime://knime.space/test.txt?version=3&spaceVersion=3",
                         HubItemVersion.of(3)),
            Arguments.of("knime://knime.space/test.txt",
                         "knime://knime.space/test.txt?version=most-recent",
                         "knime://knime.space/test.txt?version=most-recent&spaceVersion=latest",
                         HubItemVersion.latestVersion()),
            Arguments.of("knime://knime.space/test.txt",
                         "knime://knime.space/test.txt?version=current-state",
                         "knime://knime.space/test.txt?version=current-state&spaceVersion=-1",
                         HubItemVersion.currentState())
        );
    }

    static Stream<Arguments> workflowRelativeLeavingScope() {
        return Stream.of(
            Arguments.of("knime://knime.workflow/../test.txt",
                         "knime://knime.workflow/../test.txt?version=3",
                         "knime://knime.workflow/../test.txt?version=3&spaceVersion=3",
                         HubItemVersion.of(3)),
            Arguments.of("knime://knime.workflow/../test.txt",
                         "knime://knime.workflow/../test.txt?spaceVersion=3",
                         "knime://knime.workflow/../test.txt?version=3&spaceVersion=3",
                         HubItemVersion.of(3)),
            Arguments.of("knime://knime.workflow/../test.txt",
                         "knime://knime.workflow/../test.txt?version=most-recent",
                         "knime://knime.workflow/../test.txt?version=most-recent&spaceVersion=latest",
                         HubItemVersion.latestVersion()),
            Arguments.of("knime://knime.workflow/../test.txt",
                         "knime://knime.workflow/../test.txt?version=current-state",
                         "knime://knime.workflow/../test.txt?version=current-state&spaceVersion=-1",
                         HubItemVersion.currentState())
        );
    }

    // Always unversioned URLs

    static Stream<Arguments> workflowRelativeInScope() {
        return Stream.of(
            // Workflow-relative in workflow directory; referenced resource does not have its own item version
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

    static Stream<Arguments> nodeRelativeInScope() {
        return Stream.of(
            // Node-relative; referenced resource does not have its own item version
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
