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

package org.knime.base.data.aggregation.collection;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.SetCell;

import org.knime.base.data.aggregation.AggregationOperator;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Collection operator that returns the union of all elements.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class OrElementOperator extends AggregationOperator {

    private final Set<DataCell> m_vals;

    /**Constructor for class UnionOperator.
     * @param maxUniqueValues the maximum number of unique values
     */
    public OrElementOperator(final int maxUniqueValues) {
        super("Union", true, false, maxUniqueValues,
                CollectionDataValue.class);
        try {
            m_vals = new LinkedHashSet<DataCell>(maxUniqueValues);
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException(
                    "Maximum unique values number to big");
        }
    }

    /**Constructor for class UnionOperator.
     * @param label of the derived class
     * @param colName the column name
     * @param maxUniqueValues the maximum number of unique values
     */
    protected OrElementOperator(final String label, final String colName,
            final int maxUniqueValues) {
        super(label, colName, true, false, maxUniqueValues,
                CollectionDataValue.class);
        try {
            m_vals = new LinkedHashSet<DataCell>(maxUniqueValues);
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException(
                    "Maximum unique values number to big");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(final DataColumnSpec origColSpec,
            final int maxUniqueValues) {
        return new OrElementOperator(maxUniqueValues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return SetCell.getCollectionType(origType.getCollectionElementType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell instanceof CollectionDataValue) {
            //missing cells are skipped
            final CollectionDataValue collectionCell =
                (CollectionDataValue)cell;
            for (final DataCell valCell : collectionCell) {
                if (m_vals.contains(cell)) {
                    continue;
                }
                //check if the set contains more values than allowed
                //before adding a new value
                if (m_vals.size() >= getMaxUniqueValues()) {
                    return true;
                }
                m_vals.add(valCell);
            }
        }
        return false;
    }

    /**
     * @return the values that have been collected so far as
     * an unmodifiable collection
     */
    protected Collection<DataCell> getValues() {
        return Collections.unmodifiableCollection(m_vals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        return CollectionCellFactory.createSetCell(m_vals);
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
        return "Creates a SetCell that contains the union of all "
        + "collection elements per group";
    }

}
