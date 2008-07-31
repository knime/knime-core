/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   16.07.2007 (gabriel): created
 */
package org.knime.base.node.preproc.colcompare;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Comparator node dialog pane to select two columns for comparison, replacement
 * value or missing, and a new column name to append.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnComparatorNodeDialogPane extends NodeDialogPane {
    
    private final DialogComponentColumnNameSelection m_firstColumn =
        new DialogComponentColumnNameSelection(
                createFirstColumnModel(), "Column left: ", 0, 
                DataValue.class);
    
    private final DialogComponentColumnNameSelection m_secondColumn =
        new DialogComponentColumnNameSelection(
                createSecondColumnModel(), "Column right: ", 0, 
                DataValue.class);
    
    private final DialogComponentStringSelection m_operator =
            new DialogComponentStringSelection(
                    createComparatorMethod(), "Operator: ",
                    ComparatorMethod.COMPARATOR_LIST);

    private final DialogComponentStringSelection m_replaceMatch;
    private final DialogComponentStringSelection m_replaceMismatch;
    
    private final DialogComponentString m_valueMatch;
    private final DialogComponentString m_valueMismatch;
    
    private final DialogComponentString m_newColumn =
        new DialogComponentString(createNewColumnName(), "Name: ");
    
    /**
     * Create new dialog pane with default components.
     */
    ColumnComparatorNodeDialogPane() {
        JPanel panel = super.getPanel();
        panel.removeAll();
        panel.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel(new GridLayout(1, 3));
        northPanel.setBorder(
                BorderFactory.createTitledBorder(" Column and Operator "));
        northPanel.add(m_firstColumn.getComponentPanel());
        northPanel.add(m_operator.getComponentPanel());
        northPanel.add(m_secondColumn.getComponentPanel());
        panel.add(northPanel, BorderLayout.NORTH);
        JPanel centerPanel = new JPanel(new GridLayout(2, 2));
        centerPanel.setBorder(
                BorderFactory.createTitledBorder(" Replacement Method "));
        final SettingsModelString matchValueModel = createMatchValue();
        m_valueMatch = new DialogComponentString(matchValueModel, "Tag: ");
        final SettingsModelString mismatchValueModel = createMismatchValue();
        m_valueMismatch = new DialogComponentString(
                mismatchValueModel, "Tag: ");
        final SettingsModelString matchModel = createMatchOption();
        final SettingsModelString mismatchModel = createMismatchOption();
        matchModel.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                if (matchModel.getStringValue().equals(
                        REPL_OPTIONS[3])) {
                    matchValueModel.setEnabled(true);
                } else {
                    matchValueModel.setEnabled(false);
                }
            }
        });
        mismatchModel.addChangeListener(new ChangeListener() {
            /**
             * {@inheritDoc}
             */
            public void stateChanged(final ChangeEvent e) {
                if (mismatchModel.getStringValue().equals(
                        REPL_OPTIONS[3])) {
                    mismatchValueModel.setEnabled(true);
                } else {
                    mismatchValueModel.setEnabled(false);
                }
            }
        });
        m_replaceMatch = new DialogComponentStringSelection(matchModel, 
                "Operator result 'true': ", Arrays.asList(REPL_OPTIONS));
        centerPanel.add(m_replaceMatch.getComponentPanel());
        centerPanel.add(m_valueMatch.getComponentPanel());
        m_replaceMismatch = new DialogComponentStringSelection(mismatchModel, 
                "Operator result 'false': ", Arrays.asList(REPL_OPTIONS));
        centerPanel.add(m_replaceMismatch.getComponentPanel());
        centerPanel.add(m_valueMismatch.getComponentPanel());
        panel.add(centerPanel, BorderLayout.CENTER);
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(BorderFactory.createTitledBorder(" New Column "));
        southPanel.add(m_newColumn.getComponentPanel());
        panel.add(southPanel, BorderLayout.SOUTH);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, 
            final DataTableSpec[] specs) throws NotConfigurableException {
        if (specs[0].getNumColumns() < 2) {
            throw new NotConfigurableException(
                    "Need at least to columns for comparison.");
        }
        m_firstColumn.loadSettingsFrom(settings, specs);
        m_secondColumn.loadSettingsFrom(settings, specs);
        m_operator.loadSettingsFrom(settings, specs);
        m_newColumn.loadSettingsFrom(settings, specs);
        m_replaceMatch.loadSettingsFrom(settings, specs);
        m_replaceMismatch.loadSettingsFrom(settings, specs);
        m_valueMatch.loadSettingsFrom(settings, specs);
        m_valueMismatch.loadSettingsFrom(settings, specs);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) 
            throws InvalidSettingsException {
        m_firstColumn.saveSettingsTo(settings);
        m_secondColumn.saveSettingsTo(settings);
        m_operator.saveSettingsTo(settings);
        m_newColumn.saveSettingsTo(settings);
        m_replaceMatch.saveSettingsTo(settings);
        m_replaceMismatch.saveSettingsTo(settings);
        m_valueMatch.saveSettingsTo(settings);
        m_valueMismatch.saveSettingsTo(settings);
    }
    
    /**
     * @return model comparator method
     */
    static SettingsModelString createComparatorMethod() {
        return new SettingsModelString("comparator_method", 
                ComparatorMethod.EQUAL.toString());
    }
    
    /**
     * @return settings model for first column selection
     */
    static SettingsModelString createFirstColumnModel() {
        return new SettingsModelString("first_column", null);
    }
    
    /**
     * @return settings model for second column selection
     */
    static SettingsModelString createSecondColumnModel() {
        return new SettingsModelString("second_column", null);
    }
    
    /**
     * Replacement options: LEFT_VALUE, RIGHT_VALUE, MISSING, and USER_DEFINED.
     */
    public final static String[] REPL_OPTIONS =
        new String[]{"LEFT_VALUE", "RIGHT_VALUE", "MISSING", "USER_DEFINED"};
    
    /**
     * @return check box model for missing value replacement
     */
    static SettingsModelString createMismatchOption() {
        return new SettingsModelString("mismatch_option", 
                REPL_OPTIONS[1]);
    }
    
    /**
     * @return check box model for missing value replacement
     */
    static SettingsModelString createMatchOption() {
        return new SettingsModelString("match_option", 
                REPL_OPTIONS[0]);
    }
    
    /**
     * @return settings model for replacement value
     */
    static SettingsModelString createMatchValue() {
        SettingsModelString model = 
            new SettingsModelString("match_value", "TRUE");
        model.setEnabled(false);
        return model;
    }
    
    /**
     * @return settings model for replacement value
     */
    static SettingsModelString createMismatchValue() {
        SettingsModelString model = 
            new SettingsModelString("mismatch_value", "FALSE");
        model.setEnabled(false);
        return model;
    }
    
    /**
     * @return settings model for new column
     */
    static SettingsModelString createNewColumnName() {
        return new SettingsModelString("new_col_name", "compare_result");
    }
    
    /**
     * Comparator methods.
     */
    public enum ComparatorMethod {
        /**
         * 
         */
        EQUAL {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean compare(final DataCell cell1, final DataCell cell2) {
                return compareValue(cell1, cell2) == 0;
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "==";
            }
        },
        /**
         * 
         */
        NOTEQUAL {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean compare(final DataCell cell1, final DataCell cell2) {
                return compareValue(cell1, cell2) != 0;
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "!=";
            }
        },
        /**
         * 
         */
        LESS {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean compare(final DataCell cell1, final DataCell cell2) {
                return compareValue(cell1, cell2) < 0;
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "<";
            }
        },
        /**
         * 
         */
        LESSEQUAL {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean compare(final DataCell cell1, final DataCell cell2) {
                return compareValue(cell1, cell2) <= 0;
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "<=";
            }
        },
        /**
         * 
         */
        GREATER {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean compare(final DataCell cell1, final DataCell cell2) {
                return compareValue(cell1, cell2) > 0;
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return ">";
            }
        },
        /**
         * 
         */
        GREATEREQUAL {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean compare(final DataCell cell1, final DataCell cell2) {
                return compareValue(cell1, cell2) >= 0;
            }
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return ">=";
            }
        };
        /**
         * Compares both cell values using the comparator method.
         * @param cell1 left column value
         * @param cell2 right column value
         * @return true, if both cell are equal with respect to the comparator
         */
        abstract boolean compare(final DataCell cell1,
                final DataCell cell2);
        /**
         * Compares both cell values using the most common's super type
         * comparator.
         * @param cell1 first cell value
         * @param cell2 second cell value
         * @return true, if both are equal
         */
        int compareValue(final DataCell cell1, final DataCell cell2) {
            DataType type = DataType.getCommonSuperType(cell1.getType(), 
                    cell2.getType());
            DataValueComparator comp = type.getComparator();
            return (comp == null ? 0 : comp.compare(cell1, cell2));
        };
        private static final List<String> COMPARATOR_LIST = 
            new ArrayList<String>();
        static {
            for (ComparatorMethod method : ComparatorMethod.values()) {
                COMPARATOR_LIST.add(method.toString());
            }
        }
        /**
         * Returns the comparator method, specified within the 
         * {@link ComparatorMethod#toString()}.
         * @param str comparator method's toString return value
         * @return a comparator method
         */
        public final static ComparatorMethod getMethod(final String str) {
            for (ComparatorMethod method : ComparatorMethod.values()) {
                if (method.toString().equals(str)) {
                    return method;
                }
            }
            assert false;
            return null;
        }
    };
    
}
