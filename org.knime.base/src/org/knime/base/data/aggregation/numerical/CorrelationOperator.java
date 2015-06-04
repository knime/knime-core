/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 26, 2015 (Lara): created
 */
package org.knime.base.data.aggregation.numerical;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.general.ColumnSelectorOperator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * Calculates correlation coefficients
 *
 * @author Lara Gorini
 * @since 2.12
 */
public class CorrelationOperator extends ColumnSelectorOperator {

    private static final String COLUMN_SETTINGS = "columnSettings";

    private static final String CORRELATION_SETTINGS = "correlationSettings";

    private static final DataType TYPE = DoubleCell.TYPE;

    private DialogComponentButtonGroup m_correlationComponent;

    private final CorrelationMethodSettings m_settings = new CorrelationMethodSettings();

    /**
     * Cells selected for aggregation
     */
    private final ResizableDoubleArray m_cells;

    /**
     * Cells selected via class ColumnSelectorOperator
     */
    private final ResizableDoubleArray add_cells;

    private JTabbedPane m_rootPanel;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Calculates the correlation coefficient between two columns per group. Pearson correlation as default.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetailedDescription() {
        return "Calculates the correlation coefficient between two columns per group; thus, you have to select a second column. "
            + "Additionally, you can choose between different correlation coefficient definitions: see "
            + "<a href=\"http://en.wikipedia.org/wiki/Pearson_product-moment_correlation_coefficient\">Pearson</a>, "
            + "<a href=\"http://en.wikipedia.org/wiki/Spearman%27s_rank_correlation_coefficient\">Spearman</a> and "
            + "<a href=\"http://en.wikipedia.org/wiki/Kendall_tau_rank_correlation_coefficient\">Kendall</a>. "
            + "Pearson is the default one.";
    }

    /**
     * Constructor for class CorrelationOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     * @param columnName name of selected column holding data to compute correlation with
     * @param correlationMethod method for computing correlation coefficient
     */
    @SuppressWarnings("unchecked")
    protected CorrelationOperator(final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings,
        final String columnName, final String correlationMethod) {
        super(new OperatorData("Correlation", true, false, DoubleValue.class, false), globalSettings,
            setInclMissingFlag(opColSettings, false), columnName, "Correlation columns", DoubleValue.class);
        try {
            int maxVal = getMaxUniqueValues();
            if (maxVal == 0) {
                maxVal++;
            }
            m_settings.setCorrelationMethodModel(correlationMethod);
            m_cells = new ResizableDoubleArray(maxVal);
            add_cells = new ResizableDoubleArray(maxVal);
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException("Maximum unique values number too big");
        }
    }

    /**
     * Constructor for class CorrelationOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public CorrelationOperator(final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        this(globalSettings, opColSettings, null, CorrelationMethodSettings.DEFAULT_METHOD);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new CorrelationOperator(globalSettings, opColSettings, getColumnName(), m_settings
            .getCorrelationMethodModel().getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataRow row, final DataCell cell) {
        if (m_cells.getNumElements() >= getMaxUniqueValues() || add_cells.getNumElements() >= getMaxUniqueValues()) {
            setSkipMessage("Group contains too many values");
            return true;
        }
        m_cells.addElement(((DoubleValue)cell).getDoubleValue());
        add_cells.addElement(((DoubleValue)row.getCell(getSelectedColumnIndex())).getDoubleValue());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return CorrelationOperator.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        CorrelationMethods method = CorrelationMethods.valueOf(m_settings.getCorrelationMethodModel().getStringValue());
        double value = method.compute(m_cells.getElements(), add_cells.getElements());
        return new DoubleCell(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_cells.clear();
        add_cells.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnLabel() {

        return getColumnName() + "-" + m_settings.getCorrelationMethodModel().getStringValue().charAt(0) + "-corr";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent getSettingsPanel() {
        if (m_rootPanel == null) {
            JComponent columnPanel = super.getSettingsPanel();
            m_rootPanel = new JTabbedPane();
            m_rootPanel.addTab("Column selection", columnPanel);
            m_rootPanel.addTab("Correlation method", getDialogComponent().getComponentPanel());
        }
        return m_rootPanel;
    }

    private DialogComponentButtonGroup getDialogComponent() {
        if (m_correlationComponent == null) {
            final SettingsModelString correlationModel = m_settings.getCorrelationMethodModel();
            m_correlationComponent =
                new DialogComponentButtonGroup(correlationModel, "Correlation method", false,
                    CorrelationMethods.values());
        }
        return m_correlationComponent;
    }

    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO colSettings = settings.getNodeSettings(COLUMN_SETTINGS);
        super.loadValidatedSettings(colSettings);
        NodeSettingsRO corrSettings = settings.getNodeSettings(CORRELATION_SETTINGS);
        m_settings.loadSettingsFrom(corrSettings);
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
        throws NotConfigurableException {
        try {
            NodeSettingsRO colSettings = settings.getNodeSettings(COLUMN_SETTINGS);
            super.loadSettingsFrom(colSettings, spec);
            NodeSettingsRO corrSettings = settings.getNodeSettings(CORRELATION_SETTINGS);
            final DataTableSpec[] specs = new DataTableSpec[]{spec};
            getDialogComponent().loadSettingsFrom(corrSettings, specs);
        } catch (InvalidSettingsException e) {

        }
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        NodeSettingsWO colSettings = settings.addNodeSettings(COLUMN_SETTINGS);
        super.saveSettingsTo(colSettings);
        NodeSettingsWO corrSettings = settings.addNodeSettings(CORRELATION_SETTINGS);
        m_settings.saveSettingsTo(corrSettings);
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO colSettings = settings.getNodeSettings(COLUMN_SETTINGS);
        super.validateSettings(colSettings);
        NodeSettingsRO corrSettings = settings.getNodeSettings(CORRELATION_SETTINGS);
        m_settings.validateSettings(corrSettings);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        super.validate();
        m_settings.validate();
    }

    interface Method {
        public double compute(double[] x, double[] y);
    }

    enum CorrelationMethods implements ButtonGroupEnumInterface {
        PEARSON("Pearson", "Computes the Pearson correlation coefficient.", new Method() {
            @Override
            public double compute(final double[] x, final double[] y) {
                PearsonsCorrelation corr = new PearsonsCorrelation();
                return corr.correlation(x, y);
            }
        }),

        SPEARMAN("Spearman", "Computes the Spearman correlation coefficient.", new Method() {
            @Override
            public double compute(final double[] x, final double[] y) {
                SpearmansCorrelation corr = new SpearmansCorrelation();
                return corr.correlation(x, y);
            }
        }),

        KENDALL("Kendall", "Computes the Kendall correlation coefficient.", new Method() {
            @Override
            public double compute(final double[] x, final double[] y) {
                KendallsCorrelation corr = new KendallsCorrelation();
                return corr.correlation(x, y);
            }
        });

        private String m_label;

        private String m_desc;

        private Method m_method;

        private CorrelationMethods(final String label, final String desc, final Method method) {
            m_label = label;
            m_desc = desc;
            m_method = method;
        }

        public double compute(final double[] x, final double[] y) {
            return m_method.compute(x, y);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getActionCommand() {
            return name();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getToolTip() {
            return m_desc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDefault() {
            return PEARSON.equals(this);
        }
    }

    /**
     * Class that save the settings of the {@link CorrelationOperator}.
     *
     * @author Lara Gorini
     */
    private class CorrelationMethodSettings {

        private static final String CFG_CUSTOM_CORRELATION = "customCorrelation";

        /**
         * default correlation method.
         */
        public static final String DEFAULT_METHOD = "PEARSON";

        private final SettingsModelString m_correlationMethodModel;

        /**
         * Constructor.
         */
        public CorrelationMethodSettings() {
            this(DEFAULT_METHOD);
        }

        /**
         * @throws InvalidSettingsException
         *
         */
        public void validate() throws InvalidSettingsException {
            if (m_correlationMethodModel.getStringValue() == null) {
                throw new InvalidSettingsException("No method selected.");
            }
        }

        /**
         * @param correlationMethod
         */
        public void setCorrelationMethodModel(final String correlationMethod) {
            m_correlationMethodModel.setStringValue(correlationMethod);
        }

        private CorrelationMethodSettings(final String correlationMethod) {
            m_correlationMethodModel = new SettingsModelString(CFG_CUSTOM_CORRELATION, correlationMethod);
        }

        /**
         * @return the correlationMethod
         */
        public SettingsModelString getCorrelationMethodModel() {
            return m_correlationMethodModel;
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException if the settings are invalid
         */
        public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String val =
                ((SettingsModelString)m_correlationMethodModel.createCloneWithValidatedValue(settings))
                    .getStringValue();
            if (val == null) {
                throw new InvalidSettingsException("No method selected.");
            }
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException if the settings are invalid
         */
        public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            m_correlationMethodModel.loadSettingsFrom(settings);

        }

        /**
         * @param settings the {@link NodeSettingsWO} to write to
         */
        public void saveSettingsTo(final NodeSettingsWO settings) {
            m_correlationMethodModel.saveSettingsTo(settings);

        }

    }

}
