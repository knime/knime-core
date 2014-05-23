/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * Created on Oct 4, 2013 by Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.util.filter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.util.filter.PatternFilterConfigurationImpl.PatternFilterType;

/**
 * Filters based on the given regular expression or wildcard pattern.
 *
 * @param <T> The type of object that this filter is filtering
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
@SuppressWarnings("serial")
final class PatternFilterPanelImpl<T> extends JPanel {

    /** Border title for exclude list. */
    private static final String NON_MATCH_LABEL = "Mismatch (Exclude)";

    /** Border title for include list. */
    private static final String MATCH_LABEL = "Match (Include)";

    private static final Dimension SMALL_LIST = new Dimension(150, 120);

    private static final Dimension NORMAL_LIST = new Dimension(150, 170);

    private JTextField m_pattern;

    private JRadioButton m_regex;

    private JRadioButton m_wildcard;

    private JCheckBox m_caseSensitive;

    private JLabel m_invalid;

    private List<ChangeListener> m_listeners;

    private String m_patternValue;

    private PatternFilterType m_typeValue;

    private boolean m_caseSensitiveValue;

    private NameFilterPanel<T> m_parentFilter;

    private String[] m_names = new String[0];

    private InputFilter<T> m_filter;

    private FilterIncludeExcludePreview<T> m_preview;

    /**
     * Create the pattern filter panel.
     *
     * @param parentFilter The filter that is parent to this pattern filter
     * @param filter The filter that filters out Ts that are not available for selection
     */
    @SuppressWarnings("unchecked")
    PatternFilterPanelImpl(final NameFilterPanel<T> parentFilter, final InputFilter<T> filter) {
        setLayout(new BorderLayout());
        m_parentFilter = parentFilter;
        m_filter = filter;
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        m_pattern = new JTextField();
        m_pattern.setText("");
        m_patternValue = m_pattern.getText();
        JPanel patternPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        patternPanel.add(new JLabel("Pattern:"), gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 5, 0, 0);
        patternPanel.add(m_pattern, gbc);
        gbc.gridx = 0;
        panel.add(patternPanel, gbc);
        ButtonGroup typeGroup = new ButtonGroup();
        m_wildcard = new JRadioButton("Wildcard ('?' matches any character, '*' matches a sequence of any characters)");
        typeGroup.add(m_wildcard);
        gbc.gridy++;
        gbc.insets = new Insets(0, 20, 0, 0);
        panel.add(m_wildcard, gbc);
        m_regex = new JRadioButton("Regular expression");
        typeGroup.add(m_regex);
        gbc.gridy++;
        panel.add(m_regex, gbc);
        m_wildcard.setSelected(true);
        m_typeValue = getSelectedFilterType();
        m_caseSensitive = new JCheckBox("Case Sensitive");
        gbc.gridy++;
        panel.add(m_caseSensitive, gbc);
        m_caseSensitive.setSelected(true);
        m_caseSensitiveValue = m_caseSensitive.isSelected();
        m_pattern.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(final CaretEvent e) {
                if (!m_patternValue.equals(m_pattern.getText())) {
                    m_patternValue = m_pattern.getText();
                    fireFilteringChangedEvent();
                }
            }
        });
        m_wildcard.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                PatternFilterType newType = getSelectedFilterType();
                if (newType != null && m_typeValue != newType) {
                    m_typeValue = newType;
                    fireFilteringChangedEvent();
                }
            }
        });
        m_regex.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                PatternFilterType newType = getSelectedFilterType();
                if (newType != null && m_typeValue != newType) {
                    m_typeValue = newType;
                    fireFilteringChangedEvent();
                }
            }
        });
        m_caseSensitive.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (m_caseSensitiveValue != m_caseSensitive.isSelected()) {
                    m_caseSensitiveValue = m_caseSensitive.isSelected();
                    fireFilteringChangedEvent();
                }
            }
        });
        // Add preview twin list
        m_preview =
            new FilterIncludeExcludePreview<T>(MATCH_LABEL, NON_MATCH_LABEL, m_parentFilter.getListCellRenderer());
        m_preview.setListSize(NORMAL_LIST);
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        panel.add(m_preview, gbc);
        // Add invalid pattern label
        m_invalid = new JLabel();
        m_invalid.setForeground(Color.RED);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        panel.add(m_invalid, gbc);
        super.add(panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_pattern.setEnabled(enabled);
        m_regex.setEnabled(enabled);
        m_wildcard.setEnabled(enabled);
        m_caseSensitive.setEnabled(enabled);
        update();
    }

    /** @param config to load from
     * @param names the available names that will be shown in the selection preview */
    void loadConfiguration(final PatternFilterConfigurationImpl config, final String[] names) {
        m_names = names;
        m_pattern.setText(config.getPattern());
        if (config.getType().equals(PatternFilterType.Regex)) {
            m_regex.doClick();
        } else if (config.getType().equals(PatternFilterType.Wildcard)) {
            m_wildcard.doClick();
        }
        m_caseSensitive.setSelected(config.isCaseSensitive());
        update();
    }

    /** @param config to save to */
    void saveConfiguration(final PatternFilterConfigurationImpl config) {
        config.setPattern(m_pattern.getText());
        if (m_regex.isSelected()) {
            config.setType(PatternFilterType.Regex);
        } else if (m_wildcard.isSelected()) {
            config.setType(PatternFilterType.Wildcard);
        }
        config.setCaseSensitive(m_caseSensitive.isSelected());
    }

    /**
     * Adds a listener which gets informed whenever the filtering changes.
     *
     * @param listener the listener
     */
    public void addChangeListener(final ChangeListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList<ChangeListener>();
        }
        m_listeners.add(listener);
    }

    /**
     * Removes the given listener from this filter panel.
     *
     * @param listener the listener.
     */
    public void removeChangeListener(final ChangeListener listener) {
        if (m_listeners != null) {
            m_listeners.remove(listener);
        }
    }

    private void fireFilteringChangedEvent() {
        update();
        if (m_listeners != null) {
            for (ChangeListener listener : m_listeners) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }

    private PatternFilterType getSelectedFilterType() {
        if (m_regex.isSelected()) {
            return PatternFilterType.Regex;
        }
        if (m_wildcard.isSelected()) {
            return PatternFilterType.Wildcard;
        }
        return null;
    }

    /**
     * Updates the preview lists and the error message.
     */
    private void update() {
        List<T> includes = new ArrayList<T>();
        List<T> excludes = new ArrayList<T>();
        boolean patternInvalid = false;
        try {
            // Create regex, this will throw an exception if the current pattern is invalid
            Pattern regex = PatternFilterConfigurationImpl.compilePattern(m_patternValue, m_typeValue,
                m_caseSensitiveValue);
            // Fill lists
            for (String name : m_names) {
                T t = m_parentFilter.getTforName(name);
                // Skip Ts that are filtered out
                if (m_filter != null) {
                    if (!m_filter.include(t)) {
                        continue;
                    }
                }
                if (regex.matcher(name).matches()) {
                    includes.add(t);
                } else {
                    excludes.add(t);
                }
            }
        } catch (PatternSyntaxException e) {
            // Build HTML message for label, replacing newlines with line break '<br>' and spaces with non breaking
            // spaces '&nbsp;'
            String htmlMessage =
                "<html><div style=\"font-family: monospace\">"
                    + e.getMessage().replace("\n", "<br>").replace(" ", "&nbsp;") + "</div></html>";
            m_invalid.setText(htmlMessage);
            patternInvalid = true;
            // Put all Ts into excludes (if not filtered out)
            for (String name : m_names) {
                T t = m_parentFilter.getTforName(name);
                if (m_filter != null) {
                    if (!m_filter.include(t)) {
                        continue;
                    }
                }
                excludes.add(t);
            }
        }
        m_preview.update(includes, excludes);
        // Show error if pattern was invalid
        m_invalid.setVisible(patternInvalid);
        // Disable twin lists if pattern was invalid
        m_preview.setEnabled(!patternInvalid && super.isEnabled());
        // Change size to make place for the error message
        if (patternInvalid) {
            m_preview.setListSize(SMALL_LIST);
        } else {
            m_preview.setListSize(NORMAL_LIST);
        }
    }

}
