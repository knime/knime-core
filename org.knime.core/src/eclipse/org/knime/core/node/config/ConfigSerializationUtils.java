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
 *   05.09.2014 (Marcel Hanser): created
 */
package org.knime.core.node.config;

import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.util.EnumSet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Collection of convenience methods that help to save or load Enum [and other Objects] to {@link ConfigWO} or
 * {@link ConfigRO} objects, this also includes {@link NodeSettingsRO} and {@link NodeSettingsWO}.
 *
 * @author Marcel Hanser
 * @since 2.11
 */
public final class ConfigSerializationUtils {
    /**
     * Utility class.
     */
    private ConfigSerializationUtils() {

    }

    /**
     * Reads an enum of type <code>T</code> under the given key or returns the default if the key does not exist or the
     * stored value is <code>null</code> or not compatible with the enum names defined by the enum type <code>T</code>.
     * This is i.e. the reverse method of {@link #addEnum(ConfigWO, String, Enum)}.
     *
     * @param reado to read the enum from
     * @param key the key
     * @param defaultEnum default value
     * @param <T> the enum type
     * @return enum backed in the given reado object or the default value
     * @throws NullPointerException if any of the given attributes is <code>null</code>
     */
    public static <T extends Enum<T>> T getEnum(final ConfigRO reado, final String key, final T defaultEnum) {
        try {
            return Enum.valueOf(defaultEnum.getDeclaringClass(), reado.getString(key, ""));
        } catch (IllegalArgumentException | NullPointerException e) {
            return defaultEnum;
        }
    }

    /**
     * Reads an enum of type <code>T</code> under the given key. This is i.e. the reverse method of
     * {@link #addEnum(ConfigWO, String, Enum)}.
     *
     * @param reado to read the enum from
     * @param key the key
     * @param clazz class of the enum
     * @param <T> the enum type
     * @return enum backed in the given reado object or the default value
     * @throws InvalidSettingsException if there exists no entry for the given key or the value is <code>null</code> or
     *             not compatible with the enum names defined by the enum type <code>T</code>.
     * @throws NullPointerException if any of the given attributes is <code>null</code>
     */
    public static <T extends Enum<T>> T getEnum(final ConfigRO reado, final String key, final Class<T> clazz)
        throws InvalidSettingsException {
        try {
            return Enum.valueOf(clazz, reado.getString(key));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidSettingsException("Enum name not set");
        }
    }

    /**
     * Reads an {@link EnumSet} of type <code>T</code> under the given key. This is i.e. the reverse method of
     * {@link #addEnumSet(ConfigWO, String, EnumSet)}.
     *
     * @param reado to read the enum from
     * @param key the key
     * @param clazz the enum class
     * @param <T> the enum type
     * @return enum backed in the given reado object or the default value
     * @throws InvalidSettingsException if the key is not set or if any string backed in the model is not a valid name
     *             of the given enum type <code>T</code>
     * @throws NullPointerException if any of the given attributes is <code>null</code>
     */
    public static <T extends Enum<T>> EnumSet<T>
        getEnumSet(final ConfigRO reado, final String key, final Class<T> clazz) throws InvalidSettingsException {
        EnumSet<T> toReturn = EnumSet.noneOf(clazz);
        String[] stringArray =
            checkSettingNotNull(reado.getStringArray(key), "Key: '%s' contains null enum array", key);
        for (String en : stringArray) {
            try {
                toReturn.add(Enum.valueOf(clazz, en));
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException("Enum name not valid: " + en);
            }
        }
        return toReturn;
    }

    /**
     * Adds the value returned by {@link Enum#name()} as string to the given {@link ConfigWO}.
     *
     * @param writeO to write the enum to
     * @param key under which the enum should be written
     * @param enumToAdd to write
     * @throws NullPointerException if any of the given attributes is <code>null</code>
     */
    public static void addEnum(final ConfigWO writeO, final String key, final Enum<?> enumToAdd) {
        writeO.addString(key, enumToAdd.name());
    }

    /**
     * Adds the values returned by {@link Enum#name()} as a string array to the given {@link ConfigWO}.
     *
     * @param writeO to write the enum to
     * @param key under which the enum should be written
     * @param enumsToAdd to write
     * @throws NullPointerException if any of the given attributes is <code>null</code>
     */
    public static void addEnumSet(final ConfigWO writeO, final String key, final EnumSet<?> enumsToAdd) {
        String[] toWrite = new String[enumsToAdd.size()];
        int index = 0;
        for (Enum<?> curEnum : enumsToAdd) {
            toWrite[index++] = curEnum.name();
        }
        writeO.addStringArray(key, toWrite);
    }
}