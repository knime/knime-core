/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   Sep 24, 2007 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.util.Map;

import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface NodeContainerPersistor {

    NodeContainer getNodeContainer(final WorkflowManager parent, final NodeID id);

    NodeContainerMetaPersistor getMetaPersistor();

    boolean needsResetAfterLoad();
    
    boolean isDirtyAfterLoad();
    
    /** Does this persistor complain if its persisted state 
     * {@link NodeContainer#getState() state} does not match the state after
     * loading (typically all non-executed nodes are configured after load). 
     * This is true for all SingleNodeContainer and newer meta nodes,
     * but it will be false for meta nodes, which are loaded from 1.x workflow.
     * @return Such a property.
     */
    boolean mustComplainIfStateDoesNotMatch();
    
    LoadResult preLoadNodeContainer(final ReferencedFile nodeFileRef,
            final NodeSettingsRO parentSettings)
            throws InvalidSettingsException, IOException;

    LoadResult loadNodeContainer(final Map<Integer, BufferedDataTable> tblRep, final ExecutionMonitor exec)
            throws InvalidSettingsException, CanceledExecutionException,
            IOException;
}
