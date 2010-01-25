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
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import java.util.ArrayList;

import org.knime.base.node.mine.sota.SotaConfigKeys;
import org.knime.base.node.mine.sota.distances.DistanceManager;
import org.knime.base.node.mine.sota.distances.DistanceManagerFactory;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaManager {
    /**
     * Default value of the Winner learningrate.
     */
    public static final double LR_WINNER = 0.1;

    /**
     * Minimal value of the Winner learningrate.
     */
    public static final double LR_WINNER_MIN = Double.MIN_VALUE;

    /**
     * Maximal value of the Winner learningrate.
     */
    public static final double LR_WINNER_MAX = 1.0;

    /**
     * Default value of the ancestor learningrate.
     */
    public static final double LR_ANCESTOR = 0.05;

    /**
     * Minimal value of the ancestor learningrate.
     */
    public static final double LR_ANCESTOR_MIN = Double.MIN_VALUE;

    /**
     * Maximal value of the ancestor learningrate.
     */
    public static final double LR_ANCESTOR_MAX = 1.0;

    /**
     * Default value of the sister learningrate.
     */
    public static final double LR_SISTER = 0.01;

    /**
     * Minimal value of the ancestor learningrate.
     */
    public static final double LR_SISTER_MIN = Double.MIN_VALUE;

    /**
     * Maximal value of the ancestor learningrate.
     */
    public static final double LR_SISTER_MAX = 1.0;

    /**
     * Default value of minimal variability.
     */
    public static final double MIN_VARIABILITY = 0.0;

    /**
     * Minimal value of minimal variability.
     */
    public static final double MIN_VARIABILITY_MIN = 0.0;

    /**
     * Maximal value of minimal variability.
     */
    public static final double MIN_VARIABILITY_MAX = Double.MAX_VALUE;

    /**
     * Default value of minimal resource.
     */
    public static final double MIN_RESOURCE = 0.01;

    /**
     * Minimal value of minimal resource.
     */
    public static final double MIN_RESOURCE_MIN = 0.0001;

    /**
     * Maximal value of minimal resource.
     */
    public static final double MIN_RESOURCE_MAX = Double.MAX_VALUE;

    /**
     * Default value of minimal error.
     */
    public static final double MIN_ERROR = 0.1;

    /**
     * Minimal value of minimal error.
     */
    public static final double MIN_ERROR_MIN = 0.0;

    /**
     * Maximal value of minimal error.
     */
    public static final double MIN_ERROR_MAX = 1.0;

    /**
     * Default flag setting.
     */
    public static final boolean USE_VARIABILITY = false;


    /**
     * Is hierarchical fuzzy data used or not .
     */
    public static final boolean USE_HIERARCHICAL_FUZZY_DATA = false;

    private SotaTreeCell m_root;

    private int m_dimension;

    private double m_learningrateWinner;

    private double m_learningrateAncestor;

    private double m_learningrateSister;

    private double m_minVariability;

    private double m_minResource;

    private double m_minError;

    private boolean m_useVariability;

    private double m_currentMaxResource;

    private SotaTreeCell m_currentMaxResourceCell;

    private int m_epoch;

    private int m_cycle;

    private DataArray m_inDataContainer;

    private ExecutionMonitor m_exec;

    private boolean m_isFuzzy;

    private SotaHelper m_helper;

    private DistanceManager m_distanceManager;

    private String m_distance;

    private boolean m_useHierarchicalFuzzyData;

    private boolean m_trained;

    private int m_currentHierarchyLevel;

    private int m_maxHierarchicalLevel;

    private DataArray m_origData;

    private double m_state = 0;
    
    private int m_indexOfClassColumn = -1;

    /**
     * Creates new instance of SotaManager with default settings.
     */
    public SotaManager() {
        this.m_root = null;
        this.m_inDataContainer = null;
        this.m_dimension = 0;
        this.m_epoch = 0;
        this.m_cycle = 0;
        this.m_isFuzzy = false;
        this.m_helper = null;
        this.m_currentMaxResource = 0;
        this.m_currentHierarchyLevel = 1;
        this.m_maxHierarchicalLevel = 1;

        this.m_learningrateWinner = SotaManager.LR_WINNER;
        this.m_learningrateAncestor = SotaManager.LR_ANCESTOR;
        this.m_learningrateSister = SotaManager.LR_SISTER;

        this.m_minError = SotaManager.MIN_ERROR;
        this.m_minResource = SotaManager.MIN_RESOURCE;
        this.m_minVariability = SotaManager.MIN_VARIABILITY;

        this.m_useVariability = SotaManager.USE_VARIABILITY;
        this.m_distance = DistanceManagerFactory.EUCLIDEAN_DIST;
        this.m_useHierarchicalFuzzyData 
            = SotaManager.USE_HIERARCHICAL_FUZZY_DATA;
        this.m_trained = false;

        this.m_exec = null;
    }

    /**
     * Resets the SotaManager.
     */
    public void reset() {
        m_root = null;
        m_cycle = 0;
        m_epoch = 0;
        m_currentMaxResourceCell = null;
        m_currentMaxResource = 0;
        m_dimension = 0;
        m_inDataContainer = null;
        m_trained = false;
        m_currentHierarchyLevel = 1;
        m_state = 0;
    }

    /**
     * Initializes the tree by creating the root node and two children cells of
     * the root node. The nodes data are the mean values of the input data
     * rows.
     * 
     * @param inData the table with the input data
     * @param originalData the original data
     * @param exec the execution monitor to set
     * @param indexOfClassColumn The index of the column containing the class
     * information. If value is -1 class values are ignored.
     * @throws CanceledExecutionException if user canceled the process
     */
    public void initializeTree(final DataTable inData,
            final DataArray originalData, final ExecutionMonitor exec, 
            final int indexOfClassColumn) throws CanceledExecutionException {

        this.m_indexOfClassColumn = indexOfClassColumn;
        this.m_origData = originalData;
        this.m_exec = exec;
        this.m_inDataContainer = new DefaultDataArray(inData, 1,
                Integer.MAX_VALUE);

        m_exec.checkCanceled();
        m_state += 0.01;
        m_exec.setProgress(m_state, "Preparing data");

        //
        // / Check for Fuzzy DataCells
        //
        this.m_isFuzzy = false;
        for (int i = 0; i < m_inDataContainer.getDataTableSpec()
                .getNumColumns(); i++) {

            DataType type = m_inDataContainer.getDataTableSpec().getColumnSpec(
                    i).getType();

            if (SotaUtil.isFuzzyIntervalType(type)) {
                this.m_isFuzzy = true;
            }
        }
        if (m_useHierarchicalFuzzyData) {
            this.m_isFuzzy = true;
            this.m_inDataContainer = new FuzzyHierarchyFilterRowContainer(
                    m_inDataContainer, m_currentHierarchyLevel);
            this.m_maxHierarchicalLevel = ((FuzzyHierarchyFilterRowContainer)
                    m_inDataContainer).getMaxLevel();
        }

        //
        /// Create distance metric
        //
        double offset = 1;
        m_distanceManager = DistanceManagerFactory.createDistanceManager(
                m_distance, m_isFuzzy, offset);

        //
        // / Create concrete specialized SotaHelper here !!!
        //
        if (this.m_isFuzzy) {
            m_helper = new SotaFuzzyHelper(m_inDataContainer, m_exec);
        } else {
            m_helper = new SotaNumberHelper(m_inDataContainer, m_exec);
        }

        m_exec.checkCanceled();
        
        // Count all number cells in rows of row container
        m_dimension = m_helper.initializeDimension();

        // initialize root and children node/cells
        m_root = m_helper.initializeTree();
        m_root.setLevel(1);

        m_exec.checkCanceled();
        
        // assign all Data to the root cell which have no missing values
        for (int i = 0; i < m_inDataContainer.size(); i++) {
            if (m_root.getDataIds().indexOf(new Integer(i)) == -1) {
                DataRow row = m_inDataContainer.getRow(i);
                if (!SotaUtil.hasMissingValues(row)) {
                    m_root.getDataIds().add(new Integer(i));
                }
            }

            m_exec.checkCanceled();
            m_state += 0.1 / m_inDataContainer.size();
            m_exec.setProgress(m_state, "Assigning data");
        }
        // assign the data to the children of the root cell
        assignDataToChildren(m_root);
    }

    /**
     * Trains the tree as many cycles as it takes to reduce the variability
     * value to the given minimum and returns the variability value.
     * 
     * @return the variability value
     * @throws CanceledExecutionException if training has been canceled
     */
    public double doTraining() throws CanceledExecutionException {
        double var = 0;

        while (m_currentHierarchyLevel <= m_maxHierarchicalLevel) {
            m_exec.checkCanceled();
            var = doCycle();
            double maxDeltaVar = var - m_minVariability;
            double maxDeltaRes = m_currentMaxResource - m_minResource;

            if (m_useVariability) {
                while (var > m_minVariability) {

                    m_state = (1.0 - ((var - m_minVariability) / maxDeltaVar));
                    m_exec.setProgress(m_state, "Cycle: " + m_cycle
                            + " has been trained ");

                    m_exec.checkCanceled();
                    var = doCycle();
                }
            } else {
                while (m_currentMaxResource > m_minResource) {
                    m_state = 1.0 - ((m_currentMaxResource - m_minResource) 
                            / maxDeltaRes);
                    m_exec.setProgress(m_state, "Cycle: " + m_cycle
                            + " has been trained ");

                    m_exec.checkCanceled();
                    var = doCycle();
                }
            }

            m_currentHierarchyLevel++;
            if (m_useHierarchicalFuzzyData) {
                ArrayList<SotaTreeCell> cells = new ArrayList<SotaTreeCell>();
                SotaManager.getCells(cells, m_root);

                cleanDataIdsOfCells(cells);

                ((FuzzyHierarchyFilterRowContainer)m_inDataContainer)
                        .setHierarchyLevel(m_currentHierarchyLevel);

                assignNewData(cells);
            }
        }

        m_exec.setProgress(1.0, "Training is finished");

        m_trained = true;
        return var;
    }

    /**
     * Computes one cycle of the sota algorithm, does the spilt afterwards and
     * returns the variability.
     * 
     * @return the variability of the tree after this cycle
     * @throws CanceledExecutionException if user canceled the process
     */
    public double doCycle() throws CanceledExecutionException {
        //
        // / Train as many epochs as it takes to reduce the relative tree
        // / error to a given threshold (m_minError).
        //
        double error1 = doEpoch();
        double error2 = doEpoch();
        double errorRel = Math.abs((error2 - error1) / error1);

        while (errorRel >= this.m_minError) {
            m_exec.checkCanceled();
            error1 = error2;
            error2 = doEpoch();
            errorRel = Math.abs((error2 - error1) / error1);
        }

        ArrayList<SotaTreeCell> cells = new ArrayList<SotaTreeCell>();
        SotaManager.getCells(cells, m_root);

        //
        // / Compute variability only if variability is really needed !!!
        // / Computation of variability is VERY time consuming (O(n)=n^2)
        //
        double var = 0;
        if (m_useVariability) {
            double tmpVar;

            for (int i = 0; i < cells.size(); i++) {
                m_exec.checkCanceled();
                tmpVar = getVariability(cells.get(i).getDataIds());
                cells.get(i).setMaxDistance(tmpVar);

                if (tmpVar > var) {
                    var = tmpVar;
                }
            }
        }

        //
        // / Spilt after training for this cycle is done
        // / Split only if variability is greater then minimal variability
        // / or resource value is greater than minimal resource value
        //
        if (m_currentMaxResourceCell != null) {
            if ((m_useVariability && var > m_minVariability)
                    || (!m_useVariability && m_currentMaxResource 
                            > m_minResource)) {

                // split cell
                m_currentMaxResourceCell.split(m_currentHierarchyLevel);

                // assign cells data to its children
                assignDataToChildren(m_currentMaxResourceCell);

                m_cycle++;
            }
        }

        return var;
    }

    /**
     * Computes one epoch of the sota algorithm and retunrs the error of the
     * tree after this epoch.
     * 
     * @return the error of the tree after this epoch
     * @throws CanceledExecutionException is execution was canceled.
     */
    public double doEpoch() throws CanceledExecutionException {
        ArrayList<SotaTreeCell> cells = new ArrayList<SotaTreeCell>();
        
        // get all cells
        SotaManager.getCells(cells, m_root);

        // 
        // Go through all cells and train then accordant to their assigned
        // data
        //
        for (int i = 0; i < cells.size(); i++) {
            for (int j = 0; j < cells.get(i).getDataIds().size(); j++) {
                m_exec.checkCanceled();
                DataRow row = m_inDataContainer.getRow(
                        cells.get(i).getDataIds().get(j));
                adjustCell(cells.get(i), row, null);
            }
        }

        //
        // / Compute the resource values of all cells
        //
        double res;
        double error = 0;
        double maxRes = 0;
        SotaTreeCell maxResCell = null;

        for (int i = 0; i < cells.size(); i++) {
            res = 0;

            for (int j = 0; j < cells.get(i).getDataIds().size(); j++) {
                m_exec.checkCanceled();
                
                int index = cells.get(i).getDataIds().get(j).intValue();
                DataRow row = m_inDataContainer.getRow(index);

                res += m_distanceManager.getDistance(row, cells.get(i));
            }

            if (res > 0) {
                res = res / cells.get(i).getDataIds().size();
                cells.get(i).setResource(res);

                error += res;

                // Get max resource value
                // but only if number of datapoints which correspond to that
                // cell is greater than 1 (so cell can still be splitted)
                if (maxRes < res && cells.get(i).getDataIds().size() > 1) {
                    maxRes = res;
                    maxResCell = cells.get(i);
                }
            }
        }

        this.m_currentMaxResource = maxRes;
        this.m_currentMaxResourceCell = maxResCell;
        m_epoch++;

        return error;
    }

    /**
     * Collects all cells of the tree recursive.
     * 
     * @param cells the ArrayList to store the cells in
     * @param currentCell the current cell to check
     */
    public static void getCells(final ArrayList<SotaTreeCell> cells,
            final SotaTreeCell currentCell) {
        if (currentCell.isCell()) {
            cells.add(currentCell);
        } else {
            SotaManager.getCells(cells, currentCell.getLeft());
            SotaManager.getCells(cells, currentCell.getRight());
        }
    }

    /**
     * Computes the variability of rows which ids are given.
     * 
     * @param ids IDs of DataRow to compute variability for
     * @return the variability value
     * @throws CanceledExecutionException if execution was canceled.
     */
    private double getVariability(final ArrayList<Integer> ids) 
    throws CanceledExecutionException {
        double maxDist = 0;
        double tmpDist;

        for (int i = 0; i < ids.size(); i++) {
            DataRow row1 = this.m_inDataContainer.getRow(ids.get(i));

            for (int j = 0; j < ids.size(); j++) {
                m_exec.checkCanceled();
                
                if (i != j) {
                    DataRow row2 = this.m_inDataContainer.getRow(ids.get(j));
                    tmpDist = m_distanceManager.getDistance(row1, row2);

                    if (tmpDist > maxDist) {
                        maxDist = tmpDist;
                    }
                }
            }
        }

        return maxDist;
    }

    /**
     * Assigns the cells DataIds to its children. This is needed after a split
     * of a cell.
     * 
     * @param cell cell with DataIds to assign to its children
     * @throws CanceledExecutionException if user canceled the process
     */
    private void assignDataToChildren(final SotaTreeCell cell) 
    throws CanceledExecutionException {
        if (cell.getDataIds().size() > 2) {
            for (int i = 0; i < cell.getDataIds().size(); i++) {
                DataRow row = m_inDataContainer.getRow(
                        cell.getDataIds().get(i));

                // find winner for current row
                SotaTreeCell winner;
                double tmpDist1, tmpDist2;

                tmpDist1 = m_distanceManager.getDistance(row, cell.getLeft());
                tmpDist2 = m_distanceManager.getDistance(row, cell.getRight());

                if (tmpDist1 > tmpDist2) {
                    winner = cell.getRight();
                } else {
                    winner = cell.getLeft();
                }

                // add data row id to winners data ids
                if (winner.getDataIds().indexOf(
                        new Integer(cell.getDataIds().get(i))) == -1) {
                    winner.getDataIds().add(
                            new Integer(cell.getDataIds().get(i)));
                }

                // get class string for row
                String cellClass = null;
                if (m_indexOfClassColumn >= 0) {
                    DataCell dataCell = row.getCell(m_indexOfClassColumn);
                    if (dataCell instanceof StringValue) {
                        cellClass = ((StringValue)dataCell).getStringValue();
                    }
                }
                
                adjustCell(winner, row, cellClass);

                m_exec.checkCanceled();
            }
        } else if (cell.getDataIds().size() == 2) {
            DataRow row1 = m_inDataContainer.getRow(cell.getDataIds().get(0));
            DataRow row2 = m_inDataContainer.getRow(cell.getDataIds().get(1));

            // add data row id to winners data ids
            if (cell.getLeft().getDataIds().indexOf(
                    new Integer(cell.getDataIds().get(0))) == -1) {
                cell.getLeft().getDataIds().add(
                        new Integer(cell.getDataIds().get(0)));
            }
            if (cell.getRight().getDataIds().indexOf(
                    new Integer(cell.getDataIds().get(1))) == -1) {
                cell.getRight().getDataIds().add(
                        new Integer(cell.getDataIds().get(1)));
            }

            // get class string for rows
            String cellClass1 = null;
            String cellClass2 = null;
            if (m_indexOfClassColumn >= 0) {
                DataCell dataCell1 = row1.getCell(m_indexOfClassColumn);
                DataCell dataCell2 = row1.getCell(m_indexOfClassColumn);
                if (dataCell1 instanceof StringValue) {
                    cellClass1 = ((StringValue)dataCell1).getStringValue();
                }
                if (dataCell2 instanceof StringValue) {
                    cellClass2 = ((StringValue)dataCell2).getStringValue();
                }
            }
            
            adjustCell(cell.getLeft(), row1, cellClass1);
            adjustCell(cell.getRight(), row2, cellClass2);
            
            m_exec.checkCanceled();
        }
    }

    /**
     * Adjusts the winner SotaTreeCell according to the given row. If its sister
     * cell is a cell too the sister and ancestor cell are adjusted too.
     * 
     * @param winner winner cell to adjust
     * @param row row to adjust winner cell with
     */
    private void adjustCell(final SotaTreeCell winner, final DataRow row, 
            final String cellClass) {
        // adjust winner weights and those of its neighbors.
        m_helper.adjustSotaCell(winner, row, m_learningrateWinner, cellClass);
        if (winner.getSister().isCell()) {
            m_helper.adjustSotaCell(winner.getSister(), row,
                    m_learningrateSister, cellClass);
            m_helper.adjustSotaCell(winner.getAncestor(), row,
                    m_learningrateAncestor, cellClass);
        }
    }

    /**
     * Removes all Data Ids from the given cells.
     * 
     * @param cells cells to remove all data Ids from
     */
    private void cleanDataIdsOfCells(final ArrayList<SotaTreeCell> cells) {
        for (int i = 0; i < cells.size(); i++) {
            // Store all RowKeys of assigned data, so that the prior
            // fuzzy rules can be hilited as well.
            for (int j = 0; j < cells.get(i).getDataIds().size(); j++) {
                int index = cells.get(i).getDataIds().get(j);
                RowKey rK = m_inDataContainer.getRow(index).getKey();
                cells.get(i).getRowKeys().add(rK);
            }
            // Delete all the assigned data, to have an empty list for the next
            // hierarchical level.
            cells.get(i).getDataIds().clear();
        }
    }

    /**
     * Assigns the row containers data to the given cells, accordant to their
     * distance to the data.
     * 
     * @param cells cells to assign data to
     * @throws CanceledExecutionException if execution was canceled.
     */
    private void assignNewData(final ArrayList<SotaTreeCell> cells) 
    throws CanceledExecutionException {
        for (int i = 0; i < m_inDataContainer.size(); i++) {
            DataRow row = m_inDataContainer.getRow(i);

            SotaTreeCell winner = null;
            double minDist = Double.MAX_VALUE;

            for (int j = 0; j < cells.size(); j++) {
                m_exec.checkCanceled();
                
                double dist = m_distanceManager.getDistance(row, cells.get(j));
                if (dist < minDist) {
                    winner = cells.get(j);
                    minDist = dist;
                }
            }

            // add data row id to winners data ids
            if (winner.getDataIds().indexOf(new Integer(i)) == -1) {
                winner.getDataIds().add(new Integer(i));
            }
        }
    }

    /**
     * @return the learningrateAncestor
     */
    public double getLearningrateAncestor() {
        return m_learningrateAncestor;
    }

    /**
     * @param ancestor the learningrateAncestor to set
     */
    public void setLearningrateAncestor(final double ancestor) {
        m_learningrateAncestor = ancestor;
    }

    /**
     * @return the learningrateSister
     */
    public double getLearningrateSister() {
        return m_learningrateSister;
    }

    /**
     * @param sister the learningrateSister to set
     */
    public void setLearningrateSister(final double sister) {
        m_learningrateSister = sister;
    }

    /**
     * @return the learningrateWinner
     */
    public double getLearningrateWinner() {
        return m_learningrateWinner;
    }

    /**
     * @param winner the learningrateWinner to set
     */
    public void setLearningrateWinner(final double winner) {
        m_learningrateWinner = winner;
    }

    /**
     * @return the minError
     */
    public double getMinError() {
        return m_minError;
    }

    /**
     * @param error the minError to set
     */
    public void setMinError(final double error) {
        m_minError = error;
    }

    /**
     * @return the minResource
     */
    public double getMinResource() {
        return m_minResource;
    }

    /**
     * @param resource the minResource to set
     */
    public void setMinResource(final double resource) {
        m_minResource = resource;
    }

    /**
     * @return the minVariability
     */
    public double getMinVariability() {
        return m_minVariability;
    }

    /**
     * @param variability the minVariability to set
     */
    public void setMinVariability(final double variability) {
        m_minVariability = variability;
    }

    /**
     * @return the dimension
     */
    public int getDimension() {
        return m_dimension;
    }

    /**
     * @return the inDataContainer
     */
    public DataArray getInDataContainer() {
        return m_inDataContainer;
    }

    /**
     * @return the root
     */
    public SotaTreeCell getRoot() {
        return m_root;
    }

    /**
     * @return the cycle
     */
    public double getCycle() {
        return m_cycle;
    }

    /**
     * @return the epoch
     */
    public double getEpoch() {
        return m_epoch;
    }

    /**
     * @return the execution monitor
     */
    public ExecutionMonitor getExecutionMonitor() {
        return m_exec;
    }

    /**
     * @return the useVariability
     */
    public boolean isUseVariability() {
        return m_useVariability;
    }

    /**
     * @param variability the useVariability to set
     */
    public void setUseVariability(final boolean variability) {
        m_useVariability = variability;
    }

    /**
     * @return the distance
     */
    public String getDistance() {
        return m_distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(final String distance) {
        this.m_distance = distance;
    }

    /**
     * @return the useHierarchicalFuzzyData
     */
    public boolean isUseHierarchicalFuzzyData() {
        return m_useHierarchicalFuzzyData;
    }

    /**
     * @param hierarchicalFuzzyData the useHierarchicalFuzzyData to set
     */
    public void setUseHierarchicalFuzzyData(
            final boolean hierarchicalFuzzyData) {
        m_useHierarchicalFuzzyData = hierarchicalFuzzyData;
    }

    /**
     * @return the trained
     */
    public boolean isTrained() {
        return m_trained;
    }

    /**
     * @return the maxHierarchicalLevel
     */
    public int getMaxHierarchicalLevel() {
        return m_maxHierarchicalLevel;
    }

    /**
     * Saves settings of algorithm to given NodeSettings object.
     * 
     * @param settings NodeSettings object to store settings in
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addDouble(SotaConfigKeys.CFGKEY_LR_WINNER, this
                .getLearningrateWinner());

        settings.addDouble(SotaConfigKeys.CFGKEY_LR_ANCESTOR, this
                .getLearningrateAncestor());

        settings.addDouble(SotaConfigKeys.CFGKEY_LR_SISTER, this
                .getLearningrateSister());

        settings.addDouble(SotaConfigKeys.CFGKEY_MIN_ERROR, this.getMinError());

        settings.addDouble(SotaConfigKeys.CFGKEY_RESOURCE, this
                .getMinResource());

        settings.addDouble(SotaConfigKeys.CFGKEY_VARIABILITY, this
                .getMinVariability());

        settings.addBoolean(SotaConfigKeys.CFGKEY_USE_VARIABILITY, this
                .isUseVariability());

        settings.addString(SotaConfigKeys.CFGKEY_USE_DISTANCE, this
                .getDistance());
    }

    /**
     * Reads settings out of given NodeSettings object and validates it. If
     * validateOnly is false, NodeSettings data will be stored in algorithm too.
     * 
     * @param settings NodeSettings object to get settings from
     * @param validateOnly if <code>true</code>, settings will be validated
     *            only, if <code>false</code>, settings will be stored in
     *            algorithm
     * @throws InvalidSettingsException will be thrown if given settings are not
     *             valid
     */
    public void readSettings(final NodeSettingsRO settings,
            final boolean validateOnly) throws InvalidSettingsException {

        // read the values into local variables first
        double lrWinner = settings.getDouble(SotaConfigKeys.CFGKEY_LR_WINNER);
        double lrSister = settings.getDouble(SotaConfigKeys.CFGKEY_LR_SISTER);
        double lrAncestor = settings
                .getDouble(SotaConfigKeys.CFGKEY_LR_ANCESTOR);

        double minError = settings.getDouble(SotaConfigKeys.CFGKEY_MIN_ERROR);
        double minVar = settings.getDouble(SotaConfigKeys.CFGKEY_VARIABILITY);
        double minRes = settings.getDouble(SotaConfigKeys.CFGKEY_RESOURCE);

        boolean useVar = settings
                .getBoolean(SotaConfigKeys.CFGKEY_USE_VARIABILITY);

        String useDist = settings.getString(SotaConfigKeys.CFGKEY_USE_DISTANCE);

        // check their correctness and completeness.
        String msg = "";
        if (lrWinner < SotaManager.LR_WINNER_MIN) {
            msg += "Learningrate for winner must be equal or greater "
                    + SotaManager.LR_WINNER_MIN + " , not " + lrWinner
                    + " ! \n";
        } else if (lrWinner > SotaManager.LR_WINNER_MAX) {
            msg += "Learningrate for winner must be equal or less "
                    + SotaManager.LR_WINNER_MAX + " , not " + lrWinner
                    + " ! \n";
        }

        if (lrSister < SotaManager.LR_SISTER_MIN) {
            msg += "Learningrate for sister must be equal or greater "
                    + SotaManager.LR_SISTER_MIN + " , not " + lrSister
                    + " ! \n";
        } else if (lrSister > SotaManager.LR_SISTER_MAX) {
            msg += "Learningrate for sister must be equal or less "
                    + SotaManager.LR_SISTER_MAX + " , not " + lrSister
                    + " ! \n";
        }

        if (lrAncestor < SotaManager.LR_ANCESTOR_MIN) {
            msg += "Learningrate for ancestor must be equal or greater "
                    + SotaManager.LR_ANCESTOR_MIN + " , not " + lrAncestor
                    + " ! \n";
        } else if (lrAncestor > SotaManager.LR_ANCESTOR_MAX) {
            msg += "Learningrate for ancestor must be equal or less "
                    + SotaManager.LR_ANCESTOR_MAX + " , not " + lrAncestor
                    + " ! \n";
        }

        if (minError < SotaManager.MIN_ERROR_MIN) {
            msg += "Minimal error must be equal or greater "
                    + SotaManager.MIN_ERROR_MIN + " , not " + minError
                    + " ! \n";
        } else if (minError > SotaManager.MIN_ERROR_MAX) {
            msg += "Minimal error must be equal or less "
                    + SotaManager.MIN_ERROR_MAX + " , not " + minError
                    + " ! \n";
        }

        if (minVar < SotaManager.MIN_VARIABILITY_MIN) {
            msg += "Minimal variability must be equal or greater "
                    + SotaManager.MIN_VARIABILITY_MIN + " , not " + minVar
                    + " ! \n";
        } else if (minVar > SotaManager.MIN_VARIABILITY_MAX) {
            msg += "Minimal variability must be equal or less "
                    + SotaManager.MIN_VARIABILITY_MAX + " , not " + minVar
                    + " ! \n";
        }

        if (minRes < SotaManager.MIN_RESOURCE_MIN) {
            msg += "Minimal resource must be equal or greater "
                    + SotaManager.MIN_RESOURCE_MIN + " , not " + minRes
                    + " ! \n";
        } else if (minRes > SotaManager.MIN_RESOURCE_MAX) {
            msg += "Minimal resource must be equal or less "
                    + SotaManager.MIN_RESOURCE_MAX + " , not " + minRes
                    + " ! \n";
        }

        if (!useDist.equals(DistanceManagerFactory.EUCLIDEAN_DIST)
                && !useDist.equals(DistanceManagerFactory.COS_DIST)) {
            msg += "Distance must be euclidean or coeffizient of correlation "
                    + "!\n";
        }

        //
        // / Throw exception and warn if errors in settings
        //
        if (msg.length() > 0) {
            throw new InvalidSettingsException(msg);
        }

        // now take them over - if we are supposed to.
        if (!validateOnly) {
            this.m_learningrateWinner = lrWinner;
            this.m_learningrateAncestor = lrAncestor;
            this.m_learningrateSister = lrSister;
            this.m_minVariability = minVar;
            this.m_minError = minError;
            this.m_minResource = minRes;
            this.m_useVariability = useVar;
            this.m_distance = useDist;
        }
    }

    /**
     * Returns the original DataTableSpec.
     * 
     * @return the original DataTableSpec
     */
    public DataArray getOriginalData() {
        return m_origData;
    }

    /**
     * Sets the maximum heirarchical level.
     * 
     * @param maxLevel the maximum heirarchical level to set
     */
    public void setMaxHierarchicalLevel(final int maxLevel) {
        m_maxHierarchicalLevel = maxLevel;
    }

    /**
     * Sets the root node.
     * 
     * @param root the root node to set
     */
    public void setRoot(final SotaTreeCell root) {
        m_root = root;
    }

    /**
     * Sets in data.
     * 
     * @param inData in data to set
     */
    public void setInData(final DataArray inData) {
        m_inDataContainer = inData;
    }

    /**
     * Sets original data.
     * 
     * @param origData original data to set
     */
    public void setOriginalData(final DataArray origData) {
        m_origData = origData;
    }
}
