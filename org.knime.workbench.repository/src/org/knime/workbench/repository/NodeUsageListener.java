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
 * ---------------------------------------------------------------------
 * 
 * History
 *   14.03.2008 (Fabian Dill): created
 */
package org.knime.workbench.repository;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public interface NodeUsageListener {
    
    /**
     * Informs the listener that the most frequent and last used nodes 
     * may have changed.
     */
    public void nodeAdded();
    
    /**
     * A more specific notification that the last used nodes history has 
     * changed.
     */
    public void usedHistoryChanged();

    /**
     * A more specific notification that the most frequent nodes history has 
     * changed.
     */
    public void frequentHistoryChanged();

}
