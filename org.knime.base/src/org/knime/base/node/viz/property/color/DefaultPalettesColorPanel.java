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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.colorchooser.AbstractColorChooserPanel;

import org.knime.base.node.viz.property.color.ColorManager2NodeModel.PaletteOption;

/**
 * A default panel to show some color palettes.
 *
 * @author Johannes Schweig, KNIME AG
 */
final class DefaultPalettesColorPanel extends AbstractColorChooserPanel {

    private final JRadioButton m_customSetRadioButton;

    private final JRadioButton m_set1RadioButton;

    private final JRadioButton m_set2RadioButton;

    private final JRadioButton m_set3RadioButton;

    private final String[] m_paletteSet1;

    private final String[] m_paletteSet2;

    private final String[] m_paletteSet3;

    /** Size of the individual elements of a palette. */
    private static final int PALETTE_ELEMENT_SIZE = 20;

    /** Spacing between the individual elements of a palette. */
    private static final int PALETTE_ELEMENT_SPACING = 4;

    /**
     * @param paletteSet1 the first, default color palette
     * @param paletteSet2 the second color palette
     */
    DefaultPalettesColorPanel(final String[] paletteSet1, final String[] paletteSet2, final String[] paletteSet3) {
        m_paletteSet1 = paletteSet1;
        m_paletteSet2 = paletteSet2;
        m_paletteSet3 = paletteSet3;
        m_customSetRadioButton = new JRadioButton("Custom");
        m_set1RadioButton = new JRadioButton("Set 1");
        m_set2RadioButton = new JRadioButton("Set 2");
        m_set3RadioButton = new JRadioButton("Set 3 (colorblind safe)");
    }

    void setChooserEnabled(final boolean enabled) {
        m_customSetRadioButton.setEnabled(enabled);
        m_set1RadioButton.setEnabled(enabled);
        m_set2RadioButton.setEnabled(enabled);
        m_set3RadioButton.setEnabled(enabled);
        if (!enabled) {
            m_customSetRadioButton.setSelected(true);
        }
    }

    /**
     * @return the palette option of the currently selected column
     */
    PaletteOption getPaletteOption() {
        if (m_set1RadioButton.isSelected()) {
            return PaletteOption.SET1;
        } else if (m_set2RadioButton.isSelected()) {
            return PaletteOption.SET2;
        } else if (m_set3RadioButton.isSelected()) {
            return PaletteOption.SET3;
        } else {
            return PaletteOption.CUSTOM_SET;
        }
    }

    /**
     * Updates the palette option with a new palette option, updates state of radio button
     * @param po new palette option
     */
    void setPaletteOption(final PaletteOption po) {
        switch (po) {
            case SET1:
                setChooserEnabled(true);
                m_set1RadioButton.doClick();
                break;
            case SET2:
                setChooserEnabled(true);
                m_set2RadioButton.doClick();
                break;
            case SET3:
                setChooserEnabled(true);
                m_set3RadioButton.doClick();
                break;
            default:
                m_customSetRadioButton.doClick();
                break;
        }
    }

    /**
     * Overwrites the default JPanel to notify the ColorSelectionModel of changes.
     */
    private final class PaletteElement extends JPanel {

        private Color m_color;

        PaletteElement(final String c, final int size) {
            m_color = Color.decode(c);
            setPreferredSize(new Dimension(size, size));
            setBackground(m_color);
            addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(final MouseEvent e){
                    getColorSelectionModel().setSelectedColor(m_color);
                }
                @Override
                public void mousePressed(final MouseEvent e){
                  setBorder(BorderFactory.createLineBorder(Color.gray));
                }
                @Override
                public void mouseReleased(final MouseEvent e){
                  setBorder(BorderFactory.createLineBorder(Color.black));
                }
                @Override
                public void mouseEntered(final MouseEvent e){
                  setBorder(BorderFactory.createLineBorder(Color.black));
                }
                @Override
                public void mouseExited(final MouseEvent e){
                  setBorder(null);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildChooser() {
        super.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JPanel set1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, PALETTE_ELEMENT_SPACING, 0));
        set1Panel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel set2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, PALETTE_ELEMENT_SPACING, 0));
        set2Panel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel set3Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, PALETTE_ELEMENT_SPACING, 0));
        set3Panel.setAlignmentX(LEFT_ALIGNMENT);

        // add radiobuttons
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_set1RadioButton);
        bg.add(m_set2RadioButton);
        bg.add(m_set3RadioButton);
        bg.add(m_customSetRadioButton);
        //add colored Panels
        String blankSpace = "   ";
        set1Panel.add(new JLabel(blankSpace));
        for (String s : m_paletteSet1) {
            set1Panel.add(new PaletteElement(s, PALETTE_ELEMENT_SIZE));
        }
        set2Panel.add(new JLabel(blankSpace));
        for (String s : m_paletteSet2) {
            set2Panel.add(new PaletteElement(s, PALETTE_ELEMENT_SIZE));
        }
        set3Panel.add(new JLabel(blankSpace));
        for (String s : m_paletteSet3) {
            set3Panel.add(new PaletteElement(s, PALETTE_ELEMENT_SIZE));
        }

        Font font = new Font(getFont().getName(), Font.PLAIN, getFont().getSize() + 2);
        m_customSetRadioButton.setFont(font);
        m_set1RadioButton.setFont(font);
        m_set2RadioButton.setFont(font);
        m_set3RadioButton.setFont(font);

        //add panels to layout
        super.add(Box.createVerticalStrut(5));

        super.add(m_set1RadioButton);
        super.add(Box.createVerticalStrut(5));
        super.add(set1Panel);

        super.add(Box.createVerticalStrut(20));
        super.add(m_set2RadioButton);
        super.add(Box.createVerticalStrut(5));
        super.add(set2Panel);

        super.add(Box.createVerticalStrut(20));
        super.add(m_set3RadioButton);
        super.add(Box.createVerticalStrut(5));
        super.add(set3Panel);

        super.add(Box.createVerticalStrut(20));
        super.add(m_customSetRadioButton);

    }


    /**
     * @param al1 the action listener for the first button
     * @param al2 the action listener for the second button
     * @param al3 the action listener for the third button
     * @param al4 the action listener for the first radio button
     * @param al5 the action listener for the second radio button
     * @param al6 the action listener for the third radio button
     * @param al7 the action listener for the fourt radio button ('custom')
     */
    void addListeners(final ActionListener al1, final ActionListener al2, final ActionListener al3,
        final ActionListener al4, final ActionListener al5, final ActionListener al6, final ActionListener al7) {
        m_set1RadioButton.addActionListener(al1);
        m_set2RadioButton.addActionListener(al2);
        m_set3RadioButton.addActionListener(al3);
        m_set1RadioButton.addActionListener(al4);
        m_set2RadioButton.addActionListener(al5);
        m_set3RadioButton.addActionListener(al6);
        m_customSetRadioButton.addActionListener(al7);
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

    /** Listener added to the radio buttons in the south -- it will apply the palette to the list in the north. */
    final class MyItemListener implements ItemListener {

        private final PaletteOption m_selectPaletteOption;

        MyItemListener(final PaletteOption selectPaletteOption) {
            m_selectPaletteOption = selectPaletteOption;
        }

        @Override
        public void itemStateChanged(final ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                setPaletteOption(m_selectPaletteOption);
            }
        }
    }

}
