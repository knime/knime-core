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
package org.knime.base.node.jsnippet.util.field;

import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.knime.base.node.jsnippet.JavaSnippet;
import org.knime.base.node.jsnippet.ui.InFieldsTableModel;
import org.knime.base.node.jsnippet.ui.OutFieldsTableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;

/**
 * Settings for a java snippet field. The field can represent input and output columns or flow variables.
 * <p>
 *
 * JavaField is can be seen as a model for the mapping of KNIME input/output (Column or FlowVariable) to a java field in
 * the Java Snippet.
 *
 * Settings are never checked for validity (in a semantic sense), but only for completeness (in a syntactic sense).
 * Model validation is done in the following places:
 *
 * <ul>
 * <li>{@link JavaSnippet#validateSettings}</li>
 * <li>{@link InFieldsTableModel#isValidValue}</li>
 * <li>{@link OutFieldsTableModel#isValidValue}</li>
 * </ul>
 *
 * <b>This class is not meant as public API and may change.</br>
 * @author Heiko Hofer
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 2.12
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class JavaField {

    /**
     * The KNIME type of an output field.
     *
     * @author Heiko Hofer
     */
    public enum FieldType {
            /** The field defines a column in a DataTable. */
            Column,
            /** The field defines a Flow Variable. */
            FlowVariable
    }

    /* Config Key for KNIME DataType or FlowVariable.Type */
    static final String KNIME_TYPE = "Type";

    /* Config Key for the name of the column or flow var */
    static final String KNIME_NAME = "Name";

    /* Config Key for java field name */
    static final String JAVA_NAME = "JavaName";

    /* Config Key for java type */
    static final String JAVA_TYPE = "JavaType";

    /* Config Key for the bundle which provided the java type */
    static final String JAVA_TYPE_PROVIDER = "JavaTypeProviderBundle";

    /* Config Key for the converter factory id */
    static final String CONV_FACTORY = "ConverterFactory";

    /**
     * The name of the KNIME flow variable or column
     */
    protected String m_knimeName;

    /**
     * The name of the java field
     */
    protected String m_javaName;

    /**
     * The name of the java type
     */
    protected String m_javaTypeName;

    /**
     * The type of the java field
     */
    protected Class<?> m_javaType;

    /**
     * The name of bundle the java type originated from. Can be "".
     */
    protected String m_bundleName;

    /**
     * Name of the flow variable or column
     *
     * @return the knimeName
     */
    public String getKnimeName() {
        return m_knimeName;
    }

    /**
     * Set the name of the flow variable or column
     *
     * @param knimeName the knimeName to set
     */
    public void setKnimeName(final String knimeName) {
        m_knimeName = knimeName;
    }

    /**
     * Name of the java field
     *
     * @return the javaName
     */
    public String getJavaName() {
        return m_javaName;
    }

    /**
     * Set the name of the java field
     *
     * @param javaName the javaName to set
     */
    public void setJavaName(final String javaName) {
        m_javaName = javaName;
    }

    /**
     * Check whether this is a Column.
     *
     * @return <code>true</code> if this field represents a table column, <code>false</code> if it represents a flow
     *         variable.
     */
    public abstract FieldType getFieldType();

    /**
     * Whether this is an input field.
     *
     * @return <code>true</code> if this is an input field.
     */
    public abstract boolean isInput();

    /**
     * Whether this is an output field.
     *
     * @return <code>true</code> if this is an output field.
     */
    public boolean isOutput() {
        return !isInput();
    }

    /**
     * The type of the java field
     *
     * @return the javaType, may be <code>null</code>
     */
    public Class<?> getJavaType() {
        return m_javaType;
    }

    /**
     * Name of the java type in case loading failed.
     *
     * @return Name of the java type
     */
    public String getJavaTypeName() {
        return m_javaTypeName;
    }

    /**
     * Saves KNIME name (variable or column name), java field name and java type name.
     *
     * @param config To save to.
     */
    public void saveSettings(final Config config) {
        config.addString(KNIME_NAME, m_knimeName);
        config.addString(JAVA_NAME, m_javaName);

        final Class<?> javaType = getJavaType();
        if (javaType == null) {
            // The case when no match could be found for converter factory id.
            // Just save the loaded settings as they are. The user may want to find a
            // replacement via dialog.
            config.addString(JAVA_TYPE, m_javaTypeName);
            config.addString(JAVA_TYPE_PROVIDER, m_bundleName);
        } else {
            config.addString(JAVA_TYPE, javaType.getName());

            // Add the bundle providing this java type in case it is not installed in the Application loading these settings.
            if (javaType.getClassLoader() instanceof ModuleClassLoader) {
                config.addString(JAVA_TYPE_PROVIDER, ((ModuleClassLoader)javaType.getClassLoader()).getBundle().getSymbolicName());
            }
        }

    }

    /**
     * Loads KNIME name (variable or column name), java field name, java type name and the name of the bundle it
     * originated from.
     *
     * @param config To load from.
     * @throws InvalidSettingsException If incomplete or wrong.
     */
    public void loadSettings(final Config config) throws InvalidSettingsException {
        m_knimeName = config.getString(KNIME_NAME);
        m_javaName = config.getString(JAVA_NAME);
        m_javaTypeName = config.getString(JAVA_TYPE, "");
        m_bundleName = config.getString(JAVA_TYPE_PROVIDER, "");

        if (m_javaTypeName.isEmpty()) {
            // While the converterFactoryId may still be valid, the java type is always stored
            // for redundancy. If it is missing, something must be wrong with the settings.
            throw new InvalidSettingsException("Java type setting was empty in a java field configuration.");
        }
    }

    /**
     * Load m_javaType from m_javaTypeName.
     *
     * @return The loaded class (m_javaType)
     * @throws InvalidSettingsException
     */
    protected Class<?> loadJavaType() throws InvalidSettingsException {
        try {
            m_javaType = Class.forName(m_javaTypeName);
        } catch (ClassNotFoundException e) {
            final StringBuilder errorString =
                new StringBuilder("Java type \"").append(m_javaTypeName).append("\" was not found.");

            if (!m_bundleName.isEmpty()) {
                errorString.append("(Was provided by \"").append(m_bundleName).append("\")");
            }
            throw new InvalidSettingsException(errorString.toString(), e);
        }

        return m_javaType;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof JavaField) {
            final JavaField other = (JavaField)obj;

            if (other.isInput() != isInput()) {
                return false;
            }

            if (other.getFieldType() != getFieldType()) {
                return false;
            }

            if (other.getJavaName() != getJavaName()) {
                return false;
            }

            if (other.getKnimeName() != getKnimeName()) {
                return false;
            }

            if (!other.getJavaType().equals(getJavaType())) {
                return false;
            }
        }

        return false;
    }

}
