/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * Created on 2013.10.21. by Gabor Bakos
 */
package org.knime.base.node.mine.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 * Helper class to create predictor output table specification with a common naming scheme.
 * <p>
 * The prediction column should have the following form: <tt>Prediction (<i>TrainingColumn</i>)</tt> (only default) and
 * <tt>P (<i>TrainingColumn</i>=<i>Value</i>)</tt> with possible suffix for the probability columns.
 *
 * @since 2.9
 * @author Gabor Bakos
 */
public class PredictorHelper {
    /**
     * The overrides the prediction column name checkbox text.
     */
    public static final String CHANGE_PREDICTION_COLUMN_NAME = "Change prediction column name";
    /** The dialog text for adding probabilities columns. */
    public static final String APPEND_COLUMNS_WITH_NORMALIZED_CLASS_DISTRIBUTION = "Append columns with normalized class distribution";
    /** Dialog text for the suffix of probability columns. */
    public static final String SUFFIX_FOR_PROBABILITY_COLUMNS = "Suffix for probability columns";

    private static final PredictorHelper INSTANCE = new PredictorHelper();

    /** Configuration key for the probability columns' suffices. */
    public static final String CFGKEY_SUFFIX = "class probability suffix";

    /** Default value for the probability columns' suffices. */
    public static final String DEFAULT_SUFFIX = "";

    /** Configuration key for the custom prediction column name. */
    public static final String CFGKEY_PREDICTION_COLUMN = "prediction column name";

    /** The default value for the prediction column name. */
    public static final String DEFAULT_PREDICTION_COLUMN = "Prediction ()";

    /** Configuration key for the change prediction column name. */
    public static final String CFGKEY_CHANGE_PREDICTION = "change prediction";

    /** The default value for the change prediction column name. */
    public static final Boolean DEFAULT_CHANGE_PREDICTION = false;

    /**
     * Hidden for the singleton instance.
     */
    private PredictorHelper() {
        super();
    }

    /**
     * @return the singleton instance
     */
    public static PredictorHelper getInstance() {
        return INSTANCE;
    }

    /**
     * @return The {@link SettingsModelString} for the probability columns' suffices.
     */
    public SettingsModelString createSuffix() {
        return new SettingsModelString(CFGKEY_SUFFIX, DEFAULT_SUFFIX);
    }

    /**
     * @return The {@link SettingsModelString} for the custom prediction column name.
     */
    public SettingsModelString createPredictionColumn() {
        SettingsModelString ret = new SettingsModelString(CFGKEY_PREDICTION_COLUMN, DEFAULT_PREDICTION_COLUMN);
        //ret.setEnabled(false);
        return ret;
    }

    /**
     * @return The {@link SettingsModelBoolean} to be able to override the predicted column name.
     */
    public SettingsModelBoolean createChangePrediction() {
        return new SettingsModelBoolean(CFGKEY_CHANGE_PREDICTION, DEFAULT_CHANGE_PREDICTION);
    }

    /**
     * Creates the {@link SettingsModelString} for the custom prediction column and adds the controls to {@code dialog}.
     *
     * @param dialog A {@link DefaultNodeSettingsPane}.
     * @return The created {@link DialogComponentString}.
     * @see #addPredictionColumn(DefaultNodeSettingsPane, SettingsModelString, SettingsModelBoolean)
     */
    public DialogComponentString addPredictionColumn(final DefaultNodeSettingsPane dialog) {
        return addPredictionColumn(dialog, createPredictionColumn(), createChangePrediction());
    }

    /**
     * Adds the prediction column name to the {@code dialog}. After that horizontal placement is set. <br>
     * Please make sure to initialize the {@code changePredictionColName} model properly or fire the model state change
     * listeners.
     *
     * @param dialog A {@link DefaultNodeSettingsPane}.
     * @param predictionColumn The {@link SettingsModelString} for the custom prediction column.
     * @param changePredictionColName The {@link SettingsModelBoolean} for overriding the prediction column.
     * @return The created {@link DialogComponentString}, although the dialog will also contain a checkbox to
     *         enable/disable the custom column name.
     */
    public DialogComponentString addPredictionColumn(final DefaultNodeSettingsPane dialog,
        final SettingsModelString predictionColumn, final SettingsModelBoolean changePredictionColName) {
        final DialogComponentString ret = new DialogComponentString(predictionColumn, "Prediction column: ", true, 40);
        //dialog.createNewGroup("Prediction Column");
        dialog.closeCurrentGroup();
        dialog.setHorizontalPlacement(true);
        DialogComponentBoolean changeDefault = new DialogComponentBoolean(changePredictionColName, CHANGE_PREDICTION_COLUMN_NAME);
        changeDefault.setToolTipText("Allows to override the default column name for the predictions.");
        changePredictionColName.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                ret.getModel().setEnabled(changePredictionColName.getBooleanValue());
            }});
        dialog.addDialogComponent(changeDefault);
        dialog.addDialogComponent(ret);
        //TODO add a dummy dialogcomponent to initialize the button.

        dialog.closeCurrentGroup();
        return ret;
    }

    /**
     * @param trainingColumnName Training column name.
     * @return Something like "<tt>Prediction (<i>trainingColumnName</i>)</tt>".
     */
    public String computePredictionDefault(final String trainingColumnName) {
        return "Prediction (" + trainingColumnName + ")";
    }

    /**
     * Computes the probability column name for the training column name and the value with suffix.
     *
     * @param trainingColumnName The training column name.
     * @param value The value used in training.
     * @param suffix The suffix for the column name.
     * @return Something like "<tt>P (<i>trainingColumnName</i>=<i>value</i>)<i>suffix</i></tt>".
     */
    public String probabilityColumnName(final String trainingColumnName, final String value, final String suffix) {
        return "P (" + trainingColumnName + "=" + value + ")" + suffix;
    }

    /**
     * Adds the suffix model to {@code dialog}.
     *
     * @param dialog A {@link DefaultNodeSettingsPane}.
     * @return The newly created {@link DialogComponentString} with a newly created model.
     * @see #addProbabilitySuffix(DefaultNodeSettingsPane, SettingsModelString)
     */
    public DialogComponentString addProbabilitySuffix(final DefaultNodeSettingsPane dialog) {
        return addProbabilitySuffix(dialog, createSuffix());
    }

    /**
     * Adds {@code suffixModel} to {@code dialog}.
     *
     * @param dialog A {@link DefaultNodeSettingsPane}.
     * @param suffixModel The {@link SettingsModelString} to use for the newly created {@link DialogComponentString} for
     *            the probability columns' suffices.
     * @return The newly created {@link DialogComponentString} for the probability columns' suffices.
     */
    public DialogComponentString
        addProbabilitySuffix(final DefaultNodeSettingsPane dialog, final SettingsModelString suffixModel) {
        DialogComponentString ret = new DialogComponentString(suffixModel, "Suffix for probability columns");
        ret.setToolTipText("This suffix will be appended after the \"P ([trainingColumn]=[value])\" columns.");
        dialog.addDialogComponent(ret);
        return ret;
    }

    /**
     * @param predictionColumn The overridden value for the prediction column name.
     * @param shouldOverride Use the overridden value ({@code true}), or the computed ({@code false})?
     * @param trainingColumnName The name of the column used to train the model.
     * @return The custom name if the model is enabled, else the pattern specified by
     *         {@link #computePredictionDefault(String)}.
     * @see #computePredictionDefault(String)
     */
    public String computePredictionColumnName(final String predictionColumn, final boolean shouldOverride,
        final String trainingColumnName) {
        return shouldOverride ? predictionColumn : computePredictionDefault(trainingColumnName);
    }

    /**
     * Checks whether the prediction column name would be empty or not.
     *
     * @param predictionColumn The overridden value for the prediction column name.
     * @param shouldOverride Use the overridden value ({@code true}), or the computed ({@code false})?
     * @param trainingColumnName The name of the column used to train the model.
     * @return The custom name if the model is enabled, else the pattern specified by
     *         {@link #computePredictionDefault(String)}.
     * @throws InvalidSettingsException Wrong prediction column name.
     * @see #computePredictionDefault(String)
     * @see #computePredictionColumnName(String, boolean, String)
     * @since 2.12
     */
    public String checkedComputePredictionColumnName(final String predictionColumn, final boolean shouldOverride,
        final String trainingColumnName) throws InvalidSettingsException {
        CheckUtils.checkSetting(!shouldOverride || (predictionColumn != null && !predictionColumn.trim().isEmpty()),
                "Prediction column name cannot be empty");
        return computePredictionColumnName(predictionColumn, shouldOverride, trainingColumnName);
    }

    private List<DataCell> getPredictionValues(final PMMLPortObjectSpec treeSpec) {
        String targetCol = treeSpec.getTargetFields().get(0);
        DataColumnSpec colSpec = treeSpec.getDataTableSpec().getColumnSpec(targetCol);
        if (colSpec.getDomain().hasValues()) {
            List<DataCell> predValues = new ArrayList<DataCell>();
            predValues.addAll(colSpec.getDomain().getValues());
            return predValues;
        } else {
            return null;
        }
    }

    /**
     * Computes the output table's specifaction based on common node settings.
     *
     * @param dataSpec The input table {@link DataColumnSpec}.
     * @param modelSpec The model {@link PMMLPortObjectSpec}.
     * @param addProbs Add the probability columns?
     * @param predictionCol Custom name of the prediction column.
     * @param shouldOverride Should we use that name?
     * @param suffix Suffix for probability columns.
     * @return The output table {@link DataTableSpec}.
     * @throws InvalidSettingsException Invalid settings for the prediction column name.
     */
    public DataTableSpec createOutTableSpec(final PortObjectSpec dataSpec, final PortObjectSpec modelSpec,
        final boolean addProbs, final String predictionCol, final boolean shouldOverride, final String suffix) throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(predictionCol, "Prediction column name cannot be null");
        CheckUtils.checkSetting(!predictionCol.trim().isEmpty(), "Prediction column name cannot be empty");
        List<DataCell> predValues = null;
        if (addProbs) {
            predValues = getPredictionValues((PMMLPortObjectSpec)modelSpec);
            if (predValues == null) {
                return null; // no out spec can be determined
            }
        }

        int numCols = (predValues == null ? 0 : predValues.size()) + 1;

        DataTableSpec inSpec = (DataTableSpec)dataSpec;
        DataColumnSpec[] newCols = new DataColumnSpec[numCols];

        /* Set bar renderer and domain [0,1] as default for the double cells
         * containing the distribution */
        //    DataColumnProperties propsRendering = new DataColumnProperties(
        //            Collections.singletonMap(
        //                    DataValueRenderer.PROPERTY_PREFERRED_RENDERER,
        //                    DoubleBarRenderer.DESCRIPTION));
        DataColumnDomain domain = new DataColumnDomainCreator(new DoubleCell(0.0), new DoubleCell(1.0)).createDomain();

        String trainingColumnName = ((PMMLPortObjectSpec)modelSpec).getTargetFields().iterator().next();
        // add all distribution columns
        for (int i = 0; i < numCols - 1; i++) {
            assert predValues != null;
            DataColumnSpecCreator colSpecCreator =
                new DataColumnSpecCreator(probabilityColumnName(trainingColumnName, predValues.get(i).toString(),
                    suffix), DoubleCell.TYPE);
            //            colSpecCreator.setProperties(propsRendering);
            colSpecCreator.setDomain(domain);
            newCols[i] = colSpecCreator.createSpec();
        }
        //add the prediction column
        String predictionColumnName = computePredictionColumnName(predictionCol, shouldOverride, trainingColumnName);
        newCols[numCols - 1] = new DataColumnSpecCreator(predictionColumnName, StringCell.TYPE).createSpec();
        DataTableSpec newColSpec = new DataTableSpec(newCols);
        return new DataTableSpec(inSpec, newColSpec);
    }
}
