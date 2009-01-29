/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules;

import org.knime.core.data.DataRow;

/**
 * Very simple interface for rule node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public interface RuleNode {
    /**
     * Evaluates this rule node.
     *
     * @param row the row with which the rule should be evaluated
     * @return <code>true</code> if this node evaluates to <code>true</code>,
     *         <code>false</code> otherwise
     */
    public boolean evaluate(DataRow row);
}
