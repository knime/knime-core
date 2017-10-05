/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize3;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.node.preproc.normalize3.NormalizerConfig.NormalizerMode;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * The NormalizeNodeDialog lets the user choose the three different methods of normalization.
 *
 * @author Nicolas Cebron, University of Konstanz
 * @since 3.2 getTableSpec() added to allow for altering exaction of {@link DataTableSpec}s from the {@link PortObjectSpec}.
 */
public class Normalizer3NodeDialog extends NodeDialogPane {

    /*
     * The tab's name.
     */
    private static final String TAB = "Methods";

    private JTextField m_minTextField;

    private JTextField m_maxTextField;

    private DataColumnSpecFilterPanel m_filterPanel;

    private Map<NormalizerMode, JRadioButton> m_buttonMap = new HashMap<>();

    /**
     * Creates a new dialog for the Normalize Node.
     */
    @SuppressWarnings("unchecked")
    public Normalizer3NodeDialog() {
        super();
        m_filterPanel = new DataColumnSpecFilterPanel(DoubleValue.class);
        JPanel panel = generateContent();

        JPanel all = new JPanel(new BorderLayout());
        all.add(m_filterPanel, BorderLayout.NORTH);
        all.add(panel, BorderLayout.SOUTH);
        super.addTab(TAB, all);
    }

    /*
     * Generates the radio buttons and text fields
     */
    private JPanel generateContent() {
        JPanel panel = new JPanel();

        // min-max
        JPanel panel1 = new JPanel();
        GridLayout gl = new GridLayout(2, 4);
        panel1.setLayout(gl);

        final JRadioButton minmaxButton = new JRadioButton("Min-Max Normalization");
        minmaxButton.setSelected(true);
        JLabel nmin = new JLabel("Min: ");
        JPanel spanel1 = new JPanel();
        spanel1.setLayout(new BorderLayout());
        spanel1.add(nmin, BorderLayout.EAST);
        spanel1.setMaximumSize(new Dimension(30, 10));

        m_minTextField = new JTextField(5);

        JLabel nmax = new JLabel("Max: ");
        JPanel spanel2 = new JPanel();
        spanel2.setLayout(new BorderLayout());
        spanel2.add(nmax, BorderLayout.EAST);
        spanel2.setMaximumSize(new Dimension(30, 10));

        m_maxTextField = new JTextField(5);

        panel1.add(minmaxButton);
        panel1.add(spanel1);
        panel1.add(m_minTextField);
        panel1.add(Box.createHorizontalGlue());
        panel1.add(new JPanel());
        panel1.add(spanel2);
        panel1.add(m_maxTextField);
        panel1.add(Box.createHorizontalGlue());

        // z-score
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout());
        final JRadioButton zScoreButton = new JRadioButton("Z-Score Normalization (Gaussian)");
        panel2.add(zScoreButton, BorderLayout.WEST);

        // decimal scaling
        JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout());
        final JRadioButton decButton = new JRadioButton("Normalization by Decimal Scaling");
        panel3.add(decButton, BorderLayout.WEST);

        // Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(minmaxButton);
        group.add(zScoreButton);
        group.add(decButton);
        m_buttonMap.put(NormalizerMode.MINMAX, minmaxButton);
        m_buttonMap.put(NormalizerMode.DECIMALSCALING, decButton);
        m_buttonMap.put(NormalizerMode.Z_SCORE, zScoreButton);

        minmaxButton.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (minmaxButton.isSelected()) {
                    m_minTextField.setEnabled(true);
                    m_maxTextField.setEnabled(true);
                }
            }
        });
        zScoreButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (zScoreButton.isSelected()) {
                    m_minTextField.setEnabled(false);
                    m_maxTextField.setEnabled(false);
                }
            }
        });
        decButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (decButton.isSelected()) {
                    m_minTextField.setEnabled(false);
                    m_maxTextField.setEnabled(false);
                }
            }
        });

        BoxLayout bly = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bly);
        panel.add(panel1);
        panel.add(panel2);
        panel.add(panel3);
        panel.setBorder(BorderFactory.createTitledBorder("Settings"));
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        NormalizerConfig config = new NormalizerConfig();
        DataTableSpec spec = getTableSpec(specs);
        config.loadConfigurationInDialog(settings, spec);
        m_maxTextField.setText(Double.toString(config.getMax()));
        m_minTextField.setText(Double.toString(config.getMin()));
        m_filterPanel.loadConfiguration(config.getDataColumnFilterConfig(), spec);
        m_buttonMap.get(config.getMode()).setSelected(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        NormalizerConfig config = new NormalizerConfig();
        for (Map.Entry<NormalizerMode, JRadioButton> entry : m_buttonMap.entrySet()) {
            if (entry.getValue().isSelected()) {
                config.setMode(entry.getKey());
                break;
            }
        }

        if (NormalizerMode.MINMAX.equals(config.getMode())) {
            config.setMin(getDouble("Min", m_minTextField));
            config.setMax(getDouble("Max", m_maxTextField));
            CheckUtils.checkSetting(config.getMin() <= config.getMax(),
                    "Min (%f) cannot be greater than Max (%f)", config.getMin(), config.getMax());
        }
        m_filterPanel.saveConfiguration(config.getDataColumnFilterConfig());
        config.saveSettings(settings);
    }

    private static double getDouble(final String name, final JTextField inputField) throws InvalidSettingsException {
        try {
            return Double.valueOf(inputField.getText());
        } catch (Exception e) {
            CheckUtils.checkSetting(false,
                "%s must be a valid double value (not a number: \"%s\")", name, inputField.getText());
            return 0;
        }
    }

    /**
     * Get the {@link DataTableSpec} from the {@link PortObjectSpec}s.
     * 
     * @param specs the node's {@link PortObjectSpec}
     * @return the corresponding {@link DataTableSpec} for the {@link PortObjectSpec}
     * @throws NotConfigurableException if the specs are not valid
     */
    protected DataTableSpec getTableSpec(final PortObjectSpec[] specs) throws NotConfigurableException {
        return (DataTableSpec)specs[0];
    }

}
