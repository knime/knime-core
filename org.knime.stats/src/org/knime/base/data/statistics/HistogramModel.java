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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.10.20. by Gabor Bakos
 */
package org.knime.base.data.statistics;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataValue;

/**
 * An abstract model for histograms without the specifics for the bin definition.
 *
 * @author Gabor Bakos
 * @param <Def> The type of the bin definition.
 */
public abstract class HistogramModel<Def> {
    /** A bin of the histogram. */
    static class Bin<Def> {
        Bin(final Def def) {
            this(def, 0, 0);
        }

        private Bin(final Def def, final int count, final int hiLited) {
            m_def = def;
            setCount(count);
            setHiLited(hiLited);
        }

/**
         * @return the count
         */
        public int getCount() {
            return m_count;
        }

        /**
         * @param count the count to set
         */
        public void setCount(final int count) {
            this.m_count = count;
        }

        /**
         * @return the hiLited
         */
        public int getHiLited() {
            return m_hiLited;
        }

        /**
         * @param hiLited the hiLited to set
         */
        public void setHiLited(final int hiLited) {
            this.m_hiLited = hiLited;
        }

        /**
         * @return the m_def
         */
        public Def getDef() {
            return m_def;
        }

        private final Def m_def;

        private int m_count;

        private int m_hiLited;

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Bin [def=" + getDef() + ", count=" + getCount() + " (" + getHiLited() + ")]";
        }
    }
    ///the bins
    private List<Bin<Def>> m_bins;

    ///max count in column
    private int m_maxCount;

    ///rows added
    private int m_rowCount;

    ///column index
    private final int m_colIndex;

    ///column name
    private final String m_colName;


    /**
     * Constructs the model.
     *
     * @param numOfBins The number of bins.
     * @param colIndex The column index in the table.
     * @param colName The name of the column.
     *
     */
    protected HistogramModel(final int numOfBins, final int colIndex, final String colName) {
        m_bins = new ArrayList<Bin<Def>>(numOfBins);
        assert colIndex >= 0 : "column index: " + colIndex;
        this.m_colIndex = colIndex;
        m_colName = colName;
    }
    int addValue(final DataValue v, final boolean hiLited) {
        m_rowCount++;
        int min = findBin(v);
        m_bins.get(min).setCount(m_bins.get(min).getCount() + 1);
        if (hiLited) {
            m_bins.get(min).setHiLited(m_bins.get(min).getHiLited() + 1);
        }
        m_maxCount = Math.max(m_maxCount, m_bins.get(min).getCount());
        return min;
    }

    /**
     * Finds the bin number based on value ({@code v})
     *
     * @param v A {@link DataValue}.
     * @return The index of the bin or {@code -1} if not found.
     */
    public abstract int findBin(final DataValue v);

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_bins.toString();
    }


    /**
     * @return the bins (modifiable)
     */
    public List<Bin<Def>> getBins() {
        return m_bins;
    }
    /**
     * @return the colIndex
     */
    public int getColIndex() {
        return m_colIndex;
    }
    /**
     * @return the colName
     */
    public String getColName() {
        return m_colName;
    }

    /**
     * @return the maxCount
     */
    public int getMaxCount() {
        return m_maxCount;
    }

    /**
     * @return Number of elements added.
     */
    public int getRowCount() {
        return m_rowCount;
    }
    /**
     * @param rowCount the rowCount to set
     */
    public void setRowCount(final int rowCount) {
        this.m_rowCount = rowCount;
    }
    /**
     * @return The number which is representing the height of the histogram.
     */
    public int getReferenceNumber() {
        return getRowCount();
    }
    /**
     * @param maxCount the maxCount to set
     */
    protected void setMaxCount(final int maxCount) {
        this.m_maxCount = maxCount;
    }

    @Override
    public HistogramModel<Def> clone() {
        HistogramModel<Def> ret = createUninitializedClone();
        ret.setMaxCount(getMaxCount());
        ret.setRowCount(getRowCount());
        List<Bin<Def>> bins = ret.getBins();
        for (int i = bins.size(); i-- > 0;) {
            Bin<Def> binI = bins.get(i);
            Bin<Def> retBinI = ret.getBins().get(i);
            retBinI.setCount(binI.getCount());
            retBinI.setHiLited(binI.getHiLited());
        }
        return ret;
    }
    /**
     * Creates a new instance of the current model, although the bin information are not copied.
     * @return The new instance.
     */
    protected abstract HistogramModel<Def> createUninitializedClone();

}
