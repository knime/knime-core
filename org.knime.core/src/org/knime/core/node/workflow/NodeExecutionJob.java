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

import java.util.Arrays;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;

public abstract class NodeExecutionJob implements Runnable {

    private final SingleNodeContainer m_snc;
    private final PortObject[] m_data;
    private final ExecutionContext m_execContext;

    public NodeExecutionJob(final SingleNodeContainer snc,
            final PortObject[] data, final ExecutionContext ec) {
        if (snc == null || data == null || ec == null) {
            throw new NullPointerException("Args must not be null.");
        }
        if (Arrays.asList(data).contains(null)) {
            throw new NullPointerException("Array arg must not contain null.");
        }
        m_snc = snc;
        m_data = data;
        m_execContext = ec;
    }

    @Override
    public void run() {
        boolean success = true;
        if (!isReConnecting()) {
            try {
                m_snc.performBeforeExecuteNode();
            } catch (IllegalContextStackObjectException e) {
                success = false;
            }
        }
        success = success && mainExecute();
        m_snc.performAfterExecuteNode(success);
    }

    public abstract boolean isReConnecting();
    public abstract boolean mainExecute();
    public abstract boolean cancel();

    public PortObject[] getPortObjects() {
        return m_data;
    }

    public SingleNodeContainer getSingleNodeContainer() {
        return m_snc;
    }

    public ExecutionContext getExecutionContext() {
        return m_execContext;
    }
}
