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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.05.2011 (meinl): created
 */
package org.knime.core.node.util;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

/**
 * This component provides multiple file selection based on the
 * {@link FilesHistoryPanel}. It lets the user selected an arbitrary number of
 * files/URLs. An empty component is always shown at the end of the list for
 * entering new locations.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class MultipleURLList extends JScrollPane {
    private static class MyFilePanel extends JPanel {
        private final List<ActionListener> m_removeListeners =
                new ArrayList<ActionListener>();

        private final List<ActionListener> m_upListeners =
                new ArrayList<ActionListener>();

        private final List<ActionListener> m_downListeners =
                new ArrayList<ActionListener>();

        private final List<ActionListener> m_selectListeners =
                new ArrayList<ActionListener>();

        private final FilesHistoryPanel m_filesPanel;

        final JButton m_remove = new JButton(new ImageIcon(
                MultipleURLList.class.getResource("trash.png")));

        final JButton m_up = new JButton(new ImageIcon(
                MultipleURLList.class.getResource("arrow_up.png")));

        final JButton m_down = new JButton(new ImageIcon(
                MultipleURLList.class.getResource("arrow_down.png")));

        public MyFilePanel(final String historyId,
                final boolean markIfNonexisting, final int mode, final String... suffixes) {
            super(new GridBagLayout());

            m_filesPanel = new FilesHistoryPanel(historyId, suffixes);
            m_filesPanel.setSelectMode(mode);
            m_filesPanel.setMarkIfNonexisting(markIfNonexisting);

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;

            add(m_filesPanel, c);
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;

            Dimension buttonDim = new Dimension(22, 22);
            c.anchor = GridBagConstraints.CENTER;
            c.insets = new Insets(0, 6, 0, 0);
            c.gridx++;
            m_remove.setPreferredSize(buttonDim);
            m_remove.setMaximumSize(buttonDim);
            add(m_remove, c);
            c.insets = new Insets(0, 2, 0, 0);
            c.gridx++;
            m_up.setPreferredSize(buttonDim);
            m_up.setMaximumSize(buttonDim);
            add(m_up, c);
            c.gridx++;
            m_down.setPreferredSize(buttonDim);
            m_down.setMaximumSize(buttonDim);
            add(m_down, c);

            m_filesPanel.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent e) {
                    ActionEvent e2 = new ActionEvent(MyFilePanel.this, 0, "");
                    for (ActionListener al : m_selectListeners) {
                        al.actionPerformed(e2);
                    }
                }
            });

            m_remove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    ActionEvent e2 = new ActionEvent(MyFilePanel.this, 0, "");
                    for (ActionListener al : m_removeListeners) {
                        al.actionPerformed(e2);
                    }
                }
            });

            m_up.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    ActionEvent e2 = new ActionEvent(MyFilePanel.this, 0, "");
                    for (ActionListener al : m_upListeners) {
                        al.actionPerformed(e2);
                    }
                }
            });

            m_down.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    ActionEvent e2 = new ActionEvent(MyFilePanel.this, 0, "");
                    for (ActionListener al : m_downListeners) {
                        al.actionPerformed(e2);
                    }
                }
            });

        }

        public String getSelectedFile() {
            return m_filesPanel.getSelectedFile();
        }

        public void setSelectedFile(final URL u) {
            if (u != null) {
                String decUrl = u.toString();
                try {
                    decUrl = URIUtil.decode(decUrl, "UTF-8");
                } catch (URIException ex) {
                    // ignore it
                }
                m_filesPanel.setSelectedFile(decUrl);
            } else {
                m_filesPanel.setSelectedFile(null);
            }
        }

        public void addRemoveListener(final ActionListener l) {
            m_removeListeners.add(l);
        }

        public void addUpListener(final ActionListener l) {
            m_upListeners.add(l);
        }

        public void addDownListener(final ActionListener l) {
            m_downListeners.add(l);
        }

        public void addSelectListener(final ActionListener l) {
            m_selectListeners.add(l);
        }
    }

    private final List<MyFilePanel> m_filePanels = new ArrayList<MyFilePanel>();

    private final String m_historyId;

    private final String[] m_suffixes;

    private int m_selectionMode = JFileChooser.FILES_ONLY;

    private final ActionListener m_removeListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            MyFilePanel fp = (MyFilePanel)e.getSource();
            m_filePanels.remove(fp);
            update();
        }
    };

    private final ActionListener m_upListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            MyFilePanel fp = (MyFilePanel)e.getSource();
            int index = m_filePanels.indexOf(fp);
            if (index > 0) {
                MyFilePanel old = m_filePanels.set(index - 1, fp);
                m_filePanels.set(index, old);
                update();
            }
        }
    };

    private final ActionListener m_downListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            MyFilePanel fp = (MyFilePanel)e.getSource();
            int index = m_filePanels.indexOf(fp);
            if (index < m_filePanels.size() - 1) {
                MyFilePanel old = m_filePanels.set(index + 1, fp);
                m_filePanels.set(index, old);
                update();
            }
        }
    };

    private final ActionListener m_selectListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            if (!m_filePanels.isEmpty() && (m_filePanels.get(m_filePanels.size() - 1).getSelectedFile().length() > 0)) {
                m_filePanels.add(createFilePanel());
                update();
            }
        }
    };

    private final boolean m_markIfNonexisting;

    /**
     * Creates a new URL list.
     *
     * @param historyId the history id for the file selection boxes
     * @param markIfNonexisting <code>true</code> if the combo boxes for
     *            non-existing files (not remote URLs!) should be colored
     * @param suffixes a list of suffixes that should be shown in the file
     *            chooser dialogs
     */
    public MultipleURLList(final String historyId,
            final boolean markIfNonexisting, final String... suffixes) {
        m_historyId = historyId;
        m_suffixes = suffixes;
        m_markIfNonexisting = markIfNonexisting;
        m_filePanels.add(createFilePanel());
        update();
    }

    private void update() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;

        int i = 0;
        while (i < m_filePanels.size()) {
            if (m_filePanels.get(i).getSelectedFile().length() == 0) {
                m_filePanels.remove(i);
            } else {
                i++;
            }
        }
        m_filePanels.add(createFilePanel());

        for (i = 0; i < m_filePanels.size(); i++) {
            MyFilePanel fp = m_filePanels.get(i);
            fp.m_remove.setEnabled(!m_filePanels.isEmpty() && (fp.getSelectedFile().length() > 0));
            fp.m_up.setEnabled((i > 0) && (i < m_filePanels.size() - 1));
            fp.m_down.setEnabled(i < m_filePanels.size() - 2);
            p.add(fp, c);
            c.gridy++;
        }

        JPanel sep = new JPanel();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        p.add(sep, c);
        setViewportView(p);
    }

    /**
     * Returns a list with all selected URLs. Empty file selection panels are
     * ignored. The list may contain invalid URLs the user has entered.
     *
     * @return a list with URLs
     */
    public List<String> getSelectedURLs() {
        List<String> urls = new ArrayList<String>();

        // it may happen that swing events which lead to a modification of
        // m_filePanels arrive during the loop and subsequently cause
        // ConcurrentModificationExceptions; therefore the list is copied
        for (MyFilePanel fp : new ArrayList<MyFilePanel>(m_filePanels)) {
            if (fp.getSelectedFile().length() > 0) {
                URL u = null;
                String encUrl = fp.getSelectedFile();
                try {
                    encUrl = URIUtil.encodePath(encUrl, "UTF-8");
                    u = new URL(encUrl);
                } catch (MalformedURLException ex) {
                    try {
                        u = new URL("file:" + encUrl);
                        encUrl = "file:" + encUrl;
                        fp.setSelectedFile(u);
                    } catch (MalformedURLException ex1) {
                        // ignore it
                    }
                } catch (URIException ex) {
                    // ignore it
                }
                if (u != null) {
                    urls.add(encUrl);
                }
            }
        }
        return urls;
    }

    /**
     * Sets the list of selected URLs that should be shown.
     *
     * @param urls a collection of {@link URL}s
     */
    public void setSelectedURLs(final Collection<URL> urls) {
        m_filePanels.clear();
        for (URL u : urls) {
            MyFilePanel fp = createFilePanel();
            fp.setSelectedFile(u);
            m_filePanels.add(fp);
        }
        m_filePanels.add(createFilePanel());
        update();
    }

    /**
     * Sets the select mode for the file chooser dialog.
     *
     * @param mode one of {@link JFileChooser#FILES_ONLY}, {@link JFileChooser#DIRECTORIES_ONLY}, or
     *            {@link JFileChooser#FILES_AND_DIRECTORIES}
     *
     * @see JFileChooser#setFileSelectionMode(int)
     * @since 2.8
     */
    public void setSelectionMode(final int mode) {
        m_selectionMode = mode;
        for (MyFilePanel p : m_filePanels) {
            p.m_filesPanel.setSelectMode(mode);
        }
    }

    private MyFilePanel createFilePanel() {
        MyFilePanel fp = new MyFilePanel(m_historyId, m_markIfNonexisting, m_selectionMode, m_suffixes);
        fp.setSelectedFile(null);
        fp.addRemoveListener(m_removeListener);
        fp.addUpListener(m_upListener);
        fp.addDownListener(m_downListener);
        fp.addSelectListener(m_selectListener);
        return fp;
    }

    /**
     * Creates an URL from the "file"name entered in the dialog.
     *
     * @param url the file's name, may already be an URL
     * @return an URL
     * @throws MalformedURLException if the URL is malformed
     */
    public static URL convertToUrl(final String url)
            throws MalformedURLException {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            File f = new File(url);
            if (f.exists()) {
                return f.toURI().toURL();
            } else {
                throw ex;
            }
        }
    }
}
