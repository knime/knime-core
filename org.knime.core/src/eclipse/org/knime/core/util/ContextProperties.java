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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.HubJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.workflowalizer.AuthorInformation;

/**
 * Static constants and utility methods for working with KNIME context properties.
 *
 * @author Noemi Balassa
 * @since 3.7
 */
public final class ContextProperties {

    /**
     * Context variable name for workflow name.
     */
    public static final String CONTEXT_PROPERTY_WORKFLOW_NAME = "context.workflow.name";

    /**
     * Context variable name for mount-point-relative workflow path.
     */
    public static final String CONTEXT_PROPERTY_WORKFLOW_PATH = "context.workflow.path";

    /**
     * Context variable name for absolute workflow path.
     */
    public static final String CONTEXT_PROPERTY_WORKFLOW_ABSOLUTE_PATH = "context.workflow.absolute-path";

    /**
     * Context variable name for workflow user.
     */
    public static final String CONTEXT_PROPERTY_SERVER_USER = "context.workflow.user";

    /**
     * Context variable name for workflow temporary location.
     */
    public static final String CONTEXT_PROPERTY_TEMP_LOCATION = "context.workflow.temp.location";

    /**
     * Context variable name for workflow author's name.
     */
    public static final String CONTEXT_PROPERTY_AUTHOR = "context.workflow.author.name";

    /**
     * Context variable name for workflow last editor's name.
     */
    public static final String CONTEXT_PROPERTY_EDITOR = "context.workflow.last.editor.name";

    /**
     * Context variable name for the creation date of the workflow.
     */
    public static final String CONTEXT_PROPERTY_CREATION_DATE = "context.workflow.creation.date";

    /**
     * Context variable name for last modified time of workflow.
     */
    public static final String CONTEXT_PROPERTY_LAST_MODIFIED = "context.workflow.last.time.modified";

    /**
     * Context variable name for job id when run on server.
     */
    private static final String CONTEXT_PROPERTY_JOB_ID = "context.job.id";

    /**
     * Context variable name for workflow user name (not listed in {@link #getContextProperties()} in AP 5.2.x).
     * @since 5.2
     */
    public static final String CONTEXT_PROPERTY_EXECUTOR_USER_NAME = "context.workflow.username";

    /**
     * The list of all context properties.
     */
    private static final List<String> CONTEXT_PROPERTIES;

    static {
        final ArrayList<String> contextProperties = new ArrayList<>();
        contextProperties.add(CONTEXT_PROPERTY_WORKFLOW_NAME);
        contextProperties.add(CONTEXT_PROPERTY_WORKFLOW_PATH);
        contextProperties.add(CONTEXT_PROPERTY_WORKFLOW_ABSOLUTE_PATH);
        contextProperties.add(CONTEXT_PROPERTY_SERVER_USER);
        contextProperties.add(CONTEXT_PROPERTY_TEMP_LOCATION);
        contextProperties.add(CONTEXT_PROPERTY_AUTHOR);
        contextProperties.add(CONTEXT_PROPERTY_EDITOR);
        contextProperties.add(CONTEXT_PROPERTY_CREATION_DATE);
        contextProperties.add(CONTEXT_PROPERTY_LAST_MODIFIED);
        contextProperties.add(CONTEXT_PROPERTY_JOB_ID);
        contextProperties.trimToSize();
        CONTEXT_PROPERTIES = Collections.unmodifiableList(contextProperties);
    }

    /**
     * Extracts the value of a context property.
     *
     * @param property the name of the context property.
     * @return the non-{@code null}, but possibly empty, value of the context property.
     * @throws IllegalArgumentException of {@code property} is not the name of a context property.
     */
    public static String extractContextProperty(final String property) {
        final WorkflowManager manager = NodeContext.getContext().getWorkflowManager();
        if (CONTEXT_PROPERTY_WORKFLOW_NAME.equals(property)) {
            return manager.getName();
        }
        if (CONTEXT_PROPERTY_JOB_ID.equals(property)) {
            final WorkflowContext context = manager.getContext();
            return context == null ? null : context.getJobId().map(UUID::toString).orElse(null);
        }
        if (CONTEXT_PROPERTY_WORKFLOW_PATH.equals(property)) {
            final WorkflowContext context = manager.getContext();
            if (context.getRelativeRemotePath().isPresent()) {
                return context.getRelativeRemotePath().get();
            }

            final File wfLocation =
                context.getOriginalLocation() == null ? context.getCurrentLocation() : context.getOriginalLocation();
            final File mpLocation = context.getMountpointRoot();
            if (mpLocation == null || wfLocation == null) {
                return "";
            }
            final String wfPath = wfLocation.getAbsolutePath();
            final String mpPath = mpLocation.getAbsolutePath();
            assert wfPath.startsWith(mpPath);
            final String resultPath = wfPath.substring(mpPath.length());
            return resultPath.replace("\\", "/");
        }
        if (CONTEXT_PROPERTY_WORKFLOW_ABSOLUTE_PATH.equals(property)) {
            final WorkflowContext context = manager.getContext();
            final File wfLocation = context.getCurrentLocation();
            if (wfLocation == null) {
                return "";
            }
            return wfLocation.getAbsolutePath().replace("\\", "/");
        }
        if (CONTEXT_PROPERTY_SERVER_USER.equals(property)) {
            return manager.getContext().getUserid();
        }
        if (CONTEXT_PROPERTY_EXECUTOR_USER_NAME.equals(property)) {
            final WorkflowContextV2 contextV2 = manager.getContextV2();
            if (contextV2 == null) {
                return "";
            }
            final var executorInfo = contextV2.getExecutorInfo();
            return executorInfo instanceof HubJobExecutorInfo hubInfo ? hubInfo.getJobCreatorName()
                : executorInfo.getUserId();
        }
        if (CONTEXT_PROPERTY_TEMP_LOCATION.equals(property)) {
            return manager.getContext().getTempLocation().getAbsolutePath();
        }
        final AuthorInformation authInfo = manager.getAuthorInformation();
        if (authInfo != null) {
            final String author = Optional.ofNullable(authInfo.getAuthor()).orElse("");
            final String dateCreated = Optional.ofNullable(authInfo.getAuthoredDate()).map(Object::toString).orElse("");
            if (CONTEXT_PROPERTY_AUTHOR.equals(property)) {
                return author;
            }
            if (CONTEXT_PROPERTY_EDITOR.equals(property)) {
                /** If there is no known last editor (e.g., since the workflow has just been created), the original
                 * author is returned as last editor. */
                return authInfo.getLastEditor().orElse(author);
            }
            if (CONTEXT_PROPERTY_CREATION_DATE.equals(property)) {
                return dateCreated;
            }
            if (CONTEXT_PROPERTY_LAST_MODIFIED.equals(property)) {
                /** If there is no known last edit date (e.g., since the workflow has just been created), the created
                 * date is returned as last edit date. */
                return authInfo.getLastEditDate().map(Object::toString).orElse(dateCreated);
            }
        } else if (CONTEXT_PROPERTY_AUTHOR.equals(property) || CONTEXT_PROPERTY_EDITOR.equals(property)
            || CONTEXT_PROPERTY_CREATION_DATE.equals(property) || CONTEXT_PROPERTY_LAST_MODIFIED.equals(property)) {
            return "";
        }

        throw new IllegalArgumentException("Not a context property : \"" + property + '"');
    }

    /**
     * Gets the list of all context properties.
     *
     * @return an unmodifiable list of context property names.
     */
    public static List<String> getContextProperties() {
        return CONTEXT_PROPERTIES;
    }

    private ContextProperties() {
        throw new UnsupportedOperationException();
    }

}
