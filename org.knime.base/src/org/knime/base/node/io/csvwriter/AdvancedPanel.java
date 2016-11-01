/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 9, 2007 (ohl): created
 */
package org.knime.base.node.io.csvwriter;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.node.io.csvwriter.FileWriterSettings.LineEnding;

/**
 *
 * @author ohl, University of Konstanz
 */
class AdvancedPanel extends JPanel {

    private static final Dimension TEXTFIELDDIM = new Dimension(75, 25);

    private JTextField m_colSeparator = new JTextField("");

    private JTextField m_missValuePattern = new JTextField("");

    private ButtonModel m_defEnding;

    private ButtonModel m_lfEnding;

    private ButtonModel m_crlfEnding;

    private ButtonModel m_crEnding;

    private ButtonGroup m_bGroup;

    /**
     *
     */
    public AdvancedPanel() {

        JPanel missPanel = new JPanel();
        missPanel.setLayout(new BoxLayout(missPanel, BoxLayout.X_AXIS));
        missPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Missing Value Pattern"));
        missPanel.add(new JLabel("Pattern written out for missing values:"));
        missPanel.add(Box.createHorizontalStrut(5));
        missPanel.add(m_missValuePattern);
        m_missValuePattern.setPreferredSize(TEXTFIELDDIM);
        m_missValuePattern.setMaximumSize(TEXTFIELDDIM);
        missPanel.add(Box.createHorizontalGlue());
        missPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, TEXTFIELDDIM.height));

        JPanel colSepPanel = new JPanel();
        colSepPanel.setLayout(new BoxLayout(colSepPanel, BoxLayout.X_AXIS));
        colSepPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Data Separator"));
        colSepPanel.add(new JLabel("Pattern written out between data values:"));
        colSepPanel.add(Box.createHorizontalStrut(5));
        colSepPanel.add(m_colSeparator);
        m_colSeparator.setPreferredSize(TEXTFIELDDIM);
        m_colSeparator.setMaximumSize(TEXTFIELDDIM);
        m_colSeparator.setToolTipText("Use \\t or \\n "
                + "for a tab or newline character.");
        colSepPanel.add(Box.createHorizontalGlue());
        colSepPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, TEXTFIELDDIM.height));

        JPanel lineSep = new JPanel();
        lineSep.setLayout(new BoxLayout(lineSep, BoxLayout.Y_AXIS));
        lineSep.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Line Endings"));
        Box defBox = Box.createHorizontalBox();
        defBox.add(Box.createHorizontalStrut(10));
        JRadioButton def = new JRadioButton("System default (operating system dependant)");
        defBox.add(def);
        defBox.add(Box.createHorizontalGlue());
        defBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, def.getMaximumSize().height));
        Box lfBox = Box.createHorizontalBox();
        lfBox.add(Box.createHorizontalStrut(10));
        JRadioButton lf = new JRadioButton("LF line endings (Linux/Unix style)");
        lfBox.add(lf);
        lfBox.add(Box.createHorizontalGlue());
        lfBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, lf.getMaximumSize().height));
        Box crlfBox = Box.createHorizontalBox();
        crlfBox.add(Box.createHorizontalStrut(10));
        JRadioButton crlf = new JRadioButton("CR+LF line endings (Windows/DOS style)");
        crlfBox.add(crlf);
        crlfBox.add(Box.createHorizontalGlue());
        crlfBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, crlf.getMaximumSize().height));
        Box crBox = Box.createHorizontalBox();
        crBox.add(Box.createHorizontalStrut(10));
        JRadioButton cr = new JRadioButton("CR only");
        crBox.add(cr);
        crBox.add(Box.createHorizontalGlue());
        crBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, cr.getMaximumSize().height));

        m_defEnding = def.getModel();
        m_lfEnding = lf.getModel();
        m_crlfEnding = crlf.getModel();
        m_crEnding = cr.getModel();

        m_bGroup = new ButtonGroup();
        m_bGroup.add(def);
        m_bGroup.add(lf);
        m_bGroup.add(crlf);
        m_bGroup.add(cr);
        m_bGroup.setSelected(m_defEnding, true);

        lineSep.add(defBox);
        lineSep.add(Box.createVerticalStrut(10));
        lineSep.add(lfBox);
        lineSep.add(Box.createVerticalStrut(10));
        lineSep.add(crlfBox);
        lineSep.add(Box.createVerticalStrut(10));
        lineSep.add(crBox);
        lineSep.add(Box.createVerticalGlue());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(colSepPanel);
        add(Box.createVerticalStrut(10));
        add(missPanel);
        add(Box.createVerticalStrut(10));
        add(lineSep);
    }

    /**
     * Reads new values from the specified object and puts them into the panel's
     * components.
     *
     * @param settings object holding the new values to show.
     */
    void loadValuesIntoPanel(final FileWriterSettings settings) {

        // support \t and \n
        String colSep = settings.getColSeparator();
        colSep = FileWriterSettings.escapeString(colSep);

        m_colSeparator.setText(colSep);
        m_missValuePattern.setText(settings.getMissValuePattern());

        LineEnding leMode = settings.getLineEndingMode();
        switch (leMode) {
            case SYST:
                m_bGroup.setSelected(m_defEnding, true);
                break;
            case LF:
                m_bGroup.setSelected(m_lfEnding, true);
                break;
            case CRLF:
                m_bGroup.setSelected(m_crlfEnding, true);
                break;
            case CR:
                m_bGroup.setSelected(m_crEnding, true);
                break;
        }
    }

    /**
     * Writes the current values from the components into the settings object.
     *
     * @param settings the object to write the values into
     */
    void saveValuesFromPanelInto(final FileWriterSettings settings) {

        // support \t and \n
        String colSep = m_colSeparator.getText();
        colSep = FileWriterSettings.unescapeString(colSep);

        settings.setColSeparator(colSep);
        settings.setMissValuePattern(m_missValuePattern.getText());

        ButtonModel lf = m_bGroup.getSelection();
        LineEnding mode;
        if (lf == m_defEnding) {
            mode = LineEnding.SYST;
        } else if (lf == m_lfEnding) {
            mode = LineEnding.LF;
        } else if (lf == m_crlfEnding) {
            mode = LineEnding.CRLF;
        } else if (lf == m_crEnding) {
            mode = LineEnding.CR;
        } else {
            mode = LineEnding.SYST;
        }
        settings.setLineEndingMode(mode);
    }

}
