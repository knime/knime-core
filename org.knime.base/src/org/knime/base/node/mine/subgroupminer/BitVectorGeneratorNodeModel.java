/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import java.util.BitSet;
import java.util.List;

import org.knime.base.data.bitvector.BitString2BitVectorCellFactory;
import org.knime.base.data.bitvector.BitVectorCell;
import org.knime.base.data.bitvector.Hex2BitVectorCellFactory;
import org.knime.base.data.bitvector.IdString2BitVectorCellFactory;
import org.knime.base.data.replace.ReplacedColumnsTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

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

    /** Config key for the StringColumn to parse. */
    public static final String CFG_STRING_COLUMN = "STRING_COLUMN";

    /** Config key for the type of string (Hex, Id, Bit). */
    public static final String CFG_STRING_TYPE = "stringType";

    /** Config key whether the threshold is used for the mean. */
    public static final String CFG_USE_MEAN = "useMean";

    /** Config key for the mean percentage. */
    public static final String CFG_MEAN_THRESHOLD = "meanThreshold";

    /** Default value for the threshold. */
    public static final double DEFAULT_THRESHOLD = 1.0;

    private double m_threshold = DEFAULT_THRESHOLD;

    private boolean m_fromString = false;

    private boolean m_useMean = false;

    private int m_meanPercentage = 100;

    private STRING_TYPES m_type = STRING_TYPES.BIT;

    private String m_stringColumn;

    private static final String FILE_NAME = "bitVectorParams";

    private DataTableSpec m_outSpec;

    private int m_nrOfProcessedRows = 0;

    private int m_bitVectorLength;

    private int m_totalNrOf1s;

    private int m_totalNrOf0s;

    private static final String INT_CFG_ROWS = "nrOfRows";

    private static final String INT_CFG_LENGTH = "bitVectorLength";

    private static final String INT_CFG_NR_ZEROS = "nrOfZeros";

    private static final String INT_CFG_NR_ONES = "nrOfOnes";

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
        return m_nrOfProcessedRows;
    }

    /**
     * 
     * @return the number of 1s generated
     */
    public int getTotalNrOf1s() {
        return m_totalNrOf1s;
    }

    /**
     * 
     * @return the number of 0s generated
     */
    public int getTotalNrOf0s() {
        return m_totalNrOf0s;
    }

    /**
     * 
     * @return the (fixed) length of the resulting bitvector
     */
    public int getBitVectorLength() {
        return m_bitVectorLength;
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo( NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addDouble(CFG_THRESHOLD, m_threshold);
        settings.addBoolean(CFG_FROM_STRING, m_fromString);
        settings.addString(CFG_STRING_COLUMN, m_stringColumn);
        settings.addBoolean(CFG_USE_MEAN, m_useMean);
        settings.addInt(CFG_MEAN_THRESHOLD, m_meanPercentage);
        settings.addString(CFG_STRING_TYPE, m_type.name());
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings( NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getDouble(CFG_THRESHOLD);
        settings.getBoolean(CFG_USE_MEAN);
        settings.getInt(CFG_MEAN_THRESHOLD);
        boolean fromString = settings.getBoolean(CFG_FROM_STRING);
        String stringCol = settings.getString(CFG_STRING_COLUMN);
        if (fromString && stringCol == null) {
            throw new InvalidSettingsException(
                    "A String column must be specified!");
        }
        settings.getString(CFG_STRING_TYPE);
    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(
     *      NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_threshold = settings.getDouble(CFG_THRESHOLD);
        m_fromString = settings.getBoolean(CFG_FROM_STRING);
        m_stringColumn = settings.getString(CFG_STRING_COLUMN);
        m_useMean = settings.getBoolean(CFG_USE_MEAN);
        m_meanPercentage = settings.getInt(CFG_MEAN_THRESHOLD);
        m_type = STRING_TYPES.valueOf(settings.getString(CFG_STRING_TYPE));
    }

    private double[] calculateMeanValues(final DataTable input) {
        double[] meanValues = new double[input.getDataTableSpec()
                .getNumColumns()];
        int nrOfRows = 0;
        for (DataRow row : input) {
            for (int i = 0; i < row.getNumCells(); i++) {
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
     * @see org.knime.core.node.NodeModel#execute( BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable numericValues = inData[0];
        List<String> nameMapping = new ArrayList<String>();
        m_nrOfProcessedRows = numericValues.getRowCount();
        for (int i = 0; i < numericValues.getDataTableSpec().getNumColumns(); 
            i++) {
            nameMapping.add(numericValues.getDataTableSpec().getColumnSpec(i)
                    .getName().toString());
        }

        if (m_fromString) {
            int stringColumnPos = inData[0].getDataTableSpec().findColumnIndex(
                    m_stringColumn);
            BufferedDataTable[] bfts = exec.createBufferedDataTables(
                    createBitVectorsFromStrings(inData[0], stringColumnPos),
                    exec);
            return bfts;

        }
        double[] meanValues = new double[0];
        double meanFactor = m_meanPercentage / 100.0;
        if (m_useMean) {
            meanValues = calculateMeanValues(inData[0]);

        }
        m_bitVectorLength = inData[0].getDataTableSpec().getNumColumns();
        long rowNr = 0;
        long rowCnt = numericValues.getRowCount();
        BufferedDataContainer cont = exec.createDataContainer(m_outSpec);
        for (RowIterator itr = numericValues.iterator(); itr.hasNext();) {
            exec.checkCanceled();
            exec.setProgress((double)rowNr / (double)rowCnt,
                    "Processing row nr.:" + rowNr);
            rowNr++;
            DataRow currRow = itr.next();
            BitSet currBitSet = new BitSet(currRow.getNumCells());
            for (int i = 0; i < currRow.getNumCells(); i++) {
                if (currRow.getCell(i).isMissing()) {
                    m_totalNrOf0s++;
                    continue;
                }
                double currValue = ((DoubleValue)currRow.getCell(i))
                        .getDoubleValue();
                if (!m_useMean) {
                    if (currValue >= m_threshold) {
                        currBitSet.set(i);
                        m_totalNrOf1s++;
                    } else {
                        m_totalNrOf0s++;
                    }
                } else {
                    if (currValue >= (meanFactor * meanValues[i])) {
                        currBitSet.set(i);
                        m_totalNrOf1s++;
                    } else {
                        m_totalNrOf0s++;
                    }
                }
            }
            DataRow bitSetRow = new DefaultRow(currRow.getKey(),
                    new DataCell[]{new BitVectorCell(currBitSet, currRow
                            .getNumCells(), nameMapping)});
            cont.addRowToTable(bitSetRow);
        }
        cont.close();
        return new BufferedDataTable[]{cont.getTable()};
    }

    private DataTable[] createBitVectorsFromStrings(
            final BufferedDataTable data,

            final int stringColIndex) {
        DataColumnSpecCreator creator = new DataColumnSpecCreator("BitVectors",
                BitVectorCell.TYPE);
        DataColumnSpec colSpec = creator.createSpec();
        DataTable result = null;
        if (m_type.equals(STRING_TYPES.BIT)) {
            BitString2BitVectorCellFactory factory 
                = new BitString2BitVectorCellFactory();
            result = new ReplacedColumnsTable(data, colSpec, stringColIndex,
                    factory);
            m_totalNrOf0s = factory.getNumberOfNotSetBits();
            m_totalNrOf1s = factory.getNumberOfSetBits();
        } else if (m_type.equals(STRING_TYPES.HEX)) {
            Hex2BitVectorCellFactory factory = new Hex2BitVectorCellFactory();
            result = new ReplacedColumnsTable(data, creator.createSpec(),
                    stringColIndex, factory);
            m_totalNrOf0s = factory.getNumberOfNotSetBits();
            m_totalNrOf1s = factory.getNumberOfSetBits();
        } else if (m_type.equals(STRING_TYPES.ID)) {
            IdString2BitVectorCellFactory factory 
                = new IdString2BitVectorCellFactory();
            result = new ReplacedColumnsTable(data, colSpec, stringColIndex,
                    factory);
            m_totalNrOf0s = factory.getNumberOfNotSetBits();
            m_totalNrOf1s = factory.getNumberOfSetBits();
        }
        return new DataTable[]{result};
    }


    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * Assume to get numeric data only. Output is one column of type BitVector.
     * 
     * @see org.knime.core.node.NodeModel#configure(
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];
        if (!m_fromString) {
            for (int i = 0; i < spec.getNumColumns(); i++) {
                if (!spec.getColumnSpec(i).getType().isCompatible(
                        DoubleValue.class)) {
                    throw new InvalidSettingsException(
                            "Only numeric columns are allowed. "
                                    + "Found non-numeric column at index " + i);
                }
            }
        } else {
            if (!spec.containsName(m_stringColumn)) {
                throw new InvalidSettingsException("Selected column "
                        + m_stringColumn + " not in the input specs");
            }
        }
        DataColumnSpec bitVectorColSpec = new DataColumnSpecCreator(
                "BitVectors", BitVectorCell.TYPE).createSpec();
        m_outSpec = new DataTableSpec(new DataColumnSpec[]{bitVectorColSpec});
        return new DataTableSpec[]{m_outSpec};
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File f = new File(internDir, FILE_NAME);
        FileInputStream fis = new FileInputStream(f);
        NodeSettingsRO internSettings = NodeSettings.loadFromXML(fis);
        try {
            m_nrOfProcessedRows = internSettings.getInt(INT_CFG_ROWS);
            m_bitVectorLength = internSettings.getInt(INT_CFG_LENGTH);
            m_totalNrOf0s = internSettings.getInt(INT_CFG_NR_ZEROS);
            m_totalNrOf1s = internSettings.getInt(INT_CFG_NR_ONES);
        } catch (InvalidSettingsException ise) {
            throw new IOException(ise.getMessage());
        }
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettings internSettings = new NodeSettings(FILE_NAME);
        internSettings.addInt(INT_CFG_ROWS, m_nrOfProcessedRows);
        internSettings.addInt(INT_CFG_LENGTH, m_bitVectorLength);
        internSettings.addInt(INT_CFG_NR_ZEROS, m_totalNrOf0s);
        internSettings.addInt(INT_CFG_NR_ONES, m_totalNrOf1s);
        File f = new File(internDir, FILE_NAME);
        FileOutputStream fos = new FileOutputStream(f);
        internSettings.saveToXML(fos);
    }
}
