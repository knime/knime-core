/* 
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
 * History
 *   Apr 25, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data.def;

import de.unikn.knime.core.data.DataCellSerializer;
import de.unikn.knime.core.data.DataType;

public final class Test {

    /**
     * @param args
     */
    public static void main(String[] args) {
        IntCell c = new IntCell(3);
        DataType t = c.getType();
        DataCellSerializer<StringCell> seri = 
            DataType.getCellSerializer(StringCell.class);
        seri = DataType.getCellSerializer(StringCell.class);
        System.out.println("hei");
    }

}
