/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation.numerical;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.apache.commons.math3.util.KthSelector;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Computes the pth quantile per group.
 *
 * @author Lara Gorini
 * @since 2.12
 */
public class QuantileOperator extends StoreResizableDoubleArrayOperator {

    private static final DataType TYPE = DoubleCell.TYPE;

    private QuantileSettingsPanel m_settingsPanel;

    private final QuantileFuntionSettings m_settings = new QuantileFuntionSettings();

    /**
     * Constructor for class QuantileOperator.
     *
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     * @param quantile the quantile value
     * @param estimation the type of estimation
     */
    protected QuantileOperator(final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings, final double quantile, final String estimation) {
        super(operatorData, globalSettings, opColSettings);
        m_settings.setQuantile(quantile);
        m_settings.setEstimation(estimation);
    }

    /**
     * Constructor for class QuantileOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public QuantileOperator(final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Quantile", true, false, DoubleValue.class, false), globalSettings,
            setInclMissingFlag(opColSettings), QuantileFuntionSettings.DEFAULT_QUANTILE,
            QuantileFuntionSettings.DEFAULT_ESTIMATION);
    }

    /**
     * Ensure that the flag is set correctly since this method does not support changing of the missing cell handling
     * option.
     *
     * @param opColSettings the {@link OperatorColumnSettings} to set
     * @return the correct {@link OperatorColumnSettings}
     */
    private static OperatorColumnSettings setInclMissingFlag(final OperatorColumnSettings opColSettings) {
        opColSettings.setInclMissing(false);
        return opColSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new QuantileOperator(getOperatorData(), globalSettings, opColSettings, m_settings.getFunctionModel()
            .getDoubleValue(), m_settings.getEstimationModel().getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnLabel() {
        return m_settings.getFunctionModel().getDoubleValue() + "-quantile";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        final double[] cells = super.getCells().getElements();
        if (cells.length == 0) {
            return DataType.getMissingCell();
        }
        double quantile = m_settings.getFunctionModel().getDoubleValue();
        EstimationType estType = Percentile.EstimationType.valueOf(m_settings.getEstimationModel().getStringValue());

        double evaluate = estType.evaluate(cells, quantile * 100, new KthSelector());
        return new DoubleCell(evaluate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the quantile per group.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetailedDescription() {
        return "Calculates the quantile per group by skipping missing cells. "
            + "There are several algorithms you can choose for (see advanced tab and "
            + "<a href=\"http://en.wikipedia.org/wiki/Quantile#Estimating_the_quantiles_of_a_population\">here</a>)."
            + "The default algorithm is 'LEGACY' with h=(N+1)p and Q<sub>p</sub> = x<sub>[h-1/2]</sub></p>";
    }

    /**
     * Override this method and return <code>true</code> if the operator requires additional settings.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * Override this method if the operator requires additional settings. {@inheritDoc}
     *
     */
    @Override
    public QuantileSettingsPanel getSettingsPanel() {
        if (m_settingsPanel == null) {
            m_settingsPanel = new QuantileSettingsPanel(m_settings);
        }
        return m_settingsPanel;
    }

    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
        throws NotConfigurableException {
        getSettingsPanel().loadSettingsFrom(settings, spec);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_settings.validate();
    }

    /**
     * {@link JPanel} that allows the user to specify layout mapping settings.
     *
     * @author Lara Gorini
     */
    private class QuantileSettingsPanel extends JTabbedPane {

        private static final long serialVersionUID = 1;

        private DialogComponentNumber m_functionComponent;

        private DialogComponentStringSelection m_estimationTypeComponent;

        /**
         * @param settings the {@link QuantileFuntionSettings} to use
         */
        public QuantileSettingsPanel(final QuantileFuntionSettings settings) {

            final SettingsModelDouble functionModel = settings.getFunctionModel();

            final SettingsModelString estimationModel = settings.getEstimationModel();

            // add basic Tab; only quantile value
            m_functionComponent = new DialogComponentNumber(functionModel, "Quantile: ", 0.1);
            addTab("Basic", m_functionComponent.getComponentPanel());

            //add advanced Tab
            List<String> estimationTypes = new LinkedList<>();
            EstimationType[] values = EstimationType.values();
            for (EstimationType estimationType : values) {
                estimationTypes.add(estimationType.name());
            }
            m_estimationTypeComponent =
                new DialogComponentStringSelection(estimationModel, "Estimation Type: ", estimationTypes);
            addTab("Advanced", m_estimationTypeComponent.getComponentPanel());
        }

        /**
         * Read value(s) of this dialog component from the configuration object. This method will be called by the
         * dialog pane only.
         *
         * @param settings the <code>NodeSettings</code> to read from
         * @param spec the input {@link DataTableSpec}
         * @throws NotConfigurableException If there is no chance for the dialog component to be valid (i.e. the
         *             settings are valid), e.g. if the given spec lacks some important columns or column types.
         */
        public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
            final DataTableSpec[] specs = new DataTableSpec[]{spec};
            m_functionComponent.loadSettingsFrom(settings, specs);
            m_estimationTypeComponent.loadSettingsFrom(settings, specs);
        }
    }

    /**
     * Class that save the settings of the {@link QuantileSettingsPanel}.
     *
     * @author Lara Gorini
     */
    private class QuantileFuntionSettings {

        private static final String CFG_CUSTOM_QUANTILE = "customQuantile";

        private static final double MIN_VALUE = 0;

        private static final double MAX_VALUE = 1;

        /** The default custom function. */
        public static final double DEFAULT_QUANTILE = 0.5;

        private final SettingsModelDouble m_function;

        private static final String CFG_CUSTOM_ESTIMATIOM = "customEstimation";

        /** The default custom estimation. */
        public static final String DEFAULT_ESTIMATION = "LEGACY";

        private final SettingsModelString m_estimationType;

        /**
         * Constructor.
         */
        public QuantileFuntionSettings() {
            this(DEFAULT_QUANTILE, DEFAULT_ESTIMATION);
        }

        /**
         * @throws InvalidSettingsException
         *
         */
        public void validate() throws InvalidSettingsException {
            final double val = m_function.getDoubleValue();
            checkBoundary(val);
        }

        /**
         * @param val
         * @throws InvalidSettingsException
         */
        private void checkBoundary(final double val) throws InvalidSettingsException {
            if (val <= MIN_VALUE || val > MAX_VALUE) {
                throw new InvalidSettingsException("Quantile must be greater than 0 and less or equal than 1");
            }
        }

        /**
         * @param quantile
         */
        public void setQuantile(final double quantile) {
            m_function.setDoubleValue(quantile);
        }

        /**
         * @param estimation
         */
        public void setEstimation(final String estimation) {
            m_estimationType.setStringValue(estimation);

        }

        private QuantileFuntionSettings(final double quantile, final String estimation) {
            m_function = new SettingsModelDouble(CFG_CUSTOM_QUANTILE, quantile);
            m_estimationType = new SettingsModelString(CFG_CUSTOM_ESTIMATIOM, estimation);
        }

        /**
         * @return the separator model
         */
        SettingsModelDouble getFunctionModel() {
            return m_function;
        }

        /**
         * @return the type of estimation
         */
        public SettingsModelString getEstimationModel() {
            return m_estimationType;
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException if the settings are invalid
         */
        public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            final double val =
                ((SettingsModelDouble)m_function.createCloneWithValidatedValue(settings)).getDoubleValue();
            checkBoundary(val);
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException if the settings are invalid
         */
        public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            m_function.loadSettingsFrom(settings);
            m_estimationType.loadSettingsFrom(settings);
        }

        /**
         * @param settings the {@link NodeSettingsWO} to write to
         */
        public void saveSettingsTo(final NodeSettingsWO settings) {
            m_function.saveSettingsTo(settings);
            m_estimationType.saveSettingsTo(settings);
        }

    }

}
