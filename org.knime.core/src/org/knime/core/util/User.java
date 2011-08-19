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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   19.08.2011 (meinl): created
 */
package org.knime.core.util;

import java.lang.reflect.Method;

/**
 * Utility class with user-related functions such as getting the current user's
 * name.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public final class User {
    private User() {}

    /**
     * Returns the name of the current user. It does not use the
     * <tt>user.name</tt> system property but relies on the authentication
     * modules.
     *
     * @return the user name or <code>null</code> if it cannot be determined
     * @throws Exception a bunch of reflection-related exceptions
     */
    public static String getUsername() throws Exception {
        String os = System.getProperty("os.name");
        String className;
        if ("Linux".equals(os) || "Mac OS X".equals(os)) {
            className = "com.sun.security.auth.module.UnixSystem";
        } else if (os.startsWith("Windows")) {
            className = "com.sun.security.auth.module.NTSystem";
        } else if ("Solaris".equals(os)) {
            className = "com.sun.security.auth.module.SolarisSystem";
        } else {
            return null;
        }
        Class<?> clazz = Class.forName(className);
        Method getUsername = clazz.getMethod("getUsername");
        Object osSystem = clazz.newInstance();
        return (String)getUsername.invoke(osSystem);
    }
}
