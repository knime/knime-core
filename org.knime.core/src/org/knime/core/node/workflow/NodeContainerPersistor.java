/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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

import java.io.File;
import java.io.IOException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public interface NodeContainerPersistor {

    NodeContainer getNodeContainer(final WorkflowManager parent, final NodeID id);

    NodeContainerMetaPersistor getMetaPersistor();

    void loadNodeContainer(final File workflowDir,
            final ExecutionMonitor exec, final int loadID)
            throws InvalidSettingsException, CanceledExecutionException,
            IOException;
}
