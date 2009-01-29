/*  
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Aug 12, 2005 (tg): created
 */
package org.knime.base.node.mine.bfn;

import org.knime.core.data.DataRow;

/**
 * 
 * @author Simona Pintilie, University of Konstanz
 */
public class BasisFunctionAntisymmetricRowOverlap implements DegreeOfAffinity {
    /**
     * {@inheritDoc}
     */
    public double getAffinityDegree(final DataRow row1, final DataRow row2) {
        BasisFunctionPredictorRow bf1 = (BasisFunctionPredictorRow)row1;
        BasisFunctionPredictorRow bf2 = (BasisFunctionPredictorRow)row2;
        return bf1.overlap(bf2, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Anti-symmetric Overlap";
    }
}
