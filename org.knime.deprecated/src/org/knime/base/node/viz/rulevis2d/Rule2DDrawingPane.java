/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   23.05.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.rulevis2d;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.knime.base.node.util.DataArray;
import org.knime.base.node.viz.scatterplot.ScatterPlotDrawingPane;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class Rule2DDrawingPane extends ScatterPlotDrawingPane implements
        HiLiteListener {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(Rule2DDrawingPane.class);

    private DataArray m_normRules;

    private DataTable m_orgData;

    // the GEOMETRIC core regions, stored for detecting a selection by
    // mouse-click
    private HashMap<RowKey, Rectangle> m_coreRegions;

    // The alpha value (indicating the transparency) of the core region
    private static final int CORE_ALPHA = 175;

    // 50 - 125
    // The aplha value of the brighter side of the gradient paint of the support
    // area
    private static final int BRIGHT_SUPPORT_ALPHA = 25;

    // The alpha value at the border of the core and support area
    private static final int DARK_SUPPORT_ALPHA = 125;

    private static final int NR_OF_POLYGON_POINTS = 4;

    private HiLiteHandler m_hiLiteHandler;

    private final Set<RowKey> m_selectedRules = new HashSet<RowKey>();

    private boolean m_hideUnhilitedRules;

    /**
     * 
     * 
     */
    public Rule2DDrawingPane() {
        super();
        addMouseListener(new MouseAdapter() {
            /**
             * If a rule is clicked it is selected. If control is down more than
             * one rules can be selected.
             * 
             * @param arg0 - the mouse event when the mouse is released.
             */
            @Override
            public void mouseReleased(final MouseEvent arg0) {
                // LOGGER.debug("mouse released...x: " + arg0.getX() + " y: " +
                // arg0.getY());
                if (arg0.isPopupTrigger()) {
                    return;
                }
                if (dragCoordSet()) {
                    return;
                }
                Point click = new Point(arg0.getX(), arg0.getY());
                if (!arg0.isControlDown()) {
                    m_selectedRules.clear();
                }
                for (Iterator<Entry<RowKey, Rectangle>> it = m_coreRegions
                        .entrySet().iterator(); it.hasNext();) {
                    Entry<RowKey, Rectangle> curr = it.next();
                    Rectangle currRec = curr.getValue();
                    if (currRec.contains(click)) {
                        m_selectedRules.add(curr.getKey());
                        // break;
                    }
                }
                repaint();
            }
        });
    }

    /**
     * Sets the normalized rules to draw.
     * 
     * @param normRules - the rules to draw.
     */
    public void setNormalizedRules(final DataArray normRules) {
        m_normRules = normRules;
    }

    /**
     * Sets the original data table containing the rules. Because of the color
     * information.
     * 
     * @param orgTable - the original DataTable containing the rules.
     */
    public void setOriginalRuleTable(final DataTable orgTable) {
        m_orgData = orgTable;
    }

    /**
     * Returns the number of selected rules.
     * 
     * @return - the number of selected rules.
     */
    public int getNrSelectedRules() {
        return m_selectedRules.size();
    }

    /**
     * Indicates whether the fade unhilited rules flag is set.
     * 
     * @return - true if unhilited rules are not displayed, false otherwise.
     */
    public boolean isHideUnhilitedRules() {
        return m_hideUnhilitedRules;
    }

    /**
     * Sets whether unhilited rules should be diplayed or not.
     * 
     * @param fadeUnhilitedRules - true if unhilited rules should not be
     *            displayed, false otherwise.
     */
    public void setHideUnhilitedRules(final boolean fadeUnhilitedRules) {
        this.m_hideUnhilitedRules = fadeUnhilitedRules;
        repaint();
    }

    /**
     * Returns the Ids of the currently selected rules.
     * 
     * @return - the ids of the selected rules.
     */
    public Set<RowKey> getSelectedRules() {
        return m_selectedRules;
    }

    /*------everything for hiliting, unhiliting and selecting rules------*/

    /**
     * Hilites the currently selected rules by adding them to the HiLiteHandler.
     */
    public void hiliteSelectedRules() {
        m_hiLiteHandler.fireHiLiteEvent(m_selectedRules);
    }

    /**
     * Unhilites the currently selected ruless by unhiliting them in the
     * HiLiteHandler.
     */
    public void unhiliteSelectedRules() {
        m_hiLiteHandler.fireUnHiLiteEvent(m_selectedRules);
    }

    /**
     * Returns the HiLiteHandler for the rules.
     * 
     * @return - the HiLiteHandler for the rules.
     */
    public HiLiteHandler getHiLiteHandler() {
        return m_hiLiteHandler;
    }

    /**
     * Sets the HiLiteHandler for the rules. Checks if the current one is
     * different and unregisters from the old one. Triggers a repaint.
     * 
     * @param hilitHdlr - the HiLiteHandler for the rules to set
     */
    public void setHiLiteHandler(final HiLiteHandler hilitHdlr) {
        LOGGER.debug("setting a new hiLiteHandler: " + hilitHdlr);
        if (m_hiLiteHandler != hilitHdlr) {
            // High light handler changed
            if (m_hiLiteHandler != null) {
                // unregister with the old handler
                m_hiLiteHandler.removeHiLiteListener(this);
            }
            m_hiLiteHandler = hilitHdlr;
            if (m_hiLiteHandler != null) {
                // register with the new one
                m_hiLiteHandler.addHiLiteListener(this);
            }
            repaint();
        }
    }

    /*-----------------The HiLiteListener methods--------------------*/
    /**
     * HiLites the rules identified by the event. Triggers a repaint.
     * 
     * @param event -the Hiliting event.
     */
    public void hiLite(final KeyEvent event) {
        // LOGGER.debug("hilite: " + event.keys());
        m_hiLiteHandler.fireHiLiteEvent(event.keys());
        repaint();
    }


    /**
     * Resets the current hilite. Triggers a repaint.
     * @see HiLiteListener#unHiLiteAll(KeyEvent) 
     */
    public void unHiLiteAll(final KeyEvent ke) {
        m_hiLiteHandler.fireClearHiLiteEvent();
        m_hideUnhilitedRules = false;
        repaint();
    }

    /**
     * Unhilites the rules identified by the event. Triggers a repaint.
     * 
     * @param event - the hiliting event.
     */
    public void unHiLite(final KeyEvent event) {
        m_hiLiteHandler.fireUnHiLiteEvent(event.keys());
        repaint();
    }

    /*--------- the drawing methods ----------------*/

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintPlotDrawingPane(final Graphics g) {
        paintRules(g);
        super.paintPlotDrawingPane(g);
    }

    private synchronized void paintRules(final Graphics g) {
        if (m_normRules == null || !m_normRules.iterator().hasNext()) {
            return;
        }
        m_coreRegions = new HashMap<RowKey, Rectangle>();
        Graphics2D g2 = (Graphics2D)g;
        RowIterator orgItr = m_orgData.iterator();
        for (Iterator<DataRow> it = m_normRules.iterator(); it.hasNext();) {
            DataRow row = it.next();
            DataRow orgRow = orgItr.next();
            // paint either if !m_hideUnhilitedRules
            // or if rule is hilited
            if (!m_hideUnhilitedRules
                    || m_hiLiteHandler.isHiLit(row.getKey())) {
                // check whether the rule is selected or hilited
                boolean selected = m_selectedRules.contains(row.getKey());
                boolean hilite = false;
                if (m_hiLiteHandler != null) {
                    hilite = m_hiLiteHandler.isHiLit(row.getKey());
                }
                Color currColor = m_orgData.getDataTableSpec().getRowColor(
                        orgRow).getColor(selected, hilite);

                FuzzyIntervalValue x = (FuzzyIntervalValue)row.getCell(0);
                FuzzyIntervalValue y = (FuzzyIntervalValue)row.getCell(1);

                // if one support region is not equal to the core region it's a
                // fuzzy rule
                if (x.getMinSupport() != x.getMinCore()
                        || x.getMaxCore() != x.getMaxSupport()
                        || y.getMinSupport() != y.getMinCore()
                        || y.getMaxCore() != y.getMaxSupport()) {
                    paintSupportArea(g2, row, currColor);
                }
                paintCoreArea(g2, row, currColor);
            } // end is unhiliting?
        } // end for
    }

    private synchronized void paintSupportArea(final Graphics2D g2,
            final DataRow currRow, final Color currColor) {
        FuzzyIntervalValue column1 = (FuzzyIntervalValue)currRow.getCell(0);
        FuzzyIntervalValue column2 = (FuzzyIntervalValue)currRow.getCell(1);

        int a1 = (int)(column1.getMinSupport());
        int b1 = (int)(column1.getMinCore());
        int c1 = (int)(column1.getMaxCore());
        int d1 = (int)(column1.getMaxSupport());

        int a2 = (int)(column2.getMinSupport());
        int b2 = (int)(column2.getMinCore());
        int c2 = (int)(column2.getMaxCore());
        int d2 = (int)(column2.getMaxSupport());

        int center1 = b1 + ((c1 - b1) / 2);
        int center2 = b2 + ((c2 - b2) / 2);

        Color brightColor = new Color(currColor.getRed(), currColor.getGreen(),
                currColor.getBlue(), BRIGHT_SUPPORT_ALPHA);
        Color darkColor = new Color(currColor.getRed(), currColor.getGreen(),
                currColor.getBlue(), DARK_SUPPORT_ALPHA);

        GradientPaint quad1Color = new GradientPaint(center1, a2, brightColor,
                center1, b2, darkColor);
        GradientPaint quad2Color = new GradientPaint(d1, center2, brightColor,
                c1, center2, darkColor);
        GradientPaint quad3Color = new GradientPaint(center1, d2, brightColor,
                center1, c2, darkColor);
        GradientPaint quad4Color = new GradientPaint(a1, center2, brightColor,
                b1, center2, darkColor);

        int[] northX = {a1, b1, c1, d1};
        int[] northY = {a2, b2, b2, a2};
        Polygon north = new Polygon(northX, northY, NR_OF_POLYGON_POINTS);

        int[] eastX = {d1, c1, c1, d1};
        int[] eastY = {a2, b2, c2, d2};
        Polygon east = new Polygon(eastX, eastY, NR_OF_POLYGON_POINTS);

        int[] southX = {a1, b1, c1, d1};
        int[] southY = {d2, c2, c2, d2};
        Polygon south = new Polygon(southX, southY, NR_OF_POLYGON_POINTS);

        int[] westX = {a1, b1, b1, a1};
        int[] westY = {a2, b2, c2, d2};
        Polygon west = new Polygon(westX, westY, NR_OF_POLYGON_POINTS);

        // draw support
        g2.setPaint(quad1Color);
        g2.fill(north);

        g2.setPaint(quad2Color);
        g2.fill(east);

        g2.setPaint(quad3Color);
        g2.fill(south);

        g2.setPaint(quad4Color);
        g2.fill(west);
    }

    private synchronized void paintCoreArea(final Graphics2D g2,
            final DataRow currRow, final Color currColor) {
        FuzzyIntervalValue column1 = (FuzzyIntervalValue)currRow.getCell(0);
        FuzzyIntervalValue column2 = (FuzzyIntervalValue)currRow.getCell(1);

        int a1 = (int)(column1.getMinSupport());
        int b1 = (int)(column1.getMinCore());
        int c1 = (int)(column1.getMaxCore());
        int d1 = (int)(column1.getMaxSupport());

        int a2 = (int)(column2.getMinSupport());
        int b2 = (int)(column2.getMinCore());
        int c2 = (int)(column2.getMaxCore());
        int d2 = (int)(column2.getMaxSupport());
        g2.setPaint(new Color(currColor.getRed(), currColor.getGreen(),
                currColor.getBlue(), CORE_ALPHA));
        // draw core:
        Rectangle core = new Rectangle(b1, b2, c1 - b1, c2 - b2);
        g2.fill(core);

        m_coreRegions.put(currRow.getKey(), core);

        // draw lines
        g2.setColor(Color.BLACK);
        g2.drawLine(a1, a2, b1, b2);
        g2.drawLine(d1, a2, c1, b2);
        g2.drawLine(d1, d2, c1, c2);
        g2.drawLine(a1, d2, b1, c2);

        // bounding boxes
        // outer
        g2.drawRect(a1, a2, (d1 - a1), (d2 - a2));
        // inner
        g2.drawRect(b1, b2, c1 - b1, c2 - b2);

        // draw vertical orientation lines
        /*
         * g2.drawLine(a1, 0, a1, getHeight()); g2.drawLine(b1, 0, b1,
         * getHeight()); g2.drawLine(c1, 0, c1, getHeight()); g2.drawLine(d1, 0,
         * d1, getHeight());
         * 
         * //draw horizontal orientation lines g2.drawLine(0, a2, getWidth(),
         * a2); g2.drawLine(0, b2, getWidth(), b2); g2.drawLine(0, c2,
         * getWidth(), c2); g2.drawLine(0, d2, getWidth(), d2);
         */
    }

}
