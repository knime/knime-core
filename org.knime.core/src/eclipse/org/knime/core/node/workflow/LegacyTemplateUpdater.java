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
 *   Dec 8, 2023 (Leon Wenzler, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;
import org.knime.core.node.workflow.TemplateUpdateUtil.TemplateUpdateCheckResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;

/**
 * A {@link TemplateUpdater} which has the old checking and updating functionality.
 * Moved that functionality from {@link WorkflowManager} to here.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
final class LegacyTemplateUpdater extends AbstractTemplateUpdater {

    /**
     * Default constructor.
     *
     * @param wfm
     */
    LegacyTemplateUpdater(final WorkflowManager wfm) {
        super(wfm);
    }

    @Override
    public Map<NodeID, UpdateStatus> checkUpdateForTemplates(final NodeID[] nodeIds, final boolean recurseInto) {
        final Map<NodeID, UpdateStatus> states = new LinkedHashMap<>();
        final var errorMessageBuilder = new StringBuilder();
        final var resultCache = getResultCache();
        for (var id : nodeIds) {
            final var loadHelper = getLoadHelper();
            final var loadResult = getLoadResult();
            states.putAll(checkUpdateForTemplateInternal(id, loadHelper, loadResult, resultCache, recurseInto));
            if (loadResult.hasErrors()) {
                errorMessageBuilder.append("\n" + loadResult.getFilteredError("  ", LoadResultEntryType.Error));
            }
        }
        if (!errorMessageBuilder.isEmpty()) {
            LOGGER.error(() -> "Errors while checking for template updates:" + errorMessageBuilder.toString());
        }
        storeAndNotifyUpdateCheckResults(states);
        return states;
    }

    private Map<NodeID, UpdateStatus> checkUpdateForTemplateInternal(final NodeID id,
        final WorkflowLoadHelper loadHelper, final LoadResult loadResult,
        final Map<URI, TemplateUpdateCheckResult> resultCache, final boolean recurseInto) {
        final var nodeContainer = m_wfm.getWorkflow().getNode(id);
        if (!(nodeContainer instanceof NodeContainerTemplate)) {
            return Collections.emptyMap();
        }
        NodeContainerTemplate tnc = (NodeContainerTemplate)nodeContainer;
        Map<NodeID, NodeContainerTemplate> nodesToCheck = new LinkedHashMap<>();

        if (tnc.getTemplateInformation().getRole() == Role.Link) {
            nodesToCheck.put(id, tnc);
        }
        if (recurseInto) {
            nodesToCheck = tnc.fillLinkedTemplateNodesList(nodesToCheck, true, false);
        }
        Map<NodeID, UpdateStatus> updateStates;
        try {
            updateStates = TemplateUpdateUtil.fillNodeUpdateStates(//
                nodesToCheck.values(), loadHelper, loadResult, resultCache);
        } catch (IOException e) { // NOSONAR
            // at this point, the error should be written in the loadResult
            return Collections.emptyMap();
        }
        return updateStates;
    }

    @Override
    public IStatus updateTemplates(final NodeID[] nodeIds, final boolean recurseInto) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    protected IStatus updateAllTemplatesRecursively(final int depthLimit) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    protected void resetInternal() {
        // no-op, all caches are method-local here
    }
}
