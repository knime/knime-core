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
 *   24.04.2014 (thor): created
 */
package org.knime.core.node.port.database;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringEscapeUtils;
import org.knime.core.node.ModelContentRO;

/**
 * The database connection port view.
 */
@SuppressWarnings("serial")
final class DatabaseConnectionView extends JPanel {
    /**
     * @param sett the {@link ModelContentRO} with the settings
     */
    DatabaseConnectionView(final ModelContentRO sett) {
        super(new GridBagLayout());
        super.setName("Connection");
        StringBuilder buf = new StringBuilder("<html><body>");
//        buf.append("<h2>Database Connection</h2>");
        buf.append("<strong>Database Driver:</strong>&nbsp;&nbsp;");
        buf.append("<tt>" + sett.getString("driver", "") + "</tt>");
        buf.append("<br/><br/>");
        buf.append("<strong>Database URL:</strong><br/>");
        buf.append("<tt>" + sett.getString("database", "") + "</tt>");
        buf.append("<br/><br/>");
        boolean useCredential = sett.containsKey("credential_name");
        if (useCredential) {
            String credName = sett.getString("credential_name", "");
            buf.append("<strong>Credential Name:</strong>&nbsp;&nbsp;");
            buf.append("<tt>" + credName + "</tt>");
        } else {
            buf.append("<strong>User Name:</strong>&nbsp;&nbsp;");
            buf.append("<tt>" + sett.getString("user", "") + "</tt>");
        }
        final String dbIdentifier = sett.getString("databaseIdentifier", null);
        if (dbIdentifier != null) {
            buf.append("<br/><br/>");
            buf.append("<strong>Database Identifier:</strong>&nbsp;&nbsp;");
            buf.append("<tt>" + dbIdentifier + "</tt>");
        }
        final String sql = sett.getString("statement", null);
        if (sql != null) {
            buf.append("<br/><br/>");
            buf.append("<strong>SQL Statement:</strong><br/>");
            final String query = StringEscapeUtils.escapeHtml4(sql);
            buf.append("<tt>" + query + "</tt>");
        }
        buf.append("</body></html>");
        final JTextPane textArea = new JTextPane();
        textArea.setContentType("text/html");
        textArea.setEditable(false);
        textArea.setText(buf.toString());
        textArea.setCaretPosition(0);
        final JScrollPane jsp = new JScrollPane(textArea);
        jsp.setPreferredSize(new Dimension(300, 300));
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        super.add(jsp, c);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        final JButton sqlButton = new JButton("Copy SQL to clipboard");
        sqlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    final StringSelection stringSelection = new StringSelection(sql);
                    final Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clpbrd.setContents(stringSelection, null);
                } catch (Throwable ex) {
                    // catch any exception that might occur
                }
            }
        });
        c.gridy++;
        c.anchor = GridBagConstraints.LINE_END;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(5, 0, 5, 5);
        if (sql != null) {
            super.add(sqlButton, c);
        }
    }
}
