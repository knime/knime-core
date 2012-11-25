/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 9, 2012 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Vector;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.ColumnRearranger.SpecAndFactoryObject;
import org.knime.core.data.container.RearrangeColumnsTable.NewColumnsProducerMapping;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperatorInternals;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnRearrangerFunction extends StreamableFunction {

    private final NewColumnsProducerMapping m_newColumnsMapping;
    private final boolean[] m_isFromRefTables;
    private final int[] m_includesIndices;
    private final StreamableOperatorInternals m_emptyInternals;

    /**
     * @param rearranger */
    ColumnRearrangerFunction(final ColumnRearranger rearranger) {
        this(rearranger, null);
    }

    /**
     * @param rearranger
     * @param emptyInternals */
    ColumnRearrangerFunction(final ColumnRearranger rearranger, final StreamableOperatorInternals emptyInternals) {
        Vector<SpecAndFactoryObject> includes = rearranger.getIncludes();
        m_newColumnsMapping = RearrangeColumnsTable.createNewColumnsProducerMapping(includes);
        final int size = includes.size();
        boolean[] isFromRefTable = new boolean[size];
        int[] includesIndex = new int[size];
        int newColIndex = 0;
        for (int i = 0; i < size; i++) {
            SpecAndFactoryObject c = includes.get(i);
            if (c.isConvertedColumn()) {
                isFromRefTable[i] = false;
                includesIndex[i] = newColIndex;
                newColIndex++;
            } else if (c.isNewColumn()) {
                isFromRefTable[i] = false;
                includesIndex[i] = newColIndex;
                newColIndex++;
            } else {
                isFromRefTable[i] = true;
                int originalIndex = c.getOriginalIndex();
                includesIndex[i] = originalIndex;
            }
        }
        m_isFromRefTables = isFromRefTable;
        m_includesIndices = includesIndex;
        m_emptyInternals = emptyInternals;
    }

    /** {@inheritDoc} */
    @Override
    public DataRow compute(final DataRow inputRow) {
        DataRow appendRow = RearrangeColumnsTable.calcNewCellsForRow(inputRow, m_newColumnsMapping);
        return JoinTableIterator.createOutputRow(inputRow, appendRow, m_includesIndices, m_isFromRefTables);
    }

    /** {@inheritDoc} */
    @Override
    public void finish() {
        super.finish();
        RearrangeColumnsTable.finishProcessing(m_newColumnsMapping);
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperatorInternals saveInternals() {
        return m_emptyInternals;
    }

}
