/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *    21.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation.util;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Arrays;
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

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationValModel;
import org.knime.base.node.viz.aggregation.AggregationValSubModel;
import org.knime.base.node.viz.aggregation.ValueScale;
import org.knime.core.node.util.ButtonGroupEnumInterface;


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
     * @param valModels the values to create the details information for
     * @return the html detail representation of the given values
     */
    @SuppressWarnings("unchecked")
    public static String createHTMLDetailData(
            final List<? extends AggregationValModel> valModels) {
        final double[] vals = new double[3];
        Arrays.fill(vals, 0);
        return createHTMLDetailData(valModels, vals);
    }
    /**
     * @param valModels the values to create the details information for
     * @param vals the value array to add the current count, value count and
     * sum to
     * @return the html detail representation of the given values
     */
    @SuppressWarnings("unchecked")
    public static String createHTMLDetailData(
            final List<? extends AggregationValModel> valModels,
            final double[] vals) {
        if (valModels == null || valModels.size() < 1) {
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
        for (final AggregationValModel bar : valModels) {
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
                vals[0] += totalCount;
                vals[1] += totalValCount;
                vals[2] += totalSum;
                buf.append("</table>");
            }
            buf.append("</td>");
            buf.append("</tr>");
        }
        buf.append("</table>");
        return buf.toString();
    }

    /**
     * @param vals the array contains the total count, value count and sum
     * in this order
     * @return the html code with the total value information
     */
    public static String createHTMLTotalData(final double[] vals) {
        if (vals == null || vals.length != 3) {
            throw new IllegalArgumentException("invalid total values");
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("<table border='1' width='100%''>");
        buf.append("<tr>");
        buf.append("<th>");
        buf.append(AggregationMethod.COUNT);
        buf.append("</th>");
        buf.append("<th>");
        buf.append(AggregationMethod.VALUE_COUNT);
        buf.append("</th>");
        buf.append("<th>");
        buf.append(AggregationMethod.SUM);
        buf.append("</th>");
        buf.append("<th>");
        buf.append(AggregationMethod.AVERAGE);
        buf.append("</th>");
        buf.append("</tr>");
        buf.append("<tr>");
        buf.append("<td>");
        buf.append(vals[0]);
        buf.append("</td>");
        buf.append("<td>");
        buf.append(vals[1]);
        buf.append("</td>");
        buf.append("<td>");
        buf.append(vals[2]);
        buf.append("</td>");
        buf.append("<td>");
        if (vals[1] != 0) {
            buf.append(vals[2] / vals[1]);
        } else {
            buf.append("&nbsp;");
        }
        buf.append("</td>");
        buf.append("</tr>");
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
        return new Color(Color.HSBtoRGB(hue, 1.0f, 1.0f));
    }

    /**
     * Sets the label of the given slider.
     *
     * @param slider the slider to label
     * @param divisor the steps are calculated
     *            <code>maxVal - minVal / divisor</code>
     * @param showDigitsAndTicks <code>true</code> if the ticks and their
     * labels should be displayed
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
            labels.put(minimum, new JLabel("Min"));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setEnabled(false);
        } else if (showDigitsAndTicks) {
            // slider.setLabelTable(slider.createStandardLabels(increment));
            final Hashtable<Integer, JLabel> labels =
                new Hashtable<Integer, JLabel>();
            // labels.put(minimum, new JLabel("Min"));
            labels.put(minimum,
                    new JLabel(Integer.toString(minimum)));
            for (int i = 1; i < divisor; i++) {
                final int value = minimum + i * increment;
                labels.put(value,
                        new JLabel(Integer.toString(value)));
            }
            // labels.put(maximum, new JLabel("Max"));
            labels.put(maximum,
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
            labels.put(minimum, new JLabel("Min"));
            labels.put(maximum, new JLabel("Max"));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setEnabled(true);
        }
    }
}
