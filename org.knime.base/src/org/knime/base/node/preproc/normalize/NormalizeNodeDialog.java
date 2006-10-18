/* 
 * -------------------------------------------------------------------
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.util.ColumnFilterPanel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;


/**
 * The NormalizeNodeDialog lets the user choose the three different methods of
 * normalization.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class NormalizeNodeDialog extends NodeDialogPane {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NormalizeNodeDialog.class);

    /*
     * The tab's name.
     */
    private static final String TAB = "Methods";

    /*
     * The tab2's name.
     */
    private static final String TAB2 = "Columns";

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
    NormalizeNodeDialog() {
        super();
        JPanel panel = generateContent();
        m_filterpanel = new ColumnFilterPanel(DoubleValue.class);
        super.addTab(TAB, panel);
        super.addTab(TAB2, m_filterpanel);
    }

    /*
     * Generates the radio buttons and text fields
     */
    private JPanel generateContent() {
        JPanel panel = new JPanel();
        // min-max
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayout(2, 3));
        m_minmaxButton = new JRadioButton("Min-Max Normalization");
        m_minmaxButton.setSelected(true);
        JLabel nmin = new JLabel("Min: ");
        JPanel spanel1 = new JPanel();
        spanel1.setLayout(new BorderLayout());
        spanel1.add(nmin, BorderLayout.EAST);

        m_newminTextField = new JTextField(2);
        JPanel nminpanel = new JPanel();
        nminpanel.setLayout(new BorderLayout());
        nminpanel.add(m_newminTextField, BorderLayout.WEST);

        JLabel nmax = new JLabel("Max: ");
        JPanel spanel2 = new JPanel();
        spanel2.setLayout(new BorderLayout());
        spanel2.add(nmax, BorderLayout.EAST);

        m_newmaxTextField = new JTextField(2);
        JPanel nmaxpanel = new JPanel();
        nmaxpanel.setLayout(new BorderLayout());
        nmaxpanel.add(m_newmaxTextField, BorderLayout.WEST);

        panel1.add(m_minmaxButton);
        panel1.add(spanel1);
        panel1.add(nminpanel);
        panel1.add(new JLabel());
        panel1.add(spanel2);
        panel1.add(nmaxpanel);

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

        BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bl);

        panel.add(panel1);
        panel.add(panel2);
        panel.add(panel3);
        return panel;
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_spec = specs[0];
        if (settings.containsKey(NormalizeNodeModel.MODE_KEY)) {
            try {
                int mode = settings.getInt(NormalizeNodeModel.MODE_KEY);
                switch (mode) {
                case NormalizeNodeModel.MINMAX_MODE:
                    m_minmaxButton.setSelected(true);
                    break;
                case NormalizeNodeModel.ZSCORE_MODE:
                    m_zscoreButton.setSelected(true);
                    break;
                case NormalizeNodeModel.DECIMALSCALING_MODE:
                    m_decButton.setSelected(true);
                    break;
                default:
                    throw new InvalidSettingsException("" + "INVALID MODE");
                }
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        if (settings.containsKey(NormalizeNodeModel.NEWMIN_KEY)) {
            try {
                double nmin = settings.getDouble(NormalizeNodeModel.NEWMIN_KEY);
                m_newminTextField.setText("" + nmin);
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        if (settings.containsKey(NormalizeNodeModel.NEWMAX_KEY)) {
            try {
                double nmax = settings.getDouble(NormalizeNodeModel.NEWMAX_KEY);
                m_newmaxTextField.setText("" + nmax);
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }

        String[] cols = new String[0];
        if (settings.containsKey(NormalizeNodeModel.COLUMNS_KEY)) {
            try {
                cols = settings.getStringArray(NormalizeNodeModel.COLUMNS_KEY);
                m_filterpanel.update(m_spec, false, cols);
            } catch (InvalidSettingsException e) {
                LOGGER.debug("Invalid Settings", e);
            }
        }
        m_filterpanel.update(m_spec, false, cols);

    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        int mode = -1;
        if (m_minmaxButton.isSelected()) {
            mode = NormalizeNodeModel.MINMAX_MODE;
        }
        if (m_zscoreButton.isSelected()) {
            mode = NormalizeNodeModel.ZSCORE_MODE;
        }
        if (m_decButton.isSelected()) {
            mode = NormalizeNodeModel.DECIMALSCALING_MODE;
        }
        settings.addInt(NormalizeNodeModel.MODE_KEY, mode);
        String newminString = m_newminTextField.getText();
        double newminD = Double.parseDouble(newminString);
        settings.addDouble(NormalizeNodeModel.NEWMIN_KEY, newminD);

        String newmaxString = m_newmaxTextField.getText();
        double newmaxD = Double.parseDouble(newmaxString);
        settings.addDouble(NormalizeNodeModel.NEWMAX_KEY, newmaxD);

        Set<String> inclset = m_filterpanel.getIncludedColumnSet();
        String[] columns = inclset.toArray(new String[inclset.size()]);
        settings.addStringArray(NormalizeNodeModel.COLUMNS_KEY, columns);
    }
}
