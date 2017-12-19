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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;

/**
 * A default panel to show two color palettes.
 *
 * @author Johannes Schweig, KNIME AG
 */
final class DefaultPalettesColorPanel extends AbstractColorChooserPanel {

    private final JButton m_set1Button = new JButton("Apply to columns");

    private final JButton m_set2Button = new JButton("Apply to columns");

    private final String[] m_paletteDefault;

    private final String[] m_palettePaired;

    /**
     * @param paletteDefault the first, default color palette
     * @param palettePaired the second color palette
     */
    DefaultPalettesColorPanel(final String[] paletteDefault, final String[] palettePaired) {
        m_paletteDefault = paletteDefault;
        m_palettePaired = palettePaired;
    }

    /**
     * Overwrites the default JButton to notify the ColorSelectionModel of changes.
     */
    private class PaletteButton extends JButton {
        private static final int SIZE = 30;

        private Color m_color;

        PaletteButton(final String c) {
            m_color = Color.decode(c);
            setPreferredSize(new Dimension(SIZE, SIZE));
            setBackground(m_color);
            setForeground(m_color);
            setBorderPainted(false);
            setFocusPainted(false);
            addActionListener(e -> getColorSelectionModel().setSelectedColor(m_color));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildChooser() {
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //JPanels
        JPanel set1Panel = new JPanel(new FlowLayout());
        set1Panel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel set2Panel = new JPanel(new FlowLayout());
        set2Panel.setAlignmentX(LEFT_ALIGNMENT);

        for (int i = 0; i < 12; i++) {
            set1Panel.add(new PaletteButton(m_paletteDefault[i]));
            set2Panel.add(new PaletteButton(m_palettePaired[i]));
        }
        //JButtons Apply
        set1Panel.add(new JPanel());
        m_set1Button.setFont(new Font(m_set1Button.getFont().getName(), Font.PLAIN, m_set1Button.getFont().getSize()));
        set1Panel.add(m_set1Button);
        set2Panel.add(new JPanel());
        m_set2Button.setFont(new Font(m_set2Button.getFont().getName(), Font.PLAIN, m_set2Button.getFont().getSize()));
        set2Panel.add(m_set2Button);

        //JLabels
        JLabel set1Label = new JLabel("Default");
        set1Label.setFont(new Font(set1Label.getFont().getName(), Font.PLAIN, set1Label.getFont().getSize() + 2));
        JLabel set2Label = new JLabel("Paired");
        set2Label.setFont(new Font(set2Label.getFont().getName(), Font.PLAIN, set2Label.getFont().getSize() + 2));

        //add panels to layout
        super.add(set1Label);
        super.add(Box.createVerticalStrut(5));
        super.add(set1Panel);
        super.add(Box.createVerticalStrut(20));
        super.add(set2Label);
        super.add(Box.createVerticalStrut(5));
        super.add(set2Panel);

    }

    /**
     * @param al1 the action listener for the first button
     * @param al2 the action listener for the second button
     */
    void addActionListeners(final ActionListener al1, final ActionListener al2) {
        m_set1Button.addActionListener(al1);
        m_set2Button.addActionListener(al2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_set1Button.setVisible(enabled);
        m_set2Button.setVisible(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "Palettes";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMnemonic() {
        return KeyEvent.VK_P;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDisplayedMnemonicIndex() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Icon getLargeDisplayIcon() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Icon getSmallDisplayIcon() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateChooser() {

    }
}
