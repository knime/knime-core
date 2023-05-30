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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;

/**
 * Implementation of the {@link TemplateUpdater}.
 * Uses update-status and download caches for efficient checking of the {@link MetaNodeTemplateInformation.UpdateStatus}.
 * See issues AP-16072, AP-16421 and AP-19382.
 *
 * TODO: Enforce the correct and consistent setting of the UpdateStatus via {@link MetaNodeTemplateInformation#setUpdateStatusInternal(UpdateStatus)}
 * and the notifying of listeners via {@link NodeContainerTemplate#notifyTemplateConnectionChangedListener()}.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
final class CachingTemplateUpdater extends AbstractTemplateUpdater {

    /**
     * Caches already downloaded NodeContainerTemplates to reuse for nodes of the same kind.
     *
     * Important: do not only compare on URI but also on the *local* timestamp before looking at this.
     * Otherwise, a check might determine no update to be available altough the first template of the kind was just newer,
     * registering its URI as null or something -- see issue AP-20451.
     */
    private final Map<URI, NodeContainerTemplate> m_downloadCache;

    /**
     * Default constructor, only to be called from {@link WorkflowManager}.
     */
    public CachingTemplateUpdater(final WorkflowManager wfm) {
        super(wfm);
        this.m_downloadCache = new HashMap<>();
    }

    @Override
    public Map<NodeID, UpdateStatus> checkUpdateForTemplates(final NodeID[] nodeIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IStatus updateTemplates(final NodeID[] nodeIds, final boolean recurseInto) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected IStatus updateAllTemplatesRecursively(final int depthLimit) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void clearCachesInternal() {
        this.m_downloadCache.clear();
    }
}
