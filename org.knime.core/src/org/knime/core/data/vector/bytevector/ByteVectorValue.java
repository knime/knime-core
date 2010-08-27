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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.09.2008 (ohl): created
 */
package org.knime.core.data.vector.bytevector;

import javax.swing.Icon;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.renderer.ByteVectorValuePixelRenderer;
import org.knime.core.data.renderer.ByteVectorValueStringRenderer;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;

/**
 * Implementations are vectors of fixed length storing byte counts at specific
 * positions. Only positive values of counts are supported. Each index can store
 * a number between 0 and 255 (both inclusive).<br />
 *
 * @author ohl, University of Konstanz
 */
public interface ByteVectorValue extends DataValue {

    /**
     * Meta information to byte vector values.
     *
     * @see DataValue#UTILITY
     */
    public static final UtilityFactory UTILITY = new ByteVectorUtilityFactory();

    /**
     * Returns the length of the byte vector. The number of stored counts.
     *
     * @return the number of bytes stored in the vector
     */
    public long length();

    /**
     * Calculates the checksum, the sum of all counts stored.
     *
     * @return the sum of all counts in this vector.
     */
    public long sumOfAllCounts();

    /**
     * Returns the number of counts larger than zero stored in this vector.
     *
     * @return the number of elements not equal to zero in this vector.
     */
    public int cardinality();

    /**
     * Returns the count stored at the specified position.
     *
     * @param index the index of the count to return
     * @return the count stored at the specified index.
     * @throws ArrayIndexOutOfBoundsException if the specified index is negative
     *             or too large.
     */
    public int get(final long index);

    /**
     * Checks all counts and returns true if they are all zero.
     *
     * @return true if all counts are zero.
     */
    public boolean isEmpty();

    /**
     * Finds the next index whose value is zero on or after the specified index.
     * Returns an index larger than or equal to the provided index, or -1 if no
     * such index exists. It is okay to pass an index larger than the length of
     * the vector.
     *
     * @param startIdx the first index to look for zero values.
     * @return the index of the next index with value zero, which is on or after
     *         the provided startIdx. Or -1 if the vector contains no zeros
     *         there after.
     * @throws ArrayIndexOutOfBoundsException if the specified startIdx negative
     */
    public long nextZeroIndex(final long startIdx);

    /**
     * Finds the next count not equal to zero on or after the specified index.
     * Returns an index larger than or equal to the provided index, or -1 if no
     * count larger than zero exists after the startIdx. It is okay to pass an
     * index larger than the length of the vector.
     *
     * @param startIdx the first index to look for non-zero counts. (It is
     *            allowed to pass an index larger then the vector's length.)
     * @return the index of the next count larger than zero, which is on or
     *         after the provided startIdx, or -1 if there isn't any
     * @throws ArrayIndexOutOfBoundsException if the specified startIdx is
     *             negative
     */
    public long nextCountIndex(final long startIdx);

    /** Implementations of the meta information of this value class. */
    public static class ByteVectorUtilityFactory extends UtilityFactory {
        /** Singleton icon to be used to display this cell type. */
        private static final Icon ICON =
                loadIcon(DoubleValue.class, "/bytevectoricon.png");

        private static final DataValueComparator COMPARATOR =
                new DataValueComparator() {
                    /** {@inheritDoc} */
                    @Override
                    protected int compareDataValues(final DataValue v1,
                            final DataValue v2) {
                        long c1 = ((ByteVectorValue)v1).cardinality();
                        long c2 = ((ByteVectorValue)v2).cardinality();
                        if (c1 < c2) {
                            return -1;
                        } else if (c1 > c2) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                };

        /** Only subclasses are allowed to instantiate this class. */
        protected ByteVectorUtilityFactory() {
            // intentionally left empty
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Icon getIcon() {
            return ICON;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueComparator getComparator() {
            return COMPARATOR;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataValueRendererFamily getRendererFamily(
                final DataColumnSpec spec) {
            return new DefaultDataValueRendererFamily(
                    ByteVectorValuePixelRenderer.INSTANCE,
                    ByteVectorValueStringRenderer.INSTANCE);
        }
    }

}
