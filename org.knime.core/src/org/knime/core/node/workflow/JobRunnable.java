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
 *   Apr 13, 2007 (mb): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.ExecutionContext;

public abstract class JobRunnable implements Runnable {

    private JobID m_id;

    private final ExecutionContext m_execContext;

    public JobRunnable(final ExecutionContext ec) {
        m_execContext = ec;
    }

    public abstract void run(final ExecutionContext ec);

    @Override
    public final void run() {
        run(m_execContext);
    }

    public final void triggerCancel() {
        m_execContext.getProgressMonitor().setExecuteCanceled();
    }

    JobID getJobID() {
        return m_id;
    }

    void setJobID(final JobID id) {
        m_id = id;
    }

}
