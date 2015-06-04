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

package org.knime.base.data.aggregation.general;

import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Selects an additional column from a given Data Table
 *
 * @author Lara Gorini
 * @since 2.12
 */
public abstract class ColumnSelectorOperator extends AggregationOperator {

    private ColumnSelectorSettingsPanel m_settingsPanel;

    private final ColumnSelectorSettings m_settings = new ColumnSelectorSettings();

    private final int m_columnIndex;

    private final String m_labelName;

    private final Class<? extends DataValue>[] m_classFilter;

    /**
     * Constructor for class ColumnSelectorOperator.
     *
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     * @param columnName name of additional selected column
     * @param labelName label for dialog in front of selection panel
     * @param classFilter which classes are available for column selection
     */
    protected ColumnSelectorOperator(final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings, final String columnName, final String labelName,
        @SuppressWarnings("unchecked") final Class<? extends DataValue>... classFilter) {
        super(operatorData, globalSettings, opColSettings);
        m_settings.setColumnName(columnName);
        m_labelName = labelName;
        m_classFilter = classFilter;
        m_columnIndex = globalSettings.findColumnIndex(columnName);
    }

    /**
     * @return index of selected column
     */
    public int getSelectedColumnIndex() {
        return m_columnIndex;
    }

    @Override
    public Collection<String> getAdditionalColumnNames() {
        Collection<String> coll = new LinkedList<>();
        coll.add(m_settings.getColumnModel().getStringValue());
        return coll;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public JComponent getSettingsPanel() {
        return getPanel();
    }

    private ColumnSelectorSettingsPanel getPanel() {
        if (m_settingsPanel == null) {
            m_settingsPanel = new ColumnSelectorSettingsPanel(m_settings, m_labelName, m_classFilter);
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
        getPanel().loadSettingsFrom(settings, spec);
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
     * {@inheritDoc}
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        m_settings.configure(spec);
    }

    /**
     * @return name of selected column
     */
    protected String getColumnName() {
        return m_settings.getColumnModel().getStringValue();
    }

    /**
     * {@link JPanel} that allows the user to specify layout mapping settings.
     *
     * @author Lara Gorini
     */
    private class ColumnSelectorSettingsPanel extends JPanel {

        private static final long serialVersionUID = 1;

        private DialogComponentColumnNameSelection m_ColumnTypeComponent;

        /**
         * @param settings the {@link ColumnSelectorSettings} to use
         * @param labelName label for dialog in front of selection panel
         * @param classFilter which classes are available for selection
         */
        public ColumnSelectorSettingsPanel(final ColumnSelectorSettings settings, final String labelName,
            @SuppressWarnings("unchecked") final Class<? extends DataValue>... classFilter) {

            final SettingsModelString columnSelectionModel = settings.getColumnModel();
            m_ColumnTypeComponent =
                new DialogComponentColumnNameSelection(columnSelectionModel, labelName + ": ", 0, classFilter);
            add(m_ColumnTypeComponent.getComponentPanel());
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
            m_ColumnTypeComponent.loadSettingsFrom(settings, specs);
        }
    }

    /**
     * Class that save the settings of the {@link ColumnSelectorSettingsPanel}.
     *
     * @author Lara Gorini
     */
    private class ColumnSelectorSettings {

        private static final String CFG_CUSTOM_COLUMN = "customColumn";

        private final SettingsModelString m_columnName;

        /**
         * Constructor.
         */
        public ColumnSelectorSettings() {
            this(null);
        }

        /**
         * @throws InvalidSettingsException
         *
         */
        public void validate() throws InvalidSettingsException {
            if (m_columnName.getStringValue() == null) {
                throw new InvalidSettingsException("No column selected.");
            }
        }

        /**
         * @param columnName
         */
        public void setColumnName(final String columnName) {
            m_columnName.setStringValue(columnName);
        }

        private ColumnSelectorSettings(final String columnName) {
            m_columnName = new SettingsModelString(CFG_CUSTOM_COLUMN, columnName);
        }

        /**
         * @return the type of estimation
         */
        public SettingsModelString getColumnModel() {
            return m_columnName;
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException if the settings are invalid
         */
        public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String val =
                ((SettingsModelString)m_columnName.createCloneWithValidatedValue(settings)).getStringValue();
            if (val == null) {
                throw new InvalidSettingsException("No column selected.");
            }
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException if the settings are invalid
         */
        public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            m_columnName.loadSettingsFrom(settings);
        }

        /**
         * @param settings the {@link NodeSettingsWO} to write to
         */
        public void saveSettingsTo(final NodeSettingsWO settings) {
            m_columnName.saveSettingsTo(settings);
        }


        private void configure(final DataTableSpec spec) throws InvalidSettingsException {

            if (spec.findColumnIndex(m_columnName.getStringValue()) < 0) {
                throw new InvalidSettingsException("Selected column " + m_columnName.getStringValue()
                    + " is not contained in Data Table");
            }

        }

    }

}
