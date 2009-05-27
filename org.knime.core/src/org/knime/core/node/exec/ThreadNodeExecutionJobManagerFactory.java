/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Oct 10, 2008 (wiswedel): created
 */
package org.knime.core.node.exec;

import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerFactory;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class ThreadNodeExecutionJobManagerFactory 
    implements NodeExecutionJobManagerFactory {
    
    public static final ThreadNodeExecutionJobManagerFactory INSTANCE =
        new ThreadNodeExecutionJobManagerFactory();

    /**
     * {@inheritDoc}
     */
    public String getID() {
        return getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabel() {
        return "Threaded Job Manager";
    }
    
    /** {@inheritDoc} */
    @Override
    public NodeExecutionJobManager getInstance() {
        return ThreadNodeExecutionJobManager.INSTANCE;
    }

}
