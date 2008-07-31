/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
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
