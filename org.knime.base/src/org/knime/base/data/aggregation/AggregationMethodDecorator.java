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
 */
package org.knime.base.data.aggregation;

import java.awt.Component;
import java.util.Collection;

import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRow;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.node.port.database.aggregation.InvalidAggregationFunction;

/**
 * Utility class that bundles an {@link AggregationMethod} with an
 * include missing cells flag. This class is used in the node dialog to add
 * additional properties to an {@link AggregationMethod} such as the include
 * missing cells flag.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.6
 */
public abstract class AggregationMethodDecorator implements AggregationMethod,
    AggregationFunctionRow<AggregationMethod> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AggregationMethodDecorator.class);

    /**Config key for the aggregation method.*/
    protected static final String CNFG_AGGR_METHODS = "aggregationMethod";

    /**Config key for the include missing value flag.*/
    protected static final String CNFG_INCL_MISSING_VALS = "inclMissingVals";

    private final AggregationMethod m_operatorTemplate;

    private boolean m_inclMissingCells;

    private boolean m_valid = true;

    /**Constructor for class AggregationOperatorDecorator.
     * @param method the {@link AggregationMethod} to use
     */
    public AggregationMethodDecorator(final AggregationMethod method) {
        this(method, method.inclMissingCells());
    }

    /**Constructor for class AggregationMethodDecorator.
     * @param method the {@link AggregationMethod} to use
     * @param inclMissingCells <code>true</code> if missing cells should be
     * considered
     */
    public AggregationMethodDecorator(final AggregationMethod method, final boolean inclMissingCells) {
        if (method == null) {
            throw new NullPointerException("method must not be null");
        }
        m_operatorTemplate = method;
        if (method.supportsMissingValueOption()) {
            //use the given missing value flag only if the method supports
            //the changing of the include missing cell option by the user
            m_inclMissingCells = inclMissingCells;
        } else {
            //use the default option of the method if it does not supports
            //the changing of the include missing cell option by the user
            m_inclMissingCells = method.inclMissingCells();
        }
    }

    /**
     * @return the method
     */
    public AggregationMethod getMethodTemplate() {
        return m_operatorTemplate;
    }

    /**
     * {@inheritDoc}
     * @since 2.11
     */
    @Override
    public AggregationMethod getFunction() {
        return getMethodTemplate();
    }

    /**
     * @return the inclMissingCells <code>true</code> if missing values should
     * be considered
     */
    @Override
    public boolean inclMissingCells() {
        return m_inclMissingCells;
    }

    /**
     * @param inclMissingCells the inclMissingCells to set
     */
    @Override
    public void setInclMissingCells(final boolean inclMissingCells) {
        m_inclMissingCells = inclMissingCells;
    }

    /**
     * @return <code>true</code> if the {@link AggregationMethod} supports
     * changing the missing value option
     */
    @Override
    public boolean supportsMissingValueOption() {
        return m_operatorTemplate.supportsMissingValueOption();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AggregationMethod o) {
        return m_operatorTemplate.compareTo(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return m_operatorTemplate.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabel() {
        return m_operatorTemplate.getLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec createColumnSpec(final String colName,
            final DataColumnSpec origSpec) {
        return m_operatorTemplate.createColumnSpec(colName, origSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnLabel() {
        return m_operatorTemplate.getColumnLabel();
    }

    /**
     * @return <code>true</code> if the operator is valid otherwise <code>false</code>
     * @since 2.8
     */
    @Override
    public boolean isValid() {
        return m_valid;
    }

    /**
     * @param valid <code>true</code> if the {@link ColumnAggregator} is valid
     * @since 2.8
     */
    @Override
    public void setValid(final boolean valid) {
        m_valid = valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompatible(final DataColumnSpec origColSpec) {
        return m_operatorTemplate.isCompatible(origColSpec);
    }

    /**
     * Returns <code>true</code> if the method supports the given
     * {@link DataType} otherwise it returns <code>false</code>.
     *
     * @param type the {@link DataType} to check for compatibility
     * @return <code>true</code> if this method supports the given
     * {@link DataType}
     */
    @Override
    public boolean isCompatible(final DataType type) {
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }
        return type.isCompatible(getSupportedType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createOperator(final GlobalSettings
            globalSettings, final OperatorColumnSettings opColSettings) {
        return m_operatorTemplate.createOperator(globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return m_operatorTemplate.getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends DataValue> getSupportedType() {
        return m_operatorTemplate.getSupportedType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSupportedTypeLabel() {
        return m_operatorTemplate.getSupportedTypeLabel();
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public boolean hasOptionalSettings() {
        return m_operatorTemplate.hasOptionalSettings();
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public Component getSettingsPanel() {
        return m_operatorTemplate.getSettingsPanel();
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_operatorTemplate.loadValidatedSettings(settings);
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
        throws NotConfigurableException {
        m_operatorTemplate.loadSettingsFrom(settings, spec);
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_operatorTemplate.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_operatorTemplate.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     * @since 2.11
     */
    @Override
    public void validate() throws InvalidSettingsException {
        m_operatorTemplate.validate();
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
        m_operatorTemplate.configure(spec);
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public Collection<String> getAdditionalColumnNames() {
        return m_operatorTemplate.getAdditionalColumnNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_operatorTemplate.getLabel() + " " + m_inclMissingCells;
    }

    /**
     * @param cfg {@link NodeSettingsWO} to write to
     * @param method the {@link AggregationFunction} to save
     * @since 2.11
     */
    public static void saveMethod(final NodeSettingsWO cfg, final AggregationMethod method) {
        cfg.addString(CNFG_AGGR_METHODS, method.getId());
        if (method.hasOptionalSettings()) {
            try {
                final NodeSettingsWO subConfig = cfg.addNodeSettings("functionSettings");
                method.saveSettingsTo(subConfig);
            } catch (Exception e) {
                LOGGER.error("Exception while saving settings for aggreation method '"
                    + method.getId() + "', reason: " + e.getMessage());
            }
        }
    }

    /**
     * @param tableSpec optional input {@link DataTableSpec}
     * @param cfg {@link NodeSettingsRO} to read from
     * @return the {@link AggregationFunction}
     * @throws InvalidSettingsException if the settings of the function are invalid
     * @since 2.11
     */
    public static AggregationMethod loadMethod(final DataTableSpec tableSpec, final NodeSettingsRO cfg)
                throws InvalidSettingsException {
        final String functionId = cfg.getString(CNFG_AGGR_METHODS);
        final AggregationMethod function = AggregationMethods.getMethod4Id(functionId);
        if (function instanceof InvalidAggregationFunction) {
            final String errMsg = "Exception while loading aggregation method. "
                    + ((InvalidAggregationFunction)function).getErrorMessage();
            LOGGER.warn(errMsg);
        } else {
            if (function.hasOptionalSettings()) {
                try {
                    final NodeSettingsRO subSettings = cfg.getNodeSettings("functionSettings");
                    if (tableSpec != null) {
                        //this method is called from the dialog
                        function.loadSettingsFrom(subSettings, tableSpec);
                    } else {
                        //this method is called from the node model where we do not
                        //have the DataTableSpec
                        function.loadValidatedSettings(subSettings);
                    }
                } catch (Exception e) {
                    final String errMsg = "Exception while loading settings for aggreation function '"
                        + function.getId() + "', reason: " + e.getMessage();
                    LOGGER.error(errMsg);
                }
            }
        }
        return function;
    }
}
