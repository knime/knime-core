/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
package org.knime.core.node.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.util.SimpleFileFilter;

/**
 * Panel that contains an editable Combo Box showing the file to write to and a
 * button to trigger a file chooser. The elements in the combo are files that
 * have been recently used.
 *
 * @see org.knime.core.node.util.StringHistory
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class FilesHistoryPanel extends JPanel {
    private final List<ChangeListener> m_changeListener =
            new ArrayList<ChangeListener>();

    private final JComboBox m_textBox;

    private final JButton m_chooseButton;

    private String[] m_suffixes;

    private final String m_historyID;

    private final JLabel m_warnMsg;

    private int m_selectMode = JFileChooser.FILES_ONLY;

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     *
     * @param historyID identifier for the string history, see
     *            {@link StringHistory}
     * @param suffixes the set of suffixes for the file chooser
     */
    public FilesHistoryPanel(final String historyID, final String... suffixes) {
        this(null, historyID, false, suffixes);
    }

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     *
     * @param historyID identifier for the string history, see
     *            {@link StringHistory}
     * @param suffixes the set of suffixes for the file chooser
     * @param fvm model to allow to use a variable instead of the text field.
     * @param showErrorMessage if true there are error messages if the file
     *            exists or the path is not available and so on.
     */
    public FilesHistoryPanel(final FlowVariableModel fvm,
            final String historyID, final boolean showErrorMessage,
            final String... suffixes) {
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

        m_textBox.setPreferredSize(new Dimension(300, 25));
        m_textBox.setRenderer(new ConvenientComboBoxRenderer());
        m_textBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if ((e.getStateChange() == ItemEvent.SELECTED)
                        && (e.getItem() != null)) {
                    ChangeEvent ev = new ChangeEvent(FilesHistoryPanel.this);
                    for (ChangeListener cl : m_changeListener) {
                        cl.stateChanged(ev);
                    }
                }
            }
        });

        // install listeners to update warn message whenever file name changes
        m_textBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                fileLocationChanged();
            }
        });

        /* install action listeners */
        m_textBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                fileLocationChanged();
            }
        });
        Component editor = m_textBox.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }
            });
        }

        m_chooseButton = new JButton("Browse...");
        m_chooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String newFile = getOutputFileName();
                if (newFile != null) {
                    m_textBox.setSelectedItem(newFile);
                    StringHistory.getInstance(m_historyID).add(newFile);
                    ChangeEvent ev = new ChangeEvent(FilesHistoryPanel.this);
                    for (ChangeListener cl : m_changeListener) {
                        cl.stateChanged(ev);
                    }
                }
            }
        });
        m_warnMsg = new JLabel("");
        // this ensures correct display of the changing label content...
        m_warnMsg.setPreferredSize(new Dimension(350, 25));
        m_warnMsg.setMinimumSize(new Dimension(350, 25));
        m_warnMsg.setForeground(Color.red);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Box fileBox = Box.createHorizontalBox();
        fileBox.add(m_textBox);
        if (fvm != null) {
            fileBox.add(Box.createHorizontalStrut(5));
            fileBox.add(new FlowVariableModelButton(fvm));
            fvm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                    FlowVariableModel wvm =
                            (FlowVariableModel)(evt.getSource());
                    m_textBox.setEnabled(!wvm.isVariableReplacementEnabled());
                    m_chooseButton.setEnabled(!wvm
                            .isVariableReplacementEnabled());
                    fileLocationChanged();
                }
            });

        }
        fileBox.add(Box.createHorizontalStrut(5));
        fileBox.add(m_chooseButton);
        Box warnBox = Box.createHorizontalBox();
        warnBox.add(m_warnMsg);
        warnBox.add(Box.createHorizontalGlue());
        warnBox.add(Box.createVerticalStrut(25));

        add(fileBox);
        if (showErrorMessage) {
            add(warnBox);
        }
        fileLocationChanged();
        updateHistory();
    }

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     *
     * @param historyID identifier for the string history, see
     *            {@link StringHistory}
     * @param showErrorMessage if true there are error messages if the file
     *            exists or the path is not available and so on.
     */
    public FilesHistoryPanel(final String historyID,
            final boolean showErrorMessage) {
        this(null, historyID, showErrorMessage);
    }

    private String getOutputFileName() {
        // file chooser triggered by choose button
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setFileFilter(new SimpleFileFilter(m_suffixes));
        fileChooser.setFileSelectionMode(m_selectMode);

        String f = m_textBox.getEditor().getItem().toString();
        File dirOrFile = getFile(f);
        if (dirOrFile.isDirectory()) {
            fileChooser.setCurrentDirectory(dirOrFile);
        } else {
            fileChooser.setSelectedFile(dirOrFile);
        }
        int r = fileChooser.showDialog(FilesHistoryPanel.this, "OK");
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists() && (m_selectMode == JFileChooser.FILES_ONLY)
                    && file.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Error: Please select "
                        + "a file, not a directory.");
                return null;
            }
            return file.getAbsolutePath();
        }
        return null;
    }

    /**
     * Set file file as part of the suffix.
     *
     * @param suffixes The new list of valid suffixes.
     */
    public void setSuffixes(final String... suffixes) {
        m_suffixes = suffixes;
    }

    /** @return the currently set list of file filter suffixes. */
    public String[] getSuffixes() {
        return m_suffixes;
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
        if (SwingUtilities.isEventDispatchThread()) {
            m_textBox.setSelectedItem(url);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        m_textBox.setSelectedItem(url);

                    }
                });
            } catch (InterruptedException ex) {
                // ignore
            } catch (InvocationTargetException ex) {
                // ignore
            }
        }
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
        DefaultComboBoxModel comboModel =
                (DefaultComboBoxModel)m_textBox.getModel();
        comboModel.removeAllElements();
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            comboModel.addElement(it.next());
        }
        // changing the model will also change the minimum size to be
        // quite big. We have tooltips, we don't need that
        Dimension newMin = new Dimension(0, getPreferredSize().height);
        setMinimumSize(newMin);
    }

    /**
     * Adds a change listener that gets notified if a new file name is entered
     * into the text field.
     *
     * @param cl a change listener
     */
    public void addChangeListener(final ChangeListener cl) {
        m_changeListener.add(cl);
    }

    /**
     * Removes the given change listener from the listener list.
     *
     * @param cl a change listener
     */
    public void removeChangeListener(final ChangeListener cl) {
        m_changeListener.remove(cl);
    }

    /**
     * Sets the select mode for the file chooser dialog.
     *
     * @param mode one of {@link JFileChooser#FILES_ONLY},
     *            {@link JFileChooser#DIRECTORIES_ONLY}, or
     *            {@link JFileChooser#FILES_AND_DIRECTORIES}
     *
     * @see JFileChooser#setFileSelectionMode(int)
     */
    public void setSelectMode(final int mode) {
        m_selectMode = mode;
    }

    /**
     * Tries to create a file from the passed string.
     *
     * @param url the string to transform into a file
     * @return file if entered value could be properly transformed
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

    private void fileLocationChanged() {
        String newMsg = "";
        String selFile = getSelectedFile();
        if (selFile != null && selFile.length() > 0) {
            File newFile = getFile(selFile);
            if (newFile.exists()) {
                if (newFile.isDirectory()) {
                    newMsg = "ERROR: a file can't be a directory";
                } else {
                    newMsg = "Warning: selected file exists.";
                }
            } else {
                if (!newFile.getParentFile().exists()) {
                    newMsg =
                            "Warning: Directory of specified file doesn't exist"
                                    + " and will be created";
                }
            }
        }

        if (newMsg.length() == 0) {
            m_warnMsg.setForeground(m_warnMsg.getBackground());
            m_warnMsg.setText("A long msg to avoid invisibility of label");
        } else {
            m_warnMsg.setForeground(Color.red);
            m_warnMsg.setText(newMsg);
        }

        revalidate();
        repaint();
        invalidate();
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_chooseButton.setEnabled(enabled);
        m_textBox.setEnabled(enabled);
    }

    private final ChangeListener m_checkIfExistsListener =
            new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    // we can only check local files, ignore everything else
                    Component editorComponent =
                        m_textBox.getEditor().getEditorComponent();
                    if (new File(getSelectedFile()).exists()) {
                        editorComponent.setBackground(Color.WHITE);
                        return;
                    }

                    try {
                        URL url = new URL(getSelectedFile());
                        if ("file".equalsIgnoreCase(url.getProtocol())) {
                            File file = new File(url.getPath());
                            if (file.exists()) {
                                editorComponent.setBackground(Color.WHITE);
                                return;
                            } else {
                                // maybe the URL is encoded (e.g. %20 for spaces)
                                // so try to decode it
                                file = new File(URLDecoder.decode(
                                        url.getPath(), "UTF-8"));
                                if (file.exists()) {
                                    editorComponent.setBackground(Color.WHITE);
                                    return;
                                }
                            }
                        } else {
                            editorComponent.setBackground(Color.WHITE);
                            return;
                        }
                    } catch (MalformedURLException ex) {
                        // ignore
                    } catch (UnsupportedEncodingException ex) {
                        // ignore
                    }

                    editorComponent.setBackground(Color.ORANGE);
                }
            };

    /**
     * Sets if the text field should be colored if the selected file does not
     * exist.
     *
     * @param b <code>true</code> if the text field should be colored,
     *            <code>false</code> otherwise
     */
    public void setMarkIfNonexisting(final boolean b) {
        if (b) {
            addChangeListener(m_checkIfExistsListener);
        } else {
            removeChangeListener(m_checkIfExistsListener);
        }
    }
}
