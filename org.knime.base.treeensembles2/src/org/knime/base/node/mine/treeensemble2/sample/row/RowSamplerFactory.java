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
 *   02.08.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.sample.row;

import org.apache.commons.math3.util.MathUtils;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnData;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnData;

/**
 * Acts as a kind of static factory that creates {@link RowSampler} objects according to the provided parameters.
 *
 * @author Adrian Nembach, KNIME.com
 */
public final class RowSamplerFactory {

    /**
     * Enlists the different row sampling modes.
     *
     * @author Adrian Nembach, KNIME.com
     */
    public enum RowSamplingMode {
        /**
         * Draws the subsample from the complete set of rows.
         */
        Random("Random"),
        /**
         * Draws first a subsample from the minority class and then draws the same number of samples
         * from each other class respectively. This means that all classes are represented by the same
         * number of rows in the final subsample.
         */
        EqualSize("Equal size"),
        /**
         * Draws the same fraction of samples from all classes. Therefore the class
         * distribution of the subsample is approximately the same as in the full set of rows.
         */
        Stratified("Stratified");

        private final String m_name;

        private RowSamplingMode(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }

        /**
         * @param isRegression true if the learned model is a regression model
         * @return an array of RowSamplingModes that can be used for the respective learning task.
         */
        public static RowSamplingMode[] values(final boolean isRegression) {
            if (isRegression) {
                return new RowSamplingMode[] {RowSamplingMode.Random};
            } else {
                return values();
            }
        }
    }

    private RowSamplerFactory() {
        // this is a static factory class
    }

    /**
     * Creates a {@link RowSampler} according to the provided parameters.
     *
     * @param targetColumn the target column that is used in the learning process
     * @param mode The sampling mode to be used (see {@link RowSamplingMode})
     * @param fraction the fraction of data that should be drawn (NOTE: the interpretation of this parameter depends
     * on the <b>mode</b> that is used.
     * @param drawWithReplacement true if the rows should be drawn with replacement i.e. one row can appear multiple times in the subsample
     * @return an object implementing the {@link RowSampler} interface.
     * a nominal target column).
     */
    public static RowSampler createRowSampler(final TreeTargetColumnData targetColumn, final RowSamplingMode mode, final double fraction,
        final boolean drawWithReplacement) {
        checkTargetColumnIsValid(targetColumn, mode);
        // check the case that all rows should be used
        if (!(mode == RowSamplingMode.EqualSize) && !drawWithReplacement && MathUtils.equals(fraction, 1.0)) {
            return new DefaultRowSampler(targetColumn.getNrRows());
        }
        final SubsetSelector<?> selector = drawWithReplacement ? SubsetWithReplacementSelector.getInstance() : SubsetNoReplacementSelector.getInstance();
        switch (mode) {
            case EqualSize:
                // we checked above that targetColumn is an instance of TreeTaretNominalColumnData
                return new EqualSizeRowSampler<>(fraction, selector, (TreeTargetNominalColumnData)targetColumn);
            case Stratified:
                // we checked above that targetColumn is an instance of TreeTaretNominalColumnData
                return new StratifiedRowSampler<>(fraction, selector, (TreeTargetNominalColumnData)targetColumn);
            case Random:
                return new RandomRowSampler<>(fraction, selector, targetColumn.getNrRows());
            default:
                throw new IllegalArgumentException("The provided RowSamplingMode \"" + mode + "\" is unknown.");
        }
    }

    private static void checkTargetColumnIsValid(final TreeTargetColumnData targetColumn, final RowSamplingMode mode) {
        if (mode == RowSamplingMode.EqualSize || mode == RowSamplingMode.Stratified) {
            if (!(targetColumn instanceof TreeTargetNominalColumnData)) {
                throw new IllegalArgumentException("The RowSamplingMode \"" + mode +"\" requires a nominal target column.");
            }
        }
    }


}
