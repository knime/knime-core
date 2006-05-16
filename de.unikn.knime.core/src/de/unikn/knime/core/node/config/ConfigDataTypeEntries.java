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

import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.def.DoubleCell;
import de.unikn.knime.core.data.def.IntCell;
import de.unikn.knime.core.data.def.StringCell;

/**
 * Register all known DataType elements here. By conventation the enumeration
 * element name has to be the simple class name of each DataType.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
enum ConfigDataTypeEntries {
    
    /**
     * StringType entry.
     */
    StringType {
        /**
         * @return StringCell.TYPE
         */
        DataType createDataType() {
            return StringCell.TYPE;
        }
    },
    
    /**
     * DoubleType entry.
     */
    DoubleType {
        /**
         * @return DoubleCell.TYPE
         */
        DataType createDataType() {
            return DoubleCell.TYPE;
        }
    },
    
    /**
     * IntType entry.
     */
    IntType {
        /**
         * @return IntCell.TYPE
         */
        DataType createDataType() {
            return IntCell.TYPE;
        }
    };
    
    /**
     * @return A DataType of this type.
     */
    abstract DataType createDataType();

}
