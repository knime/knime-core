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
 * ---------------------------------------------------------------------
 * 
 * History
 *   25.10.2006 (sieb): created
 */
package org.knime.base.node.mine.decisiontree2.learner;

import org.knime.core.data.NominalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Dialog for a decision tree learner node.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class DecisionTreeLearnerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Constructor: create NodeDialog with one column selectors and two other
     * properties.
     */
    public DecisionTreeLearnerNodeDialog() {

        super();

        // class column selection
        Class[] filter = {NominalValue.class};
        this.addDialogComponent(new DialogComponentColumnNameSelection(
                new SettingsModelString(
                        DecisionTreeLearnerNodeModel.KEY_CLASSIFYCOLUMN, null),
                "Class column", DecisionTreeLearnerNodeModel.DATA_INPORT,
                filter));

        // quality measure
        String[] qualityMethods =
                {DecisionTreeLearnerNodeModel.SPLIT_QUALITY_GAIN_RATIO,
                        DecisionTreeLearnerNodeModel.SPLIT_QUALITY_GINI};
        this.addDialogComponent(new DialogComponentStringSelection(
                new SettingsModelString(
                    DecisionTreeLearnerNodeModel.KEY_SPLIT_QUALITY_MEASURE,
                    DecisionTreeLearnerNodeModel.DEFAULT_SPLIT_QUALITY_MEASURE),
                        "Quality measure", qualityMethods));

        // pruning method
        String[] methods =
                {DecisionTreeLearnerNodeModel.PRUNING_NO,
                 DecisionTreeLearnerNodeModel.PRUNING_MDL};
                 // DecisionTreeLearnerNodeModel.PRUNING_ESTIMATED_ERROR};
        this.addDialogComponent(new DialogComponentStringSelection(
                new SettingsModelString(
                        DecisionTreeLearnerNodeModel.KEY_PRUNING_METHOD,
                        DecisionTreeLearnerNodeModel.DEFAULT_PRUNING_METHOD),
                "Pruning method", methods));

        // confidence value threshold for c4.5 pruning
//        this.addDialogComponent(new DialogComponentNumber(
//              new SettingsModelDoubleBounded(
//              DecisionTreeLearnerNodeModel.KEY_PRUNING_CONFIDENCE_THRESHOLD,
//              DecisionTreeLearnerNodeModel.DEFAULT_PRUNING_CONFIDENCE_THRESHOLD,
//              0.0, 0.5),
//              "Confidence threshold (estimated error)", 0.01, 7));

        // min number records for a node
        // also used for determine whether a partition is useful
        // both are closely related
        this.addDialogComponent(new DialogComponentNumber(
            new SettingsModelIntegerBounded(
            DecisionTreeLearnerNodeModel.KEY_MIN_NUMBER_RECORDS_PER_NODE,
            (int)DecisionTreeLearnerNodeModel.DEFAULT_MIN_NUM_RECORDS_PER_NODE,
            1, Integer.MAX_VALUE),
            "Min number records per node", 1));

        // number records to store for the view
        this.addDialogComponent(new DialogComponentNumber(
            new SettingsModelIntegerBounded(
                   DecisionTreeLearnerNodeModel.KEY_NUMBER_VIEW_RECORDS,
                   DecisionTreeLearnerNodeModel.DEFAULT_NUMBER_RECORDS_FOR_VIEW,
                   0, Integer.MAX_VALUE),
                   "Number records to store for view", 100));

        // split point set to average value or to upper value of lower partition
        this.addDialogComponent(new DialogComponentBoolean(
                new SettingsModelBoolean(
                        DecisionTreeLearnerNodeModel.KEY_SPLIT_AVERAGE,
                        DecisionTreeLearnerNodeModel.DEFAULT_SPLIT_AVERAGE),
                "Average split point"));

        // binary nominal split mode
        this.addDialogComponent(new DialogComponentBoolean(
             new SettingsModelBoolean(
                DecisionTreeLearnerNodeModel.KEY_BINARY_NOMINAL_SPLIT_MODE,
                DecisionTreeLearnerNodeModel.DEFAULT_BINARY_NOMINAL_SPLIT_MODE),
                        "Binary nominal splits"));

        // max number nominal values for complete subset calculation for binary
        // nominal splits
        this.addDialogComponent(new DialogComponentNumber(
         new SettingsModelIntegerBounded(
         DecisionTreeLearnerNodeModel.KEY_MAX_NUM_NOMINAL_VALUES,
         DecisionTreeLearnerNodeModel.DEFAULT_MAX_BIN_NOMINAL_SPLIT_COMPUTATION,
                                1, 63), "Max #nominal", 1, 5));

        // number processors to use
        this.addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                        DecisionTreeLearnerNodeModel.KEY_NUM_PROCESSORS,
                        DecisionTreeLearnerNodeModel.DEFAULT_NUM_PROCESSORS, 1,
                        Integer.MAX_VALUE), "Number threads", 1, 5));
    }
}
