/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.stats.viz.extended;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
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
class ExtendedStatisticsNodeModel extends NodeModel {
    /**
     *
     */
    private static final int DEFAULT_NUM_NOMINAL_VALUES_OUTPUT = 1000;

    /**
     *
     */
    private static final String CFGKEY_NUM_NOMINAL_VALUES_OUTPUT = "num_nominal-values_output";

    /**
     *
     */
    private static final int DEFAULT_NUM_NOMINAL_VALUES = 20;

    /**
     *
     */
    private static final String CFGKEY_NUM_NOMINAL_VALUES = "num_nominal-values";

    /**
     *
     */
    private static final boolean DEFAULT_COMPUTE_MEDIAN = false;

    /**
     *
     */
    private static final String CFGKEY_COMPUTE_MEDIAN = "compute_median";

    /**
     *
     */
    private static final String CFGKEY_FILTER_NOMINAL_COLUMNS = "filter_nominal_columns";

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

    /** The configuration key for showing the min/max(/mean) values on the x axis. */
    protected static final String CFGKEY_SHOW_MIN_MAX = "show min max";

    /** The default value for showing the min/max(/mean) values on the x axis. */
    protected static final boolean DEFAULT_SHOW_MIN_MAX = true;

    private static final String HISTOGRAMS_GZ = "histograms.xml.gz";

    private static final String DATA_ARRAY_GZ = "dataarray.gz";

    private static final BinNumberSelectionStrategy BIN_SELECTION_STRATEGY = BinNumberSelectionStrategy.DecimalRange;

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

    /**
     * Helper method to create show min/max values on the figures.
     *
     * @return A {@link SettingsModelBoolean} for showing the min/max values on the figures.
     */
    protected static SettingsModelBoolean createShowMinMax() {
        return new SettingsModelBoolean(CFGKEY_SHOW_MIN_MAX, DEFAULT_SHOW_MIN_MAX);
    }

    /**
     * @return create nominal filter model
     */
    static SettingsModelColumnFilter2 createNominalFilterModel() {
        return new SettingsModelColumnFilter2(CFGKEY_FILTER_NOMINAL_COLUMNS);
    }

    /**
     * @return boolean model to compute median
     */
    static SettingsModelBoolean createMedianModel() {
        return new SettingsModelBoolean(CFGKEY_COMPUTE_MEDIAN, DEFAULT_COMPUTE_MEDIAN);
    }

    /**
     * @return int model to restrict number of nominal values
     */
    static SettingsModelIntegerBounded createNominalValuesModel() {
        return new SettingsModelIntegerBounded(
                CFGKEY_NUM_NOMINAL_VALUES, DEFAULT_NUM_NOMINAL_VALUES, 0, Integer.MAX_VALUE);
    }

    /**
     * @return int model to restrict number of nominal values for the output
     */
    static SettingsModelIntegerBounded createNominalValuesModelOutput() {
        return new SettingsModelIntegerBounded(
                CFGKEY_NUM_NOMINAL_VALUES_OUTPUT, DEFAULT_NUM_NOMINAL_VALUES_OUTPUT, 0, Integer.MAX_VALUE);
    }


    private SettingsModelString m_imageFormat = createImageFormat();

    private SettingsModelInteger m_histogramWidth = createHistogramWidth(),
            m_histogramHeight = createHistogramHeight();

    private SettingsModelBoolean m_enableHiLite = createEnableHiLite();

    private SettingsModelBoolean m_showMinMax = createShowMinMax();

    /// column index -> nominal value -> keys
    private Map<Integer, Map<DataValue, Set<RowKey>>> m_nominalKeys =
        new HashMap<Integer, Map<DataValue, Set<RowKey>>>();

    /// column index -> bucket index -> keys
    private Map<Integer, Map<Integer, Set<RowKey>>> m_buckets = new HashMap<Integer, Map<Integer, Set<RowKey>>>();

    private Map<Integer, ? extends HistogramModel<?>> m_histograms;

    //private Map<Integer, DataType> m_nominalTypes;

    private DataArray m_subTable;

    private final SettingsModelBoolean m_computeMedian = createMedianModel();

    private final SettingsModelIntegerBounded m_nominalValues = createNominalValuesModel();

    private final SettingsModelIntegerBounded m_nominalValuesOutput = createNominalValuesModelOutput();

    private final SettingsModelColumnFilter2 m_nominalFilter = createNominalFilterModel();

    private Statistics3Table m_statTable;

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
        DataTableSpec dataSpec = inData[0].getDataTableSpec();
        List<String> includes = nominalColumns(dataSpec);
        m_statTable =
            new Statistics3Table(inData[0], m_computeMedian.getBooleanValue(), numOfNominalValuesOutput(), includes,
                init);
        if (getStatTable().getWarning() != null) {
            setWarningMessage(getStatTable().getWarning());
        }
        BufferedDataTable outTableOccurrences =
            exec.createBufferedDataTable(getStatTable().createNominalValueTable(includes), exec.createSubProgress(0.5));

        BufferedDataTable[] ret = new BufferedDataTable[3];
        DataTableSpec newSpec = renamedOccurrencesSpec(outTableOccurrences.getSpec());
        ret[2] = exec.createSpecReplacerTable(outTableOccurrences, newSpec);
        ExecutionContext table = exec.createSubExecutionContext(initPercent);
        ret[0] = getStatTable().createStatisticsInColumnsTable(table);
        ExecutionContext histogram = exec.createSubExecutionContext(1.0 / 2);
        final HistogramColumn histogramColumn = createHistogramColumn();
        HiLiteHandler hlHandler = getEnableHiLite().getBooleanValue() ? getInHiLiteHandler(0) : new HiLiteHandler();
        double[] mins = getStatTable().getMin(), maxes = getStatTable().getMax(), means = getStatTable().getMean();
        for (int i = 0; i < maxes.length; i++) {
            DataCell min = getStatTable().getNonInfMin(i);
            if (min.isMissing()) {
                mins[i] = Double.NaN;
            } else {
                mins[i] = ((DoubleValue)min).getDoubleValue();
            }

            DataCell max = getStatTable().getNonInfMax(i);
            if (max.isMissing()) {
                maxes[i] = Double.NaN;
            } else {
                maxes[i] = ((DoubleValue)max).getDoubleValue();
            }
        }
        Pair<BufferedDataTable, Map<Integer, ? extends HistogramModel<?>>> pair =
            histogramColumn.process(histogram, inData[0], hlHandler, ret[0], mins, maxes, means, numOfNominalValues(),
                getColumnNames());
        //        final BufferedDataTable outTable =
        //            histogramColumn.appendNominal(pair.getFirst(), getStatTable(), hlHandler, exec, numOfNominalValues());
        ret[0] = pair.getFirst();
        ret[1] = histogramColumn.nominalTable(getStatTable(), hlHandler, exec, numOfNominalValues());
        if (m_enableHiLite.getBooleanValue()) {
            double rest = 1 - initPercent * 2 - 1.0 / 2;
            ExecutionContext projection = exec.createSubExecutionContext(rest / 2);
            ColumnRearranger rearranger = new ColumnRearranger(dataSpec);
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
            m_histograms = histogramColumn.histograms(inData[0], getInHiLiteHandler(0), mins, maxes, means, getColumnNames());
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
     * @param dataSpec The input {@link DataTableSpec}.
     * @return The selected nominal columns.
     */
    protected List<String> nominalColumns(final DataTableSpec dataSpec) {
        return Arrays.asList(m_nominalFilter.applyTo(dataSpec).getIncludes());
    }

    /**
     * @param occurrencesSpec The original occurrences spec from the base class' implementation.
     * @return The '{@code ... count}' columns are renamed to '{@code Count (...)}'.
     * @deprecated We will not need this once the replacement for Statistics3Table is present and give the correct
     *             table.
     */
    @Deprecated
    protected DataTableSpec renamedOccurrencesSpec(final DataTableSpec occurrencesSpec) {
        DataTableSpecCreator renameSpecCreator = new DataTableSpecCreator(occurrencesSpec);
        DataColumnSpec[] specs = new DataColumnSpec[occurrencesSpec.getNumColumns()];
        for (int i = occurrencesSpec.getNumColumns(); i-- > 0;) {
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
            .withBinSelectionStrategy(BinNumberSelectionStrategy.DecimalRange)
            .withShowMinMax(getShowMinMax().getBooleanValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_statTable = null;
        m_nominalKeys.clear();
        m_buckets.clear();

    }

    /**
     * @return statistics table containing all statistic moments
     */
    final DataTable getStatsTable() {
        return getStatTable().createStatisticMomentsTable();
    }

    /**
     * @return columns used to count co-occurrences
     */
    String[] getNominalColumnNames() {
        if (getStatTable() == null) {
            return null;
        }
        return getStatTable().extractNominalColumns(
                nominalColumns(m_statTable.getSpec()));
    }

    /**
     * @return all column names
     */
    protected String[] getColumnNames() {
        if (getStatTable() == null) {
            return null;
        }
        return getStatTable().getColumnNames();
    }

    /**
     * @return number of missing values
     */
    int[] getNumMissingValues() {
        if (getStatTable() == null) {
            return null;
        }
        return getStatTable().getNumberMissingValues();
    }

    /** @return number of nominal values computed */
    protected int numOfNominalValues() {
        return m_nominalValues.getIntValue();
    }

    /** @return number of nominal values for output table */
    protected int numOfNominalValuesOutput() {
        return m_nominalValuesOutput.getIntValue();
    }

    /** @return nominal value and frequency for each column */
    List<Map<DataCell, Integer>> getNominals() {
        return getStatTable().getNominalValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        List<String> nominalValues = Arrays.asList(m_nominalFilter.applyTo(inSpecs[0]).getIncludes());
        DataTableSpec nominalSpec = createOutSpecNominal(inSpecs[0], nominalValues);

        DataTableSpec[] ret = new DataTableSpec[3];
        DataTableSpecCreator specCreator = new DataTableSpecCreator(Statistics3Table.getStatisticsSpecification());
        final HistogramColumn hc = createHistogramColumn();
        final DataColumnSpec histogramColumnSpec = hc.createHistogramColumnSpec();
        specCreator.addColumns(histogramColumnSpec);
        ret[0] = specCreator.createSpec();
        ret[1] = hc.createNominalHistogramTableSpec();
        ret[2] = nominalSpec;
        return ret;
    }

    /**
     * Create spec containing only nominal columns in same order as the input spec.
     *
     * @param inputSpec input spec
     * @param nominalValues used in map of co-occurrences
     * @return a new spec with all nominal columns (Counts are in the form: {@code Count (} nominal column name
     *         {@code )}.)
     */
    private DataTableSpec createOutSpecNominal(final DataTableSpec inputSpec, final List<String> nominalValues) {
        ArrayList<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < inputSpec.getNumColumns(); i++) {
            DataColumnSpec cspec = inputSpec.getColumnSpec(i);
            if (nominalValues.contains(cspec.getName())) {
                    cspecs.add(cspec);
                    String countCol = DataTableSpec.getUniqueColumnName(inputSpec, "Count (" + cspec.getName() + ")");
                    cspecs.add(new DataColumnSpecCreator(countCol, IntCell.TYPE).createSpec());
                }
            }
            return new DataTableSpec(cspecs.toArray(new DataColumnSpec[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_computeMedian.saveSettingsTo(settings);
        m_nominalValues.saveSettingsTo(settings);
        m_nominalValuesOutput.saveSettingsTo(settings);
        m_nominalFilter.saveSettingsTo(settings);
        getImageFormat().saveSettingsTo(settings);
        getHistogramWidth().saveSettingsTo(settings);
        getHistogramHeight().saveSettingsTo(settings);
        getEnableHiLite().saveSettingsTo(settings);
        getShowMinMax().saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_computeMedian.loadSettingsFrom(settings);
        m_nominalValues.loadSettingsFrom(settings);
        m_nominalValuesOutput.loadSettingsFrom(settings);
        m_nominalFilter.loadSettingsFrom(settings);
        getImageFormat().loadSettingsFrom(settings);
        getHistogramWidth().loadSettingsFrom(settings);
        getHistogramHeight().loadSettingsFrom(settings);
        getEnableHiLite().loadSettingsFrom(settings);
        getShowMinMax().loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_computeMedian.validateSettings(settings);
        m_nominalValues.validateSettings(settings);
        m_nominalValuesOutput.validateSettings(settings);
        m_nominalFilter.validateSettings(settings);
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
        getShowMinMax().validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException {
        NodeSettingsRO sett = NodeSettings.loadFromXML(new FileInputStream(
            new File(internDir, "statistic.xml.gz")));
        try {
            m_statTable = Statistics3Table.load(sett);
        } catch (InvalidSettingsException ise) {
            throw new IOException(ise);
        }
        double[] means = getStatTable().getMean();
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
                    HistogramColumn.loadHistograms(histogramsGz, dataArrayGz, nominalColumnNames, BIN_SELECTION_STRATEGY, means);
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
                m_histograms = HistogramColumn.loadHistograms(histogramsGz, m_buckets, BIN_SELECTION_STRATEGY, means);
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
        NodeSettings sett = new NodeSettings("statistic.xml.gz");
        getStatTable().save(sett);
        sett.saveToXML(new FileOutputStream(
                new File(internDir, sett.getKey())));
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
     * @return the showMinMax
     */
    public SettingsModelBoolean getShowMinMax() {
        return m_showMinMax;
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
     * @return The used statistics table.
     */
    protected Statistics3Table getStatTable() {
        return m_statTable;
    }
}
