/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   31 Jan 2015 (Gabor): created
 */
package org.knime.core.node.util;import java.awt.Component;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * An "accordion" (in reality just a {@link JTabbedPane} currently) with custom components. The selection means, the
 * active tab is used.
 * <br>
 * Not intended to be serialized!
 *
 * @author Gabor Bakos
 * @since 2.12
 */
@SuppressWarnings("serial")
@Deprecated
final class SelectableAccordion extends JTabbedPane {
    private static final Icon NOT_SELECTED_ICON = new ImageIcon(SelectableAccordion.class.getResource("down.png")),
            SELECTED_ICON = new ImageIcon(SelectableAccordion.class.getResource("right.png"));

//    private String m_key;

    private int m_defaultIndex;

    /**
     * @param defaultIndex The initial index to start with after construction.
     */
    public SelectableAccordion(final int defaultIndex) {
//        this.m_key = key;
        this.m_defaultIndex = defaultIndex;
        setTabPlacement(SwingConstants.LEFT);
        addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (getIconAt(getSelectedIndex()) != SELECTED_ICON) {
                    for (int i = getTabCount(); i-- > 0;) {
                        CompositeIcon iconAt = (CompositeIcon)getIconAt(i);
                        iconAt.fIcon1 = i == getSelectedIndex() ? SELECTED_ICON : NOT_SELECTED_ICON;
                        setIconAt(i, iconAt);
                    }
                }
            }
        });
    }

//    public void saveSettings(final NodeSettingsWO settings) {
//        settings.addInt(m_key, getModel().getSelectedIndex());
//    }
//
//    public void loadSettingsForDialog(final NodeSettingsRO settings) {
//        setSelectedIndex(settings.getInt(m_key, m_defaultIndex));
//    }
//
//    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
//        setSelectedIndex(settings.getInt(m_key));
//    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTab(final String title, final Component component) {
        super.addTab("", new CompositeIcon(NOT_SELECTED_ICON, new VTextIcon(this, title, VTextIcon.ROTATE_LEFT)), component);
        if (getTabCount() > m_defaultIndex) {
            setSelectedIndex(m_defaultIndex);
        }
    }
}
