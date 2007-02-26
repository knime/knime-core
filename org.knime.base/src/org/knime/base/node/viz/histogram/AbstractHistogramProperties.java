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
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.histogram.datamodel.ColorColumn;
import org.knime.base.node.viz.histogram.datamodel.HistogramVizModel;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * Abstract class which handles the default properties like x column selection.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramProperties extends
        AbstractPlotterProperties {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(AbstractHistogramProperties.class);

    private static final String COLUMN_TAB_LABEL = "Column settings";

    private static final String BAR_TAB_LABEL = "Bar settings";

    private static final String AGGREGATION_TAB_LABEL = 
        "Aggregation settings";

    private static final String VIZ_SETTINGS_TAB_LABEL = 
        "Visualization settings";

    private static final String DETAILS_TAB_LABEL = 
        "Details";
    
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

    private static final String SHOW_BAR_OUTLINE_LABEL = "Show bar outline";

//    private static final String APPLY_BUTTON_LABEL = "Apply";

    private static final Dimension HORIZONTAL_SPACER_DIM = new Dimension(10, 1);

    private AggregationMethod m_aggregationMethod;

    private final ColumnSelectionComboxBox m_xCol;

    private final ColumnSelectionComboxBox m_yCol;

    private final JSlider m_barWidth;

    private final JSlider m_noOfBins;

    private final JLabel m_noOfBinsLabel;

    private final ButtonGroup m_aggrMethButtonGrp;

    private final JCheckBox m_showEmptyBins;

    private final JCheckBox m_showMissingValBin;
    
    private final JEditorPane m_detailsHtmlPane;

//    private final JButton m_applyAggrSettingsButton;

//    private final JButton m_applyBarSettingsButton;

    private final JCheckBox m_showGrid;

    private final JCheckBox m_showBarOutline;

    private final ButtonGroup m_labelDisplayPolicy;

    private final ButtonGroup m_labelOrientation;

    private final ButtonGroup m_layoutDisplayPolicy;

    private static final String LABEL_ORIENTATION_LABEL = "Orientation:";

    private static final String LABEL_ORIENTATION_VERTICAL = "Vertical";

    private static final String LABEL_ORIENTATION_HORIZONTAL = "Horizontal";

    /**
     * Constructor for class AbstractHistogramProperties.
     * 
     * @param aggrMethod the aggregation method to set
     */
    @SuppressWarnings("unchecked")
    public AbstractHistogramProperties(final AggregationMethod aggrMethod) {
        // create all swing components first
        m_aggregationMethod = aggrMethod;
        // the column select boxes for the X axis
        m_xCol = new ColumnSelectionComboxBox((Border)null, DataValue.class);
        m_xCol.setBackground(this.getBackground());
        // create the additional settings components which get added to the
        // histogram settings panel
        m_barWidth = new JSlider(0, 20, 10);
        m_barWidth.setEnabled(false);
        m_noOfBins = new JSlider(1, 20, 10);
        m_noOfBinsLabel = new JLabel();
        m_noOfBins.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                updateNoOfBarsText(source.getValue());
            }
        });
        m_noOfBins.setEnabled(false);

        // set the aggregation method radio buttons
        final JRadioButton countMethod = new JRadioButton(
                AggregationMethod.COUNT.name());
        countMethod.setActionCommand(AggregationMethod.COUNT.name());
        countMethod.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSelectAggrMethod(e.getActionCommand());
            }
        });
        final JRadioButton sumMethod = new JRadioButton(AggregationMethod.SUM
                .name());
        sumMethod.setActionCommand(AggregationMethod.SUM.name());
        sumMethod.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSelectAggrMethod(e.getActionCommand());
            }
        });
        final JRadioButton avgMethod = new JRadioButton(
                AggregationMethod.AVERAGE.name());
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

        m_yCol = new ColumnSelectionComboxBox((Border)null, DoubleValue.class);
        m_yCol.setBackground(this.getBackground());
        m_yCol.setToolTipText(AGGREGATION_COLUMN_DISABLED_TOOLTIP);
        // select the right radio button
        for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            // enable the radio buttons only if we have some aggregation
            // columns to choose from
            button.setEnabled(m_yCol.getModel().getSize() > 0);
            if (button.getActionCommand().equals(m_aggregationMethod.name())) {
                button.setSelected(true);
            }
        }
        m_showEmptyBins = new JCheckBox(SHOW_EMPTY_BARS_LABEL);
        m_showMissingValBin = new JCheckBox(SHOW_MISSING_VALUE_BAR_LABEL);
        m_showMissingValBin.setToolTipText(SHOW_MISSING_VAL_BAR_TOOLTIP);
//        m_applyAggrSettingsButton = new JButton(
//                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
//        m_applyAggrSettingsButton.setHorizontalAlignment(
//        SwingConstants.RIGHT);
//
//        m_applyBarSettingsButton = new JButton(
//                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
//        m_applyBarSettingsButton.setHorizontalAlignment(SwingConstants.RIGHT);
        // create the visualization option elements
        m_showGrid = new JCheckBox(SHOW_GRID_LABEL, true);
        m_showBarOutline = new JCheckBox(SHOW_BAR_OUTLINE_LABEL, true);

        m_labelDisplayPolicy = AbstractHistogramProperties
                .createEnumButtonGroup(LabelDisplayPolicy.values());

        m_labelOrientation = new ButtonGroup();
        final JRadioButton labelVertical = new JRadioButton(
                LABEL_ORIENTATION_VERTICAL);
        labelVertical.setActionCommand(LABEL_ORIENTATION_VERTICAL);
        labelVertical.setSelected(true);
        m_labelOrientation.add(labelVertical);
        final JRadioButton labelHorizontal = new JRadioButton(
                LABEL_ORIENTATION_HORIZONTAL);
        labelHorizontal.setActionCommand(LABEL_ORIENTATION_HORIZONTAL);
        m_labelOrientation.add(labelHorizontal);

        m_layoutDisplayPolicy = AbstractHistogramProperties
                .createEnumButtonGroup(HistogramLayout.values());
        // the column select tab
        final JPanel columnPanel = createColumnSettingsPanel();
        addTab(COLUMN_TAB_LABEL, columnPanel);

        // The bar settings tab
        final JPanel barPanel = createBarSettingsPanel();
        addTab(BAR_TAB_LABEL, barPanel);
        // the aggregation settings tab
        final JPanel aggrPanel = createAggregationSettingsPanel();
        addTab(AGGREGATION_TAB_LABEL, aggrPanel);

        final JPanel visOptionPanel = createVizSettingsPanel();
        addTab(VIZ_SETTINGS_TAB_LABEL, visOptionPanel);
        m_detailsHtmlPane = new JEditorPane("text/html", "");
        final JPanel detailsPanel = createHTMLDetailsPanel(m_detailsHtmlPane);
        addTab(DETAILS_TAB_LABEL, detailsPanel);
//        m_detailsHtmlPane.setPreferredSize(detailsPanel.getMaximumSize());
    }

    /**
     * @param props the properties to generate the buttons for
     * @return the {@link ButtonGroup} with all properties as button
     */
    private static ButtonGroup createEnumButtonGroup(
            final HistogramProperty[] props) {
        final ButtonGroup group = new ButtonGroup();
        for (final HistogramProperty property : props) {
            final JRadioButton button = new JRadioButton(property.getLabel());
            button.setActionCommand(property.getID());
            button.setSelected(property.isDefault());
            group.add(button);
        }
        return group;
    }

    /**
     * The details data panel which contains information about the current
     * selected elements.
     * @param htmlPane the panel to write into
     * @return the details date panel
     */
    private JPanel createHTMLDetailsPanel(final JEditorPane htmlPane) {
        final JPanel detailsPanel = new JPanel();
        StringBuilder buf = new StringBuilder();
        buf.append("<h3 align='center'>Details data</h3>");
        buf.append("<br>");
        htmlPane.setText(buf.toString());
        htmlPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(htmlPane);
        detailsPanel.add(scrollPane);
        return detailsPanel;
    }
    
    /**
     * @param html the new details view
     */
    protected void updateHTMLDetailsPanel(final String html) {
        m_detailsHtmlPane.setText(html);
    }

    /**
     * The visualization panel with the following options:
     * <ol>
     * <li>Show grid line</li>
     * <li>Show bar outline</li>
     * </ol>.
     * 
     * @return the visualization settings panel
     */
    private JPanel createVizSettingsPanel() {
        final JPanel vizPanel = new JPanel();
//visualisation box
        final Box vizBox = Box.createVerticalBox();
        vizBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Display option"));
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(m_showGrid);
        vizBox.add(Box.createVerticalGlue());
//label layout box  
        final Box labelBox = Box.createHorizontalBox();
        labelBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Labels"));
        labelBox.add(Box.createHorizontalGlue());
        final Box labelDisplayBox = AbstractHistogramProperties
                .createButtonGroupBox(m_labelDisplayPolicy, true, null);
        labelBox.add(labelDisplayBox);
        labelBox.add(Box.createHorizontalGlue());
        final Box labelOrientationBox = AbstractHistogramProperties
                .createButtonGroupBox(m_labelOrientation, true,
                        LABEL_ORIENTATION_LABEL);
        labelBox.add(labelOrientationBox);
        labelBox.add(Box.createHorizontalGlue());
//bar layout box                
        final Box layoutDisplayBox = AbstractHistogramProperties
        .createButtonGroupBox(m_layoutDisplayPolicy, true, null);
        final Box barWidthBox = Box.createVerticalBox();
        // barWidthBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
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
        barWidthBox.add(Box.createVerticalGlue());
        barWidthBox.add(m_showBarOutline);
        final Box barLayoutBox = Box.createHorizontalBox();
        barLayoutBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Bar Layout"));
        barLayoutBox.add(Box.createHorizontalGlue());
        barLayoutBox.add(layoutDisplayBox);
        barLayoutBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        barLayoutBox.add(barWidthBox);
        barLayoutBox.add(Box.createHorizontalGlue());

        final Box rootBox = Box.createHorizontalBox();
        rootBox.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        rootBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        rootBox.add(vizBox);
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(labelBox);
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(barLayoutBox);
        rootBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        vizPanel.add(rootBox);
        return vizPanel;
    }

    /**
     * The column settings panel which contains only the x column selection box.
     * 
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
     * 
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
        final Box aggrButtonBox = AbstractHistogramProperties
                .createButtonGroupBox(m_aggrMethButtonGrp, false, null);
        aggrLabelButtonBox.add(aggrButtonBox);
        // the apply button box
        // the column selection and button box
        final Box colLabelBox = Box.createVerticalBox();
        final JLabel colLabel = new JLabel(
                AbstractHistogramProperties.AGGREGATION_COLUMN_LABEL);
        colLabel.setVerticalAlignment(SwingConstants.CENTER);
        // colLabelBox.add(Box.createVerticalGlue());
        colLabelBox.add(colLabel);
        colLabelBox.add(Box.createVerticalGlue());
        final Box colSelectLabelBox = Box.createVerticalBox();
        colSelectLabelBox.add(colLabelBox);
        colSelectLabelBox.add(m_yCol);

//        final Box buttonBox = Box.createHorizontalBox();
//        buttonBox.add(Box.createHorizontalGlue());
//        buttonBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
//        buttonBox.add(m_applyAggrSettingsButton);
//        buttonBox.add(Box.createHorizontalGlue());
        // the all surrounding box
        final Box aggrBox = Box.createHorizontalBox();
        aggrBox
                .setBorder(BorderFactory
                        .createEtchedBorder(EtchedBorder.RAISED));
        aggrBox.add(aggrLabelButtonBox);
        aggrBox.add(Box.createHorizontalGlue());
        aggrBox.add(colSelectLabelBox);
        aggrBox.add(Box.createHorizontalGlue());
//        aggrBox.add(buttonBox);
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
     * 
     * @return the panel with all bar related settings
     */
    private JPanel createBarSettingsPanel() {
        final JPanel barPanel = new JPanel();
        final Box barNoBox = Box.createVerticalBox();
        // barNoBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        // the number of bars label box
        final Box noOfBarsLabelBox = Box.createHorizontalBox();
        final JLabel noOfBarsLabel = new JLabel(
                AbstractHistogramProperties.NUMBER_OF_BARS_LABEL);
        noOfBarsLabelBox.add(Box.createHorizontalGlue());
        noOfBarsLabelBox.add(noOfBarsLabel);
        noOfBarsLabelBox.add(Box.createHorizontalStrut(10));
        noOfBarsLabelBox.add(m_noOfBinsLabel);
        noOfBarsLabelBox.add(Box.createHorizontalGlue());
        barNoBox.add(noOfBarsLabelBox);
        // the number of bars slider box
        final Box noOfBarsSliderBox = Box.createHorizontalBox();
        noOfBarsSliderBox.add(Box.createHorizontalGlue());
        noOfBarsSliderBox.add(m_noOfBins);
        noOfBarsSliderBox.add(Box.createHorizontalGlue());
        barNoBox.add(noOfBarsSliderBox);
        // the box with the select boxes and apply button
        final Box barSelButtonBox = Box.createVerticalBox();
        // barSelButtonBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        final Box barSelectBox = Box.createVerticalBox();
        // barSelectBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        barSelectBox.add(m_showEmptyBins);
        barSelectBox.add(Box.createVerticalGlue());
        barSelectBox.add(m_showMissingValBin);
        barSelectBox.add(Box.createVerticalGlue());
        barSelButtonBox.add(barSelectBox);
        barSelButtonBox.add(Box.createVerticalGlue());
//        final Box buttonBox = Box.createHorizontalBox();
//        // buttonBox.setBorder(BorderFactory
//        // .createEtchedBorder(EtchedBorder.RAISED));
//        final Dimension d = new Dimension(75, 1);
//        buttonBox.add(Box.createRigidArea(d));
//        buttonBox.add(m_applyBarSettingsButton);
//        barSelButtonBox.add(buttonBox);
        final Box barBox = Box.createHorizontalBox();
        barBox.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        barBox.add(Box.createHorizontalGlue());
        barBox.add(barNoBox);
        barBox.add(Box.createHorizontalGlue());
        barBox.add(barSelButtonBox);
        barPanel.add(barBox);
        return barPanel;
    }

    /**
     * 
     */
    private static Box createButtonGroupBox(final ButtonGroup group,
            final boolean vertical, final String label) {
        Box buttonBox = null;
        if (vertical) {
            buttonBox = Box.createVerticalBox();
            buttonBox.add(Box.createVerticalGlue());
            if (label != null) {
                buttonBox.add(new JLabel(label));
                buttonBox.add(Box.createVerticalGlue());
            }
        } else {
            buttonBox = Box.createHorizontalBox();
            buttonBox.add(Box.createHorizontalGlue());
            if (label != null) {
                buttonBox.add(new JLabel(label));
                buttonBox.add(Box.createHorizontalGlue());
            }
        }

        for (final Enumeration<AbstractButton> buttons = group.getElements(); 
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
            labels.put(minimum, new JLabel(Integer.toString(minimum)));
            for (int i = 1; i < divisor; i++) {
                final int value = minimum + i * increment;
                labels.put(value, new JLabel(Integer.toString(value)));
            }
            // labels.put(maximum, new JLabel("Max"));
            labels.put(maximum, new JLabel(Integer.toString(maximum)));
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

    /**
     * 
     * @param spec current data table specification
     * @param xColName preselected x column name
     * @param yColumns preselected y column names
     */
    public void updateColumnSelection(final DataTableSpec spec,
            final String xColName, final Collection<ColorColumn> yColumns) {
        try {
            m_xCol.setEnabled(true);
            m_xCol.update(spec, xColName);
        } catch (final NotConfigurableException e) {
            m_xCol.setEnabled(false);
        }
        try {
            if (yColumns == null || yColumns.size() < 1) {
                final String er = "No aggregation column available";
                LOGGER.warn(er);
                throw new IllegalArgumentException(er);
            }
            m_yCol.update(spec, yColumns.iterator().next().getColumnName());
            if (m_yCol.getModel().getSize() > 0) {
                m_yCol.setEnabled(true);
            }
        } catch (final NotConfigurableException e) {
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
        for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            button.setEnabled(m_yCol.getModel().getSize() > 0);
        }
    }

    /**
     * Updates the available slider with the current values of the Histogram
     * plotter.
     * 
     * @param plotter the <code>AbstractHistogramPlotter</code> object which
     *            contains the data
     */
    @SuppressWarnings("unchecked")
    public void updateHistogramSettings(
            final AbstractHistogramPlotter plotter) {
        if (plotter == null) {
            return;
        }
        // set the bar width slider
        final int currentBarWidth = plotter.getBinWidth();
        final int maxBarWidth = plotter.getMaxBinWidth();
        int minBarWidth = AbstractHistogramPlotter.MIN_BIN_WIDTH;
        if (minBarWidth > maxBarWidth) {
            minBarWidth = maxBarWidth;
        }
        // update the bar width values
        m_barWidth.setMaximum(maxBarWidth);
        m_barWidth.setMinimum(minBarWidth);
        m_barWidth.setValue(currentBarWidth);
        m_barWidth.setEnabled(true);
        m_barWidth
                .setToolTipText(AbstractHistogramProperties.BAR_WIDTH_TOOLTIP);
        AbstractHistogramProperties.setSliderLabels(m_barWidth, 2, false);

        // set the number of bars slider
        final HistogramVizModel histoData = plotter.getHistogramVizModel();
        int maxNoOfBins = plotter.getMaxNoOfBins();
        final int currentNoOfBins = histoData.getNoOfBins();
        if (currentNoOfBins > maxNoOfBins) {
            maxNoOfBins = currentNoOfBins;
        }
        // update the number of bar values
        m_noOfBins.setMaximum(maxNoOfBins);
        m_noOfBins.setValue(currentNoOfBins);
        m_noOfBinsLabel.setText(Integer.toString(currentNoOfBins));
        AbstractHistogramProperties.setSliderLabels(m_noOfBins, 2, true);
        // disable this noOfBars slider for nominal values
        if (!histoData.isBinNominal()) {
            m_noOfBins.setEnabled(true);
            m_noOfBins.setToolTipText(
                    AbstractHistogramProperties.NO_OF_BARS_TOOLTIP);
        } else {
            m_noOfBins.setEnabled(false);
            m_noOfBins
                    .setToolTipText("Only available for numerical properties");
        }
        // set the aggregation method if it has changed
        final AggregationMethod aggrMethod = histoData.getAggregationMethod();
        if (!m_aggregationMethod.equals(aggrMethod)) {
            m_aggregationMethod = aggrMethod;
            for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                    .getElements(); buttons.hasMoreElements();) {
                final AbstractButton button = buttons.nextElement();
                // enable the radio buttons only if we have some aggregation
                // columns to choose from
                button.setEnabled(m_yCol.getModel().getSize() > 0);
                if (button.getActionCommand()
                        .equals(m_aggregationMethod.name())) {
                    button.setSelected(true);
                }
            }
        }

        // set the values of the select boxes
        m_showEmptyBins.setSelected(histoData.isShowEmptyBins());
        m_showMissingValBin.setSelected(histoData.isShowMissingValBin());
        m_showMissingValBin.setEnabled(histoData.containsMissingValueBin());
        m_showEmptyBins.setEnabled(histoData.containsEmptyBins());
        m_showGrid.setSelected(plotter.isShowGridLines());
    }

    /**
     * @return the currently set bar width
     */
    public int getBarWidth() {
        return m_barWidth.getValue();
    }

    /**
     * @return the current no of bars
     */
    public int getNoOfBins() {
        return m_noOfBins.getValue();
    }

    /**
     * @return the current selected aggregation method
     */
    public AggregationMethod getSelectedAggrMethod() {
        if (m_aggrMethButtonGrp == null) {
            return AggregationMethod.getDefaultMethod();
        }
        final String methodName = m_aggrMethButtonGrp.getSelection()
                .getActionCommand();
        if (!AggregationMethod.valid(methodName)) {
            throw new IllegalArgumentException("No valid aggregation method");
        }
        return AggregationMethod.getMethod4String(methodName);
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
        final AggregationMethod method = AggregationMethod
                .getMethod4String(actionCommand);
        if (method.equals(AggregationMethod.COUNT)
                || m_yCol.getModel().getSize() < 1) {
            m_yCol.setEnabled(false);
            m_yCol.setToolTipText(
                    AbstractHistogramProperties.
                    AGGREGATION_COLUMN_DISABLED_TOOLTIP);
        } else {
            m_yCol.setEnabled(true);
            m_yCol.setToolTipText(
                    AbstractHistogramProperties.
                    AGGREGATION_COLUMN_ENABLED_TOOLTIP);
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
     * select box is disabled, items cannot be selected and values cannot be
     * typed into its field (if it is editable).
     * 
     * @param b a boolean value, where true enables the component and false
     *            disables it
     */
    protected void setXColEnabled(final boolean b) {
        m_xCol.setEnabled(b);
    }

    /**
     * Enables the x column select box so that items can be selected. When the
     * select box is disabled, items cannot be selected and values cannot be
     * typed into its field (if it is editable).
     * 
     * @param b a boolean value, where true enables the component and false
     *            disables it
     */
    protected void setAggrColEnabled(final boolean b) {
        m_yCol.setEnabled(b);
    }

    /**
     * Helper method to update the number of bars text field.
     * 
     * @param noOfBars the number of bars
     */
    protected void updateNoOfBarsText(final int noOfBars) {
        m_noOfBinsLabel.setText(Integer.toString(noOfBars));
    }

    /**
     * @return the current value of the show grid line select box
     */
    public boolean isShowGrid() {
        return m_showGrid.isSelected();
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
    public boolean isShowEmptyBins() {
        if (m_showEmptyBins == null) {
            return false;
        }
        return m_showEmptyBins.isSelected();
    }

    /**
     * @return if the missing value bar should be shown
     */
    public boolean isShowMissingValBar() {
        if (m_showMissingValBin == null) {
            return false;
        }
        return m_showMissingValBin.isSelected();
    }

    /**
     * @return <code>true</code> if the bar labels should be displayed
     *         vertical or <code>false</code> if the labels should be
     *         displayed horizontal
     */
    public boolean isShowLabelVertical() {
        final String actionCommand = m_labelOrientation.getSelection()
                .getActionCommand();
        return (LABEL_ORIENTATION_VERTICAL.equals(actionCommand));
    }

    /**
     * @return the label display policy
     */
    public LabelDisplayPolicy getLabelDisplayPolicy() {
        final String actionCommand = m_labelDisplayPolicy.getSelection()
                .getActionCommand();
        return LabelDisplayPolicy.getOption4ID(actionCommand);
    }

    /**
     * @return the histogram layout
     */
    public HistogramLayout getHistogramLayout() {
        final String actionCommand = m_layoutDisplayPolicy.getSelection()
                .getActionCommand();
        return HistogramLayout.getLayout4ID(actionCommand);
    }

    /**
     * @param listener the listener to listen if the label orientation has
     *            changed
     */
    protected void addLabelOrientationListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_labelOrientation
                .getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }

    /**
     * @param listener the listener to listen if the label display policy has
     *            changed
     */
    protected void addLabelDisplayListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_labelDisplayPolicy
                .getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }

    /**
     * @param listener the listener to listen if the layout has changed
     */
    protected void addLayoutListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_layoutDisplayPolicy
                .getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }

    /**
     * @param listener adds a listener to the show grid lines check box.
     */
    protected void addShowBarOutlineChangedListener(
            final ItemListener listener) {
        m_showBarOutline.addItemListener(listener);
    }
    
    /**
     * @param listener adds the listener to the bar width slider
     */
    protected void addBarWidthChangeListener(final ChangeListener listener) {
        m_barWidth.addChangeListener(listener);
    }

    /**
     * @param listener adds a listener to the show grid lines check box.
     */
    protected void addShowGridChangedListener(final ItemListener listener) {
        m_showGrid.addItemListener(listener);
    }
    
    /**
     * @param listener adds the listener to the number of bars slider
     */
    protected void addNoOfBarsChangeListener(final ChangeListener listener) {
        m_noOfBins.addChangeListener(listener);
    }

    /**
     * @param listener adds the listener to the aggregation method button
     * group
     */
    protected void addAggrMethodListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
        .getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }
    
    /**
     * @param listener adds the listener to the show empty bins select box
     */
    protected void addShowEmptyBinListener(final ItemListener listener) {
        m_showEmptyBins.addItemListener(listener);
    }
    
    /**
     * @param listener adds the listener to the show missing value bin 
     * select box
     */
    protected void addShowMissingValBinListener(final ItemListener listener) {
        m_showMissingValBin.addItemListener(listener);
    } 
//    /**
//     * @param listener adds a listener to the apply button
//     */
//    protected void addAggregationChangedListener(
//            final ActionListener listener) {
//        m_applyAggrSettingsButton.addActionListener(listener);
//        m_applyBarSettingsButton.addActionListener(listener);
//    }

}
