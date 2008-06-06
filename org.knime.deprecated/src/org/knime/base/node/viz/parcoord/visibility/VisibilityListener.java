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
 *   Jun 27, 2005 (tg): created
 */
package org.knime.base.node.viz.parcoord.visibility;

import java.util.EventListener;

/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public interface VisibilityListener extends EventListener {
    /** 
     * Invoked when some item(s) were selected. 
     * @param event Contains a list of row keys that were selected.
     */
    void select(final VisibilityEvent event);

    /** 
     * Invoked when some item(s) were unselected. 
     * @param event Contains a list of row keys that were unselected.
     */
    void unselect(final VisibilityEvent event);
    
    /** 
     * Invoked when some item(s) were made visible. 
     * @param event Contains a list of row keys that were made visible.
     */
    void makeVisible(final VisibilityEvent event);

    /** 
     * Invoked when some item(s) were made invisible. 
     * @param event Contains a list of row keys that were made invisible.
     */
    void makeInvisible(final VisibilityEvent event);
    

}
