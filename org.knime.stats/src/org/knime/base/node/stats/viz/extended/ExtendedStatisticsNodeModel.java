/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.base.node.stats.viz.extended;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.statistics.HistogramColumn;
import org.knime.base.data.statistics.HistogramColumn.BinNumberSelectionStrategy;
import org.knime.base.data.statistics.HistogramColumn.ImageFormats;
import org.knime.base.data.statistics.HistogramModel;
import org.knime.base.data.statistics.Statistics3Table;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.statistics2.Statistics3NodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.util.Pair;

/**
 * This is the model implementation of ExtendedStatistics. Calculates statistic moments with their distributions and
 * counts nominal values and their occurrences across all columns.
 *
 * @author Gabor Bakos
 */
class ExtendedStatisticsNodeModel extends Statistics3NodeModel {
    /** Configuration key for the image format of histogram. */
    protected static final String CFGKEY_IMAGE_FORMAT = "image format";

    /** The value for the PNG file format. */
    protected static final String PNG = "PNG";

    /** The value for the SVG file format. */
    protected static final String SVG = "SVG";

    /** The supported image file formats. */
    protected static final List<String> POSSIBLE_IMAGE_FORMATS = Collections.unmodifiableList(Arrays.asList(PNG, SVG));
    static {
        boolean hasPng = false, hasSvg = false;
        for (String format : POSSIBLE_IMAGE_FORMATS) {
            hasPng |= format.equals(ImageFormats.PNG.name());
            hasSvg |= format.equals(ImageFormats.SVG.name());
        }
        assert hasPng;
        assert hasSvg;
    }

    /** The default value for the image format. */
    protected static final String DEFAULT_IMAGE_FORMAT = SVG;

    /** The configuration key for the histogram width. */
    protected static final String CFGKEY_HISTOGRAM_WIDTH = "histogram width";

    /** The default value for the histogram width. */
    protected static final int DEFAULT_HISTOGRAM_WIDTH = 200;

    /** The configuration key for the histogram height. */
    protected static final String CFGKEY_HISTOGRAM_HEIGHT = "histogram height";

    /** The default value for the histogram height. */
    protected static final int DEFAULT_HISTOGRAM_HEIGHT = 100;

    /** The configuration key for enabling HiLite. */
    protected static final String CFGKEY_ENABLE_HILITE = "enable HiLite";

    /** The default value for enabling HiLite. */
    protected static final boolean DEFAULT_ENABLE_HILITE = false;

    private static final String HISTOGRAMS_GZ = "histograms.xml.gz";

    private static final String DATA_ARRAY_GZ = "dataarray.gz";

    /**
     * Helper method to create image format model.
     *
     * @return A {@link SettingsModelString} for the image format.
     */
    protected static SettingsModelString createImageFormat() {
        return new SettingsModelString(CFGKEY_IMAGE_FORMAT, DEFAULT_IMAGE_FORMAT);
    }

    /**
     * Helper method to create the histogram width model.
     *
     * @return A {@link SettingsModelIntegerBounded} for the histogram width.
     */
    protected static SettingsModelIntegerBounded createHistogramWidth() {
        return new SettingsModelIntegerBounded(CFGKEY_HISTOGRAM_WIDTH, DEFAULT_HISTOGRAM_WIDTH, 1, 2000);
    }

    /**
     * Helper method to create the histogram height model.
     *
     * @return A {@link SettingsModelIntegerBounded} for the histogram height.
     */
    protected static SettingsModelIntegerBounded createHistogramHeight() {
        return new SettingsModelIntegerBounded(CFGKEY_HISTOGRAM_HEIGHT, DEFAULT_HISTOGRAM_HEIGHT, 1, 2000);
    }

    /**
     * Helper method to create the enable HiLite model.
     *
     * @return A {@link SettingsModelBoolean} for the enable HiLite.
     */
    protected static SettingsModelBoolean createEnableHiLite() {
        return new SettingsModelBoolean(CFGKEY_ENABLE_HILITE, DEFAULT_ENABLE_HILITE);
    }

    private SettingsModelString m_imageFormat = createImageFormat();

    private SettingsModelInteger m_histogramWidth = createHistogramWidth(),
            m_histogramHeight = createHistogramHeight();

    private SettingsModelBoolean m_enableHiLite = createEnableHiLite();

    /// column index -> nominal value -> keys
    private Map<Integer, Map<DataValue, Set<RowKey>>> m_nominalKeys =
        new HashMap<Integer, Map<DataValue, Set<RowKey>>>();

    /// column index -> bucket index -> keys
    private Map<Integer, Map<Integer, Set<RowKey>>> m_buckets = new HashMap<Integer, Map<Integer, Set<RowKey>>>();

    private Map<Integer, ? extends HistogramModel<?>> m_histograms;

    //private Map<Integer, DataType> m_nominalTypes;

    private DataArray m_subTable;

    /**
     * Constructor for the node model.
     */
    protected ExtendedStatisticsNodeModel() {
        super(1, 3);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CanceledExecutionException
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws CanceledExecutionException {
        double initPercent = m_enableHiLite.getBooleanValue() ? .25 : .2;
        ExecutionContext init = exec.createSubExecutionContext(initPercent);
        BufferedDataTable[] superTables = super.execute(inData, init);
        BufferedDataTable[] ret = new BufferedDataTable[3];
        DataTableSpec superTableSpec = superTables[1].getSpec();
        DataTableSpec newSpec = renamedOccurrencesSpec(superTableSpec);
        ret[2] = exec.createSpecReplacerTable(superTables[1], newSpec);
        ExecutionContext table = exec.createSubExecutionContext(initPercent);
        ret[0] = getStatTable().createStatisticsInColumnsTable(table);
        ExecutionContext histogram = exec.createSubExecutionContext(1.0 / 2);
        final HistogramColumn histogramColumn = createHistogramColumn();
        HiLiteHandler hlHandler = getEnableHiLite().getBooleanValue() ? getInHiLiteHandler(0) : new HiLiteHandler();
        double[] mins = getStatTable().getMin(), maxes = getStatTable().getMax();
        for (int i = 0; i < maxes.length; i++) {
            DataCell min = getStatTable().getNonInfMin(i);
            if (min.isMissing()) {
                mins[i] = Double.NaN;
            } else {
                mins[i] = ((DoubleValue) min).getDoubleValue();
            }

            DataCell max = getStatTable().getNonInfMax(i);
            if (max.isMissing()) {
                maxes[i] = Double.NaN;
            } else {
                maxes[i] = ((DoubleValue) max).getDoubleValue();
            }
        }
        Pair<BufferedDataTable, Map<Integer, ? extends HistogramModel<?>>> pair =
            histogramColumn.process(histogram, inData[0], hlHandler, ret[0], mins, maxes, numOfNominalValues(), getColumnNames());
//        final BufferedDataTable outTable =
//            histogramColumn.appendNominal(pair.getFirst(), getStatTable(), hlHandler, exec, numOfNominalValues());
        ret[0] = pair.getFirst();
        ret[1] = histogramColumn.nominalTable(getStatTable(), hlHandler, exec, numOfNominalValues());
        if (m_enableHiLite.getBooleanValue()) {
            double rest = 1 - initPercent * 2 - 1.0 / 2;
            ExecutionContext projection = exec.createSubExecutionContext(rest / 2);
            ColumnRearranger rearranger = new ColumnRearranger(inData[0].getDataTableSpec());
            Set<String> colNames = new HashSet<String>(Arrays.asList(getColumnNames()));
            for (DataColumnSpec spec : rearranger.createSpec()) {
                if ((!spec.getType().isCompatible(DoubleValue.class) && !spec.getType()
                    .isCompatible(NominalValue.class)) || !colNames.contains(spec.getName())) {
                    rearranger.remove(spec.getName());
                }
            }
            ExecutionContext save = exec.createSubExecutionContext(rest / 2);
            m_subTable =
                new DefaultDataArray(projection.createColumnRearrangeTable(inData[0], rearranger, projection), 1,
                    inData[0].getRowCount(), save);
            m_histograms = histogramColumn.histograms(inData[0], getInHiLiteHandler(0), mins, maxes, getColumnNames());
            Set<String> nominalColumns = new LinkedHashSet<String>();
            for (int i = 0; i < inData[0].getSpec().getNumColumns(); ++i) {
                Map<DataCell, Integer> nominalValues = getStatTable().getNominalValues(i);
                if (nominalValues != null) {
                    nominalColumns.add(inData[0].getSpec().getColumnSpec(i).getName());
                }
            }
            final Pair<Map<Integer, Map<Integer, Set<RowKey>>>, Map<Integer, Map<DataValue, Set<RowKey>>>> bucketsAndNominals =
                HistogramColumn.construct(m_histograms, m_subTable, nominalColumns);
            m_buckets = bucketsAndNominals.getFirst();
            m_nominalKeys = bucketsAndNominals.getSecond();
        } else {
            m_histograms = pair.getSecond();
        }
        return ret;
    }

    /**
     * @param occurrencesSpec The original occurrences spec from the base class' implementation.
     * @return The '{@code ... count}' columns are renamed to '{@code Count (...)}'.
     */
    protected DataTableSpec renamedOccurrencesSpec(final DataTableSpec occurrencesSpec) {
        DataTableSpecCreator renameSpecCreator = new DataTableSpecCreator(occurrencesSpec);
        DataColumnSpec[] specs = new DataColumnSpec[occurrencesSpec.getNumColumns()];
        for (int i = occurrencesSpec.getNumColumns(); i-->0;) {
            if (i % 2 == 1) {
                DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator(occurrencesSpec.getColumnSpec(i));
                colSpecCreator.setName("Count (" + occurrencesSpec.getColumnSpec(i - 1).getName() + ")");
                specs[i] = colSpecCreator.createSpec();
            } else {
                specs[i] = occurrencesSpec.getColumnSpec(i);
            }
        }
        renameSpecCreator.dropAllColumns();
        renameSpecCreator.addColumns(specs);
        DataTableSpec newSpec = renameSpecCreator.createSpec();
        return newSpec;
    }

    /**
     * @return The helper structure to compute the histograms.
     */
    HistogramColumn createHistogramColumn() {
        return HistogramColumn.getDefaultInstance().withNumberOfBins(4)
            .withImageFormat(getImageFormat().getStringValue()).withHistogramWidth(getHistogramWidth().getIntValue())
            .withHistogramHeight(getHistogramHeight().getIntValue())
            .withBinSelectionStrategy(BinNumberSelectionStrategy.DecimalRange);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        super.reset();
        m_nominalKeys.clear();
        m_buckets.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec[] superTables = super.configure(inSpecs);
        DataTableSpec[] ret = new DataTableSpec[3];
        DataTableSpecCreator specCreator = new DataTableSpecCreator(Statistics3Table.getStatisticsSpecification());
        final HistogramColumn hc = createHistogramColumn();
        final DataColumnSpec histogramColumnSpec = hc.createHistogramColumnSpec();
        specCreator.addColumns(histogramColumnSpec);
        ret[0] = specCreator.createSpec();
        ret[1] = hc.createNominalHistogramTableSpec();
        ret[2] = renamedOccurrencesSpec(superTables[1]);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        getImageFormat().saveSettingsTo(settings);
        getHistogramWidth().saveSettingsTo(settings);
        getHistogramHeight().saveSettingsTo(settings);
        getEnableHiLite().saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        getImageFormat().loadSettingsFrom(settings);
        getHistogramWidth().loadSettingsFrom(settings);
        getHistogramHeight().loadSettingsFrom(settings);
        getEnableHiLite().loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        getImageFormat().validateSettings(settings);
        SettingsModelString tmpFormat = createImageFormat();
        tmpFormat.loadSettingsFrom(settings);
        String format = tmpFormat.getStringValue();
        if (!POSSIBLE_IMAGE_FORMATS.contains(format)) {
            throw new InvalidSettingsException("Unsupported image format: " + format);
        }
        getHistogramWidth().validateSettings(settings);
        getHistogramHeight().validateSettings(settings);
        getEnableHiLite().validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException {
        super.loadInternals(internDir, exec);
        if (m_enableHiLite.getBooleanValue()) {
            File histogramsGz = new File(internDir, HISTOGRAMS_GZ);
            File dataArrayGz = new File(internDir, DATA_ARRAY_GZ);
            try {
                Set<String> nominalColumnNames = new LinkedHashSet<String>();
                String[] columnNames = getStatTable().getColumnNames();
                for (int i = 0; i < columnNames.length; ++i) {
                    if (getStatTable().getNominalValues(i) != null) {
                        nominalColumnNames.add(columnNames[i]);
                    }
                }
                Pair<Pair<Map<Integer, ? extends HistogramModel<?>>, Map<Integer, Map<Integer, Set<RowKey>>>>, Map<Integer, Map<DataValue, Set<RowKey>>>> ppair =
                    HistogramColumn.loadHistograms(histogramsGz, dataArrayGz, nominalColumnNames);
                m_histograms = ppair.getFirst().getFirst();
                m_buckets = ppair.getFirst().getSecond();
                m_nominalKeys = ppair.getSecond();
                //                m_nominalTypes = ppair.getSecond().getSecond();
            } catch (InvalidSettingsException e) {
                getLogger().error("Failed to load settings for the HiLite, please rerun.", e);
                m_histograms = Collections.emptyMap();
                m_buckets = Collections.emptyMap();
                m_nominalKeys = Collections.emptyMap();
                //                m_nominalTypes = Collections.emptyMap();
            }
        } else {
            File histogramsGz = new File(internDir, HISTOGRAMS_GZ);
            m_nominalKeys = Collections.emptyMap();
            try {
                m_buckets = new HashMap<Integer, Map<Integer, Set<RowKey>>>();
                m_histograms = HistogramColumn.loadHistograms(histogramsGz, m_buckets);
                m_buckets.clear();
            } catch (InvalidSettingsException e) {
                m_histograms = Collections.emptyMap();
                m_buckets = Collections.emptyMap();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        super.saveInternals(internDir, exec);
        if (m_enableHiLite.getBooleanValue()) {
            File histograms = new File(internDir, HISTOGRAMS_GZ);
            File dataArray = new File(internDir, DATA_ARRAY_GZ);
            HistogramColumn.saveHistograms(m_histograms, m_buckets, m_nominalKeys/*, m_nominalTypes*/, histograms,
                m_subTable, dataArray, exec);
        } else {
            File histograms = new File(internDir, HISTOGRAMS_GZ);
            HistogramColumn.saveHistogramData(m_histograms, histograms);
        }
    }

    /**
     * @return the imageFormat
     */
    protected SettingsModelString getImageFormat() {
        return m_imageFormat;
    }

    /**
     * @return the histogramWidth
     */
    protected SettingsModelInteger getHistogramWidth() {
        return m_histogramWidth;
    }

    /**
     * @return the histogramHeight
     */
    protected SettingsModelInteger getHistogramHeight() {
        return m_histogramHeight;
    }

    /**
     * @return the enableHiLite
     */
    protected SettingsModelBoolean getEnableHiLite() {
        return m_enableHiLite;
    }

    /**
     * @return the histograms (modifiable)
     */
    protected Map<Integer, ?> getHistograms() {
        return m_histograms;
    }

    /**
     * @return the buckets (modifiable)
     */
    protected Map<Integer, Map<Integer, Set<RowKey>>> getBuckets() {
        return m_buckets;
    }

    /**
     * @return the nominalKeys
     */
    public Map<Integer, Map<DataValue, Set<RowKey>>> getNominalKeys() {
        return m_nominalKeys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Statistics3Table getStatTable() {
        return super.getStatTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int numOfNominalValues() {
        // TODO Auto-generated method stub
        return super.numOfNominalValues();
    }
}
