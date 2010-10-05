/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   08.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
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

    private final JLabel m_errText;

    /**
     * Creates a panel containing controls to adjust settings for a row number
     * range filter.
     */
    RowNoRowFilterPanel() {
        super(400, 350);

        m_errText = new JLabel("");
        m_errText.setForeground(Color.RED);
        m_first = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE,
                10));
        m_first.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateErrText();
            }
        });
        m_tilEOT = new JCheckBox("to the end of the table");
        m_tilEOT.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                tilEOTChanged();
            }
        });
        m_last = new JSpinner(new SpinnerNumberModel(1000, 1,
                Integer.MAX_VALUE, 10));
        m_last.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                updateErrText();
            }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Row number range:"));

        Box labelBox = Box.createVerticalBox();
        labelBox.add(Box.createHorizontalStrut(300));
        Box textBox = Box.createHorizontalBox();
        textBox.add(Box.createHorizontalGlue());
        labelBox.add(textBox);

        Box firstBox = Box.createHorizontalBox();
        firstBox.add(new JLabel("first row number:"));
        firstBox.add(m_first);
        firstBox.add(Box.createHorizontalGlue());

        Box eotBox = Box.createHorizontalBox();
        eotBox.add(m_tilEOT);
        eotBox.add(Box.createHorizontalGlue());

        Box lastBox = Box.createHorizontalBox();
        lastBox.add(new JLabel("last row number:"));
        lastBox.add(m_last);
        lastBox.add(Box.createHorizontalGlue());

        Box errBox = Box.createHorizontalBox();
        errBox.add(m_errText);
        errBox.add(Box.createHorizontalGlue());

        panel.add(labelBox);
        panel.add(firstBox);
        panel.add(eotBox);
        panel.add(lastBox);
        panel.add(Box.createVerticalStrut(7));
        panel.add(errBox);
        panel.add(Box.createVerticalGlue()); // do we need some glue here?!?

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
    public void loadSettingsFromFilter(final RowFilter filter)
            throws InvalidSettingsException {
        if (!(filter instanceof RowNoRowFilter)) {
            throw new InvalidSettingsException("Range filter can only load "
                    + "settings from a RowNumberFilter");
        }

        RowNoRowFilter rowNumberFilter = (RowNoRowFilter)filter;
        // do some consistency checks
        int first = rowNumberFilter.getFirstRow();
        int last = rowNumberFilter.getLastRow();
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
        m_first.setValue(new Integer(first + 1));
        m_tilEOT.setSelected(last == RowNoRowFilter.EOT); // en/disables
        // m_last
        if (last != RowNoRowFilter.EOT) {
            m_last.setValue(new Integer(last + 1));
        }
        updateErrText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowFilter createFilter(final boolean include)
            throws InvalidSettingsException {
        // just in case, because the err text is the indicator for err existence
        updateErrText();

        if (hasErrors()) {
            throw new InvalidSettingsException(m_errText.getText());
        }

        int start = readIntSpinner(m_first) - 1;
        int last = RowNoRowFilter.EOT;
        if (!m_tilEOT.isSelected()) {
            last = readIntSpinner(m_last) - 1;
        }
        return new RowNoRowFilter(start, last, include);
    }

    /*
     * sets a message in the error label if settings are not valid
     */
    private void updateErrText() {
        int first = readIntSpinner(m_first);
        int last = readIntSpinner(m_last);

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
     * read the current value from the spinner assuming it contains Integers
     */
    private int readIntSpinner(final JSpinner intSpinner) {
        try {
            intSpinner.commitEdit();
        } catch (ParseException e) {
            // if the spinner has the focus, the currently edited value
            // might not be commited. Now it is!
        }
        SpinnerNumberModel snm = (SpinnerNumberModel)intSpinner.getModel();
        return snm.getNumber().intValue();
    }
}
