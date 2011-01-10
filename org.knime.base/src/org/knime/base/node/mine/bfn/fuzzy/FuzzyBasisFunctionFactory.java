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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.base.node.mine.bfn.BasisFunctionFactory;
import org.knime.base.node.mine.bfn.BasisFunctionLearnerRow;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.base.node.mine.bfn.fuzzy.shrink.Shrink;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.node.ModelContent;

/**
 * Basic interface for all basis function algorithms. Provides the function
 * getNewBasisFunction() to initialise a new prototype. This interface is
 * needed in order to create new prototypes in the general BasisFunctionLearner.
 * Hence a BasisFunctionLearner would be initialised with an object of type
 * BasisFunctionFactory. It is used as factory to create basisfunctions. One
 * implementation of the BasisFunctionFactory; here represents the
 * FuzzyBasisFunctionFactory object.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see FuzzyBasisFunctionLearnerRow
 * 
 * @see #commit(RowKey, DataCell, DataRow)
 */
public class FuzzyBasisFunctionFactory extends BasisFunctionFactory {
    
    /** The choice of fuzzy norm. */
    private final int m_norm;

    /** The choice of shrink procedure. */
    private final int m_shrink;

    /**
     * Creates a new factory fuzzy basisfunction along with a {@link Norm} and a
     * {@link Shrink} function.
     * @param norm the choice of fuzzy norm
     * @param shrink the choice of shrink procedure
     * @param spec the data to retrieve all columns and class info from
     * @param targetColumns the class info column in the data
     * @param distance the choice of distance function
     */
    public FuzzyBasisFunctionFactory(final int norm, final int shrink,
            final DataTableSpec spec,
            final String[] targetColumns, final int distance) {
        super(spec, targetColumns, FuzzyIntervalCell.TYPE, distance);
        m_norm = norm;
        m_shrink = shrink;
    }

    /**
     * Creates and returns a new row initialised with a class label and a center
     * vector.
     * 
     * @param key the key for this row
     * @param row the initial center vector
     * @param classInfo the class info
     * @return A new basisfunction 
     */
    @Override
    public BasisFunctionLearnerRow commit(final RowKey key,
            final DataCell classInfo, final DataRow row) {
        return new FuzzyBasisFunctionLearnerRow(key, classInfo, row, m_norm,
                m_shrink, getMinimums(), getMaximums());
    }

    /**
     * Returns the upper bound for conflicting instances.
     * 
     * @return the upper bound for activation
     */
    public final int getNorm() {
        return m_norm;
    }

    /**
     * Returns the lower bound for non-conflicting instances.
     * 
     * @return the lower bound for activation
     */
    public final int getShrink() {
        return m_shrink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final ModelContent pp) {
        super.save(pp);
        pp.addInt(Norm.NORM_KEY, m_norm);
        pp.addInt(Shrink.SHRINK_KEY, m_shrink);
    }
}
