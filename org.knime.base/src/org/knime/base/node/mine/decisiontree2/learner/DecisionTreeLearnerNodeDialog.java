/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
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

        // class column selection
        this.addDialogComponent(new DialogComponentColumnNameSelection(
                createSettingsClassColumn(),
                "Class column", DecisionTreeLearnerNodeModel.DATA_INPORT,
                NominalValue.class));

        // quality measure
        String[] qualityMethods =
                {DecisionTreeLearnerNodeModel.SPLIT_QUALITY_GAIN_RATIO,
                        DecisionTreeLearnerNodeModel.SPLIT_QUALITY_GINI};
        this.addDialogComponent(new DialogComponentStringSelection(
                createSettingsQualityMeasure(),
                        "Quality measure", qualityMethods));

        // pruning method
        String[] methods =
                {DecisionTreeLearnerNodeModel.PRUNING_NO,
                 DecisionTreeLearnerNodeModel.PRUNING_MDL};
                 // DecisionTreeLearnerNodeModel.PRUNING_ESTIMATED_ERROR};
        this.addDialogComponent(new DialogComponentStringSelection(
                createSettingsPruningMethod(),
                "Pruning method", methods));

        // confidence value threshold for c4.5 pruning
//        this.addDialogComponent(new DialogComponentNumber(
//              createSettingsConfidenceValue(),
//              "Confidence threshold (estimated error)", 0.01, 7));

        // min number records for a node
        // also used for determine whether a partition is useful
        // both are closely related
        this.addDialogComponent(new DialogComponentNumber(
            createSettingsMinNumRecords(), "Min number records per node", 1));

        // number records to store for the view
        this.addDialogComponent(new DialogComponentNumber(
            createSettingsNumberRecordsForView(),
                   "Number records to store for view", 100));

        // split point set to average value or to upper value of lower partition
        this.addDialogComponent(new DialogComponentBoolean(
                createSettingsSplitPoint(), "Average split point"));

        // binary nominal split mode
        this.addDialogComponent(new DialogComponentBoolean(
             createSettingsBinaryNominalSplit(), "Binary nominal splits"));

        // max number nominal values for complete subset calculation for binary
        // nominal splits
        this.addDialogComponent(new DialogComponentNumber(
                createSettingsMaxNominalValues(), "Max #nominal", 1, 5));

        // number processors to use
        this.addDialogComponent(new DialogComponentNumber(
                createSettingsNumProcessors(), "Number threads", 1, 5));
    }
    
    /**
     * @return class column selection
     */
    static SettingsModelString createSettingsClassColumn() {
        return new SettingsModelString(
                    DecisionTreeLearnerNodeModel.KEY_CLASSIFYCOLUMN, null);
    }
    
    /**
     * @return quality measure
     */
    static SettingsModelString createSettingsQualityMeasure() {
        return new SettingsModelString(
                DecisionTreeLearnerNodeModel.KEY_SPLIT_QUALITY_MEASURE,
                DecisionTreeLearnerNodeModel.DEFAULT_SPLIT_QUALITY_MEASURE);
    }

    /**
     * @return pruning method
     */
    static SettingsModelString createSettingsPruningMethod() {
        return new SettingsModelString(
                    DecisionTreeLearnerNodeModel.KEY_PRUNING_METHOD,
                    DecisionTreeLearnerNodeModel.DEFAULT_PRUNING_METHOD);
    }
    
    /**
     * @return confidence value threshold for c4.5 pruning
     */
    static SettingsModelDoubleBounded createSettingsConfidenceValue() {
        return new SettingsModelDoubleBounded(
          DecisionTreeLearnerNodeModel.KEY_PRUNING_CONFIDENCE_THRESHOLD,
          DecisionTreeLearnerNodeModel.DEFAULT_PRUNING_CONFIDENCE_THRESHOLD,
          0.0, 1.0);
    }

    /**
     * @return minimum number of objects per node
     */
    static SettingsModelIntegerBounded createSettingsMinNumRecords() {
        // min number records for a node also used for determine whether a 
        // partition is useful both are closely related
        return new SettingsModelIntegerBounded(
                DecisionTreeLearnerNodeModel.KEY_MIN_NUMBER_RECORDS_PER_NODE,
                DecisionTreeLearnerNodeModel.DEFAULT_MIN_NUM_RECORDS_PER_NODE,
                1, Integer.MAX_VALUE);
    }
    
    /**
     * @return number records to store for the view
     */
    static SettingsModelIntegerBounded createSettingsNumberRecordsForView() {
        return new SettingsModelIntegerBounded(
               DecisionTreeLearnerNodeModel.KEY_NUMBER_VIEW_RECORDS,
               DecisionTreeLearnerNodeModel.DEFAULT_NUMBER_RECORDS_FOR_VIEW,
               0, Integer.MAX_VALUE);
    }

    /**
     * @return split point set to average value or to upper value of lower 
     *         partition
     */
    static SettingsModelBoolean createSettingsSplitPoint() {
        return new SettingsModelBoolean(
                    DecisionTreeLearnerNodeModel.KEY_SPLIT_AVERAGE,
                    DecisionTreeLearnerNodeModel.DEFAULT_SPLIT_AVERAGE);
    }
    
    /**
     * @return binary nominal split mode
     */
    static SettingsModelBoolean createSettingsBinaryNominalSplit() {
        return new SettingsModelBoolean(
            DecisionTreeLearnerNodeModel.KEY_BINARY_NOMINAL_SPLIT_MODE,
            DecisionTreeLearnerNodeModel.DEFAULT_BINARY_NOMINAL_SPLIT_MODE);
    }

    /**
     * @return max number nominal values for complete subset calculation for 
     *         binary nominal splits
     */
    static SettingsModelIntegerBounded createSettingsMaxNominalValues() {
        return new SettingsModelIntegerBounded(
                DecisionTreeLearnerNodeModel.KEY_MAX_NUM_NOMINAL_VALUES,
                DecisionTreeLearnerNodeModel
                    .DEFAULT_MAX_BIN_NOMINAL_SPLIT_COMPUTATION,
                1, Integer.MAX_VALUE);
    }
    
    /**
     * @return number processors to use
     */
    static SettingsModelIntegerBounded createSettingsNumProcessors() {
        return new SettingsModelIntegerBounded(
                    DecisionTreeLearnerNodeModel.KEY_NUM_PROCESSORS,
                    DecisionTreeLearnerNodeModel.DEFAULT_NUM_PROCESSORS, 1,
                    Integer.MAX_VALUE);
    }
}
