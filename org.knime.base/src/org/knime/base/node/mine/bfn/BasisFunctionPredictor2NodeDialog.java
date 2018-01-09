/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.bfn;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;

import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.base.node.mine.util.PredictorNodeDialog;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;


/**
 * A dialog to apply data to basis functions. Can be used to set a name for the
 * new, applied column.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @since 2.9
 */
public class BasisFunctionPredictor2NodeDialog extends PredictorNodeDialog {

    private JRadioButton m_defaultButton;

    private JRadioButton m_setButton;

    private JRadioButton m_ignoreButton;

    private JSpinner m_dontKnow;

    /** Key for the applied column: <i>apply_column</i>. */
    public static final String APPLY_COLUMN = PredictorHelper.CFGKEY_PREDICTION_COLUMN;

    /** Key for don't know probability for the unknown class. */
    public static final String DONT_KNOW_PROP = "dont_know_prop";

    /** Config key if don't know should be ignored. */
    public static final String CFG_DONT_KNOW_IGNORE = "ignore_dont_know";

    /** Config key if class probabilities should be appended to the table. */
    public static final String CFG_CLASS_PROBS = "append_class_probabilities";

    /**
     * Creates a new predictor dialog to set a name for the applied column.
     */
    public BasisFunctionPredictor2NodeDialog() {
        super(new SettingsModelBoolean(CFG_CLASS_PROBS, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addOtherControls(final JPanel panel) {
        m_dontKnow = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0, 0.1));
        m_defaultButton = new JRadioButton("Default ", true);
        m_setButton = new JRadioButton("Use ");
        m_ignoreButton = new JRadioButton("Ignore ", true);
        // add don't know probability
        m_ignoreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                selectionChanged();
            }
        });
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_ignoreButton);
        bg.add(m_defaultButton);
        bg.add(m_setButton);
        m_dontKnow.setEditor(new JSpinner.NumberEditor(
                m_dontKnow, "#.##########"));
        m_dontKnow.setPreferredSize(new Dimension(75, 25));
        JPanel dontKnowPanel = new JPanel(new GridLayout(3, 1));
        dontKnowPanel.setBorder(BorderFactory
                .createTitledBorder(" Don't Know Class "));
        FlowLayout left = new FlowLayout(FlowLayout.LEFT);
        final JPanel ignorePanel = new JPanel(left), defaultPanel = new JPanel(left);
        ignorePanel.add(m_ignoreButton);
        defaultPanel.add(m_defaultButton);
        dontKnowPanel.add(ignorePanel);
        dontKnowPanel.add(defaultPanel);
        JPanel usePanel = new JPanel(left);
        dontKnowPanel.add(usePanel);
        usePanel.add(m_setButton);
        usePanel.add(m_dontKnow);
        panel.add(dontKnowPanel);

        m_defaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_dontKnow.setEnabled(false);
            }
        });

        m_setButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_dontKnow.setEnabled(true);
            }
        });
        getLayout().putConstraint(SpringLayout.EAST, dontKnowPanel, 0, SpringLayout.EAST, panel);
        getLayout().putConstraint(SpringLayout.WEST, dontKnowPanel, 0, SpringLayout.WEST, panel);
        super.setLastAdded(dontKnowPanel);
        getPanel().setPreferredSize(new Dimension(400, 240));
    }

    private void selectionChanged() {
            if (m_defaultButton.isSelected() || m_ignoreButton.isSelected()) {
                m_dontKnow.setEnabled(false);
            } else {
                m_dontKnow.setEnabled(true);
            }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        if (settings.getBoolean(CFG_DONT_KNOW_IGNORE, false)) {
            m_ignoreButton.setSelected(true);
            m_dontKnow.setValue(new Double(0.0));
        } else {
            m_ignoreButton.setSelected(false);
            double value = settings.getDouble(DONT_KNOW_PROP, -1.0);
            if (value < 0.0) {
                m_defaultButton.setSelected(true);
                m_dontKnow.setValue(new Double(0.0));
            } else {
                m_setButton.setSelected(true);
                m_dontKnow.setValue(new Double(value));
            }
        }
        selectionChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        if (m_ignoreButton.isSelected()) {
            settings.addBoolean(CFG_DONT_KNOW_IGNORE, true);
            settings.addDouble(DONT_KNOW_PROP, 0.0);
        } else {
            settings.addBoolean(CFG_DONT_KNOW_IGNORE, false);
            if (m_defaultButton.isSelected()) {
                settings.addDouble(DONT_KNOW_PROP, -1.0);
            } else {
                Double value = (Double)m_dontKnow.getValue();
                settings.addDouble(DONT_KNOW_PROP, value.doubleValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void extractTargetColumn(final PortObjectSpec[] specs) {
        DataTableSpec spec = (DataTableSpec)specs[0];
        setLastTargetColumn(spec.getColumnSpec(spec.getNumColumns() - 5));
    }
}
