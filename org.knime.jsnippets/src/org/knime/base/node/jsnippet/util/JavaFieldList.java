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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *
 * History
 *   23.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.util;

import java.util.ArrayList;

import org.knime.base.node.jsnippet.util.field.InCol;
import org.knime.base.node.jsnippet.util.field.InVar;
import org.knime.base.node.jsnippet.util.field.JavaField;
import org.knime.base.node.jsnippet.util.field.OutCol;
import org.knime.base.node.jsnippet.util.field.OutVar;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * A List of JavaField.
 * <p>This class might change and is not meant as public API.
 *
 * @author Heiko Hofer
 * @param <T> the JavaField type.
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("serial")
public class JavaFieldList<T extends JavaField> extends ArrayList<T> {
    private static final String ARRAY_SIZE = "array-size";
    private JavaFieldSettingsFactory<T> m_factory;

    /**
     * @param factory A factory to create empty instances.
     */
    public JavaFieldList(final JavaFieldSettingsFactory<T> factory) {
        m_factory = factory;
    }

    /** Saves current parameters to settings object.
     * @param config To save to.
     */
    public void saveSettings(final Config config) {
        config.addInt(ARRAY_SIZE, size());
        for (int i = 0; i < size(); i++) {
            get(i).saveSettings(
                    config.addConfig(Integer.toString(i)));
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
            T settings = m_factory.createJavaFieldSettings();
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
            T settings = m_factory.createJavaFieldSettings();
            try {
                settings.loadSettings(config.getConfig(Integer.toString(i)));
                add(settings);
            } catch (InvalidSettingsException e) {
                // should never happen.
                throw new IllegalStateException("Integer key for config.getConfig() should always be valid, but was not.", e);
            }
        }
    }

    /**
     * Factory to create JavaField.
     * @param <T> The JavaField type to create by this factory.
     */
    abstract static class JavaFieldSettingsFactory<T extends
            JavaField> {
        /**
         * Create empty JavaField object.
         * @return new JavaField object
         */
        abstract T createJavaFieldSettings();
    }

    /**
     * A wrapper for a list of fields in the java snippet that represents
     * input columns.
     */
    public static class InColList extends JavaFieldList<InCol> {
        /** Create an empty instance. */
        public InColList() {
            super(new JavaFieldSettingsFactory<InCol>() {

                @Override
                InCol createJavaFieldSettings() {
                    return new InCol();
                }
            });
        }
    }

    /**
     * A wrapper for a list of fields in the java snippet that represents
     * input flow variables.
     */
    public static class InVarList extends JavaFieldList<InVar> {
        /** Create an empty instance. */
        public InVarList() {
            super(new JavaFieldSettingsFactory<InVar>() {

                @Override
                InVar createJavaFieldSettings() {
                    return new InVar();
                }
            });
        }
    }

    /**
     * A wrapper for a list of fields in the java snippet that represents
     * output columns.
     */
    public static class OutColList extends JavaFieldList<OutCol> {
        /** Create an empty instance. */
        public OutColList() {
            super(new JavaFieldSettingsFactory<OutCol>() {

                @Override
                OutCol createJavaFieldSettings() {
                    return new OutCol();
                }
            });
        }
    }

    /**
     * A wrapper for a list of fields in the java snippet that represents
     * output flow variables.
     */
    public static class OutVarList extends JavaFieldList<OutVar> {
        /** Create an empty instance. */
        public OutVarList() {
            super(new JavaFieldSettingsFactory<OutVar>() {

                @Override
                OutVar createJavaFieldSettings() {
                    return new OutVar();
                }
            });
        }
    }

}
