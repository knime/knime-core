package org.knime.core.node.util;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


/**
 * A panel which shows a list of values as JRadioButtons with a titled border. Values can be set and received using
 * {@link #setSelectedValue(Object)} and {@link #getSelectedValue()}. The label of the buttons is determined by the
 * {@link #toString()} of the values given in the constructor. The internals rely on a consistent {@link #hashCode()}
 * and {@link #equals(Object)} method of the values. A good choice for values are {@link Enum}s.
 *
 * @author Marcel Hanser
 * @param <T> Type of the values
 * @since 2.11
 */
@SuppressWarnings("serial")
public final class RadionButtonPanel<T> extends JPanel {
    /** Name of the property name, which is used for a property changed event. */
    public static final String SELECTED_VALUE = "SELECTED_VALUE";


    private final Map<T, JRadioButton> m_buttonMap = new LinkedHashMap<>();

    private T m_selectedValue;

    /**
     * Constructor.
     *
     * @param title used in the titled border (null for no title)
     * @param allValues values which should be shown as Radio buttons
     */
    @SafeVarargs
    public RadionButtonPanel(final String title, final T... allValues) {
        m_selectedValue = null;

        setLayout(new GridLayout(0, 1));

        ButtonGroup buttonGroup = new ButtonGroup();
        for (final T obj : allValues) {
            JRadioButton jRadioButton = new JRadioButton(obj.toString());
            jRadioButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    firePropertyChange(SELECTED_VALUE, m_selectedValue, obj);
                    m_selectedValue = obj;
                }
            });
            add(jRadioButton);
            buttonGroup.add(jRadioButton);

            m_buttonMap.put(obj, jRadioButton);
        }

        // select the first option
        m_buttonMap.values().iterator().next().doClick();

        if (title != null) {
            setBorder(BorderFactory.createTitledBorder(title));
        }
    }

    /**
     * @return the selectedValue
     */
    public T getSelectedValue() {
        return m_selectedValue;
    }

    /**
     * @param selectedValue the selectedValue to set
     * @throws IllegalArgumentException if the given value is not contained in the values given in constructor
     */
    public void setSelectedValue(final T selectedValue) {
        JRadioButton jRadioButton = m_buttonMap.get(selectedValue);
        CheckUtils.checkArgumentNotNull(jRadioButton, "No button for value '%s' defined.", selectedValue);
        jRadioButton.doClick();
    }
}