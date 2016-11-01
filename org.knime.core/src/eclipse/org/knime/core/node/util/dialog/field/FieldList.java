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
 *   25 Oct 2016 (albrecht): created
 */
package org.knime.core.node.util.dialog.field;

import java.util.ArrayList;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.util.dialog.field.ColumnField.InColumnField;
import org.knime.core.node.util.dialog.field.FlowVariableField.InFlowVariableField;


/**
 * A list of an {@link AbstractField}.
 *
 * <p>This class might change and is not meant as public API.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @author Heiko Hofer
 * @param <T> the JavaField type.
 * @since 3.3
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("serial")
public class FieldList<T extends AbstractField> extends ArrayList<T> {

    private static final String ARRAY_SIZE = "array-size";
    private FieldSettingsFactory<T> m_factory;

    /**
     * @param factory A factory to create empty instances.
     */
    public FieldList(final FieldSettingsFactory<T> factory) {
        m_factory = factory;
    }

    /** Saves current parameters to settings object.
     * @param config To save to.
     */
    public void saveSettings(final Config config) {
        config.addInt(ARRAY_SIZE, size());
        for (int i = 0; i < size(); i++) {
            get(i).saveSettings(config.addConfig(Integer.toString(i)));
        }
    }

    /** Loads parameters in NodeModel.
     * @param config To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettings(final Config config)
            throws InvalidSettingsException {
        int size = config.getInt(ARRAY_SIZE, -1);
        clear();
        for (int i = 0; i < size; i++) {
            T settings = m_factory.createFieldSettings();
            settings.loadSettings(config.getConfig(Integer.toString(i)));
            add(settings);
        }
    }

    /** Loads parameters in Dialog.
     * @param config To load from.
     */
    public void loadSettingsForDialog(final Config config) {
        int size = config.getInt(ARRAY_SIZE, -1);
        clear();
        for (int i = 0; i < size; i++) {
            T settings = m_factory.createFieldSettings();
            try {
                settings.loadSettingsForDialog(
                        config.getConfig(Integer.toString(i)));
            } catch (InvalidSettingsException e) {
                throw new IllegalStateException(e);
            }
            add(settings);
        }
    }

    /**
     * Factory to create AbstractField.
     * @param <T> The AbstractField type to create by this factory.
     */
    abstract static class FieldSettingsFactory<T extends AbstractField> {
        /**
         * Create empty JavaField object.
         * @return new JavaField object
         */
        abstract T createFieldSettings();
    }

    /**
     * A wrapper for a list of fields that represents input columns.
     */
    public static class InColumnList extends FieldList<InColumnField> {
        /** Create an empty instance. */
        public InColumnList() {
            super(new FieldSettingsFactory<InColumnField>() {

                @Override
                InColumnField createFieldSettings() {
                    return new InColumnField();
                }
            });
        }
    }

    /**
     * A wrapper for a list of fields that represents input flow variables.
     */
    public static class InFlowVariableList extends FieldList<InFlowVariableField> {
        /** Create an empty instance. */
        public InFlowVariableList() {
            super(new FieldSettingsFactory<InFlowVariableField>() {

                @Override
                InFlowVariableField createFieldSettings() {
                    return new InFlowVariableField();
                }
            });
        }
    }

    /**
     * A wrapper for a list of fields that represents output columns.
     */
    public static class OutColumnList extends FieldList<OutColumnField> {
        /** Create an empty instance. */
        public OutColumnList() {
            super(new FieldSettingsFactory<OutColumnField>() {

                @Override
                OutColumnField createFieldSettings() {
                    return new OutColumnField();
                }
            });
        }
    }

    /**
     * A wrapper for a list of fields that represents output flow variables.
     */
    public static class OutFlowVariableList extends FieldList<OutFlowVariableField> {
        /** Create an empty instance. */
        public OutFlowVariableList() {
            this(false);
        }
        /** Create an empty instance.
         * @param defineDefaultValue true, if the list contains of {@link DefaultOutFlowVariableField}
         */
        public OutFlowVariableList(final boolean defineDefaultValue) {
            super(new FieldSettingsFactory<OutFlowVariableField>() {

                @Override
                OutFlowVariableField createFieldSettings() {
                    if (defineDefaultValue) {
                        return new DefaultOutFlowVariableField();
                    } else {
                        return new OutFlowVariableField();
                    }
                }
            });
        }
    }

}
