/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.radial;

import org.knime.base.node.mine.bfn.BasisFunctionPredictorNodeModel;
import org.knime.base.node.mine.bfn.BasisFunctionPredictorRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;

/**
 * The radial basisfunction predictor model performing a prediction on the data
 * from the first input and the radial basisfunction model from the second.
 * 
 * @see org.knime.base.node.mine.bfn.BasisFunctionPredictorCellFactory
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RadialBasisFunctionPredictorNodeModel extends
        BasisFunctionPredictorNodeModel {

    /**
     * {@inheritDoc}
     */
    @Override
    public BasisFunctionPredictorRow createPredictorRow(
            final ModelContentRO pp) throws InvalidSettingsException {
        return new RadialBasisFunctionPredictorRow(pp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean normalizeClassification() {
         return true;
    }
}
