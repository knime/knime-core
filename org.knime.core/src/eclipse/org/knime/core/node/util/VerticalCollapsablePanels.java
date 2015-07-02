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
 *   9 Febr 2015 (Gabor): created
 */
package org.knime.core.node.util;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * A Swing control to provide collapsable controls, with multiple possible open panels.
 *
 * @author Gabor Bakos
 * @since 2.12
 */
@SuppressWarnings("serial")
public class VerticalCollapsablePanels extends JPanel {
    private static final Icon OPEN = ViewUtils.loadIcon(VerticalCollapsablePanels.class, "down.png"),
            COLLAPSED = ViewUtils.loadIcon(VerticalCollapsablePanels.class, "right.png");

    private static class WrappedPanel extends JPanel {
        private JPanel m_wrapped;

        private boolean m_collapsed;

        /**
         * @param panel The {@link JPanel} to wrap.
         * @param collapsed Is it collapsed by default?
         * @param text The text to show on top of the panel.
         */
        public WrappedPanel(final JPanel panel, final boolean collapsed, final String text) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentY(TOP_ALIGNMENT);
            setAlignmentX(LEFT_ALIGNMENT);
            this.m_wrapped = panel;
            this.m_collapsed = collapsed;
            final JButton button = new JButton();
            button.setAlignmentY(CENTER_ALIGNMENT);
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            AbstractAction action = new AbstractAction(text, m_collapsed ? COLLAPSED : OPEN) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_collapsed ^= true;
                    button.setIcon(m_collapsed ? COLLAPSED : OPEN);
                    m_wrapped.setVisible(!m_collapsed);
                }
            };
            button.setAction(action);
            JPanel buttonWrapper = new JPanel();
            buttonWrapper.setLayout(new BoxLayout(buttonWrapper, BoxLayout.LINE_AXIS));
            buttonWrapper.add(button);
            buttonWrapper.add(Box.createHorizontalGlue());
            buttonWrapper.setAlignmentX(CENTER_ALIGNMENT);
            buttonWrapper.setAlignmentY(TOP_ALIGNMENT);
            add(buttonWrapper);
            m_wrapped.setAlignmentX(CENTER_ALIGNMENT);
            m_wrapped.setAlignmentY(TOP_ALIGNMENT);
            add(m_wrapped);
            m_wrapped.setVisible(!m_collapsed);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setEnabled(final boolean enabled) {
            super.setEnabled(enabled);
            getComponent(0).setEnabled(enabled);
            m_wrapped.setEnabled(enabled);
        }
    }

    /**
     * Constructs a double buffered {@link VerticalCollapsablePanels}.
     *
     * @see #VerticalCollapsablePanels(boolean)
     */
    public VerticalCollapsablePanels() {
        this(true);
    }

    /**
     * Constructs a {@link VerticalCollapsablePanels} with the default vertical filler at the bottom (270 pixels).
     *
     * @param isDoubleBuffered {@code true} for double-buffering, which uses additional memory space to achieve fast,
     *            flicker-free updates
     */
    public VerticalCollapsablePanels(final boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentY(TOP_ALIGNMENT);
        setAlignmentX(LEFT_ALIGNMENT);
    }

    /**
     * Not supported, use the {@link #add(Component, int)} if you really have to.
     *
     * @throws IllegalStateException always
     */
    @Override
    @Deprecated
    public Component add(final Component comp) {
        throw new IllegalStateException();
    }

    /**
     * Adds a panel wrapped with {@code text} on the wrapper.
     *
     * @param panel The {@link JPanel} to add.
     * @param collapsed Collapsed or not.
     * @param text The text on the wrapper panel.
     * @return The wrapped panel just in case you want to enable/disable it.
     */
    public JPanel addPanel(final JPanel panel, final boolean collapsed, final String text) {
        WrappedPanel wrapped = new WrappedPanel(panel, collapsed, text);
        super.add(wrapped);
        return wrapped;
    }

    /**
     * Adds a panel wrapped without text on the wrapper.
     *
     * @param panel The {@link JPanel} to add.
     * @param collapsed Collapsed or not.
     */
    public void addPanel(final JPanel panel, final boolean collapsed) {
        addPanel(panel, collapsed, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        for (Component comp : getComponents()) {
            comp.setEnabled(enabled);
        }
    }
}
