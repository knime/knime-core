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

import java.awt.Component;
import java.util.Collection;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * Abstract class which has to be extended by all aggregation method operators
 * that can be registered using the AggregationOperator extension point.
 * The extending classes have to provide an empty constructor that can
 * call the {@link AggregationOperator#AggregationOperator(OperatorData)}
 * constructor with the operator specific {@link OperatorData} implementation.
 *
 * The {@link OperatorData} class holds all operator specific information
 * such as the name of the operator and if the operator supports missing values.
 *
 * The {@link GlobalSettings} class holds global informations such as the
 * column delimiter to use or the maximum number of unique values per group.
 * Implementations can use the {@link GlobalSettings#DEFAULT} object in the
 * constructor which is a dummy object which gets replaced when the operator
 * is created using the
 * {@link #createInstance(GlobalSettings, OperatorColumnSettings)} method.
 *
 * The {@link OperatorColumnSettings} contain column specific information for
 * the operator such as the {@link DataColumnSpec} of the column and if
 * missing values should be considered when aggregating the column.
 * The class also provides two default instances
 * {@link OperatorColumnSettings#DEFAULT_INCL_MISSING} and
 * {@link OperatorColumnSettings#DEFAULT_EXCL_MISSING} which can be used in
 * the constructor. These get like the {@link GlobalSettings} replaced by the
 * actual settings from the node dialog in the
 * {@link #createInstance(GlobalSettings, OperatorColumnSettings)} method.
 *
 * All registered classes can be used in the nodes that use the
 * aggregation operators such as the group by or pivoting node.
 * AggregationMethods are sorted first by the supported data type and then
 * by the label.
 * @see OperatorData
 * @see GlobalSettings
 * @see OperatorColumnSettings
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AggregationOperator implements AggregationMethod {
    /**If the aggregator should be skipped.*/
    private boolean m_skipped;
    private String m_skipMsg = "";

    private final GlobalSettings m_globalSettings;
    private final OperatorColumnSettings m_opColSettings;
    private final OperatorData m_operatorData;


    /**Constructor for class AggregationOperator. Uses
     * {@link GlobalSettings#DEFAULT} and
     * {@link OperatorColumnSettings#DEFAULT_INCL_MISSING} and calls the
     * {@link #AggregationOperator(OperatorData,
     * GlobalSettings, OperatorColumnSettings)} constructor.
     *
     * @param operatorData the operator specific data
     */
    public AggregationOperator(final OperatorData operatorData) {
        this(operatorData, GlobalSettings.DEFAULT,
                OperatorColumnSettings.DEFAULT_INCL_MISSING);
    }

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
     * each column. Remember to copy all operator specific settings when
     * creating a new instance!
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
    public String getValueDelimiter() {
        return m_globalSettings.getValueDelimiter();
    }


    /**
     * @return the {@link GlobalSettings} object
     */
    public GlobalSettings getGlobalSettings() {
        return m_globalSettings;
    }

    /**
     * @return the {@link OperatorColumnSettings} object
     * @since 2.7
     */
    public OperatorColumnSettings getOperatorColumnSettings() {
        return m_opColSettings;
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
     * @see #getSkipMessage()
     */
    public boolean isSkipped() {
        return m_skipped;
    }

    /**
     * This method can be used to mark this group explicitly as skipped.
     *
     * @param skipped <code>true</code> if the group should be marked as
     * skipped
     * @see #setSkipMessage(String)
     */
    protected void setSkipped(final boolean skipped) {
        m_skipped = skipped;
    }

    /**
     * @param msg the cause why the group was skipped
     * @see #setSkipped(boolean)
     */
    protected void setSkipMessage(final String msg) {
        m_skipMsg = msg;
    }

    /**
     * @return the cause why the group was skipped or an empty string
     * @see #isSkipped()
     */
    public String getSkipMessage() {
        return m_skipMsg;
    }

    /**
     * @param cell the {@link DataCell} to consider during computing
     * @deprecated use the {@link #compute(DataRow, int[])} method instead
     */
    @Deprecated
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
     * @param row the {@link DataRow} the given cell belongs to
     * @param idxs the indices of the columns to aggregate. Pass -1 to indicate
     * that the row key should be used.
     * @since 2.6
     */
    public final void compute(final DataRow row, final int... idxs) {
        if (m_skipped) {
            return;
        }
        if (row == null) {
            throw new NullPointerException("row must not be null");
        }
        if (idxs == null || idxs.length < 1) {
            throw new NullPointerException("indices must not be null or empty");
        }
        for (final int idx : idxs) {
            final DataCell cell;
            if (idx == -1) {
                //this is the row id option convert the key to a string cell
                //an proceed
                cell = new StringCell(row.getKey().getString());
            } else {
                cell = row.getCell(idx);
            }
            if (inclMissingCells() || !cell.isMissing()) {
                m_skipped = computeInternal(row, cell);
            }
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
     * Override this method if your {@link AggregationOperator} also requires
     * the {@link DataRow} and/or {@link ExecutionContext} during computation.
     * This method calls by default the {@link #computeInternal(DataCell)}
     * method. The given {@link DataRow} is filtered and contains only the
     * group and aggregation columns. The {@link DataColumnSpec}s as well as
     * the indices of the columns by name can be obtained from the
     * {@link GlobalSettings} object.
     *
     * @param row the filtered row {@link DataRow} the given cell belongs to.
     * The row contains only the group and aggregation columns.
     * @param cell the {@link DataCell} to consider during computing the cell
     * can't be <code>null</code> but can be a missing cell
     * {@link DataCell#isMissing()} if the {@link #inclMissingCells()}
     * option is set to <code>true</code>.
     * @return <code>true</code> if this column should be skipped in further
     * calculations
     * @since 2.6
     * @see AggregationOperator#computeInternal(DataCell)
     * @see GlobalSettings#getOriginalColumnSpec(int)
     * @see GlobalSettings#getOriginalColumnSpec(String)
     * @see GlobalSettings#findColumnIndex(String)
     */
    protected boolean computeInternal(final DataRow row, final DataCell cell) {
        return computeInternal(cell);
    }

    /**
     * If the {@link AggregationOperator} implementation also requires
     * the {@link DataRow} during computation override the
     * {@link #computeInternal(DataRow, DataCell)} method.
     * Even if the {@link #computeInternal(DataRow, DataCell)} method is
     * implemented  this method still might be called by previous
     * implementations prior version 2.6 and should compute a reasonable result.
     *
     * @param cell the {@link DataCell} to consider during computing the cell
     * can't be <code>null</code> but can be a missing cell
     * {@link DataCell#isMissing()} if the {@link #inclMissingCells()}
     * option is set to <code>true</code>.
     * @return <code>true</code> if this column should be skipped in further
     * calculations
     * @see AggregationOperator#computeInternal(DataRow, DataCell)
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
        m_skipMsg = "";
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
    public String getId() {
        return m_operatorData.getId();
    }

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
     * Override this method and return <code>true</code> if the operator
     * requires additional settings.
     *
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public boolean hasOptionalSettings() {
        //no settings required by default override if operator requires settings
        return false;
    }

    /**
     * Override this method if the operator requires additional settings.
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public Component getSettingsPanel() {
        //nothing to return by default override if operator requires settings
        return null;
    }

    /**
     * Override this method if the operator requires additional settings.
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        //nothing to read by default override if operator requires settings
    }

    /**
     * Override this method if the operator requires additional settings.
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
                     final DataTableSpec spec) throws NotConfigurableException {
      //nothing to read by default override if operator requires settings
    }

    /**
     * Override this method if the operator requires additional settings.
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to save by default override if operator requires settings
    }


    /**
     * Override this method if the operator requires additional settings.
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        // nothing to validate by default override if operator requires settings
    }

    /**
     * Override this method if the operator requires additional settings.
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void configure(final DataTableSpec spec) throws InvalidSettingsException {
     // nothing to validate by default override if operator requires settings
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public Collection<String> getAdditionalColumnNames() {
        //nothing to return by default override if operator requires additional
        //columns
        return null;
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
}
