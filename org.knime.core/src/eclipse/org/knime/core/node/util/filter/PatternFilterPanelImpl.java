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
 * Created on Oct 4, 2013 by Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.util.filter;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.core.node.util.filter.PatternFilterConfigurationImpl.PatternFilterType;

/**
 * Filters based on the given regular expression or wildcard pattern.
 *
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
@SuppressWarnings("serial")
final class PatternFilterPanelImpl extends JPanel {

    private JTextField m_pattern;

    private ButtonGroup m_type;

    private JRadioButton m_regex;

    private JRadioButton m_wildcard;

    private JCheckBox m_caseSensitive;

    /**
     * Create the pattern filter panel.
     */
    PatternFilterPanelImpl() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        m_pattern = new JTextField();
        m_pattern.setText("");
        panel.add(m_pattern);
        m_type = new ButtonGroup();
        m_wildcard = new JRadioButton("Wildcard ('?' matches any character, '*' matches a sequence of any characters)");
        m_type.add(m_wildcard);
        panel.add(m_wildcard);
        m_regex = new JRadioButton("Regular expression");
        m_type.add(m_regex);
        panel.add(m_regex);
        m_wildcard.setSelected(true);
        m_caseSensitive = new JCheckBox("Case Sensitive");
        panel.add(m_caseSensitive);
        m_caseSensitive.setSelected(true);
        super.add(panel);
    }

    /** @param config to load from */
    void loadConfiguration(final PatternFilterConfigurationImpl config) {
        m_pattern.setText(config.getPattern());
        if (config.getType().equals(PatternFilterType.Regex)) {
            m_regex.doClick();
        } else if (config.getType().equals(PatternFilterType.Wildcard)) {
            m_wildcard.doClick();
        }
        m_caseSensitive.setSelected(config.isCaseSensitive());
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

}
