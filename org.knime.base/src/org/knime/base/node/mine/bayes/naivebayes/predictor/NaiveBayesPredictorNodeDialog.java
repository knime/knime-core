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
 * -------------------------------------------------------------------
 *
 * History
 *   02.05.2006 (koetter): created
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * <code>NodeDialog</code> for the "Naive Bayes Predictor" Node.
 *
 * @author Tobias Koetter
 */
public class NaiveBayesPredictorNodeDialog extends DefaultNodeSettingsPane {

    private static final String INCL_PROB_VALS_LABEL =
        "Append probability value column per class instance";

    /**
     * New pane for configuring BayesianClassifier node dialog.
     */
    @SuppressWarnings("unchecked")
    public NaiveBayesPredictorNodeDialog() {
        super();
        final SettingsModelBoolean inclProbValsModel = new SettingsModelBoolean(
                NaiveBayesPredictorNodeModel.CFG_INCL_PROBABILITYVALS_KEY,
                false);
        final DialogComponentBoolean inclprobValsCompnent =
            new DialogComponentBoolean(inclProbValsModel, INCL_PROB_VALS_LABEL);
         addDialogComponent(inclprobValsCompnent);
    }
}
