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
 *   May 30, 2023 (Leon Wenzler, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.AbstractTemplateUpdater.TemplateOperationContext;
import org.knime.core.node.workflow.TemplateUpdateUtil.TemplateUpdateCheckResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * Interface for all {@link NodeContainerTemplate}-relevant link update actions. Includes checking and performing
 * updates. Sets the internal (and visible) update statuses accordingly.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public sealed interface TemplateUpdater extends GeneralTemplateUpdater permits AbstractTemplateUpdater {

    /**
     * Allows the use of more advanced context properties for checking and performing template updates.
     * These properties include:
     * <ul>
     *   <li> a {@link WorkflowLoadHelper} representing a callback during template loads
     *   <li> a {@link LoadResult} summarizing the results from loading templates
     *   <li> a given cache for {@link TemplateUpdateCheckResult} to use and fill
     *   <li> an {@link ExecutionMonitor} for tracking the operations and progress
     * </ul>
     *
     * @return {@link TemplateOperationContext}
     */
    @SuppressWarnings("javadoc")
    TemplateOperationContext withContext();

    /**
     * Creates the current default {@link TemplateUpdater} instance for a given {@link WorkflowManager}.
     *
     * @param manager WorkflowManager to use
     * @return template updater
     */
    static TemplateUpdater forWorkflowManager(final WorkflowManager manager) {
        // TODO: Switch this to the SingleCacheTemplateUpdater eventually.
        return new LegacyTemplateUpdater(manager);
    }
}
