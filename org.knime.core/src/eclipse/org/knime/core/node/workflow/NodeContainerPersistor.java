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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.util.Map;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * TODO move the
 * @author Bernd Wiswedel, University of Konstanz
 * @noextend This interface is not intended to be extended by clients.
 */
public interface NodeContainerPersistor {

    /**
     * TODO move needs reset and is dirty to load result
     *
     * TODO move to WFM
     *
     * Instantiates the node container but does not call API methods on it. This is done later
     * when {@link #loadNodeContainer(Map, ExecutionMonitor, LoadResult)} is called.
     * @param parent The parent workflow of the node.
     * @param id The id of the node
     * @return A new NC instance.
     */
    NodeContainer getNodeContainer(final WorkflowManager parent, final NodeID id);

    NodeContainerMetaPersistor getMetaPersistor();

    boolean needsResetAfterLoad();

    boolean isDirtyAfterLoad();

    /** Does this persistor complain if its persisted state
     * {@link NodeContainer#getInternalState() state} does not match the state after
     * loading (typically all non-executed nodes are configured after load).
     * This is true for all SingleNodeContainer and newer metanodes,
     * but it will be false for metanodes, which are loaded from 1.x workflow.
     * @return Such a property.
     */
    boolean mustComplainIfStateDoesNotMatch();

    /** Loads internals, configuration, etc into the node instance. This method is called
     * when the corresponding node container has been instantiated.
     * @param tblRep Global table repository - for reference table lookup.
     * @param exec progress/cancel
     * @param loadResult where to add errors to.
     * @throws InvalidSettingsException  ...
     * @throws CanceledExecutionException  ...
     * @throws IOException ...
     */
    void loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec, final LoadResult loadResult)
            throws InvalidSettingsException, CanceledExecutionException,
            IOException;
}
