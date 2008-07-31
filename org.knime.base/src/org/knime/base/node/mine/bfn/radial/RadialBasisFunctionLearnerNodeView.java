/* 
 * ------------------------------------------------------------------
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
 * 
 * History
 *   12.09.2007 (gabriel): created
 */
package org.knime.base.node.mine.bfn.radial;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeView;

/**
 * 
 * @author gabriel, University of Konstanz
 */
public class RadialBasisFunctionLearnerNodeView extends 
        BasisFunctionLearnerNodeView<RadialBasisFunctionLearnerNodeModel> {

    /**
     * Create radial learner node view.
     * @param model fuzzy learner node model
     */
    public RadialBasisFunctionLearnerNodeView(
            final RadialBasisFunctionLearnerNodeModel model) {
        super(model);
    }
}
