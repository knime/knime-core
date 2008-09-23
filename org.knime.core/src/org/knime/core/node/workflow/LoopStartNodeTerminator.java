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
 *   Sep 23, 2008 (mb): created
 */
package org.knime.core.node.workflow;

/** Interface for the start node of a loop which provides information
 * about the termination of the loop.
 * 
 * @author M. Berthold, University of Konstanz
 */
public interface LoopStartNodeTerminator extends LoopStartNode {

    /**
     * @return true if this was the last iteration of the loop, i.e. the
     *   tail node must not trigger re-execution of the loop. Some nodes
     *   may not be able to provide this and return false all the time.
     *   It will be the end node's responsibility to terminate the loop then.
     */
    boolean terminateLoop();

}
