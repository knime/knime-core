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
 *   07.06.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.template;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.osgi.framework.Bundle;

/**
 * A m_file template provider for templates relative to a plugin. Since the
 * templates are shipped out by a plugin, they cannot be removed or replaced.
 *
 * @author Heiko Hofer
 */
public class PluginFileTemplateRepositoryProvider
        implements TemplateRepositoryProvider {
    private static NodeLogger logger
        = NodeLogger.getLogger(PluginFileTemplateRepositoryProvider.class);
    private static FileTemplateRepository repo;
    private final Object m_lock = new Object[0];

    private File m_file;

    /**
     * Create a instance for the bundle "org.knime.jnippets" and the relative
     * path "/jsnippets".
     */
    public PluginFileTemplateRepositoryProvider() {
        this("org.knime.jsnippets", "jsnippets");
    }

    /**
     * @param symbolicName the name of the bundle like "org.nime.jsnipptes"
     * @param relativePath the path to the repositories base folder,
     *  i.e. "/jsnippes"
     */
    public PluginFileTemplateRepositoryProvider(final String symbolicName,
            final String relativePath) {
        try {
            Bundle bundle = Platform.getBundle(symbolicName);
            URL url = FileLocator.find(bundle, new Path(relativePath), null);
            m_file = FileUtil.getFileFromURL(FileLocator.toFileURL(url));
        } catch (Exception e) {
            logger.error("Cannot locate jsnippet templates in path "
                    + symbolicName + " of the bundle "
                    + relativePath + ".", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateRepository getRepository() {
        synchronized (m_lock) {
            if (null == repo) {
                try {
                    repo = FileTemplateRepository.createProtected(m_file);
                } catch (IOException e) {
                    logger.error("Cannot create the template provider with "
                            + "base file " + m_file.getAbsolutePath(), e);
                }
            }
        }
        return repo;
    }

}
