/*
 * ------------------------------------------------------------------ *
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Apr 16, 2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.util.concurrent.atomic.AtomicInteger;

/** Identifier (and other useful information) for queued or running jobs.
 * 
 * @author B. Wiswedel & M. Berthold, University of Konstanz
 */
public final class JobID {
    private static final AtomicInteger LASTUSEDID = new AtomicInteger();
    private final int m_id;
    
    private JobID() {
        m_id = LASTUSEDID.incrementAndGet();
    }
    
    public static final JobID createNewID() {
        return new JobID();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JobID)) {
            return false;
        }
        return m_id == ((JobID)o).m_id;
    }
    
    @Override
    public int hashCode() {
        return m_id;
    }
    
    @Override
    public String toString() {
        return "[ID:" + m_id + "]";
    }
}
