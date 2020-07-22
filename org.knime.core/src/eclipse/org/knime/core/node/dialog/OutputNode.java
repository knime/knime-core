/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
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
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.12
 * @noimplement This interface is not intended to be implemented by clients, it may change without notice
 */
public interface OutputNode {
    /**
     * Returns an object representing the node's external output. If the node is not yet executed, the returned object
     * may be empty and not contain any values.
     *
     * @return an external output, never <code>null</code>
     */
    public ExternalNodeData getExternalOutput();

    /**
     * Allows nodes to veto the use of fully qualified parameter names. That is, if returned <code>false</code> then the
     * node suggests to use the short name ("output-table") over its long name ("output-table-14") in an API description
     * (e.g. Swagger). However, even if <code>false</code> is returned the framework will still use the long variant
     * when conflicting parameter names are used. <br />
     * This came into existence as part of AP-14686. This default implementation returns <code>true</code> in order to
     * guarantee backward compatibility.
     *
     * @return <code>true</code> here but potentially overwritten by nodes (especially Container nodes)
     * @since 4.3
     */
    default boolean isUseAlwaysFullyQualifiedParameterName() {
        return true;
    }
}
