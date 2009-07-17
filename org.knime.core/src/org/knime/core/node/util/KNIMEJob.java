/* ------------------------------------------------------------------
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
 *   19.02.2009 (meinl): created
 */
package org.knime.core.node.util;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObject;

/**
 * This interface describes a generic job that can be submitted via
 * {@link ExecutionContext#submitJob(PortObject[], NodeSettingsRO, Class,
 *  org.knime.core.node.ExecutionMonitor)}.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public interface KNIMEJob {
    /**
     * Runs the job somewhere.
     *
     * @param input the input port objects
     * @param settings the settings
     * @param exec the execution context
     * @return the result port objects
     * @throws Exception any exception
     */
    public PortObject[] run(PortObject[] input, NodeSettingsRO settings,
            ExecutionContext exec) throws Exception;
}
