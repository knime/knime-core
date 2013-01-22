package org.knime.base.data.aggregation.dialogutil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.core.node.NotConfigurableException;

/**
 * This class implements the aggregation operator settings button that is
 * displayed in the aggregation operator table. It opens a settings dialog
 * that allows the user to adjust operator specific settings.
 *
 * The code was partially taken from
 * <a href="http://tips4java.wordpress.com/2009/07/12/table-button-column/">
 * Rob Camick</a>.
 * @since 2.7
 */
public class OperatorSettingsButtonCellRenderer extends AbstractCellEditor
implements TableCellRenderer, TableCellEditor, MouseListener {

    private static final JLabel NO_SETTINGS_COMPONENT = new JLabel("");

    private static final long serialVersionUID = 1L;

    private Border m_originalBorder;

    private Border m_focusBorder;

    private JButton m_renderButton;

    private JButton m_editButton;

    private Object m_editorValue;

    private boolean m_isButtonColumnEditor;

    private final AbstractAggregationPanel<?, ?, ?> m_rootPanel;

    /**
     * Create the ButtonColumn to be used as a renderer and editor.
     * The renderer and editor will automatically be installed on the
     * TableColumn of the specified column.
     * @param rootPanel the AbstractAggregationPanel this button is added
     */
    public OperatorSettingsButtonCellRenderer(
      final AbstractAggregationPanel<?, ?, ?> rootPanel) {
        if (rootPanel == null) {
            throw new NullPointerException("rootPanel must not be null");
        }
        m_rootPanel = rootPanel;
        m_renderButton = new JButton();
        m_editButton = new JButton();
        m_editButton.setFocusPainted(false);
        m_originalBorder = m_editButton.getBorder();
        setFocusBorder(new LineBorder(Color.BLUE));
        m_rootPanel.getTable().addMouseListener(this);
    }


    /**
     * Get foreground color of the button when the cell has focus.
     *
     * @return the foreground color
     */
    public Border getFocusBorder() {
        return m_focusBorder;
    }

    /**
     * The foreground color of the button when the cell has focus.
     *
     * @param focusBorder the foreground color
     */
    public void setFocusBorder(final Border focusBorder) {
        m_focusBorder = focusBorder;
        m_editButton.setBorder(focusBorder);
    }

    @Override
    public Object getCellEditorValue() {
        return m_editorValue;
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table,
         final Object value, final boolean isSelected, final int row,
         final int column) {
        if (value == null) {
            return NO_SETTINGS_COMPONENT;
        } else {
            m_editButton.setEnabled(false);
            m_editButton.setText("Edit");
            m_editButton.setIcon(null);
        }
        m_editorValue = value;
        return m_editButton;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellRendererComponent(final JTable table,
       final Object value, final boolean isSelected, final boolean hasFocus,
       final int row, final int column) {
        if (Boolean.FALSE.equals(value)) {
            return NO_SETTINGS_COMPONENT;
        }
        if (isSelected) {
            m_renderButton.setForeground(table.getSelectionForeground());
            m_renderButton.setBackground(table.getSelectionBackground());
        } else {
            m_renderButton.setForeground(table.getForeground());
            m_renderButton.setBackground(
                                 UIManager.getColor("Button.background"));
        }
        if (hasFocus) {
            m_renderButton.setBorder(m_focusBorder);
        } else {
            m_renderButton.setBorder(m_originalBorder);
        }

        m_renderButton.setText("Edit");
        m_renderButton.setIcon(null);

        return m_renderButton;
    }

    private void openSettingsDialog() {
        final JTable table = m_rootPanel.getTable();
        final int row = table.convertRowIndexToModel(table.getEditingRow());
        fireEditingStopped();

        final AggregationMethod aggr = m_rootPanel.getTableModel().getRow(row);
        if (!aggr.hasOptionalSettings()) {
            //the operator has no additional settings
            return;
        }

        // figure out the parent to be able to make the dialog modal
        Frame f = null;
        Container c = m_rootPanel.getComponentPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        try {
            final AggregationParameterDialog dialog =
                    new AggregationParameterDialog(f, aggr, m_rootPanel.getInputTableSpec());
            //center the dialog
            dialog.setLocationRelativeTo(c);
            dialog.pack();
            //show it
            dialog.setVisible(true);
        } catch (NotConfigurableException e) {
            //show the error message
              JOptionPane.showMessageDialog(m_rootPanel.getComponentPanel(), e.getMessage(),
                                "Unable to open dialog", JOptionPane.ERROR_MESSAGE);
              return;
          }
    }

    //
    //  Implement MouseListener interface
    //
    /*
     *  When the mouse is pressed the editor is invoked. If you then drag
     *  the mouse to another cell before releasing it, the editor is still
     *  active. Make sure editing is stopped when the mouse is released.
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(final MouseEvent e) {
        JTable table = m_rootPanel.getTable();
        if (table.isEditing() && table.getCellEditor() == this) {
            m_isButtonColumnEditor = true;
            openSettingsDialog();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(final MouseEvent e) {
        JTable table = m_rootPanel.getTable();
        if (m_isButtonColumnEditor && table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        m_isButtonColumnEditor = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(final MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(final MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(final MouseEvent e) {
    }

}
