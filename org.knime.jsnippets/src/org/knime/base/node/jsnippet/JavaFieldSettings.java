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

import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * Settings for java snippet field. The field can represent input and output
 * columns or flow variables.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("rawtypes")
public class JavaFieldSettings {
    private static final String KNIME_NAME = "Name";
    private static final String JAVA_NAME = "JavaName";
    private static final String JAVA_TYPE = "JavaType";


    private String m_knimeName;
    private String m_javaName;
    private Class m_javaType;


	/**
	 * @return the knimeName
	 */
	public String getKnimeName() {
		return m_knimeName;
	}

	/**
	 * @param knimeName the knimeName to set
	 */
	public void setKnimeName(final String knimeName) {
		m_knimeName = knimeName;
	}

	/**
	 * @return the javaName
	 */
	public String getJavaName() {
		return m_javaName;
	}

	/**
	 * @param javaName the javaName to set
	 */
	public void setJavaName(final String javaName) {
		m_javaName = javaName;
	}

	/**
	 * @return the javaType
	 */
	public Class getJavaType() {
		return m_javaType;
	}

	/**
	 * @param javaType the javaType to set
	 */
	public void setJavaType(final Class javaType) {
		m_javaType = javaType;
	}

	/** Saves current parameters to settings object.
     * @param config To save to.
     */
    public void saveSettings(final Config config) {
        config.addString(KNIME_NAME, m_knimeName);
        config.addString(JAVA_NAME, m_javaName);
        config.addString(JAVA_TYPE, m_javaType.getName());
    }

    /** Loads parameters in NodeModel.
     * @param config To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettings(final Config config)
            throws InvalidSettingsException {
        m_knimeName = config.getString(KNIME_NAME);
        m_javaName = config.getString(JAVA_NAME);
        try {
            m_javaType = Class.forName(config.getString(JAVA_TYPE, null));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }


    /** Loads parameters in Dialog.
     * @param config To load from.
     */
    public void loadSettingsForDialog(final Config config) {
        m_knimeName = config.getString(KNIME_NAME, null);
        m_javaName = config.getString(JAVA_NAME, null);
        try {
            m_javaType = Class.forName(config.getString(JAVA_TYPE, ""));
        } catch (ClassNotFoundException e) {
            m_javaType = null;
        }
    }

    /**
     * Fields representing knime data columns.
     *
     * @author Heiko Hofer
     */
    public static class JavaColumnFieldSettings extends JavaFieldSettings {
        private static final String KNIME_TYPE = "Type";

        private DataType m_knimeType;


        /**
         * @return the knimeType
         */
        public DataType getKnimeType() {
            return m_knimeType;
        }

        /**
         * @param knimeType the knimeType to set
         */
        public void setKnimeType(final DataType knimeType) {
            m_knimeType = knimeType;
        }

        /** Saves current parameters to settings object.
         * @param config To save to.
         */
        @Override
        public void saveSettings(final Config config) {
            super.saveSettings(config);
            config.addDataType(KNIME_TYPE, m_knimeType);
        }

        /** Loads parameters in NodeModel.
         * @param config To load from.
         * @throws InvalidSettingsException If incomplete or wrong.
         */
        @Override
        public void loadSettings(final Config config)
                throws InvalidSettingsException {
            super.loadSettings(config);
            m_knimeType = config.getDataType(KNIME_TYPE);
        }


        /** Loads parameters in Dialog.
         * @param config To load from.
         */
        @Override
        public void loadSettingsForDialog(final Config config) {
            super.loadSettingsForDialog(config);
            m_knimeType = config.getDataType(KNIME_TYPE, null);
        }
    }

    /**
     * A field in the java snippet representing flow variable.
     *
     * @author Heiko Hofer
     */
    public static class JavaFlowVarFieldSettings extends JavaFieldSettings {
        private static final String KNIME_TYPE = "Type";

        private Type m_knimeType;


        /**
         * @return the knimeType
         */
        public Type getKnimeType() {
            return m_knimeType;
        }

        /**
         * @param knimeType the knimeType to set
         */
        public void setKnimeType(final Type knimeType) {
            m_knimeType = knimeType;
        }

        /** Saves current parameters to settings object.
         * @param config To save to.
         */
        @Override
        public void saveSettings(final Config config) {
            super.saveSettings(config);
            config.addString(KNIME_TYPE, m_knimeType.toString());
        }

        /** Loads parameters in NodeModel.
         * @param config To load from.
         * @throws InvalidSettingsException If incomplete or wrong.
         */
        @Override
        public void loadSettings(final Config config)
                throws InvalidSettingsException {
            super.loadSettings(config);
            String typeName = config.getString(KNIME_TYPE);
            m_knimeType = Type.valueOf(typeName);
        }


        /** Loads parameters in Dialog.
         * @param config To load from.
         */
        @Override
        public void loadSettingsForDialog(final Config config) {
            super.loadSettingsForDialog(config);
            String typeName = config.getString(KNIME_TYPE, null);
            m_knimeType = null != typeName ? Type.valueOf(typeName) : null;
        }
    }

    /**
     * A marker class for a field in the java snippet that represents an
     * input column.
     * @author Heiko Hofer
     */
    public static class InCol extends JavaColumnFieldSettings {
		// just a marker, nothing to add.
    }


    /**
     * A marker class for a field in the java snippet that represents an
     * input variable.
     * @author Heiko Hofer
     */
    public static class InVar extends JavaFlowVarFieldSettings {
		// just a marker, nothing to add.
    }

    /**
     * A class for a field in the java snippet that represents an
     * output column.
     * @author Heiko Hofer
     */
    public static class OutCol extends JavaColumnFieldSettings {
        private static final String REPLACE_EXISTING = "replaceExisting";


        private boolean m_replaceExisting;

        /**
         * Create an instance.
         */
        public OutCol() {
            m_replaceExisting = false;
        }

        /**
		 * @return the replaceExisting
		 */
		public boolean getReplaceExisting() {
			return m_replaceExisting;
		}

		/**
		 * @param replaceExisting the replaceExisting to set
		 */
		public void setReplaceExisting(final boolean replaceExisting) {
			m_replaceExisting = replaceExisting;
		}



		/** Saves current parameters to settings object.
         * @param config To save to.
         */
        @Override
        public void saveSettings(final Config config) {
            super.saveSettings(config);
            config.addBoolean(REPLACE_EXISTING, m_replaceExisting);
        }

        /** Loads parameters in NodeModel.
         * @param config To load from.
         * @throws InvalidSettingsException If incomplete or wrong.
         */
        @Override
        public void loadSettings(final Config config)
                throws InvalidSettingsException {
            super.loadSettings(config);
            m_replaceExisting = config.getBoolean(REPLACE_EXISTING);
        }


        /** Loads parameters in Dialog.
         * @param config To load from.
         */
        @Override
        public void loadSettingsForDialog(final Config config) {
            super.loadSettingsForDialog(config);
            m_replaceExisting = config.getBoolean(REPLACE_EXISTING, false);
        }
    }

    /**
     * A marker class for a field in the java snippet that represents an
     * output variable.
     * @author Heiko Hofer
     */
    public static class OutVar extends JavaFlowVarFieldSettings {
        private static final String REPLACE_EXISTING = "replaceExisting";

        private boolean m_replaceExisting;

        /**
         * Create an instance.
         */
        public OutVar() {
            m_replaceExisting = false;
        }

        /**
         * @return the replaceExisting
         */
        public boolean getReplaceExisting() {
            return m_replaceExisting;
        }

        /**
         * @param replaceExisting the replaceExisting to set
         */
        public void setReplaceExisting(final boolean replaceExisting) {
            m_replaceExisting = replaceExisting;
        }



        /** Saves current parameters to settings object.
         * @param config To save to.
         */
        @Override
        public void saveSettings(final Config config) {
            super.saveSettings(config);
            config.addBoolean(REPLACE_EXISTING, m_replaceExisting);
        }

        /** Loads parameters in NodeModel.
         * @param config To load from.
         * @throws InvalidSettingsException If incomplete or wrong.
         */
        @Override
        public void loadSettings(final Config config)
                throws InvalidSettingsException {
            super.loadSettings(config);
            m_replaceExisting = config.getBoolean(REPLACE_EXISTING);
        }


        /** Loads parameters in Dialog.
         * @param config To load from.
         */
        @Override
        public void loadSettingsForDialog(final Config config) {
            super.loadSettingsForDialog(config);
            m_replaceExisting = config.getBoolean(REPLACE_EXISTING, false);
        }
    }

}
