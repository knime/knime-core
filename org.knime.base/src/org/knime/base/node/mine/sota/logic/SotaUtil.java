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
 * 
 * History
 *   Mar 6, 2006 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public final class SotaUtil {
    private SotaUtil() {
    }

    /**
     * Returns <code>true</code> if there are missing values in given row and
     * <code>false</code> if not.
     * 
     * @param row row to check for missing values
     * @return <code>true</code> if there are missing values in given row and
     *         <code>false</code> if not
     */
    public static boolean hasMissingValues(final DataRow row) {
        for (int i = 0; i < row.getNumCells(); i++) {
            if (row.getCell(i).isMissing()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns <code>true</code> if given DataType is a FuzzyIntervalType,
     * otherwise <code>false</code>.
     * 
     * @param type DataType to check
     * @return <code>true</code> if given DataType is a FuzzyIntervalType,
     *         otherwise <code>false</code>
     */
    public static boolean isFuzzyIntervalType(final DataType type) {
        if (type.isCompatible(DoubleValue.class)) {
            return false;
        } else if (type.isCompatible(FuzzyIntervalValue.class)) {
            return true;
        }
        return false;
    }

    /**
     * Returns <code>true</code> if given DataType is a DoubleType or an
     * IntType, otherwise <code>false</code>.
     * 
     * @param type DataType to check
     * @return <code>true</code> if given DataType is a DoubleType or an
     *         IntType, otherwise <code>false</code>
     */
    public static boolean isNumberType(final DataType type) {
        return type.isCompatible(DoubleValue.class);
    }

    /**
     * Returns <code>true</code> if given DataType is a IntType, otherwise
     * <code>false</code>.
     * 
     * @param type DataType to check
     * @return <code>true</code> if given DataType is a IntType, otherwise
     *         <code>false</code>
     */
    public static boolean isIntType(final DataType type) {
        return type.isCompatible(IntValue.class);
    }
    
    /**
     * Returns <code>true</code> if given DataType is a StringTyoe, otherwise
     * <code>false</code>.
     * 
     * @param type DataType to check.
     * @return <code>true</code> if given DataType is a StringType, otherwise
     * <code>false</code>
     */
    public static boolean isStringType(final DataType type) {
        return type.isCompatible(StringValue.class);
    }
}
