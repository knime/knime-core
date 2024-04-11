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
 *   Apr 11, 2024 (lw): created
 */
package org.knime.testing.util;

import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.Version;

/**
 * Specifies to always try loading the given workflow, irrespective of its version.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public class TryAlwaysWorkflowLoadHelper extends WorkflowLoadHelper {

    /** Default instance. */
    public TryAlwaysWorkflowLoadHelper() {
        super(false);
    }

    /**
     * Creates a new load helper with the given workflow context.
     *
     * @param workflowContext a workflow context
     */
    public TryAlwaysWorkflowLoadHelper(final WorkflowContextV2 workflowContext) {
        super(workflowContext);
    }

    /**
     * Creates a new load helper with the given workflow context and data container settings.
     *
     * @param context workflow context
     * @param settings data container settings
     */
    public TryAlwaysWorkflowLoadHelper(final WorkflowContextV2 context, final DataContainerSettings settings) {
        super(context, settings);
    }

    /**
     * Creates a new load helper with the given workflow context.
     *
     * @param isTemplate whether this is a template loader
     * @param workflowContext a workflow context
     */
    public TryAlwaysWorkflowLoadHelper(final boolean isTemplate, final WorkflowContextV2 workflowContext) {
        super(isTemplate, workflowContext);
    }

    /**
     * Creates a new load helper with the given workflow context.
     *
     * @param isTemplate whether this is a template loader
     * @param isTemplateProject whether this template is a template project, i.e. not part of a workflow. If
     *            <code>true</code>, <code>isTemplate</code> must be <code>true</code>, too.
     * @param workflowContext a workflow context
     * @throws IllegalStateException if <code>isTemplateProject</code> is <code>true</code>, but <code>isTemplate</code>
     *             isn't
     */
    public TryAlwaysWorkflowLoadHelper(final boolean isTemplate, final boolean isTemplateProject,
        final WorkflowContextV2 workflowContext) {
        super(isTemplate, isTemplateProject, workflowContext);
    }

    /** @param isTemplate whether this is a template loader */
    public TryAlwaysWorkflowLoadHelper(final boolean isTemplate) {
        super(isTemplate);
    }

    @Override
    public UnknownKNIMEVersionLoadPolicy getUnknownKNIMEVersionLoadPolicy(final LoadVersion workflowKNIMEVersion,
        final Version createdByKNIMEVersion, final boolean isNightlyBuild) {
        return UnknownKNIMEVersionLoadPolicy.Try;
    }
}
