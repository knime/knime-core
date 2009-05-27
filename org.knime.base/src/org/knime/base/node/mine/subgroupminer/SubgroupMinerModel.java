/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithm;
import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithmFactory;
import org.knime.base.node.mine.subgroupminer.freqitemset.AssociationRule;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
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
import org.knime.core.data.vector.bitvector.BitVectorValue;
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
    private SettingsModelString m_bitVectorColumn 
        = SubgroupMinerDialog.createBitVectorColumnModel();

    private SettingsModelDoubleBounded m_minSupport 
        = SubgroupMinerDialog.createMinSupportModel(); 
        
    private SettingsModelIntegerBounded m_maxItemSetLength 
        = SubgroupMinerDialog.createItemsetLengthModel();

    private SettingsModelString m_itemSetType 
        = SubgroupMinerDialog.createItemSetTypeModel();

    private SettingsModelString m_sorter 
        = SubgroupMinerDialog.createSortByModel();
    
    private SettingsModelBoolean m_associationRules
        = SubgroupMinerDialog.createAssociationRuleFlagModel();
    
    private SettingsModelDoubleBounded m_confidence 
        = SubgroupMinerDialog.createConfidenceModel(); 
            
    private SettingsModelString m_underlyingStruct 
        = SubgroupMinerDialog.createAlgorithmModel();
    
    private BufferedDataTable m_itemSetTable;

    private AprioriAlgorithm m_apriori;

    private List<String> m_nameMapping;

    private Map<Integer, RowKey> m_tidRowKeyMapping;

    private int m_nrOfRows;

    private int m_maxBitsetLength;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(SubgroupMinerModel.class);

    /**
     * Creates an instance of the SubgroubMinerModel.
     */
    public SubgroupMinerModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_bitVectorColumn.saveSettingsTo(settings);
        m_maxItemSetLength.saveSettingsTo(settings);
        m_minSupport.saveSettingsTo(settings);
        m_itemSetType.saveSettingsTo(settings);
        m_sorter.saveSettingsTo(settings);
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
        m_bitVectorColumn.validateSettings(settings);
        m_maxItemSetLength.validateSettings(settings);
        m_minSupport.validateSettings(settings);
        m_itemSetType.validateSettings(settings);
        m_sorter.validateSettings(settings);
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
        m_bitVectorColumn.loadSettingsFrom(settings);
        m_maxItemSetLength.loadSettingsFrom(settings);
        m_minSupport.loadSettingsFrom(settings);
        m_itemSetType.loadSettingsFrom(settings);
        m_sorter.loadSettingsFrom(settings);
        m_associationRules.loadSettingsFrom(settings);
        m_confidence.loadSettingsFrom(settings);
        m_underlyingStruct.loadSettingsFrom(settings);
    }

    private List<BitVectorValue> preprocess(final DataTable inData,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        // TODO: check in configure that only Double values are in the table
        m_tidRowKeyMapping = new HashMap<Integer, RowKey>();
        m_nrOfRows = 0;
        int totalNrRows = ((BufferedDataTable)inData).getRowCount();
        m_maxBitsetLength = 0;
        List<BitVectorValue> bitSets = new ArrayList<BitVectorValue>();
        int bitVectorIndex = inData.getDataTableSpec().findColumnIndex(
                m_bitVectorColumn.getStringValue());
        if (bitVectorIndex < 0) {
            return new ArrayList<BitVectorValue>();
        }
        for (RowIterator itr = inData.iterator(); itr.hasNext();) {
            exec.checkCanceled();
            DataRow currRow = itr.next();
            DataCell dc = currRow.getCell(bitVectorIndex);
            if (dc.isMissing()) {
                continue;
            }
            BitVectorValue currCell = ((BitVectorValue)currRow
                    .getCell(bitVectorIndex));
            if (currCell.length() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "bit vector in row " + currRow.getKey().getString()
                        + " is too long: " + currCell.length()
                        + ". Only bit vectors up to " + Integer.MAX_VALUE
                        + " are supported by this node.");
            }
            m_maxBitsetLength = Math.max(m_maxBitsetLength, (int)currCell
                    .length());
            bitSets.add(currCell);
            m_tidRowKeyMapping.put(m_nrOfRows, currRow.getKey());
            m_nrOfRows++;

            exec.setProgress((double)m_nrOfRows / (double)totalNrRows,
                    "preprocessing......." + m_nrOfRows);
        }
        LOGGER.debug("max length: " + m_maxBitsetLength);
        return bitSets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTable input = (BufferedDataTable)inData[0];
        ExecutionMonitor exec1 = exec.createSubProgress(0.5);
        ExecutionMonitor exec2 = exec.createSubProgress(0.5);
        List<BitVectorValue> transactions = preprocess(input, exec1);

        m_nameMapping = input.getDataTableSpec().getColumnSpec(
                m_bitVectorColumn.getStringValue()).getElementNames();
        
        
        m_apriori = AprioriAlgorithmFactory.getAprioriAlgorithm(
                AprioriAlgorithmFactory.AlgorithmDataStructure.valueOf(
                        m_underlyingStruct.getStringValue()), 
                        m_maxBitsetLength, m_nrOfRows);
        LOGGER.debug("support: " + m_minSupport);
        LOGGER.debug(m_minSupport + " start apriori: " + new Date());
        m_apriori.findFrequentItemSets(transactions, 
                m_minSupport.getDoubleValue(),
                m_maxItemSetLength.getIntValue(), 
                FrequentItemSet.Type.valueOf(m_itemSetType.getStringValue()), 
                exec2);
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
        return m_minSupport.getDoubleValue();
    }


    /**
     * {@inheritDoc}
     */
    public Set<RowKey> getKeys(final RowKey key) {
        LOGGER.debug("getKeys for: " + key);
        Set<RowKey> cells = new HashSet<RowKey>();
        for (RowIterator itr = m_itemSetTable.iterator(); itr.hasNext();) {
            DataRow currRow = itr.next();
            if (currRow.getKey().equals(key)) {
                LOGGER.debug("found key ");
                for (int i = 1; i < currRow.getNumCells(); i++) {
                    cells.add(new RowKey(currRow.getCell(i).toString()));
                }
            }
        }
        LOGGER.debug("mapping: " + cells);
        return cells;
    }

    /**
     * {@inheritDoc}
     */
    public Set<RowKey> keySet() {
        return Collections.unmodifiableSet(new LinkedHashSet<RowKey>(
                m_tidRowKeyMapping.values()));
    }

    private BufferedDataTable createOutputTable(final ExecutionContext exec)
            throws CanceledExecutionException {
        if (m_associationRules.getBooleanValue()) {
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
                .getFrequentItemSets(
                        FrequentItemSet.Type.valueOf(
                                m_itemSetType.getStringValue()));
        List<FrequentItemSetRow> rows = new LinkedList<FrequentItemSetRow>();
        // iterate over set list
        int rowKeyCounter = 0;
        for (FrequentItemSet set : freqSets) {
            exec.setProgress((double)rowKeyCounter / (double)m_nrOfRows,
                    "creating output table. Row number: " + rowKeyCounter);
            List<String> itemList = new ArrayList<String>();
            for (int i = 0; i < set.getItems().size(); i++) {
                if (i > m_maxItemSetLength.getIntValue()) {
                    break;
                }
                Integer item = set.getItems().get(i);
                // for every item look at the referring column name
                String itemName;
                if (m_nameMapping != null && m_nameMapping.size() > item) {
                    itemName = m_nameMapping.get(item);
                } else {
                    itemName = "item" + item;
                }
                itemList.add(itemName);
            }
            // create for every set a row
            FrequentItemSetRow row = new FrequentItemSetRow(new RowKey(
                    "item set " + rowKeyCounter++), itemList,
                    m_maxItemSetLength.getIntValue(), set.getSupport());
            rows.add(row);
        }
        DataTableSpec outSpec = createItemsetOutputSpec();
        FrequentItemSetTable result = new FrequentItemSetTable(rows, outSpec);
        result.sortBy(FrequentItemSetTable.Sorter.valueOf(
                m_sorter.getStringValue()));
        return exec.createBufferedDataTable(result, exec);
    }

    /*
     * {@inheritDoc}
     *
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        assert index == 0;
        // check if the node is executed
        if (m_apriori != null) {
            if (m_associationRules.getBooleanValue()) {
                AssociationRuleModel model = new AssociationRuleModel();
                model.setNameMapping(m_nameMapping);
                model.setAssociationRules(m_apriori
                        .getAssociationRules(m_confidence.getDoubleValue()));
                model.saveToModelContent(predParams);

            } else {
                FrequentItemSetModel model = new FrequentItemSetModel();
                model.setNameMapping(m_nameMapping);
                model.setFrequentItemsets(m_apriori
                        .getFrequentItemSets(
                                FrequentItemSet.Type.valueOf(
                                        m_itemSetType.getStringValue())));
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
                .getAssociationRules(m_confidence.getDoubleValue());
        // for every association rule
        int rowKeyCounter = 0;
        for (AssociationRule r : associationRules) {
            // get the support
            double support = r.getSupport();
            // get the confidence
            double confidence = r.getConfidence();
            // get the antecedence (which is one item) -> cell
            FrequentItemSet antecedent = r.getAntecedent();
            // get the consequence
            FrequentItemSet consequent = r.getConsequent();

            DataCell[] allCells 
                = new DataCell[m_maxItemSetLength.getIntValue() + 4];
            allCells[0] = new DoubleCell(support);
            allCells[1] = new DoubleCell(confidence);
            // consequent is alsways only one item -> access with get(0) ok
            if (m_nameMapping != null 
                    && m_nameMapping.size() > consequent.getItems().get(0)) {
                allCells[2] = new StringCell(m_nameMapping.get(
                        consequent.getItems().get(0)));
            } else {
                allCells[2] = new StringCell(
                        "Item" + consequent.getItems().get(0));
            }
            allCells[3] = new StringCell("<---");
            for (int i = 0; i < antecedent.getItems().size() 
                && i < m_maxItemSetLength.getIntValue() + 4; i++) {
                if (m_nameMapping != null 
                        && m_nameMapping.size() > antecedent.getItems().get(i)) {
                    allCells[i + 4] = new StringCell(m_nameMapping
                            .get(antecedent.getItems().get(i)));
                } else {
                    allCells[i + 4] = new StringCell(
                            "Item" + antecedent.getItems().get(i));
                }
            }
            int start = Math.min(antecedent.getItems().size() + 4, 
                    m_maxItemSetLength.getIntValue() + 4);
            for (int i = start; i < m_maxItemSetLength.getIntValue() + 4; i++) {
                allCells[i] = DataType.getMissingCell();
            }
            if (antecedent.getItems().size() > 0) {
                DataRow row = new DefaultRow("rule"
                        + rowKeyCounter++, allCells);
                ruleRows.addRowToTable(row);
            }
        }
        ruleRows.close();
        return ruleRows.getTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }
    /*
     * TODO: activate later (switch to PMML)
    private PMMLAssociationRulePortObject createPortObject(
            final DataTableSpec spec) {
        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(
                spec);
        Set<String>learningCols = new HashSet<String>();
        learningCols.add(m_bitVectorColumn.getStringValue());
        creator.setLearningColsNames(learningCols);
        if (m_associationRules.getBooleanValue()) {
        PMMLAssociationRulePortObject portObj 
            = new PMMLAssociationRulePortObject(
                    creator.createSpec(),
                    m_apriori.getAssociationRules(
                            m_confidence.getDoubleValue()),
                    m_minSupport.getDoubleValue(),
                    m_confidence.getDoubleValue(),
                    m_nrOfRows, m_maxBitsetLength
                    );
            if (m_nameMapping != null && !m_nameMapping.isEmpty()) {
                portObj.setNameMapping(m_nameMapping);
            }
            return portObj;
        } else {
            return null;
        }
    }
    */

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        // check if there is at least one BitVector column
        boolean hasBitVectorColumn = false;
        boolean autoguessed = false;
        boolean autoconfigured = false;
        DataTableSpec tableSpec = inSpecs[0];
        for (int i = 0; i < tableSpec.getNumColumns(); i++) {
            if (tableSpec.getColumnSpec(i).getType().isCompatible(
                    BitVectorValue.class)) {
                hasBitVectorColumn = true;
                if (autoconfigured) {
                    autoguessed = true;
                    autoconfigured = false;
                }
                if (m_bitVectorColumn.getStringValue().equals("")) {
                    m_bitVectorColumn.setStringValue(
                            tableSpec.getColumnSpec(i).getName());
                    autoconfigured = true;
                }
            }
        }
        if (!hasBitVectorColumn) {
            throw new InvalidSettingsException(
                    "Expecting at least on BitVector column");
        }
        if (autoguessed) {
            setWarningMessage("Auto-guessed the bitvector column: " 
                    + m_bitVectorColumn.getStringValue());
        }
        if (m_bitVectorColumn.getStringValue().equals("")
                || !tableSpec.containsName(
                        m_bitVectorColumn.getStringValue())) {
            throw new InvalidSettingsException(
                    "Set the column with the bit vectors");
        }
        DataTableSpec outputSpec;
        if (m_associationRules.getBooleanValue()) {
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
        DataColumnSpec[] colSpecs 
            = new DataColumnSpec[m_maxItemSetLength.getIntValue() + 1];
        DataColumnSpecCreator colspeccreator = new DataColumnSpecCreator(
                "Support(0-1):", DoubleCell.TYPE);
        colspeccreator.setDomain(new DataColumnDomainCreator(new DoubleCell(0),
                new DoubleCell(1)).createDomain());
        colSpecs[0] = colspeccreator.createSpec();
        for (int i = 1; i < m_maxItemSetLength.getIntValue() + 1; i++) {
            colSpecs[i] = new DataColumnSpecCreator("Item_" + i,
                    StringCell.TYPE).createSpec();
        }
        return new DataTableSpec(colSpecs);
    }

    private DataTableSpec createAssociationRulesSpec() {
        /* now create the table spec */
        DataColumnSpec[] colSpecs 
            = new DataColumnSpec[m_maxItemSetLength.getIntValue() + 4];
        DataColumnSpecCreator creator = new DataColumnSpecCreator("Support",
                DoubleCell.TYPE);
        colSpecs[0] = creator.createSpec();
        creator = new DataColumnSpecCreator("Confidence", DoubleCell.TYPE);
        colSpecs[1] = creator.createSpec();
        creator = new DataColumnSpecCreator("Consequent", StringCell.TYPE);
        colSpecs[2] = creator.createSpec();
        creator = new DataColumnSpecCreator("implies", StringCell.TYPE);
        colSpecs[3] = creator.createSpec();
        for (int i = 0; i < m_maxItemSetLength.getIntValue(); i++) {
            creator = new DataColumnSpecCreator("Item" + i, StringCell.TYPE);
            colSpecs[i + 4] = creator.createSpec();
        }
        return new DataTableSpec(colSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
    }
}
