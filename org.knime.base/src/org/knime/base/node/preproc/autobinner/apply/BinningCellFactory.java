/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   24.06.2010 (hofer): created
 */
package org.knime.base.node.preproc.autobinner.apply;

import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretize;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizeBin;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;

/**
 *
 * @author Heiko Hofer
 */
public class BinningCellFactory implements CellFactory {
    private String m_name;
    private final int m_colIdx;
    private final List<NumericBin> m_bins;
    private final DataCell m_mapMissingTo;
    private final DataCell m_defaultValue;

    /**
     * @param name The name of the column
     * @param colIdx The index of the column
     * @param discretize Binning configuration
     */
    public BinningCellFactory(final String name, final int colIdx,
            final PMMLDiscretize discretize) {
        m_name = name;
        m_colIdx = colIdx;
        m_mapMissingTo = discretize.getMapMissingTo() != null
            ? new StringCell(discretize.getMapMissingTo())
            : DataType.getMissingCell();
        m_defaultValue = discretize.getDefaultValue() != null
            ? new StringCell(discretize.getDefaultValue())
            : DataType.getMissingCell();
        m_bins = new ArrayList<NumericBin>();
        for (PMMLDiscretizeBin pmmlBin : discretize.getBins()) {
            DataCell binValue = new StringCell(pmmlBin.getBinValue());
            for (PMMLInterval pmmlInterval : pmmlBin.getIntervals()) {
                boolean isLeftOpen = pmmlInterval.getClosure().equals(
                        PMMLInterval.Closure.openOpen)
                        || pmmlInterval.getClosure().equals(
                                PMMLInterval.Closure.openClosed);
                boolean isRightOpen = pmmlInterval.getClosure().equals(
                        PMMLInterval.Closure.openOpen)
                        || pmmlInterval.getClosure().equals(
                                PMMLInterval.Closure.closedOpen);
                m_bins.add(new NumericBin(binValue,
                        isLeftOpen, pmmlInterval.getLeftMargin(),
                        isRightOpen, pmmlInterval.getRightMargin()));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        DataCell cell = row.getCell(m_colIdx);
        return new DataCell[]{apply(cell)};
    }

    /**
     * Apply a value to this binning trying to cover it at all available
     * bins.
     *
     * @param cell the value to cover
     * @return the bin's name as DataCell which cover's this value
     */
    private DataCell apply(final DataCell cell) {
        if (cell.isMissing()) {
            return m_mapMissingTo;
        }
        for (NumericBin bin : m_bins) {
            if (bin.covers(cell)) {
                return bin.getValue();
            }
        }
        return m_defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public DataColumnSpec[] getColumnSpecs() {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(m_name,
                StringCell.TYPE);
        DataCell[] values = new DataCell[m_bins.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_bins.get(i).getValue();
        }
        DataColumnDomainCreator domainCreator =
            new DataColumnDomainCreator(values);
        creator.setDomain(domainCreator.createDomain());
        return new DataColumnSpec[] {creator.createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey,
            final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double) rowCount);
    }

}
