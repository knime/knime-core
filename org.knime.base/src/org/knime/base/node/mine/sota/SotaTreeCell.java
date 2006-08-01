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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 16, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.knime.base.node.mine.sota.view.Hiliteable;
import org.knime.base.node.mine.sota.view.Locatable;
import org.knime.base.node.mine.sota.view.Selectable;
import org.knime.core.data.RowKey;

/**
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaTreeCell implements Locatable, Hiliteable, Selectable,
        Serializable {
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
    private boolean m_hilited = false;

    // Related to interface Selectable
    private boolean m_selected = false;

    /**
     * Creates new instance of Cell with given dimension of data vector, given
     * level of hierarchy and given <code>isCell</code> flag.
     * 
     * @param dimension Dimension of the data vector.
     * @param lev hierarchy level of the cell
     * @param isCell flags if cell is a Cell or a Node
     */
    public SotaTreeCell(final int dimension, final int lev, final boolean isCell) {
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
     * @see de.unikn.knime.dev.node.sota.view.Locatable#getStartX()
     */
    public int getStartX() {
        return m_startX;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Locatable#getStartY()
     */
    public int getStartY() {
        return m_startY;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Locatable#getEndX()
     */
    public int getEndX() {
        return m_endX;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Locatable#getEndY()
     */
    public int getEndY() {
        return m_endY;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Locatable#setStartX(int)
     */
    public void setStartX(final int x) {
        m_startX = x;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Locatable#setStartY(int)
     */
    public void setStartY(final int y) {
        m_startY = y;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Locatable#setEndX(int)
     */
    public void setEndX(final int x) {
        m_endX = x;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Locatable#setEndY(int)
     */
    public void setEndY(final int y) {
        m_endY = y;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Hiliteable#isHilited()
     */
    public boolean isHilited() {
        return m_hilited;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Hiliteable#setHilited(boolean)
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
     * @see de.unikn.knime.dev.node.sota.view.Selectable#isSelected()
     */
    public boolean isSelected() {
        return m_selected;
    }

    /**
     * @see de.unikn.knime.dev.node.sota.view.Selectable#setSelected(boolean)
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
     * Writes first its siblings serialized to given stream, and then itself.
     * During recursive method call, the number of cells will be counted and
     * returned afterwards.
     * 
     * @param out the stream to write its sibling and itself to
     * @param cellCount the number of cells to beginn the cell count with
     * @return the number of cells written to given stream, plus the initial
     *         value.
     * @throws IOException if cell could not be written to the stream
     */
    public int writeToFile(final ObjectOutputStream out, final int cellCount)
            throws IOException {
        int count = cellCount + 1;

        if (m_left != null) {
            count = m_left.writeToFile(out, count);
        }
        if (m_right != null) {
            count = m_right.writeToFile(out, count);
        }

        out.writeObject(this);
        out.flush();

        return count;
    }
}
