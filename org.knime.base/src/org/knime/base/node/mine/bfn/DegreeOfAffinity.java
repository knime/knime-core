/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Jun 22, 2005 (tg): created
 */
package org.knime.base.node.mine.bfn;

import org.knime.core.data.DataRow;

/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public interface DegreeOfAffinity {
    
    /**
     * @return the affinity degree between the two rules
     * @param dr1 the first rule
     * @param dr2 the second rule
     */
    public double getAffinityDegree(final DataRow dr1, final DataRow dr2); 
    
}
