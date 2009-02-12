/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.base.data.append.column;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;

/**
 * The Iterator implementation for an
 * {@link AppendedColumnTable}.
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
     * @param table the table backing this iterator
     */
    AppendedColumnRowIterator(final AppendedColumnTable table) {
        m_colType = table.getAppendedColumnClasses();
        m_iterator = table.getBaseIterator();
        m_factory = table.getFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
            if (!m_colType[i].isASuperTypeOf(resultType)) {
                throw new IllegalStateException("Given cell is not subtype of "
                        + m_colType[i].getClass().getName() + ": "
                        + append.getClass());
            }
        }
        return new AppendedColumnRow(base, append);
    }
}
