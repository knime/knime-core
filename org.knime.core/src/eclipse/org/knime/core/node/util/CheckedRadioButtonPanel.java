package org.knime.core.node.util;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * Extends the {@link RadionButtonPanel} with a CheckBox placed on the border. If the CheckBox is not checked, than all
 * buttons are disabled. An additional value is given in the constructor, which is not shown as radio button but instead
 * determines the state if the CheckBox is not checked.
 *
 *
 * @see RadionButtonPanel
 * @author Marcel Hanser
 * @param <T> Type of the values
 * @since 2.11
 */
@SuppressWarnings("serial")
public final class CheckedRadioButtonPanel<T> extends JPanel {

    private final T m_notSelectedValue;

    private final JCheckBox m_isSelected;

    private final String m_disabledLabel;

    private final Map<T, JRadioButton> m_buttonMap = new LinkedHashMap<>();

    private final JPanel m_radioButtonPanel;

    private T m_selectedValue;

    private JPanel m_disabledLabelPanel;

    /**
     * Constructor.
     *
     * @param checkBoxTitle label of the CheckBox on the border.
     * @param disabledLabel the content of the JLabel which is set if this panel gets disabled
     * @param notSelectedValue return value of {@link #getSelectedValue()} if this panel is disabled or the CheckBox is
     *            not checked
     * @param allValues values which should be shown as Radio buttons.
     */
    @SafeVarargs
    public CheckedRadioButtonPanel(final String checkBoxTitle, final String disabledLabel, final T notSelectedValue,
        final T... allValues) {
        m_disabledLabel = disabledLabel;
        m_notSelectedValue = notSelectedValue;
        m_selectedValue = notSelectedValue;

        setLayout(new FlowLayout());
        m_isSelected = new JCheckBox(checkBoxTitle);
        m_isSelected.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                //deselect all buttons
                for (JRadioButton b : m_buttonMap.values()) {
                    b.setEnabled(m_isSelected.isSelected());
                    if (b.isSelected()) {
                        b.doClick();
                    }
                }
            }
        });

        m_radioButtonPanel = new JPanel(new GridLayout(0, 1));
        m_radioButtonPanel.setBorder(new ComponentBorder(m_isSelected, m_radioButtonPanel, BorderFactory
            .createTitledBorder("")));

        ButtonGroup buttonGroup = new ButtonGroup();
        for (final T obj : allValues) {
            if (!obj.equals(notSelectedValue)) {
                JRadioButton jRadioButton = new JRadioButton(obj.toString());
                jRadioButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        m_selectedValue = obj;
                    }
                });
                m_radioButtonPanel.add(jRadioButton);
                buttonGroup.add(jRadioButton);

                m_buttonMap.put(obj, jRadioButton);
            }
        }
        m_radioButtonPanel.validate();

        // select the first option
        m_buttonMap.values().iterator().next().doClick();
        add(m_radioButtonPanel);

        m_disabledLabelPanel = new JPanel();
        m_disabledLabelPanel.add(new JLabel(m_disabledLabel));
        m_disabledLabelPanel.setBorder(new ComponentBorder(m_isSelected, m_disabledLabelPanel, BorderFactory
            .createTitledBorder("")));
        m_disabledLabelPanel.setPreferredSize(m_radioButtonPanel.getPreferredSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        preferredSize.height -= m_isSelected.getPreferredSize().height;
        return preferredSize;
    }

    /**
     * @return the selectedValue
     */
    public T getSelectedValue() {
        return m_isSelected.isSelected() ? m_selectedValue : m_notSelectedValue;
    }

    /**
     * If the value does not equal any of the values given in the constructor the CheckBox is cleaderd and all buttons
     * are disabled. The {@link #getSelectedValue()} will return the "notSelectedValue" given in the constructor.
     * Otherwise all buttons are enabled and the button according to the given value is set.
     *
     * @param selectedValue the selectedValue to set
     */
    public void setSelectedValue(final T selectedValue) {
        m_isSelected.setEnabled(true);
        JRadioButton jRadioButton = m_buttonMap.get(selectedValue);
        if (jRadioButton != null) {
            m_isSelected.setSelected(true);
            jRadioButton.doClick();
            for (JRadioButton b : m_buttonMap.values()) {
                b.setEnabled(true);
            }
        } else {
            //deselect all buttons
            m_isSelected.setSelected(false);
            for (JRadioButton b : m_buttonMap.values()) {
                b.setEnabled(false);
            }
        }
        this.m_selectedValue = selectedValue;
    }

    /**
     * Disables/enables the panel and sets a label instead of the buttons or the other way around.
     *
     * @param val the value to
     */
    @Override
    public void setEnabled(final boolean val) {
        if (!val && m_isSelected.isSelected()) {
            m_isSelected.doClick();
        }
        m_isSelected.setEnabled(val);
        if (!val) {
            removeAll();
            add(m_disabledLabelPanel);
        } else {
            removeAll();
            add(m_radioButtonPanel);
        }
        revalidate();
    }
}