/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.core.node.config;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.def.DoubleCell;
import de.unikn.knime.core.data.def.IntCell;
import de.unikn.knime.core.data.def.StringCell;

/**
 * Register all known DataCell elements here. By conventation the enumeration
 * element name has to be the simple class name of each DataCell.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
enum ConfigDataCellEntries {
    
    /**
     * StringType entry.
     */
    DefaultStringCell {
        /**
         * @param value The String value.
         * @return StringCell.TYPE
         */
        StringCell createDataCell(final String value) {
            return new StringCell(value);
        }
        /**
         * @see ConfigDataCellEntries#toStringValue(DataCell)
         */
        String toStringValue(final DataCell cell) {
            return ((StringCell) cell).getStringValue();
        }
    },
    
    /**
     * DoubleType entry.
     */
    DefaultDoubleCell {
        /**
         * @param value The double value as String.
         * @return DoubleCell.TYPE
         */
        DoubleCell createDataCell(final String value) {
            return new DoubleCell(Double.parseDouble(value));
        }
        /**
         * @see ConfigDataCellEntries#toStringValue(DataCell)
         */
        String toStringValue(final DataCell cell) {
            return Double.toString(((DoubleCell) cell).getDoubleValue());
        }
    },
    
    /**
     * IntType entry.
     */
    DefaultIntCell {
        /**
         * @param value The int value as String.
         * @return IntCell.TYPE
         */
        IntCell createDataCell(final String value) {
            return new IntCell(Integer.parseInt(value));
        }
        /**
         * @see ConfigDataCellEntries#toStringValue(DataCell)
         */
        String toStringValue(final DataCell cell) {
            return Integer.toString(((IntCell) cell).getIntValue());
        }
    };
    
    /**
     * Creates a new DataCell of this type with the given value.
     * @param value The value for the new DataCell.
     * @return A DataCell of this type.
     */
    abstract DataCell createDataCell(final String value);
    
    /**
     * Returns a String representation of the given DataCell.
     * @param cell The cell to get String representation for.
     * @return A String representation.
     */
    abstract String toStringValue(final DataCell cell);

}
