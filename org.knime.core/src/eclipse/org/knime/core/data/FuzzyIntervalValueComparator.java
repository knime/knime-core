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
 * 
 * History
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data;

/**
 * Comparator returned by the {@link FuzzyIntervalValue} datacell type.
 *  
 * @see org.knime.core.data.FuzzyIntervalValue.FuzzyIntervalUtilityFactory
 * @author Michael Berthold, University of Konstanz
 */
public class FuzzyIntervalValueComparator extends DataValueComparator {

    /**
     * The compare function called by the abstract {@link DataValueComparator}
     * class. The comparison is based on the border values returned by the
     * <code>FuzzyIntervalValue.get{Min,Max}{Core,Support}()</code> methods.
     * Note that comparing fuzzy intervals is far from trivial - we base the
     * comparison used here on the center of gravities of the fuzzy sets. Do not
     * call this method directly. Use
     * {@link DataValueComparator#compare(DataCell, DataCell)} instead.
     * 
     * @see org.knime.core.data.DataValueComparator
     *      #compareDataValues(DataValue, DataValue)
     */
    @Override
    protected int compareDataValues(final DataValue v1, final DataValue v2) {

        FuzzyIntervalValue f1 = (FuzzyIntervalValue)v1;
        FuzzyIntervalValue f2 = (FuzzyIntervalValue)v2;

        // compute center of gravities of both trapezoid
        double f1CoG = f1.getCenterOfGravity();
        double f2CoG = f2.getCenterOfGravity();
        // perform actual comparison
        if (f1CoG > f2CoG) {
            return +1;
        }
        if (f1CoG == f2CoG) {
            return 0;
        }
        return -1;
    }

}
