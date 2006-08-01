/*
 * ------------------------------------------------------------------
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
 */
package org.knime.base.node.viz.histogram;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.knime.base.node.viz.plotter2D.AbstractPlotter2D;
import org.knime.base.node.viz.plotter2D.PlotterPropertiesPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * The properties panel of the Histogram plotter which allows the user to change
 * the look and behaviour of the histogram plotter. The following options are
 * available:
 * <ol>
 * <li>Bar width</li>
 * <li>Number of bars for a numeric x column</li>
 * <li>different aggregation methods</li>
 * <li>hide empty bars</li>
 * <li>show missing value bar</li>
 * </ol>
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramProperties extends PlotterPropertiesPanel {
    /** The title of the histogram settings region. */
    private static final String SETTINGS_TITLE = "Histogram settings";

    private AggregationMethod m_aggregationMethod;

    private final ColumnSelectionComboxBox m_xCol;

    private final ColumnSelectionComboxBox m_yCol;

    private JSlider m_barWidth;

    private JSlider m_noOfBars = null;

    private ButtonGroup m_aggrMethButtonGrp = null;

    private JCheckBox m_showEmptyBars = null;

    private JCheckBox m_showMissingValBar = null;

    /**
     * Constructor for class HistogramProperties.
     * 
     * @param aggrMethod the aggregation method which should be set
     */
    public HistogramProperties(final AggregationMethod aggrMethod) {
        m_aggregationMethod = aggrMethod;

        // the column select boxes for the X axis
        m_xCol = new ColumnSelectionComboxBox("X Column:");
        m_xCol.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onXColChanged(m_xCol.getSelectedColumn());
            }
        });
        m_xCol.setBackground(this.getBackground());
        m_yCol = new ColumnSelectionComboxBox("Aggregation Column:",
                DoubleValue.class);
        m_yCol.setBackground(this.getBackground());

        Box columnBox = Box.createVerticalBox();
        columnBox.add(m_xCol);
        columnBox.add(Box.createVerticalGlue());
        columnBox.add(m_yCol);

        addPropertiesComponent(columnBox);
        // create the additional settings components which get added to the
        // histogram
        // settings panel
        setUpdateHistogramSettings(getHistogramPlotter());

        // create all dialog component boxes
        // the bar width label box
        final Box barWidthLabelBox = Box.createHorizontalBox();
        final JLabel barWidthLabel = new JLabel("Bar size:");
        barWidthLabelBox.add(Box.createHorizontalGlue());
        barWidthLabelBox.add(barWidthLabel);
        barWidthLabelBox.add(Box.createHorizontalGlue());

        // the bar width slider box
        final Box barWidthSliderBox = Box.createHorizontalBox();
        barWidthSliderBox.add(Box.createHorizontalGlue());
        barWidthSliderBox.add(m_barWidth);
        barWidthSliderBox.add(Box.createHorizontalGlue());

        // the number of bars label box
        final Box noOfBarsLabelBox = Box.createHorizontalBox();
        final JLabel noOfBarsLabel = new JLabel("Number of bars:");
        noOfBarsLabelBox.add(Box.createHorizontalGlue());
        noOfBarsLabelBox.add(noOfBarsLabel);
        noOfBarsLabelBox.add(Box.createHorizontalGlue());

        // the number of bars slider box
        final Box noOfBarsSliderBox = Box.createHorizontalBox();
        noOfBarsSliderBox.add(Box.createHorizontalGlue());
        noOfBarsSliderBox.add(m_noOfBars);
        noOfBarsSliderBox.add(Box.createHorizontalGlue());

        // the aggregation method label box
        final Box aggrLabelBox = Box.createHorizontalBox();
        final JLabel aggrMethLabel = new JLabel("Aggregation method:");
        aggrMethLabel.setVerticalAlignment(SwingConstants.CENTER);
        aggrLabelBox.add(Box.createHorizontalGlue());
        aggrLabelBox.add(aggrMethLabel);
        aggrLabelBox.add(Box.createHorizontalGlue());

        // the aggregation method radio button box
        final Box aggrButtonBox = Box.createHorizontalBox();
        aggrButtonBox.add(Box.createHorizontalGlue());
        for (Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                .getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            aggrButtonBox.add(button);
            aggrButtonBox.add(Box.createHorizontalGlue());
        }

        // the show empty values box
        final Box showEmptyBox = Box.createHorizontalBox();
        // showEmptyBox.add(Box.createGlue());
        showEmptyBox.add(m_showEmptyBars);
        showEmptyBox.add(Box.createHorizontalGlue());

        // the show missing value box
        final Box showMissingBox = Box.createHorizontalBox();
        // showMissingBox.add(Box.createGlue());
        showMissingBox.add(m_showMissingValBar);
        showMissingBox.add(Box.createHorizontalGlue());

        // the apply button box
        final JButton applyButton = new JButton("Apply");
        applyButton.setHorizontalAlignment(SwingConstants.RIGHT);
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onApply();
            }
        });
        final Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(applyButton);

        // the width of the border around some components
        final int strutWidth = 5;
        // Create the three sections of the histogram settings box
        // the left box
        final Box leftBox = Box.createVerticalBox();
        leftBox.add(barWidthLabelBox);
        leftBox.add(barWidthSliderBox);
        leftBox.add(Box.createVerticalStrut(strutWidth));
        leftBox.add(showEmptyBox);
        // the middle box
        final Box middleBox = Box.createVerticalBox();
        middleBox.add(noOfBarsLabelBox);
        middleBox.add(noOfBarsSliderBox);
        middleBox.add(Box.createVerticalStrut(strutWidth));
        middleBox.add(showMissingBox);
        // the right box
        final Box rightBox = Box.createVerticalBox();
        rightBox.add(aggrLabelBox);
        rightBox.add(aggrButtonBox);
        rightBox.add(Box.createVerticalStrut(strutWidth));
        rightBox.add(buttonBox);

        // Create the histogram setting root box
        final Box histoBox = Box.createHorizontalBox();
        final TitledBorder histoBorder = BorderFactory
                .createTitledBorder(HistogramProperties.SETTINGS_TITLE);
        histoBox.setBorder(histoBorder);

        // add the sections to the root box
        histoBox.add(leftBox);
        histoBox.add(Box.createHorizontalStrut(strutWidth));
        histoBox.add(middleBox);
        histoBox.add(Box.createHorizontalStrut(strutWidth));
        histoBox.add(rightBox);

        // add the Histogram settings panel to the parent properties panel
        addPropertiesComponent(histoBox);
    }

    /**
     * 
     * @param spec current data table spec
     * @param xColName preselected x column name
     * @param yColName preselected y column name
     */
    public void updateColumnSelection(final DataTableSpec spec,
            final String xColName, final String yColName) {
        try {
            m_xCol.setEnabled(true);
            m_xCol.update(spec, xColName);
        } catch (NotConfigurableException e) {
            m_xCol.setEnabled(false);
        }
        try {
            m_yCol.update(spec, yColName);
            if (m_yCol.getModel().getSize() > 0) {
                m_yCol.setEnabled(true);
            }
        } catch (NotConfigurableException e) {
            m_yCol.setEnabled(false);
        }
        // set the values for the y axis select box
        if (m_aggregationMethod.equals(AggregationMethod.COUNT)
                || m_yCol.getModel().getSize() < 1) {
            // if the aggregation method is count disable the y axis select box
            m_yCol.setEnabled(false);
        } else {
            // enable the select box only if it contains at least one value
            m_yCol.setEnabled(true);
        }
        // eneable or disable the aggregation method buttons depending if
        // aggregation columns available or not
        for (Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                .getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            button.setEnabled(m_yCol.getModel().getSize() > 0);
        }
    }

    /**
     * Updates the available slider with the current values of the Histogram
     * plotter.
     * 
     * @param plotter the <code>HistogramPlotter</code> object which contains
     *            the data. Could be <code>null</code>.
     */
    protected void setUpdateHistogramSettings(final HistogramPlotter plotter) {
        if (plotter == null) {
            // set all components with dummy values
            m_barWidth = new JSlider(0, 20, 10);
            m_barWidth.setEnabled(false);
            m_noOfBars = new JSlider(1, 20, 10);
            m_noOfBars.setEnabled(false);

            // set the aggregation method radio buttons
            JRadioButton countMethod = new JRadioButton(AggregationMethod.COUNT
                    .name());
            countMethod.setActionCommand(AggregationMethod.COUNT.name());
            countMethod.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    onSelectAggrMethod(e.getActionCommand());
                }
            });
            JRadioButton sumMethod = new JRadioButton(AggregationMethod.SUMMARY
                    .name());
            sumMethod.setActionCommand(AggregationMethod.SUMMARY.name());
            sumMethod.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    onSelectAggrMethod(e.getActionCommand());
                }
            });
            JRadioButton avgMethod = new JRadioButton(AggregationMethod.AVERAGE
                    .name());
            avgMethod.setActionCommand(AggregationMethod.AVERAGE.name());
            avgMethod.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    onSelectAggrMethod(e.getActionCommand());
                }
            });
            // Group the radio buttons.
            m_aggrMethButtonGrp = new ButtonGroup();
            m_aggrMethButtonGrp.add(countMethod);
            m_aggrMethButtonGrp.add(sumMethod);
            m_aggrMethButtonGrp.add(avgMethod);

            // select the right radio button
            for (Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                    .getElements(); buttons.hasMoreElements();) {
                AbstractButton button = buttons.nextElement();
                button.setEnabled(m_yCol.getModel().getSize() > 0);
                if (button.getActionCommand()
                        .equals(m_aggregationMethod.name())) {
                    button.setSelected(true);
                }
            }
            m_showEmptyBars = new JCheckBox("Show empty bars:");
            m_showMissingValBar = new JCheckBox("Show missing value bar:");
        } else {
            HistogramDataModel histoData = plotter.getHistogramDataModel();
            // set the bar width slider
            int currentBarWidth = plotter.getBarWidth();
            int maxBarWidth = plotter.getMaxBarWidth();
            int minBarWidth = HistogramPlotter.MIN_BAR_WIDTH;
            if (minBarWidth > maxBarWidth) {
                minBarWidth = maxBarWidth;
            }
            if (m_barWidth == null) {
                m_barWidth = new JSlider(minBarWidth, maxBarWidth,
                        currentBarWidth);
            } else {
                m_barWidth.setMaximum(maxBarWidth);
                m_barWidth.setMinimum(minBarWidth);
                m_barWidth.setValue(currentBarWidth);
            }
            m_barWidth.setEnabled(true);
            m_barWidth.setToolTipText("Number of bars in total");
            setSliderLabels(m_barWidth, 2);

            // set the number of bars slider
            // int currentNoOfBars = plotter.getNoOfDisplayedBars();
            int currentNoOfBars = histoData.getNumberOfBars();
            int maxNoOfBars = plotter.getMaxNoOfBars();
            if (currentNoOfBars > maxNoOfBars) {
                maxNoOfBars = currentNoOfBars;
            }
            if (m_noOfBars == null) {
                m_noOfBars = new JSlider(1, maxNoOfBars, currentNoOfBars);
            } else {
                m_noOfBars.setMaximum(maxNoOfBars);
                m_noOfBars.setValue(currentNoOfBars);
            }
            setSliderLabels(m_noOfBars, 2);
            // disable this noOfBars slider for nominal values
            if (!plotter.getHistogramDataModel().isNominal()) {
                m_noOfBars.setEnabled(true);
                m_noOfBars.setToolTipText("Number of binned bars");
            } else {
                m_noOfBars.setEnabled(false);
                m_noOfBars
                        .setToolTipText("Only available for numerical properties");
            }
            // set the values of the select boxes
            if (m_showEmptyBars == null) {
                m_showEmptyBars = new JCheckBox("Show empty bars");
            }
            m_showEmptyBars.setSelected(plotter.isShowEmptyBars());
            if (m_showMissingValBar == null) {
                m_showMissingValBar = new JCheckBox("Show missing value bar");
            }
            m_showMissingValBar.setSelected(plotter.isShowMissingvalBar());
            m_showMissingValBar.setEnabled(histoData.containsMissingValueBar());
        } // end of else of if plotter == null
    }

    /**
     * Sets the label of the given slider.
     * 
     * @param slider the slider to label
     * @param divisor the steps are calculated
     *            <code>maxVal - minVal / divisor</code>
     */
    private void setSliderLabels(final JSlider slider, final int divisor) {
        // show at least the min, middle and max value on the slider.
        final int minimum = slider.getMinimum();
        final int maximum = slider.getMaximum();
        final int increment = (maximum - minimum) / divisor;
        if (increment < 1) {
            // if their is no increment we don't need to enable this slider
            // Hashtable labels = m_barWidth.createStandardLabels(1);
            Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>(
                    1);
            labels.put(minimum, new JLabel("Min"));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setEnabled(false);
        } else {
            // slider.setLabelTable(slider.createStandardLabels(increment));
            Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
            labels.put(minimum, new JLabel("Min"));
            for (int i = 1; i < divisor; i++) {
                int value = minimum + i * increment;
                labels.put(value, new JLabel(Integer.toString(value)));
            }
            // labels.put(maximum, new JLabel("Max"));
            labels.put(maximum, new JLabel(Integer.toString(maximum)));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setEnabled(true);
        }
    }

    /**
     * Called whenever user changes the x column selection.
     * 
     * @param xColName the new selected x column
     */
    private void onXColChanged(final String xColName) {
        final HistogramPlotter plotter = getHistogramPlotter();
        if (plotter == null || xColName == null) {
            return;
        }
        plotter.setXColumn(xColName);
        m_xCol.setToolTipText(xColName);
        // repaint the plotter
        plotter.updatePaintModel();
        // update the slider values and the select boxes
        setUpdateHistogramSettings(plotter);
    }

    /**
     * Called whenever user changes the y column selection.
     * 
     * @param yColName the new selected y column
     * @param aggrMethod the aggregation method like count, average, ...
     */
    private void selectedYColChanged(final String yColName,
            final AggregationMethod aggrMethod) {
        final HistogramPlotter plotter = getHistogramPlotter();
        if (plotter == null) {
            return;
        }
        plotter.setAggregationColumn(yColName, aggrMethod);
        m_yCol.setToolTipText(yColName);
    }

    /**
     * Sets the aggregation method. If it's count the aggregation column select
     * box gets disabled otherwise it gets enabled.
     * 
     * @param aggrMethod to set
     */
    protected void setAggregationMethod(final AggregationMethod aggrMethod) {
        m_aggregationMethod = aggrMethod;
        if (m_aggregationMethod.equals(AggregationMethod.COUNT)
                || m_yCol.getModel().getSize() < 1) {
            m_yCol.setEnabled(false);
        } else {
            m_yCol.setEnabled(true);
        }
    }

    /**
     * @return the <code>HistogramPlotter</code> object to whom this
     *         properties panel belongs
     */
    private HistogramPlotter getHistogramPlotter() {
        AbstractPlotter2D plotter = getPlotter();
        if (plotter instanceof HistogramPlotter) {
            return (HistogramPlotter)plotter;
        }
        return null;
    }

    /**
     * @return the currently set bar width
     */
    protected int getBarWidth() {
        return m_barWidth.getValue();
    }

    /**
     * @return the current no of bars
     */
    protected int getNoOfBars() {
        return m_noOfBars.getValue();
    }

    /**
     * @return the current selected aggregation method
     */
    private AggregationMethod getSelectedAggrMethod() {
        if (m_aggrMethButtonGrp == null) {
            return AggregationMethod.getDefaultMethod();
        } else {
            String methodName = m_aggrMethButtonGrp.getSelection()
                    .getActionCommand();
            if (!AggregationMethod.valid(methodName)) {
                throw new IllegalArgumentException(
                        "No valid aggregation method");
            }
            return AggregationMethod.getMethod4String(methodName);
        }
    }

    /**
     * Applies the settings to the plotter model.
     */
    protected void onApply() {
        final HistogramPlotter plotter = getHistogramPlotter();
        if (plotter == null) {
            return;
        }
        HistogramDataModel histoModel = plotter.getHistogramDataModel();
        if (histoModel == null) {
            throw new IllegalStateException("HisotgramModel shouldn't be null");
        }
        plotter.setBarWidth(getBarWidth());
        if (!histoModel.isNominal()) {
            // this is only available for none nominal x axis properties
            plotter.setNumberOfBars(getNoOfBars());
        }
        m_aggregationMethod = getSelectedAggrMethod();
        selectedYColChanged(m_yCol.getSelectedColumn(), m_aggregationMethod);
        plotter.setShowEmptyBars(m_showEmptyBars.isSelected());
        plotter.setShowMissingvalBar(m_showMissingValBar.isSelected());
        // force the repainting of the plotter
        plotter.updatePaintModel();
        // update the labels of the sliders and the select boxes
        setUpdateHistogramSettings(plotter);
        return;
    }

    /**
     * @param actionCommand the action command of the radio button which should
     *            be the name of the <code>AggregationMethod</code>
     */
    protected void onSelectAggrMethod(final String actionCommand) {
        AggregationMethod method = AggregationMethod
                .getMethod4String(actionCommand);
        if (method.equals(AggregationMethod.COUNT)
                || m_yCol.getModel().getSize() < 1) {
            m_yCol.setEnabled(false);
        } else {
            m_yCol.setEnabled(true);
        }
    }
}
