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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;

/**
 * Provides basic functionality for the {@link CachingTemplateUpdater}. Includes overriding basic methods where the
 * stored WFM can be used to fill default values, e.g. checking all updates.
 *
 * Specifies the class {@link TemplateInformationStatus} which is used to keep track of scanned NodeID's and their
 * associated {@link MetaNodeTemplateInformation}s.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
abstract sealed class AbstractTemplateUpdater implements TemplateUpdater permits CachingTemplateUpdater {

    private final WorkflowManager m_wfm;

    private final Map<NodeID, TemplateInformationStatus> m_templateStatuses;

    /**
     * TODO: Find out how to connect this to and redirect from the {@link WorkflowManager}.
     *
     * @param wfm WorkflowManager.
     */
    protected AbstractTemplateUpdater(final WorkflowManager wfm) {
        this.m_wfm = wfm;
        this.m_templateStatuses = new HashMap<>();
    }

    /**
     * Checks if any node container templates with an update exist.
     *
     * @return has at least one update?
     */
    public boolean isUpdatesAvailable() {
        return this.getNumberUpdatesAvailable() > 0;
    }

    /**
     * Returns the number of currently stored HasUpdate statuses for NodeIDs. This disregards the definitive flag of
     * {@link TemplateInformationStatus} because this method is and can only be used as a rough estimate on how many
     * updates to do.
     *
     * @return Number of current updates available.
     */
    public long getNumberUpdatesAvailable() {
        return m_templateStatuses.values().stream().filter(TemplateInformationStatus::hasUpdate).count();
    }

    @Override
    public Map<NodeID, UpdateStatus> checkUpdateForAllTemplates() {
        return this.checkUpdateForTemplates(this.findAllNodeContainerTemplates());
    }

    /**
     * Recursively scans the stored WFM's {@link NodeContainerTemplate}s, and collects them into an array.
     *
     * @return Array of NodeIDs corresponding to updateable NodeContainerTemplates.
     */
    private NodeID[] findAllNodeContainerTemplates() {
        List<NodeID> nctList = new LinkedList<>();
        findAllNodeContainerTemplates(this.m_wfm, nctList);
        return nctList.toArray(new NodeID[nctList.size()]);
    }

    /**
     * Recursive method to collect all updateable NodeContainerTemplates.
     *
     * @param node NodeContainerTemplate entry point
     * @param nctList list to collect the NodeIDs in
     */
    private void findAllNodeContainerTemplates(final NodeContainerTemplate node, final Collection<NodeID> nctList) {
        for (NodeContainer nc : node.getNodeContainers()) {
            if (nc instanceof NodeContainerTemplate nct) {
                // TODO: Does the root WFM now about each of the nested NCT's updateable state?
                if (this.m_wfm.canUpdateMetaNodeLink(nct.getID())) {
                    nctList.add(nct.getID());
                }
                findAllNodeContainerTemplates(nct, nctList);
            }
        }
    }

    @Override
    public IStatus updateTemplatesShallow() {
        // Shallow update only looks on the top level.
        return this.updateAllTemplatesRecursively(1);
    }

    @Override
    public IStatus updateTemplatesDeep() {
        // Deep update has no depth limit.
        return this.updateAllTemplatesRecursively(Integer.MAX_VALUE);
    }

    /**
     * Recursive updating process, updates the children of NodeContainerTemplates as far as the `depthLimit` specifies.
     * Updates are only executed if `depthLimit > 0`. Each recursive call must decrement the depth limit. Summarizes all
     * update processes into a single status to be displayed.
     *
     * @param depthLimit integer limit of recursive depth
     * @return Summary IStatus
     */
    protected abstract IStatus updateAllTemplatesRecursively(final int depthLimit);

    @Override
    public void clearCaches() {
        this.m_templateStatuses.clear();
        // Overriden by the implementation, usually contains downloaded artifacts.
        this.clearCachesInternal();
    }

    /**
     * Clears internal caches, which are used for a more efficient update routine.
     */
    protected abstract void clearCachesInternal();

    /**
     * Pair of a stored {@link MetaNodeTemplateInformation} instance and a flag, indicating whether it is defintive. A
     * definitive TemplateInformationStatus cannot be changed anymore. The definitive flag is useful for when estimating
     * updates of nested linked templates. Because of the unpredictable nature of updates the a nested
     * {@link MetaNodeTemplateInformation} can totally change with a parent update or stay the same.
     *
     * For this reason, any non-top-level NodeContainerTemplates receive a non-defintive TemplateInformationStatus.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    protected static class TemplateInformationStatus {

        private boolean m_definitive;

        private MetaNodeTemplateInformation m_information;

        /**
         * Private construtor, only to be called from create*() methods.
         *
         * @param info MetaNodeTemplateInformation
         */
        private TemplateInformationStatus(final MetaNodeTemplateInformation info) {
            CheckUtils.checkNotNull(info, "New template information must not be null");
            this.m_information = info;
            // Per default non-definitive statuses are created, i.e. they are to be updated.
            this.m_definitive = false;
        }

        /**
         * Getter for the definitive flag.
         *
         * @return is definitive?
         */
        boolean isDefinitive() {
            return m_definitive;
        }

        /**
         * Sets this instance to definitive. This locks it from any change-inducing updates and signals the update
         * process the "real" status. This action is irreversible.
         *
         * @return
         */
        TemplateInformationStatus toDefinitive() {
            m_definitive = true;
            return this;
        }

        /**
         * Getter for whether the stored {@link MetaNodeTemplateInformation} contains a UpdateStatus.HasUpdate.
         *
         * @return has this metanode/component an update available?
         */
        boolean hasUpdate() {
            return this.m_information.getUpdateStatus() == UpdateStatus.HasUpdate;
        }

        /**
         * Updates the store {@link MetaNodeTemplateInformation}, e.g. after an parent template update. Only possible on
         * non-definitive instances.
         *
         * @param info {@link MetaNodeTemplateInformation}
         */
        void updateInformation(final MetaNodeTemplateInformation info) {
            CheckUtils.checkNotNull(info, "New template information must not be null");
            CheckUtils.checkArgument(!m_definitive, "Cannot update a definitive template information status");
            this.m_information = info;
        }

        /**
         * Creates a non-definitive {@link TemplateInformationStatus}.
         *
         * @param info {@link MetaNodeTemplateInformation}.
         * @return status
         */
        static TemplateInformationStatus create(final MetaNodeTemplateInformation info) {
            return new TemplateInformationStatus(info);
        }

        /**
         * Creates a definitive {@link TemplateInformationStatus}.
         *
         * @param info {@link MetaNodeTemplateInformation}.
         * @return status
         */
        static TemplateInformationStatus createDefintive(final MetaNodeTemplateInformation info) {
            return create(info).toDefinitive();
        }
    }
}
