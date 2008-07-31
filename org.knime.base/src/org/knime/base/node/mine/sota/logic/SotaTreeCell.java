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
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.logic;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.knime.base.node.mine.sota.view.interaction.Hiliteable;
import org.knime.base.node.mine.sota.view.interaction.Locatable;
import org.knime.base.node.mine.sota.view.interaction.Selectable;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaTreeCell implements Locatable, Hiliteable, Selectable,
        Serializable {
    
    private static final String CFG_KEY_IS_CELL = "IsCell";
    
    private static final String CFG_KEY_DATA = "Data";
    
    private static final String CFG_KEY_CLASS = "CellClass";
    
    private static final String CFG_KEY_LEFT = "Left";
    
    private static final String CFG_KEY_RIGHT = "Right";
    
    private static final String CFG_KEY_RESOURCE = "Resource";
    
    private static final String CFG_KEY_MAX_DISTANCE = "MaxDistance";
    
    private static final String CFG_KEY_LEVEL = "Level";
    
    private static final String CFG_KEY_H_LEVEL = "HierarchyLevel";
    
    private static final String CFG_KEY_LEVEL_IN_H = "LevelInHierarchy";
    
    private static final String CFG_KEY_DATA_ID = "DataId";
    
    private static final String CFG_KEY_ROW_KEY = "RowKey";
    
    private static final String CFG_KEY_START_X = "StartX";
    
    private static final String CFG_KEY_END_X = "EndX";
    
    private static final String CFG_KEY_START_Y = "StartY";
    
    private static final String CFG_KEY_END_Y = "EndY";
    
    /**
     * Default cell class.
     */
    public static final String DEFAULT_CLASS = "NoClassDefined";
    
    private SotaCell[] m_data;

    private boolean m_isCell;

    private SotaTreeCell m_left;

    private SotaTreeCell m_right;

    private SotaTreeCell m_ancestor;

    private SotaTreeCell m_sister;

    private double m_resource = 0;

    private double m_maxDistance = 0;

    private int m_level = 0;

    private int m_hierarchyLevel = 0;

    private int m_levelInHierarchy = 0;

    private ArrayList<Integer> m_dataIds;
    
    private CellClassCounter m_classCounter = new CellClassCounter();

    // this list is only used when dealing with hierarchical fuzzy data.
    // It stores the RowKeys of the fuzzy rules of the prior levels, so that
    // rules of the prior levels can be hilited, as well as the rules of the
    // final level.
    private ArrayList<RowKey> m_rowKeys;

    // Related to interface Locatable
    private int m_startY = Locatable.X;

    private int m_startX = Locatable.Y;

    private int m_endY = Locatable.X;

    private int m_endX = Locatable.Y;

    // Related to interface Hilitable
    private transient boolean m_hilited = false;

    // Related to interface Selectable
    private transient boolean m_selected = false;
    
    /**
     * Creates new instance of Cell with given dimension of data vector, given
     * level of hierarchy and given <code>isCell</code> flag.
     * 
     * @param dimension Dimension of the data vector.
     * @param lev hierarchy level of the cell
     * @param isCell flags if cell is a Cell or a Node
     */
    public SotaTreeCell(final int dimension, final int lev, 
            final boolean isCell) {
        this.m_data = new SotaCell[dimension];
        this.m_level = lev;
        this.m_dataIds = new ArrayList<Integer>();
        this.m_isCell = isCell;
        this.m_rowKeys = new ArrayList<RowKey>();
    }

    /**
     * Creates new instance of Cell with given dimension of data vector and
     * given isCell flag. The hierarchz level ist set to 0 by default.
     * 
     * @param dimension dimension of the data vector
     * @param isCell flags if cell is a Cell or a Node
     */
    public SotaTreeCell(final int dimension, final boolean isCell) {
        this(dimension, 0, isCell);
    }

    /**
     * Creates new instance of Cell with given vector data, given level of
     * hierarchy and given isCell flag.
     * 
     * @param dat data to set to the data vector
     * @param lev hierarchy level of the cell
     * @param isCell flags if Cell is a Cell or a Node
     */
    public SotaTreeCell(final SotaCell[] dat, final int lev,
            final boolean isCell) {
        this.m_data = dat;
        this.m_level = lev;
        this.m_dataIds = new ArrayList<Integer>();
        this.m_isCell = isCell;
        this.m_rowKeys = new ArrayList<RowKey>();
    }

    /**
     * Creates new instance of Cell with given vector data and given isCell
     * flag. The level of hierarchy is set to 0 by default.
     * 
     * @param dat data to set to the data vector
     * @param isCell flags if cell is a Cell or a Node
     */
    public SotaTreeCell(final SotaCell[] dat, final boolean isCell) {
        this(dat, 0, isCell);
    }
    
    /**
     * @return the class of the tree cell.
     */
    public String getTreeCellClass() {
        String cellClass = m_classCounter.getMostFrequentClass();
        if (cellClass == null) {
            return DEFAULT_CLASS;
        }
        return cellClass;
    }

    /**
     * @param treeCellClass the class to assign to the tree cell.
     */
    public void addTreeCellClass(final String treeCellClass) {
        if (treeCellClass != null) {
            m_classCounter.addClass(treeCellClass);
        }
    }

    /**
     * Splits the current cell that it becomes a Node. The child cells have the
     * same data as the ancestor and the given level. The maximum distnace and
     * resource values are set to 0 be default and the data Id vector is empty.
     * 
     * @param lev the level to set to the newly created cells
     * @param hierarchicalLev the fuzzy rule level (is only used when training
     *            with hierarchical fuzzy data)
     */
    public void split(final int lev, final int hierarchicalLev) {
        if (m_isCell) {

            SotaCell[] dataRight = new SotaCell[m_data.length];
            SotaCell[] dataLeft = new SotaCell[m_data.length];
            for (int i = 0; i < m_data.length; i++) {
                dataRight[i] = m_data[i].clone();
                dataLeft[i] = m_data[i].clone();
            }

            int levelInHierarchy = m_levelInHierarchy + 1;
            if (m_hierarchyLevel != hierarchicalLev) {
                levelInHierarchy = 1;
            }

            m_right = new SotaTreeCell(dataRight, lev, true);
            m_right.setAncestor(this);
            m_right.setMaxDistance(0);
            m_right.setResource(0);
            m_right.setHierarchyLevel(hierarchicalLev);
            m_right.setLevelInHierarchy(levelInHierarchy);

            m_left = new SotaTreeCell(dataLeft, lev, true);
            m_left.setAncestor(this);
            m_left.setMaxDistance(0);
            m_left.setResource(0);
            m_left.setHierarchyLevel(hierarchicalLev);
            m_left.setLevelInHierarchy(levelInHierarchy);

            m_left.setSister(m_right);
            m_right.setSister(m_left);

            m_isCell = false;
        }
    }

    /**
     * Splits the current cell that it becomes a Node. The child cells have the
     * same data and level as the ancestor. The maximum distance and resource
     * values are set to 0 be default and the data Id vector is empty.
     * 
     * @param hierarchicalLev the fuzzy rule level (is only used when training
     *            with hierarchical fuzzy data)
     */
    public void split(final int hierarchicalLev) {
        split(m_level + 1, hierarchicalLev);
    }

    /**
     * Splits the current cell that it becomes a Node. The child cells have the
     * same data and level as the ancestor. The maximum distance and resource
     * values are set to 0 be default the data Id vector is empty and the
     * hierarchy level is set to 1.
     */
    public void split() {
        split(m_level + 1, 1);
    }

    /**
     * Returns the cells data as String. The values are comma separated and the
     * given decimal precision defines the decimal precision of the values.
     * 
     * @param precision the decimal precision to use for the values
     * @return the cells data as string
     */
    public String getDataAsString(final int precision) {
        StringBuffer buf = new StringBuffer();

        String nu = "";
        for (int i = 0; i < precision; i++) {
            nu += "0";
        }

        DecimalFormat df = new DecimalFormat("############################."
                + nu);

        for (int i = 0; i < m_data.length; i++) {
            buf.append(df.format(m_data[i].getValue()));
            buf.append(", ");
        }

        return buf.toString();
    }

    /**
     * Returns the cells ancestor.
     * 
     * @return the cells ancestor
     */
    public SotaTreeCell getAncestor() {
        return m_ancestor;
    }

    /**
     * Sets the given ancestor value.
     * 
     * @param anc the cells ancestor to set
     */
    public void setAncestor(final SotaTreeCell anc) {
        this.m_ancestor = anc;
    }

    /**
     * @return the sister
     */
    public SotaTreeCell getSister() {
        return m_sister;
    }

    /**
     * @param sister The sister to set.
     */
    public void setSister(final SotaTreeCell sister) {
        this.m_sister = sister;
    }

    /**
     * Returns the cells data array.
     * 
     * @return the cells data array
     */
    public SotaCell[] getData() {
        return m_data;
    }

    /**
     * Sets the given data array.
     * 
     * @param dat the cells data array to set
     */
    public void setData(final SotaCell[] dat) {
        this.m_data = dat;
    }

    /**
     * Returns the cells Ids of according data sets.
     * 
     * @return the cells Ids of according data sets
     */
    public ArrayList<Integer> getDataIds() {
        return m_dataIds;
    }

    /**
     * Sets the given Ids of according data sets.
     * 
     * @param ids the cells Ids of according data sets to set
     */
    public void setDataIds(final ArrayList<Integer> ids) {
        this.m_dataIds = ids;
    }

    /**
     * Returns <code>true</code> if the cell is a Cell, <code>false</code>
     * if it is a Node.
     * 
     * @return <code>true</code> if the cell is a Cell, <code>false</code>
     *         if it is a Node
     */
    public boolean isCell() {
        return m_isCell;
    }

    /**
     * Sets the given isCell flag. If <code>true</code> it is a Cell else it
     * is a Node.
     * 
     * @param cell if <code>true</code> it is a Cell else it is a Node
     */
    public void setCell(final boolean cell) {
        this.m_isCell = cell;
    }

    /**
     * Returns the left child Cell of the current Node, or <code>null</code>.
     * 
     * @return the left child Cell of the current Node, or <code>null</code>
     */
    public SotaTreeCell getLeft() {
        return m_left;
    }

    /**
     * Sets the cells (Nodes) left child Cell.
     * 
     * @param l the cells (Nodes) left child Cell to set
     */
    public void setLeft(final SotaTreeCell l) {
        this.m_left = l;
    }

    /**
     * Returns the cells level of hierarchy inside the binary tree.
     * 
     * @return the cells level of hierarchy inside the binary tree
     */
    public int getLevel() {
        return m_level;
    }

    /**
     * Sets the cells level of hierarchy inside the binary tree.
     * 
     * @param lev the cells level of hierarchy inside the binary tree to set
     */
    public void setLevel(final int lev) {
        this.m_level = lev;
    }

    /**
     * Returns the maximum distance between the data related to the cell.
     * 
     * @return the maximum distance between the data related to the cell
     */
    public double getMaxDistance() {
        return m_maxDistance;
    }

    /**
     * Sets the given value as maximum distance between the data related to the
     * cell.
     * 
     * @param maxDist the maximum distance between the data related to the cell
     *            to set
     */
    public void setMaxDistance(final double maxDist) {
        this.m_maxDistance = maxDist;
    }

    /**
     * Returns the cells resource value.
     * 
     * @return the cells resource value
     */
    public double getResource() {
        return m_resource;
    }

    /**
     * Sets the given value as the cells resource value.
     * 
     * @param res the value to set as the cells resource value
     */
    public void setResource(final double res) {
        this.m_resource = res;
    }

    /**
     * Returns the cells (Nodes) right child Cell.
     * 
     * @return the cells (Nodes) right child Cell
     */
    public SotaTreeCell getRight() {
        return m_right;
    }

    /**
     * Sets the given Cell as the cells right child Cell.
     * 
     * @param r the Cell to set as the cells right child Cell
     */
    public void setRight(final SotaTreeCell r) {
        this.m_right = r;
    }

    /**
     * {@inheritDoc}
     */
    public int getStartX() {
        return m_startX;
    }

    /**
     * {@inheritDoc}
     */
    public int getStartY() {
        return m_startY;
    }

    /**
     * {@inheritDoc}
     */
    public int getEndX() {
        return m_endX;
    }

    /**
     * {@inheritDoc}
     */
    public int getEndY() {
        return m_endY;
    }

    /**
     * {@inheritDoc}
     */
    public void setStartX(final int x) {
        m_startX = x;
    }

    /**
     * {@inheritDoc}
     */
    public void setStartY(final int y) {
        m_startY = y;
    }

    /**
     * {@inheritDoc}
     */
    public void setEndX(final int x) {
        m_endX = x;
    }

    /**
     * {@inheritDoc}
     */
    public void setEndY(final int y) {
        m_endY = y;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHilited() {
        return m_hilited;
    }

    /**
     * {@inheritDoc}
     */
    public void setHilited(final boolean hilit) {
        setHilitedRec(this, hilit);
    }

    /**
     * Sets the hilit flag to the cell without recursive method call.
     * 
     * @param hilit flag to set
     */
    public void setCellHilited(final boolean hilit) {
        m_hilited = hilit;
    }

    /**
     * Sets the hilit flag to all the cells children recursively.
     * 
     * @param cell current cell to set the hilit flag
     * @param hilit flag to set
     */
    private void setHilitedRec(final SotaTreeCell cell, final boolean hilit) {
        cell.setCellHilited(hilit);
        if (!cell.isCell()) {
            setHilitedRec(cell.getLeft(), hilit);
            setHilitedRec(cell.getRight(), hilit);
        }
    }

    /**
     * @return the hierarchyLevel
     */
    public int getHierarchyLevel() {
        return m_hierarchyLevel;
    }

    /**
     * @param level the hierarchyLevel to set
     */
    public void setHierarchyLevel(final int level) {
        m_hierarchyLevel = level;
    }

    /**
     * @return the levelInHierarchy
     */
    public int getLevelInHierarchy() {
        return m_levelInHierarchy;
    }

    /**
     * @param levelInHierarchy the levelInHierarchy to set
     */
    public void setLevelInHierarchy(final int levelInHierarchy) {
        m_levelInHierarchy = levelInHierarchy;
    }

    /**
     * @return the rowKeys
     */
    public ArrayList<RowKey> getRowKeys() {
        return m_rowKeys;
    }

    /**
     * @return the type of the cells data (fuzzy or double / number).
     */
    public String getCellType() {
        if (m_data.length > 0) {
            return m_data[0].getType();
        }
        return null;
    }
    
    /**
     * Returns the RowKeys as a string separated with ",".
     * 
     * @return the RowKeys as a string separated with ","
     */
    public String getRowKeysAsString() {
        StringBuffer rk = new StringBuffer();
        for (int i = 0; i < m_rowKeys.size(); i++) {
            rk.append(m_rowKeys.get(i).toString());
            if (i < m_rowKeys.size() - 1) {
                rk.append(", ");
            }
        }
        return rk.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSelected() {
        return m_selected;
    }

    /**
     * {@inheritDoc}
     */
    public void setSelected(final boolean select) {
        setSelectedRec(this, select);
    }

    /**
     * Sets the selected flag to all the cells children recursively.
     * 
     * @param cell current cell to set the selected flag
     * @param select flag to set
     */
    private void setSelectedRec(final SotaTreeCell cell, final boolean select) {
        if (cell.isSelected() != select) {
            cell.setCellSelected(select);
        } else {
            cell.setCellSelected(!select);
        }
        if (!cell.isCell()) {
            setSelectedRec(cell.getLeft(), select);
            setSelectedRec(cell.getRight(), select);
        }
    }

    /**
     * Sets the select flag to the cell without recursive method call.
     * 
     * @param select flag to set
     */
    public void setCellSelected(final boolean select) {
        m_selected = select;
    }

    /**
     * Deselects cell and all its children and subchildren recursively.
     */
    public void deselectSubtree() {
        m_selected = false;
        if (!m_isCell) {
            getLeft().deselectSubtree();
            getRight().deselectSubtree();
        }
    }    
    
    /**
     * Saves the value of the <code>SotaTreeCell</code> to the given 
     * <code>ModelContentWO</code>.
     * 
     * @param modelContent The <code>ModelContentWO</code> to save the cells
     * to. 
     * @param index The index of the cell to save.
     */
    public final void saveTo(final ModelContentWO modelContent, 
            final int index) {
        int ind = index;
        ind++;
        
        // save resource ect.
        modelContent.addBoolean(CFG_KEY_IS_CELL, m_isCell);
        modelContent.addDouble(CFG_KEY_RESOURCE, m_resource);
        modelContent.addDouble(CFG_KEY_MAX_DISTANCE, m_maxDistance);
        
        // save level information
        modelContent.addInt(CFG_KEY_LEVEL, m_level);
        modelContent.addInt(CFG_KEY_H_LEVEL, m_hierarchyLevel);
        modelContent.addInt(CFG_KEY_LEVEL_IN_H, m_levelInHierarchy);
        
        // save coordinates
        modelContent.addInt(CFG_KEY_START_X, m_startX);
        modelContent.addInt(CFG_KEY_START_Y, m_startY);
        modelContent.addInt(CFG_KEY_END_X, m_endX);
        modelContent.addInt(CFG_KEY_END_Y, m_endY);
        
        // save data ids
        int dataIdCount = 0;
        modelContent.addInt(CFG_KEY_DATA_ID + "SIZE", m_dataIds.size());
        for (Integer i : m_dataIds) {
            modelContent.addInt(CFG_KEY_DATA_ID + dataIdCount, i);
            dataIdCount++;
        }
        
        // save row keys
        modelContent.addInt(CFG_KEY_ROW_KEY + "SIZE", m_rowKeys.size());
        int rowKeyCount = 0;
        for (RowKey r : m_rowKeys) {
            modelContent.addString(CFG_KEY_ROW_KEY + rowKeyCount, r.toString());
            rowKeyCount++;
        }
        
        // save cell data
        modelContent.addInt(CFG_KEY_DATA + "SIZE", m_data.length);
        if (m_data.length > 0) {
            modelContent.addString(CFG_KEY_DATA + "TYPE", m_data[0].getType());
        }
        for (int i = 0; i < m_data.length; i++) {
            ModelContentWO subContent = modelContent.addModelContent(
                    CFG_KEY_DATA + i);
            m_data[i].saveTo(subContent);
        }
                
        // save data
        modelContent.addString(CFG_KEY_CLASS, 
                m_classCounter.getMostFrequentClass());
        
        // save left and right
        if (m_left != null) {
            modelContent.addBoolean("HAS" + CFG_KEY_LEFT, true);
            ModelContentWO subContent = modelContent.addModelContent(
                    CFG_KEY_LEFT + ind);
            m_left.saveTo(subContent, ind);
        } else {
            modelContent.addBoolean("HAS" + CFG_KEY_LEFT, false);
        }
        if (m_right != null) {
            modelContent.addBoolean("HAS" + CFG_KEY_RIGHT, true);
            ModelContentWO subContent = modelContent.addModelContent(
                    CFG_KEY_RIGHT + ind);
            m_right.saveTo(subContent, ind);            
        } else {
            modelContent.addBoolean("HAS" + CFG_KEY_RIGHT, false);
        }
    }    
    
    
    /**
     * Loads the values from the given <code>ModelContentWO</code>.
     * 
     * @param modelContent The <code>ModelContentWO</code> to load the cells 
     * from.
     * @param index The index of the cell to load.
     * @param anchestor The anchsetor cell of the cell to load.
     * @param isLeft Specifies if the cell to load is a cell at the left side of
     * its anchestor.
     * 
     * @throws InvalidSettingsException If setting to load is not valid.
     */
    public void loadFrom(final ModelContentRO modelContent, final int index,
            final SotaTreeCell anchestor, final boolean isLeft) 
    throws InvalidSettingsException {
        
        int ind = index;
        ind++;
                
        // load resource etc.
        m_isCell = modelContent.getBoolean(CFG_KEY_IS_CELL);
        m_resource = modelContent.getDouble(CFG_KEY_RESOURCE);
        m_maxDistance = modelContent.getDouble(CFG_KEY_MAX_DISTANCE);
        
        // load level information
        m_level = modelContent.getInt(CFG_KEY_LEVEL);
        m_hierarchyLevel = modelContent.getInt(CFG_KEY_H_LEVEL);
        m_levelInHierarchy = modelContent.getInt(CFG_KEY_LEVEL_IN_H);
        
        // load coordinates
        m_startX = modelContent.getInt(CFG_KEY_START_X);
        m_startY = modelContent.getInt(CFG_KEY_START_Y);
        m_endX = modelContent.getInt(CFG_KEY_END_X);
        m_endY = modelContent.getInt(CFG_KEY_END_Y);
     
        // load data ids
        int size = modelContent.getInt(CFG_KEY_DATA_ID + "SIZE");
        m_dataIds = new ArrayList<Integer>(); 
        int dataIdCount = 0;
        for (int i = 0; i < size; i++) {
            m_dataIds.add(modelContent.getInt(CFG_KEY_DATA_ID + dataIdCount));
            dataIdCount++;
        }
        
        // load row keys
        size = modelContent.getInt(CFG_KEY_ROW_KEY + "SIZE");
        m_rowKeys = new ArrayList<RowKey>();
        int rowKeyCount = 0;
        for (int i = 0; i < size; i++) {
            m_rowKeys.add(new RowKey(modelContent.getString(
                    CFG_KEY_ROW_KEY + rowKeyCount)));
            rowKeyCount++;
        }
        
        // load data
        String type = "";
        size = modelContent.getInt(CFG_KEY_DATA + "SIZE");
        if (size > 0) {
            type = modelContent.getString(CFG_KEY_DATA + "TYPE");
            m_data = new SotaCell[size];
        }
        for (int i = 0; i < size; i++) {
            ModelContentRO subContent = modelContent.getModelContent(
                    CFG_KEY_DATA + i);
            m_data[i] = SotaCellFactory.createSotaCell(type);
            m_data[i].loadFrom(subContent);
        }        
        
        // load data
        String cellClass = modelContent.getString(CFG_KEY_CLASS);
        m_classCounter = new CellClassCounter();
        m_classCounter.addClass(cellClass);
        
        // load left and right
        boolean hasLeft = modelContent.getBoolean("HAS" + CFG_KEY_LEFT);
        boolean hasRight = modelContent.getBoolean("HAS" + CFG_KEY_RIGHT);
        if (hasLeft) {
            ModelContentRO subContent = modelContent.getModelContent(
                    CFG_KEY_LEFT + ind);
            m_left = new SotaTreeCell(0, true);
            m_left.loadFrom(subContent, ind, this, true);
        } else {
            m_left = null;
        }
        if (hasRight) {
            ModelContentRO subContent = modelContent.getModelContent(
                    CFG_KEY_RIGHT + ind);
            m_right = new SotaTreeCell(0, true);
            m_right.loadFrom(subContent, ind, this, false);            
        } else {
            m_right = null;
        }
        
        // set anchestor
        if (anchestor != null) {
            this.setAncestor(anchestor);
        }
        // set sister
        if (!isLeft) {
            if (this.getAncestor() != null) {
                SotaTreeCell leftSister = this.getAncestor().getLeft();
                if (leftSister != null) {
                    this.setSister(leftSister);
                    leftSister.setSister(this);
                }
            }
        }
    }
}
