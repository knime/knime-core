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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   10.11.2011 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.image;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.mine.decisiontree2.image.DecTreeToImageNodeSettings.Scaling;
import org.knime.base.node.mine.decisiontree2.image.DecTreeToImageNodeSettings.UnfoldMethod;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * The node dialog of the Decision Tree to Image node.
 *
 * @author Heiko Hofer
 */
public class DecTreeToImageNodeDialog extends NodeDialogPane {
    private static final NumberFormat format;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        format = new DecimalFormat("#.0####%", symbols);
    }


    private static final String FIXED = "Fixed value";
    private static final String FIT = "Fit to image area";
    private static final String SHRINK = "Shrink to image area";
    private final DecTreeToImageNodeSettings m_settings;

    private JRadioButton m_unfoldMethodLevel;
    private JSpinner m_unfoldToLevel;
    private JRadioButton m_unfoldMethodCoverage;
    private JTextField m_unfoldWithCoverage;
    private JCheckBox m_displayTable;
    private JCheckBox m_displayChart;

    private JTextField m_width;
    private JTextField m_height;
    private JComboBox m_scaling;
    private JComboBox m_scaleFactor;

    private ButtonGroup m_unfoldMethod;


    /** Create a new instance. */
    DecTreeToImageNodeDialog() {
        m_settings = new DecTreeToImageNodeSettings();
        addTab("Settings", createSettingsPanel());
    }

    private JPanel createSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;


        Insets leftInsets = new Insets(3, 8, 3, 8);
        Insets rightInsets = new Insets(3, 0, 3, 8);
        Insets leftCategoryInsets = new Insets(11, 8, 3, 8);
        Insets rightCategoryInsets = new Insets(11, 0, 3, 8);



        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Width (in Pixel):"), c);
        c.gridx = 1;
        c.insets = rightCategoryInsets;
        c.weightx = 1;
        m_width = new JTextField();
        p.add(m_width, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Height (in Pixel):"), c);
        c.gridx = 1;
        c.insets = rightInsets;
        c.weightx = 1;
        m_height = new JTextField();
        p.add(m_height, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Tree scaling:"), c);
        c.gridx = 1;
        c.insets = rightInsets;
        c.weightx = 0;
        m_scaling = new JComboBox(new Object[] {FIXED, FIT, SHRINK});
        m_scaling.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                String selected = (String)m_scaling.getSelectedItem();
                m_scaleFactor.setEnabled(selected.equals(FIXED));
            }
        });
        p.add(m_scaling, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        p.add(new JLabel("Zoom:"), c);
        c.gridx = 1;
        c.insets = rightInsets;
        c.weightx = 1;

        m_scaleFactor = new JComboBox(new Object[] {
                "140.0%", "120.0%", "100.0%", "80.0%", "60.0%"});
        m_scaleFactor.setEditable(true);
        m_scaleFactor.setSelectedItem("100.0%");
        p.add(m_scaleFactor, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 2;
        c.weightx = 0;
        JPanel unfoldMethodPanel = createUnfoldMethodPanel();
        unfoldMethodPanel.setBorder(
                BorderFactory.createTitledBorder("Branch Display"));
        p.add(unfoldMethodPanel, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 2;
        c.weightx = 0;
        JPanel nodeDisplayPanel = createNodeDisplayPanel();
        nodeDisplayPanel.setBorder(
                BorderFactory.createTitledBorder("Node Display"));
        p.add(nodeDisplayPanel, c);

        c.gridx = 0;
        c.gridy++;
        c.weighty = 1;
        c.gridwidth = 2;
        p.add(new JLabel(), c);

        return p;
    }

    private JPanel createUnfoldMethodPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;


        Insets leftInsets = new Insets(3, 0, 3, 8);
        Insets rightInsets = new Insets(3, 0, 3, 0);
        Insets leftCategoryInsets = new Insets(0, 0, 3, 8);
        Insets rightCategoryInsets = new Insets(0, 0, 3, 0);

        ActionListener unfoldMethodListener = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                updateUnfoldOptions();
            }

        };

        c.gridx = 0;
        c.insets = leftCategoryInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        m_unfoldMethodCoverage = new JRadioButton(
                "Unfold with data coverage:");
        m_unfoldMethodCoverage.addActionListener(unfoldMethodListener);
        p.add(m_unfoldMethodCoverage, c);
        c.gridx = 1;
        c.insets = rightCategoryInsets;
        c.weightx = 1;
        m_unfoldWithCoverage = new JTextField();
        p.add(m_unfoldWithCoverage, c);


        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        c.gridwidth = 1;
        c.weightx = 0;
        m_unfoldMethodLevel = new JRadioButton("Unfold to level:");
        m_unfoldMethodLevel.addActionListener(unfoldMethodListener);
        p.add(m_unfoldMethodLevel, c);
        c.gridx = 1;
        c.insets = rightInsets;
        c.weightx = 1;
        m_unfoldToLevel = new JSpinner(
                new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
        p.add(m_unfoldToLevel, c);

        m_unfoldMethod = new ButtonGroup();
        m_unfoldMethod.add(m_unfoldMethodLevel);
        m_unfoldMethod.add(m_unfoldMethodCoverage);

        return p;
    }

    private JPanel createNodeDisplayPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.BASELINE;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1;
        c.weighty = 0;

        Insets leftInsets = new Insets(3, 0, 3, 8);
        Insets leftCategoryInsets = new Insets(0, 0, 3, 8);

        c.insets = leftCategoryInsets;
        m_displayTable = new JCheckBox("Display table");
        p.add(m_displayTable, c);

        c.gridy++;
        c.gridx = 0;
        c.insets = leftInsets;
        m_displayChart = new JCheckBox("Display chart");
        p.add(m_displayChart, c);

        return p;
    }

    private void updateUnfoldOptions() {
        m_unfoldToLevel.setEnabled(m_unfoldMethodLevel.isSelected());
        m_unfoldWithCoverage.setEnabled(m_unfoldMethodCoverage.isSelected());
    }

    private float getScaleFactor() throws InvalidSettingsException {
        Object selected = m_scaleFactor.getSelectedItem();
        String str = ((String)selected).trim();
        str = str.endsWith("%") ? str : str + "%";
        float scaleFactor = -1f;

        try {
            scaleFactor = format.parse(str).floatValue();
        } catch (ParseException e) {
            throw new InvalidSettingsException("Cannot parse number in "
                    + "the field \"Zoom\"");
        }

        if (scaleFactor < 0.1f) {
            throw new InvalidSettingsException(
                    "A zoom which is lower than 10% "
                    + "is not supported");
        }
        if (scaleFactor > 5f) {
            throw new InvalidSettingsException(
                    "A zoom which is greater than 500% "
                    + "is not supported");
        }
        return scaleFactor;
    }

    private void setScaleFactor(final float scaleFactor) {
        String sf = format.format(scaleFactor);
        m_scaleFactor.setSelectedItem(sf);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.setUnfoldToLevel(
                ((Number)m_unfoldToLevel.getValue()).intValue());
        try {
            String str = m_unfoldWithCoverage.getText().trim();
            str = str.endsWith("%") ? str : str + "%";
            m_settings.setUnfoldWidthCoverage(format.parse(str).doubleValue());
            m_unfoldWithCoverage.setText(
                    format.format(m_settings.getUnfoldWithCoverage()));
        } catch (ParseException e) {
            throw new InvalidSettingsException("Cannot parse number in "
                    + "the field \"Unfold with data coverage\"");
        }
        if (m_unfoldMethodLevel.isSelected()) {
            m_settings.setUnfoldMethod(UnfoldMethod.level);
        } else {
            m_settings.setUnfoldMethod(UnfoldMethod.totalCoverage);
        }
        m_settings.setDisplayTable(m_displayTable.isSelected());
        m_settings.setDisplayChart(m_displayChart.isSelected());
        m_settings.setWidth(Integer.valueOf(m_width.getText()));
        m_settings.setHeight(Integer.valueOf(m_height.getText()));
        m_settings.setScaleFactor(getScaleFactor());
        if (m_scaling.getSelectedItem().equals(FIXED)) {
            m_settings.setScaling(Scaling.fixed);
        } else if (m_scaling.getSelectedItem().equals(FIT)) {
            m_settings.setScaling(Scaling.fit);
        } else {
            m_settings.setScaling(Scaling.shrink);
        }

        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_unfoldToLevel.setValue(new Integer(m_settings.getUnfoldToLevel()));
        m_unfoldWithCoverage.setText(
                format.format(m_settings.getUnfoldWithCoverage()));
        if (m_settings.getUnfoldMethod().equals(UnfoldMethod.level)) {
            m_unfoldMethod.setSelected(m_unfoldMethodLevel.getModel(), true);
        } else { // m_settings.getUnfoldMethod().equals(
                 //     unfoldMethod.totalCoverage)
            m_unfoldMethod.setSelected(m_unfoldMethodCoverage.getModel(), true);
        }
        m_displayTable.setSelected(m_settings.getDisplayTable());
        m_displayChart.setSelected(m_settings.getDisplayChart());
        m_width.setText(Integer.toString(m_settings.getWidth()));
        m_height.setText(Integer.toString(m_settings.getHeight()));
        setScaleFactor(m_settings.getScaleFactor());
        if (m_settings.getScaling().equals(Scaling.fixed)) {
            m_scaling.setSelectedItem(FIXED);
        } else if (m_settings.getScaling().equals(Scaling.fit)) {
            m_scaling.setSelectedItem(FIT);
        } else {
            m_scaling.setSelectedItem(SHRINK);
        }
        String selected = (String)m_scaling.getSelectedItem();
        m_scaleFactor.setEnabled(selected.equals(FIXED));

        updateUnfoldOptions();
    }

}
