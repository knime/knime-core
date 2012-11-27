/*
 * ------------------------------------------------------------------------
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH,
 * Konstanz, Germany Website: http://www.knime.org; Email: contact@knime.org
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License, Version 3, as published by the
 * Free Software Foundation. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses>. Additional permission under GNU GPL version 3
 * section 7: KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in
 * APIs. Hence, KNIME and ECLIPSE are both independent programs and are not
 * derived from each other. Should, however, the interpretation of the GNU GPL
 * Version 3 ("License") under any applicable laws result in KNIME and ECLIPSE
 * being a combined program, KNIME GMBH herewith grants you the additional
 * permission to use and propagate KNIME together with ECLIPSE with only the
 * license terms in place for ECLIPSE applying to ECLIPSE and the GNU GPL
 * Version 3 applying for KNIME, provided the license terms of ECLIPSE
 * themselves allow for the respective use and propagation of ECLIPSE together
 * with KNIME. Additional permission relating to nodes for KNIME that extend the
 * Node Extension (and in particular that are based on subclasses of NodeModel,
 * NodeDialog, and NodeView) and that only interoperate with KNIME through
 * standard APIs ("Nodes"): Nodes are deemed to be separate and independent
 * programs and to not be covered works. Notwithstanding anything to the
 * contrary in the License, the License does not apply to Nodes, you are not
 * required to license Nodes under the License, and you are granted a license to
 * prepare and propagate Nodes, in each case even if such Nodes are propagated
 * with or for interoperation with KNIME. The owner of a Node may freely choose
 * the license terms applicable to such Node, including when such Node is
 * propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation;

import java.awt.Component;
import java.util.Collection;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Utility class that bundles an {@link AggregationMethod} with an
 * include missing cells flag. This class is used in the node dialog to add
 * additional properties to an {@link AggregationMethod} such as the include
 * missing cells flag.
 *
 * @author Tobias Koetter, University of Konstanz
 * @since 2.6
 */
public abstract class AggregationMethodDecorator
    implements AggregationMethod {

    /**Config key for the aggregation method.*/
    protected static final String CNFG_AGGR_METHODS = "aggregationMethod";

    /**COnfig key for the include missing value flag.*/
    protected static final String CNFG_INCL_MISSING_VALS = "inclMissingVals";

    private final AggregationMethod m_operatorTemplate;

    private boolean m_inclMissingCells;

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
    public AggregationMethodDecorator(final AggregationMethod method,
            final boolean inclMissingCells) {
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
}
