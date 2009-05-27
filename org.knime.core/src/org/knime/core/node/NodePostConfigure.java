/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   08.01.2009 (ohl): created
 */
package org.knime.core.node;

import org.knime.core.node.port.PortObjectSpec;

/**
 * Object passed to {@link Node#configure(PortObjectSpec[], NodePostConfigure)}
 * in order to modify the output specs in case the node is wrapped and its
 * output is modified.
 *
 * @author ohl, University of Konstanz
 */
public interface NodePostConfigure {

    /**
     * Modifies the output table specs calculated by the
     * {@link NodeModel#configure(PortObjectSpec[])} method.
     *
     * @param inSpecs port object specs from predecessor node(s)
     * @param nodeModelOutSpecs the output specs created by the underlying node
     * @return the output specs actually delivered at the node's output ports
     * @throws InvalidSettingsException if the node can't be executed.
     */
    public PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
            PortObjectSpec[] nodeModelOutSpecs) throws InvalidSettingsException;

}
