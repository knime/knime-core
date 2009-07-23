/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * KNIME.com, Zurich, Switzerland
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
 *   Apr 12, 2007 (mb): created
 */
package org.knime.core.node.workflow;


/**
 * Main entry point for compute intensive jobs. Controls resource (thread)
 * allocation...
 *
 * @author M. Berthold & B. Wiswedel, University of Konstanz
 */
public interface NodeExecutionJobManagerFactory {

    /**
     * Returns a unique ID of this job manager implementations. Preferably this
     * is the fully qualifying name of its package. <br />
     * For a user readable label, see {@link #toString()}
     *
     * @return a unique ID of this job manager implementations
     */
    String getID();

    /**
     * Returns a user readable - but still most likely unique - label. This is
     * displayed in dialogs and user messages.
     *
     * @return a user readable label for this job manager
     */
    public String getLabel();

    /** Get an instance for use in a node. It is to the discretion of
     * implementing classes to return a new instance or a singleton here,
     * whereby the latter should be returned if the job manager can not be
     * parameterized.
     * @return a new instance of a job manager or a singleton.
     */
    NodeExecutionJobManager getInstance();

}
