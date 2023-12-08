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

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IStatus;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;
import org.knime.core.node.workflow.TemplateUpdateUtil.TemplateUpdateCheckResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * Provides basic functionality for the template updater implementations. Includes overriding basic methods where the
 * stored WFM was originally used to fill default values, e.g. checking all updates.
 *
 * Specifies the class {@link TemplateInformationStatus} which is used to keep track of scanned NodeID's, the mutability
 * of found results, and their associated {@link MetaNodeTemplateInformation}s.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
abstract sealed class AbstractTemplateUpdater implements TemplateUpdater
    permits LegacyTemplateUpdater, SingleCacheTemplateUpdater {

    protected static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractTemplateUpdater.class);

    protected final WorkflowManager m_wfm;

    protected final Map<NodeID, TemplateInformationStatus> m_updateStatesCache;

    protected Deque<TemplateOperationContext> m_contextStack;

    /**
     * Default constructor, only to be called from {@link TemplateUpdater}.
     *
     * @param wfm WorkflowManager.
     */
    protected AbstractTemplateUpdater(final WorkflowManager wfm) {
        m_wfm = wfm;
        m_updateStatesCache = new HashMap<>();
        m_contextStack = new ArrayDeque<>();
    }

    /**
     * Checks if any node container templates with an update exist.
     *
     * @return has at least one update?
     */
    public boolean isUpdatesAvailable() {
        return getNumberUpdatesAvailable() > 0;
    }

    /**
     * Returns the number of currently stored HasUpdate statuses for NodeIDs. This disregards the definitive flag of
     * {@link TemplateInformationStatus} because this method is and can only be used as a rough estimate on how many
     * updates to do.
     *
     * @return Number of current updates available.
     */
    public long getNumberUpdatesAvailable() {
        return m_updateStatesCache.values().stream().filter(TemplateInformationStatus::hasUpdate).count();
    }

    @Override
    public Map<NodeID, UpdateStatus> checkUpdateForTemplatesShallow() {
        return checkUpdateForTemplates(findTemplateEntryPoints(), false);
    }

    @Override
    public Map<NodeID, UpdateStatus> checkUpdateForTemplatesDeep() {
        return checkUpdateForTemplates(findTemplateEntryPoints(), true);
    }

    /**
     * Recursively scans the stored WFM's {@link NodeContainerTemplate}s, and collects them into an array.
     *
     * @return Array of NodeIDs corresponding to updateable NodeContainerTemplates.
     */
    private NodeID[] findTemplateEntryPoints() {
        final Map<NodeID, NodeContainerTemplate> entryPoints = new HashMap<>();
        m_wfm.fillLinkedTemplateNodesList(entryPoints, true, true);
        return entryPoints.keySet().toArray(NodeID[]::new);
    }

    /**
     * Stores retrieved updates states from {@link #checkUpdateForTemplates(NodeID[], boolean)} in the
     * {@link #m_updateStatesCache} caches for reuse and easy querying. Also notifies ands sets template update status
     * indicators (little red/green) arrow.
     *
     * TODO: Ensure consistent setting of the UpdateStatus in all error (!) and update cases. TODO: Make all linked
     * templates nested within linked templates as non-definitive.
     *
     * @param updateStates
     */
    protected void storeAndNotifyUpdateCheckResults(final Map<NodeID, UpdateStatus> updateStates) {
        for (Map.Entry<NodeID, UpdateStatus> entry : updateStates.entrySet()) {
            final var nodeId = entry.getKey();
            final var status = entry.getValue();
            final var container = m_wfm.findNodeContainer(nodeId);
            if (container instanceof NodeContainerTemplate template) {
                final var info = Objects.requireNonNull(template.getTemplateInformation());
                if (info.setUpdateStatusInternal(status)) {
                    template.notifyTemplateConnectionChangedListener();
                }
                // if the template lies within another linked template, don't mark as definitive
                final var infoStatus = TemplateInformationStatus.create(info);
                if (!isTemplateNestedWithinLinked(container)) {
                    infoStatus.toDefinitive();
                }
                m_updateStatesCache.put(nodeId, infoStatus);
            }
        }
    }

    /**
     * Iteratively retrieves the parents of a found {@link NodeContainer} template
     * to check if one of them is a linked template. Returns true, if that's the case.
     *
     * TODO: Is there a better way to check that?
     *
     * @param container nodeContainer to check
     * @return lies within linked template?
     */
    private static boolean isTemplateNestedWithinLinked(final NodeContainer container) {
        var parent = container.getDirectNCParent();
        while (parent instanceof NodeContainerTemplate parentTemplate) {
            if (parentTemplate.getTemplateInformation().getRole() == Role.Link) {
                return true;
            }
            parent = parent.getDirectNCParent();
        }
        return false;
    }

    @Override
    public IStatus updateTemplatesShallow() {
        // shallow update scans on the top level
        return updateAllTemplatesRecursively(1);
    }

    @Override
    public IStatus updateTemplatesDeep() {
        // deep update has no depth limit
        return updateAllTemplatesRecursively(Integer.MAX_VALUE);
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
    public void reset() {
        m_updateStatesCache.clear();
        m_contextStack.clear();
        // usually contains downloaded artifacts
        resetInternal();
    }

    /**
     * Clears internal caches, which are used for a more efficient update routine.
     */
    protected abstract void resetInternal();

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
    protected static final class TemplateInformationStatus {

        private boolean m_definitive;

        private MetaNodeTemplateInformation m_information;

        /**
         * Private construtor, only to be called from create*() methods.
         *
         * @param info MetaNodeTemplateInformation
         */
        private TemplateInformationStatus(final MetaNodeTemplateInformation info) {
            CheckUtils.checkNotNull(info, "New template information must not be null");
            m_information = info;
            // per default non-definitive statuses are created, i.e. they are still mutable
            m_definitive = false;
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
            return m_information.getUpdateStatus() == UpdateStatus.HasUpdate;
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
            m_information = info;
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

    @Override
    public TemplateOperationContext withContext() {
        return new TemplateOperationContext();
    }

    /**
     * Creates a context of properties around an operation performed by a {@link AbstractTemplateUpdater}
     * implementation. The {@link TemplateOperationContext} specifies the following properties:
     * <ul>
     * <li>a {@link WorkflowLoadHelper} representing a callback during template loads
     * <li>a {@link LoadResult} summarizing the results from loading templates
     * <li>a given cache for {@link TemplateUpdateCheckResult} to use and fill
     * <li>an {@link ExecutionMonitor} for tracking the operations and progress
     * </ul>
     *
     * Using the {@link #perform(Function)} method, a given template operation is performed within the built context.
     * After the operation, the context is removed again.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    public final class TemplateOperationContext {

        /**
         * Callback used during loading workflows, here templates.
         */
        private static final Supplier<WorkflowLoadHelper> DEFAULT_LOAD_HELPER = () -> null;

        private WorkflowLoadHelper m_loadHelper = DEFAULT_LOAD_HELPER.get();

        /**
         * Summary of the status from loading node container templates.
         */
        private static final Supplier<LoadResult> DEFAULT_LOAD_RESULT = () -> new LoadResult("ignored");

        private LoadResult m_loadResult = DEFAULT_LOAD_RESULT.get();

        /**
         * Cache of visited templates for update checks.
         */
        private static final Supplier<Map<URI, TemplateUpdateCheckResult>> DEFAULT_RESULT_CACHE = HashMap::new;

        private Map<URI, TemplateUpdateCheckResult> m_resultCache = DEFAULT_RESULT_CACHE.get();

        /**
         * Progress and execution tracker for the operation.
         */
        private static final Supplier<ExecutionMonitor> DEFAULT_MONITOR = ExecutionMonitor::new;

        private ExecutionMonitor m_monitor = DEFAULT_MONITOR.get();

        /**
         * Sets a given {@link WorkflowLoadHelper} in the context. Null-valued helper by default.
         *
         * @param helper
         * @return context
         */
        public TemplateOperationContext loadHelper(final WorkflowLoadHelper helper) {
            m_loadHelper = helper;
            return this;
        }

        /**
         * Sets a given {@link LoadResult} in the context. Ignored result by default.
         *
         *
         * @param result
         * @return context
         */
        public TemplateOperationContext loadResult(final LoadResult result) {
            m_loadResult = result;
            return this;
        }

        /**
         * Sets a given {@link Map} of {@link URI}s to {@link TemplateUpdateCheckResult} in the context. Used for more
         * efficient checking of updates. Empty map by default.
         *
         * @param resultCache
         * @return context
         */
        public TemplateOperationContext resultCache(final Map<URI, TemplateUpdateCheckResult> resultCache) {
            m_resultCache = resultCache;
            return this;
        }

        /**
         * Sets a given {@link ExecutionMonitor} to track progress and operations in the context. Plain
         * {@link ExecutionMonitor#ExecutionMonitor()} by default.
         *
         * @param monitor
         * @return context
         */
        public TemplateOperationContext executionMonitor(final ExecutionMonitor monitor) {
            m_monitor = monitor;
            return this;
        }

        /**
         * Performs a given template operation within the context of properties. After the operation completes, the
         * context is removed again.
         *
         * @param <T> generic type parameter for result type
         * @param templateOperation operation to be performed
         * @return result of type T
         */
        public <T> T perform(final Function<GeneralTemplateUpdater, T> templateOperation) {
            m_contextStack.push(this);
            try {
                return templateOperation.apply(AbstractTemplateUpdater.this);
            } finally {
                // equivalent to Stack#pop but does not throw an exception if empty
                m_contextStack.pollFirst();
            }
        }
    }

    protected synchronized WorkflowLoadHelper getLoadHelper() {
        if (m_contextStack.isEmpty()) {
            return TemplateOperationContext.DEFAULT_LOAD_HELPER.get();
        }
        return m_contextStack.peek().m_loadHelper;
    }

    protected synchronized LoadResult getLoadResult() {
        if (m_contextStack.isEmpty()) {
            return TemplateOperationContext.DEFAULT_LOAD_RESULT.get();
        }
        return m_contextStack.peek().m_loadResult;
    }

    protected synchronized Map<URI, TemplateUpdateCheckResult> getResultCache() {
        if (m_contextStack.isEmpty()) {
            return TemplateOperationContext.DEFAULT_RESULT_CACHE.get();
        }
        return m_contextStack.peek().m_resultCache;
    }

    protected synchronized ExecutionMonitor getExecutionMonitor() {
        if (m_contextStack.isEmpty()) {
            return TemplateOperationContext.DEFAULT_MONITOR.get();
        }
        return m_contextStack.peek().m_monitor;
    }
}
