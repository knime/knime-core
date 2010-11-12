/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *   05.08.2010 (hofer): created
 */
package org.knime.base.node.io.tablecreator.table;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A swing action to adjust the row ID settings.
 *
 * @author Heiko Hofer
 */
class PropertyRowsAction extends AbstractAction {
    private static final long serialVersionUID = -3036007263997791404L;
    private RowHeaderTable m_table;

    /**
     * Creates a new instance.
     *
     * @param table the 'model' for this action
     */
    PropertyRowsAction(final RowHeaderTable table) {
        super("Row ID Properties...");
        m_table = table;

        m_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                setEnabled(!m_table.getSelectionModel().isSelectionEmpty());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent e) {
        MyPanel panel = new MyPanel(
                m_table.getRowModel().getRowIdPrefix()
                , m_table.getRowModel().getRowIdSuffix()
                , m_table.getRowModel().getRowIdStartValue());

        Frame parent = (Frame)SwingUtilities.getAncestorOfClass(
                Frame.class, m_table);

        OkCancelDialog dialog = new OkCancelDialog(parent,
                "Row Properties", panel);
        dialog.setVisible(true);

        if (!dialog.isCanceled()) {
            m_table.getRowModel().setRowIdPrefix(panel.getRowIdPrefix());
            m_table.getRowModel().setRowIdSuffix(panel.getRowIdSuffix());
            m_table.getRowModel().setRowIdStartValue(
                    panel.getRowIdStartValue());
        }
    }

    private static class MyPanel extends JPanel {
        private static final long serialVersionUID = 3367270705637831649L;
        private JTextField m_rowIdPrefix;
        private JTextField m_rowIdSuffix;
        private JSpinner m_rowIdStartValue;
        /**
         * @param rowIdPrefix
         * @param rowIdSuffix
         * @param rowIdStartValue
         */
        MyPanel(final String rowIdPrefix,
                final String rowIdSuffix,
                final int rowIdStartValue) {
            super(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            m_rowIdPrefix = new JTextField(rowIdPrefix);
            m_rowIdSuffix = new JTextField(rowIdSuffix);
            m_rowIdStartValue = new JSpinner(
                    new SpinnerNumberModel(5, 0, Integer.MAX_VALUE, 1));
            m_rowIdStartValue.setValue(rowIdStartValue);

            c.insets = new Insets(2, 2, 2, 2);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 0;
            add(new JLabel("Prefix of row IDs: "), c);
            c.gridx++;
            c.weightx = 1.0;
            add(m_rowIdPrefix, c);
            c.gridx = 0;
            c.gridy++;
            add(new JLabel("Suffix of row IDs: "), c);
            c.gridx++;
            c.weightx = 1.0;
            add(m_rowIdSuffix, c);
            c.gridx = 0;
            c.gridy++;
            add(new JLabel("Start counting with: "), c);
            c.gridx++;
            c.weightx = 1.0;
            add(m_rowIdStartValue, c);
        }
        /**
         * @return the rowIdPrefix
         */
        public final String getRowIdPrefix() {
            return m_rowIdPrefix.getText();
        }
        /**
         * @return the rowIdSuffix
         */
        public final String getRowIdSuffix() {
            return m_rowIdSuffix.getText();
        }
        /**
         * @return the rowIdStartValue
         */
        public final Integer getRowIdStartValue() {
            return (Integer)m_rowIdStartValue.getValue();
        }


    }

    /**
     * A generic OK-Cancel button with a custom view area.
     *
     * @author Heiko Hofer
     */
    private static class OkCancelDialog extends JDialog {
        private static final long serialVersionUID = -83691452814849618L;
        private boolean m_canceled = true;

        public OkCancelDialog(final Frame frame, final String title,
                final JComponent comp) {
            super(frame, title, true);

            getContentPane().setLayout(new BorderLayout());
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            JButton okButton = new JButton("Ok");
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    cancel(false);
                }
            });
            buttonPanel.add(okButton);
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    cancel(true);
                }
            });
            buttonPanel.add(cancelButton);
            getContentPane().add(buttonPanel, BorderLayout.SOUTH);

            getContentPane().add(comp, BorderLayout.CENTER);

            KeyStroke k = KeyStroke.getKeyStroke(
                    java.awt.event.KeyEvent.VK_ESCAPE, 0);
            Object actionKey = "cancel"; // NOI18N
            getRootPane().getInputMap(
                    JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                            k, actionKey);

            Action cancelAction = new AbstractAction() {
                    public void actionPerformed(final ActionEvent ev) {
                        cancel(true);
                    }
                };

            getRootPane().getActionMap().put(actionKey, cancelAction);
            addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent ev) {
                        m_canceled = true;
                    }
                }
            );
            pack();

            // center dialog on screen
            Dimension dim = getToolkit().getScreenSize();
            Rectangle abounds = getBounds();
            setLocation((dim.width - abounds.width) / 2,
                (dim.height - abounds.height) / 2);
        }

        private void cancel(final boolean canceled) {
            m_canceled = canceled;
            setVisible(false);
        }

        public boolean isCanceled() {
            return m_canceled;
        }
    }

}
