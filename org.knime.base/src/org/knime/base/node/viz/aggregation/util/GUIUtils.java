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
 *    21.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation.util;

import org.knime.core.node.util.ButtonGroupEnumInterface;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationValModel;
import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.aggregation.ValueScale;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;


/**
 * This class provides different methods which are used in multiple aggregation
 * implementations mainly in the GUI creation.
 * @author Tobias Koetter, University of Konstanz
 */
public final class GUIUtils {

    /**This message is displayed in the details tab if no element is selected.*/
    public static final String NO_ELEMENT_SELECTED_TEXT =
        "Select an element for detailed information";

    /**Used to format the aggregation value for the aggregation method count.*/
    private static final DecimalFormat AGGREGATION_LABEL_FORMATER_COUNT =
        new DecimalFormat("#");

    private GUIUtils() {
        //avoid object creation
    }

    /**
     * @param elements the elements to create the button group for and selects
     * the last element which returns <code>true</code> as isDefault()
     * @param l the optional listener to add to each button of this group
     * @return the button group
     */
    public static ButtonGroup createButtonGroup(
            final ButtonGroupEnumInterface[] elements,
            final ActionListener l) {
       return createButtonGroup(elements, null, l);
    }
    /**
     * @param elements the elements to create the button group for and selects
     * the last element which returns <code>true</code> as isDefault()
     * @param defaultButton the default group element to select or
     * <code>null</code> if the enumeration default element should be selected
     * @param l the optional listener to add to each button of this group
     * @return the button group
     */
    public static ButtonGroup createButtonGroup(
            final ButtonGroupEnumInterface[] elements,
            final ButtonGroupEnumInterface defaultButton,
            final ActionListener l) {
        final ButtonGroup group = new ButtonGroup();
        boolean defaultFound = false;
        for (final ButtonGroupEnumInterface element : elements) {
            final JRadioButton button = new JRadioButton(element.getText());
            button.setActionCommand(element.getActionCommand());
            if (defaultButton != null) {
                if (element.equals(defaultButton)) {
                    button.setSelected(true);
                    defaultFound = true;
                }
            } else {
                if (element.isDefault()) {
                    button.setSelected(true);
                    defaultFound = true;
                }
            }
            if (element.getToolTip() != null) {
                button.setToolTipText(element.getToolTip());
            }
            if (l != null) {
                button.addActionListener(l);
            }
            group.add(button);
        }
        if (!defaultFound && group.getButtonCount() > 0) {
            //select the first button if none is by default selected
            group.getElements().nextElement().setSelected(true);
        }
        return group;
    }

    /**
     * @param group the button group to put in a swing box
     * @param vertical if the group should be layout vertical
     * @param label the label of the swing box
     * @param border <code>true</code> if the label should be displayed
     * in a surrounding border
     * @return a swing box with the buttons of the given group
     */
    public static Box createButtonGroupBox(final ButtonGroup group,
                final boolean vertical, final String label,
                final boolean border) {
        Box buttonBox = null;
        if (vertical) {
            buttonBox = Box.createVerticalBox();
            buttonBox.add(Box.createVerticalGlue());
            if (!border && label != null) {
                buttonBox.add(new JLabel(label));
                buttonBox.add(Box.createVerticalGlue());
            }
        } else {
            buttonBox = Box.createHorizontalBox();
            buttonBox.add(Box.createHorizontalGlue());
            if (!border && label != null) {
                buttonBox.add(new JLabel(label));
                buttonBox.add(Box.createHorizontalGlue());
            }
        }
        if (border && label != null) {
            buttonBox.setBorder(
                    BorderFactory.createTitledBorder(BorderFactory
                    .createEtchedBorder(), label));
        }
        for (final Enumeration<AbstractButton> buttons =
            group.getElements();
            buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            buttonBox.add(button);
            if (vertical) {
                buttonBox.add(Box.createVerticalGlue());
            } else {
                buttonBox.add(Box.createHorizontalGlue());
            }
        }
        return buttonBox;
    }

    /**
     * @param aggrVal the value to use as label
     * @param noOfDigits the number of digits if it's a floating point number
     * @param aggrMethod the {@link AggregationMethod}
     * @return the rounded aggregation value as <code>String</code> label
     */
    public static String createLabel(final double aggrVal, final int noOfDigits,
            final AggregationMethod aggrMethod) {
        return createLabel(aggrVal, noOfDigits, aggrMethod,
                ValueScale.ORIGINAL);
    }

    /**
     * @param aggrVal the value to use as label
     * @param noOfDigits the number of digits if it's a floating point number
     * @param aggrMethod the {@link AggregationMethod}
     * @param scale the {@link ValueScale}
     * @return the rounded aggregation value as <code>String</code> label
     */
    public static String createLabel(final double aggrVal, final int noOfDigits,
            final AggregationMethod aggrMethod, final ValueScale scale) {
        // return Double.toString(aggrVal);
        if ((AggregationMethod.COUNT.equals(aggrMethod)
                || AggregationMethod.VALUE_COUNT.equals(aggrMethod))
                && !ValueScale.PERCENT.equals(scale)) {
            return AGGREGATION_LABEL_FORMATER_COUNT.format(aggrVal);
        }
        // the given doubleVal is less then zero
        final char[] interval = Double.toString(aggrVal).toCharArray();
        final StringBuilder decimalFormatBuf = new StringBuilder();
        boolean digitFound = false;
        int digitCounter = 0;
        int positionCounter = 0;
        boolean dotFound = false;
        for (final int length = interval.length; positionCounter < length
                && digitCounter <= noOfDigits; positionCounter++) {
            final char c = interval[positionCounter];
            if (c == '.') {
                decimalFormatBuf.append(".");
                dotFound = true;
            } else {
                if (c != '0' || digitFound) {
                    digitFound = true;
                    if (dotFound) {
                        digitCounter++;
                    }
                }
                if (digitCounter <= noOfDigits) {
                    decimalFormatBuf.append("#");
                }
            }
        }
        final DecimalFormat df = new DecimalFormat(decimalFormatBuf.toString());
        final String resultString = df.format(aggrVal);
        if (scale.getExtension() != null) {
            final StringBuilder buf = new StringBuilder(resultString);
            buf.append(' ');
            buf.append(scale.getExtension());
            return buf.toString();
        }
        return resultString;
    }

    /**
     * @param vals the values to create the details information for
     * @return the html detail representation of the given values
     */
    @SuppressWarnings("unchecked")
    public static String createHTMLDetailData(
            final List<? extends AggregationValModel> vals) {
        if (vals == null || vals.size() < 1) {
            return (NO_ELEMENT_SELECTED_TEXT);
        }
        final StringBuilder aggrHeadBuf = new StringBuilder();
        aggrHeadBuf.append("<th>");
        aggrHeadBuf.append(AggregationMethod.COUNT);
        aggrHeadBuf.append("</th>");
        aggrHeadBuf.append("<th>");
        aggrHeadBuf.append(AggregationMethod.VALUE_COUNT);
        aggrHeadBuf.append("</th>");
        aggrHeadBuf.append("<th>");
        aggrHeadBuf.append(AggregationMethod.SUM);
        aggrHeadBuf.append("</th>");
        aggrHeadBuf.append("<th>");
        aggrHeadBuf.append(AggregationMethod.AVERAGE);
        aggrHeadBuf.append("</th>");
        final String aggrMethodHead = aggrHeadBuf.toString();
        final StringBuilder buf = new StringBuilder();
        buf.append("<table border='1'>");
        for (final AggregationValModel bar : vals) {
            final String barBgColor = "#" + Integer.toHexString(
                    bar.getColor().getRGB() & 0x00ffffff);
            buf.append("<tr>");
            buf.append("<td rowspan='2' bgcolor='");
            buf.append(barBgColor);
            buf.append("'>");
            buf.append("&nbsp;");
            buf.append("</td>");
            buf.append("<td bgcolor='");
            buf.append(barBgColor);
            buf.append("'>");
            buf.append(bar.getName());
            buf.append("</td>");
            buf.append("</tr>");
            buf.append("<tr>");
            buf.append("<td>");
            final Collection<AggregationValSubModel> selectedElements =
                bar.getSelectedElements();
            if (selectedElements == null
                    || selectedElements.size() < 1) {
                buf.append("No elements selected");
            } else {
                //element table
                buf.append("<table border='1' width='100%'>");
                //display all aggregation values of the
                //selected element
                buf.append("<tr>");
                buf.append("<th>");
                buf.append("&nbsp;");
                buf.append("</th>");
                buf.append(aggrMethodHead);
                buf.append("</tr>");
                int totalCount = 0;
                int totalValCount = 0;
                double totalSum = 0;
                for (final AggregationValSubModel element
                        : selectedElements) {
                    final String bgColor = "#" + Integer.toHexString(
                            element.getColor().getRGB() & 0x00ffffff);
                    buf.append("<tr>");
                    buf.append("<td bgcolor='");
                    buf.append(bgColor);
                    buf.append("'>");
                    buf.append("&nbsp;");
                    buf.append("</td>");
                    final int count = (int) element.getAggregationValue(
                            AggregationMethod.COUNT);
                    totalCount += count;
                    buf.append("<td>");
                    buf.append(count);
                    buf.append("</td>");
                    final int valCount = (int) element.getAggregationValue(
                            AggregationMethod.VALUE_COUNT);
                    totalValCount += valCount;
                    buf.append("<td>");
                    buf.append(valCount);
                    buf.append("</td>");
                    final double sum = element.getAggregationValue(
                            AggregationMethod.SUM);
                    totalSum += sum;
                    buf.append("<td>");
                    buf.append(sum);
                    buf.append("</td>");
                    buf.append("<td>");
                    if (count != 0) {
                        buf.append(sum / count);
                    } else {
                        buf.append("&nbsp;");
                    }
                    buf.append("</td>");
                    buf.append("</tr>");
                }
                if (selectedElements.size() > 1) {
                    //the element summary row
                    buf.append("<tr>");
                    buf.append("<td>");
                    buf.append("Total:");
                    buf.append("</td>");
                    buf.append("<td>");
                    buf.append(totalCount);
                    buf.append("</td>");
                    buf.append("<td>");
                    buf.append(totalValCount);
                    buf.append("</td>");
                    buf.append("<td>");
                    buf.append(totalSum);
                    buf.append("</td>");
                    buf.append("<td>");
                    if (totalCount != 0) {
                        buf.append(totalSum / totalCount);
                    } else {
                        buf.append("&nbsp;");
                    }
                    buf.append("</td>");
                    buf.append("</tr>");
                }
                buf.append("</table>");
            }
            buf.append("</td>");
            buf.append("</tr>");
        }
        buf.append("</table>");
        return buf.toString();
    }

    /**
     * Generates most distinct colors for neighbor indexes.
     * @param idx the current index
     * @param size the total number of colors to generate
     * @return the color for the current index
     */
    public static Color generateDistinctColor(final int idx, final int size) {
        final float hue;
        if (size == 0) {
            hue = 0;
        } else {
            //all even indices are place on the first half of the hue circle
            //while all uneven indices use the second half of the circle
            final int index;
            if (idx % 2 == 0) {
                index = idx / 2;
            } else {
                final int half = (int)Math.ceil(size / 2.0);
                index = half + (idx / 2);
            }
            hue = index / (float)size;
        }
        return Color.getColor(null, Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    /**
     * Sets the label of the given slider.
     *
     * @param slider the slider to label
     * @param divisor the steps are calculated
     *            <code>maxVal - minVal / divisor</code>
     */
    public static void setSliderLabels(final JSlider slider,
            final int divisor, final boolean showDigitsAndTicks) {
        // show at least the min, middle and max value on the slider.
        final int minimum = slider.getMinimum();
        final int maximum = slider.getMaximum();
        final int increment = (maximum - minimum) / divisor;
        if (increment < 1) {
            // if their is no increment we don't need to enable this slider
            // Hashtable labels = m_barWidth.createStandardLabels(1);
            final Hashtable<Integer, JLabel> labels =
                new Hashtable<Integer, JLabel>(1);
            labels.put(new Integer(minimum), new JLabel("Min"));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setEnabled(false);
        } else if (showDigitsAndTicks) {
            // slider.setLabelTable(slider.createStandardLabels(increment));
            final Hashtable<Integer, JLabel> labels =
                new Hashtable<Integer, JLabel>();
            // labels.put(minimum, new JLabel("Min"));
            labels.put(new Integer(minimum),
                    new JLabel(Integer.toString(minimum)));
            for (int i = 1; i < divisor; i++) {
                final int value = minimum + i * increment;
                labels.put(new Integer(value),
                        new JLabel(Integer.toString(value)));
            }
            // labels.put(maximum, new JLabel("Max"));
            labels.put(new Integer(maximum),
                    new JLabel(Integer.toString(maximum)));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setMajorTickSpacing(divisor);
            slider.setPaintTicks(true);
            // slider.setSnapToTicks(true);
            slider.setEnabled(true);
        } else {
            final Hashtable<Integer, JLabel> labels =
                new Hashtable<Integer, JLabel>();
            labels.put(new Integer(minimum), new JLabel("Min"));
            labels.put(new Integer(maximum), new JLabel("Max"));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setEnabled(true);
        }
    }
}
