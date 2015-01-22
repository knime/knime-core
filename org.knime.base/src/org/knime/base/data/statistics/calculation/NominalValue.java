/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 * History
 *   02.05.2014 (Marcel Hanser): created
 */
package org.knime.base.data.statistics.calculation;

import static org.knime.core.node.util.CheckUtils.checkArgument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.statistics.Statistic;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.util.MutableInteger;

/**
 * Computes the nominal values of a column.
 *
 * @author Marcel Hanser
 * @since 2.12
 */
public class NominalValue extends Statistic {
    private final int m_maxAmountOfCountedValues;

    private Map<DataCell, MutableInteger>[] m_nominalValues;

    private Set<String> m_exceededColumns;

    /**
     * @param maxAmountOfCountedDistinctValues max amount of values which will be considered per column
     * @param columns the columns to count possible values
     */
    public NominalValue(final int maxAmountOfCountedDistinctValues, final String... columns) {
        super(org.knime.core.data.NominalValue.class, columns);
        checkArgument(maxAmountOfCountedDistinctValues >= 0, "The maximal amount of counted values cannot be negative");
        m_maxAmountOfCountedValues = maxAmountOfCountedDistinctValues;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void init(final DataTableSpec spec, final int size) {
        m_nominalValues = new Map[size];
        for (int i = 0; i < size; i++) {
            m_nominalValues[i] = new HashMap<>();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void consumeRow(final DataRow dataRow) {
        int index = 0;
        for (int i : getIndices()) {
            MutableInteger mutableInt = m_nominalValues[index].get(dataRow.getCell(i));
            if (mutableInt == null) {
                if (m_nominalValues[index].size() < m_maxAmountOfCountedValues) {
                    m_nominalValues[index].put(dataRow.getCell(i), new MutableInteger(1));
                } else {
                    if (m_exceededColumns == null) {
                        m_exceededColumns = new HashSet<String>(5);
                    }
                    m_exceededColumns.add(getColumns()[index]);
                }
            } else {
                mutableInt.inc();
            }
            index++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String afterEvaluation() {
        if (m_exceededColumns != null) {
            return String.format("Following columns exceeded the maximum amount of distinct values:\n'%s'",
                ConvenienceMethods.getShortStringFrom(m_exceededColumns, 5));
        }
        return null;
    }
}
