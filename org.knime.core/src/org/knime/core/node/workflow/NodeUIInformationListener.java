/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 *
 * History
 *   20.09.2007 (Fabian Dill): created
 */
package org.knime.core.node.workflow;

import java.util.EventListener;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public interface NodeUIInformationListener extends EventListener {

    /**
     * Invoked when the ui information has changed.
     *
     * @param evt the event
     */
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt);

}
