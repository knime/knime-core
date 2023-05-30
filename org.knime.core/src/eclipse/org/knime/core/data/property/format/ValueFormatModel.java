/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   22 May 2023 (carlwitt): created
 */
package org.knime.core.data.property.format;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.knime.core.data.DataValue;
import org.knime.core.data.property.VisualModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * TODO might be replaced with/moved to core-ui code (e.g., DataValueTextRenderer)
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 */
public interface ValueFormatModel extends VisualModel {

    /**
     * @param dataCell holds the value to display
     * @return the html representation for the given data cell.
     */
    String getHTML(DataValue dataCell);

    abstract class ValueFormatModelSerializer<T extends ValueFormatModel> {

        public abstract void save(final T model, final ConfigWO config);

        public abstract T load(final ConfigRO config) throws InvalidSettingsException;

        @SuppressWarnings("unchecked")
        Class<T> getModelClass() {
             for (Type type : ConvenienceMethods.getAllGenericInterfaces(getClass())) {
                if (type instanceof ParameterizedType) {
                    Type rawType = ((ParameterizedType) type).getRawType();
                    Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
                    if (ValueFormatModelSerializer.class == rawType) {
                        if (typeArgument instanceof Class) {
                            return (Class<T>)typeArgument;
                        } else if (typeArgument instanceof ParameterizedType) { // e.g. ImgPlusCell<T>
                            return (Class<T>)((ParameterizedType) typeArgument).getRawType();
                        }
                    }
                }
            }

            for (Type type : ConvenienceMethods.getAllGenericSuperclasses(getClass())) {
                if (type instanceof ParameterizedType) {
                    Type typeArgument = ((ParameterizedType)type).getActualTypeArguments()[0];
                    if (ValueFormatModel.class.isAssignableFrom((Class<?>)typeArgument)) {
                        if (typeArgument instanceof Class) {
                            return (Class<T>)typeArgument;
                        } else if (typeArgument instanceof ParameterizedType) {
                            return (Class<T>)((ParameterizedType) typeArgument).getRawType();
                        }
                    }
                }
            }

            try {
                Class<T> c = (Class<T>)getClass().getMethod("load", ConfigRO.class).getGenericReturnType();
                if (!ValueFormatModel.class.isAssignableFrom(c) || ((c.getModifiers() & Modifier.ABSTRACT) != 0)) {
                    NodeLogger.getLogger(getClass())
                        .coding(getClass().getName() + " does not use generics properly, the type of the created value "
                            + "format model is '" + c.getName() + "'. Please fix your implementation by specifying a "
                            + "non-abstract value format type in the extended ValueFormatModelSerializer class.");
                    return (Class<T>)ValueFormatModel.class;
                } else {
                    return c;
                }
            } catch (NoSuchMethodException ex) {
                // this is not possible
                throw new AssertionError("Someone removed the 'load' method from this class");
            }
        }
    }

}
