/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   23.01.2012 (hofer): created
 */
package org.knime.base.node.jsnippet;

import java.util.ArrayList;

import org.knime.base.node.jsnippet.JavaFieldSettings.InCol;
import org.knime.base.node.jsnippet.JavaFieldSettings.InVar;
import org.knime.base.node.jsnippet.JavaFieldSettings.OutCol;
import org.knime.base.node.jsnippet.JavaFieldSettings.OutVar;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * A List of JavaFieldSettings.
 *
 * @author Heiko Hofer
 * @param <T> the JavaFieldSettings type.
 */
@SuppressWarnings("serial")
public class JavaFieldSettingsList<T extends JavaFieldSettings>
        extends ArrayList<T> implements Iterable<T> {
    private static final String ARRAY_SIZE = "array-size";
    private JavaFieldSettingsFactory<T> m_factory;

    /**
     * @param factory A factory to create empty instances.
     */
    public JavaFieldSettingsList(final JavaFieldSettingsFactory<T> factory) {
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
                settings.loadSettingsForDialog(
                        config.getConfig(Integer.toString(i)));
            } catch (InvalidSettingsException e) {
                throw new IllegalStateException(e);
            }
            add(settings);
        }
    }


    /**
     * Factory to create JavaFieldSettings.
     * @param <T> The JavaFieldSettings type to create by this factory.
     */
    abstract static class JavaFieldSettingsFactory<T extends
            JavaFieldSettings> {
        /**
         * Create empty JavaFieldSettings object.
         * @return new JavaFieldSettings object
         */
        abstract T createJavaFieldSettings();
    }

    /**
     * A wrapper for a list of fields in the java snippet that represents
     * input columns.
     */
    @SuppressWarnings("serial")
    public static class InColList extends JavaFieldSettingsList<InCol> {
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
    @SuppressWarnings("serial")
    public static class InVarList extends JavaFieldSettingsList<InVar> {
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
    @SuppressWarnings("serial")
    public static class OutColList extends JavaFieldSettingsList<OutCol> {
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
    @SuppressWarnings("serial")
    public static class OutVarList extends JavaFieldSettingsList<OutVar> {
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
