/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 */
package org.knime.base.node.mine.subgroupminer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithm;
import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithmFactory;
import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRule;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.SparseBitVector;
import org.knime.core.data.vector.bitvector.SparseBitVectorCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The SubgroupMinerModel2 searches for frequent itemsets with an apriori
 * algorithm using a prefixtree structure.
 *
 * @author Fabian Dill, University of Konstanz
 * @author Iris Adae, University of Konstanz
 */
public class SubgroupMinerModel2 extends NodeModel {

    /** Config key for the column containing the transactions. */
    public static final String CFG_TRANSACTION_COL = "TRANSACTION_COLUMN";

    /** Config key for the minimum support. */
    public static final String CFG_MIN_SUPPORT = "MIN_SUPPORT";

    /** Config key for the maximal itemset length. */
    public static final String CFG_MAX_ITEMSET_LENGTH = "MAX_ITEMSET_LENGTH";

    /** Config key for the itemset type (free, closed or maximal). */
    public static final String CFG_ITEMSET_TYPE = "ITEMSET_TYPE";

    /** Config key for the sorting method. */
    public static final String CFG_SORT_BY = "SORT_BY";

    /** Config key if association rules should be output. */
    public static final String CFG_ASSOCIATION_RULES = "ASSOCIATION_RULES";

    /** Config key for the confidence of the association rules. */
    public static final String CFG_CONFIDENCE = "CONFIDENCE";

    /*------------------underlying data structure fields ---------------*/

    /** Config key for the algorithm to use. */
    public static final String CFG_UNDERLYING_STRUCT = "UNDERLYING_STRUCT";

    /* ------------------ Defaults -------------------- */

    /** Default value for the minimum support. */
    public static final double DEFAULT_MIN_SUPPORT = 0.9;

    /** Default value for the maximal itemset length. */
    public static final int DEFAULT_MAX_ITEMSET_LENGTH = 10;

    /** Default value for the confidence. */
    public static final double DEFAULT_CONFIDENCE = 0.8;

    /* ---------- fields ---------- */

    private final SettingsModelString m_transactionColumn =
            SubgroupMinerDialog2.createBitVectorColumnModel();

    private final SettingsModelDoubleBounded m_minSupport =
            SubgroupMinerDialog2.createMinSupportModel();

    private final SettingsModelIntegerBounded m_maxItemSetLength =
            SubgroupMinerDialog2.createItemsetLengthModel();

    private final SettingsModelString m_itemSetType =
            SubgroupMinerDialog2.createItemSetTypeModel();

    private final SettingsModelBoolean m_associationRules =
            SubgroupMinerDialog2.createAssociationRuleFlagModel();

    private final SettingsModelDoubleBounded m_confidence =
            SubgroupMinerDialog2.createConfidenceModel();

    private final SettingsModelString m_underlyingStruct =
            SubgroupMinerDialog2.createAlgorithmModel();

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SubgroupMinerModel2.class);

    /**
     * Creates an instance of the SubgroubMinerModel.
     */
    public SubgroupMinerModel2() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_transactionColumn.saveSettingsTo(settings);
        m_maxItemSetLength.saveSettingsTo(settings);
        m_minSupport.saveSettingsTo(settings);
        m_itemSetType.saveSettingsTo(settings);
        m_associationRules.saveSettingsTo(settings);
        m_confidence.saveSettingsTo(settings);
        m_underlyingStruct.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_transactionColumn.validateSettings(settings);
        m_maxItemSetLength.validateSettings(settings);
        m_minSupport.validateSettings(settings);
        m_itemSetType.validateSettings(settings);
        m_associationRules.validateSettings(settings);
        m_confidence.validateSettings(settings);
        m_underlyingStruct.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_transactionColumn.loadSettingsFrom(settings);
        m_maxItemSetLength.loadSettingsFrom(settings);
        m_minSupport.loadSettingsFrom(settings);
        m_itemSetType.loadSettingsFrom(settings);
        m_associationRules.loadSettingsFrom(settings);
        m_confidence.loadSettingsFrom(settings);
        m_underlyingStruct.loadSettingsFrom(settings);
    }

    private List<BitVectorValue> preprocess(final DataTable inData,
            final ExecutionMonitor exec,
            final Map<Integer, RowKey> tidRowKeyMapping,
            final AtomicInteger maxBitsetLength)
            throws CanceledExecutionException {
        int nrOfRows = 0;
        int totalNrRows = ((BufferedDataTable)inData).getRowCount();
        List<BitVectorValue> bitSets = new ArrayList<BitVectorValue>();
        int bitVectorIndex =
                inData.getDataTableSpec().findColumnIndex(
                        m_transactionColumn.getStringValue());
        if (bitVectorIndex < 0) {
            return new ArrayList<BitVectorValue>();
        }
        for (DataRow currRow : inData) {
            exec.checkCanceled();
            DataCell dc = currRow.getCell(bitVectorIndex);
            if (dc.isMissing()) {
                continue;
            }
            BitVectorValue currCell =
                    ((BitVectorValue)currRow.getCell(bitVectorIndex));
            if (currCell.length() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("bit vector in row "
                        + currRow.getKey().getString() + " is too long: "
                        + currCell.length() + ". Only bit vectors up to "
                        + Integer.MAX_VALUE + " are supported by this node.");
            }
            maxBitsetLength.set(Math.max(maxBitsetLength.get(),
                    (int)currCell.length()));
            bitSets.add(currCell);
            tidRowKeyMapping.put(nrOfRows, currRow.getKey());
            nrOfRows++;

            exec.setProgress((double)nrOfRows / (double)totalNrRows,
                    "preprocessing..." + nrOfRows);
        }
        LOGGER.debug("max length: " + maxBitsetLength.get());
        return bitSets;
    }

    /**The preprocessing of the cells, if the selected column is a collection.
     * the collection values are saved internally, and a bitvector is
     * created for each transaction.
     *
     * @param input the data table.
     * @param exec the execution context.
     * @return the list of bitvectors
     */
    private List<BitVectorValue> preprocessCollCells(
            final BufferedDataTable inData,
            final ExecutionMonitor exec,
            final List<DataCell> nameMapping,
            final Map<Integer, RowKey> tidRowKeyMapping,
            final AtomicInteger maxBitsetLength)
            throws CanceledExecutionException {

        final Map<DataCell, Integer> cell2ItemMap =
                new HashMap<DataCell, Integer>();

        int transIndex =
                inData.getDataTableSpec().findColumnIndex(
                        m_transactionColumn.getStringValue());

        for (final DataRow row : inData) {
            final DataCell cell = row.getCell(transIndex);
            if (!cell.isMissing()) {
                final CollectionDataValue colCell = (CollectionDataValue)cell;
                for (final DataCell valCell : colCell) {
                    exec.checkCanceled();
                    if (!cell2ItemMap.containsKey(valCell)) {
                        cell2ItemMap.put(valCell, new Integer(cell2ItemMap
                                .size()));
                        nameMapping.add(valCell);
                    }
                }
            }
        }

        // afterwards create the bitvectors
        int nrOfRows = 0;
        int totalNrRows = inData.getRowCount();
        List<BitVectorValue> bitSets = new ArrayList<BitVectorValue>();
        for (final DataRow row : inData) {
            exec.checkCanceled();
            DataCell dc = row.getCell(transIndex);
            if (dc.isMissing()) {
                continue;
            }
            CollectionDataValue currCell =
                    ((CollectionDataValue)row.getCell(transIndex));
            SparseBitVector bitvec = new SparseBitVector(nameMapping.size());
            for (final DataCell valCell : currCell) {
                exec.checkCanceled();
                Integer itemID = cell2ItemMap.get(valCell);
                assert (itemID != null);
                bitvec.set(itemID.intValue(), true);
            }

            if (currCell.size() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("bit vector in row "
                        + row.getKey().getString() + " is too long: "
                        + currCell.size() + ". Only bit vectors up to "
                        + Integer.MAX_VALUE + " are supported by this node.");
            }

            bitSets.add(new SparseBitVectorCellFactory(bitvec)
                            .createDataCell());
            tidRowKeyMapping.put(nrOfRows, row.getKey());
            nrOfRows++;

            exec.setProgress((double)nrOfRows / (double)totalNrRows,
                    "preprocessing..." + nrOfRows);
        }
        maxBitsetLength.set(nameMapping.size());
        LOGGER.debug("max length: " + maxBitsetLength.get());
        return bitSets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable input = inData[0];
        DataTableSpec spec = input.getDataTableSpec();
        ExecutionMonitor exec1 = exec.createSubProgress(0.5);
        ExecutionMonitor exec2 = exec.createSubProgress(0.5);
        Map<Integer, RowKey> tidRowKeyMapping = new HashMap<Integer, RowKey>();
        LinkedList<DataCell> nameMapping = new LinkedList<DataCell>();

        List<BitVectorValue> transactions;
        AtomicInteger maxBitsetLength = new AtomicInteger(0);
        if (spec.getColumnSpec(
            m_transactionColumn.getStringValue()).getType().isCompatible(
                BitVectorValue.class)) {
            transactions = preprocess(input, exec1, tidRowKeyMapping,
                    maxBitsetLength);
            List<String> columnstrings = spec.getColumnSpec(
                 m_transactionColumn.getStringValue()).getElementNames();
            for (String s : columnstrings) {
                nameMapping.add(new StringCell(s));
            }
            // fix #2505: use maximum bitset length
            maxBitsetLength.set(Math.max(maxBitsetLength.get(),
                    nameMapping.size()));
        } else if (spec.getColumnSpec(
                m_transactionColumn.getStringValue()).getType().isCompatible(
                CollectionDataValue.class)) {
            transactions = preprocessCollCells(input, exec1, nameMapping,
                    tidRowKeyMapping, maxBitsetLength);

            // for the name Mapping is taken care in the preprocessing
        } else {
            // the selected column is neither a bitvector nor a collection
            // data value.
            throw new IOException(
                    "Selected column is not a possible transaction");
        }

        AprioriAlgorithm apriori = AprioriAlgorithmFactory.getAprioriAlgorithm(
		        AprioriAlgorithmFactory.AlgorithmDataStructure
		                .valueOf(m_underlyingStruct.getStringValue()),
		        maxBitsetLength.get(), input.getRowCount());
        LOGGER.debug("support: " + m_minSupport);
        LOGGER.debug(m_minSupport + " start apriori: " + new Date());
        try{
           apriori.findFrequentItemSets(transactions,
                m_minSupport.getDoubleValue(), m_maxItemSetLength.getIntValue(),
                FrequentItemSet.Type.valueOf(m_itemSetType.getStringValue()),
                exec2);
        } catch (OutOfMemoryError oome) {
            throw new OutOfMemoryError(
        	        "Execution resulted in an out of memory error, "
        			 + "please increase the support threshold.");
        }
        LOGGER.debug("ended apriori: " + new Date());
        BufferedDataTable itemSetTable = createOutputTable(spec, exec, apriori,
                nameMapping);
        return new BufferedDataTable[]{itemSetTable};
    }

    /**
     * @return the minimum support
     */
    public double getMinSupport() {
        return m_minSupport.getDoubleValue();
    }

    // TODO fix bug 2506: implement HiLiteTranslator
    private final HiLiteHandler m_outHiLiteHandler = new HiLiteHandler();

    /** {@inheritDoc} */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_outHiLiteHandler;
    }

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//	public Set<RowKey> getKeys(final RowKey key) {
//        LOGGER.debug("getKeys for: " + key);
//        Set<RowKey> cells = new HashSet<RowKey>();
//        for (RowIterator itr = m_itemSetTable.iterator(); itr.hasNext();) {
//            DataRow currRow = itr.next();
//            if (currRow.getKey().equals(key)) {
//                LOGGER.debug("found key ");
//                for (int i = 1; i < currRow.getNumCells(); i++) {
//                    cells.add(new RowKey(currRow.getCell(i).toString()));
//                }
//            }
//        }
//        LOGGER.debug("mapping: " + cells);
//        return cells;
//    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//	public Set<RowKey> keySet() {
//        return Collections.unmodifiableSet(new LinkedHashSet<RowKey>(
//                m_tidRowKeyMapping.values()));
//    }

    private BufferedDataTable createOutputTable(
            final DataTableSpec inputSpec, final ExecutionContext exec,
            final AprioriAlgorithm apriori,
            final List<DataCell> nameMapping)
            throws CanceledExecutionException {
        if (m_associationRules.getBooleanValue()) {
            return createAssociationRulesOutput(inputSpec, exec, apriori,
                    nameMapping);
        }
        return createFrequentItemsetOutput(inputSpec, exec, apriori,
                nameMapping);
    }

    /**
     * @param apriori
     * @return a DataTable containing the frequent item sets and their support
     */
    private BufferedDataTable createFrequentItemsetOutput(
            final DataTableSpec spec, final ExecutionContext exec,
            final AprioriAlgorithm apriori, final List<DataCell> nameMapping)
            throws CanceledExecutionException {

        DataTableSpec outSpec = createItemsetOutputSpec(spec);
        BufferedDataContainer itemRows = exec.createDataContainer(outSpec);
        assert nameMapping != null;
        List<FrequentItemSet> freqSets =
                apriori.getFrequentItemSets(FrequentItemSet.Type
                        .valueOf(m_itemSetType.getStringValue()));

        // iterate over set list
        int rowKeyCounter = 0;
        for (FrequentItemSet set : freqSets) {
            exec.setProgress((double)rowKeyCounter / (double)freqSets.size(),
                    "creating output table. Row number: " + rowKeyCounter);
            exec.checkCanceled();
            Set<DataCell> itemList = new HashSet<DataCell>();
            for (int i = 0; i < set.getItems().size(); i++) {
                Integer item = set.getItems().get(i);
                // for every item look at the referring column name
                DataCell itemName;
                if (nameMapping.size() > item) {
                    itemName = nameMapping.get(item);
                } else {
                    itemName = new StringCell("item" + item);
                }
                itemList.add(itemName);
            }
            // create for every set a row
            DataCell supp = new DoubleCell(set.getSupport());
            DataCell itemCell = CollectionCellFactory.createSetCell(itemList);
            DataRow row = new DefaultRow("item set "
                        + (rowKeyCounter++), supp, itemCell);
            itemRows.addRowToTable(row);
        }

         itemRows.close();
        return itemRows.getTable();
    }

    private BufferedDataTable createAssociationRulesOutput(
            final DataTableSpec inputSpec,
            final ExecutionContext exec, final AprioriAlgorithm apriori,
            final List<DataCell> nameMapping) {
        DataTableSpec outSpec = createAssociationRulesSpec(inputSpec);
        BufferedDataContainer ruleRows = exec.createDataContainer(outSpec);
        assert nameMapping != null;
        List<AssociationRule> associationRules =
                apriori.getAssociationRules(m_confidence.getDoubleValue());
        // for every association rule
        int rowKeyCounter = 0;
        for (AssociationRule r : associationRules) {
            // get the support
            double support = r.getSupport();
            // get the confidence
            double confidence = r.getConfidence();
            // get lift
            double lift = r.getLift();
            // get the antecedence (which is one item) -> cell
            FrequentItemSet antecedent = r.getAntecedent();
            // get the consequence
            FrequentItemSet consequent = r.getConsequent();

            DataCell[] allCells = new DataCell[6];
            allCells[0] = new DoubleCell(support);
            allCells[1] = new DoubleCell(confidence);
            allCells[2] = new DoubleCell(lift);
            // consequent is always only one item -> access with get(0) ok
            if (nameMapping.size() > consequent.getItems().get(0)) {
                allCells[3] = nameMapping.get(consequent.getItems().get(0));
            } else {
                allCells[3] =
                        new StringCell("Item" + consequent.getItems().get(0));
            }
            allCells[4] = new StringCell("<---");

            Set<DataCell> allcells = new HashSet<DataCell>();
            for (int i = 0; i < antecedent.getItems().size()
                    && i < m_maxItemSetLength.getIntValue() + 5; i++) {
                if (nameMapping.size()
                        > antecedent.getItems().get(i)) {
                    allcells.add(nameMapping.get(antecedent
                            .getItems().get(i)));
                } else {
                    allcells.add(new StringCell("Item"
                            + antecedent.getItems().get(i)));
                }
            }
            allCells[5] = CollectionCellFactory.createSetCell(allcells);
            if (antecedent.getItems().size() > 0) {
                DataRow row =
                        new DefaultRow("rule" + (rowKeyCounter++), allCells);
                ruleRows.addRowToTable(row);
            }
        }
        ruleRows.close();
        return ruleRows.getTable();
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    	// empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check if there is at least one BitVector column
        boolean hasTransactionColumn = false;
        boolean autoguessed = false;
        boolean autoconfigured = false;
        DataTableSpec tableSpec = inSpecs[0];
        for (int i = 0; i < tableSpec.getNumColumns(); i++) {
            if (tableSpec.getColumnSpec(i).getType().isCompatible(
                    BitVectorValue.class)
                    || tableSpec.getColumnSpec(i).getType().isCompatible(
                            CollectionDataValue.class)) {
                hasTransactionColumn = true;
                if (autoconfigured) {
                    autoguessed = true;
                    autoconfigured = false;
                }
                if (m_transactionColumn.getStringValue().equals("")) {
                    m_transactionColumn.setStringValue(tableSpec.getColumnSpec(
                            i).getName());
                    autoconfigured = true;
                }
            }
        }
        if (!hasTransactionColumn) {
            throw new InvalidSettingsException(
                    "Expecting at least one column containing transactions,"
                            + "(BitVectors or CollectionCells)");
        }
        if (autoguessed) {
            setWarningMessage("Auto-guessed the transaction column: "
                    + m_transactionColumn.getStringValue());
        }
        if (m_transactionColumn.getStringValue().equals("")
                || !tableSpec
                        .containsName(m_transactionColumn.getStringValue())) {
            throw new InvalidSettingsException(
                    "Set the column with the transactions");
        }
        DataTableSpec outputSpec;
        if (m_associationRules.getBooleanValue()) {
            outputSpec = createAssociationRulesSpec(tableSpec);
        } else {
            outputSpec = createItemsetOutputSpec(tableSpec);
        }

        return new DataTableSpec[]{outputSpec};
    }

    private DataTableSpec createItemsetOutputSpec(final DataTableSpec spec) {
        /*
         * creating the output spec with (maxDepth + 1) String columns and the
         * first column as an int column (the support)
         */
        DataColumnSpec[] colSpecs = new DataColumnSpec[2];
        DataColumnSpecCreator colspeccreator =
                new DataColumnSpecCreator("Support(0-1):", DoubleCell.TYPE);
        colspeccreator.setDomain(new DataColumnDomainCreator(new DoubleCell(0),
                new DoubleCell(1)).createDomain());
        colSpecs[0] = colspeccreator.createSpec();

        DataType transType = spec.getColumnSpec(
                m_transactionColumn.getStringValue()).getType();
        DataType transCollType = transType.getCollectionElementType();
        colSpecs[1] = new DataColumnSpecCreator("Items",
            transCollType == null ? SetCell.getCollectionType(StringCell.TYPE)
                    : SetCell.getCollectionType(transCollType)).createSpec();
        return new DataTableSpec(colSpecs);
    }

    private DataTableSpec createAssociationRulesSpec(final DataTableSpec spec) {
        DataType transType = spec.getColumnSpec(
            m_transactionColumn.getStringValue()).getType();

        // now create the table spec
        DataColumnSpec[] colSpecs = new DataColumnSpec[6];
        DataColumnSpecCreator creator =
                new DataColumnSpecCreator("Support", DoubleCell.TYPE);
        colSpecs[0] = creator.createSpec();
        creator = new DataColumnSpecCreator("Confidence", DoubleCell.TYPE);
        colSpecs[1] = creator.createSpec();
        creator = new DataColumnSpecCreator("Lift", DoubleCell.TYPE);
        colSpecs[2] = creator.createSpec();
        DataType transCollType = transType.getCollectionElementType();
        creator = new DataColumnSpecCreator("Consequent",
                transCollType == null ? StringCell.TYPE : transCollType);
        colSpecs[3] = creator.createSpec();
        creator = new DataColumnSpecCreator("implies", StringCell.TYPE);
        colSpecs[4] = creator.createSpec();
        creator = new DataColumnSpecCreator("Items",
                transCollType == null ? SetCell.getCollectionType(
                StringCell.TYPE) : SetCell.getCollectionType(transCollType));
        colSpecs[5] = creator.createSpec();
        return new DataTableSpec(colSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        // empty
    }
}
