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

import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;

/**
 * Parent interface for {@link TemplateUpdater} which contains the general interface
 * for checking and performing updates. The {@link TemplateUpdater} has more details on this.
 *
 * @see TemplateUpdater
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public sealed interface GeneralTemplateUpdater permits TemplateUpdater {

    /**
     * Checks for updates on the given selection of NodeIDs. Returns a map of the found {@link UpdateStatus}es per
     * {@link NodeID}.
     *
     * @param nodeIds Array of NodeIDs, representing updateable {@link NodeContainerTemplate}s.
     * @param recurseInto whether to recurse into each of the selected NodeIDs.
     * @return Map between node ID and its update status.
     */
    Map<NodeID, UpdateStatus> checkUpdateForTemplates(final NodeID[] nodeIds, final boolean recurseInto);

    /**
     * Checks for update of all *top-level*, unlinked NodeContainerTemplates in the workflow.
     * Returns a map of the found {@link UpdateStatus}es per {@link NodeID}.
     *
     * @param nodeIds Array of NodeIDs, representing updateable {@link NodeContainerTemplate}s.
     * @return Map between node ID and its update status.
     */
    Map<NodeID, UpdateStatus> checkUpdateForTemplatesShallow();

    /**
     * Checks for updates on all updateable NodeContainerTemplates in the workflow. Returns a map of the found
     * {@link UpdateStatus}es per {@link NodeID}.
     *
     * @param nodeIds Array of NodeIDs, representing updateable {@link NodeContainerTemplate}s.
     * @return Map between node ID and its update status.
     */
    Map<NodeID, UpdateStatus> checkUpdateForTemplatesDeep();

    /**
     * Updates the given selection of {@link NodeID}s, uses potentially cached information from earlier runs of an
     * update-checking method. If no previous run was found, it performs a new update check before.
     * Returns a summary status on how the updating went, useful for displaying information to the user.
     *
     * @param nodeIds Array of NodeIDs, representing updateable {@link NodeContainerTemplate}s.
     * @param recurseInto whether to recurse into each of the selected NodeIDs.
     * @return Summary IStatus
     */
    IStatus updateTemplates(final NodeID[] nodeIds, final boolean recurseInto);

    /**
     * Updates the all *top-level*, unlinked NodeContainerTemplates in the workflow.
     * Uses potentially cached information from earlier runs of an update-checking method.
     * If no previous run was found, it performs a new update check before.
     * Returns a summary status on how the updating went, useful for displaying information to the user.
     *
     * @param nodeIds Array of NodeIDs, representing updateable {@link NodeContainerTemplate}s.
     * @return Summary IStatus
     */
    IStatus updateTemplatesShallow();

    /**
     * Updates the *all* (deeply-nested) found NodeContainerTemplates in the workflow.
     * Uses potentially cached information from earlier runs of an update-checking method.
     * If no previous run was found, it performs a new update check before.
     * Returns a summary status on how the updating went, useful for displaying information to the user.
     *
     * @param nodeIds Array of NodeIDs, representing updateable {@link NodeContainerTemplate}s.
     * @return Summary IStatus
     */
    IStatus updateTemplatesDeep();

    /**
     * Clears the internal caches of found update states, operation contexts, and downloaded artifacts.
     */
    void reset();
}
