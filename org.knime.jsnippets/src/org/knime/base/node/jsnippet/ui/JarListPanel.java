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
package org.knime.base.node.jsnippet.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListDataListener;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.jsnippet.util.JavaSnippetUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.FileUtil;
import org.knime.core.util.SimpleFileFilter;

/**
 * List of jars required for compilation (separate tab).
 * <p>
 * This class might change and is not meant as public API.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("serial")
public class JarListPanel extends JPanel {

    private final JButton m_addJarFilesButton = new JButton("Add File(s)...");

    private final JButton m_addJarURLsButton = new JButton("Add KNIME URL...");

    private final JButton m_removeButton = new JButton("Remove");

    private JFileChooser m_jarFileChooser;

    private String[] m_filesCache;

    private DefaultListModel<JarListEntry> m_listModel = new DefaultListModel<>();

    private final JList<JarListEntry> m_addJarList = new JList<JarListEntry>(m_listModel) {
        @Override
        protected void processComponentKeyEvent(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
                // Ctrl+A => Select All
                final int end = getModel().getSize() - 1;
                getSelectionModel().setSelectionInterval(0, end);
            } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                // Remove selection on DEL key.
                onJarRemove();
            }
        }
    };

    /**
     * Wrapper around a String with some metadata to display to the user (e.g. whether the JarFile was found)
     *
     * @author Jonathan Hale
     */
    private static class JarListEntry {
        String m_jarFilename;

        boolean m_exists = true;

        /**
         * Constructor
         *
         * @param filename Filename of the jar
         */
        public JarListEntry(final String filename) {
            m_jarFilename = filename;
        }

        /** Whether the jar file was not found and should produce a warning. */
        public void setExists(final boolean b) {
            m_exists = b;
        }

        /**
         * Whether the jar file was not found and should produce a warning.
         *
         * @return true if this jar file was not found
         */
        public boolean exists() {
            return m_exists;
        }

        @Override
        public String toString() {
            return m_jarFilename;
        }

        @Override
        public int hashCode() {
            return m_jarFilename.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return m_jarFilename.equals(obj);
        }

        /**
         * @return Filename of the jar file
         */
        public String getFilename() {
            return m_jarFilename;
        }
    }

    class JarListCellRenderer extends ConvenientComboBoxRenderer {
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
            final boolean isSelected, final boolean cellHasFocus) {
            final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            /* Mark not found jar files RED and set tooltip to inform the user */
            if (value instanceof JarListEntry) {
                if (!((JarListEntry)value).exists()) {
                    c.setForeground(Color.RED);
                    if (c instanceof JComponent) {
                        ((JComponent)c).setToolTipText(value.toString() + " was not found.");
                    }
                }
            }

            return c;
        }
    }

    /** Inits GUI. */
    public JarListPanel() {
        super(new BorderLayout());

        m_addJarList.setCellRenderer(new JarListCellRenderer());

        add(new JScrollPane(m_addJarList), BorderLayout.CENTER);

        m_addJarURLsButton.setToolTipText("Add 'knime' URLs that resolve to local paths");

        /* Set action listeners */
        m_addJarFilesButton.addActionListener(e -> onJarFileAdd());
        m_addJarURLsButton.addActionListener(e -> onJarURLAdd());
        m_removeButton.addActionListener(e -> onJarRemove());

        /* Enable remove button only if item is selected */
        m_removeButton.setEnabled(!m_addJarList.isSelectionEmpty());
        m_addJarList.addListSelectionListener(e -> m_removeButton.setEnabled(!m_addJarList.isSelectionEmpty()));

        final JPanel southP = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        southP.add(m_addJarFilesButton);
        southP.add(m_addJarURLsButton);
        southP.add(m_removeButton);
        add(southP, BorderLayout.SOUTH);

        final JPanel northP = new JPanel(new FlowLayout());
        northP.add(new JLabel("Specify additional jar files that are necessary for the snippet to run"));
        add(northP, BorderLayout.NORTH);
    }

    private void onJarFileAdd() {
        final Set<JarListEntry> hash = new HashSet<>(Collections.list(m_listModel.elements()));
        final StringHistory history = StringHistory.getInstance("java_snippet_jar_dirs");
        if (m_jarFileChooser == null) {
            File dir = null;
            for (final String h : history.getHistory()) {
                final File temp = new File(h);
                if (temp.isDirectory()) {
                    dir = temp;
                    break;
                }
            }
            m_jarFileChooser = new JFileChooser(dir);
            m_jarFileChooser.setFileFilter(new SimpleFileFilter(".zip", ".jar"));
            m_jarFileChooser.setMultiSelectionEnabled(true);
        }

        final int result = m_jarFileChooser.showDialog(m_addJarList, "Select");
        if (result == JFileChooser.APPROVE_OPTION) {
            for (final File f : m_jarFileChooser.getSelectedFiles()) {
                final String s = f.getAbsolutePath();
                final JarListEntry e = new JarListEntry(s);
                if (hash.add(e)) {
                    m_listModel.addElement(e);
                }
            }
            history.add(m_jarFileChooser.getCurrentDirectory().getAbsolutePath());
        }
    }

    @SuppressWarnings("null")
    private void onJarURLAdd() {
        final Set<JarListEntry> hash = new HashSet<>(Collections.list(m_listModel.elements()));
        String input = "knime://knime.workflow/example.jar";

        boolean valid = false;
        while (!valid) {
            input = JOptionPane.showInputDialog(this, "Enter a \"knime:\" URL to a JAR file", input);
            if (StringUtils.isEmpty(input)) {
                valid = true;
            } else {
                try {
                    final URL url = new URL(input);
                    final File file = FileUtil.getFileFromURL(url);
                    CheckUtils.checkArgument(file != null, "Could not resolve to local file");
                    CheckUtils.checkArgument(file.exists(), "File does not exists; location resolved to\n%s",
                        file.getAbsolutePath());
                    valid = true;
                } catch (MalformedURLException | IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(this, "Invalid URL\n" + e.getMessage(), "Error parsing URL",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        final JarListEntry e = new JarListEntry(input);
        if (!StringUtils.isEmpty(input) && hash.add(e)) {
            m_listModel.addElement(e);
        }
    }

    private void onJarRemove() {
        final int[] sels = m_addJarList.getSelectedIndices();
        final int last = Integer.MAX_VALUE;
        // traverse backwards (editing list in loop body)
        for (int i = sels.length - 1; i >= 0; i--) {
            assert sels[i] < last : "Selection list not ordered";
            m_listModel.remove(sels[i]);
        }
    }

    /**
     * Adds a listener to the list that's notified each time a change to the list of jar files occurs.
     *
     * @param l the <code>ListDataListener</code> to be added
     */
    public void addListDataListener(final ListDataListener l) {
        m_listModel.addListDataListener(l);
    }

    /**
     * Set the files to display.
     *
     * @param files the files
     */
    public void setJarFiles(final String[] files) {
        if (m_filesCache == null || !Arrays.deepEquals(m_filesCache, files)) {
            m_filesCache = files;

            m_listModel.removeAllElements();
            for (final String f : files) {
                final JarListEntry e = new JarListEntry(f);
                try {
                    JavaSnippetUtil.toFile(f); // validate existence etc.
                } catch (InvalidSettingsException ex) {
                    e.setExists(false);
                }
                m_listModel.addElement(e);
            }
        }
    }

    /**
     * Get the jar files defined in this panel.
     *
     * @return the jar files
     */
    public String[] getJarFiles() {
        final String[] copy = new String[m_listModel.getSize()];
        for (int i = 0; i < m_listModel.getSize(); ++i) {
            copy[i] = m_listModel.get(i).getFilename();
        }
        return copy;
    }

    /**
     * Sets whether or not this component is enabled. A component that is enabled may respond to user input, while a
     * component that is not enabled cannot respond to user input.
     *
     * @param enabled true if this component should be enabled, false otherwise
     */
    @Override
    public void setEnabled(final boolean enabled) {
        if (isEnabled() != enabled) {
            m_addJarList.setEnabled(enabled);
            m_addJarFilesButton.setEnabled(enabled);
            m_addJarURLsButton.setEnabled(enabled);
            m_removeButton.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }
}
