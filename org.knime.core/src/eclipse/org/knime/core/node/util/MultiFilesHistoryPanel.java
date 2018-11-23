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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;

/**
 * This component provides multiple file selection based on the {@link FilesHistoryPanel}. It lets the user selected an
 * arbitrary number of files/URLs. An empty component is always shown at the end of the list for entering new locations.
 *
 * @author Ferry Abt, KNIME AG, Zürich
 */
public class MultiFilesHistoryPanel extends JScrollPane {
    private class FilesHistoryPanelWrapper extends JPanel {
        private final FilesHistoryPanel m_filesPanel;

        final JButton m_remove = new JButton(SharedIcons.DELETE_TRASH.get());

        final JButton m_up = new JButton(SharedIcons.MOVE_UP.get());

        final JButton m_down = new JButton(SharedIcons.MOVE_DOWN.get());

        private FilesHistoryPanelWrapper() {
            super(new GridBagLayout());
            m_filesPanel = new FilesHistoryPanel(m_historyId, m_suffixes);
            m_filesPanel.setSelectMode(m_selectionMode);
            m_filesPanel.setMarkIfNonexisting(m_markIfNonexisting);

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
            m_filesPanel.setSelectedFile(null);
            m_remove.addActionListener(m_removeListener);
            m_up.addActionListener(m_upListener);
            m_down.addActionListener(m_downListener);
            m_filesPanel.addChangeListener(m_selectListener);
        }

        private String getSelectedFile() {
            return m_filesPanel.getSelectedFile();
        }

        private void setSelectedFile(final String url) {
            m_filesPanel.setSelectedFile(url);
        }

        private boolean isEmpty() {
            return m_filesPanel.getSelectedFile().length() == 0;
        }

        @Override
        public void setEnabled(final boolean enabled) {
            super.setEnabled(enabled);
            m_filesPanel.setEnabled(enabled);
        }
    }

    private final List<FilesHistoryPanelWrapper> m_filePanels = new ArrayList<FilesHistoryPanelWrapper>();

    private final String m_historyId;

    private final String[] m_suffixes;

    private int m_selectionMode = JFileChooser.FILES_ONLY;

    private final ActionListener m_removeListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            JButton source = (JButton)e.getSource();
            FilesHistoryPanelWrapper fp = (FilesHistoryPanelWrapper)source.getParent();
            m_filePanels.remove(fp);
            update();
        }
    };

    private final ActionListener m_upListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            JButton source = (JButton)e.getSource();
            FilesHistoryPanelWrapper fp = (FilesHistoryPanelWrapper)source.getParent();
            int index = m_filePanels.indexOf(fp);
            if (index > 0) {
                FilesHistoryPanelWrapper old = m_filePanels.set(index - 1, fp);
                m_filePanels.set(index, old);
                update();
            }
        }
    };

    private final ActionListener m_downListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            JButton source = (JButton)e.getSource();
            FilesHistoryPanelWrapper fp = (FilesHistoryPanelWrapper)source.getParent();
            int index = m_filePanels.indexOf(fp);
            if (index < m_filePanels.size() - 1) {
                FilesHistoryPanelWrapper old = m_filePanels.set(index + 1, fp);
                m_filePanels.set(index, old);
                update();
            }
        }
    };

    private final ChangeListener m_selectListener = new ChangeListener() {
        @Override
        public void stateChanged(final ChangeEvent e) {
            if (!m_filePanels.isEmpty() && !m_filePanels.get(m_filePanels.size() - 1).isEmpty()) {
                m_filePanels.add(new FilesHistoryPanelWrapper());
                update();
                if (!m_variableReplacementEnabled) {
                    m_filePanels.get(m_filePanels.size() - 2).m_filesPanel.requestFocus();
                }
            }
        }
    };

    private FlowVariableModelButton m_flowVariableButton;

    private boolean m_variableReplacementEnabled = false;

    private final boolean m_markIfNonexisting;

    /**
     * Creates a new URL list.
     *
     * @param historyId the history id for the file selection boxes
     * @param markIfNonexisting <code>true</code> if the combo boxes for non-existing files (not remote URLs!) should be
     *            colored
     * @param fvm
     * @param suffixes a list of suffixes that should be shown in the file chooser dialogs
     */
    public MultiFilesHistoryPanel(final String historyId, final boolean markIfNonexisting, final FlowVariableModel fvm,
        final String... suffixes) {
        m_historyId = historyId;
        m_suffixes = suffixes;
        m_markIfNonexisting = markIfNonexisting;
        m_filePanels.add(new FilesHistoryPanelWrapper());
        if (fvm != null) {
            m_flowVariableButton = new FlowVariableModelButton(fvm);
            fvm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                    FlowVariableModel wvm = (FlowVariableModel)(evt.getSource());
                    m_variableReplacementEnabled = wvm.isVariableReplacementEnabled();
                    if (m_variableReplacementEnabled) {
                        // if the location is overwritten by a variable show its value
                        if (wvm.getVariableValue().isPresent()) {
                            String[] urlStrings = wvm.getVariableValue().get().getStringValue().split(";");
                            setSelectedFiles(Arrays.asList(urlStrings));
                        }
                    }
                    update();
                }
            });
        }
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
            if (m_filePanels.get(i).isEmpty()) {
                m_filePanels.remove(i);
            } else {
                m_filePanels.get(i).setEnabled(!m_variableReplacementEnabled);
                i++;
            }
        }
        if (!m_variableReplacementEnabled) {
            m_filePanels.add(new FilesHistoryPanelWrapper());
        }

        for (i = 0; i < m_filePanels.size(); i++) {
            FilesHistoryPanelWrapper fp = m_filePanels.get(i);
            fp.m_remove.setEnabled(!m_variableReplacementEnabled && !m_filePanels.isEmpty() && !fp.isEmpty());
            fp.m_up.setEnabled(!m_variableReplacementEnabled && (i > 0) && (i < m_filePanels.size() - 1));
            fp.m_down.setEnabled(!m_variableReplacementEnabled && i < m_filePanels.size() - 2);
            p.add(fp, c);
            c.gridy++;
        }
        if (m_flowVariableButton != null) {
            JPanel fvbPanel = new JPanel();
            fvbPanel.add(m_flowVariableButton);
            p.add(fvbPanel, c);
            c.gridy++;
        }
        JPanel sep = new JPanel();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        p.add(sep, c);
        setViewportView(p);
    }

    /**
     * Returns a list with all selected URLs. Empty file selection panels are ignored. The list may contain invalid URLs
     * the user has entered.
     *
     * @return a list with URLs
     */
    public List<String> getSelectedFiles() {
        List<String> urls = new ArrayList<String>();
        for (FilesHistoryPanelWrapper fp : new ArrayList<FilesHistoryPanelWrapper>(m_filePanels)) {
            if (!fp.isEmpty()) {
                urls.add(fp.getSelectedFile());
            }
        }
        return urls;
    }

    /**
     * Sets the list of selected URLs that should be shown.
     *
     * @param urls a collection of {@link URL}s
     */
    public void setSelectedFiles(final Collection<String> urls) {
        m_filePanels.clear();
        for (String s : urls) {
            FilesHistoryPanelWrapper fp = new FilesHistoryPanelWrapper();
            fp.setSelectedFile(s);
            m_filePanels.add(fp);
        }
        m_filePanels.add(new FilesHistoryPanelWrapper());
        update();
    }

    /**
     * Sets the list of selected URLs that should be shown.
     *
     * @param urls a collection of {@link URL}s
     */
    public void setSelectedURLs(final Collection<URL> urls) {
        m_filePanels.clear();
        for (URL u : urls) {
            if (u != null) {
                String decUrl = u.toString();
                try {
                    decUrl = URIUtil.decode(decUrl, "UTF-8");
                } catch (URIException ex) {
                    // ignore it
                }
                FilesHistoryPanelWrapper fp = new FilesHistoryPanelWrapper();
                fp.setSelectedFile(decUrl);
                m_filePanels.add(fp);
            }
        }
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
        for (FilesHistoryPanelWrapper p : m_filePanels) {
            p.m_filesPanel.setSelectMode(mode);
        }
    }

}
