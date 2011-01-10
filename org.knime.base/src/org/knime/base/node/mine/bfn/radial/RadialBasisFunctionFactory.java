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
package org.knime.base.node.mine.bfn.radial;

import org.knime.base.node.mine.bfn.BasisFunctionFactory;
import org.knime.base.node.mine.bfn.BasisFunctionLearnerRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.ModelContent;

/**
 * Basic interface for all basis function algorithms. Provides the function
 * getNewBasisFunction(.) to initialise a new prototype. This interface is
 * needed in order to create new prototypes in the general BasisFunctionLearner.
 * Hence a BasisFunctionLearner would be initialised with an object of type
 * BasisFunctionFactory. It is used as inter-class to init BasisFunction(s). One
 * implementation of the BasisFunctionFactory; here represents the
 * RadialBasisFunctionFactory object.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see RadialBasisFunctionLearnerRow
 * @see #commit(RowKey, DataCell, DataRow)
 */
public class RadialBasisFunctionFactory extends BasisFunctionFactory {
    
    /** theta minus value. */
    private final double m_thetaMinus;

    /** theta plus value. */
    private final double m_thetaPlus;

    /**
     * Creates a new factory for a radial basis function learner.
     * @param thetaMinus the upper bound activation for conflicting instances
     * @param thetaPlus the lower bound activation for non-conflicting instances
     * @param distance the choice of distance function
     * @param spec the input data to learn from
     * @param targetColumns the class info columns in the data
     */
    protected RadialBasisFunctionFactory(final double thetaMinus, 
            final double thetaPlus, final int distance, 
            final DataTableSpec spec, final String[] targetColumns) {
        super(spec, targetColumns, DoubleCell.TYPE, distance);
        m_thetaMinus = thetaMinus;
        m_thetaPlus = thetaPlus;
    }

    /**
     * Creates and returns a new {@link RadialBasisFunctionLearnerRow}
     * initialized with a center vector and a class label.
     * 
     * @param key this row's key
     * @param row the initial center vector
     * @param classInfo the class info
     * @return A new basisfunction.
     */
    @Override
    public BasisFunctionLearnerRow commit(final RowKey key,
            final DataCell classInfo, final DataRow row) {
        return new RadialBasisFunctionLearnerRow(key, classInfo, row,
                m_thetaMinus, m_thetaPlus, super.getDistance());
    }

    /**
     * Returns the upper bound for conflicting instances.
     * 
     * @return the upper bound for activation
     */
    public final double getThetaMinus() {
        return m_thetaMinus;
    }

    /**
     * Returns the lower bound for non-conflicting instances.
     * 
     * @return the lower bound for activation
     */
    public final double getThetaPlus() {
        return m_thetaPlus;
    }

    /** Key of theta minus. */
    static final String THETA_MINUS = "theta_minus";

    /** Key of theta plus. */
    static final String THETA_PLUS = "theta_plus";

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final ModelContent pp) {
        super.save(pp);
        pp.addDouble(THETA_MINUS, m_thetaMinus);
        pp.addDouble(THETA_PLUS, m_thetaPlus);
    }
}
