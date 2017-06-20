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
 *   Jun 20, 2017 (oole): created
 */
package org.knime.core.node.port.database.aggregation.function.presto;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.port.database.StatementManipulator;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.DBAggregationFunctionFactory;

/**
 * This class adds support for the <tt>APPROX_PERCENTILE</tt> aggregation function.
 *
 * @author Ole Ostergaard, KNIME.com
 * @since 3.4
 */
public class ApproxPercentileDBAggregationFunction implements DBAggregationFunction{

    private static final String ID = "APPROX_PERCENTILE";

    private final SettingsModelBoolean m_useWeightModel = new SettingsModelBoolean("useWeight", false);
    private final SettingsModelIntegerBounded m_weightModel =
            new SettingsModelIntegerBounded("weightParameter", 1, 1,Integer.MAX_VALUE);

    private final DialogComponentBoolean m_useWeightComponent = new DialogComponentBoolean(m_useWeightModel, "Weight: ");
    private final DialogComponentNumberEdit m_weightComponent = new DialogComponentNumberEdit(m_weightModel, "");

    private final SettingsModelDoubleBounded m_percentageModel =
            new SettingsModelDoubleBounded("percentageParameter", 0.5,0, 1);

    private final DialogComponentNumberEdit m_percentageComponent =
            new DialogComponentNumberEdit(m_percentageModel, "Percentage: ");

    private final SettingsModelBoolean m_useAccuracyModel = new SettingsModelBoolean("useAccuracy", false);
    private final SettingsModelDoubleBounded m_accuracyModel =
            new SettingsModelDoubleBounded("accuracyParameter", 0.5, 0, 1);

    private final DialogComponentBoolean m_useAccuracyComponent = new DialogComponentBoolean(m_useAccuracyModel, "Accuracy: ");
    private final DialogComponentNumberEdit m_accuracyComponent = new DialogComponentNumberEdit(m_accuracyModel, "");


    /**Factory for the parent class.*/
    public static final class Factory implements DBAggregationFunctionFactory {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getId() {
            return ID;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DBAggregationFunction createInstance() {
            return new ApproxPercentileDBAggregationFunction();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabel() {
        return getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompatible(final DataType type) {
        return type.isCompatible(DoubleValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Returns the approximate (optionally weighted) percentile for all input values using the optional "
            + "per-item weight, at the given percentage, with a maximum rank error of accuracy. "
            + "The weight must be an integer value of at least one. It is effectively a replication count "
            + "for the value in the percentile set. The percentage must be between zero and one and must be constant "
            + "for all input rows.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getSettingsPanel() {
        final JPanel rootPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        m_useWeightModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                m_weightModel.setEnabled(m_useWeightModel.getBooleanValue());
                m_useAccuracyModel.setEnabled(m_useWeightModel.getBooleanValue());
                m_accuracyModel.setEnabled(m_useWeightModel.getBooleanValue());
           }
        });
        rootPanel.add(m_useWeightComponent.getComponentPanel(), c);
        c.gridx++;
        rootPanel.add(m_weightComponent.getComponentPanel(),c);

        c.gridx++;
        rootPanel.add(m_percentageComponent.getComponentPanel(),c);
        c.gridx++;
        m_useAccuracyModel.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                m_accuracyModel.setEnabled(m_useAccuracyModel.getBooleanValue());
            }
        });
        rootPanel.add(m_useAccuracyComponent.getComponentPanel(),c);
        c.gridx++;
        rootPanel.add(m_accuracyComponent.getComponentPanel(),c);
        return rootPanel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_useWeightModel.loadSettingsFrom(settings);
        m_weightModel.loadSettingsFrom(settings);

        m_percentageModel.loadSettingsFrom(settings);

        m_useAccuracyModel.loadSettingsFrom(settings);
        m_accuracyModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec) throws NotConfigurableException {
        DataTableSpec[] specs = (new DataTableSpec[] {spec});
        m_useWeightComponent.loadSettingsFrom(settings, specs);
        m_weightComponent.loadSettingsFrom(settings, specs);
        m_weightModel.setEnabled(m_useWeightModel.getBooleanValue());
        m_percentageComponent.loadSettingsFrom(settings, specs);

        m_useAccuracyComponent.loadSettingsFrom(settings, specs);
        m_accuracyComponent.loadSettingsFrom(settings, specs);
        m_useAccuracyModel.setEnabled(m_useWeightModel.getBooleanValue());
        m_accuracyModel.setEnabled(m_useAccuracyModel.getBooleanValue()&& m_useWeightModel.getBooleanValue());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_useWeightModel.saveSettingsTo(settings);
        m_weightModel.saveSettingsTo(settings);

        m_percentageModel.saveSettingsTo(settings);

        m_useAccuracyModel.saveSettingsTo(settings);
        m_accuracyModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_useWeightModel.validateSettings(settings);
        m_weightModel.validateSettings(settings);

        m_percentageModel.validateSettings(settings);

        m_useAccuracyModel.validateSettings(settings);
        m_accuracyModel.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getType(final DataType originalType) {
        return DoubleCell.TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSQLFragment(final StatementManipulator manipulator, final String tableName, final String columnName) {
        StringBuilder builder = new StringBuilder();
        builder.append(getLabel());
        builder.append("(" + manipulator.quoteIdentifier(tableName) + "." + manipulator.quoteIdentifier(columnName)
        + ", ");
        if (m_useWeightModel.getBooleanValue()) {
            builder.append( m_weightModel.getIntValue() + ", ");
        }
        builder.append(m_percentageModel.getDoubleValue());
        if(m_useAccuracyModel.getBooleanValue() && m_useWeightModel.getBooleanValue()) {
            builder.append(", " + m_accuracyModel.getDoubleValue());
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSQLFragment4SubQuery(final StatementManipulator manipulator, final String tableName, final String subQuery) {
        StringBuilder builder = new StringBuilder();
        builder.append(getLabel());
        builder.append("((" + subQuery + "), ");
        if (m_useWeightModel.getBooleanValue()) {
            builder.append( m_weightModel.getIntValue() + ", ");
        }
        builder.append(m_percentageModel.getDoubleValue());
        if(m_useAccuracyModel.getBooleanValue() && m_useWeightModel.getBooleanValue()) {
            builder.append(", " + m_accuracyModel.getDoubleValue());
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName() {
        return getId();
    }

}
