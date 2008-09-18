/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Sep 18, 2008 (mb): created
 */
package org.knime.core.node.workflow;

/** Interface of the node starting a loop with a simply end condition
 * which the start node already provdes. This guarantees that a
 * int-variable called "terminateLoop" is available on the stack for
 * the subsequent end node, indicating if the loop continues (=0)
 * or is supposed to finish (=1).
 * 
 * @author M. Berthold, University of Konstanz
 */
public interface LoopStartNodeWhileDo extends LoopStartNode {

}
