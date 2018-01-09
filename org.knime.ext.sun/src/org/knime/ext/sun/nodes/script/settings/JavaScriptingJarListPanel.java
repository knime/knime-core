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
 */
package org.knime.ext.sun.nodes.script.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.SimpleFileFilter;

/**
 * List of jars required for compilation (separate tab).
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class JavaScriptingJarListPanel extends JPanel {

    private final JList m_addJarList;

    private JFileChooser m_jarFileChooser;

    /** Inits GUI. */
    public JavaScriptingJarListPanel() {
        super(new BorderLayout());
        m_addJarList = new JList(new DefaultListModel()) {
            /** {@inheritDoc} */
            @Override
            protected void processComponentKeyEvent(final KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
                    int end = getModel().getSize() - 1;
                    getSelectionModel().setSelectionInterval(0, end);
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    onJarRemove();
                }
            }
        };
        m_addJarList.setCellRenderer(new ConvenientComboBoxRenderer());
        add(new JScrollPane(m_addJarList), BorderLayout.CENTER);
        JPanel southP = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton addButton = new JButton("Add...");
        addButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                onJarAdd();
            }
        });
        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                onJarRemove();
            }
        });
        m_addJarList.addListSelectionListener(new ListSelectionListener() {
            /** {@inheritDoc} */
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                removeButton.setEnabled(!m_addJarList.isSelectionEmpty());
            }
        });
        removeButton.setEnabled(!m_addJarList.isSelectionEmpty());
        southP.add(addButton);
        southP.add(removeButton);
        add(southP, BorderLayout.SOUTH);

        JPanel northP = new JPanel(new FlowLayout());
        JLabel label = new JLabel("<html><body>Specify additional jar files "
                + "that are necessary for the snippet to run</body></html>");
        northP.add(label);
        add(northP, BorderLayout.NORTH);
    }

    private void onJarAdd() {
        DefaultListModel model = (DefaultListModel)m_addJarList.getModel();
        Set<Object> hash = new HashSet<Object>();
        for (Enumeration<?> e = model.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        StringHistory history =
            StringHistory.getInstance("java_snippet_jar_dirs");
        if (m_jarFileChooser == null) {
            File dir = null;
            for (String h : history.getHistory()) {
                File temp = new File(h);
                if (temp.isDirectory()) {
                    dir = temp;
                    break;
                }
            }
            m_jarFileChooser = new JFileChooser(dir);
            m_jarFileChooser.setFileFilter(
                    new SimpleFileFilter(".zip", ".jar"));
            m_jarFileChooser.setMultiSelectionEnabled(true);
        }
        int result = m_jarFileChooser.showDialog(m_addJarList, "Select");

        if (result == JFileChooser.APPROVE_OPTION) {
            for (File f : m_jarFileChooser.getSelectedFiles()) {
                String s = f.getAbsolutePath();
                if (hash.add(s)) {
                    model.addElement(s);
                }
            }
            history.add(
                    m_jarFileChooser.getCurrentDirectory().getAbsolutePath());
        }
    }

    private void onJarRemove() {
        DefaultListModel model = (DefaultListModel)m_addJarList.getModel();
        int[] sels = m_addJarList.getSelectedIndices();
        int last = Integer.MAX_VALUE;
        // traverse backwards (editing list in loop body)
        for (int i = sels.length - 1; i >= 0; i--) {
            assert sels[i] < last : "Selection list not ordered";
            model.remove(sels[i]);
        }
    }

    /** Restore settings.
     * @param s To load from.
     */
    public void loadSettingsFrom(final JavaScriptingSettings s) {
        DefaultListModel jarListModel =
            (DefaultListModel)m_addJarList.getModel();
        jarListModel.removeAllElements();
        String[] files = s.getJarFiles();
        for (String f : files) {
            try {
                File file = JavaScriptingSettings.toFile(f);
                jarListModel.addElement(file.getAbsolutePath());
            } catch (InvalidSettingsException e) {
                // ignore
            }
        }
    }

    /** Save current settings.
     * @param s To save to.
     */
    public void saveSettingsTo(final JavaScriptingSettings s) {
        DefaultListModel jarListModel =
            (DefaultListModel)m_addJarList.getModel();
        if (jarListModel.getSize() > 0) {
            String[] copy = new String[jarListModel.getSize()];
            jarListModel.copyInto(copy);
            s.setJarFiles(copy);
        }
    }

}
