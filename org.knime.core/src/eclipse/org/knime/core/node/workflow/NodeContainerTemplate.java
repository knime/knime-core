/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   May 23, 2014 ("Patrick Winter"): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.WorkflowPersistor.NodeContainerTemplateLinkUpdateResult;
import org.knime.core.util.LockFailedException;


/**
 *
 * @author "Patrick Winter"
 * @since 2.10
 */
public interface NodeContainerTemplate {

    /**
     * @param directory The directory to save in
     * @param exec The execution monitor
     * @return Information about the metanode template
     * @throws IOException If an IO error occurs
     * @throws CanceledExecutionException If execution is canceled during the operation
     * @throws LockFailedException If locking failed
     * @throws InvalidSettingsException if defaults can't be set (meta node settings to be reverted in template)
     */
    public MetaNodeTemplateInformation saveAsTemplate(final File directory, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException, LockFailedException, InvalidSettingsException;

    /** Set new template info (not null!), fire events.
     * @param tI the new templateInformation */
    public void setTemplateInformation(MetaNodeTemplateInformation tI);

    public MetaNodeTemplateInformation getTemplateInformation();

    public WorkflowCipher getWorkflowCipher();

    public NodeID getID();

    public void updateMetaNodeLinkInternalRecursively(final ExecutionMonitor exec,
        final WorkflowLoadHelper loadHelper, final Map<URI, NodeContainerTemplate> visitedTemplateMap,
        final NodeContainerTemplateLinkUpdateResult loadRes) throws Exception;

    public WorkflowManager getParent();

    public String getNameWithID();

    public void notifyTemplateConnectionChangedListener();

    public abstract Map<NodeID, NodeContainerTemplate> fillLinkedTemplateNodesList(final Map<NodeID, NodeContainerTemplate> mapToFill, final boolean recurse, final boolean stopRecursionAtLinkedMetaNodes);

    /** @return collection of NodeContainers in this node. */
    public Collection<NodeContainer> getNodeContainers();

    /** @return true if this workflow or its child workflows contain at least
     * one executed node. */
    public boolean containsExecutedNode();


}
