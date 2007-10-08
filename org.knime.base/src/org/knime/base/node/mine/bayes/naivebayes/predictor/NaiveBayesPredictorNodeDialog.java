/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2007
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
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
 * <code>NodeDialog</code> for the "BayesianClassifier" Node.
 * This is the description of the Bayesian classifier
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
