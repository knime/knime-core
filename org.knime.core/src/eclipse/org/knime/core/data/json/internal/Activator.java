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
 *   11 Sept. 2014 (Gabor): created
 */
package org.knime.core.data.json.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;

/**
 *
 * @author Gabor Bakos
 */
public class Activator implements BundleActivator {

    private static ClassLoader m_jsonProviderClassLoader;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext ctx) throws Exception {

        Bundle implBundle = null;
        for (Bundle b : ctx.getBundles()) {
            if ("org.glassfish.javax.json".equals(b.getSymbolicName())) {
                implBundle = b;
                break;
            }
        }
        if (implBundle == null) {
            throw new IllegalStateException("The glassfish implementation for the javax.json API could not be loaded.");
        }
        m_jsonProviderClassLoader = implBundle.adapt(BundleWiring.class).getClassLoader();
        KNIMEJsonProvider.init(m_jsonProviderClassLoader);
        //        ServiceLoader<JsonProvider> loader = ServiceLoader.load(JsonProvider.class, m_jsonProviderClassLoader);
        //        JsonProvider jsonProvider = loader.iterator().next();
        //ServiceLoader.loadInstalled(iterator.next().getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext ctx) throws Exception {
        // TODO Auto-generated method stub

    }

    /**
     * @return the jsonProviderClassLoader
     */
    public static ClassLoader getJsonProviderClassLoader() {
        return m_jsonProviderClassLoader;
    }

}
