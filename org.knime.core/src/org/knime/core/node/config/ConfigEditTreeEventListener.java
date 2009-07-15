/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 *   14.07.2009 (wiswedel): created
 */
package org.knime.core.node.config;

import java.util.EventListener;

/**
 * Listener interface for {@link ConfigEditTreeEvent}.
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface ConfigEditTreeEventListener extends EventListener {

    /** Called when a node in a {@link ConfigEditTreeModel} has changed
     * its state.
     * @param event The event fired.
     */
    public void configEditTreeChanged(final ConfigEditTreeEvent event);
}
