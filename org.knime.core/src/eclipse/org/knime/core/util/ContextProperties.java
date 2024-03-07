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
 *   Aug 21, 2018 (Noemi_Balassa): created
 */
package org.knime.core.util;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.net.URIBuilder;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.ExecutorType;

/**
 * Static constants and utility methods for working with KNIME context properties.
 *
 * @author Noemi Balassa
 * @since 3.7
 */
public final class ContextProperties {

    private enum Property {
            WORKFLOW_NAME(CONTEXT_PROPERTY_WORKFLOW_NAME, ContextProperties::extractWorkflowName),
            WORKFLOW_PATH(CONTEXT_PROPERTY_WORKFLOW_PATH, ContextProperties::extractWorkflowPath),
            WORKFLOW_ABSOLUTE_PATH(CONTEXT_PROPERTY_WORKFLOW_ABSOLUTE_PATH,
                ContextProperties::extractWorkflowAbsolutePath),
            SERVER_USER(CONTEXT_PROPERTY_SERVER_USER,
                (nc, wfcv2) -> wfcv2 == null ? "" : wfcv2.getExecutorInfo().getUserId()),
            TEMP_LOCATION(CONTEXT_PROPERTY_TEMP_LOCATION,
                (nc, wfcv2) -> wfcv2 == null ? "" : wfcv2.getExecutorInfo().getTempFolder().toFile().getAbsolutePath()),
            AUTHOR(CONTEXT_PROPERTY_AUTHOR, ContextProperties::extractAuthor), //
            EDITOR(CONTEXT_PROPERTY_EDITOR, ContextProperties::extractEditor),
            CREATION_DATE(CONTEXT_PROPERTY_CREATION_DATE, ContextProperties::extractCreationDate),
            LAST_MODIFIED(CONTEXT_PROPERTY_LAST_MODIFIED, ContextProperties::extractLastModified),
            JOB_ID(CONTEXT_PROPERTY_JOB_ID, ContextProperties::extractJobId), //
            JOB_ACCOUNT_ID(CONTEXT_PROPERTY_JOB_ACCOUNT_ID, ContextProperties::extractJobAccountId),
            JOB_ACCOUNT_NAME(CONTEXT_PROPERTY_JOB_ACCOUNT_NAME, ContextProperties::extractJobAccountName),

            /* Properties introduced with AP 5.3 (AP-20735). */
            EXECUTOR_USER_NAME(CONTEXT_PROPERTY_EXECUTOR_USER_NAME, ContextProperties::extractExecutorUserName),
            EXECUTOR_VERSION(CONTEXT_PROPERTY_EXECUTOR_VERSION, ContextProperties::extractExecutorVersion),
            HUB_ITEM_ID(CONTEXT_PROPERTY_HUB_ITEM_ID,
                ContextProperties::extractHubItemId),
            HUB_SPACE_ID(CONTEXT_PROPERTY_HUB_SPACE_ID, ContextProperties::extractHubSpaceId),
            HUB_SPACE_PATH(CONTEXT_PROPERTY_HUB_SPACE_PATH, ContextProperties::extractHubSpacePath),
            HUB_API_BASE_URL(CONTEXT_PROPERTY_HUB_API_BASE_URL, ContextProperties::extractHubApiBaseUrl);

        private final String m_property;

        private final BiFunction<NodeContext, WorkflowContextV2, String> m_extractor;

        Property(final String property, final BiFunction<NodeContext, WorkflowContextV2, String> extractor) {
            m_property = property;
            m_extractor = extractor;
        }

        public String getProperty() {
            return m_property;
        }
    }

    /** Context variable name for workflow name. */
    public static final String CONTEXT_PROPERTY_WORKFLOW_NAME = "context.workflow.name";

    /** Context variable name for mount-point-relative workflow path.
     */
    public static final String CONTEXT_PROPERTY_WORKFLOW_PATH = "context.workflow.path";

    /** Context variable name for absolute workflow path. */
    public static final String CONTEXT_PROPERTY_WORKFLOW_ABSOLUTE_PATH = "context.workflow.absolute-path";

    /** Context variable name for workflow user ID. */
    public static final String CONTEXT_PROPERTY_SERVER_USER = "context.workflow.user";

    /** Context variable name for workflow temporary location. */
    public static final String CONTEXT_PROPERTY_TEMP_LOCATION = "context.workflow.temp.location";

    /** Context variable name for workflow author's name. */
    public static final String CONTEXT_PROPERTY_AUTHOR = "context.workflow.author.name";

    /** Context variable name for workflow last editor's name. */
    public static final String CONTEXT_PROPERTY_EDITOR = "context.workflow.last.editor.name";

    /** Context variable name for the creation date of the workflow. */
    public static final String CONTEXT_PROPERTY_CREATION_DATE = "context.workflow.creation.date";

    /** Context variable name for last modified time of workflow. */
    public static final String CONTEXT_PROPERTY_LAST_MODIFIED = "context.workflow.last.time.modified";

    /** Context variable name for job id when run on server. */
    private static final String CONTEXT_PROPERTY_JOB_ID = "context.job.id";

    /** Context variable name for job id when run on server. */
    private static final String CONTEXT_PROPERTY_JOB_ACCOUNT_ID = "context.job.account.id";

    /** Context variable name for job id when run on server. */
    private static final String CONTEXT_PROPERTY_JOB_ACCOUNT_NAME = "context.job.account.name";

    /** Context variable name for workflow user name. */
    private static final String CONTEXT_PROPERTY_EXECUTOR_USER_NAME = "context.workflow.username";

    /** Context variable name for executor AP version. */
    private static final String CONTEXT_PROPERTY_EXECUTOR_VERSION = "context.workflow.executor.version";

    /** Context variable name for workflow ID when stored on KNIME Hub. */
    private static final String CONTEXT_PROPERTY_HUB_ITEM_ID = "context.workflow.hub.item.id";

    /** Context variable name for the enclosing space's ID when stored on KNIME Hub. */
    private static final String CONTEXT_PROPERTY_HUB_SPACE_ID = "context.workflow.hub.space.id";

    /** Context variable name for the enclosing space's path when stored on KNIME Hub. */
    private static final String CONTEXT_PROPERTY_HUB_SPACE_PATH = "context.workflow.hub.space.path";

    /** Context variable name for the enclosing space's path when stored on KNIME Hub. */
    private static final String CONTEXT_PROPERTY_HUB_API_BASE_URL = "context.workflow.hub.api.base-url";

    /**
     * Extracts the value of a context property.
     *
     * @param property the name of the context property.
     * @return the non-{@code null}, but possibly empty, value of the context property.
     * @throws IllegalArgumentException of {@code property} is not the name of a context property.
     */
    public static String extractContextProperty(final String property) { // NOSONAR
        final var optP = Arrays.stream(Property.values()).filter(p -> p.getProperty().equals(property)).findAny();
        final var prop =
            optP.orElseThrow(() -> new IllegalArgumentException("Not a context property : \"" + property + '"'));

        final var nodeContext = NodeContext.getContext();
        final var contextV2 = nodeContext.getContextObjectForClass(WorkflowContextV2.class).orElse(null);
        return prop.m_extractor.apply(nodeContext, contextV2);
    }

    private static String extractWorkflowName(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        final var manager = nodeContext.getWorkflowManager();
        if (manager != null) {
            return manager.getName();
        } else if (contextV2 != null && contextV2.getLocationInfo() instanceof RestLocationInfo restInfo) {
            return Path.forPosix(restInfo.getWorkflowPath()).lastSegment();
        }
        return "";

    }

    private static String extractJobId(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 != null && contextV2.getExecutorType() != ExecutorType.ANALYTICS_PLATFORM) {
            return ((JobExecutorInfo)contextV2.getExecutorInfo()).getJobId().toString();
        }
        return "";
    }

    private static String extractWorkflowPath(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 == null) {
            return "";
        }

        // TODO the following logic doesn't work correctly in temp-copy "yellow bar" mode
        if (contextV2.getExecutorType() != ExecutorType.ANALYTICS_PLATFORM) {
            return ((RestLocationInfo)contextV2.getLocationInfo()).getWorkflowPath();
        }

        final var executorInfo = (AnalyticsPlatformExecutorInfo)contextV2.getExecutorInfo();
        final var mountPoint = executorInfo.getMountpoint().orElse(null);
        if (mountPoint == null) {
            return "";
        }

        final var wfLocation = executorInfo.getLocalWorkflowPath().toAbsolutePath();
        final var mpLocation = mountPoint.getSecond().toAbsolutePath();
        CheckUtils.checkState(wfLocation.startsWith(mpLocation),
            "Workflow '%s' is not contained in mountpoint root '%s'.", wfLocation, mpLocation);
        final var relPath = Path.fromOSString(mpLocation.relativize(wfLocation).toString()).makeAbsolute();
        return relPath.toString();
    }

    private static String extractWorkflowAbsolutePath(final NodeContext nodeContext,
        final WorkflowContextV2 contextV2) {
        final var wfLocation = contextV2 == null ? null : contextV2.getExecutorInfo().getLocalWorkflowPath();
        if (wfLocation == null) {
            return "";
        }
        // Note: this isn't safe in general because POSIX paths can contain backslashes
        return wfLocation.toFile().getAbsolutePath().replace("\\", "/");
    }

    private static String extractHubApiBaseUrl(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 != null && contextV2.getLocationInfo() instanceof HubSpaceLocationInfo hubLocation) {
            final var repositoryAddress = hubLocation.getRepositoryAddress();
            return getHubApiBaseUrl(repositoryAddress).orElse("");
        }
        return "";
    }

    private static String extractExecutorUserName(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 == null) {
            return "";
        }
        final var executorInfo = contextV2.getExecutorInfo();
        return executorInfo instanceof HubJobExecutorInfo hubInfo ? hubInfo.getJobCreatorName()
            : executorInfo.getUserId();
    }

    private static String extractExecutorVersion(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 == null) {
            return "";
        }

        if (contextV2.getExecutorInfo() instanceof JobExecutorInfo jobExec && jobExec.isRemote()) {
            // version is reported via the RWE
            return jobExec.getRemoteExecutorVersion().orElse("");
        }
        return KNIMEConstants.VERSION;
    }

    private static String extractHubItemId(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 != null && contextV2.getLocationInfo() instanceof HubSpaceLocationInfo hubInfo) {
            return hubInfo.getWorkflowItemId();
        }
        return "";
    }

    private static String extractHubSpaceId(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 != null && contextV2.getLocationInfo() instanceof HubSpaceLocationInfo hubInfo) {
            return hubInfo.getSpaceItemId();
        }
        return "";
    }

    private static String extractHubSpacePath(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 != null && contextV2.getLocationInfo() instanceof HubSpaceLocationInfo hubInfo) {
            return hubInfo.getSpacePath();
        }
        return "";
    }

    private static String extractJobAccountId(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 != null && contextV2.getExecutorInfo() instanceof HubJobExecutorInfo hubJob) {
            return hubJob.getScopeId();
        }
        return "";
    }

    private static String extractJobAccountName(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        if (contextV2 != null && contextV2.getExecutorInfo() instanceof HubJobExecutorInfo hubJob) {
            return hubJob.getScopeName();
        }
        return "";
    }

    private static String extractAuthor(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        final var authInfo = Optional.ofNullable(nodeContext.getWorkflowManager())
            .map(WorkflowManager::getAuthorInformation).orElse(null);
        if (authInfo != null) {
            return Optional.ofNullable(authInfo.getAuthor()).orElse("");
        }
        return "";
    }

    private static String extractEditor(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        final var authInfo = Optional.ofNullable(nodeContext.getWorkflowManager())
            .map(WorkflowManager::getAuthorInformation).orElse(null);
        if (authInfo != null) {
            final var author = Optional.ofNullable(authInfo.getAuthor()).orElse("");
            /**
             * If there is no known last editor (e.g., since the workflow has just been created), the original author is
             * returned as last editor.
             */
            return authInfo.getLastEditor().orElse(author);
        }
        return "";
    }

    private static String extractCreationDate(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        final var authInfo = Optional.ofNullable(nodeContext.getWorkflowManager())
            .map(WorkflowManager::getAuthorInformation).orElse(null);
        if (authInfo != null) {
            return Optional.ofNullable(authInfo.getAuthoredDate()).map(Object::toString).orElse("");
        }
        return "";
    }

    private static String extractLastModified(final NodeContext nodeContext, final WorkflowContextV2 contextV2) {
        final var authInfo = Optional.ofNullable(nodeContext.getWorkflowManager())
            .map(WorkflowManager::getAuthorInformation).orElse(null);
        if (authInfo != null) {
            final var dateCreated = Optional.ofNullable(authInfo.getAuthoredDate()).map(Object::toString).orElse("");
            /**
             * If there is no known last edit date (e.g., since the workflow has just been created), the created date is
             * returned as last edit date.
             */
            return authInfo.getLastEditDate().map(Object::toString).orElse(dateCreated);
        }
        return "";
    }

    /**
     * Extrapolates the Hub API base URL from the Hub repository address. This removes
     * <ul>
     * <li>the (optional) {@code /knime/rest/v4} and
     * <li>the (mandatory) {@code /repository} segment
     * </ul>
     * from the end of the URL. So both {@code https://127.0.0.1/foo/bar/knime/rest/v4/repository/} and
     * {https://127.0.0.1/foo/bar/repository/} will be rewritten to {@code https://127.0.0.1/foo/bar}.
     *
     * @param repositoryAddress repository address from the workflow context
     * @return hub API base URL as a string or {@link Optional#empty()} if no {@code /repository} segment was found
     */
    private static Optional<String> getHubApiBaseUrl(final URI repositoryAddress) {
        final var uriBuilder = new URIBuilder(repositoryAddress);
        final var path = uriBuilder.getPathSegments();
        int end = path.size();

        // skip empty segments (trailing slashes)
        while (end > 0 && StringUtils.isEmpty(path.get(end - 1))) {
            end--;
        }

        // expect and skip `repository` segment
        if (end == 0 || !"repository".equals(path.get(end - 1))) {
            return Optional.empty();
        }
        end--;

        // skip `/knime/rest/v4` suffix if found, it's synthetic
        if (end >= 3 && path.subList(end - 3, end).equals(List.of("knime", "rest", "v4"))) {
            end -= 3;
        }

        // remove all skipped segments
        uriBuilder.setPathSegments(path.subList(0, end));

        return Optional.of(uriBuilder.toString());
    }

    /**
     * Gets the list of all context properties.
     *
     * @return an unmodifiable list of context property names.
     */
    public static List<String> getContextProperties() {
        return Arrays.stream(Property.values()).map(Property::getProperty).toList();
    }

    private ContextProperties() {
        throw new UnsupportedOperationException();
    }

}
