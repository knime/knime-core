/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   18.08.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * Abstract class which handles the default properties like x column selection.
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramProperties extends 
    AbstractPlotterProperties {

    private static final String COLUMN_TAB_LABEL = "Column settings";
    
    private static final String BAR_TAB_LABEL = "Bar settings";
    
    private static final String AGGREGATION_TAB_LABEL = "Aggregation settings";
    
    private static final String VIZ_SETTINGS_TAB_LABEL = 
        "Visualization settings";

    
    private static final String X_COLUMN_LABEL = "X Column:";
    private static final String AGGREGATION_COLUMN_ENABLED_TOOLTIP = 
        "Select the column used for aggregation an press 'Apply'.";
    private static final String AGGREGATION_COLUMN_DISABLED_TOOLTIP = 
        "Not available for aggregation method count";
    private static final String AGGREGATION_METHOD_LABEL = 
        "Aggregation method:";
    private static final String AGGREGATION_COLUMN_LABEL = 
        "Aggregation column:";
    private static final String BAR_SIZE_LABEL = "Bar size:";
    private static final String BAR_WIDTH_TOOLTIP = "Width of the bars";
    private static final String NUMBER_OF_BARS_LABEL = "Number of bars:";
    private static final String NO_OF_BARS_TOOLTIP = 
        "Number of bars (incl. empty bars, excl. missing value bar)";
    private static final String SHOW_MISSING_VALUE_BAR_LABEL = 
        "Show missing value bar";
    private static final String SHOW_MISSING_VAL_BAR_TOOLTIP = "Shows a bar "
            + "with rows which have a missing value for the selected x column.";
    private static final String SHOW_EMPTY_BARS_LABEL = "Show empty bars";
    private static final String SHOW_GRID_LABEL = "Y grid lines";
    private static final String SHOW_BAR_OUTLINE_LABEL = 
        "Show outline on bars";
    private static final String APPLY_BUTTON_LABEL = "Apply";
    
    private static final Dimension HORIZONTAL_SPACER_DIM = new Dimension(10, 1);

    private AggregationMethod m_aggregationMethod;
    private final ColumnSelectionComboxBox m_xCol;
    private final ColumnSelectionComboxBox m_yCol;
    private final JSlider m_barWidth;
    private final JSlider m_noOfBars;
    private final JLabel m_noOfBarsLabel;
    private final ButtonGroup m_aggrMethButtonGrp;
    private final JCheckBox m_showEmptyBars;
    private final JCheckBox m_showMissingValBar;
    private final JButton m_applyAggrSettingsButton;
    private final JButton m_applyBarSettingsButton;
    private final JCheckBox m_showGrid;
    private final JCheckBox m_showBarOutline;

    
    /**Constructor for class AbstractHistogramProperties.
     * @param aggrMethod the aggregation method to set
     */
    @SuppressWarnings("unchecked")
    public AbstractHistogramProperties(final AggregationMethod aggrMethod) {
        //create all swing components first
        m_aggregationMethod = aggrMethod;
        // the column select boxes for the X axis
        m_xCol = new ColumnSelectionComboxBox((Border) null, DataValue.class);
        m_xCol.setBackground(this.getBackground());
        // create the additional settings components which get added to the
        // histogram settings panel
        m_barWidth = new JSlider(0, 20, 10);
        m_barWidth.setEnabled(false);
        m_noOfBars = new JSlider(1, 20, 10);
        m_noOfBarsLabel = new JLabel();
        m_noOfBars.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                updateNoOfBarsText(source.getValue());
            } 
        });
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
        
        m_yCol = new ColumnSelectionComboxBox((Border) null, 
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
        m_applyAggrSettingsButton = new JButton(
                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
        m_applyAggrSettingsButton.setHorizontalAlignment(SwingConstants.RIGHT);
        
        m_applyBarSettingsButton = new JButton(
                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
        m_applyBarSettingsButton.setHorizontalAlignment(SwingConstants.RIGHT);
        //create the visualization option elements
        m_showGrid = new JCheckBox(SHOW_GRID_LABEL, true);
        m_showBarOutline = new JCheckBox(SHOW_BAR_OUTLINE_LABEL, true);
        
//the column select tab
        final JPanel columnPanel = createColumnSettingsPanel();
        addTab(COLUMN_TAB_LABEL, columnPanel);
        
//The bar settings tab
        final JPanel barPanel = createBarSettingsPanel();
        addTab(BAR_TAB_LABEL, barPanel);
//the aggregation settings tab
        final JPanel aggrPanel = createAggregationSettingsPanel();
        addTab(AGGREGATION_TAB_LABEL, aggrPanel);
        
        final JPanel visOptionPanel = createVizSettingsPanel();
        addTab(VIZ_SETTINGS_TAB_LABEL, visOptionPanel);
    }

    /**
     * The visualization panel with the following options:
     * <ol><li>Show grid line</li><li>Show bar outline</li></ol>.
     * @return the visualization settings panel
     */
    private JPanel createVizSettingsPanel() {
        final JPanel vizPanel = new JPanel();
        final Box vizBox = Box.createHorizontalBox();
//        vizBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
//                .createEtchedBorder(), "Layout"));
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(m_showGrid);
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(m_showBarOutline);
        vizBox.add(Box.createVerticalGlue());
//        final Box labelBox = Box.createHorizontalBox();
//        labelBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
//                .createEtchedBorder(), "Labels"));
//        labelBox.add(Box.createHorizontalGlue());
//        final Box labelShowBox = Box.createVerticalBox();
//        //labelShowBox.add();
//        labelBox.add(Box.createHorizontalGlue());
        final Box rootBox = Box.createHorizontalBox();
        rootBox.setBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.RAISED));
        rootBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        rootBox.add(vizBox);
//        rootBox.add(Box.createHorizontalGlue());
//        rootBox.add(labelBox);
        rootBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        vizPanel.add(rootBox);
        return vizPanel;
    }

    /**
     * The column settings panel which contains only the x column 
     * selection box.
     * @return the column selection panel
     */
    private JPanel createColumnSettingsPanel() {
        final JPanel columnPanel = new JPanel();
        final Box columnBox = Box.createHorizontalBox();
        columnBox.setBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.RAISED));
        final JLabel xColLabelLabel = new JLabel(
                AbstractHistogramProperties.X_COLUMN_LABEL);
        columnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        columnBox.add(xColLabelLabel);
        columnBox.add(Box.createHorizontalGlue());
        columnBox.add(m_xCol);
        columnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        columnPanel.add(columnBox);
        return columnPanel;
    }

    /**
     * The bar aggregation settings:
     * <ol>
     * <li>aggregation method</li>
     * <li>aggregation column</li>
     * </ol>.
     * @return the aggregation settings panel
     */
    private JPanel createAggregationSettingsPanel() {
        final JPanel aggrPanel = new JPanel();
        // the aggregation method label box
        final Box aggrLabelButtonBox = Box.createVerticalBox();
        final Box aggrLabelBox = Box.createHorizontalBox();
        final JLabel aggrMethLabel = new JLabel(
                AbstractHistogramProperties.AGGREGATION_METHOD_LABEL);
        aggrMethLabel.setVerticalAlignment(SwingConstants.CENTER);
        aggrLabelBox.add(Box.createHorizontalGlue());
        aggrLabelBox.add(aggrMethLabel);
        aggrLabelBox.add(Box.createHorizontalGlue());
        aggrLabelButtonBox.add(aggrLabelBox);
        // the aggregation method radio button box
        final Box aggrButtonBox = Box.createHorizontalBox();
        aggrButtonBox.add(Box.createHorizontalGlue());
        for (Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                .getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            aggrButtonBox.add(button);
            aggrButtonBox.add(Box.createHorizontalGlue());
        }
        aggrLabelButtonBox.add(aggrButtonBox);
        // the apply button box
        //the column selection and button box
        final Box colLabelBox = Box.createVerticalBox();
        final JLabel colLabel = new JLabel(
                AbstractHistogramProperties.AGGREGATION_COLUMN_LABEL);
        colLabel.setVerticalAlignment(SwingConstants.CENTER);
        //colLabelBox.add(Box.createVerticalGlue());
        colLabelBox.add(colLabel);
        colLabelBox.add(Box.createVerticalGlue());
        final Box colSelectLabelBox = Box.createVerticalBox();
        colSelectLabelBox.add(colLabelBox);
        colSelectLabelBox.add(m_yCol);
        
        final Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        buttonBox.add(m_applyAggrSettingsButton);
        buttonBox.add(Box.createHorizontalGlue());
//the all surrounding box
        final Box aggrBox = Box.createHorizontalBox();
        aggrBox.setBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.RAISED));
        aggrBox.add(aggrLabelButtonBox);
        aggrBox.add(Box.createHorizontalGlue());
        aggrBox.add(colSelectLabelBox);
        aggrBox.add(Box.createHorizontalGlue());
        aggrBox.add(buttonBox);
        aggrPanel.add(aggrBox);
        return aggrPanel;
    }

    /**
     * The bar related settings:
     * <ol>
     * <li>size</li>
     * <li>number of bars</li>
     * <li>show empty bars</li>
     * <li>show missing value bar</li>
     * </ol>.
     * @return the panel with all bar related settings
     */
    private JPanel createBarSettingsPanel() {
        final JPanel barPanel = new JPanel();
        final Box barWidthBox = Box.createVerticalBox();
//        barWidthBox.setBorder(BorderFactory
//                .createEtchedBorder(EtchedBorder.RAISED));
        final Box barWidthLabelBox = Box.createHorizontalBox();
        final JLabel barWidthLabel = new JLabel(
                AbstractHistogramProperties.BAR_SIZE_LABEL);
        barWidthLabelBox.add(Box.createHorizontalGlue());
        barWidthLabelBox.add(barWidthLabel);
        barWidthLabelBox.add(Box.createHorizontalGlue());
        barWidthBox.add(barWidthLabelBox);
        // the bar width slider box
        final Box barWidthSliderBox = Box.createHorizontalBox();
        barWidthSliderBox.add(Box.createHorizontalGlue());
        barWidthSliderBox.add(m_barWidth);
        barWidthSliderBox.add(Box.createHorizontalGlue());
        barWidthBox.add(barWidthSliderBox);
        final Box barNoBox = Box.createVerticalBox();
//        barNoBox.setBorder(BorderFactory
//                .createEtchedBorder(EtchedBorder.RAISED));
//      the number of bars label box
        final Box noOfBarsLabelBox = Box.createHorizontalBox();
        final JLabel noOfBarsLabel = new JLabel(
                AbstractHistogramProperties.NUMBER_OF_BARS_LABEL);
        noOfBarsLabelBox.add(Box.createHorizontalGlue());
        noOfBarsLabelBox.add(noOfBarsLabel);
        noOfBarsLabelBox.add(Box.createHorizontalStrut(10));
        noOfBarsLabelBox.add(m_noOfBarsLabel);
        noOfBarsLabelBox.add(Box.createHorizontalGlue());
        barNoBox.add(noOfBarsLabelBox);
//      the number of bars slider box
        final Box noOfBarsSliderBox = Box.createHorizontalBox();
        noOfBarsSliderBox.add(Box.createHorizontalGlue());
        noOfBarsSliderBox.add(m_noOfBars);
        noOfBarsSliderBox.add(Box.createHorizontalGlue());
        barNoBox.add(noOfBarsSliderBox);
        //the box with the select boxes and apply button
        final Box barSelButtonBox = Box.createVerticalBox();
//        barSelButtonBox.setBorder(BorderFactory
//                .createEtchedBorder(EtchedBorder.RAISED));
        final Box barSelectBox = Box.createVerticalBox();
//        barSelectBox.setBorder(BorderFactory
//                .createEtchedBorder(EtchedBorder.RAISED));
        barSelectBox.add(m_showEmptyBars);
        barSelectBox.add(Box.createVerticalGlue());
        barSelectBox.add(m_showMissingValBar);
        barSelectBox.add(Box.createVerticalGlue());
        barSelButtonBox.add(barSelectBox);
        barSelButtonBox.add(Box.createVerticalGlue());
        final Box buttonBox = Box.createHorizontalBox();
//        buttonBox.setBorder(BorderFactory
//                .createEtchedBorder(EtchedBorder.RAISED));
        final Dimension d = new Dimension(75, 1);
        buttonBox.add(Box.createRigidArea(d));
        buttonBox.add(m_applyBarSettingsButton);
        barSelButtonBox.add(buttonBox);
        final Box barBox = Box.createHorizontalBox();
        barBox.setBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.RAISED));
        barBox.add(barWidthBox);
        barBox.add(Box.createHorizontalGlue());
        barBox.add(barNoBox);
        barBox.add(Box.createHorizontalGlue());
        barBox.add(barSelButtonBox);
        barPanel.add(barBox);
        return barPanel;
    }
    
    /**
     * Sets the label of the given slider.
     * 
     * @param slider the slider to label
     * @param divisor the steps are calculated
     *            <code>maxVal - minVal / divisor</code>
     */
    private static void setSliderLabels(final JSlider slider, 
            final int divisor, final boolean showDigitsAndTicks) {
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
        } else if (showDigitsAndTicks) {
            // slider.setLabelTable(slider.createStandardLabels(increment));
            Hashtable<Integer, JLabel> labels = 
                new Hashtable<Integer, JLabel>();
//            labels.put(minimum, new JLabel("Min"));
            labels.put(minimum, new JLabel(Integer.toString(minimum)));
            for (int i = 1; i < divisor; i++) {
                int value = minimum + i * increment;
                labels.put(value, new JLabel(Integer.toString(value)));
            }
            // labels.put(maximum, new JLabel("Max"));
            labels.put(maximum, new JLabel(Integer.toString(maximum)));
            slider.setLabelTable(labels);
            slider.setPaintLabels(true);
            slider.setMajorTickSpacing(divisor);
            slider.setPaintTicks(true);
            //slider.setSnapToTicks(true);
            slider.setEnabled(true);
        }  else {
            final Hashtable<Integer, JLabel> labels = 
                new Hashtable<Integer, JLabel>();
            labels.put(minimum, new JLabel("Min"));
            labels.put(maximum, new JLabel("Max"));
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
     * contains the data
     */
    @SuppressWarnings("unchecked")
    public void updateHistogramSettings(
            final AbstractHistogramPlotter plotter) {
        if (plotter == null) {
            return;
        } else {
            // set the bar width slider
            int currentBarWidth = plotter.getBarWidth();
            int maxBarWidth = plotter.getMaxBarWidth();
            int minBarWidth = AbstractHistogramPlotter.MIN_BAR_WIDTH;
            if (minBarWidth > maxBarWidth) {
                minBarWidth = maxBarWidth;
            }
            //update the bar width values
            m_barWidth.setMaximum(maxBarWidth);
            m_barWidth.setMinimum(minBarWidth);
            m_barWidth.setValue(currentBarWidth);
            m_barWidth.setEnabled(true);
            m_barWidth.setToolTipText(
                    AbstractHistogramProperties.BAR_WIDTH_TOOLTIP);
            setSliderLabels(m_barWidth, 2, false);
    
            // set the number of bars slider
            final AbstractHistogramDataModel histoData = 
                plotter.getHistogramDataModel();
            int currentNoOfBars = histoData.getNumberOfBars();
            int maxNoOfBars = plotter.getMaxNoOfBars();
            if (currentNoOfBars > maxNoOfBars) {
                maxNoOfBars = currentNoOfBars;
            }
            //update the number of bar values
            m_noOfBars.setMaximum(maxNoOfBars);
            m_noOfBars.setValue(currentNoOfBars);
            m_noOfBarsLabel.setText(Integer.toString(currentNoOfBars));
            setSliderLabels(m_noOfBars, 2, true);
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
            //set the aggregation method if it has changed
            final AggregationMethod aggrMethod = 
                plotter.getAggregationMethod();
            if (!m_aggregationMethod.equals(aggrMethod)) {
                m_aggregationMethod = aggrMethod;
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
            }
            
            // set the values of the select boxes
            m_showEmptyBars.setSelected(plotter.isShowEmptyBars());
            m_showMissingValBar.setSelected(plotter.isShowMissingValBar());
            m_showMissingValBar.setEnabled(histoData.containsMissingValueBar());
            m_showEmptyBars.setEnabled(histoData.containsEmptyValueBars());
            m_showGrid.setSelected(plotter.isShowGridLines());
        } // end of else of if plotter == null
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
    public AggregationMethod getSelectedAggrMethod() {
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
     * @return the name of the column the user has selected as y coordinate
     */
    public String getSelectedAggrColumn() {
        return m_yCol.getSelectedColumn();
    }
    
    /**
     * @return the name of the column the user has selected as x coordinate
     */
    public String getSelectedXColumn() {
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
    public ColumnSelectionComboxBox getXColSelectBox() {
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
    
    /**
     * Helper method to update the number of bars text field.
     * @param noOfBars the number of bars 
     */
    protected void updateNoOfBarsText(final int noOfBars) {
        m_noOfBarsLabel.setText(Integer.toString(noOfBars));
    }

    /**
     * @param listener adds a listener to the apply button
     */
    public void addAggregationChangedListener(final ActionListener listener) {
        m_applyAggrSettingsButton.addActionListener(listener);
        m_applyBarSettingsButton.addActionListener(listener);
    }
    
    /**
     * @param listener adds a listener to the show grid lines check box.
     */
    public void addShowGridChangedListener(final ItemListener listener) {
        m_showGrid.addItemListener(listener);
    }

    /**
     * @return the current value of the show grid line select box
     */
    public boolean isShowGrid() {
        return m_showGrid.isSelected();
    }
    /**
     * @param listener adds a listener to the show grid lines check box.
     */
    public void addShowBarOutlineChangedListener(final ItemListener listener) {
        m_showBarOutline.addItemListener(listener);
    }
    /**
     * @return the current value of the show bar outline select box
     */
    public boolean isShowBarOutline() {
        return m_showBarOutline.isSelected();
    }
    /**
     * @return if the empty bars should be shown
     */
    protected boolean getShowEmptyBars() {
        if (m_showEmptyBars == null) {
            return false;
        }
        return m_showEmptyBars.isSelected();
    }

    /**
     * @return if the missing value bar should be shown
     */
    public boolean getShowMissingvalBar() {
        if (m_showMissingValBar == null) {
            return false;
        }
        return m_showMissingValBar.isSelected();
    }

}
