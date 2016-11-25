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
 *   Jan 8, 2016 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataType;
import org.knime.core.node.util.StringHistory;

/**
 * Cell editor used the SQL data type in the table
 *
 * @author Budi Yanto, KNIME.com
 */
class SQLTypeCellEditor extends DefaultCellEditor {

    private static final long serialVersionUID = 1L;

    private static final int STRING_HISTORY_MAX_ENTRY = 50;

    private static final String STRING_HISTORY_DELIMITER = "|";

    static final int NO_RELATED_COLUMN = -1;

    private final int m_relatedColumn;

    private String m_relatedKey;

    private String m_dbIdentifier;

    private final Map<String, SQLTypeComboBoxModel> m_models =
        new HashMap<>();

    private static String DEFAULT_MODEL_KEY = "modelKey";

    private JTable m_table;
    private int m_row;
    private int m_col;

    /**
     * Creates a new instance of SQLTypeCellEditor
     */
    SQLTypeCellEditor() {
        this(NO_RELATED_COLUMN);
    }

    /**
     * Creates a new instance of SQLTypeCellEditor
     *
     * @param relatedColumn
     */
    SQLTypeCellEditor(final int relatedColumn) {
        this(new JComboBox<String>(), relatedColumn);
    }

    /**
     * Creates a new instance of SQLTypeCellEditor
     *
     * @param comboBox
     * @param relatedColumn
     */
    private SQLTypeCellEditor(final JComboBox<String> comboBox, final int relatedColumn) {
        super(comboBox);
        initializeComboBox(comboBox);
        m_relatedColumn = relatedColumn;
        m_relatedKey = DEFAULT_MODEL_KEY;
    }

    /**
     * Initializes the underlying JComboBox used as cell editor
     *
     * @param comboBox
     */
    private void initializeComboBox(final JComboBox<String> comboBox) {
        comboBox.setEditable(true);

        comboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent ev) {
                if (ev.getActionCommand().equals("comboBoxEdited")) {
                    String elem = ((String)comboBox.getEditor().getItem()).trim();
                    m_table.setValueAt(elem, m_row, m_col);
                    addElement(m_relatedKey.toString(), elem);
                }
            }
        });

        comboBox.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                comboBox.getEditor().getEditorComponent().setEnabled(false);
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                comboBox.getEditor().getEditorComponent().setEnabled(true);
            }

            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {

            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
        final int row, final int column) {
        final ComboBoxModel<String> model = getModelForRow(table, row, value);
        @SuppressWarnings("unchecked")
        JComboBox<String> comboBox = (JComboBox<String>)getComponent();
        comboBox.setModel(model);
        comboBox.setSelectedItem(value);
        m_table = table;
        m_row = row;
        m_col = column;
        return comboBox;
    }

    /**
     * Get the model for the specified row
     * @param value
     */
    private ComboBoxModel<String> getModelForRow(final JTable table, final int row, final Object value) {
        if (m_relatedColumn == NO_RELATED_COLUMN) {
            return m_models.get(DEFAULT_MODEL_KEY);
        }

        //  Use the value from the related column to do the lookup for the model
        m_relatedKey = table.getModel().getValueAt(row, m_relatedColumn).toString();
        SQLTypeComboBoxModel model = m_models.get(m_relatedKey);
        if (model == null) {
            final Collection<String> options = new LinkedList<>();
            options.add(value.toString());
            model = new SQLTypeComboBoxModel(options);
            m_models.put(m_relatedKey, model);
        }
        return model;
    }

    /**
     * Adds a new user-defined SQL data type to the list retrieved using the given key
     *
     * @param modelKey key to retrieve the list from map
     * @param element the SQL data type to add
     */
    void addElement(final String modelKey, final String element) {
        if (m_models.get(modelKey) == null) {
            m_models.put(modelKey, new SQLTypeComboBoxModel());
        }
        m_models.get(modelKey).addElement(element);
    }

    /**
     * Adds a new user-defined SQL data type to the list retrieved using the default key
     *
     * @param element the SQL data type to add
     */
    void addElement(final String element) {
        validateMethod();
        addElement(DEFAULT_MODEL_KEY, element);
    }

    /**
     * Returns the SQL data type at specified index in the list retrieved using the given key
     *
     * @param modelKey key to retrieve the SQL data type list
     * @param index index of the SQL data type in the list
     * @return
     */
    String getElementAt(final String modelKey, final int index) {
        if (m_models.get(modelKey) == null) {
            return null;
        }
        return m_models.get(modelKey).getElementAt(index);
    }

    /**
     * Returns the SQL data type at specified index in the list retrieved using the default key
     *
     * @param index index of the SQL data type in the list
     * @return the SQL data type
     */
    String getElementAt(final int index) {
        validateMethod();
        return getElementAt(DEFAULT_MODEL_KEY, index);
    }

    String getSelectedItem() {
        validateMethod();
        return getSelectedItem(DEFAULT_MODEL_KEY);
    }

    String getSelectedItem(final String modelKey) {
        if(m_models.get(modelKey) == null) {
            return null;
        }
        return m_models.get(modelKey).getSelectedItem().toString();
    }

    /**
     * Returns the number of elements in the list retrieved using the given key
     *
     * @param modelKey key used to retrieve the list
     * @return the number of elements in the list
     */
    int getSize(final String modelKey) {
        if (m_models.get(modelKey) == null) {
            return -1;
        }
        return m_models.get(modelKey).getSize();
    }

    /**
     * Returns the number of elements in the list retrieved using the default key
     *
     * @return the number of elements in the list
     */
    int getSize() {
        validateMethod();
        return getSize(DEFAULT_MODEL_KEY);
    }

    /**
     * A helper method to verify that the default key can be used to retrieve the list of SQL data types from the map
     */
    private void validateMethod() {
        if (m_relatedColumn != NO_RELATED_COLUMN && m_models.size() > 1) {
            throw new IllegalArgumentException("there are more than 1 model.");
        }
    }

    /**
     * Save all SQL data types to StringHistory
     */
    void saveSettings() {
        String historyId = createHistoryId(m_dbIdentifier);
        StringHistory sh = StringHistory.getInstance(historyId, STRING_HISTORY_MAX_ENTRY);
        for (Entry<String, SQLTypeComboBoxModel> entry : m_models.entrySet()) {
            String history = entry.getKey() + STRING_HISTORY_DELIMITER;
            final SQLTypeComboBoxModel model = entry.getValue();
            for (int i = 0; i < model.getSize(); i++) {
                sh.add(history + model.getElementAt(i));
            }
        }
    }

    /**
     * Load SQL data types from StringHistory
     *
     * @param identifier identifier used to get the corresponding StringHistory
     */
    void loadSettings(final String identifier) {
        m_dbIdentifier = identifier;
        m_models.clear();

        // Load from StringHistory if it is available
        String historyId = createHistoryId(identifier);
        String[] data = StringHistory.getInstance(historyId, STRING_HISTORY_MAX_ENTRY).getHistory();
        for (String elem : data) {
            String[] history = elem.split("\\" + STRING_HISTORY_DELIMITER);
            addElement(history[0].trim(), history[1].trim());
        }

        // map is empty which means no history is available, thus load from default values
        if (m_models.size() < 1) {
            if (m_relatedColumn == NO_RELATED_COLUMN) {
                SQLTypeComboBoxModel model = new SQLTypeComboBoxModel(DBUtil.getSqlTypes(m_dbIdentifier));
                m_models.put(DEFAULT_MODEL_KEY, model);
            } else {
                for (Entry<DataType, Set<String>> entry : DBUtil.getSqlTypesMap(m_dbIdentifier).entrySet()) {
                    SQLTypeComboBoxModel model = new SQLTypeComboBoxModel(entry.getValue());
                    m_models.put(entry.getKey().toString(), model);
                }
            }
        }
    }

    /**
     * Creates a history id for StringHistory
     *
     * @param identifier identifier used to create the history id
     * @return a history id for StringHistory
     */
    private String createHistoryId(final String identifier) {
        return this.getClass().getCanonicalName() + "_" + identifier;
    }

    /**
     * Creates a new instance of SQLTypeComboBoxModel
     *
     * @author Budi Yanto, KNIME.com
     */
    private class SQLTypeComboBoxModel extends DefaultComboBoxModel<String> {

        private static final long serialVersionUID = 1L;

        /**
         * Creates a new instance of SQLTypeComboBoxModel
         */
        private SQLTypeComboBoxModel() {
            super();
        }

        /**
         * Creates a new instance of SQLTypeComboBoxModel
         *
         * @param elements elements used to initialize the SQLTypeComboBoxModel
         */
        private SQLTypeComboBoxModel(final Collection<String> elements) {
            for (String elem : elements) {
                addElement(elem);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addElement(final String anObject) {
            insertElementAt(anObject, 0);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void insertElementAt(String anObject, int index) {
            if (anObject != null && !StringUtils.isBlank(anObject)) {
                anObject = anObject.toLowerCase();
                if (getIndexOf(anObject) < 0) { // Object doesn't exist in the list
                    // Determine where to insert element to keep model in sorted order
                    for (index = 0; index < getSize(); index++) {
                        String elem = getElementAt(index);
                        if (elem.compareTo(anObject) > 0) {
                            break;
                        }
                    }
                    super.insertElementAt(anObject, index);
                }
                setSelectedItem(anObject);
            } else {
                // Select the first item in the list if the inserted object is null or empty
                setSelectedItem(getElementAt(0));
            }
        }
    }

}
