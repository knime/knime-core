/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   06.12.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.data.bitvector.BitString2BitVectorCellFactory;
import org.knime.base.data.bitvector.BitVectorCellFactory;
import org.knime.base.data.bitvector.Hex2BitVectorCellFactory;
import org.knime.base.data.bitvector.IdString2BitVectorCellFactory;
import org.knime.base.data.bitvector.Numeric2BitVectorMeanCellFactory;
import org.knime.base.data.bitvector.Numeric2BitVectorThresholdCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.bitvector.DenseBitVectorCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The BitvectorGenerator translates all values above or equal to a given
 * threshold to a set bit, values below that threshold to bits set to zero.
 * Thus, an n-column input will be translated to a 1-column table, where each
 * column contains a bitvector of length n.
 *
 * @author Fabian Dill, University of Konstanz
 */
public class BitVectorGeneratorNodeModel extends NodeModel {
    /**
     * Represents the string types that can be parsed.
     *
     * @author Fabian Dill, University of Konstanz
     */
    public enum STRING_TYPES {
        /** A hexadecimal representation of bits. */
        HEX,
        /**
         * A string of ids, where every id indicates the position in the bitset
         * to set.
         */
        ID,
        /** A string of '0' and '1'. */
        BIT;
    }

    // private static final NodeLogger logger
    // = NodeLogger.getLogger(BitVectorGeneratorNodeModel.class);

    /** Config key for the threshold. */
    public static final String CFG_THRESHOLD = "THRESHOLD";

    /** Config key if a StringCell should be parsed. */
    public static final String CFG_FROM_STRING = "FROM_STRING";

    /** Config key for the type of string (Hex, Id, Bit). */
    public static final String CFG_STRING_TYPE = "stringType";

    /** Config key whether the threshold is used for the mean. */
    public static final String CFG_USE_MEAN = "useMean";

    /** Config key for the mean percentage. */
    public static final String CFG_MEAN_THRESHOLD = "meanThreshold";

    /** Default value for the threshold. */
    public static final double DEFAULT_THRESHOLD = 1.0;

    /** Flag whether column(s) should be replaced or not. */
    public static final String CFG_REPLACE = "replace";

    private double m_threshold = DEFAULT_THRESHOLD;

    private boolean m_fromString = true;

    private boolean m_useMean = false;

    private int m_meanPercentage = 100;

    private STRING_TYPES m_type = STRING_TYPES.BIT;

    private final SettingsModelString m_stringColumn = createStringColModel();
    
    private final SettingsModelFilterString m_includedColumns 
        = createColumnFilterModel();

    private static final String FILE_NAME = "bitVectorParams";

    private static final String INT_CFG_ROWS = "nrOfRows";

    private static final String INT_CFG_NR_ZEROS = "nrOfZeros";

    private static final String INT_CFG_NR_ONES = "nrOfOnes";

    private BitVectorCellFactory m_factory;

    private boolean m_replace = false;

    private int m_nrOfProcessedRows;

    private int m_totalNrOf0s;

    private int m_totalNrOf1s;

    /**
     * Creates an instance of the BitVectorGeneratorNodeModel with one inport
     * and one outport. The numerical values from the inport are translated to
     * bivectors in the output.
     */
    public BitVectorGeneratorNodeModel() {
        super(1, 1);
    }

    /**
     * @return the number of processed rows
     */
    public int getNumberOfProcessedRows() {
        if (m_factory != null) {
            m_nrOfProcessedRows = m_factory.getNrOfProcessedRows();
        }
        return m_nrOfProcessedRows;
    }

    /**
     *
     * @return the number of 1s generated
     */
    public int getTotalNrOf1s() {
        if (m_factory != null) {
            m_totalNrOf1s = m_factory.getNumberOfSetBits();
        }
        return m_totalNrOf1s;
    }

    /**
     *
     * @return the number of 0s generated
     */
    public int getTotalNrOf0s() {
        if (m_factory != null) {
            m_totalNrOf0s = m_factory.getNumberOfNotSetBits();
        }
        return m_totalNrOf0s;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addDouble(CFG_THRESHOLD, m_threshold);
        settings.addBoolean(CFG_FROM_STRING, m_fromString);
        m_stringColumn.saveSettingsTo(settings);
        m_includedColumns.saveSettingsTo(settings);
        settings.addBoolean(CFG_USE_MEAN, m_useMean);
        settings.addInt(CFG_MEAN_THRESHOLD, m_meanPercentage);
        settings.addString(CFG_STRING_TYPE, m_type.name());
        settings.addBoolean(CFG_REPLACE, m_replace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getDouble(CFG_THRESHOLD);
        settings.getBoolean(CFG_USE_MEAN);
        settings.getInt(CFG_MEAN_THRESHOLD);
        boolean fromString = settings.getBoolean(CFG_FROM_STRING);
        SettingsModelString tmp =
            m_stringColumn.createCloneWithValidatedValue(settings);
        String stringCol = tmp.getStringValue();
        if (fromString && stringCol == null) {
            throw new InvalidSettingsException(
                    "A String column must be specified!");
        }
        settings.getString(CFG_STRING_TYPE);
        settings.getBoolean(CFG_REPLACE, false);
        // try to load them
        SettingsModelFilterString clone = null;
        try {
            clone = m_includedColumns.createCloneWithValidatedValue(settings);
        } catch (InvalidSettingsException ise) {
            // added with 2.0 -> no validation for backward compatibility
        }
        if (clone != null) {
            if (clone.isEnabled() && clone.getIncludeList().isEmpty()) {
                throw new InvalidSettingsException(
                        "No numeric input columns selected.");
            }
        }
            
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_threshold = settings.getDouble(CFG_THRESHOLD);
        m_fromString = settings.getBoolean(CFG_FROM_STRING);
        m_stringColumn.loadSettingsFrom(settings);
        
        m_useMean = settings.getBoolean(CFG_USE_MEAN);
        m_meanPercentage = settings.getInt(CFG_MEAN_THRESHOLD);
        String type = settings.getString(CFG_STRING_TYPE);
        try {
            m_type = STRING_TYPES.valueOf(type);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Illegal conversion type: '"
                    + type + "'");
        }
        m_replace = settings.getBoolean(CFG_REPLACE, false);
        
        try {
            // for backward compatibility try to load it
            m_includedColumns.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            // if not available: use all numeric columns
        }
    }

    private double[] calculateMeanValues(final DataTable input) {
        double[] meanValues = new double[input.getDataTableSpec()
                .getNumColumns()];
        int nrOfRows = 0;
        for (DataRow row : input) {
            for (int i = 0; i < row.getNumCells(); i++) {
                if (row.getCell(i).isMissing()
                 || !row.getCell(i).getType().isCompatible(DoubleValue.class)) {
                    continue;
                }
                meanValues[i] += ((DoubleValue)row.getCell(i)).getDoubleValue();
            }
            nrOfRows++;
        }
        for (int i = 0; i < meanValues.length; i++) {
            meanValues[i] = meanValues[i] / nrOfRows;
        }
        return meanValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        if (m_fromString) {
            // parse id, hex or bit strings
            int stringColumnPos = inData[0].getDataTableSpec().findColumnIndex(
                    m_stringColumn.getStringValue());
            BufferedDataTable[] bfdt = createBitVectorsFromStrings(
                    inData[0], stringColumnPos, exec);
            if (!m_factory.wasSuccessful() && inData[0].getRowCount() > 0) {
                throw new IllegalArgumentException(
                        "Errors during conversion of data in column '"
                        + inData[0].getDataTableSpec().getColumnSpec(
                                stringColumnPos).getName()
                        + "'. Check node description for supported input "
                        + "formats!");
            }
            return bfdt;

        } else {
            BufferedDataTable[] bfdt = createBitVectorsFromNumericData(
                    inData[0], exec);
            return bfdt;
        }
    }

    private BufferedDataTable[] createBitVectorsFromNumericData(
            final BufferedDataTable data,
            final ExecutionContext exec)
            throws CanceledExecutionException {
        DataColumnSpec colSpec =
                createNumericOutputSpec(data.getDataTableSpec());
        // get the indices for included columns
        List<Integer>colIndices = new ArrayList<Integer>();
        for (String colName : m_includedColumns.getIncludeList()) {
            int index = data.getDataTableSpec().findColumnIndex(colName);
            if (index < 0) {
                throw new IllegalArgumentException(
                        "Column " + colName + " is not available in " 
                        + "current data. Please re-configure the node.");
            }
            colIndices.add(index);
        }
        // calculate bits from numeric data
        if (m_useMean) {
            // either from a percentage of the mean
            double[] meanValues = new double[0];
            double meanFactor = m_meanPercentage / 100.0;
            meanValues = calculateMeanValues(data);
            m_factory =
                    new Numeric2BitVectorMeanCellFactory(colSpec,
                            meanValues, meanFactor, colIndices);
        } else {
            // or dependend on fixed threshold
            m_factory =
                    new Numeric2BitVectorThresholdCellFactory(colSpec,
                            m_threshold, colIndices);
        }
        ColumnRearranger c = new ColumnRearranger(data.getDataTableSpec());
        c.append(m_factory);
        if (m_replace) {
            List<String>includeList = m_includedColumns.getIncludeList();
            c.remove(includeList.toArray(new String[includeList.size()]));
        }
        BufferedDataTable out = exec.createColumnRearrangeTable(data, c,
                exec);
        return new BufferedDataTable[]{out};
    }
    
    private BufferedDataTable[] createBitVectorsFromStrings(
            final BufferedDataTable data,
            final int stringColIndex, final ExecutionContext exec)
            throws CanceledExecutionException, InvalidSettingsException {
        ColumnRearranger c = createColumnRearranger(data.getDataTableSpec(),
                stringColIndex);
        ExecutionMonitor creationExec = exec;
        if (m_type.equals(STRING_TYPES.ID)) {
            ExecutionMonitor scanExec = exec.createSubProgress(0.5);
            creationExec = exec.createSubProgress(0.5);
            exec.setMessage("preparing");
            int maxPos = scanMaxPos(data, scanExec);
            ((IdString2BitVectorCellFactory)m_factory).setMaxPos(maxPos);
        }
        exec.setMessage("creating output");
        BufferedDataTable out = exec.createColumnRearrangeTable(data, c,
                creationExec);
        return new BufferedDataTable[]{out};
    }

    private int scanMaxPos(final BufferedDataTable data,
            final ExecutionMonitor exec) {
        int maxPos = Integer.MIN_VALUE;
        int cellIdx = data.getDataTableSpec().findColumnIndex(
                m_stringColumn.getStringValue());
        int nrRows = data.getRowCount();
        int currRow = 0;
        for (DataRow row : data) {
            currRow++;
            exec.setProgress((double)currRow / (double)nrRows,
                    "scanning row " + currRow);
            DataCell cell = row.getCell(cellIdx);
            if (cell.isMissing()) {
                continue;
            }
            if (!cell.getType().isCompatible(StringValue.class)) {
                throw new RuntimeException("Found incompatible type in row "
                + row.getKey().getString());
            }
            String toParse = ((StringValue)cell).getStringValue();
            String[] numbers = toParse.split("\\s");
            for (int i = 0; i < numbers.length; i++) {
                int pos = -1;
                try {
                    pos = Integer.parseInt(numbers[i].trim());
                    maxPos = Math.max(maxPos, pos);
                } catch (NumberFormatException nfe) {
                    // nothing to do here
                    // same exception will be logged from cell factory
                }
            }
        }
        return maxPos + 1;
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec,
            final int colIdx) throws InvalidSettingsException {
        // BW: fixed here locally: the annotation and the column name
        // are taken from input spec (21 Sep 2006)
        DataColumnSpecCreator creator = new DataColumnSpecCreator(
                spec.getColumnSpec(colIdx));
        creator.setDomain(null);
        creator.setType(DenseBitVectorCell.TYPE);
        if (!m_replace) {
            String colName = spec.getColumnSpec(colIdx).getName() + "_bits";
            creator.setName(colName);
            if (spec.containsName(colName)) {
                throw new InvalidSettingsException("Column " + colName
                        + " already exist in table!");
            }
        }
        if (m_type.equals(STRING_TYPES.BIT)) {
            m_factory = new BitString2BitVectorCellFactory(creator.createSpec(),
                    colIdx);
        } else if (m_type.equals(STRING_TYPES.HEX)) {
            m_factory = new Hex2BitVectorCellFactory(creator.createSpec(),
                    colIdx);
        } else if (m_type.equals(STRING_TYPES.ID)) {
            m_factory = new IdString2BitVectorCellFactory(creator.createSpec(),
                    colIdx);
        } else {
            throw new InvalidSettingsException(
                    "String type to parse bitvectors" + " from unknown!");
        }
        ColumnRearranger c = new ColumnRearranger(spec);
        if (m_replace) {
            c.replace(m_factory, colIdx);
        } else {
            c.append(m_factory);
        }
        return c;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_factory = null;
        m_totalNrOf0s = 0;
        m_totalNrOf1s = 0;
        m_nrOfProcessedRows = 0;
    }

    /**
     * Assume to get numeric data only. Output is one column of type BitVector.
     *
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];
        if (!m_fromString) {
            // numeric input
            // check if there is at least one numeric column selected
            if (m_includedColumns.isEnabled() 
                    && m_includedColumns.getIncludeList().isEmpty()) {
                // the includeColumns model can not be empty 
                // through the dialog (see #validateSettings)
                // only case where !m_fromString and includeColumns evaluates 
                // to true is for old workflows.
                // For backward compatiblity include all numeric columns
                // which was the behavior before 2.0
                List<String>allNumericColumns = new ArrayList<String>();
                for (DataColumnSpec colSpec : spec) {
                    if (colSpec.getType().isCompatible(DoubleValue.class)) {
                        allNumericColumns.add(colSpec.getName());
                    }
                }
                m_includedColumns.setIncludeList(allNumericColumns);
            }
            for (String inclColName : m_includedColumns.getIncludeList()) {
                DataColumnSpec colSpec = spec.getColumnSpec(inclColName);
                if (colSpec == null) {
                    throw new InvalidSettingsException(
                            "Column " + inclColName 
                            + " not found in input table. " 
                            + "Please re-configure the node.");
                }
                if (!colSpec.getType().isCompatible(
                        DoubleValue.class)) {
                    throw new InvalidSettingsException(
                            "Column " + inclColName + " is not a numeric column"
                            );
                }
            }
        } else {
            // parse from string column 
            if (m_stringColumn.getStringValue() == null) {
                throw new InvalidSettingsException(
                        "No string column selected. " 
                        + "Please (re-)configure the node.");
            }
                // -> check if selected column is a string column 
            if (!spec.containsName(m_stringColumn.getStringValue())
                    || !(spec.getColumnSpec(m_stringColumn.getStringValue())
                            .getType().isCompatible(StringValue.class))) {
                throw new InvalidSettingsException("Selected string column "
                        + m_stringColumn.getStringValue()
                        + " not in the input table");
            }
        }
        if (m_fromString) {
            int stringColIdx =
                inSpecs[0].findColumnIndex(m_stringColumn.getStringValue());
            ColumnRearranger c = createColumnRearranger(
                    inSpecs[0], stringColIdx);
            return new DataTableSpec[]{c.createSpec()};
        } else {
            // numeric input
            DataTableSpec newSpec;
            DataColumnSpec newColSpec = createNumericOutputSpec(spec);
            if (m_replace) {
                ColumnRearranger colR = new ColumnRearranger(spec);
                colR.remove(m_includedColumns.getIncludeList().toArray(
                        new String[m_includedColumns.getIncludeList().size()]));
                newSpec = new DataTableSpec(colR.createSpec(), 
                        new DataTableSpec(newColSpec));
            } else {
                newSpec = new DataTableSpec(spec, 
                        new DataTableSpec(newColSpec));
            }
            return new DataTableSpec[]{newSpec};
        }
    }


    private DataColumnSpec createNumericOutputSpec(final DataTableSpec spec) {
        String name;
        int j = 0;
        do {
            name = "BitVectors" + (j == 0 ? "" : "_" + j);
            j++;
        } while (spec.containsName(name));
        // get the names of numeric columns
        List<String> nameMapping = new ArrayList<String>();
        nameMapping.addAll(m_includedColumns.getIncludeList());
        DataColumnSpecCreator creator = new DataColumnSpecCreator(
                name, DenseBitVectorCell.TYPE);
        creator.setElementNames(nameMapping.toArray(
                new String[nameMapping.size()]));
        return creator.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, FILE_NAME);
        FileInputStream fis = new FileInputStream(f);
        NodeSettingsRO internSettings = NodeSettings.loadFromXML(fis);
        try {
            m_nrOfProcessedRows = internSettings.getInt(INT_CFG_ROWS);
            m_totalNrOf0s = internSettings.getInt(INT_CFG_NR_ZEROS);
            m_totalNrOf1s = internSettings.getInt(INT_CFG_NR_ONES);
        } catch (InvalidSettingsException ise) {
            throw new IOException(ise.getMessage());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettings internSettings = new NodeSettings(FILE_NAME);
        internSettings.addInt(INT_CFG_ROWS, m_nrOfProcessedRows);
        internSettings.addInt(INT_CFG_NR_ZEROS, m_totalNrOf0s);
        internSettings.addInt(INT_CFG_NR_ONES, m_totalNrOf1s);
        File f = new File(internDir, FILE_NAME);
        FileOutputStream fos = new FileOutputStream(f);
        internSettings.saveToXML(fos);
    }

    /**
     * @return the settings model holding the selected string column name
     */
    static SettingsModelString createStringColModel() {
        return new SettingsModelString("STRING_COLUMN", null);
    }
    
    /**
     * 
     * @return the settings model for included numeric columns
     */
    static SettingsModelFilterString createColumnFilterModel() {
        return new SettingsModelFilterString("included.numeric.columns");
    }
    
}
