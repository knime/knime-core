/*
 * ------------------------------------------------------------------------
 *
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
 *   Sep 21, 2016 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 *
 * Panel used to define additional SQL statement
 *
 * @author Budi Yanto, KNIME.com
 */
public class AdditionalSQLStatementPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** the sql editor */
    private final RSyntaxTextArea m_editor = createEditor();

    /** the clear button */
    private final JButton m_clear = new JButton("Clear");

    /** the configuration */
    private final DBTableCreatorConfiguration m_config;

    /**
     * Creates a new instance of AdditionalSQLStatementPanel
     *
     * @param config config to store all of the settings
     */
    public AdditionalSQLStatementPanel(final DBTableCreatorConfiguration config) {
        m_config = config;
        setLayout(new BorderLayout());
        final JLabel editorLabel = new JLabel("SQL Statement");
        editorLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        final RTextScrollPane editorScrollPane = new RTextScrollPane(m_editor);
        editorScrollPane.setFoldIndicatorEnabled(true);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(m_clear);
        m_clear.addActionListener(new ClearButtonListener());

        add(editorLabel, BorderLayout.NORTH);
        add(editorScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Create and initialize an SQL syntax editor
     * @return the newly created editor
     */
    private static RSyntaxTextArea createEditor(){
        final RSyntaxTextArea editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        editor.setAutoIndentEnabled(true);
        editor.setFadeCurrentLineHighlight(true);
        editor.setHighlightCurrentLine(true);
        editor.setLineWrap(false);
        editor.setRoundedSelectionEdges(true);
        editor.setBorder(new EtchedBorder());
        editor.setTabSize(4);
        return editor;
    }

    /**
     * Actions invoked during save
     */
    public void onSave() {
        m_config.setAdditionalOptions(m_editor.getText());
    }

    /**
     * Actions invoked during load
     */
    public void onLoad() {
        m_editor.setText(m_config.getAdditionalOptions());
    }

    /**
     * Clear button listener
     */
    private class ClearButtonListener implements ActionListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void actionPerformed(final ActionEvent e) {
            m_editor.setText("");
            m_editor.requestFocus();
        }

    }

}
