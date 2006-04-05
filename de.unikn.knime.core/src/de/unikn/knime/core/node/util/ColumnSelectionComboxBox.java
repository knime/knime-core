/*
 * Created on Mar 6, 2006
 */
package de.unikn.knime.core.node.util;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.border.Border;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DataValue;

/**
 * Class extends a JComboxBox to choose a column of a certain type retrieved
 * from the <code>DataTableSpec</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColumnSelectionComboxBox extends JComboBox {

    private static final long serialVersionUID = 5797563450894378207L;

    /**
     * Show only columns of types that are compatible to one of theses classes.
     */
    private final Class<? extends DataValue>[] m_filterClasses;

    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a titled border with name "Column Selection".
     * 
     * @param filterValueClasses classes derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of these
     *            classes. All other columns will be ignored.
     * 
     * @see #update(DataTableSpec,DataCell)
     */
    public ColumnSelectionComboxBox(
            final Class<? extends DataValue>... filterValueClasses) {
        this(" Column Selection ", filterValueClasses);
    }

    /**
     * Creates a new column selection panel with the given border title; all
     * column are included in the combox box.
     * 
     * @param borderTitle The border title.
     */
    public ColumnSelectionComboxBox(final String borderTitle) {
        this(borderTitle, DataValue.class);
    }

    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a title border with a given title.
     * 
     * @param filterValueClasses a class derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of these
     *            classes. All other columns will be ignored.
     * @param borderTitle The title of the border
     * 
     * @see #update(DataTableSpec,DataCell)
     */
    public ColumnSelectionComboxBox(final String borderTitle,
            final Class<? extends DataValue>... filterValueClasses) {
        this(BorderFactory.createTitledBorder(borderTitle), filterValueClasses);
    }

    /**
     * Creates new Panel that will filter columns for particular value classes.
     * The panel will have a border as given. If null, no border is set.
     * 
     * @param filterValueClasses classes derived from DataValue. The combo box
     *            will allow to select only columns compatible with one of
     *            theses classes. All other columns will be ignored.
     * @param border Border for the panel or null to have no border.
     * 
     * @see #update(DataTableSpec,DataCell)
     */
    public ColumnSelectionComboxBox(final Border border,
            final Class<? extends DataValue>... filterValueClasses) {
        if (filterValueClasses == null || filterValueClasses.length == 0) {
            throw new NullPointerException("Classes must not be null");
        }
        List<Class<? extends DataValue>> list = Arrays
                .asList(filterValueClasses);
        if (list.contains(null)) {
            throw new NullPointerException("List of value classes must not "
                    + "contain null elements.");
        }
        m_filterClasses = filterValueClasses;
        if (border != null) {
            setBorder(border);
        }
        setRenderer(new DataColumnSpecListCellRenderer());
        setMinimumSize(new Dimension(100, 25));
    }

    
    /**
     * Updates this filter panel by removing all current items and adding the
     * columns according to the content of the argument <code>spec</code>. If
     * a column name is provided and it is not filtered out the corresponding
     * item in the combo box will be selected.
     * 
     * @param spec To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     */
    public final void update(final DataTableSpec spec, final DataCell selColName) {
        update(spec, selColName, false);
    }
    
    /**
     * Updates this filter panel by removing all current items and adding the
     * columns according to the content of the argument <code>spec</code>. If
     * a column name is provided and it is not filtered out the corresponding
     * item in the combo box will be selected.
     * 
     * @param spec To get the column names, types and the current index from.
     * @param selColName The column name to be set as chosen.
     * @param suppressEvents <code>true</code> if events caused by adding items to the combo
     * box should be suppressed, <code>false</code> otherwise
     */
    public final void update(final DataTableSpec spec, final DataCell selColName, final boolean suppressEvents) {
        ItemListener[] itemListeners = null;
        ActionListener[] actionListeners = null;
        
        if (suppressEvents) {
            itemListeners = getListeners(ItemListener.class);
            for (ItemListener il : itemListeners) {
                removeItemListener(il);
            }
            
            actionListeners = getListeners(ActionListener.class);
            for (ActionListener al : actionListeners) {
                removeActionListener(al);
            }
        }
        
        removeAllItems();
        DataColumnSpec selectMe = null;
        if (spec != null) {
            for (int c = 0; c < spec.getNumColumns(); c++) {
                DataColumnSpec current = spec.getColumnSpec(c);
                DataType type = current.getType();
                for (Class<? extends DataValue> cl : m_filterClasses) {
                    if (type.isCompatible(cl)) {
                        addItem(current);
                        if (current.getName().equals(selColName)) {
                            selectMe = current;
                        }
                        break;
                    }
                }
            }
            setSelectedItem(null);
        }

        if (suppressEvents) {
            for (ItemListener il : itemListeners) {
                addItemListener(il);
            }
            
            for (ActionListener al : actionListeners) {
                addActionListener(al);
            }
        }

    
        if (selectMe != null) {            
            setSelectedItem(selectMe);
        } else {
            // select last element
            int size = getItemCount();
            if (size > 0) {
                setSelectedIndex(size - 1);
            }
        }
    }

    /**
     * Gets the selected column.
     * 
     * @return The cell that is currently being selected.
     */
    public final DataCell getSelectedColumn() {
        DataColumnSpec selected = (DataColumnSpec)getSelectedItem();
        if (selected != null) {
            return selected.getName();
        }
        return null;
    }
}
