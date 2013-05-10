/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * Created on 07.05.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.interactive;

import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Interface for all interactive views.
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @author B. Wiswedel, M. Berthold, Th. Gabriel, C. Albrecht
 * @param <T> underlying NodeModel
 * @param <V>
 * @since 2.8
 */
public interface InteractiveView<T extends NodeModel & InteractiveNode<V>, V extends ViewContent> {

    /**
     * Set access to workflowmanager and node so the view can trigger re-execution. Note
     * part of the constructor, so derived classes don't have access to this information.
     *
     * @param wfm the parent WorkflowManager
     * @param id of the node
     */
    public void setWorkflowManagerAndNodeID(final WorkflowManager wfm, final NodeID id);

    /**
     * @return true if node can be re-executed.
     */
    public boolean canReExecute();

    /**
     * Re-Execute underlying node. Also trigger:<br>
     * - reset of node and successors (ask user first!)<br>
     * - configure node and successors<br>
     * - execute node but not successors (can be canceled by user of fail during execution!)
     *
     * @param vc
     * @param callback Callback for confirm messages and progress information.
     */
    public void triggerReExecution(final V vc, final ReexecutionCallback callback);

    /**
     * Make sure current node internals are used as new default NodeSettings.<br>
     * Results in:<br>
     * - reset of node and successors (ask user first!)<br>
     * - NodeModel.saveSettingsTo()<br>
     * - configure node and successors
     *
     * @param callback Callback for confirm messages.
     */
    public void setNewDefaultConfiguration(final ConfigureCallback callback);

}
