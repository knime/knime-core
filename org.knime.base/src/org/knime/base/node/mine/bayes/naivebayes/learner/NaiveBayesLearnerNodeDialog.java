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
package org.knime.base.node.mine.bayes.naivebayes.learner;

import org.knime.core.data.NominalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Naive Bayes Learner" node.
 *
 * @author Tobias Koetter
 */
public class NaiveBayesLearnerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring BayesianClassifier node dialog.
     */
    @SuppressWarnings("unchecked")
    public NaiveBayesLearnerNodeDialog() {
        super();
        final SettingsModelString columnName = new SettingsModelString(
                NaiveBayesLearnerNodeModel.CFG_CLASSIFYCOLUMN_KEY, null);
         final DialogComponentColumnNameSelection selectionBox =
             new DialogComponentColumnNameSelection(columnName,
                 "Classification Column:",
                 NaiveBayesLearnerNodeModel.TRAINING_DATA_PORT,
                 NominalValue.class);
         addDialogComponent(selectionBox);

         final SettingsModelBoolean skipMissingVals = new SettingsModelBoolean(
                 NaiveBayesLearnerNodeModel.CFG_SKIP_MISSING_VALUES, false);
         final DialogComponentBoolean skipMissingComp =
             new DialogComponentBoolean(skipMissingVals,
                     "Skip missing values (incl. class column)");
         skipMissingComp.setToolTipText("Skips all rows with a missing "
                 + "value during learning and prediction");
         addDialogComponent(skipMissingComp);

         final SettingsModelIntegerBounded noMo =
             new SettingsModelIntegerBounded(
                 NaiveBayesLearnerNodeModel.CFG_MAX_NO_OF_NOMINAL_VALS_KEY, 20,
                 0, Integer.MAX_VALUE);
         final DialogComponentNumber maxNomVals = new DialogComponentNumber(
                 noMo,
                 "Maximum number of unique nominal values per attribute: ",
                 1);
         maxNomVals.setToolTipText("Nominal attributes with more than "
                 + "the specified number of unique values are skipped");
         addDialogComponent(maxNomVals);
    }
}
