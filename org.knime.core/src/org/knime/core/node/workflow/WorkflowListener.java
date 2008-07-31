/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * -------------------------------------------------------------------
 * 
 * History
 *   19.04.2005 (georg): created
 *   12.01.2006 (mb): clean up for code review
 */
package org.knime.core.node.workflow;

/**
 * Interface for listeners that receive workflow events.
 * 
 * @author Florian Georg, University of Konstanz
 */
public interface WorkflowListener { 
    /**
     * Called from the manager if something changed.
     * 
     * @param event the event that occured
     */
    public void workflowChanged(final WorkflowEvent event);

}
