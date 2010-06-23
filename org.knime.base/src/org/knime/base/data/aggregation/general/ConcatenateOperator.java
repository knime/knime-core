/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

package org.knime.base.data.aggregation.general;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;

import org.knime.base.data.aggregation.AggregationOperator;

/**
 * Returns the all values concatenated per group.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class ConcatenateOperator extends AggregationOperator {

    private final DataType m_type = StringCell.TYPE;

    private final StringBuilder m_buf = new StringBuilder();

    private boolean m_first = true;

    /**Constructor for class Concatenate.
     */
    public ConcatenateOperator() {
        super("Concatenate", false, false, 1,
                DataValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return m_type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final DataColumnSpec origColSpec, final int maxUniqueValues) {
        return new ConcatenateOperator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell.isMissing()) {
            return false;
        }
        if (m_first) {
            m_first = false;
        } else {
            m_buf.append(getDelimiter());
        }
        m_buf.append(cell.toString());
        return false;
    }

    /**
     * Override this method to change the standard delimiter.
     * @return the delimiter to use.
     */
    protected String getDelimiter() {
        return AggregationOperator.STANDARD_DELIMITER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        return new StringCell(m_buf.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_buf.setLength(0);
        m_first = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Concatenates the values per group.";
    }
}