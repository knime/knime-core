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
package de.unikn.knime.base.data.append.column;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;

/**
 * The Iterator implementation to a <code>AppendColumnTable</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedColumnRowIterator extends RowIterator {

    private final DataType[] m_colType;

    private final RowIterator m_iterator;

    private final AppendedCellFactory m_factory;

    /**
     * Creates new row iterator based on the parameters of the table argument.
     * 
     * @param table The Table backing this iterator.
     */
    AppendedColumnRowIterator(final AppendedColumnTable table) {
        m_colType = table.getAppendedColumnClasses();
        m_iterator = table.getBaseIterator();
        m_factory = table.getFactory();
    }

    /**
     * @see de.unikn.knime.core.data.RowIterator#hasNext()
     */
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    /**
     * @see de.unikn.knime.core.data.RowIterator#next()
     */
    public DataRow next() {
        DataRow base = m_iterator.next();
        DataCell[] append = m_factory.getAppendedCell(base);
        if (append.length != m_colType.length) {
            throw new IllegalStateException(
                    "Illegal return value of factory, wrong length: "
                            + append.length + " vs. " + m_colType.length);
        }
        for (int i = 0; i < append.length; i++) {
            DataType resultType = append[i].getType();
            if (!m_colType[i].isOneSuperTypeOf(resultType)) {
                throw new IllegalStateException("Given cell is not subtype of "
                        + m_colType[i].getClass().getName() + ": "
                        + append.getClass());
            }
        }
        return new AppendedColumnRow(base, append);
    }

}
