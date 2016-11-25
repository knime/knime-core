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
 *   29.05.2016 (koetter): created
 */
package org.knime.core.node.port.database;

import java.util.Properties;

import org.knime.core.node.port.database.connection.CachedConnectionFactory;
import org.knime.core.node.port.database.connection.DBDriverFactory;

/**
 * Oracle specific {@link CachedConnectionFactory} that only sets user name and password if they are none empty strings.
 * @author Tobias Koetter, KNIME.com
 * @since 3.2
 */
public class OracleCachedConnectionFactory extends CachedConnectionFactory {

    /**
     * @param driverFactory
     */
    public OracleCachedConnectionFactory(final DBDriverFactory driverFactory) {
        super(driverFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Properties createConnectionProperties(final String user, final String pass) {
        final Properties props = new Properties();
        //To support Oracle wallet user name and password must not be set. The default CachedConnectionFactory class
        //also sets them if the strin gis empty which is the case when the user has not entered anything in the dialog.
        //See https://knime-com.atlassian.net/browse/AP-5700.
        if (addVal(user)) {
            props.put("user", user);
        }
        if (addVal(pass)) {
            props.put("password", pass);
        }
        return props;
    }

    private boolean addVal(final String val) {
        return val != null && !val.trim().isEmpty();
    }
}
