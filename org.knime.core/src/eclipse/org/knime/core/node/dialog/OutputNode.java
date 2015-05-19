/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on Feb 15, 2015 by wiswedel
 */
package org.knime.core.node.dialog;


/**
 * Interface for nodes that produce results that can be queried externally, e.g. web services.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.12
 * @noimplement This interface is not intended to be implemented by clients, it may change without notice
 */
public interface OutputNode {
    /**
     * Returns an object representing the node's external output.
     * @return an external output, never <code>null</code>
     */
    public ExternalNodeData getExternalOutput();
}
