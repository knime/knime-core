/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on 2013.10.02. by Gabor Bakos
 */
package org.knime.base.data.statistics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.knime.base.data.statistics.HistogramModel.Bin;
import org.knime.base.data.xml.SvgCell;
import org.knime.base.data.xml.SvgValueRenderer;
import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;
import org.knime.core.util.Pair;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.svg.SVGDocument;

/**
 * A helper class to add a column with histogram images.
 *
 * @since 2.9
 *
 * @author Gabor Bakos
 */
public class HistogramColumn implements Cloneable {
    /**
     *
     */
    private static final int MIN_MAX_AREA_HEIGHT = 15;

    /**
     *
     */
    private static final BigDecimal FIVE = new BigDecimal(5);

    /**
     * The visual view for {@link HistogramModel}s.
     */
    private final class HistogramComponent extends JPanel implements HiLiteListener, MouseMotionListener {
        private final HistogramModel<?> m_hd;

        private static final long serialVersionUID = 6356639541220896639L;

        private final HiLiteHandler m_hlHandler;

        private final Map<Integer, Set<RowKey>> m_rowKeys;

        private final boolean m_paintLabels;

        /**
         * @param hd A {@link HistogramModel}.
         */
        private HistogramComponent(final HistogramModel<?> hd, final HiLiteHandler hlHandler,
            final Map<Integer, Set<RowKey>> rowKeys, final boolean paintLabels) {
            this.m_hd = hd;
            this.m_hlHandler = hlHandler;
            m_hlHandler.addHiLiteListener(this);
            this.m_rowKeys = rowKeys;
            m_paintLabels = paintLabels;
            hiLite(new KeyEvent("", hlHandler.getHiLitKeys()));
            addMouseMotionListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void paint(final Graphics g) {
            super.paint(g);
            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D)g;
                HistogramColumn.this.paint(m_hd, g2);
                if (m_paintLabels) {
                    HistogramColumn.this.paintLabels(m_hd, g2, binWidth(m_hd));
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void hiLite(final KeyEvent event) {
            int delta = 1;
            update(event, delta);
        }

        /**
         * @param event
         * @param delta
         */
        private void update(final KeyEvent event, final int delta) {
            if (m_rowKeys == null) {
                return;
            }
            for (Entry<Integer, Set<RowKey>> entry : m_rowKeys.entrySet()) {
                Set<RowKey> set = entry.getValue();
                int bin = entry.getKey().intValue();
                for (RowKey key : event.keys()) {
                    if (set.contains(key)) {
                        int hiLited = m_hd.getBins().get(bin).getHiLited() + delta;
                        m_hd.getBins().get(bin).setHiLited(hiLited);
                    }
                }
            }
            repaint();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void unHiLite(final KeyEvent event) {
            update(event, -1);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void unHiLiteAll(final KeyEvent event) {
            for (HistogramModel.Bin<?> bin : m_hd.getBins()) {
                bin.setHiLited(0);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDragged(final MouseEvent e) {
            // Do nothing
        }

        /**
         * {@inheritDoc}
         */
        //        @Override
        @Override
        public void mouseMoved(final MouseEvent e) {
            int x2 = e.getX();
            int bin =
                Math.min((int)Math.floor(x2 / ((double)m_width / m_hd.getBins().size())), m_hd.getBins().size() - 1);
            if (bin >= 0 && bin < m_hd.getBins().size()) {
                HistogramModel.Bin<?> b = m_hd.getBins().get(bin);
                setToolTipText(b.getDef() + ", No.: " + b.getCount()
                    + (b.getHiLited() > 0 ? ", HiLited: " + b.getHiLited() : ""));
            }
        }
    }

    /**
     * The supported image formats.
     */
    public enum ImageFormats {
        /** SVG */
        SVG {
            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getDataType() {
                return SvgCell.TYPE;
            }
        },
        /** PNG */
        PNG {
            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getDataType() {
                return PNGImageContent.TYPE;
            }
        };

        /** @return the associated {@link DataType}. */
        public abstract DataType getDataType();
    }

    /** The strategy how to select the number of bins for the histogram. */
    public enum BinNumberSelectionStrategy {
        /** The number of bins as specified. */
        Specified,
        /**
         * The range should be divided as follows. First select the range, if it contains 0, sum of the bins for the
         * left and for the right of 0 with the same (higher) power of 10, else compute the range and the highest power
         * of 10 that is smaller than that, divide the range by that number and compute its ceiling.
         */
        DecimalRange;
    }

    /** Hide the default constructor, but allow extending. */
    protected HistogramColumn() {
        super();
    }

    /**
     * @return the default instance
     */
    public static HistogramColumn getDefaultInstance() {
        return INSTANCE;
    }

    private static final HistogramColumn INSTANCE = new HistogramColumn();

    private static final String HISTOGRAMS = "Histograms";

    private static final String NUMERIC_COLUMNS = "numeric column indices";

    private static final String HISTOGRAM = "histogram";

    private static final String MIN = "min";

    private static final String MAX = "max";

    private static final String WIDTH = "width";

    private static final String COL_INDEX = "column index";

    private static final String MAX_COUNT = "maximum count";

    private static final String ROW_COUNT = "row count";

    private static final String BIN_MINS = "bin minimums";

    private static final String BIN_MAXES = "bin maximums";

    private static final String BIN_COUNTS = "bin counts";

    private static final String COL_NAME = "column name";

    /**
     * A class to store the statistics on a numeric column.
     */
    static class HistogramNumericModel extends HistogramModel<Pair<Double, Double>> {
        private final NumberFormat m_format;

        private final BinNumberSelectionStrategy m_strategy;

        private class NumericBin extends Bin<Pair<Double, Double>> implements DisplayableBin {
            NumericBin(final double min, final double max) {
                this(Pair.create(min, max));
            }

            /**
             * @param minMax
             */
            NumericBin(final Pair<Double, Double> minMax) {
                super(minMax);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "Bin [min=" + getDef().getFirst() + ", max=" + getDef().getSecond() + ", count=" + getCount()
                    + " (" + getHiLited() + ")]";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getTooltip() {
                return "|[" + num(getDef().getFirst()) + "; " + num(getDef().getSecond()) + ">|= " + getCount()
                    + " (HiLited: " + getHiLited() + ")";
            }

            /**
             * @param num A number.
             * @return The {@link String} formatted to two digits.
             */
            private synchronized String num(final Double num) {
                return m_format.format(num);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String getText() {
                return num(getDef().getFirst());
            }
        }

        /**
         * Inits {@link HistogramNumericModel}.
         *
         * @param min The min value of the model.
         * @param max The max value of the model.
         * @param numOfBins The number of bins.
         * @param colIndex The column index.
         * @param colName The column name.
         */
        HistogramNumericModel(final double min, final double max, final int numOfBins, final int colIndex,
            final String colName, final double realMin, final double realMax, final double mean) {
            this(min, max, numOfBins, colIndex, colName, BinNumberSelectionStrategy.DecimalRange, realMin, realMax,
                mean);
        }

        HistogramNumericModel(final double min, final double max, final int numOfBins, final int colIndex,
            final String colName, final BinNumberSelectionStrategy strategy, final double realMin,
            final double realMax, final double mean) {
            super(numOfBins, colIndex, colName);
            m_strategy = strategy;
            assert max >= min : "min: " + min + ", max: " + max;
            m_min = min;
            m_max = max;
            m_realMin = realMin;
            m_realMax = realMax;
            m_mean = mean;
            m_width = (max - min) / numOfBins;
            double[] binStarts = new double[numOfBins];
            for (int i = 0; i < numOfBins; ++i) {
                binStarts[i] = min + i * m_width;
                getBins().add(new NumericBin(binStarts[i], min + (i + 1) * m_width));
            }
            m_format = new FormatDoubles().formatterForNumbers(binStarts);
        }

        ///min and max values in the column.
        private final double m_min, m_max, m_realMin, m_realMax;

        private final double m_mean;

        ///width of a bin (bin's max - bin's min)
        private final double m_width;

        @Override
        public int findBin(final DataValue v) {
            if (v instanceof DoubleValue) {
                DoubleValue dv = (DoubleValue)v;
                double d = dv.getDoubleValue();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return -1;
                }
                if (d < m_min || d > m_max) {
                    throw new IllegalArgumentException("Value out of range: " + v + " [" + m_min + ", " + m_max + "]");
                }
                int idx = (int)Math.floor((d - m_min) / m_width);
                int min = Math.min(idx, getBins().size() - 1);
                return min;
            }
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected HistogramModel<Pair<Double, Double>> createUninitializedClone() {
            MinMaxBinCount adjusted = MinMaxBinCount.adjust(m_min, m_max, m_strategy, getBins().size());
            return new HistogramNumericModel(adjusted.getMin(), adjusted.getMax(), getBins().size(), getColIndex(),
                getColName(), m_realMin, m_realMax, m_mean);
        }

        /**
         * @return the realMin
         */
        public double getRealMin() {
            return m_realMin;
        }

        /**
         * @return the realMax
         */
        public double getRealMax() {
            return m_realMax;
        }

        /**
         * @return the mean
         */
        public double getMean() {
            return m_mean;
        }
    }

    static class HistogramNominalModel extends HistogramModel<DataValue> {
        private final List<DataValue> m_values;

        private final Map<DataValue, Integer> m_index = new HashMap<DataValue, Integer>();

        protected HistogramNominalModel(final Map<? extends DataValue, Integer> values, final int colIndex,
            final String colName, final int rowCount) {
            this(values.keySet(), colIndex, colName);
            List<HistogramModel.Bin<DataValue>> bins = getBins();
            int maxCount = 0;
            for (Entry<? extends DataValue, Integer> entry : values.entrySet()) {
                int count = entry.getValue().intValue();
                bins.get(findBin(entry.getKey())).setCount(count);
                maxCount = Math.max(maxCount, count);
            }
            setMaxCount(maxCount);
            setRowCount(rowCount);
        }

        /**
         * @param values The values to store.
         * @param colIndex The column index.
         * @param colName The column name.
         */
        protected HistogramNominalModel(final Collection<? extends DataValue> values, final int colIndex,
            final String colName) {
            super(values.size(), colIndex, colName);
            this.m_values = new ArrayList<DataValue>(values);
            int i = 0;
            for (DataValue v : m_values) {
                getBins().add(new Bin<DataValue>(v));
                m_index.put(v, Integer.valueOf(i));
                ++i;
            }
        }

        /**
         * Call only after the index have been created by the constructor (
         * {@link #HistogramNominalModel(Collection, int, String)}). {@inheritDoc}
         */
        @Override
        public int findBin(final DataValue v) {
            Integer bin = m_index.get(v);
            if (bin == null) {
                return -1;
            }
            return bin;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected HistogramModel<DataValue> createUninitializedClone() {
            return new HistogramNominalModel(m_values, getColIndex(), getColName());
        }
    }

    static class MinMaxBinCount {
        private final double m_min, m_max;

        private final int m_binCount;

        /**
         * @param min
         * @param max
         * @param binCount
         */
        private MinMaxBinCount(final double min, final double max, final int binCount) {
            super();
            this.m_min = min;
            this.m_max = max;
            this.m_binCount = binCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("MinMaxBinCount [%s, %s] binCount=%s]", m_min, m_max, m_binCount);
        }

        double[] boundaries() {
            double[] ret = new double[m_binCount + 1];
            for (int i = 0; i < ret.length; ++i) {
                ret[i] = m_min + i * (m_max - m_min) / m_binCount;
            }
            return ret;
        }

        /**
         * @return the min
         */
        public double getMin() {
            return m_min;
        }

        /**
         * @return the max
         */
        public double getMax() {
            return m_max;
        }

        /**
         * @return the binCount
         */
        public int getBinCount() {
            return m_binCount;
        }

        static MinMaxBinCount adjust(final double min, final double max, final BinNumberSelectionStrategy strategy, final int defaultNumOfBins) {
            switch (strategy) {
                case Specified:
                    return new MinMaxBinCount(min, max, defaultNumOfBins);
                case DecimalRange:
                    BigDecimal bMin = new BigDecimal(Double.toString(min)),
                    bMax = new BigDecimal(Double.toString(max));
                    if (min == max) {
                        return new MinMaxBinCount(min, max, 1);
                    }
                    BigDecimal range =
                        bMax.subtract(bMin).round(new MathContext(2, RoundingMode.UP)).stripTrailingZeros();
                    int power = range.scale();
                    bMin = roundToNearest5Down(bMin, power + 1);
                    bMax = roundToNearest5Up(bMax, power + 1);
                    range = bMax.subtract(bMin)/*.stripTrailingZeros()*/;
                    if (range.scale() < 0) {
                        range = range.setScale(0);
                    }
                    BigDecimal width = BigDecimal.ONE.scaleByPowerOfTen(-power - 2);//Math.exp(power * Math.log(10));
                    int binCount = range.divide(width, RoundingMode.UP).intValue();//(int)Math.ceil(range / width);
                    while (hasLargePrimeDivisor(binCount)) {
                        BigDecimal diff = scaled5(power + 1);
                        if (min - bMin.doubleValue() < bMax.doubleValue() - max) {
                            bMin = bMin.subtract(diff);
                        } else {
                            bMax = bMax.add(diff);
                        }
                        range = range.add(diff);
                        if (range.scale() < 0) {
                            range = range.setScale(0);
                        }
                        binCount = range.divide(width, RoundingMode.UP).intValue();
                    }
                    int twoDivisors = twoDivisors(binCount),
                    fiveDivisors = fiveDivisors(binCount);
                    while (binCount > 40) {
                        if (fiveDivisors > 0) {
                            binCount /= 5;
                            --fiveDivisors;
                        }
                        if (twoDivisors > fiveDivisors) {
                            binCount /= 2;
                            --twoDivisors;
                        }
                    }
                    switch (binCount) {
                        case 40:
                            binCount = 10;
                            break;
                        case 38:
                            binCount = 19;
                            break;
                        case 36:
                            binCount = 9;
                            break;
                        case 35:
                            binCount = 7;
                        case 34:
                            binCount = 17;
                            break;
                        case 32:
                            binCount = 8;
                            break;
                        case 30:
                            binCount = 15;
                            break;
                        case 28:
                            binCount = 7;
                            break;
                        case 27:
                            //should not happen
                            binCount = 9;
                            break;
                        case 26:
                            binCount = 13;
                            break;
                        case 25:
                            binCount = 5;
                            break;
                        case 24:
                            binCount = 12;
                            break;
                        case 22:
                            binCount = 11;
                            break;
                        case 21:
                            binCount = 7;
                            break;
                        case 20:
                            binCount = 10;
                            break;
                        case 18:
                            binCount = 9;
                            break;
                        case 16:
                            binCount = 8;
                            break;
                        case 15:
                            binCount = 5;
                            break;
                        case 14:
                            binCount = 7;
                            break;
                        default:
                            break;
                    }
                    if (binCount == 40 || binCount == 20) {
                        binCount = 10;
                    }
                    //            int more = 1;
                    //            while (binCount <= 3) {
                    //                binCount =
                    //                    range.divide(java.math.BigDecimal.ONE.scaleByPowerOfTen(-power - (more++)),
                    //                        java.math.RoundingMode.UP).intValue();//(int)Math.ceil(range / Math.exp((power - 1) * Math.log(10)));
                    //            }
                    return new MinMaxBinCount(bMin.doubleValue(), bMax.doubleValue(), binCount);
                default:
                    throw new UnsupportedOperationException("Not supported: " + strategy);
            }
        }

        /**
         * @param binCount
         * @return
         */
        private static int twoDivisors(final int binCount) {
            return divisors(binCount, 2);
        }

        /**
         * @param binCount
         */
        private static int fiveDivisors(final int binCount) {
            return divisors(binCount, 5);
        }

        /**
         * @param binCount
         * @param div
         * @return
         */
        private static int divisors(final int binCount, final int div) {
            int num = binCount, ret = 0;
            while (num % div == 0) {
                num /= div;
                ++ret;
            }
            return ret;
        }

        /**
         * @param binCount
         * @return
         */
        private static boolean hasLargePrimeDivisor(final int binCount) {
            int num = binCount;
            if (binCount == 0) {
                throw new IllegalStateException("Bin count cannot be 0!");
            }
            int wrongDivisors = 1;
            for (int p : new int[]{2, 5}) {
                while (num % p == 0) {
                    num /= p;
                }
            }
            for (int p : new int[]{3, 7, 11, 13, 17, 19}) {
                while (num % p == 0) {
                    num /= p;
                    wrongDivisors *= p;
                }
            }
            return wrongDivisors > 22 || Math.abs(num) > 1;
        }
    }

    private int m_numOfBins = 10;

    private int m_width = 300, m_height = 100;

    private String m_colName = "Histogram";

    private Color m_lineColor = ColorAttr.BORDER, m_fillColor = new Color(0xaaccff), m_textColor = Color.BLACK;

    private ImageFormats m_format = ImageFormats.SVG;

    private BinNumberSelectionStrategy m_binSelectionStrategy = BinNumberSelectionStrategy.Specified;

    private boolean m_showMinMax = false;

    /**
     * @param numOfBins The new number of bins.
     * @return The instance with the {@code numOfBins} bins.
     */
    public HistogramColumn withNumberOfBins(final int numOfBins) {
        HistogramColumn ret = safeClone();
        ret.m_numOfBins = numOfBins;
        return ret;
    }

    /**
     * @param width Width of the histogram column.
     * @return The instance with the {@code width}.
     */
    public HistogramColumn withHistogramWidth(final int width) {
        final HistogramColumn ret = safeClone();
        ret.m_width = width;
        return ret;
    }

    /**
     * @param height Height of the histogram column.
     * @return The instance with the {@code height}.
     */
    public HistogramColumn withHistogramHeight(final int height) {
        final HistogramColumn ret = safeClone();
        ret.m_height = height;
        return ret;
    }

    /**
     * @param format The format of the histogram column.
     * @return The instance with {@code format}.
     */
    public HistogramColumn withImageFormat(final String format) {
        return withImageFormat(ImageFormats.valueOf(format));
    }

    /**
     * @param format The format of the histogram column.
     * @return The instance with {@code format}.
     */
    public HistogramColumn withImageFormat(final ImageFormats format) {
        final HistogramColumn ret = safeClone();
        ret.m_format = format;
        return ret;
    }

    /**
     * @param name The name of the histogram column.
     * @return The instance with {@code name} column name.
     */
    public HistogramColumn withName(final String name) {
        final HistogramColumn ret = safeClone();
        ret.m_colName = name;
        return ret;
    }

    /**
     * @param strategy The strategy to use to select the number of bins.
     * @return The instance with {@code strategy} for bin number selection.
     */
    public HistogramColumn withBinSelectionStrategy(final BinNumberSelectionStrategy strategy) {
        final HistogramColumn ret = safeClone();
        ret.m_binSelectionStrategy = strategy;
        return ret;
    }

    /**
     * @param showMinMax Show min/max/mean values on the x axis?
     * @return The instance with {@code showMinMax} for show min max property.
     */
    public HistogramColumn withShowMinMax(final boolean showMinMax) {
        final HistogramColumn ret = safeClone();
        ret.m_showMinMax = showMinMax;
        return ret;
    }

    /**
     * @return private clone of this.
     * @throws IllegalStateException when clone is not supported. Should not happen.
     */
    private HistogramColumn safeClone() {
        try {
            return (HistogramColumn)this.clone();
        } catch (CloneNotSupportedException e) {
            //Should not happen.
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Constructs the helper data structures from the saved/computed data.
     *
     * @param histograms The numeric histogram models.
     * @param data The data containing the row keys.
     * @param nominalColumnNames The nominal column names.
     * @return {@link Pair} of map from col index to bin index to row keys and another map from col index to data values
     *         to row keys.
     */
    public static Pair<Map<Integer, Map<Integer, Set<RowKey>>>, Map<Integer, Map<DataValue, Set<RowKey>>>> construct(
        final Map<Integer, ?> histograms, final DataTable data, final Set<String> nominalColumnNames) {
        for (Entry<Integer, ?> entry : histograms.entrySet()) {
            if (!(entry.getValue() instanceof HistogramNumericModel)) {
                throw new IllegalStateException("Not a histogram data: " + entry.getValue().getClass() + "\n" + entry);
            }
        }
        @SuppressWarnings("unchecked")
        Map<Integer, HistogramNumericModel> casted = (Map<Integer, HistogramNumericModel>)histograms;
        return constructFromDataArray(casted, data, nominalColumnNames);
    }

    /**
     * Constructs the helper data structures from the numeric hostigran models and the data as {@link DataArray}.
     *
     * @param histograms The numeric histograms.
     * @param data The input data.
     * @param nominalColumnNames The nominal column names.
     * @return The helper data structures.
     * @see #construct(Map, DataTable, Set)
     */
    protected static Pair<Map<Integer, Map<Integer, Set<RowKey>>>, Map<Integer, Map<DataValue, Set<RowKey>>>>
        constructFromDataArray(final Map<Integer, HistogramNumericModel> histograms, final DataTable data,
            final Set<String> nominalColumnNames) {
        Map<Integer, Map<Integer, Set<RowKey>>> numericMapping = new HashMap<Integer, Map<Integer, Set<RowKey>>>();
        Map<Integer, Map<DataValue, Set<RowKey>>> nominalMapping = new HashMap<Integer, Map<DataValue, Set<RowKey>>>();
        DataTableSpec tableSpec = data.getDataTableSpec();
        for (DataColumnSpec colSpec : tableSpec) {
            int colIndex = tableSpec.findColumnIndex(colSpec.getName());
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                //                assert histograms.containsKey(Integer.valueOf(colIndex)) && histograms.get(colIndex) != null : "ColIndex: "
                //                    + colIndex;
                if (histograms.containsKey(Integer.valueOf(colIndex)) && histograms.get(colIndex) != null) {
                    numericMapping.put(colIndex, new HashMap<Integer, Set<RowKey>>());
                }
            }
            if (colSpec.getDomain().hasValues() || nominalColumnNames.contains(colSpec.getName())) {
                nominalMapping.put(colIndex, new HashMap<DataValue, Set<RowKey>>());
            }
        }

        for (DataRow dataRow : data) {
            for (Entry<Integer, Map<Integer, Set<RowKey>>> outer : numericMapping.entrySet()) {
                Integer key = outer.getKey();
                DataCell cell = dataRow.getCell(key);
                if (cell instanceof DoubleValue) {
                    DoubleValue dv = (DoubleValue)cell;
                    Integer bin = Integer.valueOf(histograms.get(key).findBin(dv));
                    Map<Integer, Set<RowKey>> inner = outer.getValue();
                    if (!inner.containsKey(bin)) {
                        inner.put(bin, new HashSet<RowKey>());
                    }
                    inner.get(bin).add(dataRow.getKey());
                }
            }
            for (Entry<Integer, Map<DataValue, Set<RowKey>>> outer : nominalMapping.entrySet()) {
                int key = outer.getKey().intValue();
                DataCell cell = dataRow.getCell(key);
                if (!cell.isMissing()/* && cell instanceof NominalValue*/) {
                    Map<DataValue, Set<RowKey>> inner = outer.getValue();
                    if (!inner.containsKey(cell)) {
                        inner.put(cell, new HashSet<RowKey>());
                    }
                    inner.get(cell).add(dataRow.getKey());
                }
            }
        }
        return Pair.create(numericMapping, nominalMapping);
    }

    /**
     * Updates the HiLite information within the {@link HistogramNumericModel} buckets.
     *
     * @param input The input {@link HistogramNumericModel}.
     * @param buckets The buckets belonging to the column of {@code input}.
     * @param hlHandler The {@link HiLiteHandler}.
     * @return The updated {@link HistogramNumericModel}.
     */
    HistogramNumericModel updateHiLiteInfo(final HistogramNumericModel input, final Map<Integer, Set<RowKey>> buckets,
        final HiLiteHandler hlHandler) {
        //        HistogramData ret = new HistogramData(input.m_min, input.m_max, input.m_bins.length, input.m_colIndex);
        //        ret.m_maxCount = input.m_maxCount;
        HistogramNumericModel ret = (HistogramNumericModel)input.clone();
        for (int idx = ret.getBins().size(); idx-- > 0;) {
            //ret.m_bins[idx].m_count = input.m_bins[idx].m_count;
            Integer keyIdx = Integer.valueOf(idx);
            if (buckets.containsKey(keyIdx)) {
                Set<RowKey> keys = buckets.get(keyIdx);
                int hiLited = 0;
                for (RowKey rowKey : keys) {
                    if (hlHandler.isHiLit(rowKey)) {
                        hiLited++;
                    }
                }
                ret.getBins().get(idx).setHiLited(hiLited);
            }
        }
        return ret;
    }

    /**
     * Adds the histogram to the {@code stats} table based on the input parameters.
     *
     * @param exec The {@link ExecutionContext}.
     * @param data The original table.
     * @param hlHandler The HiLite handler.
     * @param stats The table to add the column.
     * @param mins The minimal values for {@code columns}.
     * @param maxs The maximal values for {@code columns}.
     * @param means The mean values for {@code columns}.
     * @param maxBins The maximum number of bins till we draw labels.
     * @param columns The columns to summarize as a histogram.
     * @return The histogram added to the input {@code data} table and the {@link HistogramModel}s.
     * @throws CanceledExecutionException Cancelled.
     */
    public Pair<BufferedDataTable, Map<Integer, ? extends HistogramModel<?>>> process(final ExecutionContext exec,
        final BufferedDataTable data, final HiLiteHandler hlHandler, final BufferedDataTable stats,
        final double[] mins, final double[] maxs, final double[] means, final int maxBins, final String... columns)
        throws CanceledExecutionException {
        exec.setMessage("Collecting histogram data");
        final Map<Integer, HistogramNumericModel> histograms =
            histogramsPrivate(data, hlHandler, mins, maxs, means, columns);
        exec.setMessage("Generating histogram");
        ColumnRearranger rearranger = createColumnRearranger(data, stats, histograms, maxBins, columns);
        return Pair.<BufferedDataTable, Map<Integer, ? extends HistogramModel<?>>> create(
            exec.createColumnRearrangeTable(stats, rearranger, exec), histograms);

    }

    /**
     * Creates the rearranger that adds the histograms.
     *
     * @param data The input data table that contains the columns referred by {@code histograms} keys.
     * @param stats The statistics table to be adjusted.
     * @param histograms The histograms.
     * @param columns The columns to be described.
     * @return The {@link ColumnRearranger}.
     */
    ColumnRearranger createColumnRearranger(final BufferedDataTable data, final BufferedDataTable stats,
        final Map<Integer, HistogramNumericModel> histograms, final int maxBinCount, final String... columns) {
        ColumnRearranger rearranger = new ColumnRearranger(stats.getDataTableSpec());
        final DataColumnSpec spec = createHistogramColumnSpec();
        rearranger.append(new SingleCellFactory(true, spec) {
            String[] m_sortedColumns = columns.clone();
            {
                Arrays.sort(m_sortedColumns);
            }

            @Override
            public DataCell getCell(final DataRow row) {
                if (Arrays.binarySearch(m_sortedColumns, row.getKey().getString()) < 0) {
                    return DataType.getMissingCell();
                }
                final int columnIndex = data.getSpec().findColumnIndex(row.getKey().getString());
                final HistogramNumericModel histogramData = histograms.get(Integer.valueOf(columnIndex));
                if (histogramData == null) {
                    //Wrong bounds
                    return DataType.getMissingCell();
                }
                assert columnIndex == histogramData.getColIndex() : "Expected: " + columnIndex + ", but got: "
                    + histogramData.getColIndex();

                return createImageCell(histogramData, false);
            }

        });
        return rearranger;
    }

    /**
     * Paints the {@code histogramData} to {@code g}.
     *
     * @param histogramData A {@link HistogramNumericModel}.
     * @param g A {@link Graphics2D} object.
     */
    private void paint(final HistogramModel<?> histogramData, final Graphics2D g) {
        double binWidth = binWidth(histogramData);
        int height = m_showMinMax && histogramData instanceof HistogramNumericModel ? m_height - MIN_MAX_AREA_HEIGHT : m_height;
        //g.setFont(Font.getFont("Arial").deriveFont((float)(binWidth * .8d)));
        g.setColor(m_fillColor);
        if (histogramData.getBins().isEmpty()) {
            g.setColor(new Color(0, 0, 0, 0));
            g.drawRect(0, 0, m_width, m_height);
        }
        for (int i = histogramData.getBins().size(); i-- > 0;) {
            HistogramModel.Bin<?> bin = histogramData.getBins().get(i);
            int upperPosition = upperPosition(histogramData, bin);
            g.fillRect((int)(i * binWidth), upperPosition, (int)Math.ceil(binWidth), height - upperPosition);
        }
        g.setColor(m_lineColor);
        g.drawLine(0, height, m_width, height);
        g.setStroke(new BasicStroke(1.5f));
        for (int i = histogramData.getBins().size(); i-- > 0;) {
            HistogramModel.Bin<?> bin = histogramData.getBins().get(i);
            int startX = (int)(i * binWidth), nextX = (int)((i + 1) * binWidth), upper =
                upperPosition(histogramData, bin);
            drawCap(g, startX, nextX, upper, height);
        }
        g.setColor(ColorAttr.HILITE);
        for (int i = histogramData.getBins().size(); i-- > 0;) {
            Bin<?> bin = histogramData.getBins().get(i);
            if (bin.getHiLited() > 0) {
                int upper = upperPosition(histogramData.getReferenceNumber(), bin.getHiLited());
                g.fillRect((int)(i * binWidth), upper, (int)Math.ceil(binWidth), height - upper);
            }
        }
        //g.setColor(ColorAttr.SELECTED_HILITE);
        g.setColor(m_lineColor);
        for (int i = histogramData.getBins().size(); i-- > 0;) {
            HistogramModel.Bin<?> bin = histogramData.getBins().get(i);
            int startX = (int)(i * binWidth), nextX = (int)((i + 1) * binWidth);
            if (bin.getHiLited() > 0) {
                int upper = upperPosition(histogramData.getReferenceNumber(), bin.getHiLited());
                drawCap(g, startX, nextX, upper, height);
            }
        }
        if (m_showMinMax) {
            paintMinMax(histogramData, g, height);
        }
    }

    /**
     * @param histogramData
     * @param g
     * @param height
     */
    protected void paintMinMax(final HistogramModel<?> histogramData, final Graphics2D g, final int height) {
        g.setColor(m_textColor);

        if (histogramData instanceof HistogramNumericModel) {
            HistogramNumericModel hnm = (HistogramNumericModel)histogramData;

            double realMin = hnm.getRealMin(), realMax = hnm.getRealMax();
            NumberFormat formatter = new FormatDoubles().formatterWithSamePrecisio(realMin, realMax);
            String min = formatter.format(realMin);
            String max = formatter.format(realMax);
            FontMetrics fm = g.getFontMetrics();
            Rectangle2D minBounds = fm.getStringBounds(min, g);
            Rectangle2D maxBounds = fm.getStringBounds(max, g);
            List<Bin<Pair<Double, Double>>> bins = hnm.getBins();
            double visibleMin = bins.get(0).getDef().getFirst().doubleValue(), visibleMax = bins.get(bins.size() - 1).getDef().getSecond().doubleValue();
            double visibleRange = visibleMax - visibleMin;
            int minPos = (int)((realMin - visibleMin)/ visibleRange * m_width), maxPos = (int)((realMax - visibleMin) / visibleRange * m_width);
            if (minPos + minBounds.getWidth() + maxBounds.getWidth() + 2 < maxPos) {
                for (int pos : new int[] {minPos, maxPos}) {
                    g.drawLine(pos, height, pos, height + MIN_MAX_AREA_HEIGHT / 3);
                }
                g.drawString(min, minPos + 1, m_height);
                g.drawString(max, (int)(maxPos - maxBounds.getWidth() - 1), m_height);
            } else if (min.equals(max)) {
                g.drawString(min, (m_width = (int)maxBounds.getWidth()) / 2, m_height);
                g.drawLine(m_width / 2, height, m_width / 2, height + MIN_MAX_AREA_HEIGHT / 3);
            }
        }
    }

    /**
     * @param histogramData
     * @return
     */
    double binWidth(final HistogramModel<?> histogramData) {
        return m_width / (double)(histogramData.getBins().size());
    }

    /**
     * Paints the labels on the canvas ({@link Graphics2D}). The too (>25) long labels will be truncated.
     *
     * @param histogramData The {@link HistogramModel} to paint labels.
     * @param g A {@link Graphics2D} object.
     * @param binWidth The width of the bins on the screen.
     */
    void paintLabels(final HistogramModel<?> histogramData, final Graphics2D g, final double binWidth) {
        g.setColor(m_textColor);
        g.translate(m_width, 0);
        g.rotate(Math.PI / 2);
        int height =
            m_showMinMax && histogramData instanceof HistogramNumericModel ? m_height - MIN_MAX_AREA_HEIGHT : m_height;
        for (int i = histogramData.getBins().size(); i-- > 0;) {
            HistogramModel.Bin<?> bin = histogramData.getBins().get(i);
            int startX = (int)(i * binWidth);
            String label = bin.getDef().toString();
            if (label.length() > 25) {
                label = label.substring(0, 11) + "\u2026" + label.substring(label.length() - 11);
            }
            if (bin instanceof DisplayableBin) {
                DisplayableBin display = (DisplayableBin)bin;
                label = display.getText();
            }
            Rectangle2D bounds = g.getFontMetrics().getStringBounds(label, g);
            g.drawString(label, height - 1 - (int)bounds.getWidth(), (int)(m_width - startX - binWidth / 8));
        }
    }

    /**
     * Draws a horizontal line with two vertical till the bottom.
     *
     * @param g The canvas to draw.
     * @param startX The x position of the left line.
     * @param nextX The x position of the right line.
     * @param upper The y position of the top line.
     * @param height The y position of the bottom area.
     */
    private void drawCap(final Graphics2D g, final int startX, final int nextX, final int upper, final int height) {
        g.drawLine(startX, upper, startX, height);
        g.drawLine(startX, upper, nextX, upper);
        g.drawLine(nextX, upper, nextX, height);
    }

    /**
     * @param histogramData A {@link HistogramNumericModel}.
     * @param bin A bin of {@code histogramData}.
     * @return The position vertically of the top of the bin.
     */
    private int upperPosition(final HistogramModel<?> histogramData, final HistogramModel.Bin<?> bin) {
        int reference = histogramData.getReferenceNumber();
        int count = bin.getCount();
        return upperPosition(reference, count);
    }

    /**
     * @param referenceCount The max possible count in the histogram.
     * @param count The actual count for the bin or HiLite.
     * @return The position vertically of the top of the bin.
     */
    private int upperPosition(final int referenceCount, final int count) {
        int height = m_showMinMax ? m_height - MIN_MAX_AREA_HEIGHT : m_height;
        return (int)((referenceCount - count) * height / (double)referenceCount);
    }

    /**
     * Computes the histograms for the selected numeric columns.
     *
     * @param data The input data.
     * @param hlHandler The {@link HiLiteHandler}.
     * @param mins The minimum values. (Preferably non-infinite.)
     * @param maxs The maximum values. (Preferably non-infinite.)
     * @param means The mean values. (Preferably non-NaN.)
     * @param columns The name of the columns.
     * @return The numeric {@link HistogramModel} for {@code columns} from {@code data} (where the keys are the array
     *         indices from {@code columns}).
     */
    public Map<Integer, ? extends HistogramModel<?>> histograms(final DataTable data, final HiLiteHandler hlHandler,
        final double[] mins, final double[] maxs, final double[] means, final String... columns) {
        return histogramsPrivate(data, hlHandler, mins, maxs, means, columns);
    }

    private Map<Integer, HistogramNumericModel> histogramsPrivate(final DataTable data, final HiLiteHandler hlHandler,
        final double[] mins, final double[] maxs, final double[] means, final String... columns) {
        final Map<Integer, HistogramNumericModel> histograms = new HashMap<Integer, HistogramNumericModel>();
        for (int columnIndex = 0; columnIndex < columns.length; ++columnIndex) {
            String column = columns[columnIndex];
            assert column != null : Arrays.toString(columns);
            DataColumnSpec colSpec = data.getDataTableSpec().getColumnSpec(column);

            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                double min = mins[columnIndex];
                double max = maxs[columnIndex];
                if (Double.isInfinite(min) || Double.isInfinite(max) || Double.isNaN(min) || Double.isNaN(max)) {
                    continue;
                }
                double mean = means[columnIndex];
                int numOfBins = addHistogramNumericModel(histograms, columnIndex, colSpec, min, max, mean);
                NodeLogger.getLogger(HistogramColumn.class).debug(
                    "Number of bins: " + numOfBins + " for " + colSpec + " [" + mins[columnIndex] + ", "
                        + maxs[columnIndex] + "]");
            }
        }
        final Set<RowKey> hiLitKeys = hlHandler.getHiLitKeys();
        String[] columnNames = data.getDataTableSpec().getColumnNames();

        for (DataRow dataRow : data) {
            final boolean isHiLited = hiLitKeys.contains(dataRow.getKey());
            for (Entry<Integer, HistogramNumericModel> entry : histograms.entrySet()) {
                String colName = entry.getValue().getColName();
                int columnIndexInTable =  Arrays.asList(columnNames).indexOf(colName);
                //DataCell cell = dataRow.getCell(entry.getKey().intValue());
                DataCell cell = dataRow.getCell(columnIndexInTable);
                if (!cell.isMissing()) {
                    final double d = ((DoubleValue)cell).getDoubleValue();
                    if (Double.isNaN(d) || Double.isInfinite(d)) {
                        continue;
                    }
                    final HistogramNumericModel hd = entry.getValue();
                    hd.addValue(cell, isHiLited);
                }
            }
        }
        return histograms;
    }

    /**
     * @param histograms The {@link Map} of histograms.
     * @param columnIndex The column index.
     * @param colSpec The {@link DataColumnSpec}.
     * @param minOrig The original min value.
     * @param maxOrig The original max value.
     * @param mean The mean value.
     * @return The bin count of the model.
     */
    protected int addHistogramNumericModel(final Map<Integer, HistogramNumericModel> histograms, final int columnIndex,
        final DataColumnSpec colSpec, final double minOrig, final double maxOrig, final double mean) {
        MinMaxBinCount adjusted = MinMaxBinCount.adjust(minOrig, maxOrig, m_binSelectionStrategy, m_numOfBins);
        histograms.put(Integer.valueOf(columnIndex), new HistogramNumericModel(adjusted.getMin(), adjusted.getMax(),
            adjusted.getBinCount(), columnIndex, colSpec.getName(), minOrig, maxOrig, mean));
        return adjusted.getBinCount();
    }

    /**
     * @param num A number.
     * @param scale The scale of the divisor.
     * @return The nearest number close to {@code num}, still smaller with {@link #scaled5(int)} on {@code scale}.
     */
    private static BigDecimal roundToNearest5Down(final BigDecimal num, final int scale) {
        BigDecimal rounded = num.setScale(scale, RoundingMode.FLOOR);
        BigDecimal remainder = positiveRemainder(rounded, scale);
        return rounded.subtract(remainder);
    }

    /**
     * @param num A number.
     * @param scale The scale of the divisor.
     * @return The nearest number close to {@code num}, still larger with {@link #scaled5(int)} on {@code scale}.
     */
    private static BigDecimal roundToNearest5Up(final BigDecimal num, final int scale) {
        BigDecimal rounded = num.setScale(scale, RoundingMode.CEILING);
        BigDecimal remainder = positiveRemainder(rounded, scale);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal scaled = scaled5(scale);
            return rounded.add(scaled.subtract(remainder));
        }
        return rounded;
    }

    /**
     * @param num A number.
     * @param scale The scale of the divisor.
     * @return The positive remainder of the number divided by {@link #scaled5(int)} on {@code scale}.
     */
    protected static BigDecimal positiveRemainder(final BigDecimal num, final int scale) {
        BigDecimal scaled = scaled5(scale);
        BigDecimal remainder = num.remainder(scaled);
        if (remainder.compareTo(BigDecimal.ZERO) < 0) {
            remainder = remainder.add(scaled);
        }
        return remainder;
    }

    /**
     * @param scale A scale of the multiplication.
     * @return {@code 5} scaled by power of {@code 10} to {@code -scale}.
     */
    protected static BigDecimal scaled5(final int scale) {
        return FIVE.scaleByPowerOfTen(-scale);
    }

    /**
     * @return The {@link DataColumnSpec} for the column to be created.
     */
    public DataColumnSpec createHistogramColumnSpec() {
        final DataColumnSpecCreator columnSpecCreator = new DataColumnSpecCreator(m_colName, m_format.getDataType());
        if (m_format == ImageFormats.SVG) {
            final Map<String, String> widthAndHeight = new LinkedHashMap<String, String>();
            widthAndHeight.put(SvgValueRenderer.OPTION_KEEP_ASPECT_RATIO, Boolean.toString(true));
            widthAndHeight.put(SvgValueRenderer.OPTION_PREFERRED_WIDTH, Integer.toString(m_width));
            widthAndHeight.put(SvgValueRenderer.OPTION_PREFERRED_HEIGHT, Integer.toString(m_height));
            final DataColumnProperties props = new DataColumnProperties(widthAndHeight);
            columnSpecCreator.setProperties(props);
        }
        DataColumnSpec histogramColumnSpec = columnSpecCreator.createSpec();
        return histogramColumnSpec;
    }

    /**
     * Loads the histograms from the saved internal files.
     *
     * @param histogramsGz The file for the histograms.
     * @param dataArrayGz The data array file for the row keys.
     * @param nominalColumns The nominal columns.
     * @param strategy The strategy used to compute the bins.
     * @param means The mean values for the numeric columns.
     * @return A triple (Pair(Pair(,),)) of histograms, numeric and nominal row keys.
     * @throws IOException Failed to read the files.
     * @throws InvalidSettingsException Something went wrong.
     */
    public static
        Pair<Pair<Map<Integer, ? extends HistogramModel<?>>, Map<Integer, Map<Integer, Set<RowKey>>>>, Map<Integer, Map<DataValue, Set<RowKey>>>>
        loadHistograms(final File histogramsGz, final File dataArrayGz, final Set<String> nominalColumns,
            final BinNumberSelectionStrategy strategy, final double[] means) throws IOException,
            InvalidSettingsException {
        Map<Integer, Map<Integer, Set<RowKey>>> numericKeys = new HashMap<Integer, Map<Integer, Set<RowKey>>>();
        Map<Integer, HistogramNumericModel> histograms =
            loadHistogramsPrivate(histogramsGz, numericKeys, strategy, means);
        Map<Integer, Map<DataValue, Set<RowKey>>> nominalKeys = new HashMap<Integer, Map<DataValue, Set<RowKey>>>();
        ContainerTable table = DataContainer.readFromZip(dataArrayGz);
        Set<Integer> numericColIndices = numericKeys.keySet();
        for (String colName : nominalColumns) {
            int colIndex = table.getDataTableSpec().findColumnIndex(colName);
            if (colIndex < 0) {
                continue;
            }
            nominalKeys.put(Integer.valueOf(colIndex), new HashMap<DataValue, Set<RowKey>>());
        }
        for (DataRow dataRow : table) {
            for (Integer col : numericColIndices) {
                //Integer col = Integer.valueOf(colIdx);
                HistogramNumericModel hd = histograms.get(col);
                Map<Integer, Set<RowKey>> map = numericKeys.get(col);
                DataCell cell = dataRow.getCell(col.intValue());
                if (!cell.isMissing() && cell instanceof DoubleValue) {
                    DoubleValue dv = (DoubleValue)cell;
                    Integer bin = Integer.valueOf(hd.findBin(dv));
                    if (!map.containsKey(bin)) {
                        map.put(bin, new HashSet<RowKey>());
                    }
                    map.get(bin).add(dataRow.getKey());
                }
            }
            for (Entry<Integer, Map<DataValue, Set<RowKey>>> entry : nominalKeys.entrySet()) {
                DataCell value = dataRow.getCell(entry.getKey().intValue());
                Map<DataValue, Set<RowKey>> map = entry.getValue();
                if (!map.containsKey(value)) {
                    map.put(value, new HashSet<RowKey>());
                }
                map.get(value).add(dataRow.getKey());
            }
        }
        return Pair.create(
            new Pair<Map<Integer, ? extends HistogramModel<?>>, Map<Integer, Map<Integer, Set<RowKey>>>>(histograms,
                numericKeys), nominalKeys);
    }

    /**
     * Loads the histograms and fills the content of {@code numericKeys}.
     *
     * @param histogramsGz The file for the histograms.
     * @param numericKeys The keys map to fill.
     * @param strategy The strategy used to compute the bins.
     * @param means The mean values for the numeric columns.
     * @return The {@link Map} from the column indices to the numeric {@link HistogramModel}s.
     * @throws IOException Problem reading the file.
     * @throws InvalidSettingsException Something went wrong.
     */
    public static Map<Integer, ? extends HistogramModel<?>> loadHistograms(final File histogramsGz,
        final Map<Integer, Map<Integer, Set<RowKey>>> numericKeys, final BinNumberSelectionStrategy strategy,
        final double[] means) throws IOException, InvalidSettingsException {
        return loadHistogramsPrivate(histogramsGz, numericKeys, strategy, means);
    }

    private static Map<Integer, HistogramNumericModel> loadHistogramsPrivate(final File histogramsGz,
        final Map<Integer, Map<Integer, Set<RowKey>>> numericKeys, final BinNumberSelectionStrategy strategy,
        final double[] means) throws IOException, InvalidSettingsException {
        final FileInputStream is = new FileInputStream(histogramsGz);
        final GZIPInputStream inData = new GZIPInputStream(is);
        final ConfigRO config = NodeSettings.loadFromXML(inData);
        Map<Integer, HistogramNumericModel> histograms = new HashMap<Integer, HistogramNumericModel>();
        ConfigRO hs = config;//.getConfig(HISTOGRAMS);
        int[] numColumnIndices = config.getIntArray(NUMERIC_COLUMNS);
        for (int colIdx : numColumnIndices) {
            Config h = hs.getConfig(HISTOGRAM + colIdx);
            double min = h.getDouble(MIN), max = h.getDouble(MAX), width = h.getDouble(WIDTH);
            int maxCount = h.getInt(MAX_COUNT);
            int rowCount = h.getInt(ROW_COUNT);
            String colName = h.getString(COL_NAME);
            double[] binMins = h.getDoubleArray(BIN_MINS), binMaxes = h.getDoubleArray(BIN_MAXES);
            int[] binCounts = h.getIntArray(BIN_COUNTS);
            double mean = means[colIdx];
            HistogramNumericModel histogramData =
                new HistogramNumericModel(min, max, binMins.length, colIdx, colName, min, max, mean);
            for (int i = binMins.length; i-- > 0;) {
                histogramData.getBins().set(i, histogramData.new NumericBin(binMins[i], binMaxes[i]));
                histogramData.getBins().get(i).setCount(binCounts[i]);
            }
            histogramData.setMaxCount(maxCount);
            histogramData.setRowCount(rowCount);
            assert Math.abs(histogramData.m_width - width) < 1e-9: "histogram data width: " + histogramData.m_width + " width: " + width;
            histograms.put(colIdx, histogramData);
            numericKeys.put(colIdx, new HashMap<Integer, Set<RowKey>>());
        }
        return histograms;
    }

    /**
     * Save the histograms to an internal file.
     *
     * @param histograms The column index to histograms {@link Map}.
     * @param buckets The {@link Map} for the numeric row keys.
     * @param nominalKeys The {@link Map} for the nominal row keys.
     * @param histogramsFile The destination file.
     * @param subTable The {@link DataArray} to save.
     * @param dataArrayFile The file to save the {@code subTable}.
     * @param exec An {@link ExecutionMonitor}.
     * @throws IOException Something went wrong during save.
     * @throws CanceledExecutionException Cancelled execution.
     */
    public static void saveHistograms(final Map<Integer, ?> histograms,
        final Map<Integer, Map<Integer, Set<RowKey>>> buckets,
        final Map<Integer, Map<DataValue, Set<RowKey>>> nominalKeys, final File histogramsFile,
        final DataArray subTable, final File dataArrayFile, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        assert histograms.keySet().equals(buckets.keySet()) : histograms.keySet() + "\n" + buckets.keySet();
        saveHistogramData(histograms, histogramsFile);
        exec.checkCanceled();
        saveRowKeys(buckets, nominalKeys/*, nominalTypes*/, subTable, dataArrayFile, exec);
    }

    /**
     * Saves the row keys to a file ({@code dataArrayFile}).
     *
     * @param buckets The numeric row keys.
     * @param nominalKeys The nominal row keys.
     * @param dataArrayFile The data array file.
     * @param exec The {@link ExecutionMonitor}.
     * @throws IOException Something went wrong.
     * @throws CanceledExecutionException Cancelled execution.
     */
    private static void saveRowKeys(final Map<Integer, Map<Integer, Set<RowKey>>> buckets,
        final Map<Integer, Map<DataValue, Set<RowKey>>> nominalKeys/*, final Map<Integer, DataType> nominalTypes*/,
        final DataArray subTable, final File dataArrayFile, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        ///The columns for the bin indices and the nominal values for the keys.
        DataColumnSpec[] colSpecs = new DataColumnSpec[buckets.size() + nominalKeys.size()];
        List<Integer> numColIndices = new ArrayList<Integer>(buckets.keySet()), nomColIndices =
            new ArrayList<Integer>(nominalKeys.keySet());
        Collections.sort(numColIndices);
        Collections.sort(nomColIndices);
        int col = 0;
        for (; col < numColIndices.size(); ++col) {
            colSpecs[col] = new DataColumnSpecCreator(Integer.toString(col), IntCell.TYPE).createSpec();
        }
        DataContainer.writeToZip(subTable, dataArrayFile, exec);
    }

    /**
     * Saves the numeric histogram data to a file.
     *
     * @param histograms The numeric histogram models associated to the column indices.
     * @param histogramsFile The output file.
     * @throws IOException File write problem.
     */
    public static void saveHistogramData(final Map<Integer, ?> histograms, final File histogramsFile)
        throws IOException {
        Config histogramData = new NodeSettings(HISTOGRAMS);
        final FileOutputStream os = new FileOutputStream(histogramsFile);
        final GZIPOutputStream dataOS = new GZIPOutputStream(os);

        List<Integer> colIndices = new ArrayList<Integer>(histograms.keySet());
        Collections.sort(colIndices);
        int[] numericColumnIndices = new int[colIndices.size()];
        for (int i = colIndices.size(); i-- > 0;) {
            numericColumnIndices[i] = colIndices.get(i).intValue();
        }
        histogramData.addIntArray(NUMERIC_COLUMNS, numericColumnIndices);
        for (Integer colIdx : colIndices) {
            Object object = histograms.get(colIdx);
            if (object instanceof HistogramNumericModel) {
                HistogramNumericModel hd = (HistogramNumericModel)object;
                assert hd.getColIndex() == colIdx.intValue() : "colIdx: " + colIdx + ", but: " + hd.getColIndex();
                Config h = histogramData.addConfig(HISTOGRAM + colIdx);
                h.addDouble(MIN, hd.m_min);
                h.addDouble(MAX, hd.m_max);
                h.addDouble(WIDTH, hd.m_width);
                h.addInt(MAX_COUNT, hd.getMaxCount());
                h.addInt(ROW_COUNT, hd.getRowCount());
                h.addInt(COL_INDEX, hd.getColIndex());
                h.addString(COL_NAME, hd.getColName());
                double[] minValues = new double[hd.getBins().size()], maxValues = new double[hd.getBins().size()];
                int[] counts = new int[hd.getBins().size()];
                for (int c = 0; c < hd.getBins().size(); c++) {
                    HistogramNumericModel.NumericBin bin = (HistogramNumericModel.NumericBin)hd.getBins().get(c);
                    minValues[c] = bin.getDef().getFirst().doubleValue();
                    maxValues[c] = bin.getDef().getSecond().doubleValue();
                    counts[c] = bin.getCount();
                }
                h.addDoubleArray(BIN_MINS, minValues);
                h.addDoubleArray(BIN_MAXES, maxValues);
                h.addIntArray(BIN_COUNTS, counts);
            } else {
                throw new IllegalStateException("Illegal argument: " + colIdx + ": " + object.getClass() + "\n   "
                    + object);
            }
        }
        histogramData.saveToXML(dataOS);
    }

    /**
     * Constructs the visual representation of the {@code histogramDescription} to show the histogram.
     *
     * @param histogramDescription Data representation of the histogram.
     * @param width Width of the component.
     * @param height Height of the component.
     * @param hiLiteHandler The {@link HiLiteHandler} for the input data.
     * @param rowKeys The keys belonging to the bins.
     * @param maxBinSize The maximum number of bins till we draw labels.
     * @return The histogram of the column.
     */
    public JComponent createComponent(final Object histogramDescription, final int width, final int height,
        final HiLiteHandler hiLiteHandler, final Map<Integer, Set<RowKey>> rowKeys, final int maxBinSize) {
        JPanel ret;
        if (histogramDescription instanceof HistogramModel) {
            final HistogramModel<?> hd = (HistogramModel<?>)histogramDescription;
            ret = new HistogramComponent(hd, hiLiteHandler, rowKeys, false);
        } else {
            ret = new JPanel();
        }
        ret.setPreferredSize(new Dimension(width, height));
        return ret;
    }

    /**
     * Append the nominal rows to the histogram table.
     *
     * @param numericWithHistograms The output from the
     *            {@link Statistics3Table#createStatisticsInColumnsTable(ExecutionContext)}.
     * @param statTable The {@link Statistics3Table}.
     * @param hlHandler The {@link HiLiteHandler} to HiLite parts.
     * @param exec An {@link ExecutionContext}.
     * @param maxBinCount The maximum number of bins till we draw labels.
     * @return The {@link BufferedDataTable} with new rows for selected nominal columns.
     * @throws CanceledExecutionException Execution cancelled.
     */
    @Deprecated
    public BufferedDataTable appendNominal(final BufferedDataTable numericWithHistograms,
        final Statistics3Table statTable, final HiLiteHandler hlHandler, final ExecutionContext exec,
        final int maxBinCount) throws CanceledExecutionException {
        DataTableSpec tableSpec = numericWithHistograms.getDataTableSpec();
        BufferedDataContainer nominals = exec.createDataContainer(tableSpec);
        for (int i = 0; i < statTable.getNominalValues().size(); ++i) {
            Map<DataCell, Integer> nominalValues = statTable.getNominalValues(i);
            if (nominalValues == null) {
                continue;
            }
            final String colName = statTable.getColumnNames()[i];
            DataCell[] row = new DataCell[tableSpec.getNumColumns()];
            for (int u = row.length; u-- > 0;) {
                row[u] = DataType.getMissingCell();
            }
            row[0] = new StringCell(colName);
            HistogramNominalModel model =
                new HistogramNominalModel(new LinkedHashMap<DataValue, Integer>(nominalValues), i, colName,
                    statTable.getRowCount());
            row[row.length - 1] = createImageCell(model, false);
            nominals.addRowToTable(new DefaultRow(colName + " (as nominal)", row));
        }
        nominals.close();
        return exec.createConcatenateTable(exec, numericWithHistograms, nominals.getTable());
    }

    /**
     * Creates the nominal table (histogram with number of missing values).
     *
     * @param statTable The statistics table.
     * @param hlHandler The {@link HiLiteHandler}.
     * @param exec An {@link ExecutionContext}.
     * @param maxBinCount The maximum number of bins when we put labels to the histogram.
     * @return The table containing the histograms.
     * @see #createNominalHistogramTableSpec()
     */
    public BufferedDataTable nominalTable(final Statistics3Table statTable, final HiLiteHandler hlHandler,
        final ExecutionContext exec, final int maxBinCount) {
        DataTableSpec tableSpec = createNominalHistogramTableSpec();
        BufferedDataContainer nominals = exec.createDataContainer(tableSpec);
        for (int i = 0; i < statTable.getNominalValues().size(); ++i) {
            Map<DataCell, Integer> nominalValues = statTable.getNominalValues(i);
            if (nominalValues == null) {
                continue;
            }
            final String colName = statTable.getColumnNames()[i];
            DataCell[] row = new DataCell[tableSpec.getNumColumns()];
            for (int u = row.length; u-- > 0;) {
                row[u] = DataType.getMissingCell();
            }
            row[0] = new StringCell(colName);
            row[1] = new IntCell(statTable.getNumberMissingValues()[i]);
            HistogramNominalModel model =
                new HistogramNominalModel(new LinkedHashMap<DataValue, Integer>(nominalValues), i, colName,
                    statTable.getRowCount());
            row[row.length - 1] = createImageCell(model, maxBinCount >= model.getBins().size());
            nominals.addRowToTable(new DefaultRow(colName, row));
        }
        nominals.close();
        return nominals.getTable();
    }

    /**
     * @return The {@link DataTableSpec} for the nominal descriptor (column name, number of missings and a histogram).
     */
    public DataTableSpec createNominalHistogramTableSpec() {
        DataTableSpecCreator tableSpecCreator = new DataTableSpecCreator();
        tableSpecCreator.addColumns(new DataColumnSpecCreator("Column", StringCell.TYPE).createSpec());
        tableSpecCreator.addColumns(new DataColumnSpecCreator("No. missings", IntCell.TYPE).createSpec());
        tableSpecCreator.addColumns(createHistogramColumnSpec());
        DataTableSpec tableSpec = tableSpecCreator.createSpec();
        return tableSpec;
    }

    /**
     * @param histogramData A {@link HistogramModel}.
     * @return The SVG image cell.
     */
    private DataCell createSvgImageCell(final HistogramModel<?> histogramData, final boolean paintLabels) {
        DOMImplementation domImpl = new SVGDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        Document myFactory = domImpl.createDocument(svgNS, "svg", null);
        SVGGraphics2D g = new SVGGraphics2D(myFactory);
        g.setSVGCanvasSize(new Dimension(m_width, m_height));
        paint(histogramData, paintLabels, g);

        myFactory.replaceChild(g.getRoot(), myFactory.getDocumentElement());
        DataCell dc = new SvgCell((SVGDocument)myFactory);
        return dc;
    }

    /**
     * @param histogramData A {@link HistogramModel}.
     * @return The PNG image cell.
     */
    private DataCell createPngImageCell(final HistogramModel<?> histogramData, final boolean paintLabels) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            try {
                BufferedImage image = new BufferedImage(m_width, m_height, BufferedImage.TYPE_4BYTE_ABGR);
                Graphics2D g = image.createGraphics();
                paint(histogramData, paintLabels, g);
                ImageIO.write(image, "png", baos);
            } finally {
                baos.close();
            }
        } catch (IOException e) {
            // Should not happen, all in-memory
            throw new IllegalStateException(e.getMessage(), e);
        }
        return new PNGImageContent(baos.toByteArray()).toImageCell();
    }

    /**
     * Paints the histograms on the canvas ({@code g}).
     *
     * @param histogramData The {@link HistogramModel} to paint.
     * @param paintLabels Should we paint the labels too?
     * @param g A {@link Graphics2D} object.
     */
    void paint(final HistogramModel<?> histogramData, final boolean paintLabels, final Graphics2D g) {
        paint(histogramData, g);
        if (paintLabels) {
            paintLabels(histogramData, g, binWidth(histogramData));
        }
    }

    /**
     * @param histogramData A {@link HistogramModel}.
     * @param paintLabels Should we paint the labels too?
     * @return The PNG or SVG image cell according to the format.
     */
    private DataCell createImageCell(final HistogramModel<?> histogramData, final boolean paintLabels) {
        switch (m_format) {
            case PNG:
                return createPngImageCell(histogramData, paintLabels);
            case SVG:
                return createSvgImageCell(histogramData, paintLabels);
            default:
                throw new UnsupportedOperationException("Not supported image format: " + m_format.name());
        }
    }

    /**
     * Creates a nominal {@link HistogramModel} from the {@link Statistics3Table#getNominalValues()} structure..
     *
     * @param counts The frequencies.
     * @param colIndex The column index.
     * @param colName The name of the column.
     * @return The nominal {@link HistogramModel}.
     */
    public HistogramModel<?> fromNominalModel(final Map<? extends DataValue, Integer> counts, final int colIndex,
        final String colName) {
        int sum = 0;
        for (Integer v : counts.values()) {
            sum += v.intValue();
        }
        return new HistogramNominalModel(counts, colIndex, colName, sum);
    }
}
