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
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.io.extractsysprop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultRowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ReadSysPropertyConfiguration {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(ReadSysPropertyConfiguration.class);

    private boolean m_isExtractAllProps;
    private boolean m_failIfSomeMissing;
    private String[] m_selectedProps;
    /** @return the isExtractAllProps */


    boolean isExtractAllProps() {
        return m_isExtractAllProps;
    }
    /** @param isExtractAllProps the isExtractAllProps to set */
    void setExtractAllProps(final boolean isExtractAllProps) {
        m_isExtractAllProps = isExtractAllProps;
    }
    /** @return the failIfSomeMissing */
    boolean isFailIfSomeMissing() {
        return m_failIfSomeMissing;
    }
    /** @param failIfSomeMissing the failIfSomeMissing to set */
    void setFailIfSomeMissing(final boolean failIfSomeMissing) {
        m_failIfSomeMissing = failIfSomeMissing;
    }
    /** @return the selectedProps */
    String[] getSelectedProps() {
        return m_selectedProps;
    }
    /** @param selectedProps the selectedProps to set */
    void setSelectedProps(final String[] selectedProps) {
        m_selectedProps = selectedProps;
    }

    /** Saves current configuration to argument.
     * @param settings to save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean("isExtractAllProps", m_isExtractAllProps);
        if (!m_isExtractAllProps) {
            settings.addBoolean("failIfSomeMissing", m_failIfSomeMissing);
            settings.addStringArray("selectedProps", m_selectedProps);
        }
    }

    /** Loads configuration, fails if fields are missing/invalid.
     * @param settings to load from.
     * @throws InvalidSettingsException If that fails.
     */
    void loadSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_isExtractAllProps = settings.getBoolean("isExtractAllProps");
        if (!m_isExtractAllProps) {
            m_failIfSomeMissing = settings.getBoolean("failIfSomeMissing");
            m_selectedProps = settings.getStringArray("selectedProps");
            boolean isNothingSelected =
                m_selectedProps == null || m_selectedProps.length == 0;
            if (!m_failIfSomeMissing && isNothingSelected) {
                throw new InvalidSettingsException("Nothing selected");
            }
        }
    }

    /** Loads configuration, inits defaults if fields are missing/invalid.
     * @param settings to load from.
     */
    void loadSettingsNoFail(final NodeSettingsRO settings) {
        m_isExtractAllProps = settings.getBoolean("isExtractAllProps", true);
        if (!m_isExtractAllProps) {
            m_failIfSomeMissing =
                settings.getBoolean("failIfSomeMissing", false);
            Set<String> allPropKeys = readAllProps().keySet();
            String[] defSelProps = allPropKeys.toArray(
                    new String[allPropKeys.size()]);
            m_selectedProps =
                settings.getStringArray("selectedProps", defSelProps);
            boolean isNothingSelected =
                m_selectedProps == null || m_selectedProps.length == 0;
            if (!m_failIfSomeMissing && isNothingSelected) {
                m_selectedProps = defSelProps;
            }
        }
    }

    /** Read all java properties into map of Strings.
     * @return A new map of string containing all system properties.
     */
    static Map<String, String> readAllProps() {
        Map<String, String> result = new LinkedHashMap<String, String>();
        Properties properties = System.getProperties();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            Object key = e.getKey();
            Object val = e.getValue();
            if (key instanceof String && val instanceof String) {
                result.put((String)key, (String)val);
            } else {
                String keyT = key == null ? "null" : key.getClass().getName();
                String valT = val == null ? "null" : val.getClass().getName();
                LOGGER.warn("Ignoring sys property \"" + key + "\": not a "
                        + "<string,string> but <" + keyT + "," + valT + ">");
            }
        }
        return result;
    }


    /** Creates a result object for the current configuration.
     * @return A new result.
     * @throws InvalidSettingsException If any property is missing and the
     *         fail-on-missing flag is set.
     */
    Result createResult() throws InvalidSettingsException {
        Map<String, String> props;
//        InetAddress add = InetAddress.getLocalHost();
//        add.getCanonicalHostName();
        String message = null;
        if (isExtractAllProps()) {
            props = readAllProps();
        } else {
            props = new LinkedHashMap<String, String>();
            String[] selectedProps = getSelectedProps();
            boolean failIfSomeMissing = isFailIfSomeMissing();
            if (selectedProps == null || selectedProps.length == 0) {
                throw new InvalidSettingsException("No properties selected");
            }
            Properties sysProps = System.getProperties();
            List<String> ignored = new ArrayList<String>();
            for (String s : selectedProps) {
                String value = sysProps.getProperty(s);
                if (value == null && failIfSomeMissing) {
                    throw new InvalidSettingsException("System property \""
                            + s + "\" + not available in runtime environment");
                } else if (value == null) {
                    ignored.add(s);
                } else {
                    props.put(s, value);
                }
            }
            if (!ignored.isEmpty()) {
                message = "Ignoring selected system properties: "
                    + ignored.toString();
            }
        }
        return new Result(props, message);
    }

    /** Class representing a result. It contains a map containing the key-value
     * pairs and possibly a warning message that should be displayed after
     * execution.
     */
    static final class Result {

        private static final DataTableSpec SPEC = new DataTableSpec(
                new String[] {"Value"}, new DataType[] {StringCell.TYPE});

        private final String m_warningMessage;
        private final Map<String, String> m_props;

        private Result(final Map<String, String> props,
                final String warningMessage) {
            m_props = props;
            m_warningMessage = warningMessage;
        }

        /** @return the table */
        DataTable getTable() {
            final List<DataRow> rows = new ArrayList<DataRow>();
            for (Map.Entry<String, String> e : m_props.entrySet()) {
                rows.add(new DefaultRow(e.getKey(), e.getValue()));
            }
            return new DataTable() {

                /** {@inheritDoc} */
                public DataTableSpec getDataTableSpec() {
                    return SPEC;
                }

                /** {@inheritDoc} */
                public RowIterator iterator() {
                    return new DefaultRowIterator(rows);
                }
            };
        }

        /** @return the props */
        Map<String, String> getProps() {
            return m_props;
        }

        /** @return the warningMessage */
        String getWarningMessage() {
            return m_warningMessage;
        }

    }

}
