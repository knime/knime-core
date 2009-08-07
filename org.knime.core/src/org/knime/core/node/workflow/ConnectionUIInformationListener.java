/*
 * ------------------------------------------------------------------ *
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
 * -------------------------------------------------------------------
 *
 * History
 *   20.09.2007 (Fabian Dill): created
 */
package org.knime.core.node.workflow;

import java.util.EventListener;

/**
 * Listener for ui information changes on {@link ConnectionContainer}.
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface ConnectionUIInformationListener extends EventListener {

    /**
     * Invoked when the ui information has changed.
     *
     * @param evt the event
     */
    public void connectionUIInformationChanged(
            final ConnectionUIInformationEvent evt);
}
