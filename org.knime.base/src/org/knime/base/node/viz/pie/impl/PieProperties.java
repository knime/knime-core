/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.impl;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeListener;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.ValueScale;
import org.knime.base.node.viz.aggregation.util.GUIUtils;
import org.knime.base.node.viz.aggregation.util.LabelDisplayPolicy;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.node.NodeLogger;


/**
 * The abstract pie properties panel which allows to change the different
 * view options.
 * @author Tobias Koetter, University of Konstanz
 * @param <D> the {@link PieVizModel}
 */
public abstract class PieProperties<D extends PieVizModel>
extends AbstractPlotterProperties {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PieProperties.class);

    /**The label of the pie column.*/
    public static final String PIE_COLUMN_LABEL = "Pie column: ";

    /**The label of the aggregation column.*/
    public static final String AGGREGATION_COLUMN_LABEL =
    "Aggregation column: ";

    private static final String COLUMN_TAB_LABEL =
        "Column/Aggregation settings";

//    private static final String AGGREGATION_TAB_LABEL =
//        "Aggregation settings";

    private static final String VIZ_SETTINGS_TAB_LABEL =
        "Visualization settings";

    private static final String DETAILS_TAB_LABEL =
        "Details";

    private static final String COLUMNS_BORDER_LABEL = "Columns";

    private static final String AGGREGATION_METHOD_LABEL =
        "Aggregation method";

    private static final String AGGREGATION_METHOD_DISABLE_TOOLTIP =
        "Please select a aggregation column first";

    private static final String PIE_SIZE_LABEL = "Pie size:";

    private static final String PIE_SIZE_TOOLTIP = "Diameter of the pie";

    private static final String EXPLODE_SIZE_LABEL = "Explode size:";

    private static final String EXPLODE_SIZE_TOOLTIP =
        "Diameter of the exploded pie";

    private static final String SHOW_MISSING_VALUE_SECTION_LABEL =
        "Show missing value section";

    private static final String SHOW_MISSING_VAL_SECTION_TOOLTIP =
        "Shows a section with rows which have a missing value for the "
        + "selected pie column.";

    private static final String SHOW_SECTION_OUTLINE_LABEL =
        "Show section outline";

    private static final String EXPLODE_SELECTED_SECTIONS =
        "Explode selected sections";

    private static final String SHOW_DETAILS = "Color selected sections";

    private static final String SHOW_DETAILS_TOOLTIP =
        "Show colors for selected sections";

    private static final String SHOW_DETAILS_TOOLTIP_DISABLED =
        "No colors available";

    private static final Dimension HORIZONTAL_SPACER_DIM =
        new Dimension(10, 1);

    private final JSlider m_pieSize;

    private final JSlider m_explodeSize;

    private final ButtonGroup m_aggrMethButtonGrp;

    private final JCheckBox m_showMissingValSection;

    private final JCheckBox m_showSectionOutline;

    private final JCheckBox m_explodeSelectedSections;

    private final JCheckBox m_showDetails;

    private final ButtonGroup m_labelDisplayPolicy;

    private final ButtonGroup m_valueScale;

    private final JPanel m_detailsPane;

    private final JScrollPane m_detailsScrollPane;

    private final JEditorPane m_detailsHtmlPane;


    /**
     * Constructor for class PieProperties.
     *
     * @param vizModel the visualization model to initialize all swing
     * components
     */
    public PieProperties(final D vizModel) {
        if (vizModel == null) {
            throw new IllegalArgumentException("VizModel must not be null");
        }

        // create the additional settings components which get added to the
        // histogram settings panel
        m_pieSize = new JSlider((int)(PieVizModel.MINIMUM_PIE_SIZE * 100),
                (int)(PieVizModel.MAXIMUM_PIE_SIZE * 100),
                (int)(vizModel.getPieSize() * 100));
        m_pieSize.setToolTipText(PIE_SIZE_TOOLTIP);

        m_explodeSize = new JSlider((int)(
                PieVizModel.MINIMUM_EXPLODE_SIZE * 100),
                (int)(PieVizModel.MAXIMUM_EXPLODE_SIZE * 100),
                (int)(vizModel.getExplodeSize() * 100));
        m_pieSize.setToolTipText(EXPLODE_SIZE_TOOLTIP);

        m_aggrMethButtonGrp = GUIUtils.createButtonGroup(
                AggregationMethod.values(), vizModel.getAggregationMethod(),
                new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final String methodName = e.getActionCommand();
                if (!AggregationMethod.valid(methodName)) {
                    throw new IllegalArgumentException(
                            "No valid aggregation method");
                }
                final AggregationMethod aggrMethod =
                    AggregationMethod.getMethod4Command(methodName);
                onSelectAggrMethod(aggrMethod);
            }
        });
        // disable the the button group if no aggregation column is available
        final Enumeration<AbstractButton> buttons =
            m_aggrMethButtonGrp.getElements();
        final boolean enable = vizModel.getAggregationColumnName() != null;
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.setEnabled(enable);
        }
        m_showMissingValSection = new JCheckBox(
                SHOW_MISSING_VALUE_SECTION_LABEL,
                vizModel.showMissingValSection());
        m_showMissingValSection.setToolTipText(
                SHOW_MISSING_VAL_SECTION_TOOLTIP);
        enableMissingSectionOption(vizModel.hasMissingSection());
        m_explodeSelectedSections = new JCheckBox(
        EXPLODE_SELECTED_SECTIONS, vizModel.explodeSelectedSections());

        m_showDetails = new JCheckBox(
                SHOW_DETAILS, vizModel.showDetails());
        enableShowDetailsOption(vizModel.detailsAvailable());
        m_showSectionOutline = new JCheckBox(
                SHOW_SECTION_OUTLINE_LABEL, vizModel.drawSectionOutline());

        m_labelDisplayPolicy = GUIUtils.createButtonGroup(
                LabelDisplayPolicy.values(), vizModel.getLabelDisplayPolicy(),
                null);
        m_valueScale = GUIUtils.createButtonGroup(ValueScale.values(),
                vizModel.getValueScale(), null);
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
    }

    /**
     * @param enable <code>true</code> if the show missing section check box
     * should be enabled or not
     */
    protected void enableMissingSectionOption(final boolean enable) {
        m_showMissingValSection.setEnabled(enable);
    }

    /**
     * @param enable <code>true</code> if the show details check box
     * should be enabled or not
     */
    protected void enableShowDetailsOption(final boolean enable) {
        if (enable) {
            m_showDetails.setToolTipText(SHOW_DETAILS_TOOLTIP);
        } else {
            m_showDetails.setToolTipText(SHOW_DETAILS_TOOLTIP_DISABLED);
        }
        m_showDetails.setEnabled(enable);
    }

    /**
     * @param enable <code>true</code> if the aggregation method group should
     * be enabled
     */
    protected void enableAggrMethodGroup(final boolean enable) {
        final Enumeration<AbstractButton> buttons =
            m_aggrMethButtonGrp.getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.setEnabled(enable);
            if (enable) {
                button.setToolTipText(AggregationMethod.getMethod4Command(
                        button.getActionCommand()).getToolTip());
            } else {
                button.setToolTipText(AGGREGATION_METHOD_DISABLE_TOOLTIP);
            }
        }
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
        vizBox.add(m_showMissingValSection);
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(m_showSectionOutline);
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(m_explodeSelectedSections);
        vizBox.add(Box.createVerticalGlue());
        vizBox.add(m_showDetails);
        vizBox.add(Box.createVerticalGlue());

//label layout box
        final Box labelBox = GUIUtils
                .createButtonGroupBox(m_labelDisplayPolicy, true, "Labels",
                        true);

  //scale box
          final Box scaleBox = GUIUtils
                  .createButtonGroupBox(m_valueScale, true, "Scale", true);
//bar layout box
        final Box pieLayoutBox = Box.createVerticalBox();
        pieLayoutBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Pie Layout"));
        pieLayoutBox.add(Box.createVerticalGlue());
        pieLayoutBox.add(new JLabel(PIE_SIZE_LABEL));
        pieLayoutBox.add(Box.createVerticalGlue());
        pieLayoutBox.add(m_pieSize);
        pieLayoutBox.add(Box.createVerticalGlue());
        pieLayoutBox.add(new JLabel(EXPLODE_SIZE_LABEL));
        pieLayoutBox.add(Box.createVerticalGlue());
        pieLayoutBox.add(m_explodeSize);
        pieLayoutBox.add(Box.createVerticalGlue());

        final Box rootBox = Box.createHorizontalBox();
        rootBox.setBorder(
                BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        rootBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        rootBox.add(vizBox);
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(labelBox);
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(scaleBox);
        rootBox.add(Box.createHorizontalGlue());
        rootBox.add(pieLayoutBox);
        rootBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        vizPanel.add(rootBox);
        return vizPanel;
    }

    /**
     * @param html the new details view
     */
    public void updateHTMLDetailsPanel(final String html) {
        m_detailsHtmlPane.setText(html);
        //scroll to the top of the details pane
        m_detailsHtmlPane.setCaretPosition(0);
    }

    /**
     * @param pieCol the pie column component
     * @param aggrCol the aggregation column component
     */
    protected void addColumnTab(final JComponent pieCol,
            final JComponent aggrCol) {
        //      the column select tab
        final Box aggrLabelButtonBox = GUIUtils.createButtonGroupBox(
                m_aggrMethButtonGrp, true, AGGREGATION_METHOD_LABEL, true);
        final Box rootBox = Box.createHorizontalBox();
        rootBox.add(aggrLabelButtonBox);
        rootBox.add(Box.createHorizontalStrut(5));
        rootBox.add(createColumnSettingsBox(pieCol, aggrCol));
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
     * @param vizModel the actual {@link PieVizModel}
     */
    public void updatePanel(final D vizModel) {
        if (vizModel == null) {
            return;
        }
        //select the actual aggregation method
        final Enumeration<AbstractButton> buttons =
            m_aggrMethButtonGrp.getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            if (button.getActionCommand()
                    .equals(vizModel.getAggregationMethod().name())) {
                button.setSelected(true);
            }
            button.setEnabled(vizModel.getAggregationColumnName() != null);
        }
        m_explodeSelectedSections.setSelected(
                vizModel.explodeSelectedSections());
        final Enumeration<AbstractButton> labels =
            m_labelDisplayPolicy.getElements();
        while (labels.hasMoreElements()) {
            final AbstractButton button = labels.nextElement();
            if (button.getActionCommand().equals(
                    vizModel.getLabelDisplayPolicy().getActionCommand())) {
                button.setSelected(true);
            }
        }
        m_pieSize.setValue((int)(vizModel.getPieSize() * 100));
        m_explodeSize.setValue((int) vizModel.getExplodeSize() * 100);
        m_showDetails.setSelected(vizModel.showDetails());
        m_showMissingValSection.setSelected(vizModel.showMissingValSection());
        enableMissingSectionOption(vizModel.hasMissingSection());
        enableShowDetailsOption(vizModel.detailsAvailable());
        m_showSectionOutline.setSelected(vizModel.drawSectionOutline());
        updatePanelInternal(vizModel);
    }

    /**
     * @param vizModel the actual {@link PieVizModel}
     */
    protected abstract void updatePanelInternal(final D vizModel);

    /**
     * @return the label display policy
     */
    public ValueScale getValueScale() {
        final String actionCommand = m_valueScale.getSelection()
                .getActionCommand();
        return ValueScale.getScale4Command(actionCommand);
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
     * @param aggrMethod new {@link AggregationMethod}
     */
    protected abstract void onSelectAggrMethod(
            final AggregationMethod aggrMethod);

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
     * @param listener the listener to listen if the label display policy has
     *            changed
     */
    protected void addValueScaleListener(final ActionListener listener) {
        final Enumeration<AbstractButton> buttons = m_valueScale.getElements();
        while (buttons.hasMoreElements()) {
            final AbstractButton button = buttons.nextElement();
            button.addActionListener(listener);
        }
    }

    /**
     * @param listener adds the listener to the show section outline check box
     */
    public void addShowSectionOutlineChangedListener(
            final ItemListener listener) {
        m_showSectionOutline.addItemListener(listener);
    }

    /**
     * @param listener adds the listener to the bin width slider
     */
    protected void addPieSizeChangeListener(final ChangeListener listener) {
        m_pieSize.addChangeListener(listener);
    }

    /**
     * @param listener adds the listener to the bin width slider
     */
    protected void addExplodeSizeChangeListener(final ChangeListener listener) {
        m_explodeSize.addChangeListener(listener);
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
    protected void addExplodeSelectedSectionListener(
            final ItemListener listener) {
        m_explodeSelectedSections.addItemListener(listener);
    }

    /**
     * @param listener adds the listener to the show empty bins select box
     */
    protected void addShowDetailsListener(
            final ItemListener listener) {
        m_showDetails.addItemListener(listener);
    }

    /**
     * @param listener adds the listener to the show missing value bin
     * select box
     */
    protected void addShowMissingValSectionListener(
            final ItemListener listener) {
        m_showMissingValSection.addItemListener(listener);
    }


    /**
     * @return the column information box
     */
    private static Box createColumnSettingsBox(final JComponent pieCol,
            final JComponent aggrCol) {
      //the x column box
        final Box xColumnBox = Box.createHorizontalBox();
//        xColumnBox.setBorder(BorderFactory
//                .createEtchedBorder(EtchedBorder.RAISED));
        final JLabel xColLabelLabel =
            new JLabel(PieProperties.PIE_COLUMN_LABEL);
        xColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
        xColumnBox.add(xColLabelLabel);
        xColumnBox.add(Box.createHorizontalGlue());
        xColumnBox.add(pieCol);
        xColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
//the aggregation column box
        final Box aggrColumnBox = Box.createHorizontalBox();
//      xColumnBox.setBorder(BorderFactory
//              .createEtchedBorder(EtchedBorder.RAISED));
      final JLabel aggrColLabelLabel =
          new JLabel(PieProperties.AGGREGATION_COLUMN_LABEL);
      aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
      aggrColumnBox.add(aggrColLabelLabel);
      aggrColumnBox.add(Box.createHorizontalGlue());
      aggrColumnBox.add(aggrCol);
      aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));
//        final Box aggrColumnBox = Box.createHorizontalBox();
//        aggrColumnBox.add(m_aggrCol);
//        aggrColumnBox.add(Box.createRigidArea(HORIZONTAL_SPACER_DIM));

//the box which surround both column selection boxes
        final Box columnsBox = Box.createVerticalBox();
        columnsBox.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), COLUMNS_BORDER_LABEL));
        columnsBox.add(Box.createVerticalGlue());
        columnsBox.add(xColumnBox);
        columnsBox.add(Box.createVerticalGlue());
        columnsBox.add(aggrColumnBox);
        columnsBox.add(Box.createVerticalGlue());
        return columnsBox;
    }
}
