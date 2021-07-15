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
 * Created on Oct 4, 2013 by Patrick Winter, KNIME AG, Zurich, Switzerland
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;

import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.util.filter.PatternFilterConfiguration.PatternFilterType;

/**
 * Filters based on the given regular expression or wildcard pattern.
 *
 * @param <T> The type of object that this filter is filtering
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @since 3.4
 */
@SuppressWarnings("serial")
public class PatternFilterPanel<T> extends JPanel {

    /** Border title for exclude list if mismatches should be excluded. */
    private static final String EXCLUDE_NON_MATCH_LABEL = "Mismatch (Exclude)";

    /** Border title for include list if matches should be included. */
    private static final String INCLUDE_MATCH_LABEL = "Match (Include)";

    /** Border title for exclude list if mismatches should be included. */
    private static final String INCLUDE_NON_MATCH_LABEL = "Mismatch (Include)";

    /** Border title for include list if matches should be excluded. */
    private static final String EXCLUDE_MATCH_LABEL = "Match (Exclude)";

    private static final Dimension SMALL_LIST = new Dimension(150, 120);

    private static final Dimension NORMAL_LIST = new Dimension(150, 170);

    private JTextField m_pattern;

    private JRadioButton m_regex;

    private JRadioButton m_wildcard;

    private JCheckBox m_caseSensitive;

    private JCheckBox m_excludeMatching;

    private JLabel m_invalid;

    private List<ChangeListener> m_listeners; //NOSONAR: KNIME doesn't use Java's serialization

    private String m_patternValue;

    private PatternFilterType m_typeValue;

    private boolean m_caseSensitiveValue;

    private boolean m_excludeMatchingValue;

    private boolean m_additionalCheckBoxValue;

    private NameFilterPanel<T> m_parentFilter;

    private String[] m_names = new String[0];

    private InputFilter<T> m_filter; //NOSONAR: KNIME doesn't use Java's serialization

    private FilterIncludeExcludePreview<T> m_preview;

    /**
     * Additional Checkbox (e.g. "Include Missing Values")
     */
    private JCheckBox m_additionalCheckbox;

    /**
     * Create the pattern filter panel.
     *
     * @param parentFilter The filter that is parent to this pattern filter
     * @param filter The filter that filters out Ts that are not available for selection
     */
    protected PatternFilterPanel(final NameFilterPanel<T> parentFilter, final InputFilter<T> filter) {
        setLayout(new BorderLayout()); // NOSONAR: common in Swing
        m_parentFilter = parentFilter;
        m_filter = filter;
        final var panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        // Pattern label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(4, 0, 0, 8);
        panel.add(new JLabel("Pattern:"), gbc);
        // Pattern TextField
        m_pattern = new JTextField();
        m_pattern.setText("");
        m_patternValue = m_pattern.getText();
        gbc.gridx++;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(4, 0, 0, 0);
        panel.add(m_pattern, gbc);
        // Wildcard RadioButton + image
        final var typeGroup = new ButtonGroup();
        m_wildcard = new JRadioButton("Wildcard", true);
        m_wildcard.setToolTipText("'?' matches any character, '*' matches a sequence of any characters");
        typeGroup.add(m_wildcard);
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(m_wildcard, gbc);
        final var infoLabel = new JLabel("", SharedIcons.INFO_OUTLINE.get(), SwingConstants.LEFT);
        infoLabel.setToolTipText("'?' matches any character, '*' matches a sequence of any characters");
        gbc.gridx++;
        gbc.insets = new Insets(4, 0, 8, 0);
        panel.add(infoLabel, gbc);
        // Regex RadioButton
        m_regex = new JRadioButton("Regular expression");
        typeGroup.add(m_regex);
        gbc.insets = new Insets(0, 8, 8, 0);
        gbc.gridx++;
        panel.add(m_regex, gbc);
        // Case Sensitive CheckBox
        m_caseSensitive = new JCheckBox("Case Sensitive", true);
        gbc.gridx++;
        panel.add(m_caseSensitive, gbc);
        // Exclude Matching CheckBox
        m_excludeMatching = new JCheckBox("Exclude Matching", false);
        gbc.gridx++;
        panel.add(m_excludeMatching, gbc);

        m_typeValue = getSelectedFilterType();

        m_caseSensitiveValue = m_caseSensitive.isSelected();
        m_excludeMatchingValue = m_excludeMatching.isSelected();
        m_additionalCheckbox = createAdditionalCheckbox(); //NOSONAR: This is the intention in this case
        if (m_additionalCheckbox != null) {
            gbc.gridy++;
            gbc.gridx--;
            gbc.gridwidth = 2;
            panel.add(m_additionalCheckbox, gbc);
            m_additionalCheckbox.setSelected(false);
            m_additionalCheckBoxValue = m_additionalCheckbox.isSelected();
        }
        if (Platform.getOS().equals(Platform.OS_MACOSX)) {
            // see AP-13012
            m_pattern.setCaret(new DefaultCaret());
        }
        m_pattern.addCaretListener(e -> {
            if (!m_patternValue.equals(m_pattern.getText())) {
                m_patternValue = m_pattern.getText();
                // async update - see bug 6073
                SwingUtilities.invokeLater(this::fireFilteringChangedEventWithCaret);
            }
        });
        m_wildcard.addChangeListener(e -> refreshPatternFilterType());
        m_regex.addChangeListener(e -> refreshPatternFilterType());
        m_caseSensitive.addChangeListener(e -> {
            if (m_caseSensitiveValue != m_caseSensitive.isSelected()) {
                m_caseSensitiveValue = m_caseSensitive.isSelected();
                fireFilteringChangedEvent();
            }
        });
        m_excludeMatching.addChangeListener(e -> {
            if (m_excludeMatchingValue != m_excludeMatching.isSelected()) {
                m_excludeMatchingValue = m_excludeMatching.isSelected();
                updatePreviewLabels();
                fireFilteringChangedEvent();
            }
        });
        if (m_additionalCheckbox != null) {
            m_additionalCheckbox.addChangeListener(e -> {
                if (m_additionalCheckBoxValue != m_additionalCheckbox.isSelected()) {
                    m_additionalCheckBoxValue = m_additionalCheckbox.isSelected();
                    fireFilteringChangedEvent();
                }
            });
        }
        // Add preview twin list
        m_preview = new FilterIncludeExcludePreview<>(m_parentFilter.getListCellRenderer());
        updatePreviewLabels();
        m_preview.setListSize(NORMAL_LIST);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = 6;
        gbc.insets = new Insets(0, 0, 0, 0);
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

    private void fireFilteringChangedEventWithCaret() {
        final int caretPosition = m_pattern.getCaretPosition();
        fireFilteringChangedEvent();
        m_pattern.requestFocusInWindow();
        m_pattern.setCaretPosition(caretPosition);
    }

    /**
     * An additional Checkbox, e.g. for missing values. Possibly overwritten by subclasses. Default implementation
     * returns null.
     *
     * @return null (here)
     */
    protected JCheckBox createAdditionalCheckbox() {
        return null;
    }

    /**
     * @return the additionalCheckbox
     */
    protected final Optional<JCheckBox> getAdditionalCheckbox() {
        return Optional.ofNullable(m_additionalCheckbox);
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
        m_excludeMatching.setEnabled(enabled);
        if (m_additionalCheckbox != null) {
            m_additionalCheckbox.setEnabled(enabled);
        }
        update();
    }

    /**
     * @param config to load from
     * @param names the available names that will be shown in the selection preview
     */
    protected void loadConfiguration(final PatternFilterConfiguration config, final String[] names) {
        m_names = names;
        m_pattern.setText(config.getPattern());
        if (config.getType() == PatternFilterType.Regex) {
            m_regex.setSelected(true);
            refreshPatternFilterType();
        } else if (config.getType() == PatternFilterType.Wildcard) {
            m_wildcard.setSelected(true);
            refreshPatternFilterType();
        }
        m_caseSensitive.setSelected(config.isCaseSensitive());
        m_excludeMatching.setSelected(config.isExcludeMatching());
        update();
    }

    /** @param config to save to */
    protected void saveConfiguration(final PatternFilterConfiguration config) {
        config.setPattern(m_pattern.getText());
        if (m_regex.isSelected()) {
            config.setType(PatternFilterType.Regex);
        } else if (m_wildcard.isSelected()) {
            config.setType(PatternFilterType.Wildcard);
        }
        config.setCaseSensitive(m_caseSensitive.isSelected());
        config.setExcludeMatching(m_excludeMatching.isSelected());
    }

    /**
     * Adds a listener which gets informed whenever the filtering changes.
     *
     * @param listener the listener
     */
    public void addChangeListener(final ChangeListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList<>();
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
            m_listeners.remove(listener);//NOSONAR: only called once
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

    private void updatePreviewLabels() {
        if (m_excludeMatchingValue) {
            m_preview.setBorderTitles(EXCLUDE_MATCH_LABEL, INCLUDE_NON_MATCH_LABEL);
        } else {
            m_preview.setBorderTitles(EXCLUDE_NON_MATCH_LABEL, INCLUDE_MATCH_LABEL);
        }
        m_preview.revalidate();
        m_preview.repaint();
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
    void update() {
        List<T> includes = new ArrayList<>();
        List<T> excludes = new ArrayList<>();
        var patternInvalid = false;
        try {
            filterPattern(includes, excludes);
        } catch (PatternSyntaxException e) {
            NodeLogger.getLogger(getClass()).debug(e);
            // Build HTML message for label, replacing newlines with line break '<br>' and spaces with non breaking
            // spaces '&nbsp;'
            String htmlMessage = "<html><div style=\"font-family: monospace\">"
                + e.getMessage().replace("\n", "<br>").replace(" ", "&nbsp;") + "</div></html>";
            m_invalid.setText(htmlMessage);
            patternInvalid = true;
            // Put all Ts into excludes (if not filtered out)
            for (String name : m_names) {
                final var t = m_parentFilter.getTforName(name);
                if (m_filter != null && !m_filter.include(t)) {
                    continue;
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

    /**
     * Filters the saved names according to the glob or regex pattern and puts the result in the provided list
     *
     * @param includes list containing the names that should be included
     * @param excludes list containing the names that should be excluded
     */
    private void filterPattern(final List<T> includes, final List<T> excludes) {
        // Create regex, this will throw an exception if the current pattern is invalid
        final var regex = PatternFilterConfiguration.compilePattern(m_patternValue, m_typeValue, m_caseSensitiveValue);
        Predicate<String> include = regex.asMatchPredicate();
        if (m_excludeMatchingValue) {
            include = include.negate();
        }
        // Fill lists
        Set<T> hiddenNames = m_parentFilter.getHiddenNames();
        for (String name : m_names) {
            final var t = m_parentFilter.getTforName(name);
            // Skip Ts that are filtered out or hidden
            if ((m_filter != null && !m_filter.include(t)) || hiddenNames.contains(t)) {
                continue;
            }
            if (include.test(name)) {
                includes.add(t);
            } else {
                excludes.add(t);
            }
        }
    }

    /**
     * @param filter The filter that filters out Ts that are not available for selection
     */
    void setFilter(final InputFilter<T> filter) {
        m_filter = filter;
        update();
    }

    /**
     * @param exclude title for the left box
     * @param include title for the right box
     * @since 3.4
     */
    public void setBorderTitles(final String exclude, final String include) {
        m_preview.setBorderTitles(exclude, include);
    }

    /**
     * sets the text of the "Include Missing Value"-Checkbox
     *
     * @param newText
     */
    public void setAdditionalCheckboxText(final String newText) {
        m_additionalCheckbox.setText(newText);
    }

    /**
     * Refresh the PatternFilterType with the currently selected one.
     */
    private void refreshPatternFilterType() {
        PatternFilterType newType = getSelectedFilterType();
        if (newType != null && m_typeValue != newType) {
            m_typeValue = newType;
            fireFilteringChangedEvent();
        }
    }
}
