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
import java.text.DecimalFormatSymbols;
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
    private static final long serialVersionUID = 7974028405608643754L;

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

    private int m_tickLength;

    private CoordinateMapping[] m_coordMap;

    private final DecimalFormat SCIENTIFIC = new DecimalFormat("0.##E0");

    private final DecimalFormat NORMAL = new DecimalFormat("#######0.###");

    private DecimalFormat m_currFormat = NORMAL;

    private List<ChangeListener> m_changeListener =
            new LinkedList<ChangeListener>();

    private JRadioButtonMenuItem m_notationNormalRB;

    private JRadioButtonMenuItem m_notationScientificRB;

    private JMenu m_mappingMethodMenu;

    private JMenu m_notationsMenu;

    private final boolean m_inverse;

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
        this(orientation, length, false);
    }

    /**
     * Creates a new ruler in either horizontal or vertical orientation.
     *
     * @param orientation specifies the orientation of this instance. Use
     *            Header.HORIZONTAL, or Header.VERTICAL
     * @param length the initial entire length of the ruler in pixels.
     * @param inverse true if the the values on the axes should be reversed
     */
    public Axis(final int orientation, final int length,
            final boolean inverse) {
        if ((orientation != HORIZONTAL) && (orientation != VERTICAL)) {
            throw new IllegalArgumentException("Argument 'orientation' must"
                    + " be either Header.HORIZONTAL or Header.VERTICAL.");
        }
        m_fullLength = length;
        m_horizontal = (orientation == HORIZONTAL);
        setToolTipText("complete label");
        m_tickLength = TICKLENGTH;
        m_inverse = inverse;
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
                if (pivot > mapping.getMappingValue() - m_tickDist / 3.0
                        && pivot < mapping.getMappingValue() + m_tickDist / 3.0) {
                    return createToolTip(mapping);
                }
            }
        } else {
            // map the y value to screen coordinates
            pivot = m_fullLength - event.getY();
            for (CoordinateMapping mapping : m_coordMap) {
                if (pivot > mapping.getMappingValue() - m_tickDist / 3.0
                        && pivot < mapping.getMappingValue() + m_tickDist / 3.0) {
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
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (g.getClipBounds() == null) {
            return;
        }

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
            long pos = (long)mapping.getMappingValue() + m_startTickOffset;
            drawTick(g,
                    pos,
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
        int exponent = 0;
        boolean negative = false;
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
                if (value < -1e-15) {
                    negative = true;
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

            DecimalFormatSymbols symbols = m_currFormat.getDecimalFormatSymbols();
            int decimalIndex = l.indexOf(symbols.getDecimalSeparator());
            int exponentIndex = l.indexOf(symbols.getExponentSeparator());

            if ((decimalIndex > -1) && (exponentIndex > -1)) {
                int temp = exponentIndex - decimalIndex - 1;
                if (temp > decimals) {
                    decimals = temp;
                }
                temp = l.length() - exponentIndex - 1;
                if (temp > exponent) {
                    exponent = temp;
                }
            } else if (decimalIndex > -1) {
                int temp = l.length() - decimalIndex - 1;
                if (temp > decimals) {
                    decimals = temp;
                }
            }
        }

        // exponent would not be greater than 3. Space for 4 Values should be
        // given.
        if (exponent + decimals > 4) {
            decimals = 4 - exponent;
        }

        if (negative || m_horizontal) {
            if (exponent + decimals > 3) {
                decimals = 3 - exponent;
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
            int y = 2;
            if (m_inverse) {
                y -= m_tickLength;
            } else {
                y += m_tickLength;
            }
            g.drawLine(x, 2, x, y);

            // if (!m_coordinate.isNominal()) {
            // for the label we adjust the coordinates
            int labelPixelLength = g.getFontMetrics().stringWidth(label);

            // place the label in the middle of a tick
            if (!m_rotateXLabels || !getCoordinate().isNominal()) {
                x -= labelPixelLength / 2;
            }

            if (x + labelPixelLength > m_fullLength) {
                // if the label would be printed beyond the right border
                if (m_rotateXLabels && getCoordinate().isNominal()) {
                    x = (int)(x - (0.25 * labelPixelLength));
                } else {
                    x = m_fullLength - labelPixelLength;
                }
            }
            // }

            if (x < 0) {
                x = 0;
            }


            int labelY = SIZE - m_tickLength - 3;
            if (useOffset) {
                labelY -= HORIZ_OFFSET;
            }
            if (m_inverse) {
                labelY -= SIZE;
            }
            if (m_coordinate.isNominal()) {
                Rectangle rect =
                        new Rectangle(x, m_tickLength
                                + g.getFontMetrics().getHeight(), m_tickDist,
                                SIZE - m_tickLength);
                if (m_inverse) {
                    int trans = SIZE + m_tickLength + FONTSIZE + 5;
                    g.translate(0, -trans);
                    LabelPaintUtil.drawLabel(label, (Graphics2D)g, rect,
                            LabelPaintUtil.Position.TOP, m_rotateXLabels);
                    g.translate(0, trans);
                } else {
                    LabelPaintUtil.drawLabel(label, (Graphics2D)g, rect,
                        LabelPaintUtil.Position.BOTTOM, m_rotateXLabels);
                }
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

            int xLabelStart = -1;
//            int xLabelEnd = -1;
            int xTickStart = -1;
            int xTickEnd = -1;

            if (m_inverse) {
                xTickStart = getX() + SIZE;
                xTickEnd = xTickStart + m_tickLength;
                if (m_coordinate.isNominal()) {
                    xLabelStart = SIZE + (int)(1.4 * m_tickLength);
                } else {
                    xLabelStart = xTickStart + (int)(1.4 * m_tickLength);
//                    xLabelEnd = xLabelStart + g.getFontMetrics().stringWidth(
//                            label);
                }
            } else {
                xTickStart = SIZE - 2;
                xTickEnd = xTickStart - m_tickLength;
                if (m_coordinate.isNominal()) {
                    xLabelStart = m_tickLength;
//                    xLabelEnd = xLabelStart + g.getFontMetrics().stringWidth(
//                            label);
                } else {
                    xLabelStart = SIZE - (int)(1.4 * m_tickLength)
                        - g.getFontMetrics().stringWidth(label);
//                    xLabelEnd = SIZE - (int)(1.4 * m_tickLength);
                }
            }
            g.drawLine(xTickStart, y, xTickEnd, y);
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
                g.drawString(label, xLabelStart, y);
            } else {
                Rectangle rect =
                        new Rectangle(xLabelStart, y - m_tickDist,
                                SIZE - (int)(1.4 * m_tickLength), m_tickDist);
                if (m_inverse) {
                    LabelPaintUtil.drawLabel(label, (Graphics2D)g, rect,
                            LabelPaintUtil.Position.RIGHT, m_rotateYLabels);
                } else {
                    LabelPaintUtil.drawLabel(label, (Graphics2D)g, rect,
                            LabelPaintUtil.Position.LEFT, m_rotateYLabels);
                }
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
        m_currFormat = NORMAL;
        JPopupMenu popupMenu = new JPopupMenu();
        createNotationMenu(popupMenu);

        // policies
        if (m_coordinate != null) {
            List<PolicyStrategy> strategies = new LinkedList<PolicyStrategy>();
            Set<PolicyStrategy>policies = m_coordinate.getCompatiblePolicies();
            if (policies != null) {
                strategies.addAll(policies);
                Collections.sort(strategies, new Comparator<PolicyStrategy>() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
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
                        @Override
                        public void itemStateChanged(final ItemEvent e) {
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                m_coordinate.setPolicy(tempStrategy);
                                // recreate popup menu
                                setComponentPopupMenu(createPopupMenu());
                                if (tempStrategy.isMappingAllowed()) {
                                    m_mappingMethodMenu.setEnabled(true);
                                    m_notationsMenu.setEnabled(true);
                                } else {
                                    // hide mapping methods
                                    m_mappingMethodMenu.setEnabled(false);
                                    m_notationsMenu.setEnabled(false);
                                    if (getCoordinate() != null) {
                                        getCoordinate().setActiveMappingMethod(
                                                null);
                                    }
                                }
                                notifyChangeListeners();
                            }
                        }
                    });
                }
            } // add strategies

            Set<MappingMethod> mappingMethods =
                    m_coordinate.getCompatibleMappingMethods();
            m_mappingMethodMenu = new JMenu("Mapping Methods");
            if (mappingMethods != null && mappingMethods.size() > 0) {
                popupMenu.add(m_mappingMethodMenu);
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
                            m_coordinate.setActiveMappingMethod(null);
                            notifyChangeListeners();
                        }
                    }
                });
                m_mappingMethodMenu.add(none);
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
                    if (method.equals(m_coordinate.getActiveMappingMethod())) {
                        checkbox.setSelected(true);
                    }
                    checkbox.addItemListener(new ItemListener() {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void itemStateChanged(final ItemEvent e) {
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                m_coordinate.setActiveMappingMethod(tempMethod);
                            }
                            notifyChangeListeners();
                        }
                    });
                    m_mappingMethodMenu.add(checkbox);
                }

                for (Map.Entry<MappingMethod, JRadioButtonMenuItem> entry
                        : checkboxes.entrySet()) {
                    if (entry.getKey().isCompatibleWithDomain(
                            getCoordinate().getDomain())) {
                        entry.getValue().setEnabled(true);
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

            m_notationsMenu = new JMenu("Notations");
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

            m_notationsMenu.add(m_notationScientificRB);
            m_notationsMenu.add(m_notationNormalRB);
            popupMenu.add(m_notationsMenu);
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
