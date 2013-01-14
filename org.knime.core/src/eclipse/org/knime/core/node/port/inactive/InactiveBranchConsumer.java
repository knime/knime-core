/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
 *   30.09.2010 (berthold_2): created
 */
package org.knime.core.node.port.inactive;


/** Marker interface implemented by nodes
 * ({@link org.knime.core.node.NodeModel}) that are able to consume
 * inactive branches. The configure and execute methods of such implementations
 * must accept classes of {@link InactiveBranchPortObject} and
 * {@link InactiveBranchPortObjectSpec} (which kind of violates the assertions
 * made by the method API).
 *
 * <p>This interface is implemented by nodes such as the End IF and End Case
 * node.
 *
 * @author B. Wiswedel, University of Konstanz
 */
public interface InactiveBranchConsumer {

}
