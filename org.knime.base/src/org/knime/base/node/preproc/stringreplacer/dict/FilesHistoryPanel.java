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
 *   Dec 17, 2005 (wiswedel): created
 */
package org.knime.base.node.preproc.stringreplacer.dict;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import org.knime.core.node.util.StringHistory;
import org.knime.core.util.SimpleFileFilter;


/**
 * Panel that contains an editable Combo Box showing the file to read from and a
 * button to trigger a file chooser. The elements in the combo are files that
 * have been recently used.
 * 
 * <p>This file may move to a different package (utility class in core) when
 * we decide that people benefit from it. So far, we do not recommend
 * to use this class elsewhere as it is subject to change.
 * 
 * @see org.knime.core.node.util.StringHistory
 * @author Bernd Wiswedel, University of Konstanz
 */
final class FilesHistoryPanel extends JPanel {

    private final JComboBox m_textBox;

    private final JButton m_chooseButton;
    
    private final String[] m_suffixes;
    
    private final String m_historyID;

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     * @param historyID Identifier for the string history, 
     *        see {@link StringHistory}.
     * @param suffixes The set of suffixes for the file chooser.
     */
    public FilesHistoryPanel(
            final String historyID, final String...  suffixes) {
        if (historyID == null || suffixes == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        if (Arrays.asList(suffixes).contains(null)) {
            throw new NullPointerException("Array must not contain null.");
        }
        m_historyID = historyID;
        m_suffixes = suffixes;
        m_textBox = new JComboBox(new DefaultComboBoxModel());
        m_textBox.setEditable(true);
        m_textBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
//        m_textBox.setPreferredSize(new Dimension(300, 25));
        m_textBox.setRenderer(new MyComboBoxRenderer());
        m_chooseButton = new JButton("Browse...");
        m_chooseButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                String newFile = getOutputFileName();
                if (newFile != null) {
                    m_textBox.setSelectedItem(newFile);
                }
            }
        });
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(m_textBox);
        add(m_chooseButton);
        updateHistory();
    }

    private String getOutputFileName() {
        // file chooser triggered by choose button
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        if (m_suffixes.length > 0) {
            fileChooser.setFileFilter(new SimpleFileFilter(m_suffixes));
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        String f = m_textBox.getEditor().getItem().toString();
        File dirOrFile = getFile(f);
        if (dirOrFile.isDirectory()) {
            fileChooser.setCurrentDirectory(dirOrFile);
        } else {
            fileChooser.setSelectedFile(dirOrFile);
        }
        int r = fileChooser.showDialog(FilesHistoryPanel.this, "Save");
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists() && file.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Error: Please specify "
                        + "a file, not a directory.");
                return null;
            }
            return file.getAbsolutePath();
        }
        return null;
    }

    /**
     * Get currently selected file.
     * 
     * @return the current file url
     * @see javax.swing.JComboBox#getSelectedItem()
     */
    public String getSelectedFile() {
        return m_textBox.getEditor().getItem().toString();
    }

    /**
     * Set the file url as default.
     * 
     * @param url the file to choose
     * @see javax.swing.JComboBox#setSelectedItem(java.lang.Object)
     */
    public void setSelectedFile(final String url) {
        m_textBox.setSelectedItem(url);
    }

    /** Updates the elements in the combo box, reads the file history. */
    public void updateHistory() {
        StringHistory history = StringHistory.getInstance(m_historyID);
        String[] allVals = history.getHistory();
        LinkedHashSet<String> list = new LinkedHashSet<String>();
        for (int i = 0; i < allVals.length; i++) {
            try {
                String cur = allVals[i];
                File file = textToFile(cur);
                list.add(file.getAbsolutePath());
            } catch (MalformedURLException mue) {
                continue;
            }
        }
        DefaultComboBoxModel comboModel = (DefaultComboBoxModel)m_textBox
                .getModel();
        comboModel.removeAllElements();
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            comboModel.addElement(it.next());
        }
        // changing the model will also change the minimum size to be
        // quite big. We have tooltips, we don't need that
        Dimension newMin = new Dimension(0, getPreferredSize().height);
        setMinimumSize(newMin);
    }
    
    /** Adds the specified file to the history. Does nothing if the file is
     * null or does not exist.
     * @param file File to add to history.
     */
    public void addToHistory(final File file) {
        if (file != null && file.exists()) {
            StringHistory.getInstance(m_historyID).add(file.getAbsolutePath());
        }
    }

    /**
     * Tries to create a File from the passed string.
     * 
     * @param url the string to transform into an File
     * @return File if entered value could be properly transformed
     * @throws MalformedURLException if the value passed was invalid
     */
    private static File textToFile(final String url)
            throws MalformedURLException {
        if ((url == null) || (url.equals(""))) {
            throw new MalformedURLException("Specify a not empty valid URL");
        }

        String file;
        try {
            URL newURL = new URL(url);
            file = newURL.getFile();
        } catch (MalformedURLException e) {
            // see if they specified a file without giving the protocol
            return new File(url);
        }
        if (file == null || file.equals("")) {
            throw new MalformedURLException("Can't get file from file '" + url
                    + "'");
        }
        return new File(file);
    }

    /**
     * Return a file object for the given fileName. It makes sure that if the
     * fileName is not absolute it will be relative to the user's home dir.
     * 
     * @param fileName the file name to convert to a file
     * @return a file representing fileName
     */
    public static final File getFile(final String fileName) {
        File f = new File(fileName);
        if (!f.isAbsolute()) {
            f = new File(new File(System.getProperty("user.home")), fileName);
        }
        return f;
    }

    /** renderer that also supports to show customized tooltip. */
    private static class MyComboBoxRenderer extends BasicComboBoxRenderer {
        /**
         * @see BasicComboBoxRenderer#getListCellRendererComponent(
         *      javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        @Override
        public Component getListCellRendererComponent(final JList list,
                final Object value, final int index, final boolean isSelected,
                final boolean cellHasFocus) {
            if (index > -1) {
                list.setToolTipText(value.toString());
            }
            return super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
        }
    }
}
