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
 * History
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.base.node.viz.histogram.util.ColorColumn;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * This is the fixed data model implementation of the histogram which
 * is created only once when the user executes a node.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramDataModel {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(FixedHistogramDataModel.class);

    /**The name of the data file which contains all data in serialized form.*/
    private static final String CFG_DATA_FILE = "dataFile.xml.gz";
    private static final String CFG_DATA = "fixedHistogramDataModel";
    private static final String CFG_X_COL_SPEC = "xColSpec";
    private static final String CFG_NOMINAL = "binNominal";
    private static final String CFG_AGGR_COLS = "aggregationColumns";
    private static final String CFG_AGGR_METHOD = "aggregationMethod";
    private static final String CFG_AGGR_COL_COUNTER = "aggrColCounter";
    private static final String CFG_COLOR_COL = "aggrCol_";
    private static final String CFG_BINS = "bins";
    private static final String CFG_BIN_COUNTER = "binCounter";
    private static final String CFG_BIN = "bin_";
    private static final String CFG_MISSING_BIN = "missingBin";
    private static final String CFG_COLOR_COLS = "rowColors";
    private static final String CFG_ROW_COLOR_COUNTER = "rowColorCounter";
    private static final String CFG_ROW_COLOR = "rowColor_";


    private final DataColumnSpec m_xColSpec;
    private final Collection<ColorColumn> m_aggrColumns;
    private final SortedSet<Color> m_rowColors;
    private boolean m_binNominal;
    private final List<BinDataModel> m_bins;
    private final BinDataModel m_missingValueBin;

    private final AggregationMethod m_aggrMethod;

    /**Constructor for class HistogramDataModel.
     * @param xColSpec the column specification of the bin column
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param aggrColumns the aggregation columns
     * @param noOfBins the number of bins to create
     */
    public FixedHistogramDataModel(final DataColumnSpec xColSpec,
            final AggregationMethod aggrMethod,
            final Collection<ColorColumn> aggrColumns, final int noOfBins) {
        LOGGER.debug("Entering HistogramDataModel(xColSpec, aggrColumns) "
                + "of class HistogramDataModel.");
        if (xColSpec == null) {
            throw new NullPointerException(
                    "Binning column specification must not be null");
        }
        m_aggrMethod = aggrMethod;
        m_aggrColumns = aggrColumns;
        m_xColSpec = xColSpec;
        final DataColumnDomain domain = m_xColSpec.getDomain();
        if (domain == null) {
            throw new NullPointerException(
                    "The binning column domain must not be null");
        }
        if (m_xColSpec.getType().isCompatible(
                DoubleValue.class)) {
            m_binNominal = false;
            m_bins = BinningUtil.createIntervalBins(xColSpec, noOfBins);
        } else {
            m_binNominal = true;
            m_bins = BinningUtil.createNominalBins(xColSpec);
        }
        m_missingValueBin  = new BinDataModel(
                AbstractHistogramVizModel.MISSING_VAL_BAR_CAPTION, 0, 0);
        m_rowColors  =
            new TreeSet<Color>(HSBColorComparator.getInstance());
        LOGGER.debug("Exiting HistogramDataModel(xColSpec, aggrColumns) "
                + "of class HistogramDataModel.");
    }

    /**Constructor for class FixedHistogramDataModel used in serialization.
     * @param xColSpec the column specification of the bin column
     * @param aggrColumns the aggregation columns
     * @param binNominal if the bins are nominal or not
     * @param bins the bins itself
     * @param missingBin the missing value bin
     * @param rowColors the row colors
     */
    private FixedHistogramDataModel(final DataColumnSpec xColSpec,
            final AggregationMethod aggrMethod,
            final Collection<ColorColumn> aggrColumns,
            final boolean binNominal, final List<BinDataModel> bins,
            final BinDataModel missingBin, final SortedSet<Color> rowColors) {
        m_xColSpec = xColSpec;
        m_aggrMethod = aggrMethod;
        m_aggrColumns = aggrColumns;
        m_binNominal = binNominal;
        m_bins = bins;
        m_missingValueBin = missingBin;
        m_rowColors = rowColors;
    }

    /**
     * Adds the given row values to the histogram.
     * @param id the row key of this row
     * @param rowColor the color of this row
     * @param xCell the x value
     * @param aggrCells the aggregation values
     */
    public void addDataRow(final RowKey id, final Color rowColor,
            final DataCell xCell, final DataCell... aggrCells) {
        if (xCell == null) {
            throw new NullPointerException("X value must not be null.");
        }
        if (!m_rowColors.contains(rowColor)) {
            m_rowColors.add(rowColor);
        }
        final int startBin = 0;
        try {
            BinningUtil.addDataRow2Bin(m_binNominal, m_bins, m_missingValueBin,
                    startBin, xCell, rowColor, id, m_aggrColumns, aggrCells);
        } catch (final IllegalArgumentException e) {
            if (!BinningUtil.checkDomainRange(xCell, getXColumnSpec())) {
                throw new IllegalStateException(
                    "Invalid column domain for column "
                    + m_xColSpec.getName()
                    + ". " + e.getMessage());
            }
            throw e;
        }
    }

    /**
     * @return the x column name
     */
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * @return the x column specification
     */
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }
    /**
     * @return the columns to use for aggregation.
     * THIS IS AN UNMODIFIABLE {@link Collection}!
     */
    public Collection<ColorColumn> getAggrColumns() {
        if (m_aggrColumns == null) {
            return null;
        }
        return Collections.unmodifiableCollection(m_aggrColumns);
    }


    /**
     * @return the aggrMethod to use
     */
    public AggregationMethod getAggrMethod() {
        return m_aggrMethod;
    }

    /**
     * @return all available element colors. This is the color the user has
     * set for one attribute in the Color Manager node.
     */
    public List<Color> getRowColors() {
        return new ArrayList<Color>(m_rowColors);
    }

    /**
     * @return <code>true</code> if the bins are nominal
     */
    public boolean getBinNominal() {
        return m_binNominal;
    }


    /**
     * @return a copy of all bins
     */
    @SuppressWarnings("unchecked")
    public List<BinDataModel> getClonedBins() {
        LOGGER.debug("Entering getClonedBins() of class "
                + "FixedHistogramDataModel.");
        final long startTime = System.currentTimeMillis();
        List<BinDataModel> binClones = null;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(m_bins);
            final ByteArrayInputStream bais =
                new ByteArrayInputStream(baos.toByteArray());
            binClones =
                (List<BinDataModel>)new ObjectInputStream(bais).readObject();
        } catch (final Exception e) {
              binClones =
              new ArrayList<BinDataModel>(m_bins.size());
              for (final BinDataModel bin : m_bins) {
                  binClones.add(bin.clone());
              }
        }

        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for cloning. " + durationTime + " ms");
        LOGGER.debug("Exiting getClonedBins() of class "
                + "FixedHistogramDataModel.");
        return binClones;
    }


    /**
     * @return a copy of the bin with all rows where the x value was missing
     */
    public BinDataModel getClonedMissingValueBin() {
        return m_missingValueBin.clone();
    }

    /**
     * @param directory the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @throws IOException if a file exception occurs
     * @throws CanceledExecutionException if the operation is canceled
     */
    public void save2File(final File directory,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (exec != null) {
            exec.setProgress(0.0, "Start saving histogram data model to file");
        }
        final File dataFile = new File(directory, CFG_DATA_FILE);
        final FileOutputStream os = new FileOutputStream(dataFile);
        final GZIPOutputStream dataOS = new GZIPOutputStream(os);
        final Config config = new NodeSettings(CFG_DATA);
        final ConfigWO xConf = config.addConfig(CFG_X_COL_SPEC);
        m_xColSpec.save(xConf);
        if (exec != null) {
            exec.setProgress(0.1, "Binning column specification saved");
            exec.setMessage("Start saving aggregation columns...");
        }
        config.addBoolean(CFG_NOMINAL, m_binNominal);
        config.addString(CFG_AGGR_METHOD, m_aggrMethod.getActionCommand());

        final Config aggrConf = config.addConfig(CFG_AGGR_COLS);
        aggrConf.addInt(CFG_AGGR_COL_COUNTER, m_aggrColumns.size());
        int idx = 0;
        for (final ColorColumn col : m_aggrColumns) {
            final ConfigWO aggrColConf =
                aggrConf.addConfig(CFG_COLOR_COL + idx++);
            col.save2File(aggrColConf, exec);
        }
        if (exec != null) {
            exec.setProgress(0.3, "Start saving bins...");
        }
        final ConfigWO binsConf = config.addConfig(CFG_BINS);
        binsConf.addInt(CFG_BIN_COUNTER , m_bins.size());
        idx = 0;
        for (final BinDataModel bin : m_bins) {
            final ConfigWO binConf = binsConf.addConfig(CFG_BIN + idx++);
            bin.save2File(binConf, exec);
        }
        final ConfigWO missingBin = binsConf.addConfig(CFG_MISSING_BIN);
        m_missingValueBin.save2File(missingBin, exec);
        if (exec != null) {
            exec.setProgress(0.8, "Start saving element colors...");
        }
        final List<Color> rowColors = getRowColors();
        final ConfigWO colorColsConf = config.addConfig(CFG_COLOR_COLS);
        colorColsConf.addInt(CFG_ROW_COLOR_COUNTER, rowColors.size());
        idx = 0;
        for (final Color color : rowColors) {
            colorColsConf.addInt(CFG_ROW_COLOR + idx++, color.getRGB());
        }
        config.saveToXML(dataOS);
        dataOS.flush();
        dataOS.close();
        os.flush();
        os.close();
        if (exec != null) {
            exec.setProgress(1.0, "Histogram data model saved");
        }
    }

    /**
     * @param directory the directory to write to
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the histogram data model
     * @throws InvalidSettingsException if the x column specification
     * wasn't valid
     * @throws IOException if a file exception occurs
     * @throws CanceledExecutionException if the operation is canceled
     */
    @SuppressWarnings("unchecked")
    public static FixedHistogramDataModel loadFromFile(final File directory,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            IOException, CanceledExecutionException {
        if (exec != null) {
            exec.setProgress(0.0, "Start reading data from file");
        }
        final ConfigRO config;
        final FileInputStream is;
        final GZIPInputStream inData;
        try {
            final File settingsFile = new File(directory, CFG_DATA_FILE);
            is = new FileInputStream(settingsFile);
            inData = new GZIPInputStream(is);
            config = NodeSettings.loadFromXML(inData);
        } catch (final FileNotFoundException e) {
            throw e;
        } catch (final IOException e) {
            LOGGER.error("Unable to load histogram data: " + e.getMessage());
            throw new IOException("Please reexecute the histogram node. "
                    + "(For details see log file)");
        }
        final Config xConfig = config.getConfig(CFG_X_COL_SPEC);
        final DataColumnSpec xColSpec = DataColumnSpec.load(xConfig);
        final boolean binNominal = config.getBoolean(CFG_NOMINAL);
        AggregationMethod aggrMethod = AggregationMethod.getDefaultMethod();
        try {
            aggrMethod = AggregationMethod.getMethod4Command(
                    config.getString(CFG_AGGR_METHOD));
        } catch (final Exception e) {
            // Take the default method
        }
        if (exec != null) {
            exec.setProgress(0.1, "Binning column specification loaded");
            exec.setProgress("Loading aggregation columns...");
        }
        final Config aggrConf = config.getConfig(CFG_AGGR_COLS);
        final int aggrColCounter = aggrConf.getInt(CFG_AGGR_COL_COUNTER);
        final ArrayList<ColorColumn> aggrCols =
            new ArrayList<ColorColumn>(aggrColCounter);
        for (int i = 0; i < aggrColCounter; i++) {
            final Config aggrColConf = aggrConf.getConfig(CFG_COLOR_COL + i);
            aggrCols.add(ColorColumn.loadFromFile(aggrColConf, exec));
        }
        if (exec != null) {
            exec.setProgress(0.3, "Loading bins...");
        }
        final ConfigRO binsConf = config.getConfig(CFG_BINS);
        final int binCounter = binsConf.getInt(CFG_BIN_COUNTER);
        final List<BinDataModel> bins = new ArrayList<BinDataModel>(binCounter);
        for (int i = 0; i < binCounter; i++) {
            final Config binConf = binsConf.getConfig(CFG_BIN + i);
            bins.add(BinDataModel.loadFromFile(binConf, exec));
        }
        final Config missingConfig = binsConf.getConfig(CFG_MISSING_BIN);
        final BinDataModel missingBin =
            BinDataModel.loadFromFile(missingConfig, exec);
        if (exec != null) {
            exec.setProgress(0.9, "Loading element colors...");
        }
        final ConfigRO colorColsConf = config.getConfig(CFG_COLOR_COLS);
        final int counter = colorColsConf.getInt(CFG_ROW_COLOR_COUNTER);
        final SortedSet<Color> rowColors =
            new TreeSet<Color>(HSBColorComparator.getInstance());
        for (int i = 0; i < counter; i++) {
            rowColors.add(new Color(colorColsConf.getInt(CFG_ROW_COLOR + i)));
        }
        if (exec != null) {
            exec.setProgress(1.0, "Histogram data model loaded ");
        }
        //close the stream
        inData.close();
        is.close();
        return new FixedHistogramDataModel(xColSpec, aggrMethod, aggrCols,
                binNominal, bins, missingBin, rowColors);
    }
}
