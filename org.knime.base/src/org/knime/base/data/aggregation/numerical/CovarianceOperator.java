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
 *   May 29, 2015 (Lara): created
 */
package org.knime.base.data.aggregation.numerical;

import org.apache.commons.math.util.ResizableDoubleArray;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.base.data.aggregation.general.ColumnSelectorOperator;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;

/**
 * Calculates the covariance between two columns per group.
 *
 * @author Lara Gorini
 * @since 2.12
 *
 */
public class CovarianceOperator extends ColumnSelectorOperator {

    private static final DataType TYPE = DoubleCell.TYPE;

    private final ResizableDoubleArray m_cells;

    private final ResizableDoubleArray add_cells;

    /**
     * Constructor for class CovarianceOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     * @param columnName name of selected column holding data to compute correlation with
     */
    @SuppressWarnings("unchecked")
    protected CovarianceOperator(final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings,
        final String columnName) {
        super(new OperatorData("Covariance", true, false, DoubleValue.class, false), globalSettings,
            setInclMissingFlag(opColSettings, false), columnName, "Covariance columns", DoubleValue.class);
        try {
            int maxVal = getMaxUniqueValues();
            if (maxVal == 0) {
                maxVal++;
            }
            m_cells = new ResizableDoubleArray(maxVal);
            add_cells = new ResizableDoubleArray(maxVal);
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException("Maximum unique values number too big");
        }
    }

    /**
     * Constructor for class CovarianceOperator.
     *
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public CovarianceOperator(final GlobalSettings globalSettings, final OperatorColumnSettings opColSettings) {
        this(globalSettings, opColSettings, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Computes the covariance between two columns per group. Attention: calculation is bias-corrected and the number of data in both groups must match. If "
            + "the latter does not hold, a missing cell will be returned.";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnLabel() {
        return getColumnName() + "-covariance";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new CovarianceOperator(globalSettings, opColSettings, getColumnName());
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
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_cells.getNumElements() != add_cells.getNumElements()) {
            return DataType.getMissingCell();
        }
        Covariance cov = new Covariance();
        double value = cov.covariance(m_cells.getElements(), add_cells.getElements());
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
        // TODO Auto-generated method stub
        return false;
    }

}
