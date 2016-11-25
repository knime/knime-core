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
 *   29.07.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.sample.row;

/**
 * Abstract implementation of a {@link RowSampler}.
 * It provides methods for subclasses to access parameters that are shared by the different implementations.
 *
 * @author Adrian Nembach, KNIME.com
 * @param <T> the {@link RowSample} type that is used.
 */
public abstract class AbstractRowSampler <T extends RowSample> implements RowSampler {

    private final double m_fraction;
    private final SubsetSelector<T> m_subsetSelector;
    private final int m_nrRows;

    /**
     * @param fraction the fraction that should be used for sampling
     * @param subsetSelector the {@link SubsetSelector} that is used to draw samples (with or without replacement)
     * @param nrRows the number of rows in the full set of rows (i.e. the size of the input table)
     */
    public AbstractRowSampler(final double fraction, final SubsetSelector<T> subsetSelector, final int nrRows) {
        m_fraction = fraction;
        m_subsetSelector = subsetSelector;
        m_nrRows = nrRows;
    }

    /**
     * NOTE: The interpretation of the fraction depends on the individual sampling strategy
     * @return the fraction of data that should be included.
     */
    protected double getFraction() {
        return m_fraction;
    }

    /**
     * @return the subset selector that is used to select subsets from within a super set.
     */
    protected SubsetSelector<T> getSubsetSelector() {
        return m_subsetSelector;
    }

    /**
     * @return the number of rows of the full set from which we want to draw samples.
     */
    protected int getNrRows() {
        return m_nrRows;
    }

}
