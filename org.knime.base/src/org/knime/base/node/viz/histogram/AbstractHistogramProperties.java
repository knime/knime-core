/*-------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   18.08.2006 (Tobias Koetter): created
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
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.knime.base.node.viz.plotter2D.AbstractPlotter2D;
import org.knime.base.node.viz.plotter2D.PlotterPropertiesPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramProperties extends PlotterPropertiesPanel {

    protected static final String X_COLUMN_LABEL = "X Column:";
    private static final String AGGREGATION_COLUMN_ENABLED_TOOLTIP = "Select the column used for aggregation an press 'Apply'.";
    private static final String AGGREGATION_COLUMN_DISABLED_TOOLTIP = "Not available for aggregation method count";
    protected static final String AGGREGATION_METHOD_LABEL = "Aggregation method:";
    protected static final String BAR_SIZE_LABEL = "Bar size:";
    private static final String BAR_WIDTH_TOOLTIP = "Width of the bars";
    protected static final String NUMBER_OF_BARS_LABEL = "Number of bars:";
    private static final String NO_OF_BARS_TOOLTIP = "Number of bars";
    private static final String SHOW_MISSING_VALUE_BAR_LABEL = "Show missing value bar";
    private static final String SHOW_MISSING_VAL_BAR_TOOLTIP = "Shows a bar "
            + "with rows which have a missing value for the selected x column.";
    private static final String SHOW_EMPTY_BARS_LABEL = "Show empty bars";
    private static final String APPLY_BUTTON_LABEL = "Apply";
    /** The title of the histogram settings region. */
    private static final String SETTINGS_TITLE = "Histogram settings";
    private AggregationMethod m_aggregationMethod;
    private final ColumnSelectionComboxBox m_xCol;
    private ColumnSelectionComboxBox m_yCol;
    private JSlider m_barWidth;
    private JSlider m_noOfBars = null;
    private ButtonGroup m_aggrMethButtonGrp = null;
    private JCheckBox m_showEmptyBars = null;
    private JCheckBox m_showMissingValBar = null;

    
    public AbstractHistogramProperties(final AggregationMethod aggrMethod) {
        m_aggregationMethod = aggrMethod;
        // the column select boxes for the X axis
        m_xCol = new ColumnSelectionComboxBox(
                InteractiveHistogramProperties.X_COLUMN_LABEL);
        m_xCol.setBackground(this.getBackground());
        //the column select box for the aggregation method which gets added to
        // the button box later
        /*m_yCol = new ColumnSelectionComboxBox(
                AbstractHistogramProperties.AGGREGATION_COLUMN_LABEL,
                DoubleValue.class);*/
        Box columnBox = Box.createVerticalBox();
        /*m_xCol.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                (int)m_xCol.getPreferredSize().getHeight()));*/
        columnBox.add(m_xCol);
        columnBox.add(Box.createVerticalGlue());
        //columnBox.add(m_yCol);

        addPropertiesComponent(columnBox);
        // create the additional settings components which get added to the
        // histogram settings panel
        setUpdateHistogramSettings(getHistogramPlotter());

        // create all dialog component boxes
        // the bar width label box
        final Box barWidthLabelBox = Box.createHorizontalBox();
        final JLabel barWidthLabel = new JLabel(
                AbstractHistogramProperties.BAR_SIZE_LABEL);
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
        final JLabel noOfBarsLabel = new JLabel(
                AbstractHistogramProperties.NUMBER_OF_BARS_LABEL);
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
        final JLabel aggrMethLabel = new JLabel(
                AbstractHistogramProperties.AGGREGATION_METHOD_LABEL);
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
        final JButton applyButton = new JButton(
                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
        applyButton.setHorizontalAlignment(SwingConstants.RIGHT);
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onApply();
            }
        });
        final Box buttonBox = Box.createHorizontalBox();
        //buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(m_yCol);
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(applyButton);

        // the width of the border around some components
        //final int strutWidth = 5;
        // Create the three sections of the histogram settings box
        // the left box
        final Box leftBox = Box.createVerticalBox();
        leftBox.add(barWidthLabelBox);
        leftBox.add(barWidthSliderBox);
        //leftBox.add(Box.createVerticalStrut(strutWidth));
        leftBox.add(Box.createVerticalGlue());
        leftBox.add(showEmptyBox);
        // the middle box
        final Box middleBox = Box.createVerticalBox();
        middleBox.add(noOfBarsLabelBox);
        middleBox.add(noOfBarsSliderBox);
//        middleBox.add(Box.createVerticalStrut(strutWidth));
        middleBox.add(Box.createVerticalGlue());
        middleBox.add(showMissingBox);
        // the right box
        final Box rightBox = Box.createVerticalBox();
        rightBox.add(aggrLabelBox);
        rightBox.add(aggrButtonBox);
//        rightBox.add(Box.createVerticalStrut(strutWidth));
        rightBox.add(Box.createVerticalGlue());
        rightBox.add(buttonBox);

        // Create the histogram setting root box
        final Box histoBox = Box.createHorizontalBox();
        final TitledBorder histoBorder = BorderFactory.createTitledBorder(
                AbstractHistogramProperties.SETTINGS_TITLE);
        histoBox.setBorder(histoBorder);

        // add the sections to the root box
        histoBox.add(leftBox);
        //histoBox.add(Box.createHorizontalStrut(strutWidth));
        histoBox.add(Box.createHorizontalGlue());
        histoBox.add(middleBox);
//        histoBox.add(Box.createHorizontalStrut(strutWidth));
        histoBox.add(Box.createHorizontalGlue());
        histoBox.add(rightBox);

        // add the Histogram settings panel to the parent properties panel
        addPropertiesComponent(histoBox);
    }
    
    /**
     * Sets the label of the given slider.
     * 
     * @param slider the slider to label
     * @param divisor the steps are calculated
     *            <code>maxVal - minVal / divisor</code>
     */
    private static void setSliderLabels(final JSlider slider, 
            final int divisor) {
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
            Hashtable<Integer, JLabel> labels = 
                new Hashtable<Integer, JLabel>();
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
     * 
     * @param spec current data table specification
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
        // enable or disable the aggregation method buttons depending if
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
     * @param plotter the <code>AbstractHistogramPlotter</code> object which 
     * contains the data. Could be <code>null</code>.
     */
    protected void setUpdateHistogramSettings(
            final AbstractHistogramPlotter plotter) {
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
            JRadioButton sumMethod = new JRadioButton(AggregationMethod.SUM
                    .name());
            sumMethod.setActionCommand(AggregationMethod.SUM.name());
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
            
            Border nullBorder = null;
            m_yCol = new ColumnSelectionComboxBox(nullBorder, 
                    DoubleValue.class);
            m_yCol.setBackground(this.getBackground());
            m_yCol.setToolTipText(AGGREGATION_COLUMN_DISABLED_TOOLTIP);
            // select the right radio button
            for (Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                    .getElements(); buttons.hasMoreElements();) {
                AbstractButton button = buttons.nextElement();
                //enable the radio buttons only if we have some aggregation 
                //columns to choose from
                button.setEnabled(m_yCol.getModel().getSize() > 0);
                if (button.getActionCommand()
                        .equals(m_aggregationMethod.name())) {
                    button.setSelected(true);
                }
            }
            m_showEmptyBars = new JCheckBox(SHOW_EMPTY_BARS_LABEL);
            m_showMissingValBar = new JCheckBox(SHOW_MISSING_VALUE_BAR_LABEL);
            m_showMissingValBar.setToolTipText(SHOW_MISSING_VAL_BAR_TOOLTIP);
        } else {
            AbstractHistogramDataModel histoData = 
                plotter.getHistogramDataModel();
            // set the bar width slider
            int currentBarWidth = plotter.getBarWidth();
            int maxBarWidth = plotter.getMaxBarWidth();
            int minBarWidth = AbstractHistogramPlotter.MIN_BAR_WIDTH;
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
            m_barWidth.setToolTipText(
                    AbstractHistogramProperties.BAR_WIDTH_TOOLTIP);
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
                m_noOfBars.setToolTipText(
                        AbstractHistogramProperties.NO_OF_BARS_TOOLTIP);
            } else {
                m_noOfBars.setEnabled(false);
                m_noOfBars.setToolTipText(
                        "Only available for numerical properties");
            }
            // set the values of the select boxes
            if (m_showEmptyBars == null) {
                m_showEmptyBars = new JCheckBox(
                AbstractHistogramProperties.SHOW_EMPTY_BARS_LABEL);
            }
            m_showEmptyBars.setSelected(plotter.isShowEmptyBars());
            if (m_showMissingValBar == null) {
                m_showMissingValBar = new JCheckBox(
                AbstractHistogramProperties.SHOW_MISSING_VALUE_BAR_LABEL);
            }
            m_showMissingValBar.setSelected(plotter.isShowMissingValBar());
            m_showMissingValBar.setEnabled(histoData.containsMissingValueBar());
        } // end of else of if plotter == null
    }

    /**
     * @return the <code>FixedColumnHistogramPlotter</code> object to whom this
     *         properties panel belongs
     */
    protected AbstractHistogramPlotter getHistogramPlotter() {
        AbstractPlotter2D plotter = getPlotter();
        if (plotter instanceof AbstractHistogramPlotter) {
            return (AbstractHistogramPlotter)plotter;
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
    protected AggregationMethod getSelectedAggrMethod() {
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
        final AbstractHistogramPlotter plotter = getHistogramPlotter();
        if (plotter == null) {
            return;
        }
        AbstractHistogramDataModel histoModel = plotter.getHistogramDataModel();
        if (histoModel == null) {
            throw new IllegalStateException("HistogramModel shouldn't be null");
        }
        plotter.setPreferredBarWidth(getBarWidth());
        if (!histoModel.isNominal()) {
            // this is only available for none nominal x axis properties
            plotter.setNumberOfBars(getNoOfBars());
        }
        m_aggregationMethod = getSelectedAggrMethod();
        plotter.setAggregationMethod(m_aggregationMethod);
        plotter.setShowEmptyBars(m_showEmptyBars.isSelected());
        plotter.setShowMissingvalBar(m_showMissingValBar.isSelected());
        // force the repainting of the plotter
        plotter.updatePaintModel();
        // update the labels of the sliders and the select boxes
        setUpdateHistogramSettings(plotter);
        return;
    }

    /**
     * @return the name of the column the user has selected as y coordinate
     */
    protected String getSelectedAggrColumn() {
        return m_yCol.getSelectedColumn();
    }
    
    /**
     * @return the name of the column the user has selected as x coordinate
     */
    protected String getSelectedXColumn() {
        return m_xCol.getSelectedColumn();
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
            m_yCol.setToolTipText(
            AbstractHistogramProperties.AGGREGATION_COLUMN_DISABLED_TOOLTIP);
        } else {
            m_yCol.setEnabled(true);
            m_yCol.setToolTipText(
            AbstractHistogramProperties.AGGREGATION_COLUMN_ENABLED_TOOLTIP);
        }
    }

    /**
     * @return the select box for the x column
     */
    protected ColumnSelectionComboxBox getXColSelectBox() {
        return m_xCol;
    }

    /**
     * Enables the x column select box so that items can be selected. When the
     * select box is disabled, items cannot be selected and values
     * cannot be typed into its field (if it is editable).
     *
     * @param b a boolean value, where true enables the component and
     *          false disables it
     */
    protected void setXColEnabled(final boolean b) {
        m_xCol.setEnabled(b);
    }
    
    /**
     * Enables the x column select box so that items can be selected. When the
     * select box is disabled, items cannot be selected and values
     * cannot be typed into its field (if it is editable).
     *
     * @param b a boolean value, where true enables the component and
     *          false disables it
     */
    protected void setAggrColEnabled(final boolean b) {
        m_yCol.setEnabled(b);
    }
}