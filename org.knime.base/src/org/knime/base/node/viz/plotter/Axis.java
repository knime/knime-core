/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 * History
 *   14.09.2004 (ohl): created
 */
package org.knime.base.node.viz.plotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.CoordinateMapping;
import org.knime.base.util.coordinate.MappingMethod;
import org.knime.base.util.coordinate.NominalCoordinate;
import org.knime.base.util.coordinate.PolicyStrategy;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;

/**
 * Implements a header for a scrollable area. Can be used as horizontal (row) or
 * vertical (columns) header. It tries to find "useful" increments between the
 * ticks. There are various parameters you can play with to get a nice display -
 * but I admit it's hard and only sometimes successful...
 *
 * @author ohl, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 * @author Stephan Sellien, University of Konstanz
 */
public class Axis extends JComponent {

    // private static final NodeLogger LOGGER =
    // NodeLogger.getLogger(Axis.class);

    /**
     * Constant for the horizontal enumeration value.
     */
    public static final int HORIZONTAL = 0;

    /**
     * Constant for the vertical enumeration value.
     */
    public static final int VERTICAL = 1;

    /**
     * the "thickness" of the header.
     */
    public static final int SIZE = 50;

    /** the length of the tick line. */
    private static final int TICKLENGTH = 7;

    /** the size of the font we draw with. */
    private static final int FONTSIZE = 10;

    /** the font used to label the ruler. */
    private static final Font RULER_FONT =
            new Font("SansSerif", Font.PLAIN, FONTSIZE);

    /** the offset between adjacent labels in horizontal headers. */
    private static final int HORIZ_OFFSET = FONTSIZE;

    /** holding the orientation of this instance. */
    private final boolean m_horizontal;

    /** the full length of the ruler (in pixels). */
    private int m_fullLength;

    /**
     * The underlying coordinate for this axis header.
     */
    private Coordinate m_coordinate;

    /**
     * This is the offset to start the ticks from. This is necessary, i.e. for
     * the scatter plotter which would set half the current dot size, which must
     * be painted below its actual value.
     */
    private int m_startTickOffset;

    private boolean m_rotateXLabels;

    private boolean m_rotateYLabels;

    // private int m_nrOfTicks;
    private int m_tickDist;

    private List<Integer> m_tickPositions;

    private int m_tickLength;

    private CoordinateMapping[] m_coordMap;

    private static final DecimalFormat SCIENTIFIC = new DecimalFormat("0.##E0");

    private static final DecimalFormat NORMAL =
            new DecimalFormat("#######0.###");

    private DecimalFormat m_currFormat = NORMAL;

    private List<ChangeListener> m_changeListener =
            new LinkedList<ChangeListener>();

    private JRadioButtonMenuItem m_notationNormalRB;

    private JRadioButtonMenuItem m_notationScientificRB;

    /**
     * Adds a {@link ChangeListener}, which is notified if repaint is
     * necessary.
     *
     * @param listener the {@link ChangeListener}
     */
    public void addChangeListener(final ChangeListener listener) {
        m_changeListener.add(listener);
    }

    private void notifyChangeListeners() {
        if (m_changeListener.size() == 0) {
            return;
        }
        for (ChangeListener listener : m_changeListener) {
            listener.stateChanged(new ChangeEvent(this));
        }
    }

    /**
     * Creates a new ruler in either horizontal or vertical orientation.
     *
     * @param orientation specifies the orientation of this instance. Use
     *            Header.HORIZONTAL, or Header.VERTICAL
     * @param length the initial entire length of the ruler in pixels.
     */
    public Axis(final int orientation, final int length) {
        if ((orientation != HORIZONTAL) && (orientation != VERTICAL)) {
            throw new IllegalArgumentException("Argument 'orientation' must"
                    + " be either Header.HORIZONTAL or Header.VERTICAL.");
        }
        m_fullLength = length;
        m_tickPositions = new LinkedList<Integer>();
        m_horizontal = (orientation == HORIZONTAL);
        setToolTipText("complete label");
        m_tickLength = TICKLENGTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent event) {
        if (m_coordMap == null || m_coordMap.length == 0 || m_tickDist == 0) {
            return "";
        }
        int pivot;
        if (m_horizontal) {
            pivot = event.getX() - m_startTickOffset;
            for (int i = 0; i < m_coordMap.length; i++) {
                CoordinateMapping mapping = m_coordMap[i];
                if (pivot > mapping.getMappingValue() - m_tickDist / 3
                        && pivot < mapping.getMappingValue() + m_tickDist / 3) {
                    return createToolTip(mapping);
                }
            }
        } else {
            // map the y value to screen coordinates
            pivot = m_fullLength - event.getY();
            for (CoordinateMapping mapping : m_coordMap) {
                if (pivot > mapping.getMappingValue() - m_tickDist / 3
                        && pivot < mapping.getMappingValue() + m_tickDist / 3) {
                    return createToolTip(mapping);
                }
            }
        }

        return "";
    }

    private String createToolTip(final CoordinateMapping coordMapping) {
        String tooltip = "";
        for (DataValue v : coordMapping.getValues()) {
            if (v == null) {
                continue;
            }
            if (v instanceof DoubleValue) {
                double value = ((DoubleValue)v).getDoubleValue();
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    continue;
                }
                tooltip += new BigDecimal(value, new MathContext(25)) + " ";
            } else {
                tooltip += v.toString() + " ";
            }
        }
        return tooltip.trim();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (g.getClipBounds() == null) {
            return;
        }

        m_tickPositions.clear();

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int x = 0;
        int y = 0;
        int width;
        int height;
        if (m_horizontal) {
            width = m_fullLength;
            height = SIZE;
        } else {
            width = SIZE;
            height = m_fullLength;
        }
        // Do the ruler labels in a small font that's black.
        g.setFont(RULER_FONT);
        g.setColor(Color.black);

        // the length we draw the ruler in
        int length;
        if (m_horizontal) {
            length = width;
        } else {
            length = height;
        }
        if (length <= 5) {
            // no use in drawing in such a small area
            return;
        }

        // draw the axis
        if (m_horizontal) {
            g.drawLine(x, y + 2, x + width, y + 2);
        } else {
            g.drawLine(x + width - 1, y, x + width - 1, y + height);
        }

        // draw the ticks and the labels
        // the offset must be subtracted 2 times
        if (m_coordinate == null) {
            return;
        }

        m_coordMap =
                m_coordinate.getTickPositions(length - 2 * m_startTickOffset);

        if (m_coordMap == null || m_coordMap.length == 0) {
            return;
        }

        // implicit assumption: equidistant ticks
        m_tickDist = length / m_coordMap.length;

        // better: at least real distance between 2 ticks
        if (m_coordMap.length >= 2) {
            int v2 = (int)m_coordMap[m_coordMap.length - 1].getMappingValue();
            int v1 = (int)m_coordMap[m_coordMap.length - 2].getMappingValue();
            m_tickDist = v2 - v1;
        }

        int minDecimals = m_currFormat.getMinimumFractionDigits();
        int maxDecimals = m_currFormat.getMaximumFractionDigits();

        // search for too long labels
        List<String> labels = new LinkedList<String>();
        int decimals = getDecimals(labels, g.getFontMetrics());

        if (m_horizontal) {
            // check the distance between the ticks
            m_rotateXLabels =
                    LabelPaintUtil.rotateLabels(labels,
                            (int)(m_tickDist * 1.2), g.getFontMetrics());
        } else {
            m_rotateYLabels =
                    LabelPaintUtil.rotateLabels(labels, SIZE - m_tickLength, g
                            .getFontMetrics());
        }

        m_currFormat.setMinimumFractionDigits(decimals);
        m_currFormat.setMaximumFractionDigits(decimals);

        if (m_coordinate.isNominal()) {
            m_coordMap =
                    ((NominalCoordinate)m_coordinate)
                            .getReducedTickPositions(length - 2
                                    * m_startTickOffset);

        }

        // this offset is for the label drawing in the horizontal header
        // it puts the labels alternatively up and down to have more
        // space for the labeling
        boolean useOffset = false;

        Set<String> drawnLabels = new HashSet<String>();

        for (int i = 0; i < m_coordMap.length; i++) {

            CoordinateMapping mapping = m_coordMap[i];
            String label = mapping.getDomainValueAsString();
            try {
                double value = Double.parseDouble(label);
                if (Math.abs(value) <= 1e-15) {
                    label = "0";
                } else {
                    label = formatDouble(value);
                }
            } catch (Exception e) {
                // no number.. no formatting necessary
            }

            if (drawnLabels.contains(label) && !label.equals("...")) {
                continue;
            }

            drawnLabels.add(label);
            drawTick(g, (long)mapping.getMappingValue() + m_startTickOffset,
                    label, useOffset);
            useOffset = !useOffset;
        }
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        m_currFormat.setMinimumFractionDigits(minDecimals);
        m_currFormat.setMaximumFractionDigits(maxDecimals);
    }

    private int getDecimals(final List<String> labels, final FontMetrics fm) {
        int decimals = 0;
        boolean tooLong = false;

        for (CoordinateMapping cm : m_coordMap) {
            String l = cm.getDomainValueAsString();
            try {
                double value = Double.parseDouble(l);
                if (Math.abs(value) <= 1e-15) {
                    l = "0";
                } else {
                    l = formatDouble(value);
                }
            } catch (Exception e) {
                // no number.. no formatting necessary
            }
            labels.add(l);
            if (!m_horizontal) {
                if (fm.stringWidth(l) > SIZE - m_tickLength) {
                    tooLong = true;
                }
            } else {
                if (fm.stringWidth(l) > m_tickDist * 1.4) {
                    tooLong = true;
                }
            }
            if (l.contains(".") && l.contains("E")) {
                int temp = l.indexOf('E') - l.indexOf('.') - 1;
                if (temp > decimals) {
                    decimals = temp;
                }
            } else if (l.contains(".")) {
                int temp = l.length() - l.indexOf('.') - 1;
                if (temp > decimals) {
                    decimals = temp;
                }
            }
        }
        if (tooLong) {
            if (m_notationScientificRB != null) {
                m_notationScientificRB.setSelected(true);
                m_notationNormalRB.setEnabled(false);
            }
        }

        return decimals;
    }

    /*
     * draws a tick and writes a label at the corresponding place. Takes into
     * account the orientation of the header.
     */
    private void drawTick(final Graphics g, final long at, final String label,
            final boolean useOffset) {
        if (label.equals("...")) { // no tick at all
            m_tickLength = 0;
        }
        if (m_horizontal) {
            drawHorizontalTick(g, at, label, useOffset);
        } else {
            drawVerticalTick(g, at, label);
        }
        if (m_tickLength == 0) {
            m_tickLength = TICKLENGTH;
        }
    }

    private void drawHorizontalTick(final Graphics g, final long at,
            final String label, final boolean useOffset) {
        if (at <= m_fullLength) {
            int x = (int)at;
            // if this would mean that the label is not displayed
            // at the left border set x to 0
            if (x < 0) {
                x = 0;
            }
            g.drawLine(x, 2, x, m_tickLength + 2);

            // if (!m_coordinate.isNominal()) {
            // for the label we adjust the coordinates
            int lablePixelLength = g.getFontMetrics().stringWidth(label);

            // place the label in the middle of a tick
            if (!m_rotateXLabels) {
                x -= lablePixelLength / 2;
            }

            if (x + lablePixelLength > m_fullLength) {
                // if the label would be printed beyond the right border

                if (m_rotateXLabels) {
                    x = (int)(x - (0.25 * lablePixelLength));
                } else {
                    x = m_fullLength - lablePixelLength;
                }
            }
            // }

            if (x < 0) {
                x = 0;
            }

            // store the tick position for later tool tip retrieval
            m_tickPositions.add(x);
            int labelY = SIZE - m_tickLength - 3;
            if (useOffset) {
                labelY -= HORIZ_OFFSET;
            }
            if (labelY < FONTSIZE) {
                labelY = FONTSIZE;
            }
            if (m_coordinate.isNominal() && m_rotateXLabels) {
                Rectangle rect =
                        new Rectangle(x, m_tickLength
                                + g.getFontMetrics().getHeight(), m_tickDist,
                                SIZE - m_tickLength);
                LabelPaintUtil.drawLabel(label, (Graphics2D)g, rect,
                        LabelPaintUtil.Position.BOTTOM, m_rotateXLabels);
            } else {
                g.drawString(label, x, labelY);
            }

        }
    }

    private void drawVerticalTick(final Graphics g, final long at,
            final String label) {
        if (at <= m_fullLength) {
            // int y = (int) at;
            // have the origin sitting in the lower left

            int y = m_fullLength - (int)at;
            if (y == m_fullLength) {
                y -= 1;
            }

            g.drawLine(SIZE - m_tickLength - 1, y, SIZE - 1, y);

            m_tickPositions.add(y);

            // System.out.println(m_rotateYLabels);

            if (!m_coordinate.isNominal()) { // for the label we adjust the
                // coordinates
                int lablePixelHeight = g.getFontMetrics().getHeight();
                y += lablePixelHeight / 2 - 1;
                // if (y < lablePixelHeight) {
                // y += g.getFontMetrics().getAscent();
                // } else
                if (y + lablePixelHeight > m_fullLength) {
                    // if the label would be printed beyond the border
                    y = m_fullLength;
                }
                if (y < lablePixelHeight) {
                    // move the upper tick a bit
                    y = lablePixelHeight;
                }
                g.drawString(label, SIZE - (int)(1.4 * m_tickLength)
                        - g.getFontMetrics().stringWidth(label), y);
                // + g.getFontMetrics().getHeight() / 3);
            } else {

                Rectangle rect =
                        new Rectangle(m_tickLength, y - m_tickDist, SIZE
                                - (2 * m_tickLength), m_tickDist);
                // new Rectangle(0, y - m_tickDist, SIZE - m_tickLength,
                // m_tickDist);
                LabelPaintUtil.drawLabel(label, (Graphics2D)g, rect,
                        LabelPaintUtil.Position.LEFT, m_rotateYLabels);
            }
        }
    }

    /**
     * Sets the preferred size of the component. Depending on the orientation,
     * the parameter <code>l</code> will either specify the width or height.
     * The values set (start and end) will be evenly spread over the entire
     * length.
     *
     * @param l the length of the ruler
     */
    public void setPreferredLength(final int l) {
        m_fullLength = l;
        if (m_horizontal) {
            setPreferredSize(new Dimension(l, SIZE));
        } else {
            setPreferredSize(new Dimension(SIZE, l));
        }
    }

    /**
     * Sets the underlying coordinate for this header.
     *
     * @param coordinate the coordinate to set
     */
    public void setCoordinate(final Coordinate coordinate) {
        m_coordinate = coordinate;
        SCIENTIFIC.setMinimumFractionDigits(0);
        SCIENTIFIC.setMaximumFractionDigits(2);
        NORMAL.setMinimumFractionDigits(0);
        NORMAL.setMaximumFractionDigits(3);
        this.setComponentPopupMenu(createPopupMenu());
        repaint();
    }

    private JPopupMenu createPopupMenu() {
        if (getCoordinate() == null) {
            return null;
        }
        JPopupMenu popupMenu = new JPopupMenu();
        createNotationMenu(popupMenu);

        // policies
        if (m_coordinate != null) {
            List<PolicyStrategy> strategies = new LinkedList<PolicyStrategy>();
            if (m_coordinate.getCompatiblePolicies() != null) {
                strategies.addAll(m_coordinate.getCompatiblePolicies());
                Collections.sort(strategies, new Comparator<PolicyStrategy>() {
                    /**
                     * {@inheritDoc}
                     */
                    public int compare(final PolicyStrategy o1,
                            final PolicyStrategy o2) {
                        return o1.getDisplayName().compareTo(
                                o2.getDisplayName());
                    }
                });
            }
            JMenu tickPolicyMenu = new JMenu("Tick policies");
            ButtonGroup tickPolicyButtons = new ButtonGroup();
            if (strategies.size() > 0) {
                popupMenu.add(tickPolicyMenu);

                for (PolicyStrategy strategy : strategies) {
                    final PolicyStrategy tempStrategy = strategy;
                    JRadioButtonMenuItem tickPolicy =
                            new JRadioButtonMenuItem(strategy.getDisplayName());
                    tickPolicyButtons.add(tickPolicy);
                    tickPolicyMenu.add(tickPolicy);
                    if (strategy.equals(m_coordinate.getCurrentPolicy())) {
                        tickPolicy.setSelected(true);
                    }
                    tickPolicy.addItemListener(new ItemListener() {
                        /**
                         * {@inheritDoc}
                         */
                        public void itemStateChanged(final ItemEvent e) {
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                m_coordinate.setPolicy(tempStrategy);
                                notifyChangeListeners();
                            }
                        }
                    });
                }
            } // add strategies

            Set<MappingMethod> mappingMethods =
                    m_coordinate.getCompatibleMappingMethods();
            JMenu mappingMethodMenu = new JMenu("Mapping Methods");
            if (mappingMethods != null && mappingMethods.size() > 0) {
                popupMenu.add(mappingMethodMenu);
                ButtonGroup buttons = new ButtonGroup();
                final Map<MappingMethod, JRadioButtonMenuItem> checkboxes =
                        new HashMap<MappingMethod, JRadioButtonMenuItem>();
                JRadioButtonMenuItem none =
                        new JRadioButtonMenuItem("none", true);
                buttons.add(none);
                none.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(final ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            List<MappingMethod> list =
                                    new LinkedList<MappingMethod>();
                            m_coordinate.setActiveMappingMethods(list);
                        }
                    }
                });
                mappingMethodMenu.add(none);
                for (MappingMethod method : mappingMethods) {
                    final MappingMethod tempMethod = method; // final needed
                    JRadioButtonMenuItem checkbox =
                            new JRadioButtonMenuItem(method.getDisplayName(),
                                    false);
                    checkbox
                            .setEnabled(method
                                    .isCompatibleWithDomain(getCoordinate()
                                            .getDomain()));
                    checkboxes.put(method, checkbox);
                    buttons.add(checkbox);
                    if (m_coordinate.getActiveMappingMethods().contains(method)) {
                        checkbox.setSelected(true);
                    }
                    checkbox.addItemListener(new ItemListener() {
                        /**
                         * {@inheritDoc}
                         */
                        public void itemStateChanged(final ItemEvent e) {
                            int stateChange = e.getStateChange();
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                if (!m_coordinate.getActiveMappingMethods()
                                        .contains(tempMethod)) {
                                    List<MappingMethod> list =
                                            new LinkedList<MappingMethod>();
                                    m_coordinate.setActiveMappingMethods(list);
                                    m_coordinate
                                            .addActiveMappingMethod(tempMethod);
                                }
                            } else if (stateChange == ItemEvent.DESELECTED) {
                                m_coordinate
                                        .removeActiveMappingMethod(tempMethod);
                            }
                            for (Map.Entry<MappingMethod, JRadioButtonMenuItem> entry : checkboxes
                                    .entrySet()) {
                                if (entry.getKey().isCompatibleWithDomain(
                                        getCoordinate().getDomain())) {
                                    entry.getValue().setEnabled(true);
                                } else {
                                    if (!entry.getValue().isSelected()) {
                                        // the user should be able to disable a
                                        // mapping method
                                        entry.getValue().setEnabled(false);
                                    }
                                }
                            }
                            notifyChangeListeners();
                        }
                    });
                    mappingMethodMenu.add(checkbox);
                }

                for (Map.Entry<MappingMethod, JRadioButtonMenuItem> entry : checkboxes
                        .entrySet()) {
                    if (entry.getKey().isCompatibleWithDomain(
                            getCoordinate().getDomain())) {
                        entry.getValue().setEnabled(true);
                    } else {
                        if (!entry.getValue().isSelected()) {
                            entry.getValue().setEnabled(false);
                        }
                    }
                }
            }
        }
        if (popupMenu.getComponentCount() < 1) {
            return null;
        }
        return popupMenu;
    }

    private void createNotationMenu(final JPopupMenu popupMenu) {
        // notation
        if (m_coordinate != null && !m_coordinate.isNominal()) {

            JMenu notationsMenu = new JMenu("Notations");
            ButtonGroup notationBG = new ButtonGroup();

            m_notationScientificRB = new JRadioButtonMenuItem("scientific");
            m_notationScientificRB.setSelected(m_currFormat == SCIENTIFIC);
            m_notationScientificRB.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        m_currFormat = SCIENTIFIC;
                    }
                    repaint();
                }
            });
            notationBG.add(m_notationScientificRB);

            m_notationNormalRB = new JRadioButtonMenuItem("normal");
            m_notationNormalRB.setSelected(m_currFormat == NORMAL);
            m_notationNormalRB.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        m_currFormat = NORMAL;
                    }
                    repaint();
                }
            });
            notationBG.add(m_notationNormalRB);

            notationsMenu.add(m_notationScientificRB);
            notationsMenu.add(m_notationNormalRB);
            popupMenu.add(notationsMenu);
        }
    }

    /**
     * Set the offset to start the tick-painting from. This is necessary, i.e.
     * for the scatter plotter which would set half the current dot size, which
     * must be painted below its actual value.
     *
     * @param dotSizeOffset the offset
     */
    public void setStartTickOffset(final int dotSizeOffset) {
        m_startTickOffset = dotSizeOffset;
    }

    /**
     *
     * @return the tick offset
     */
    public int getTickOffset() {
        return m_startTickOffset;
    }

    /**
     * @return the underlying coordinate
     */
    public Coordinate getCoordinate() {
        return m_coordinate;
    }

    private String formatDouble(final double d) {
        m_currFormat.setRoundingMode(RoundingMode.FLOOR);
        return m_currFormat.format(d);
    }
}
