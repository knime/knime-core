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
import de.unikn.knime.core.data.DoubleType;
import de.unikn.knime.core.data.IntType;
import de.unikn.knime.core.data.StringType;

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
         * @return StringType.STRING_TYPE
         */
        StringType createDataType() {
            return de.unikn.knime.core.data.StringType.STRING_TYPE;
        }
    },
    
    /**
     * DoubleType entry.
     */
    DoubleType {
        /**
         * @return DoubleType.DOUBLE_TYPE
         */
        DoubleType createDataType() {
            return de.unikn.knime.core.data.DoubleType.DOUBLE_TYPE;
        }
    },
    
    /**
     * IntType entry.
     */
    IntType {
        /**
         * @return IntType.INT_TYPE
         */
        IntType createDataType() {
            return de.unikn.knime.core.data.IntType.INT_TYPE;
        }
    };
    
    /**
     * @return A DataType of this type.
     */
    abstract DataType createDataType();

}
