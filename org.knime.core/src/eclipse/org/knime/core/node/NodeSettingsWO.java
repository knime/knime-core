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
 * -------------------------------------------------------------------
 *
 * History
 *   12.07.2006 (gabriel): created
 */
package org.knime.core.node;

import org.knime.core.node.config.ConfigWO;

/**
 * Write-only <code>NodeSettingsWO</code> interface.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public interface NodeSettingsWO extends ConfigWO {

    /**
     * Creates new <code>NodeSettingsWO</code> object for the given key and
     * returns it.
     * @param key The identifier for the given config.
     * @return A new <code>NodeSettingsWO</code> object.
     */
    NodeSettingsWO addNodeSettings(String key);

    /**
     * Add the given <code>NodeSettings</code> object to this Config using the
     * key of the argument's <code>NodeSettings</code>.
     * @param settings The object to add to this <code>Config</code>.
     */
    void addNodeSettings(NodeSettings settings);

    /**
     * Stores a password in weakly encrypted form. Node implementations should pay special attention to only store
     * passwords in a node configuration when absolutely required since in some installations storing passwords to disc
     * is forbidden (see {@link org.knime.core.node.KNIMEConstants#PROPERTY_WEAK_PASSWORDS_IN_SETTINGS_FORBIDDEN}. When
     * handling passwords node implementations should always offer the option to retrieve the password from a
     * credentials variable (and only the identifier of the credentials object needs to be stored).
     */
    @Override
    void addPassword(String key, String encryptionKey, String value);

}
