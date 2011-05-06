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
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;


/**
 * Abstract class which has to be extended by all aggregation method operators
 * that can be registered using the AggregationOperator extension point.
 * All registered classes can be used in the nodes that use the
 * aggregation operators such as the group by node.
 * AggregationMethods are sorted first by the supported data type and then
 * by the label.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AggregationOperator implements AggregationMethod {
    /**If the aggregator should be skipped.*/
    private boolean m_skipped;

    private final GlobalSettings m_globalSettings;
    private final OperatorColumnSettings m_opColSettings;
    private final OperatorData m_operatorData;


    /**Constructor for class AggregationOperator.
     * @param operatorData the operator specific data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public AggregationOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        m_globalSettings = globalSettings;
        m_opColSettings = opColSettings;
        m_operatorData = operatorData;
    }

    /**
     * Creates a new instance of this operator. A new instance is created for
     * each column.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     *
     * @return a new instance of this operator
     *
     */
    public abstract AggregationOperator createInstance(
            GlobalSettings globalSettings,
            OperatorColumnSettings opColSettings);

    /**
     * @return the {@link OperatorData} of this operator
     */
    public OperatorData getOperatorData() {
        return m_operatorData;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends DataValue> getSupportedType() {
        return m_operatorData.getSupportedType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSupportedTypeLabel() {
        return AggregationMethods.getUserTypeLabel(getSupportedType());
    }

    /**
     * @return the maxUniqueValues
     */
    public int getMaxUniqueValues() {
        return m_globalSettings.getMaxUniqueValues();
    }

    /**
     * @return the standard delimiter to use for value separation
     */
    public String getValueDelimiter(){
        return m_globalSettings.getValueDelimiter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsMissingValueOption() {
        return m_operatorData.supportsMissingValueOption();
    }

    /**
     * @param inclMissingCells <code>true</code> if missing cells should be
     * considered during aggregation
     */
    void setInclMissing(final boolean inclMissingCells) {
        m_opColSettings.setInclMissing(inclMissingCells);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean inclMissingCells() {
        return m_opColSettings.inclMissingCells();
    }

    /**
     * @return <code>true</code> if this operator was skipped
     */
    public boolean isSkipped() {
        return m_skipped;
    }

    /**
     * @param cell the {@link DataCell} to consider during computing
     */
    public final void compute(final DataCell cell) {
        if (m_skipped) {
            return;
        }
        if (cell == null) {
            throw new NullPointerException("cell must not be null");
        }
        if (inclMissingCells() || !cell.isMissing()) {
            m_skipped = computeInternal(cell);
        }
    }

    /**
     * @return <code>true</code> if the original {@link DataColumnSpec} should
     * be kept.
     */
    public boolean keepColumnSpec() {
        return m_operatorData.keepColumnSpec();
    }

    /**
     * @param cell the {@link DataCell} to consider during computing the cell
     * can't be <code>null</code> but can be a missing cell
     * {@link DataCell#isMissing()} if the option is
     * {@link #inclMissingCells()} option is set to <code>true</code>.
     * @return <code>true</code> if this column should be skipped in further
     * calculations
     */
    protected abstract boolean computeInternal(final DataCell cell);

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec createColumnSpec(final String colName,
            final DataColumnSpec origSpec) {
        if (origSpec == null) {
            throw new NullPointerException(
                    "Original column spec must not be null");
        }
        final DataType newType = getDataType(origSpec.getType());
        final DataColumnSpecCreator specCreator;
        if (keepColumnSpec() && (newType == null
                || origSpec.getType().equals(newType))) {
             specCreator = new DataColumnSpecCreator(origSpec);
        } else {
            final DataType type;
            if (newType == null) {
                type = origSpec.getType();
            } else {
                type = newType;
            }
            specCreator = new DataColumnSpecCreator(colName, type);
        }
        specCreator.setName(colName);
        return specCreator.createSpec();
    }

    /**
     * @param origType the {@link DataType} of the original column to aggregate
     * @return the {@link DataType} of the aggregation result
     */
    protected abstract DataType getDataType(final DataType origType);

    /**
     * @return the result {@link DataCell}
     */
    public final DataCell getResult() {
        if (m_skipped) {
            return DataType.getMissingCell();
        }
        return getResultInternal();
    }

    /**
     * @return the result {@link DataCell}
     */
    protected abstract DataCell getResultInternal();

    /**
     * Should reset the operator to the start values.
     */
    public final void reset() {
        m_skipped = false;
        resetInternal();
    }

    /**
     * Should reset the operator to the start values.
     */
    protected abstract void resetInternal();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabel() {
        return m_operatorData.getLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnLabel() {
        if (supportsMissingValueOption() && inclMissingCells()) {
            //add the star to indicate that missing values are included
            //but only if the method supports the changing of this option
            //by the user to be compatible to old methods
            return m_operatorData.getColumnLabel() + "*";
        }
        return m_operatorData.getColumnLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createOperator(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return createInstance(globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCompatible(final DataColumnSpec origColSpec) {
        if (origColSpec == null) {
            throw new NullPointerException("column spec must not be null");
        }
        return isCompatible(origColSpec.getType());
    }

    /**
     * @param type the {@link DataType} to check for compatibility
     * @return true if this method supports the given {@link DataType}
     */
    public boolean isCompatible(final DataType type) {
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }
        return type.isCompatible(getSupportedType());
    }

    /**
     * @return <code>true</code> if this method checks the maximum unique
     * values limit.
     */
    public boolean isUsesLimit() {
        return m_operatorData.usesLimit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AggregationMethod o) {
        if (o instanceof AggregationOperator) {
            final AggregationOperator operator = (AggregationOperator)o;
            final int typeComp = getSupportedType().getName().compareTo(
                    operator.getSupportedType().getName());
            if (typeComp != 0) {
                //add the operators that support general types last
                if (getSupportedType().equals(DataValue.class)) {
                    return 1;
                } else if (
                        operator.getSupportedType().equals(DataValue.class)) {
                    return -1;
                }
                //sort by type first
                return typeComp;
            }
            //they support the same type sort them by operator label
        }
        //sort by label
        return getLabel().compareTo(o.getLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getLabel()
            + " Skipped: " + m_skipped
            + " Incl. missing: " + inclMissingCells();
    }
//
//    /**
//     * @param label the new label to use. This method is necessary for
//     * compatibility issues to support older methods.
//     */
//    void setLabel(final String label) {
//        m_operatorData.setLabel(label);
//    }
//
//    /**
//     * @param colName the new column name to use.
//     * This method is necessary for compatibility issues to support
//     * older methods.
//     */
//    void setColName(final String colName) {
//        m_operatorData.setColName(colName);
//    }
}
