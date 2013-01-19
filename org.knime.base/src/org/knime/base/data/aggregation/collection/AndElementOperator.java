/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.SetCell;


/**
 * Collection operator that returns the intersection of all elements.
 * @author Tobias Koetter, University of Konstanz
 */
public class AndElementOperator extends AggregationOperator {

    private final Set<DataCell> m_vals;
    private boolean m_first = true;

    /**Constructor for class AndElementOperator.
     * @param operatorData the operator data
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    protected AndElementOperator(final OperatorData operatorData,
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        try {
            m_vals = new LinkedHashSet<DataCell>(getMaxUniqueValues());
        } catch (final OutOfMemoryError e) {
            throw new IllegalArgumentException(
                    "Maximum unique values number to big");
        }
    }
    /**Constructor for class AndElementOperator.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public AndElementOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Intersection", true, false,
                CollectionDataValue.class, true), globalSettings,
                opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new AndElementOperator(getOperatorData(), globalSettings, opColSettings);
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
            final Set<DataCell> valCells =
                new HashSet<DataCell>(collectionCell.size());
            for (final DataCell valCell : collectionCell) {
                valCells.add(valCell);
            }
            if (m_first) {
                //we have to check this only for the first set since the result
                //can't get bigger
                if (valCells.size() >= getMaxUniqueValues()) {
                    setSkipMessage("Group contains to many unique values");
                    return true;
                }
                m_vals.addAll(valCells);
                m_first = false;
            } else {
                //keep only the matching ones
                m_vals.retainAll(valCells);
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
        m_first = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Creates a SetCell that contains the intersection of all "
        + "collection elements per group."
        + " Only elements that are present in all collections are kept.";
    }
}
