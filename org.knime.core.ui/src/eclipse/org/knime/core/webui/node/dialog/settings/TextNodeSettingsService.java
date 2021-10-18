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
 *   Oct 16, 2021 (hornm): created
 */
package org.knime.core.webui.node.dialog.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A {@link NodeSettingsService} which transfers {@link NodeSettings} to and from a string.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public interface TextNodeSettingsService extends NodeSettingsService {

    /**
     * {@inheritDoc}
     */
    @Override
    default void writeSettings(final InputStream in, final NodeSettingsWO settings) throws InvalidSettingsException {
        try {
            writeSettings(new String(in.readAllBytes(), StandardCharsets.UTF_8), settings);
        } catch (IOException ex) {
            throw new InvalidSettingsException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void readSettings(final NodeSettingsRO settings, final OutputStream out) {
        try {
            out.write(readSettings(settings).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            // should never happen
            throw new IllegalStateException("Problem reading the node settings", ex);
        }
    }

    /**
     * Transfers the node settings from a string into a {@link NodeSettingsWO}-object.
     *
     * @param s the string representation of the settings
     * @param settings the settings object to write into
     * @throws InvalidSettingsException
     */
    void writeSettings(String s, NodeSettingsWO settings) throws InvalidSettingsException;

    /**
     * Converts a {@link NodeSettingsRO}-object into a string representing the settings.
     *
     * @param settings the settings to read from
     * @return the string-representation of the setting
     */
    String readSettings(NodeSettingsRO settings);

}
