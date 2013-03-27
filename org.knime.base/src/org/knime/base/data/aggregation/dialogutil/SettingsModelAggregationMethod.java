/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * Created on 07.12.2012 by koetter
 */
package org.knime.base.data.aggregation.dialogutil;

import java.util.List;

import javax.swing.event.ChangeListener;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * {@link SettingsModel} the stores the user settings of an {@link AggregationMethod}.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.8
 * @see DialogComponentAggregationMethod
 */
public class SettingsModelAggregationMethod extends SettingsModel {

    @SuppressWarnings("deprecation")
    private static final GlobalSettings DEFAULT_SETTINGS = new GlobalSettings(1000);

    private static final String CFG_MAX_UNIQUE_VALUES = "maxUniqueValues";

    private static final String CFG_VALUE_SEPARATOR = "valueSeparator";

    private static final String CFG_METHOD_SETTINGS = "methodSettings";

    private static final String CFG_METHOD_ID = "aggregationMethodId";

    private final int m_inputPortIndex;

    private AggregationMethod m_method;

    private final String m_configName;

    private String m_valueDelimiter;

    private int m_maxUniqueValues;

    /**
     * Creates a new object holding an {@link AggregationMethod}.
     *
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param defaultMethod the default {@link AggregationMethod} to use
     *
     */
    public SettingsModelAggregationMethod(final String configName, final AggregationMethod defaultMethod) {
        this(configName, -1, DEFAULT_SETTINGS.getValueDelimiter(),
             DEFAULT_SETTINGS.getMaxUniqueValues(), defaultMethod);
    }

    /**
     * Creates a new object holding an {@link AggregationMethod}.
     *
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param inputPortIndex the index of the input port that contains the data table spec
     * @param defaultMethod the default {@link AggregationMethod} to use
     *
     */
    public SettingsModelAggregationMethod(final String configName, final int inputPortIndex,
                                          final AggregationMethod defaultMethod) {
        this(configName, inputPortIndex, DEFAULT_SETTINGS.getValueDelimiter(),
             DEFAULT_SETTINGS.getMaxUniqueValues(), defaultMethod);
    }

    /**
     * Creates a new object holding an {@link AggregationMethod}.
     *
     * @param configName the identifier the value is stored with in the
     *            {@link org.knime.core.node.NodeSettings} object
     * @param inputPortIndex the index of the input port that contains the data table spec
     * @param separator the default separator to use
     * @param maxUniqueValues the number of maximum unique values
     * @param defaultMethod the default {@link AggregationMethod} to use
     *
     */
    public SettingsModelAggregationMethod(final String configName, final int inputPortIndex,
                  final String separator, final int maxUniqueValues, final AggregationMethod defaultMethod) {
        if (configName == null || configName.isEmpty()) {
            throw new IllegalArgumentException("The configName must not be empty");
        }
        if (defaultMethod == null) {
            throw new NullPointerException("defaultMethod must not be null");
        }
        m_configName = configName;
        m_method = defaultMethod;
        m_inputPortIndex = inputPortIndex;
        m_maxUniqueValues = maxUniqueValues;
        m_valueDelimiter = separator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prependChangeListener(final ChangeListener l) {
        super.prependChangeListener(l);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelAggregationMethod createClone() {
        return new SettingsModelAggregationMethod(m_configName, m_inputPortIndex,
                                                  m_valueDelimiter, m_maxUniqueValues, m_method);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_aggregationMethod";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * @param method the possibly new {@link AggregationMethod}
     * @param valueDelimiter the possibly new value separator
     * @param maxUniqueValues the possible new maximum number of unique values
     */
    protected void setValues(final AggregationMethod method, final String valueDelimiter,
                          final int maxUniqueValues) {
        boolean changed = updateAggregationMethod(method);
        changed = updateValueDelimiter(valueDelimiter) || changed;
        changed = updateMaxUniqueValues(maxUniqueValues) || changed;
        if (changed) {
            //One of the values has changed notify the change listener
            notifyChangeListeners();
        }
    }

    /**
     * set the value stored to the new value.
     *
     * @param method the new {@link AggregationMethod} to store.
     */
    public void setAggregationMethod(final AggregationMethod method) {
        if (updateAggregationMethod(method)) {
            notifyChangeListeners();
        }
    }

    /**
     * @param method the method
     * @return <code>true</code> if it has changed
     */
    private boolean updateAggregationMethod(final AggregationMethod method) {
        if (!m_method.equals(method)) {
            m_method = method;
            return true;
        }
        return false;
    }

    /**
     * @return the method
     */
    public AggregationMethod getAggregationMethod() {
        return m_method;
    }

    /**
     *
     * @param fileStoreFactory the {@link FileStoreFactory}
     * @param groupColNames the names of the columns to group by
     * @param spec the {@link DataTableSpec} of the table to process
     * @param noOfRows the number of rows of the input table
     * @param origColSpec the {@link DataColumnSpec} from the column to
     * aggregate
     * @return the {@link AggregationOperator} to use
     */
    public AggregationOperator getAggregationOperator(final FileStoreFactory fileStoreFactory,
          final List<String> groupColNames, final DataTableSpec spec,
          final DataColumnSpec origColSpec, final int noOfRows) {
        return m_method.createOperator(new GlobalSettings(fileStoreFactory, groupColNames,
                          m_maxUniqueValues, m_valueDelimiter, spec, noOfRows),
                              new OperatorColumnSettings(m_method.inclMissingCells(), origColSpec));
    }

    /**
     * @param valueDelimiter the valueSeparator to set
     */
    public void setValueDelimiter(final String valueDelimiter) {
        if (updateValueDelimiter(valueDelimiter)) {
            notifyChangeListeners();
        }
    }

    /**
     * @param valueDelimiter the value separator
     * @return <code>true</code> if it has changed
     */
    private boolean updateValueDelimiter(final String valueDelimiter) {
        if (!m_valueDelimiter.equals(valueDelimiter)) {
            m_valueDelimiter = valueDelimiter;
            return true;
        }
        return false;
    }

    /**
     * @return the valueSeparator
     */
    public String getValueDelimiter() {
        return m_valueDelimiter;
    }

    /**
     * @param maxUniqueValues the maximum number of unique values
     */
    public void setMaxUniqueValues(final int maxUniqueValues) {
        if (updateMaxUniqueValues(maxUniqueValues)) {
            notifyChangeListeners();
        }
    }

    /**
     * @param maxUniqueValues the maximum unique values
     * @return <code>true</code> if it has changed
     */
    private boolean updateMaxUniqueValues(final int maxUniqueValues) {
        if (m_maxUniqueValues != maxUniqueValues) {
            m_maxUniqueValues = maxUniqueValues;
            return true;
        }
        return false;
    }

    /**
     * @return the maxUniqueValues
     */
    public int getMaxUniqueValues() {
        return m_maxUniqueValues;
    }

    /**
     * @return the inputPortIndex
     */
    public int getInputPortIndex() {
        return m_inputPortIndex;
    }

    /**
     * Check if all settings are valid.
     *
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        if (m_maxUniqueValues < 0) {
            throw new InvalidSettingsException("MAximum unique values must be positive.");
        }
        if (m_method.hasOptionalSettings()) {
            final NodeSettings settings = new NodeSettings("tmp");
            m_method.saveSettingsTo(settings);
            m_method.validateSettings(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getNodeSettings(m_configName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            final NodeSettingsRO subSettings = settings.getNodeSettings(m_configName);
            final String methodId = subSettings.getString(CFG_METHOD_ID);
            final AggregationMethod method = AggregationMethods.getMethod4Id(methodId);
            if (method.hasOptionalSettings()) {
                final NodeSettingsRO methodSettings = subSettings.getNodeSettings(CFG_METHOD_SETTINGS);
                method.loadValidatedSettings(methodSettings);
            }
            // no default value, throw an exception instead
            setValues(method, subSettings.getString(CFG_VALUE_SEPARATOR),
                      subSettings.getInt(CFG_MAX_UNIQUE_VALUES));
        } catch (final IllegalArgumentException iae) {
            throw new InvalidSettingsException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final NodeSettingsWO subSettings = settings.addNodeSettings(m_configName);
        subSettings.addString(CFG_METHOD_ID, m_method.getId());
        subSettings.addString(CFG_VALUE_SEPARATOR, m_valueDelimiter);
        subSettings.addInt(CFG_MAX_UNIQUE_VALUES, m_maxUniqueValues);
        if (m_method.hasOptionalSettings()) {
            final NodeSettingsWO methodSettings = subSettings.addNodeSettings(CFG_METHOD_SETTINGS);
            m_method.saveSettingsTo(methodSettings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            DataTableSpec spec;
            if (m_inputPortIndex < 0 || specs == null
                    || specs.length < m_inputPortIndex || specs[m_inputPortIndex] == null) {
                //we do not have a data table spec available this might be the reason if
                //the dialog component is used in a node that does not aggregate an input
                //data table
                spec = new DataTableSpec();
            } else {
                spec = (DataTableSpec)specs[m_inputPortIndex];
            }
            final NodeSettingsRO subSettings = settings.getNodeSettings(m_configName);
            final String methodId = subSettings.getString(CFG_METHOD_ID);
            final AggregationMethod method = AggregationMethods.getMethod4Id(methodId);
            if (method.hasOptionalSettings()) {
                final NodeSettingsRO methodSettings = subSettings.getNodeSettings(CFG_METHOD_SETTINGS);
                method.loadSettingsFrom(methodSettings, spec);
            }
            // no default value, throw an exception instead
            setValues(method, subSettings.getString(CFG_VALUE_SEPARATOR),
                      subSettings.getInt(CFG_MAX_UNIQUE_VALUES));
        } catch (final Exception iae) {
            throw new NotConfigurableException(iae.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }
}
