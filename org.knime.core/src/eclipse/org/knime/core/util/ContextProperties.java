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
import java.util.Date;
import java.util.List;

import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowManager.AuthorInformation;

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
        contextProperties.trimToSize();
        CONTEXT_PROPERTIES = Collections.unmodifiableList(contextProperties);
    }

    /**
     * Extracts the value of a context property.
     *
     * @param property the name of the context property.
     * @return the non-{@code null}, but possibly emtpy, value of the context property.
     * @throws IllegalArgumentException of {@code property} is not the name of a context property.
     */
    public static String extractContextProperty(final String property) {
        final WorkflowManager manager = NodeContext.getContext().getWorkflowManager();
        if (CONTEXT_PROPERTY_WORKFLOW_NAME.equals(property)) {
            return manager.getName();
        }
        if (CONTEXT_PROPERTY_WORKFLOW_PATH.equals(property)) {
            final WorkflowContext context = manager.getContext();
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
        if (CONTEXT_PROPERTY_TEMP_LOCATION.equals(property)) {
            return manager.getContext().getTempLocation().getAbsolutePath();
        }
        final AuthorInformation author = manager.getAuthorInformation();
        if (author != null) {
            if (CONTEXT_PROPERTY_AUTHOR.equals(property)) {
                return author.getAuthor();
            }
            if (CONTEXT_PROPERTY_EDITOR.equals(property)) {
                return author.getLastEditor();
            }
            if (CONTEXT_PROPERTY_CREATION_DATE.equals(property)) {
                final Date creationDate = author.getAuthoredDate();
                if (creationDate != null) {
                    return creationDate.toString();
                }
            }
            if (CONTEXT_PROPERTY_LAST_MODIFIED.equals(property)) {
                final Date modDate = author.getLastEditDate();
                if (modDate != null) {
                    return modDate.toString();
                }
            }
        } else if (CONTEXT_PROPERTY_AUTHOR.equals(property) || CONTEXT_PROPERTY_EDITOR.equals(property)
            || CONTEXT_PROPERTY_CREATION_DATE.equals(property) || CONTEXT_PROPERTY_LAST_MODIFIED.equals(property)) {
            return null;
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
