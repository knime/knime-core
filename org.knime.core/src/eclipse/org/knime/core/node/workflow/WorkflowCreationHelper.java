/*
 * ------------------------------------------------------------------------
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
 * Created on 03.06.2013 by thor
 */
package org.knime.core.node.workflow;

import java.util.Optional;

import org.knime.core.node.workflow.contextv2.WorkflowContextV2;

/**
 * Instances of this class are used during creation of new workflows. They contains meta information that is used to
 * create a workflow manager instance for the new workflow.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.8
 */
public class WorkflowCreationHelper {
    private WorkflowContextV2 m_context;
    private WorkflowDataRepository m_workflowDataRepository;
    private WorkflowTableBackendSettings m_tableBackendSettings;

    /**
     * Workflow creation helper without a workflow context.
     */
    @Deprecated(since = "4.7")
    public WorkflowCreationHelper() {
    }

    /**
     * Workflow creation helper with the given workflow context.
     *
     * @param context workflow context
     * @since 4.7
     */
    public WorkflowCreationHelper(final WorkflowContextV2 context) {
        m_context = context;
    }

    /**
     * Sets the context for the workflow that is being created.
     * @param context a workflow context
     * @return this (method chaining)
     */
    @Deprecated(since = "4.7")
    public WorkflowCreationHelper setWorkflowContext(final WorkflowContext context) {
        m_context = WorkflowContextV2.fromLegacyWorkflowContext(context);
        return this;
    }

    /**
     * Sets the context for the workflow that is being created.
     *
     * @param context a workflow context
     * @return this (method chaining)
     * @since 4.7
     */
    public WorkflowCreationHelper setWorkflowContext(final WorkflowContextV2 context) {
        m_context = context;
        return this;
    }

    /**
     * Returns a context for the workflow that is being loaded. If not context is available <code>null</code> is
     * returned.
     *
     * @return a workflow context or <code>null</code>
     */
    public WorkflowContextV2 getWorkflowContext() {
        return m_context;
    }

    /** Set data repository used to init the workflow with. Both args are usually null but if one is non-null the
     * other must not be null either. Used in "sandbox workflow" where the isolated workflow inherits the data
     * handlers of the original workflow.
     * @param workflowDataRepository ...
     * @return this (for method chaining)
     * @since 3.1
     * @noreference This method is not intended to be referenced by clients.
     */
    public WorkflowCreationHelper setWorkflowDataRepository(final WorkflowDataRepository workflowDataRepository) {
        m_workflowDataRepository = workflowDataRepository;
        return this;
    }

    /**
     * @return the data repository as set by {@link #setWorkflowDataRepository(WorkflowDataRepository)}
     * @since 3.1
     * @noreference This method is not intended to be referenced by clients.
     */
    public WorkflowDataRepository getWorkflowDataRepository() {
        return m_workflowDataRepository;
    }

    /**
     * @return custom table backend settings
     * @since 5.4
     */
    public Optional<WorkflowTableBackendSettings> getTableBackendSettings() {
        return Optional.ofNullable(m_tableBackendSettings);
    }

    /**
     * @param tableBackendSettings custom table backend settings, {@code null} for default settings
     * @since 5.4
     */
    public void setTableBackendSettings(final WorkflowTableBackendSettings tableBackendSettings) {
        m_tableBackendSettings = tableBackendSettings;
    }

}