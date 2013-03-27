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
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilterPanel;


/**
 * The NormalizeNodeDialog lets the user choose the three different methods of
 * normalization.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class NormalizerNodeDialog extends NodeDialogPane {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NormalizerNodeDialog.class);

    /*
     * The tab's name.
     */
    private static final String TAB = "Methods";

    /*
     * DataTableSpec to convert DataCells into column positions.
     */
    private DataTableSpec m_spec;

    /*
     * GUI elements
     */

    private JRadioButton m_minmaxButton;

    private JRadioButton m_zscoreButton;

    private JRadioButton m_decButton;

    private JTextField m_newminTextField;

    private JTextField m_newmaxTextField;

    private ColumnFilterPanel m_filterpanel;

    /**
     * Creates a new dialog for the Normalize Node.
     */
    @SuppressWarnings("unchecked")
    public NormalizerNodeDialog() {
        super();
        JPanel panel = generateContent();
        m_filterpanel = new ColumnFilterPanel(DoubleValue.class);
        m_filterpanel.setIncludeTitle("Normalize");
        m_filterpanel.setExcludeTitle("Do not normalize");
        JPanel all = new JPanel();
        BoxLayout yaxis = new BoxLayout(all, BoxLayout.Y_AXIS);
        all.setLayout(yaxis);
        all.add(panel);
        all.add(m_filterpanel);
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

        m_minmaxButton = new JRadioButton("Min-Max Normalization");
        m_minmaxButton.setSelected(true);
        JLabel nmin = new JLabel("Min: ");
        JPanel spanel1 = new JPanel();
        spanel1.setLayout(new BorderLayout());
        spanel1.add(nmin, BorderLayout.EAST);
        spanel1.setMaximumSize(new Dimension(30, 10));

        m_newminTextField = new JTextField(2);
        JPanel nminpanel = new JPanel();
        nminpanel.setLayout(new BorderLayout());
        nminpanel.add(m_newminTextField, BorderLayout.WEST);

        JLabel nmax = new JLabel("Max: ");
        JPanel spanel2 = new JPanel();
        spanel2.setLayout(new BorderLayout());
        spanel2.add(nmax, BorderLayout.EAST);
        spanel2.setMaximumSize(new Dimension(30, 10));

        m_newmaxTextField = new JTextField(2);
        JPanel nmaxpanel = new JPanel();
        nmaxpanel.setLayout(new BorderLayout());
        nmaxpanel.add(m_newmaxTextField, BorderLayout.WEST);

        panel1.add(m_minmaxButton);
        panel1.add(spanel1);
        panel1.add(nminpanel);
        panel1.add(Box.createHorizontalGlue());
        panel1.add(new JPanel());
        panel1.add(spanel2);
        panel1.add(nmaxpanel);
        panel1.add(Box.createHorizontalGlue());

        // z-score
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout());
        m_zscoreButton = new JRadioButton("Z-Score Normalization");
        panel2.add(m_zscoreButton, BorderLayout.WEST);

        // decimal scaling
        JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout());
        m_decButton = new JRadioButton("Normalization by Decimal Scaling");
        panel3.add(m_decButton, BorderLayout.WEST);

        // Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(m_minmaxButton);
        group.add(m_zscoreButton);
        group.add(m_decButton);

        m_minmaxButton.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (m_minmaxButton.isSelected()) {
                    m_filterpanel.setEnabled(true);
                    m_newminTextField.setEnabled(true);
                    m_newmaxTextField.setEnabled(true);
                }
            }
        });
        m_zscoreButton.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (m_zscoreButton.isSelected()) {
                    m_filterpanel.setEnabled(true);
                    m_newminTextField.setEnabled(false);
                    m_newmaxTextField.setEnabled(false);
                }
            }
        });
        m_decButton.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                if (m_decButton.isSelected()) {
                    m_filterpanel.setEnabled(true);
                    m_newminTextField.setEnabled(false);
                    m_newmaxTextField.setEnabled(false);
                }
            }
        });

        BoxLayout bly = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bly);
        panel.add(panel1);
        panel.add(panel2);
        panel.add(panel3);
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_spec = (DataTableSpec)specs[0];

        String[] cols = null;
        if (settings.containsKey(NormalizerNodeModel.COLUMNS_KEY)) {
            try {
                cols = settings.getStringArray(NormalizerNodeModel.COLUMNS_KEY);
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        if (settings.containsKey(NormalizerNodeModel.MODE_KEY)) {
            try {
                int mode = settings.getInt(NormalizerNodeModel.MODE_KEY);
                switch (mode) {
                case NormalizerNodeModel.NONORM_MODE:
                    // that's an old NormalizerNode setting.
                    // We set the default method z-score and exclude all cols.
                    m_zscoreButton.setSelected(true);
                    cols = new String[0];
                    break;
                case NormalizerNodeModel.MINMAX_MODE:
                    m_minmaxButton.setSelected(true);
                    break;
                case NormalizerNodeModel.ZSCORE_MODE:
                    m_zscoreButton.setSelected(true);
                    break;
                case NormalizerNodeModel.DECIMALSCALING_MODE:
                    m_decButton.setSelected(true);
                    break;
                default:
                    throw new InvalidSettingsException("" + "INVALID MODE");
                }
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        if (settings.containsKey(NormalizerNodeModel.NEWMIN_KEY)) {
            try {
                double nmin = settings.getDouble(
                        NormalizerNodeModel.NEWMIN_KEY);
                m_newminTextField.setText("" + nmin);
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        if (settings.containsKey(NormalizerNodeModel.NEWMAX_KEY)) {
            try {
                double nmax = settings.getDouble(
                        NormalizerNodeModel.NEWMAX_KEY);
                m_newmaxTextField.setText("" + nmax);
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }


        if (cols == null) {
            m_filterpanel.update(m_spec, true, new String[0]);
        } else {
            m_filterpanel.update(m_spec, false, cols);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        int mode = -1;
        if (m_minmaxButton.isSelected()) {
            mode = NormalizerNodeModel.MINMAX_MODE;
        }
        if (m_zscoreButton.isSelected()) {
            mode = NormalizerNodeModel.ZSCORE_MODE;
        }
        if (m_decButton.isSelected()) {
            mode = NormalizerNodeModel.DECIMALSCALING_MODE;
        }
        settings.addInt(NormalizerNodeModel.MODE_KEY, mode);
        String newminString = m_newminTextField.getText();
        double newminD = Double.parseDouble(newminString);
        settings.addDouble(NormalizerNodeModel.NEWMIN_KEY, newminD);

        String newmaxString = m_newmaxTextField.getText();
        double newmaxD = Double.parseDouble(newmaxString);
        settings.addDouble(NormalizerNodeModel.NEWMAX_KEY, newmaxD);

        Set<String> inclset = m_filterpanel.getIncludedColumnSet();
        String[] columns = inclset.toArray(new String[inclset.size()]);
        settings.addStringArray(NormalizerNodeModel.COLUMNS_KEY, columns);

        boolean usedAll = Arrays.deepEquals(columns,
                NormalizerNodeModel.findAllNumericColumns(m_spec));
        settings.addBoolean(NormalizerNodeModel.CFG_USE_ALL_NUMERIC, usedAll);
    }
}
