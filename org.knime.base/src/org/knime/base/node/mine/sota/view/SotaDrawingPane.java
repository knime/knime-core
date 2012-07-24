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
 * History
 *   Nov 30, 2005 (Kilian Thiel): created
 */
package org.knime.base.node.mine.sota.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

import org.knime.base.node.mine.sota.logic.SotaManager;
import org.knime.base.node.mine.sota.logic.SotaTreeCell;
import org.knime.base.node.mine.sota.view.interaction.SotaTreeCellLocations;
import org.knime.base.node.util.DataArray;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;

/**
 *
 * @author Kilian Thiel, University of Konstanz
 */
public class SotaDrawingPane extends JPanel implements HiLiteListener {
    // Widht and height of JPanel
    private int m_jpWidth = 900;

    private int m_jpHeight = 400;

    // Zoomfactor and zoomkeys
    private static final char ZOOM_IN = '+';

    private static final char ZOOM_OUT = '-';

    private int m_zoomFactor = 100;

    private boolean m_zooming = true;

    // Width and height of Datapixels
    private static final int PIXEL_HEIGHT = 20;

    private static final int HILITE_HEIGHT = 7;

    private static final int DATA_SEPARATOR_HEIGHT = 0;

    private int m_pixelWidth = 6;

    private static int DEF_PIXEL_WIDTH = 3;

    // Line height and rectangle width of cluster lines and rectangles
    private int m_clusterLineHeight = 50;

    private int m_clusterRectWidth = 4;

    // Impreciseness for clicks at clusterlines
    private int m_impreciseness = 3;

    // indata, root tree node, tree cells and maximum level of tree
    private SotaTreeCell m_root;

    private SotaTreeCellLocations m_rootLocation;

    private DataArray m_data;

    private int m_maxLevel = 0;

    private boolean m_isHierarchicalFuzzyData = false;

    private boolean m_drawHierarchicalFuzzyData = false;

    private boolean m_drawHierarchicalSeparators = false;

    private int m_maxHLevel;

    private int m_accentHLevel;

    private Hashtable<Integer, Integer> m_hierarchicalMaxLevels;

    private ArrayList<SotaTreeCell> m_cells;

    private HashMap<SotaTreeCell, SotaTreeCellLocations> m_cellLocations;

    // Hilited keys Selected keys and Hilitehandler
    private ArrayList<RowKey> m_selectedKeys;

    private Set<RowKey> m_hilitedKeys;

    private HiLiteHandler m_hiliteHandler;

    // Coordinates of data and clusters
    private Hashtable<Integer, Integer> m_dataCoordIndex;

    private Hashtable<Integer, ArrayList<SotaTreeCell>> m_nodesCoordIndex;

    // Tooltiptext seperator
    private String m_toolTipSeperator = ", ";

    // Hilite and background colors
    private final Color m_background = Color.WHITE;

    private final Color m_hilit = ColorAttr.HILITE;

    private final Color m_selected = ColorAttr.SELECTED;

    private final Color m_selectedHilited = ColorAttr.SELECTED_HILITE;

    private final Color m_clusterLines = Color.BLACK;

    private final Color m_seperatorLines = Color.BLACK;

    private final Color m_unusedClusterLines = Color.GRAY;

    private final Color m_accentHLevelColor = Color.RED;

    private final Color m_hierarchySeparatorLines = new Color(0.85f, 0.85f,
            0.85f);

    private DataArray m_originalData;

    /**
     * Creates new instance of SotaDrawingPane, which draws the given data and
     * the trained binary cluster tree given by its root node.
     *
     * @param root the root node of the binary tree to draw
     * @param data the data to draw
     * @param originalData the original data
     * @param isHFuzzyData if <code>true</code>, clusters will be drawn as
     *            hierarchical fuzzy clusters
     * @param maxHLevel the maximum hierarchical level
     */
    public SotaDrawingPane(final SotaTreeCell root, final DataArray data,
            final DataArray originalData, final boolean isHFuzzyData,
            final int maxHLevel) {
        super(true);

        m_originalData = originalData;
        m_data = data;
        m_root = root;
        m_rootLocation = new SotaTreeCellLocations();
        m_isHierarchicalFuzzyData = isHFuzzyData;
        m_drawHierarchicalFuzzyData = isHFuzzyData;
        m_drawHierarchicalSeparators = isHFuzzyData;
        m_maxHLevel = maxHLevel;
        m_accentHLevel = 0;
        m_hierarchicalMaxLevels = new Hashtable<Integer, Integer>();

        m_selectedKeys = new ArrayList<RowKey>();
        m_hilitedKeys = new HashSet<RowKey>();
        m_cells = null;
        m_cellLocations = null;
        m_hiliteHandler = null;

        m_dataCoordIndex = new Hashtable<Integer, Integer>();
        m_nodesCoordIndex = new Hashtable<Integer, ArrayList<SotaTreeCell>>();

        ToolTipManager.sharedInstance().registerComponent(this);

        this.setFocusable(true);
        PaneController pc = new PaneController();
        this.addKeyListener(pc);
        this.addMouseListener(pc);

        this.setBackground(m_background);
        this.setOpaque(true);

        this.setSize(m_jpWidth, m_jpHeight);
        this.setPreferredSize(new Dimension(m_jpWidth, m_jpHeight));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent event) {
        if (m_data != null && m_root != null) {

            DataRow row = getDataRowAtCursor(event.getX(), event.getY());
            if (row != null) {
                String text = "RowKey: " + row.getKey().getString();
                text += "\nData: ";

                for (int i = 0; i < row.getNumCells(); i++) {
                    text += row.getCell(i).toString() + m_toolTipSeperator;
                }
                return text;

            } else {
                SotaTreeCell cell = getCellAtCursor(event.getX(), event.getY());
                if (cell != null) {
                    String title;
                    if (cell.isCell()) {
                        title = "Cell";
                    } else {
                        title = "Node";
                    }

                    String hAddOn = "";
                    if (m_isHierarchicalFuzzyData) {
                        hAddOn = "(H-Level: " + cell.getHierarchyLevel()
                                + ") (inner H-Level: "
                                + cell.getLevelInHierarchy() + ")";
                    }

                    return title + ": " + "(Level: " + cell.getLevel() + ") "
                            + hAddOn + "\n" + "Data: "
                            + cell.getDataAsString(2);
                }
            }
        }

        return "Press + or - to zoom in or out. [" + event.getX() + ", "
                + event.getY() + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JToolTip createToolTip() {
        JMultiLineToolTip mlTT = new JMultiLineToolTip();
        return mlTT;
    }

    /**
     * Creates the Tree structure for the JTree and paints it.
     *
     * @param first if <code>true</code> size of JPanel will be recomputed
     */
    public void modelChanged(final boolean first) {
        if (m_root != null && m_data != null) {

            // computes size for the first time the oanel is shown
            if (first) {
                // when there is only a small amount of data increase the
                // default pixel width by the magic factor.
                int factor = 1;
                if (m_data.size() <= 150) {
                    factor = 2;
                    if (m_data.size() <= 50) {
                        factor = 5;
                    }
                }
                m_jpWidth = m_data.size() * DEF_PIXEL_WIDTH * factor;

                // resize to optimal size
                setSize(m_jpWidth, m_jpHeight);
                setPreferredSize(new Dimension(m_jpWidth, m_jpHeight));
            }

            // compute pixel width of data pixels
            if (m_data.size() == 0) {
                m_pixelWidth = 0;
            } else  {
                m_pixelWidth = m_jpWidth / m_data.size();
            }

            // get all cells
            m_cells = new ArrayList<SotaTreeCell>();
            SotaManager.getCells(m_cells, m_root);

            // set default locations
            m_cellLocations =
                new HashMap<SotaTreeCell, SotaTreeCellLocations>();
            SotaManager.initDefaultLocations(m_cellLocations, m_root);


            // compute maxmimum tree level
            m_maxLevel = getMaxLevel(m_maxLevel, m_root);
            m_clusterLineHeight = (m_jpHeight - PIXEL_HEIGHT)
                    / m_maxLevel;

            // store max Level in each hierarchy if data is hierarchical
            if (m_isHierarchicalFuzzyData && m_drawHierarchicalFuzzyData) {
                getHierarchicalMaxLevels();
            }

            // reset X coordinates and related cells
            m_nodesCoordIndex.clear();

            // copmute the coordinates for the tree nodes and cluster lines
            computeTreeNodeCoordinates();

            // after computation of cluster lines the height of the jpanel
            // may be to small. We have to check if clusterlines are too high
            // and redurce them if they are.
            if (m_isHierarchicalFuzzyData && m_drawHierarchicalFuzzyData) {
                // if clusterlines too high
                if (m_rootLocation.getEndY() < 0) {
                    int lineCount = (Math.abs(m_rootLocation.getEndY())
                            + m_jpHeight - PIXEL_HEIGHT) / m_clusterLineHeight;

                    m_clusterLineHeight = (m_jpHeight - PIXEL_HEIGHT)
                            / lineCount;

                    // after new computation of optimal cluster line height
                    // recompute the cluster line positions.
                    m_nodesCoordIndex.clear();
                    computeTreeNodeCoordinates();
                }
            }

            repaint();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics g) {
        super.paint(g);

        // If zooming disabled resize panel when frame is resized
        if ((getParent().getSize().width < getSize().width || getParent()
                .getSize().height < getSize().height)
                && !m_zooming) {
            // If size of frame is smaller than size of panel

            m_jpWidth = getParent().getSize().width;
            m_jpHeight = getParent().getSize().height;

            setSize(m_jpWidth, m_jpHeight);
            setPreferredSize(new Dimension(m_jpWidth, m_jpHeight));

            modelChanged(false);
        } else if ((m_jpWidth != getSize().width
                || m_jpHeight != getSize().height)
                && !m_zooming) {
            // If size of panel is smaller than size of frame and size of panel
            // was changed

            m_jpWidth = getSize().width;
            m_jpHeight = getSize().height;

            setSize(m_jpWidth, m_jpHeight);
            setPreferredSize(new Dimension(m_jpWidth, m_jpHeight));

            modelChanged(false);
        }

        // repaint all the data
        if (m_data != null && m_root != null && m_cells != null
                && m_originalData != null) {
            drawData(g);
            drawTree(g);

            if (m_isHierarchicalFuzzyData && m_drawHierarchicalSeparators) {
                drawHierarchySeparatorLines(g);
            }
        }
    }

    /**
     * Draws the datapixels.
     *
     * @param g graphics instance to draw with
     */
    private void drawData(final Graphics g) {

        if (m_originalData == null || m_data == null) {
            return;
        }

        int startX = 0;
        int startY = m_jpHeight - PIXEL_HEIGHT;
        int tmpStartY = startY;
        int clusterSeparatorHeight = DATA_SEPARATOR_HEIGHT;

        int count = 0;
        // through all cells
        for (int j = 0; j < m_cells.size(); j++) {
            // through all dataIds of the cells
            for (int i = 0; i < m_cells.get(j).getDataIds().size(); i++) {
                DataRow row = m_data.getRow(m_cells.get(j).getDataIds().get(i));
                DataRow origRow = m_originalData.getRow(m_cells.get(j)
                        .getDataIds().get(i));

                // draw data rectangle
                g.setColor(m_originalData.getDataTableSpec().getRowColor(
                        origRow).getColor());
               // g.setColor(m_data.getTableSpec().getRowColor(row).getColor());

                g.fillRect(startX, m_jpHeight - HILITE_HEIGHT,
                        (m_pixelWidth - 1), HILITE_HEIGHT);

                // draw hilite rectangle
                if (m_hilitedKeys != null && m_selectedKeys != null) {
                    if (m_hilitedKeys.contains(row.getKey())
                            && m_selectedKeys.contains(row.getKey())) {
                        g.setColor(m_selectedHilited);
                    } else if (m_hilitedKeys.contains(row.getKey())
                            && !m_selectedKeys.contains(row.getKey())) {
                        g.setColor(m_hilit);
                    } else if (m_selectedKeys.contains(row.getKey())
                            && !m_hilitedKeys.contains(row.getKey())) {
                        g.setColor(m_selected);
                    }
                }
                g.fillRect(startX, startY, (m_pixelWidth - 1), PIXEL_HEIGHT
                        - HILITE_HEIGHT);

                // increase X value
                startX += m_pixelWidth - 1;

                // draw separator line
                if (i == m_cells.get(j).getDataIds().size() - 1) {
                    tmpStartY = startY - clusterSeparatorHeight;
                } else {
                    tmpStartY = startY;
                }
                g.setColor(m_seperatorLines);
                g.drawLine(startX, tmpStartY, startX, m_jpHeight);
                startX++;

                // save position of data and its index in DataArray
                // to access the data later via position.
                m_dataCoordIndex.put(count, m_cells
                        .get(j).getDataIds().get(i));
                count++;
            }
        }
    }

    /**
     * Draws the tree recursive by using drawTreeNode method.
     *
     * @param g graphics instance to draw with
     */
    private void drawTree(final Graphics g) {
        g.setColor(m_clusterLines);
        drawTreeNode(g, m_root);
    }

    /**
     * Draws the given {@link SotaTreeCell} and its children recursive, by using
     * their coordinates.
     *
     * @param g graphics instance to draw with
     * @param cell the cell to draw
     */
    private void drawTreeNode(final Graphics g, final SotaTreeCell cell) {
        if (!cell.isCell()) {
            drawTreeNode(g, cell.getLeft());
            drawTreeNode(g, cell.getRight());
        }

        g.setColor(getCellColor(cell, false));
        SotaTreeCellLocations cellLocation = m_cellLocations.get(cell);

        // Draw cluster line
        g.drawLine(cellLocation.getStartX(), cellLocation.getStartY(),
                cellLocation.getEndX(), cellLocation.getEndY());

        // Draw cluster rectangle
        g.drawRect(cellLocation.getEndX() - (m_clusterRectWidth / 2),
                cellLocation.getEndY() - (m_clusterRectWidth / 2),
                m_clusterRectWidth, m_clusterRectWidth);

        SotaTreeCell sister = cell.getSister();
        if (sister == null) {
            sister = cell;
        }
        if (cell.isHilited() && sister.isHilited() && !cell.isSelected()) {
            g.setColor(m_hilit);
        } else if (cell.isHilited()
                && sister.isHilited()
                && cell.isSelected()) {
            g.setColor(m_selectedHilited);
        } else {
            g.setColor(getCellColor(cell, true));
        }

        // Draw line between sisters
        if (cell.getSister() != null) {
            SotaTreeCellLocations sisterCellLocation =
                m_cellLocations.get(cell.getSister());

            g.drawLine(cellLocation.getStartX(), cellLocation.getEndY(),
                    sisterCellLocation.getStartX(),
                    sisterCellLocation.getEndY());
        }
    }

    /**
     * Returns the color of the given cell.
     *
     * @param cell cell to return its color
     * @param ignoreUnusedClusters if <code>true</code>, the color for unused
     *            cells or clusters is not returned, but a regular color
     *            accordant to that cell
     * @return the color of the given cell
     */
    private Color getCellColor(final SotaTreeCell cell,
            final boolean ignoreUnusedClusters) {
        Color col;
        if (cell.getDataIds().size() < 1 && !ignoreUnusedClusters) {
            col = m_unusedClusterLines;
        } else {
            if (cell.isHilited() && cell.isSelected()) {
                col = m_selectedHilited;
            } else if (cell.isHilited() && !cell.isSelected()) {
                col = m_hilit;
            } else if (cell.isSelected() && !cell.isHilited()) {
                col = m_selected;
            } else {
                if (!m_isHierarchicalFuzzyData) {
                    col = m_clusterLines;
                } else {
                    if (m_accentHLevel == cell.getHierarchyLevel()) {
                        col = m_accentHLevelColor;
                    } else {
                        col = m_clusterLines;
                    }
                }
            }
        }
        return col;
    }

    /**
     * Draws the separator lines before each hierarchical level.
     *
     * @param g graphics object to draw with
     */
    private void drawHierarchySeparatorLines(final Graphics g) {
        int seperatorDelta = m_clusterLineHeight / 6;
        int y = m_jpHeight - PIXEL_HEIGHT - seperatorDelta;

        for (int i = m_maxHLevel; i >= 1; i--) {
            y -= (m_hierarchicalMaxLevels.get(i) * m_clusterLineHeight);

            if (m_accentHLevel == i) {
                g.setColor(m_accentHLevelColor);
            } else {
                g.setColor(m_hierarchySeparatorLines);
            }

            int width = m_data.size() * m_pixelWidth;
            if (width > m_jpWidth) {
                width = m_jpWidth;
            }

            g.drawLine(0, y, width, y);
        }
    }

    /**
     * Computes the coordinates of the SotaTreeNodes and sets them, so that they
     * can be reused to draw the nodes. This method only computes the
     * coordinates of the trees cell level and delegates the rest of the work to
     * the recursive method
     * {@link #computeParentTreeNodeCoordinates(ArrayList, int)} where the
     * coordinates of the cells parents and parents parents and so on is
     * computed.
     */
    private void computeTreeNodeCoordinates() {
        int startX = 0;
        int startY = m_jpHeight - PIXEL_HEIGHT;

        for (int j = 0; j < m_cells.size(); j++) {
            int ids = m_cells.get(j).getDataIds().size();
            int x = ids * m_pixelWidth / 2;
            x += startX;

            // save coordinates
            SotaTreeCellLocations cellLocation = m_cellLocations.get(
                    m_cells.get(j));
            cellLocation.setStartX(x);
            cellLocation.setEndX(x);
            cellLocation.setStartY(startY);

            int heightFactor;
            if (m_isHierarchicalFuzzyData && m_drawHierarchicalFuzzyData) {
                // if hierarchical data is used:
                // get hierarchical level of current cell
                int currentHLevel = m_cells.get(j).getHierarchyLevel();
                // get difference to maximal hierarchical level
                int hLevelDiff = m_maxHLevel - currentHLevel;

                heightFactor = m_hierarchicalMaxLevels.get(currentHLevel)
                        - m_cells.get(j).getLevelInHierarchy();

                for (int l = 1; l <= hLevelDiff; l++) {
                    heightFactor += m_hierarchicalMaxLevels.get(currentHLevel
                            + l);
                }
            } else {
                heightFactor = m_maxLevel - m_cells.get(j).getLevel();
            }

            cellLocation.setEndY(
                    (startY - m_clusterLineHeight)
                            - (heightFactor * m_clusterLineHeight));

            startX += ids * m_pixelWidth;

            // Save the X coordinates and the related cell
            addCellToCoordHash(x, m_cells.get(j));
        }

        computeParentTreeNodeCoordinates(m_cells, m_maxLevel);
    }

    /**
     * Computes the coordinates of the parents of the given children nodes.
     *
     * @param children the children of the parents to compute the coordinates
     *            for
     * @param level current tree level
     * @return the parents in a list for which the coordinates were computed
     */
    private ArrayList<?> computeParentTreeNodeCoordinates(
            final ArrayList<SotaTreeCell> children, final int level) {
        ArrayList<SotaTreeCell> parents = new ArrayList<SotaTreeCell>();

        for (int i = 0; i < children.size(); i++) {
            // only if cell has given level, if not put cell in parents array
            if (children.get(i).getLevel() == level) {

                // if parent is not in parents list and has no coordinates
                if (!parents.contains(children.get(i).getAncestor())) {
                    parents.add(children.get(i).getAncestor());

//                    SotaTreeCell c = children.get(i);
//                    SotaTreeCell cS = children.get(i).getSister();

                    SotaTreeCellLocations childrenCellLocation =
                        m_cellLocations.get(children.get(i));
                    SotaTreeCellLocations childrenSisterCellLocation =
                        m_cellLocations.get(children.get(i).getSister());

                    int x = (childrenCellLocation.getStartX()
                            + childrenSisterCellLocation.getStartX()) / 2;

                    int startY = childrenCellLocation.getEndY();

                    int heightFactor = 0;
                    if (m_isHierarchicalFuzzyData
                            && m_drawHierarchicalFuzzyData) {
                        // if hierarchical data is used:
                        heightFactor = level
                                - children.get(i).getAncestor().getLevel();

                        int currentHLevel = children.get(i).getAncestor()
                                .getHierarchyLevel();
                        int childrenHLevel = children.get(i)
                                .getHierarchyLevel();

                        if (currentHLevel != childrenHLevel) {

                            heightFactor += m_hierarchicalMaxLevels
                                    .get(currentHLevel)
                                    - children.get(i).getAncestor()
                                            .getLevelInHierarchy();

                            for (int l = childrenHLevel - 1; l > currentHLevel;
                                l--) {
                                heightFactor += m_hierarchicalMaxLevels.get(l);
                            }
                        }
                    } else {
                        heightFactor = level
                                - children.get(i).getAncestor().getLevel();
                    }

                    int endY = startY - (heightFactor * m_clusterLineHeight);

                    SotaTreeCellLocations childrenAncestorCellLocation =
                        m_cellLocations.get(children.get(i).getAncestor());
                    if (childrenAncestorCellLocation != null) {
                        childrenAncestorCellLocation.setStartX(x);
                        childrenAncestorCellLocation.setEndX(x);

                        childrenAncestorCellLocation.setStartY(startY);
                        childrenAncestorCellLocation.setEndY(endY);
                    }

                    // Save the X coordinates and the related cell
                    addCellToCoordHash(x, children.get(i).getAncestor());
                }
            } else if (children.get(i).getLevel() < level) {
                parents.add(children.get(i));
            }
        }

        if (level > 1) {
            return computeParentTreeNodeCoordinates(parents, level - 1);
        }
        return null;
    }

    /**
     * Computes the maximum level of the tree recursive.
     *
     * @param maxLevel the current maximum level
     * @param cell the cell to check its level
     * @return the new maximum level after checking the cells level and its
     *         childrens level
     */
    private int getMaxLevel(final int maxLevel, final SotaTreeCell cell) {
        int mLevel = maxLevel;
        if (cell.getLevel() > mLevel) {
            mLevel = cell.getLevel();
        }
        if (!cell.isCell()) {
            int maxLevLeft = getMaxLevel(mLevel, cell.getLeft());
            int maxLevRight = getMaxLevel(mLevel, cell.getRight());
            if (mLevel < maxLevLeft) {
                mLevel = maxLevLeft;
            }
            if (mLevel < maxLevRight) {
                mLevel = maxLevRight;
            }
        }
        return mLevel;
    }

    /**
     * Computes the maximum level for given hierarchy level of the tree.
     *
     * @param maxLevel the current maximum level
     * @param cell the cell to check its level
     * @param hLevel hierarchical level to search maxmimal level for
     * @return the new maximum level after checking the cells level and its
     *         childrens level
     */
    private int getMaxLevelOfHLevel(final int maxLevel,
            final SotaTreeCell cell, final int hLevel) {
        int mLevel = maxLevel;
        if (hLevel == cell.getHierarchyLevel()) {
            mLevel++;
        }
        if (!cell.isCell()) {
            int maxLevLeft = getMaxLevelOfHLevel(mLevel, cell.getLeft(),
                    hLevel);
            int maxLevRight = getMaxLevelOfHLevel(mLevel, cell.getRight(),
                    hLevel);
            if (mLevel < maxLevLeft) {
                mLevel = maxLevLeft;
            }
            if (mLevel < maxLevRight) {
                mLevel = maxLevRight;
            }
        }
        return mLevel;
    }

    /**
     * Computes the maximum levels for all hierarchy levels.
     */
    private void getHierarchicalMaxLevels() {
        for (int i = 0; i <= m_maxHLevel; i++) {
            m_hierarchicalMaxLevels.put(i, getMaxLevelOfHLevel(0,
                    m_root, i));
        }
    }

    /**
     * Returns the row related to the given mouse position, if no row is related
     * to that position <code>null</code> is returned.
     *
     * @param x mouse position on x coordinate
     * @param y mouse position on y coordinate
     * @return the row related to the given mouse position or <code>null</code>
     *         if no row is available at that position
     */
    private DataRow getDataRowAtCursor(final int x, final int y) {
        if (m_data != null) {
            int maxX = m_data.size() * m_pixelWidth;

            if (x <= maxX && x >= 0 && y <= m_jpHeight
                    && y >= (m_jpHeight - PIXEL_HEIGHT)) {
                int place = x / m_pixelWidth;

                Integer index = m_dataCoordIndex.get(place);
                if (index != null) {
                    return m_data.getRow(index);
                }
            }
        }

        return null;
    }

    /**
     * Returns the cell related to the given mouse position, if no cell is
     * related to that position <code>null</code> is returned.
     *
     * @param x mouse position on x coordinate
     * @param y mouse position on y coordinate
     * @return the cell related to the given mouse position or <code>null</code>
     *         if no cell is available at that position
     */
    private SotaTreeCell getCellAtCursor(final int x, final int y) {
        if (m_data != null && m_root != null) {
            int maxX = m_data.size() * m_pixelWidth;
            int yCoord = y;
            int xCoord;

            // if mouseclick is not exactly at a cells x coordinate this two
            // for loops will handle clicks a little more imprecise. If there
            // is no cell at clicked x coordinated the next x coordinates
            // around (-m_impreciseness/+m_impreciseness) will be checked
            // for a cell.
            for (int j = 0; j <= m_impreciseness; j++) {
                for (int dir = -1; dir <= 1; dir += 2) {
                    xCoord = x + (j * dir);

                    if (xCoord <= maxX && xCoord >= 0
                            && yCoord <= m_jpHeight - PIXEL_HEIGHT) {
                        ArrayList<SotaTreeCell> list = m_nodesCoordIndex
                                .get(xCoord);
                        if (list != null) {
                            for (int i = 0; i < list.size(); i++) {
                                SotaTreeCellLocations cellLocation =
                                    m_cellLocations.get(list.get(i));

                                if (yCoord <= cellLocation.getStartY()
                                        && yCoord >= cellLocation.getEndY()) {
                                    return list.get(i);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Adds the given cell to Nodes_ccord_index Hashtable at given key x.
     *
     * @param x key to add the given cell at
     * @param cell cell to add to Hashtable
     */
    private void addCellToCoordHash(final int x, final SotaTreeCell cell) {
        // if a list with cells at current x coordinate exists then
        // add cell to list if not build one.
        if (m_nodesCoordIndex.get(x) == null) {
            ArrayList<SotaTreeCell> list = new ArrayList<SotaTreeCell>();
            list.add(cell);
            m_nodesCoordIndex.put(x, list);
        } else {
            m_nodesCoordIndex.get(x).add(cell);
        }
    }

    /**
     * @return the root
     */
    public SotaTreeCell getRoot() {
        return m_root;
    }

    /**
     * @param root the root to set
     */
    public void setRoot(final SotaTreeCell root) {
        this.m_root = root;
    }

    /**
     * @param data the data to set
     */
    public void setData(final DataArray data) {
        this.m_data = data;
    }

    /**
     * @param data the original data to set.
     */
    public void setOriginalData(final DataArray data) {
        m_originalData = data;
    }

    /**
     * @return the data
     */
    public DataArray getData() {
        return m_data;
    }

    /**
     * Zooms in.
     */
    protected void zoomIn() {
        if (m_zooming) {
            setZoomFactor();
            m_jpWidth += m_zoomFactor;
            setSize(m_jpWidth, m_jpHeight);
            setPreferredSize(new Dimension(m_jpWidth, m_jpHeight));
            modelChanged(false);
        }
    }

    /**
     * Zooms out.
     */
    protected void zoomOut() {
        if (m_zooming) {
            m_jpWidth -= m_zoomFactor;
            setSize(m_jpWidth, m_jpHeight);
            setPreferredSize(new Dimension(m_jpWidth, m_jpHeight));
            modelChanged(false);
        }
    }

    private void setZoomFactor() {
        m_zoomFactor = m_data.size()
                - (m_jpWidth - (m_data.size() * m_pixelWidth));
    }

    /**
     * Controls the KeyEvents of the SotaDrawingPane.
     *
     * @author Kilian Thiel, University of Konstanz
     */
    class PaneController extends KeyAdapter implements MouseListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyTyped(final KeyEvent e) {
            if (e.getKeyChar() == ZOOM_IN) {
                zoomIn();
            } else if (e.getKeyChar() == ZOOM_OUT) {
                zoomOut();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void mouseClicked(final MouseEvent e) {
            if (m_data != null && m_root != null) {

                DataRow row = getDataRowAtCursor(e.getX(), e.getY());
                if (row != null) {
                    // Select Data
                    if (m_selectedKeys.contains(row.getKey())) {
                        m_selectedKeys.remove(row.getKey());
                    } else {
                        m_selectedKeys.add(row.getKey());
                    }
                } else {
                    SotaTreeCell cell = getCellAtCursor(e.getX(), e.getY());
                    if (cell != null) {
                        // Select Cell and subtree
                        selectCellData(cell);

                        if (!cell.isSelected()) {
                            cell.setSelected(true);
                        } else {
                            cell.setSelected(false);
                        }
                    } else {
                        // Nothing was selected so deselect all
                        m_root.deselectSubtree();
                        m_selectedKeys.clear();
                    }
                }
                repaint();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void mousePressed(final MouseEvent e) {
        }

        /**
         * {@inheritDoc}
         */
        public void mouseReleased(final MouseEvent e) {
        }

        /**
         * {@inheritDoc}
         */
        public void mouseEntered(final MouseEvent e) {
        }

        /**
         * {@inheritDoc}
         */
        public void mouseExited(final MouseEvent e) {
        }
    }

    /**
     * Stores the Cells data in given HashSet if given cell is a cell and not a
     * node, otherwise this method is called recursively with the given cells
     * children.
     *
     * @param cell cell to store its data.
     */
    private void selectCellData(final SotaTreeCell cell) {
        if (!cell.isCell()) {
            selectCellData(cell.getLeft());
            selectCellData(cell.getRight());
        } else {
            for (int i = 0; i < cell.getDataIds().size(); i++) {
                if (m_selectedKeys.contains(m_data.getRow(
                        cell.getDataIds().get(i)).getKey())) {
                    m_selectedKeys.remove(m_data.getRow(
                            cell.getDataIds().get(i)).getKey());
                } else {
                    m_selectedKeys.add(m_data.getRow(cell.getDataIds().get(i))
                            .getKey());
                }
            }
        }
    }

    /*
     * Changes Selected state to hilited state. @param cell The cell to start
     * recursive call from.
     */
    private void turnSelectedIntoHilited(final SotaTreeCell cell,
            final boolean hilite) {
        if (cell.isSelected()) {
            cell.setCellHilited(hilite);
        }
        if (!cell.isCell()) {
            turnSelectedIntoHilited(cell.getLeft(), hilite);
            turnSelectedIntoHilited(cell.getRight(), hilite);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLiteAll(
            final org.knime.core.node.property.hilite.KeyEvent event) {
        if (m_root != null) {
            m_root.setHilited(false);
        }
        if (m_hilitedKeys != null) {
            m_hilitedKeys.clear();
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void hiLite(
            final org.knime.core.node.property.hilite.KeyEvent event) {
        Iterator<RowKey> i = event.keys().iterator();
        while (i.hasNext()) {
            RowKey cell = i.next();
            m_hilitedKeys.add(cell);
        }

        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void unHiLite(
            final org.knime.core.node.property.hilite.KeyEvent event) {
        Set<RowKey> tmpSet = new HashSet<RowKey>();

        Iterator<RowKey> i = m_hilitedKeys.iterator();
        while (i.hasNext()) {
            RowKey cell = i.next();
            if (!event.keys().contains(cell)) {
                tmpSet.add(cell);
            }
        }
        m_hilitedKeys = tmpSet;

        repaint();
    }

    /**
     * @param handler the hiliteHandler to set
     */
    public void setHiliteHandler(final HiLiteHandler handler) {
        m_hiliteHandler = handler;
    }

    /**
     * @return the hiliteHandler
     */
    public HiLiteHandler getHiliteHandler() {
        return m_hiliteHandler;
    }

    /**
     * @return the isHierarchicalFuzzyData
     */
    public boolean isHierarchicalFuzzyData() {
        return m_isHierarchicalFuzzyData;
    }

    /**
     * @return the drawHierarchicalFuzzyData.
     */
    public boolean isDrawHierarchicalFuzzyData() {
        return m_drawHierarchicalFuzzyData;
    }

    /**
     * @param hierarchicalFuzzyData the drawHierarchicalFuzzyData to set
     */
    public void setDrawHierarchicalFuzzyData(
            final boolean hierarchicalFuzzyData) {
        m_drawHierarchicalFuzzyData = hierarchicalFuzzyData;
    }

    /**
     * @return the drawHierarchicalSeparators
     */
    public boolean isDrawHierarchicalSeparators() {
        return m_drawHierarchicalSeparators;
    }

    /**
     * @param hierarchicalSeparators the drawHierarchicalSeparators to set
     */
    public void setDrawHierarchicalSeparators(
            final boolean hierarchicalSeparators) {
        m_drawHierarchicalSeparators = hierarchicalSeparators;
    }

    /**
     * @param level the maxHLevel to set
     */
    public void setMaxHLevel(final int level) {
        m_maxHLevel = level;
    }

    /**
     * @return the maxHLevel
     */
    public int getMaxHLevel() {
        return m_maxHLevel;
    }

    /**
     * @param level accentHLevel to set
     */
    public void setAccentHLevel(final int level) {
        m_accentHLevel = level;
    }

    /** Popup menue entry constant. */
    public static final String POPUP_HILITE_SELECTED = "Hilite selected";

    /** Popup menue entry constant. */
    public static final String POPUP_UNHILITE_SELECTED = "Unhilite selected";

    /** Popup menue entry constant. */
    public static final String POPUP_UNHILITE = "Clear Hilite";

    /**
     *
     * @return a JMenu entry handling the hiliting of objects
     */
    public JMenu createHiLiteMenu() {
        JMenu menu = new JMenu("Hilite");
        menu.setMnemonic('H');
        ActionListener actL = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals(POPUP_HILITE_SELECTED)) {
                    // hilite selected rows with the hilite manager
                    for (int i = 0; i < m_selectedKeys.size(); i++) {
                        if (!m_hilitedKeys.contains(m_selectedKeys.get(i))) {
                            m_hilitedKeys.add(m_selectedKeys.get(i));
                        }
                    }
                    turnSelectedIntoHilited(m_root, true);
                    m_hiliteHandler.fireHiLiteEvent(m_hilitedKeys);

                    repaint();
                } else if (e.getActionCommand().equals(
                        POPUP_UNHILITE_SELECTED)) {
                    HashSet<RowKey> unhilite = new HashSet<RowKey>();
                    for (int i = 0; i < m_selectedKeys.size(); i++) {
                        if (m_hilitedKeys.contains(m_selectedKeys.get(i))) {
                            m_hilitedKeys.remove(m_selectedKeys.get(i));
                            unhilite.add(m_selectedKeys.get(i));
                        }
                    }
                    turnSelectedIntoHilited(m_root, false);
                    m_hiliteHandler.fireUnHiLiteEvent(unhilite);

                    repaint();
                } else if (e.getActionCommand().equals(POPUP_UNHILITE)) {
                    m_root.setHilited(false);
                    m_hilitedKeys.clear();
                    m_hiliteHandler.fireClearHiLiteEvent();

                    repaint();
                }
            }
        };
        JMenuItem item = new JMenuItem(POPUP_HILITE_SELECTED);
        item.addActionListener(actL);
        item.setMnemonic('H');
        menu.add(item);
        item = new JMenuItem(POPUP_UNHILITE_SELECTED);
        item.addActionListener(actL);
        item.setMnemonic('U');
        menu.add(item);
        item = new JMenuItem(POPUP_UNHILITE);
        item.addActionListener(actL);
        item.setMnemonic('E');
        menu.add(item);

        return menu;
    }

    /**
     * @return the m_zooming
     */
    public boolean isZooming() {
        return m_zooming;
    }

    /**
     * @param zooming zooming to set
     */
    public void setZooming(final boolean zooming) {
        this.m_zooming = zooming;
    }
}
