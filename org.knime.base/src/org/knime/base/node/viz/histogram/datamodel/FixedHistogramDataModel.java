/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * History
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.ConfigRO;

/**
 * This is the fixed data model implementation of the histogram which
 * is created only once when the user executes a node.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramDataModel {

    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(FixedHistogramDataModel.class);
        
    /**The name of the xml file which contains the x column specification.*/
    private static final String CFG_SETTINGS_FILE = "settingsFile";

    /**The name of the root tag of the x column specification.*/
    private static final String CFG_SETTINGS_NAME = "histoSettings";
    
    /**The name of the data file which contains all data in serialized form.*/
    private static final String CFG_DATA_FILE = "dataFile";
    
    private final DataColumnSpec m_xColSpec;
   
    private final Collection<ColorColumn> m_aggrColumns;
    
    private final SortedSet<Color> m_rowColors;
    
    private boolean m_binNominal;
    
    private final List<BinDataModel> m_bins;
    
    private final BinDataModel m_missingValueBin;

    /**Constructor for class HistogramDataModel.
     * @param xColSpec the column specification of the bin column
     * @param aggrColumns the aggregation columns
     * @param noOfBins the number of bins to create
     */
    public FixedHistogramDataModel(final DataColumnSpec xColSpec,
            final Collection<ColorColumn> aggrColumns, final int noOfBins) {
        LOGGER.debug("Entering HistogramDataModel(xColSpec, aggrColumns) "
                + "of class HistogramDataModel.");
        if (xColSpec == null) {
            throw new NullPointerException(
                    "X column specification must not be null");
        }
        m_aggrColumns = aggrColumns;
        m_xColSpec = xColSpec;
        final DataColumnDomain domain = m_xColSpec.getDomain();
        if (domain == null) {
            throw new NullPointerException(
                    "The x column domain must not be null");
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
            new TreeSet<Color>(new HSBColorComparator());
        LOGGER.debug("Exiting HistogramDataModel(xColSpec, aggrColumns) "
                + "of class HistogramDataModel.");
    }
    
    /**Constructor for class FixedHistogramDataModel used in serialization.
     * @param xColSpecxColSpec the column specification of the bin column
     * @param aggrColumns the aggregation columns
     * @param binNominal if the bins are nominal or not
     * @param bins the bins itself
     * @param missingBin the missing value bin
     * @param rowColors the row colors
     */
    private FixedHistogramDataModel(final DataColumnSpec xColSpec,
            final Collection<ColorColumn> aggrColumns, 
            final boolean binNominal,
            final List<BinDataModel> bins, final BinDataModel missingBin,
            final SortedSet<Color> rowColors) {
        m_xColSpec = xColSpec;
        m_aggrColumns = aggrColumns;
        m_binNominal = binNominal;
        m_bins = bins;
        m_missingValueBin = missingBin;
        m_rowColors = rowColors;
    }
   
    /**
     * Adds the given {@link FixedHistogramDataRow} to the histogram.
     * @param id the row key of this row
     * @param rowColor the color of this row
     * @param xCell the x value
     * @param aggrCells the aggregation values
     */
    public void addDataRow(final DataCell id, final Color rowColor, 
            final DataCell xCell, final DataCell... aggrCells) {
        if (xCell == null) {
            throw new NullPointerException("X value must not be null.");
        }
        if (!m_rowColors.contains(rowColor)) {
            m_rowColors.add(rowColor);
        }
        final int startBin = 0;
        BinningUtil.addDataRow2Bin(m_binNominal, m_bins, m_missingValueBin, 
                startBin, xCell, rowColor, id, m_aggrColumns, aggrCells);
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
     * @return all available element colors. This is the color the user has
     * set for one attribute in the ColorManager node.
     * THIS IS AN UNMODIFIABLE {@link SortedSet}!
     */
    public SortedSet<Color> getRowColors() {
        return Collections.unmodifiableSortedSet(m_rowColors);
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
        } catch (Exception e) {
              binClones = 
              new ArrayList<BinDataModel>(m_bins.size());
              for (BinDataModel bin : m_bins) {
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
     */
    public void save2File(final File directory, 
            final ExecutionMonitor exec) throws IOException {
        if (exec != null) {
            exec.setProgress(0.0, "Start saving histogram data model to file");
        }
        final File settingsFile = new File(directory, CFG_SETTINGS_FILE);
        final FileOutputStream settingsOS = new FileOutputStream(settingsFile);
        final NodeSettings settings = new NodeSettings(CFG_SETTINGS_NAME);
        m_xColSpec.save(settings);
        settings.saveToXML(settingsOS);
        if (exec != null) {
            exec.setProgress(0.1, "X column specification saved");
            exec.setMessage("Start saving aggregation columns...");
        }
        final File dataFile = new File(directory, CFG_DATA_FILE);
        final FileOutputStream dataOS = new FileOutputStream(dataFile);
        final ObjectOutputStream os = new ObjectOutputStream(dataOS);
        os.writeObject(m_aggrColumns);
        if (exec != null) {
            exec.setProgress(0.3, "Start saving bins...");
        }
        os.writeBoolean(m_binNominal);
        os.writeObject(m_bins);
        os.writeObject(m_missingValueBin);
        if (exec != null) {
            exec.setProgress(0.8, "Start saving element colors...");
        }
        os.writeObject(m_rowColors);
        os.flush();
        os.close();
        dataOS.flush();
        dataOS.close();
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
     * @throws ClassNotFoundException if a class couldn't be deserialized
     */
    @SuppressWarnings("unchecked")
    public static FixedHistogramDataModel loadFromFile(final File directory, 
            final ExecutionMonitor exec) throws InvalidSettingsException, 
            IOException, ClassNotFoundException {
        if (exec != null) {
            exec.setProgress(0.0, "Start reading data from file");
        }
        final File settingsFile = new File(directory, CFG_SETTINGS_FILE);
        FileInputStream in = new FileInputStream(settingsFile);
        final ConfigRO settings = NodeSettings.loadFromXML(in);
        DataColumnSpec xColSpec = DataColumnSpec.load(settings);
        if (exec != null) {
            exec.setProgress(0.1, "X column specification loaded");
            exec.setProgress("Loading aggregation columns...");
        }
        final File dataFile = new File(directory, CFG_DATA_FILE);
        final FileInputStream dataIS = new FileInputStream(dataFile);
        final ObjectInputStream os = new ObjectInputStream(dataIS);
        final Collection<ColorColumn> aggrColumns = 
            (Collection<ColorColumn>)os.readObject();
        if (exec != null) {
            exec.setProgress(0.3, "Loading bins...");
        }
        final boolean binNominal = os.readBoolean();
        final List<BinDataModel> bins = (List<BinDataModel>)os.readObject();
        final BinDataModel missingBin = (BinDataModel)os.readObject();
        if (exec != null) {
            exec.setProgress(0.8, "Loading element colors...");
        }
        final SortedSet<Color> rowColors = (SortedSet<Color>)os.readObject();
        if (exec != null) {
            exec.setProgress(1.0, "Histogram data mdoel loaded ");
        }
        //close the streams
        os.close();
        dataIS.close();
        return new FixedHistogramDataModel(xColSpec, aggrColumns, binNominal,
                bins, missingBin, rowColors);
    }
}
