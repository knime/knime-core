/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.base.node.mine.bfn.BasisFunctionPredictorNodeModel;
import org.knime.base.node.mine.bfn.BasisFunctionPredictorRow;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;

/**
 * The fuzzy basis function predictor model performing a prediction on the data
 * from the first input and the suzzy basisfunction model from the second.
 * 
 * @see de.unikn.knime.dev.node.mine.bfn.BasisFunctionPredictorTable
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class FuzzyBasisFunctionPredictorNodeModel extends
        BasisFunctionPredictorNodeModel {

    /**
     * @see de.unikn.knime.dev.node.mine.bfn.BasisFunctionPredictorNodeModel
     *      #createPredictorRow(org.knime.core.node.ModelContentRO)
     */
    @Override
    protected BasisFunctionPredictorRow createPredictorRow(
            final ModelContentRO pp) throws InvalidSettingsException {
        return new FuzzyBasisFunctionPredictorRow(pp);
    }
}
