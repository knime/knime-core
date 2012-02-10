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

package org.knime.base.data.aggregation.general;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.util.MutableInteger;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Returns the concatenation of all different values per group and the
 * number of cells per distinct value.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class UniqueConcatenateWithCountOperator
    extends AggregationOperator {

    private final DataType m_type = StringCell.TYPE;

    private final Map<DataCell, MutableInteger> m_vals;

    /**Constructor for class UniqueConcatenateWithCountOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public UniqueConcatenateWithCountOperator(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("uniqueConcatenateWithCount_V2.4",
                "Unique concatenate with count",
                "Unique concatenate with count", true, false,
                DataValue.class, true), globalSettings, opColSettings);
    }

    /**Constructor for class UniqueConcatenateWithCountOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected UniqueConcatenateWithCountOperator(
            final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        try {
            m_vals = new LinkedHashMap<DataCell, MutableInteger>(
                    getMaxUniqueValues());
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException(
                    "Maximum unique values number to big");
        }
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
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new UniqueConcatenateWithCountOperator(globalSettings,
                opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        final MutableInteger counter = m_vals.get(cell);
        if (counter != null) {
            counter.inc();
            return false;
        }
        //check if the map contains more values than allowed
        //before adding a new value
        if (m_vals.size() >= getMaxUniqueValues()) {
            setSkipMessage("Group contains to many unique values");
            return true;
        }
        m_vals.put(cell, new MutableInteger(1));
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        if (m_vals.isEmpty()) {
            return DataType.getMissingCell();
        }
        final StringBuilder buf = new StringBuilder();
        final Set<Entry<DataCell, MutableInteger>> entrySet =
            m_vals.entrySet();
        boolean first = true;
        for (final Entry<DataCell, MutableInteger> entry : entrySet) {
            if (first) {
                first = false;
            } else {
                buf.append(getValueDelimiter());
            }
            buf.append(entry.getKey().toString());
            buf.append('(');
            buf.append(Integer.toString(entry.getValue().intValue()));
            buf.append(')');
        }
        return new StringCell(buf.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void resetInternal() {
        m_vals.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Concatenates each member only once with the number of its "
             + "occurrences per group.";
    }
}