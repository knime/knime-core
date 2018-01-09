/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * Created on 2014.03.20. by gabor
 */
package org.knime.base.node.preproc.bitvector.expand;


import org.knime.base.node.util.ExpandVectorNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of ExpandBitVector. Expands the bitvector to individual integer columns.
 *
 * @author Gabor Bakos
 * @since 2.10
 */
public class ExpandBitVectorNodeModel extends ExpandVectorNodeModel {
    private static final IntCell[] VALUES = new IntCell[2];
    static {
        for (int i = 2; i-- > 0;) {
            VALUES[i] = new IntCell(i);
        }
    }
    private static final String DEFAULT_OUTPUT_PREFIX = "bitvector";

    /**
     * @return The generated output column names' prefix model.
     */
    protected static SettingsModelString createOutputPrefix() {
        return new SettingsModelString(CFGKEY_OUTPUT_PREFIX, DEFAULT_OUTPUT_PREFIX);
    }

    /**
     * Constructor for the node model.
     */
    protected ExpandBitVectorNodeModel() {
        super(createOutputPrefix(), BitVectorValue.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractCellFactory createCellFactory(final String[] colNames, final DataColumnSpec[] outputColumns,
        final int inputIndex) {
        return new AbstractCellFactory(outputColumns) {
            @Override
            public DataCell[] getCells(final DataRow row) {
                DataCell[] vs = new DataCell[colNames.length];
                DataCell cell = row.getCell(inputIndex);
                if (cell instanceof BitVectorValue) {
                    BitVectorValue bvv = (BitVectorValue)cell;
                    int length = Math.min(vs.length, (int)bvv.length());
                    for (int i = length; i-- > 0;) {
                        vs[i] = VALUES[bvv.get(i) ? 1 : 0];
                    }
                    for (int i = vs.length; i-- > length;) {
                        vs[i] = DataType.getMissingCell();
                    }
                } else {
                    for (int i = 0; i < vs.length; i++) {
                        vs[i] = DataType.getMissingCell();
                    }
                }
                return vs;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long getCellLength(final DataCell cell) {
        if (cell instanceof BitVectorValue) {
            BitVectorValue bvv = (BitVectorValue)cell;
            return bvv.length();
        }
        //we assume it is missing
        if (cell.isMissing()) {
            return 0;
        }
        throw new IllegalArgumentException("Wrong type: " + cell.getType());
    }
}
