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
 */
package org.knime.base.node.jsnippet.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
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
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.base.node.jsnippet.JavaSnippetUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.SimpleFileFilter;

/**
 * List of jars required for compilation (separate tab).
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
@SuppressWarnings("serial")
public class JarListPanel extends JPanel {

    private final JList m_addJarList;
    private JButton m_addButton;
    private JButton m_removeButton;

    private JFileChooser m_jarFileChooser;
    private String[] m_filesCache;

    /** Inits GUI. */
    public JarListPanel() {
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
        m_addButton = new JButton("Add...");
        m_addButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                onJarAdd();
            }
        });
        m_removeButton = new JButton("Remove");
        m_removeButton.addActionListener(new ActionListener() {
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
                m_removeButton.setEnabled(!m_addJarList.isSelectionEmpty());
            }
        });
        m_removeButton.setEnabled(!m_addJarList.isSelectionEmpty());
        southP.add(m_addButton);
        southP.add(m_removeButton);
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

    /**
     * Adds a listener to the list that's notified each time a change
     * to the list of jar files occurs.
     * @param l the <code>ListDataListener</code> to be added
     */
    public void addListDataListener(final ListDataListener l) {
        m_addJarList.getModel().addListDataListener(l);
    }

    /**
     * Set the files to display.
     * @param files the files
     */
    public void setJarFiles(final String[] files) {
        boolean doUpdate = false;
        if (m_filesCache == null) {
            m_filesCache = files;
            doUpdate = true;
        } else {
            if (!Arrays.deepEquals(m_filesCache, files)) {
                m_filesCache = files;
                doUpdate = true;
            }
        }
        if (doUpdate) {
            DefaultListModel jarListModel =
                (DefaultListModel)m_addJarList.getModel();
            jarListModel.removeAllElements();
            for (String f : files) {
                try {
                    File file = JavaSnippetUtil.toFile(f);
                    jarListModel.addElement(file.getAbsolutePath());
                } catch (InvalidSettingsException e) {
                    // ignore
                }
            }
        }
    }


    /**
     * Get the jar files defined in this panel.
     * @return the jar files
     */
    public String[] getJarFiles() {
        DefaultListModel jarListModel =
            (DefaultListModel)m_addJarList.getModel();
        String[] copy = new String[jarListModel.getSize()];
        if (jarListModel.getSize() > 0) {
            jarListModel.copyInto(copy);
        }
        return copy;
    }

    /**
     * Sets whether or not this component is enabled.
     * A component that is enabled may respond to user input,
     * while a component that is not enabled cannot respond to
     * user input.
     * @param enabled true if this component should be enabled, false otherwise
     */
    @Override
    public void setEnabled(final boolean enabled) {
        if (isEnabled() != enabled) {
            m_addJarList.setEnabled(enabled);
            m_addButton.setEnabled(enabled);
            m_removeButton.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

}
