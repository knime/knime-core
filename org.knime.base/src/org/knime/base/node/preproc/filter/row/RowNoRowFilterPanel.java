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
 * -------------------------------------------------------------------
 *
 * History
 *   08.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowNoRowFilter;
import org.knime.core.node.InvalidSettingsException;

/**
 *
 * @author Peter Ohl, University of Konstanz
 */
public class RowNoRowFilterPanel extends RowFilterPanel {
    private final JSpinner m_first;

    private final JCheckBox m_tilEOT;

    private final JSpinner m_last;

    private final JTextArea m_errText;

    /**
     * Creates a panel containing controls to adjust settings for a row number
     * range filter.
     */
    RowNoRowFilterPanel() {
        super(400, 350);

        m_errText = new JTextArea();
        m_errText.setEditable(false);
        m_errText.setLineWrap(true);
        m_errText.setWrapStyleWord(true);
        m_errText.setBackground(getBackground());
        m_errText.setFont(new Font(m_errText.getFont().getName(), Font.BOLD, m_errText.getFont().getSize()));
        m_errText.setMinimumSize(new Dimension(350, 50));
        m_errText.setMaximumSize(new Dimension(350, 100));
        m_errText.setForeground(Color.RED);

        m_first = new JSpinner(new SpinnerNumberModel(1L, (Comparable) Long.valueOf(1L), Long.MAX_VALUE, 1L));
        m_first.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateErrText();
            }
        });
        m_tilEOT = new JCheckBox("to the end of the table");
        m_tilEOT.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                tilEOTChanged();
            }
        });
        m_last = new JSpinner(new SpinnerNumberModel(1000L, (Comparable) Long.valueOf(1L), Long.MAX_VALUE, 1L));
        m_last.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateErrText();
            }
        });
        m_first.setMaximumSize(new Dimension(6022, 25));
        m_first.setMinimumSize(new Dimension(100, 25));
        m_first.setPreferredSize(new Dimension(100, 25));

        m_last.setMaximumSize(new Dimension(6022, 25));
        m_last.setMinimumSize(new Dimension(100, 25));
        m_last.setPreferredSize(new Dimension(100, 25));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Row number range"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.WEST;

        panel.add(new JLabel("First row number   "), c);
        c.gridx = 1;
        panel.add(m_first, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        panel.add(m_tilEOT, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        panel.add(new JLabel("Last row number   "), c);
        c.gridx = 1;
        panel.add(m_last, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(m_errText, c);

        this.add(panel);
    }

    /**
     * Sets the enabled status of the 'lastRow' spinner depending on the checked
     * status of the 'until the end' box.
     */
    protected void tilEOTChanged() {
        m_last.setEnabled(!m_tilEOT.isSelected());
        updateErrText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFromFilter(final IRowFilter filter) throws InvalidSettingsException {
        if (!(filter instanceof RowNoRowFilter)) {
            throw new InvalidSettingsException("Range filter can only load "
                    + "settings from a RowNumberFilter");
        }

        RowNoRowFilter rowNumberFilter = (RowNoRowFilter)filter;
        // do some consistency checks
        long first = rowNumberFilter.getFirstRow();
        long last = rowNumberFilter.getLastRow();
        if (first < 0) {
            throw new InvalidSettingsException("The RowNumberFilter range "
                    + "cannot start at a row number less than 1.");
        }
        if ((last != RowNoRowFilter.EOT) && (last < first)) {
            throw new InvalidSettingsException("The end of the RowNumberFilter"
                    + " range must be greater than the start.");
        }

        // the filter contains index values (starting from 0)
        // the spinner show the numbers, so we need to add 1 here.
        m_first.setValue(Long.valueOf(first + 1));
        m_tilEOT.setSelected(last == RowNoRowFilter.EOT); // en/disables
        // m_last
        if (last != RowNoRowFilter.EOT) {
            m_last.setValue(Long.valueOf(last + 1));
        }
        updateErrText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRowFilter createFilter(final boolean include) throws InvalidSettingsException {
        // just in case, because the err text is the indicator for err existence
        updateErrText();

        if (hasErrors()) {
            throw new InvalidSettingsException(m_errText.getText());
        }

        long start = readSpinnerValue(m_first) - 1;
        long last = RowNoRowFilter.EOT;
        if (!m_tilEOT.isSelected()) {
            last = readSpinnerValue(m_last) - 1;
        }
        return new RowNoRowFilter(start, last, include);
    }

    /*
     * sets a message in the error label if settings are not valid
     */
    private void updateErrText() {
        long first = readSpinnerValue(m_first);
        long last = readSpinnerValue(m_last);

        m_errText.setText("");

        if (first < 1) {
            m_errText.setText("The first row number of the range"
                    + " can't be smaller than 1.");
        }
        if ((!m_tilEOT.isSelected()) && (last < first)) {
            m_errText.setText("The row number range"
                    + " end must be larger than the start.");
        }

    }

    /**
     * @return <code>true</code> if the settings in the panel are invalid,
     *         <code>false</code> if they are consistent and usable
     */
    public boolean hasErrors() {
        return m_errText.getText().length() > 0;
    }

    /**
     * @return a message to the user if hasErrors returns true
     */
    public String getErrMsg() {
        return m_errText.getText();
    }

    /*
     * read the current value from the spinner assuming it contains Longs
     */
    private long readSpinnerValue(final JSpinner spinner) {
        try {
            spinner.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)spinner.getModel();
        return snm.getNumber().longValue();
    }
}
