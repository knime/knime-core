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
 *   18.08.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.impl;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Enumeration;

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
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.util.GUIUtils;
import org.knime.base.node.viz.aggregation.util.LabelDisplayPolicy;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;

/**
 * Abstract class which handles the default properties like layouting.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramProperties extends
        AbstractPlotterProperties {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AbstractHistogramProperties.class);

    private static final String COLUMN_TAB_LABEL =
        "Column/Aggregation settings";

    private static final String BIN_AGGR_TAB_LABEL =
        "Bin settings";

//    private static final String AGGREGATION_TAB_LABEL =
//        "Aggregation settings";

    private static final String VIZ_SETTINGS_TAB_LABEL =
        "Visualization settings";

    private static final String DETAILS_TAB_LABEL =
        "Details";

    private static final String AGGREGATION_METHOD_LABEL =
        "Aggregation method:";

    private static final String BIN_SIZE_LABEL = "Bin size:";

    private static final String BIN_WIDTH_TOOLTIP = "Width of the bins";

    private static final String NUMBER_OF_BINS_LABEL = "Number of bins:";

    private static final String NO_OF_BINS_TOOLTIP =
        "Number of bins (incl. empty bins, excl. missing value bin)";

    private static final String SHOW_MISSING_VALUE_BIN_LABEL =
        "Show missing value bin";

    private static final String SHOW_MISSING_VAL_BIN_TOOLTIP = "Shows a bin "
    + "with rows which have a missing value for the selected binning column.";

    private static final String SHOW_EMPTY_BINS_LABEL = "Show empty bins";

    private static final String SHOW_GRID_LABEL = "Show grid lines";

    private static final String SHOW_BIN_OUTLINE_LABEL = "Show bin outline";

    private static final String SHOW_BAR_OUTLINE_LABEL = "Show bar outline";

    private static final String SHOW_ELEMENT_OUTLINE_LABEL =
        "Show element outline";

    private static final String AGGR_METHOD_DISABLED_TOOLTIP =
        "Only available with aggregation column";

//    private static final String APPLY_BUTTON_LABEL = "Apply";

    private static final Dimension HORIZONTAL_SPACER_DIM = new Dimension(10, 1);

    private final JSlider m_binWidth;

    private final JSlider m_noOfBins;

    private final JLabel m_noOfBinsLabel;

    private final ButtonGroup m_aggrMethButtonGrp;

    private final JCheckBox m_showEmptyBins;

    private final JCheckBox m_showMissingValBin;

    private final JPanel m_detailsPane;

    private final JScrollPane m_detailsScrollPane;

    private final JEditorPane m_detailsHtmlPane;

//    private final JButton m_applyAggrSettingsButton;

//    private final JButton m_applyBarSettingsButton;

    private final JCheckBox m_showGrid;

    private final JCheckBox m_showBinOutline;

    private final JCheckBox m_showBarOutline;

    private final JCheckBox m_showElementOutline;

    private final ButtonGroup m_labelDisplayPolicy;

    private final ButtonGroup m_labelOrientation;

    private final ButtonGroup m_layoutDisplayPolicy;

    private static final String LABEL_ORIENTATION_LABEL = "Orientation:";

    private static final String LABEL_ORIENTATION_VERTICAL = "Vertical";

    private static final String LABEL_ORIENTATION_HORIZONTAL = "Horizontal";

    /**
     * Constructor for class AbstractHistogramProperties.
     *
     * @param tableSpec the {@link DataTableSpec} to initialize the column
     * @param vizModel the aggregation method to set
     */
    public AbstractHistogramProperties(final DataTableSpec tableSpec,
            final AbstractHistogramVizModel vizModel) {
        if (vizModel == null) {
            throw new IllegalArgumentException("VizModel must not be null");
        }
        if (tableSpec == null) {
            throw new IllegalArgumentException("TableSpec must not be null");
        }

        // create the additional settings components which get added to the
        // histogram settings panel
        m_binWidth = new JSlider(0, vizModel.getBinWidth(),
                vizModel.getBinWidth());
        m_binWidth.setEnabled(false);
        m_noOfBins = new JSlider(1, 1, 1);
        m_noOfBinsLabel = new JLabel();
        m_noOfBins.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final JSlider source = (JSlider)e.getSource();
                updateNoOfBinsText(source.getValue());
            }
        });
        m_noOfBins.setEnabled(false);

        m_aggrMethButtonGrp =
            GUIUtils.createButtonGroup(AggregationMethod.values(),
                new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                onSelectAggrMethod(e.getActionCommand());
            }
        });

        // select the right radio button
        for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand().equals(
                    vizModel.getAggregationMethod().name())) {
                button.setSelected(true);
            }
        }
        m_showEmptyBins = new JCheckBox(SHOW_EMPTY_BINS_LABEL,
                vizModel.isShowEmptyBins());
        m_showMissingValBin = new JCheckBox(SHOW_MISSING_VALUE_BIN_LABEL,
                vizModel.isShowEmptyBins());
        m_showMissingValBin.setToolTipText(SHOW_MISSING_VAL_BIN_TOOLTIP);
//        m_applyAggrSettingsButton = new JButton(
//                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
//        m_applyAggrSettingsButton.setHorizontalAlignment(
//        SwingConstants.RIGHT);
//
//        m_applyBarSettingsButton = new JButton(
//                AbstractHistogramProperties.APPLY_BUTTON_LABEL);
//        m_applyBarSettingsButton.setHorizontalAlignment(SwingConstants.RIGHT);
        // create the visualization option elements
        m_showGrid = new JCheckBox(SHOW_GRID_LABEL, vizModel.isShowGridLines());
        m_showBinOutline = new JCheckBox(SHOW_BIN_OUTLINE_LABEL, true);
        m_showBarOutline = new JCheckBox(SHOW_BAR_OUTLINE_LABEL, true);
        m_showElementOutline = new JCheckBox(SHOW_ELEMENT_OUTLINE_LABEL, true);

        m_labelDisplayPolicy = GUIUtils
                .createButtonGroup(LabelDisplayPolicy.values(), null);

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

        m_layoutDisplayPolicy = GUIUtils
                .createButtonGroup(HistogramLayout.values(), null);
        // The bin settings tab
        final JPanel binPanel = createBinSettingsPanel();
        addTab(BIN_AGGR_TAB_LABEL, binPanel);
        // the aggregation settings tab
//        final JPanel aggrPanel = createAggregationSettingsPanel();
//        addTab(AGGREGATION_TAB_LABEL, aggrPanel);

        final JPanel visOptionPanel = createVizSettingsPanel();
        addTab(VIZ_SETTINGS_TAB_LABEL, visOptionPanel);

        //create the details panel
        m_detailsHtmlPane = new JEditorPane("text/html", "");
        //I have to subtract the tab height from the preferred size
        Dimension tabSize = getTabSize();
        if (tabSize == null) {
            tabSize = new Dimension(1, 1);
        }
        m_detailsHtmlPane.setText(GUIUtils.NO_ELEMENT_SELECTED_TEXT);
        m_detailsHtmlPane.setEditable(false);
        m_detailsHtmlPane.setBackground(getBackground());
        m_detailsScrollPane = new JScrollPane(m_detailsHtmlPane);
        m_detailsScrollPane.setPreferredSize(tabSize);
        m_detailsPane = new JPanel();
        m_detailsPane.add(m_detailsScrollPane);
        addTab(DETAILS_TAB_LABEL, m_detailsPane);
        addLabelDisplayListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                for (final Enumeration<AbstractButton> buttons =
                    m_labelOrientation.getElements();
                buttons.hasMoreElements();) {
                    //disable the label orientation buttons if noe label
                    //should be displayed
                    buttons.nextElement().setEnabled(
                            !LabelDisplayPolicy.NONE.equals(
                            getLabelDisplayPolicy()));
                }
            }
        });
    }

    private Dimension getTabSize() {
        try {
            final Dimension totalSize = getPreferredSize();
            return new Dimension((int)totalSize.getWidth(),
                    (int) totalSize.getHeight() - 10);
        } catch (final Exception e) {
            LOGGER.debug("Exception in getTabSize: " + e.getMessage());
        }
        return null;
    }
//
//    /**
//     * This method is called to resize the tabs.
//     */
//    public void resize() {
//        final Dimension tabSize = getTabSize();
//        if (tabSize != null) {
//            m_detailsScrollPane.setPreferredSize(tabSize);
//        }
//    }

    /**
     * @param html the new details view
     */
    public void updateHTMLDetailsPanel(final String html) {
        m_detailsHtmlPane.setText(html);
        //scroll to the top of the details pane
        m_detailsHtmlPane.setCaretPosition(0);
    }

    /**
     * @param colBox the box with the implementation specific column
     * information
     */
    protected void addColumnTab(final Box colBox) {
        //      the column select tab
        // the aggregation method label box
        final Box aggrLabelBox = Box.createHorizontalBox();
        final JLabel aggrMethLabel = new JLabel(
                AbstractHistogramProperties.AGGREGATION_METHOD_LABEL);
        aggrMethLabel.setVerticalAlignment(SwingConstants.CENTER);
        aggrLabelBox.add(Box.createHorizontalGlue());
        aggrLabelBox.add(Box.createHorizontalStrut(5));
        aggrLabelBox.add(aggrMethLabel);
        aggrLabelBox.add(Box.createHorizontalStrut(5));
        aggrLabelBox.add(Box.createHorizontalGlue());
        // the aggregation method radio button box
        final Box aggrButtonBox = GUIUtils
                .createButtonGroupBox(m_aggrMethButtonGrp, true, null, false);
        aggrButtonBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        final Box aggrLabelButtonBox = Box.createVerticalBox();
        aggrLabelButtonBox.setBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.RAISED));
        aggrLabelButtonBox.add(aggrLabelBox);
        aggrLabelButtonBox.add(aggrButtonBox);
        final Box rootBox = Box.createHorizontalBox();
        rootBox.add(aggrLabelButtonBox);
        rootBox.add(Box.createHorizontalStrut(5));
        rootBox.add(colBox);
        final JPanel columnPanel = new JPanel();
        columnPanel.add(rootBox);
        final int tabCount = getTabCount();
        int colTabIdx = 1;
        if (tabCount < 1) {
            colTabIdx = 0;
        }
        insertTab(COLUMN_TAB_LABEL, null, columnPanel,
                null, colTabIdx);
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
        vizBox.add(m_showBinOutline);
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(m_showBarOutline);
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(m_showElementOutline);

//label layout box
        final Box labelBox = Box.createHorizontalBox();
        labelBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Labels"));
        labelBox.add(Box.createHorizontalGlue());
        final Box labelDisplayBox = GUIUtils
                .createButtonGroupBox(m_labelDisplayPolicy, true, null, false);
        labelBox.add(labelDisplayBox);
        labelBox.add(Box.createHorizontalGlue());
        final Box labelOrientationBox = GUIUtils
                .createButtonGroupBox(m_labelOrientation, true,
                        LABEL_ORIENTATION_LABEL, false);
        labelBox.add(labelOrientationBox);
        labelBox.add(Box.createHorizontalGlue());

//bar layout box
        final Box layoutDisplayBox = GUIUtils
        .createButtonGroupBox(m_layoutDisplayPolicy, false, null, false);
        final Box binWidthBox = Box.createVerticalBox();
        // barWidthBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        final Box binWidthLabelBox = Box.createHorizontalBox();
        final JLabel binWidthLabel = new JLabel(
                AbstractHistogramProperties.BIN_SIZE_LABEL);
        binWidthLabelBox.add(Box.createHorizontalGlue());
        binWidthLabelBox.add(binWidthLabel);
        binWidthLabelBox.add(Box.createHorizontalGlue());
        binWidthBox.add(binWidthLabelBox);
        // the bin width slider box
        final Box binWidthSliderBox = Box.createHorizontalBox();
        binWidthSliderBox.add(Box.createHorizontalGlue());
        binWidthSliderBox.add(m_binWidth);
        binWidthSliderBox.add(Box.createHorizontalGlue());
        binWidthBox.add(binWidthSliderBox);
        final Box barLayoutBox = Box.createVerticalBox();
        barLayoutBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Bar Layout"));
        barLayoutBox.add(Box.createVerticalGlue());
        barLayoutBox.add(binWidthBox);
        barLayoutBox.add(Box.createVerticalGlue());
        barLayoutBox.add(layoutDisplayBox);
        barLayoutBox.add(Box.createVerticalGlue());

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
     * The bar aggregation settings:
     * <ol>
     * <li>aggregation method</li>
     * <li>aggregation column</li>
     * </ol>.
     *
     * @return the aggregation settings panel
     */
//    private JPanel createAggregationSettingsPanel() {
//        final JPanel aggrPanel = new JPanel();
//
//        aggrPanel.add(aggrBox);
//        return aggrPanel;
//    }

    /**
     * The bin related settings:
     * <ol>
     * <li>size</li>
     * <li>number of bins</li>
     * <li>show empty bins</li>
     * <li>show missing value bin</li>
     * </ol>.
     *
     * @return the panel with all bar related settings
     */
    private JPanel createBinSettingsPanel() {
        final Box binNoBox = Box.createVerticalBox();
        // barNoBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        // the number of bars label box
        final Box noOfBinsLabelBox = Box.createHorizontalBox();
        final JLabel noOfBinsLabel = new JLabel(
                AbstractHistogramProperties.NUMBER_OF_BINS_LABEL);
        noOfBinsLabelBox.add(Box.createHorizontalGlue());
        noOfBinsLabelBox.add(noOfBinsLabel);
        noOfBinsLabelBox.add(Box.createHorizontalStrut(10));
        noOfBinsLabelBox.add(m_noOfBinsLabel);
        noOfBinsLabelBox.add(Box.createHorizontalGlue());
        binNoBox.add(noOfBinsLabelBox);
        // the number of bins slider box
        final Box noOfBinsSliderBox = Box.createHorizontalBox();
        noOfBinsSliderBox.add(Box.createHorizontalGlue());
        noOfBinsSliderBox.add(m_noOfBins);
        noOfBinsSliderBox.add(Box.createHorizontalGlue());
        binNoBox.add(noOfBinsSliderBox);
        // the box with the select boxes and apply button
        final Box binSelButtonBox = Box.createVerticalBox();
        // barSelButtonBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        final Box binSelectBox = Box.createVerticalBox();
        // barSelectBox.setBorder(BorderFactory
        // .createEtchedBorder(EtchedBorder.RAISED));
        binSelectBox.add(m_showEmptyBins);
        binSelectBox.add(Box.createVerticalGlue());
        binSelectBox.add(m_showMissingValBin);
        binSelectBox.add(Box.createVerticalGlue());
        binSelButtonBox.add(binSelectBox);
        binSelButtonBox.add(Box.createVerticalGlue());
//        final Box buttonBox = Box.createHorizontalBox();
//        // buttonBox.setBorder(BorderFactory
//        // .createEtchedBorder(EtchedBorder.RAISED));
//        final Dimension d = new Dimension(75, 1);
//        buttonBox.add(Box.createRigidArea(d));
//        buttonBox.add(m_applyBarSettingsButton);
//        barSelButtonBox.add(buttonBox);
        final Box binBox = Box.createHorizontalBox();
        binBox.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        binBox.add(Box.createHorizontalGlue());
        binBox.add(binNoBox);
        binBox.add(Box.createHorizontalGlue());
        binBox.add(binSelButtonBox);

        final JPanel binPanel = new JPanel();
        binPanel.add(binBox);
        return binPanel;
    }

  /**
     *
     * @param spec current data table specification
     * @param xColName preselected x column name
     * @param yColumns preselected y column names
     * @param aggrMethod the current {@link AggregationMethod}
     */
    public abstract void updateColumnSelection(final DataTableSpec spec,
            final String xColName, final Collection<ColorColumn> yColumns,
            final AggregationMethod aggrMethod);

    /**
     * Updates the available slider with the current values of the Histogram
     * plotter.
     *
     * @param vizModel the {@link AbstractHistogramVizModel} object which
     *            contains the data
     */
    public void updateHistogramSettings(
            final AbstractHistogramVizModel vizModel) {
//        final long startTime = System.currentTimeMillis();
        if (vizModel == null) {
            return;
        }
//update the bin settings tab components
        //update the number of bin values
        final int maxNoOfBins = vizModel.getMaxNoOfBins();
        final int currentNoOfBins = vizModel.getNoOfBins();
        final ChangeListener[] noOfListeners = m_noOfBins.getChangeListeners();
        for (final ChangeListener listener : noOfListeners) {
            m_noOfBins.removeChangeListener(listener);
        }
        m_noOfBins.setMaximum(maxNoOfBins);
        m_noOfBins.setValue(currentNoOfBins);
        m_noOfBinsLabel.setText(Integer.toString(currentNoOfBins));
        GUIUtils.setSliderLabels(m_noOfBins, 2, true);
        // disable this noOfbins slider for nominal values
        if (vizModel.isBinNominal() || !vizModel.supportsHiliting()) {
            m_noOfBins.setEnabled(false);
            if (!vizModel.supportsHiliting()) {
                m_noOfBins.setToolTipText("Not available for "
                        + "this histogram implementation");
            } else {
                m_noOfBins
                    .setToolTipText("Only available for numerical properties");
            }
        } else {
            m_noOfBins.setEnabled(true);
            m_noOfBins.setToolTipText(
                    AbstractHistogramProperties.NO_OF_BINS_TOOLTIP);
        }
        for (final ChangeListener listener : noOfListeners) {
            m_noOfBins.addChangeListener(listener);
        }
        //show empty bins box
        updateCheckBox(m_showEmptyBins, vizModel.isShowEmptyBins(),
                vizModel.containsEmptyBins());
        //show missing value bin box
        updateCheckBox(m_showMissingValBin, vizModel.isShowMissingValBin(),
                vizModel.containsMissingValueBin());
//update the aggregation settings tab
//      set the right aggregation method settings
        //since the set selected method doesn't trigger an event
        //we don't need to remove/add the action listener
        final Collection<? extends ColorColumn> aggrColumns =
            vizModel.getAggrColumns();
        if ((aggrColumns == null || aggrColumns.size() < 1)
                && !vizModel.supportsHiliting()) {
            //if we have no aggregation columns selected disable all
            //aggregation methods but not count
            for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                    .getElements(); buttons.hasMoreElements();) {
                final AbstractButton button = buttons.nextElement();
                if (!button.getActionCommand()
                        .equals(AggregationMethod.COUNT.name())) {
                    button.setEnabled(false);
                    button.setToolTipText(AGGR_METHOD_DISABLED_TOOLTIP);
                }
//              select the current aggregation method
                if (button.getActionCommand()
                        .equals(vizModel.getAggregationMethod().name())) {
                    button.setSelected(true);
                }
            }
        } else {
            //enable all buttons
            for (final Enumeration<AbstractButton> buttons = m_aggrMethButtonGrp
                    .getElements(); buttons.hasMoreElements();) {
                final AbstractButton button = buttons.nextElement();
                button.setEnabled(true);
                //remove the tool tip
                button.setToolTipText(null);
                //select the current aggregation method
                if (button.getActionCommand()
                        .equals(vizModel.getAggregationMethod().name())) {
                    button.setSelected(true);
                }
            }
        }
//update the visualization settings tab
        //show grid lines
        updateCheckBox(m_showGrid, vizModel.isShowGridLines(), true);
        //Labels group
        //select the current display policy
        //since the set selected method doesn't trigger an event
        //we don't need to remove/add the action listener
        for (final Enumeration<AbstractButton> buttons = m_labelDisplayPolicy
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand().equals(
                    vizModel.getLabelDisplayPolicy().getActionCommand())) {
                button.setSelected(true);
            }
        }

      //select the current label orientation and disable the buttons if the
        //show none label options is selected
        //since the set selected method doesn't trigger an event
        //we don't need to remove/add the action listener
        for (final Enumeration<AbstractButton> buttons = m_labelOrientation
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand()
                    .equals(LABEL_ORIENTATION_VERTICAL)
                    && vizModel.isShowLabelVertical()) {
                button.setSelected(true);
            } else if (button.getActionCommand()
                    .equals(LABEL_ORIENTATION_HORIZONTAL)
                    && !vizModel.isShowLabelVertical()) {
                button.setSelected(true);
            }
            //disable the label orientation buttons if noe label
            //should be displayed
            button.setEnabled(!LabelDisplayPolicy.NONE.equals(
                    vizModel.getLabelDisplayPolicy()));
        }

        //Bar layout group
        //select the current layout
        //since the set selected method doesn't trigger an event
        //we don't need to remove/add the action listener
        for (final Enumeration<AbstractButton> buttons = m_layoutDisplayPolicy
                .getElements(); buttons.hasMoreElements();) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand()
                    .equals(vizModel.getHistogramLayout().getActionCommand())) {
                button.setSelected(true);
            }
        }
        final int currentBinWidth = vizModel.getBinWidth();
        final int maxBinWidth = vizModel.getMaxBinWidth();
        int minBinWidth = AbstractHistogramVizModel.MIN_BIN_WIDTH;
        if (minBinWidth > maxBinWidth) {
            minBinWidth = maxBinWidth;
        }
        // update the bin width values
        final ChangeListener[] widthListeners = m_binWidth.getChangeListeners();
        for (final ChangeListener listener : widthListeners) {
            m_binWidth.removeChangeListener(listener);
        }
        m_binWidth.setMaximum(maxBinWidth);
        m_binWidth.setMinimum(minBinWidth);
        m_binWidth.setValue(currentBinWidth);
        m_binWidth.setEnabled(true);
        m_binWidth.setToolTipText(
                AbstractHistogramProperties.BIN_WIDTH_TOOLTIP);
        GUIUtils.setSliderLabels(m_binWidth, 2, false);
        for (final ChangeListener listener : widthListeners) {
            m_binWidth.addChangeListener(listener);
        }
        //show bin outline
        updateCheckBox(m_showBinOutline, vizModel.isShowBinOutline(),
                true);
        //show bar outline
        updateCheckBox(m_showBarOutline, vizModel.isShowBarOutline(),
                true);
        //show element outline
        updateCheckBox(m_showElementOutline, vizModel.isShowElementOutline(),
                true);
//        final long endTime = System.currentTimeMillis();
//        final long durationTime = endTime - startTime;
//        LOGGER.debug("Time for updateHistogramSettings. "
//                + durationTime + " ms");
    }

    /**
     * Removes all listener from the given box updates the values and
     * adds all previous removed listener.
     * @param box the select box to update
     * @param selected the selected value
     * @param enabled the enable value
     */
    private static void updateCheckBox(final JCheckBox box,
            final boolean selected,
            final boolean enabled) {
        final ItemListener[] listeners =
            box.getItemListeners();
        for (final ItemListener listener : listeners) {
            box.removeItemListener(listener);
        }
        box.setSelected(selected);
        box.setEnabled(enabled);
        for (final ItemListener listener : listeners) {
            box.addItemListener(listener);
        }
    }

    /**
     * @return the currently set bin width
     */
    public int getBinWidth() {
        return m_binWidth.getValue();
    }

    /**
     * @return the current no of bins
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
        return AggregationMethod.getMethod4Command(methodName);
    }

    /**
     * @param actionCommand the action command of the radio button which should
     *            be the name of the <code>AggregationMethod</code>
     */
    protected abstract void onSelectAggrMethod(final String actionCommand);

    /**
     * Helper method to update the number of bins text field.
     *
     * @param noOfbins the number of bins
     */
    protected void updateNoOfBinsText(final int noOfbins) {
        m_noOfBinsLabel.setText(Integer.toString(noOfbins));
    }

    /**
     * @return the current value of the show grid line select box
     */
    public boolean isShowGrid() {
        return m_showGrid.isSelected();
    }


    /**
     * @return the current value of the show bin outline select box
     */
    public boolean isShowBinOutline() {
        return m_showBinOutline.isSelected();
    }

    /**
     * @return the current value of the show bar outline select box
     */
    public boolean isShowBarOutline() {
        return m_showBarOutline.isSelected();
    }

    /**
     * @return the current value of the show element outline select box
     */
    public boolean isShowElementOutline() {
        return m_showElementOutline.isSelected();
    }

    /**
     * @return if the empty bins should be shown
     */
    public boolean isShowEmptyBins() {
        if (m_showEmptyBins == null) {
            return false;
        }
        return m_showEmptyBins.isSelected();
    }

    /**
     * @return if the missing value bin should be shown
     */
    public boolean isShowMissingValBin() {
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
     * @param listener adds the listener to the show bin outline check box
     */
    public void addShowBinOutlineChangedListener(final ItemListener listener) {
        m_showBinOutline.addItemListener(listener);
    }

    /**
     * @param listener adds the listener to the show bar outline check box
     */
    public void addShowBarOutlineChangedListener(final ItemListener listener) {
        m_showBarOutline.addItemListener(listener);
    }

    /**
     * @param listener adds a listener to the show element outline check box.
     */
    protected void addShowElementOutlineChangedListener(
            final ItemListener listener) {
        m_showElementOutline.addItemListener(listener);
    }

    /**
     * @param listener adds the listener to the bin width slider
     */
    protected void addBinWidthChangeListener(final ChangeListener listener) {
        m_binWidth.addChangeListener(listener);
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
    protected void addNoOfBinsChangeListener(final ChangeListener listener) {
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
}
