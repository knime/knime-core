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
 * ---------------------------------------------------------------------
 * 
 * History
 *   01.08.2007 (thor): created
 */
package org.knime.core.util;

import java.util.Timer;

/**
 * This final singleton class is a global timer available for all classes inside
 * KNIME. This timer is especially useful for nodes that execute external code
 * which is not aware of execution canceling and such stuff.
 * 
 * <b>Users of this timer must make sure, that the scheduled tasks are
 * fast-running, otherwise other tasks will be blocked.</b>
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class KNIMETimer extends Timer {
    private static final KNIMETimer INSTANCE = new KNIMETimer();

    private KNIMETimer() {
        super("Global KNIME Timer");
    }

    /**
     * Do not call this method, it won't let you cancel this timer, but
     * instead throw an {@link UnsupportedOperationException} at you.
     */
    @Override
    public void cancel() {
        throw new UnsupportedOperationException(
                "You must not cancel the global timer!");
    }

    /**
     * Returns the singleton instance of the global KNIME timer.
     * 
     * @return a timer
     */
    public static KNIMETimer getInstance() {
        return INSTANCE;
    }
}
