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
 *   30.11.2005 (dill): created
 */
package org.knime.base.node.mine.subgroupminer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.bitvector.BitVectorValue;
import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithm;
import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithmFactory;
import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRule;
import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRuleModel;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSetModel;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSetRow;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSetTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteMapper;

/**
 * The SubgroupMinerModel searches for frequent itemsets with an apriori
 * algorithm using a prefixtree structure.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class SubgroupMinerModel extends NodeModel implements HiLiteMapper {
    /* ------------the input -------------------- */
    /** Config key for the column containing the transactions as bitvectors. */
    public static final String CFG_BITVECTOR_COL = "BITVECTOR_COLUMN";

    /* ------------------------output fields------------------------- */
    /** Config key for the minimum support. */
    public static final String CFG_MIN_SUPPORT = "MIN_SUPPORT";

    /** Config key for the maximal itemset length. */
    public static final String CFG_MAX_ITEMSET_LENGTH = "MAX_ITEMSET_LENGTH";

    /**
     * Config key for the itemset type (free, closed or maximal).
     */
    public static final String CFG_ITEMSET_TYPE = "ITEMSET_TYPE";

    /**
     * Config key for the sorting method.
     */
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
    private String m_bitVectorColumn;

    private double m_minSupport = DEFAULT_MIN_SUPPORT;

    private int m_maxItemSetLength = DEFAULT_MAX_ITEMSET_LENGTH;

    private FrequentItemSet.Type m_itemSetType = FrequentItemSet.Type.CLOSED;

    private FrequentItemSetTable.Sorter m_sorter 
        = FrequentItemSetTable.Sorter.NONE;

    private boolean m_associationRules = false;

    private double m_confidence = DEFAULT_CONFIDENCE;

    private AprioriAlgorithmFactory.AlgorithmDataStructure m_underlyingStruct 
        = AprioriAlgorithmFactory.AlgorithmDataStructure.ARRAY;

    private BufferedDataTable m_itemSetTable;

    private AprioriAlgorithm m_apriori;

    private List<String> m_nameMapping;

    private Map<Integer, DataCell> m_tidRowKeyMapping;

    private int m_nrOfRows;

    private int m_maxBitsetLength;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SubgroupMinerModel.class);

    /**
     * Creates an instance of the SubgroubMinerModel.
     */
    public SubgroupMinerModel() {
        super(1, 1, 0, 1);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // input
        settings.addString(CFG_BITVECTOR_COL, m_bitVectorColumn);

        // output
        settings.addInt(CFG_MAX_ITEMSET_LENGTH, m_maxItemSetLength);
        settings.addDouble(CFG_MIN_SUPPORT, m_minSupport);
        settings.addString(CFG_ITEMSET_TYPE, m_itemSetType.name());
        settings.addString(CFG_SORT_BY, m_sorter.name());
        settings.addBoolean(CFG_ASSOCIATION_RULES, m_associationRules);
        settings.addDouble(CFG_CONFIDENCE, m_confidence);
        // data structure:
        settings.addString(CFG_UNDERLYING_STRUCT, m_underlyingStruct.name());
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        settings.getString(CFG_BITVECTOR_COL);

        int maxLength = settings.getInt(CFG_MAX_ITEMSET_LENGTH);
        if (maxLength <= 0) {
            throw new InvalidSettingsException("Invalid max item set length: "
                    + maxLength);
        }
        double min = settings.getDouble(CFG_MIN_SUPPORT);
        if (min <= 0) {
            throw new InvalidSettingsException("Invalid min support: " + min);
        }
        settings.getString(CFG_ITEMSET_TYPE);
        settings.getString(CFG_SORT_BY);
        settings.getBoolean(CFG_ASSOCIATION_RULES);
        settings.getDouble(CFG_CONFIDENCE);
        settings.getString(CFG_UNDERLYING_STRUCT);
    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(
     *      NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // input
        m_bitVectorColumn = settings.getString(CFG_BITVECTOR_COL);

        // output
        m_minSupport = settings.getDouble(CFG_MIN_SUPPORT);
        m_maxItemSetLength = settings.getInt(CFG_MAX_ITEMSET_LENGTH);
        m_itemSetType = FrequentItemSet.Type.valueOf(settings
                .getString(CFG_ITEMSET_TYPE));
        m_sorter = FrequentItemSetTable.Sorter.valueOf(settings
                .getString(CFG_SORT_BY));
        if (m_minSupport == 0) {
            m_minSupport = DEFAULT_MIN_SUPPORT;
        }
        if (m_maxItemSetLength == 0) {
            m_maxItemSetLength = DEFAULT_MAX_ITEMSET_LENGTH;
        }
        m_associationRules = settings.getBoolean(CFG_ASSOCIATION_RULES);
        m_confidence = settings.getDouble(CFG_CONFIDENCE);
        // underlying data structure
        m_underlyingStruct = AprioriAlgorithmFactory.AlgorithmDataStructure
                .valueOf(settings.getString(CFG_UNDERLYING_STRUCT));
    }

    private List<BitSet> preprocess(final DataTable inData,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        // TODO: check in configure that only Double values are in the table
        m_tidRowKeyMapping = new HashMap<Integer, DataCell>();
        m_nrOfRows = 0;
        int totalNrRows = ((BufferedDataTable)inData).getRowCount();
        m_maxBitsetLength = 0;
        List<BitSet> bitSets = new ArrayList<BitSet>();
        int bitVectorIndex = inData.getDataTableSpec().findColumnIndex(
                m_bitVectorColumn);
        if (bitVectorIndex < 0) {
            return new ArrayList<BitSet>();
        }
        boolean first = true;
        for (RowIterator itr = inData.iterator(); itr.hasNext();) {
            exec.checkCanceled();
            DataRow currRow = itr.next();
            BitVectorValue currCell = ((BitVectorValue)currRow
                    .getCell(bitVectorIndex));
            BitSet currBitSet = currCell.getBitSet();
            m_maxBitsetLength = Math.max(m_maxBitsetLength, currCell
                    .getNumBits());
            if (first) {
                m_nameMapping = currCell.getNaming();
                first = false;
            }
            bitSets.add(currBitSet);
            m_tidRowKeyMapping.put(m_nrOfRows, currRow.getKey().getId());
            m_nrOfRows++;

            exec.setProgress((double)m_nrOfRows / (double)totalNrRows,
                    "preprocessing......." + m_nrOfRows);
        }
        LOGGER.debug("max length: " + m_maxBitsetLength);
        return bitSets;
    }

    /**
     * @see org.knime.core.node.NodeModel#execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTable input = inData[0];
        ExecutionMonitor exec1 = exec.createSubProgress(0.5);
        ExecutionMonitor exec2 = exec.createSubProgress(0.5);
        List<BitSet> transactions = preprocess(input, exec1);

        m_apriori = AprioriAlgorithmFactory.getAprioriAlgorithm(
                m_underlyingStruct, m_maxBitsetLength, m_nrOfRows);
        LOGGER.debug("support: " + m_minSupport);
        LOGGER.debug(m_minSupport + " start apriori: " + new Date());
        m_apriori.findFrequentItemSets(transactions, m_minSupport,
                m_maxItemSetLength, m_itemSetType, exec2);
        LOGGER.debug("ended apriori: " + new Date());
        m_itemSetTable = createOutputTable(exec);

        return new BufferedDataTable[]{m_itemSetTable};
    }

    /**
     * Returns the frequent itemsets as a table.
     * 
     * @return the frequent itemsets
     */
    public DataTable getItemSetTable() {
        return m_itemSetTable;
    }

    /**
     * Returns the minimum support.
     * 
     * @return the minimum support
     */
    public double getMinSupport() {
        return m_minSupport;
    }

    /**
     * Returns the intern hilite handler.
     * 
     * @return the internal hilite handler
     */
    public HiLiteHandler getInternHiLiteHandler() {
        return getOutHiLiteHandler(0);
    }

    /**
     * 
     * @see org.knime.core.node.property.hilite.HiLiteMapper#getKeys(
     *      org.knime.core.data.DataCell)
     */
    public Set<DataCell> getKeys(final DataCell key) {
        LOGGER.debug("getKeys for: " + key);
        Set<DataCell> cells = new HashSet<DataCell>();
        for (RowIterator itr = m_itemSetTable.iterator(); itr.hasNext();) {
            DataRow currRow = itr.next();
            if (currRow.getKey().getId().equals(key)) {
                LOGGER.debug("found key ");
                for (int i = 1; i < currRow.getNumCells(); i++) {
                    cells.add(currRow.getCell(i));
                }
            }
        }
        LOGGER.debug("mapping: " + cells);
        return cells;
    }

    /**
     * @see org.knime.core.node.property.hilite.HiLiteMapper#keySet()
     */
    public Set<DataCell> keySet() {
        return Collections.unmodifiableSet(new LinkedHashSet<DataCell>(
                m_tidRowKeyMapping.values()));
    }

    private BufferedDataTable createOutputTable(final ExecutionContext exec)
            throws CanceledExecutionException {
        if (m_associationRules) {
            return createAssociationRulesOutput(exec);
        }
        return createFrequentItemsetOutput(exec);
    }

    /*
     * @return a DataTable cotaining the frequent itemsets and their support
     */
    private BufferedDataTable createFrequentItemsetOutput(
            final ExecutionContext exec) throws CanceledExecutionException {
        List<FrequentItemSet> freqSets = m_apriori
                .getFrequentItemSets(m_itemSetType);
        List<FrequentItemSetRow> rows = new LinkedList<FrequentItemSetRow>();
        // iterate over set list
        int rowKeyCounter = 0;
        for (FrequentItemSet set : freqSets) {
            exec.setProgress((double)rowKeyCounter / (double)m_nrOfRows,
                    "creating output table. Row number: " + rowKeyCounter);
            List<String> itemList = new ArrayList<String>();
            for (int i = 0; i < set.getItems().size(); i++) {
                if (i > m_maxItemSetLength) {
                    break;
                }
                Integer item = set.getItems().get(i);
                // for every item look at the referring column name
                String itemName;
                if (m_nameMapping != null) {
                    itemName = m_nameMapping.get(item);
                } else {
                    itemName = "item" + item;
                }
                itemList.add(itemName);
            }
            // create for every set a row
            FrequentItemSetRow row = new FrequentItemSetRow(new RowKey(
                    "item set " + rowKeyCounter++), itemList,
                    m_maxItemSetLength, set.getSupport());
            rows.add(row);
        }
        DataTableSpec outSpec = createItemsetOutputSpec();
        FrequentItemSetTable result = new FrequentItemSetTable(rows, outSpec);
        result.sortBy(m_sorter);
        return exec.createBufferedDataTable(result, exec);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveModelContent(int,
     *      org.knime.core.node.ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        assert index == 0;
        // check if the node is executed
        if (m_apriori != null) {
            if (m_associationRules) {
                AssociationRuleModel model = new AssociationRuleModel();
                model.setNameMapping(m_nameMapping);
                model.setAssociationRules(m_apriori
                        .getAssociationRules(m_confidence));
                model.saveToModelContent(predParams);

            } else {
                FrequentItemSetModel model = new FrequentItemSetModel();
                model.setNameMapping(m_nameMapping);
                model.setFrequentItemsets(m_apriori
                        .getFrequentItemSets(m_itemSetType));
                model.setDBSize(m_nrOfRows);
                model.saveToModelContent(predParams);
            }
        }
    }

    /*
     * @return -
     */
    private BufferedDataTable createAssociationRulesOutput(
            final ExecutionContext exec) {
        DataTableSpec outSpec = createAssociationRulesSpec();
        BufferedDataContainer ruleRows = exec.createDataContainer(outSpec);

        List<AssociationRule> associationRules = m_apriori
                .getAssociationRules(m_confidence);
        // for every association rule
        int rowKeyCounter = 0;
        for (AssociationRule r : associationRules) {
            // get the support
            double support = r.getSupport();
            // get the confidence
            double confidence = r.getConfidence();
            // get the antecedence (which is one item) -> cell
            List<Integer> antecedent = r.getAntecedent();
            // get the consequence
            Integer consequent = r.getConsequent();

            DataCell[] allCells = new DataCell[m_maxItemSetLength + 4];
            allCells[0] = new DoubleCell(support);
            allCells[1] = new DoubleCell(confidence);
            if (m_nameMapping != null) {
                allCells[2] = new StringCell(m_nameMapping.get(consequent));
            } else {
                allCells[2] = new StringCell("Item" + consequent);
            }
            allCells[3] = new StringCell("<---");
            for (int i = 0; i < antecedent.size() 
                && i < m_maxItemSetLength + 4; i++) {
                if (m_nameMapping != null) {
                    allCells[i + 4] = new StringCell(m_nameMapping
                            .get(antecedent.get(i)));
                } else {
                    allCells[i + 4] = new StringCell(
                            "Item" + antecedent.get(i));
                }
            }
            int start = Math.min(antecedent.size() + 4, m_maxItemSetLength + 4);
            for (int i = start; i < m_maxItemSetLength + 4; i++) {
                allCells[i] = DataType.getMissingCell();
            }
            if (antecedent.size() > 0) {
                DataRow row = new DefaultRow(new StringCell("rule"
                        + rowKeyCounter++), allCells);
                ruleRows.addRowToTable(row);
            }
        }
        ruleRows.close();
        return ruleRows.getTable();
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see org.knime.core.node.NodeModel#configure(
     *      org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check if there is at least one BitVector column
        boolean hasBitVectorColumn = false;
        for (int i = 0; i < inSpecs[0].getNumColumns(); i++) {
            if (inSpecs[0].getColumnSpec(i).getType().isCompatible(
                    BitVectorValue.class)) {
                hasBitVectorColumn = true;
            }
        }
        if (!hasBitVectorColumn) {
            throw new InvalidSettingsException(
                    "Expecting at least on BitVector column");
        }
        if (m_bitVectorColumn == null
                || !inSpecs[0].containsName(m_bitVectorColumn)) {
            throw new InvalidSettingsException(
                    "Set the column with the bit vectors");
        }
        DataTableSpec outputSpec;
        if (m_associationRules) {
            outputSpec = createAssociationRulesSpec();
        } else {
            outputSpec = createItemsetOutputSpec();
        }
        return new DataTableSpec[]{outputSpec};
    }

    private DataTableSpec createItemsetOutputSpec() {
        /*
         * creating the ouput spec with (maxDepth + 1) String columns and the
         * first column as an int colum (the support)
         */
        DataColumnSpec[] colSpecs = new DataColumnSpec[m_maxItemSetLength + 1];
        DataColumnSpecCreator colspeccreator = new DataColumnSpecCreator(
                "Support(0-1):", DoubleCell.TYPE);
        colspeccreator.setDomain(new DataColumnDomainCreator(new DoubleCell(0),
                new DoubleCell(1)).createDomain());
        colSpecs[0] = colspeccreator.createSpec();
        for (int i = 1; i < m_maxItemSetLength + 1; i++) {
            colSpecs[i] = new DataColumnSpecCreator("Item_" + i,
                    StringCell.TYPE).createSpec();
        }
        return new DataTableSpec(colSpecs);
    }

    private DataTableSpec createAssociationRulesSpec() {
        /* now create the table spec */
        DataColumnSpec[] colSpecs = new DataColumnSpec[m_maxItemSetLength + 4];
        DataColumnSpecCreator creator = new DataColumnSpecCreator("Support",
                DoubleCell.TYPE);
        colSpecs[0] = creator.createSpec();
        creator = new DataColumnSpecCreator("Confidence", DoubleCell.TYPE);
        colSpecs[1] = creator.createSpec();
        creator = new DataColumnSpecCreator("Consequent", StringCell.TYPE);
        colSpecs[2] = creator.createSpec();
        creator = new DataColumnSpecCreator("implies", StringCell.TYPE);
        colSpecs[3] = creator.createSpec();
        for (int i = 0; i < m_maxItemSetLength; i++) {
            creator = new DataColumnSpecCreator("Item" + i, StringCell.TYPE);
            colSpecs[i + 4] = creator.createSpec();
        }
        return new DataTableSpec(colSpecs);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
    }
}
