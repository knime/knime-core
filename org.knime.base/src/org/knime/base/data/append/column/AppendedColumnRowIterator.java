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
