/* This source code, its documentation and all appendant files
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
 */
package org.knime.core.node.workflow;

/**
 * 
 * @author Fabian Dill, KNIME.com GmbH
 */
public interface JobManagerChangedListener {
    
    /**
     * Gets informed whenever the {@link NodeExecutionJobManager} has
     * changed. The {@link JobManagerChangedEvent} contains the {@link NodeID}
     * of the source, which has to be queried in order to get the new 
     * {@link NodeExecutionJobManager}. 
     * 
     * @param e event containing the {@link NodeID} of the source node, whose 
     * {@link NodeExecutionJobManager} has changed
     */
    public void jobManagerChanged(JobManagerChangedEvent e);

}
