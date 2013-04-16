/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   31.05.2012 (hofer): created
 */
package org.knime.base.node.jsnippet.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.knime.base.node.jsnippet.JSnippetTemplate;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;

/**
 * A {@link TemplateRepository} which stores templates on the disk.
 *
 * @author Heiko Hofer
 */
@SuppressWarnings("rawtypes")
public final class FileTemplateRepository extends TemplateRepository {
    private static NodeLogger logger
        = NodeLogger.getLogger(FileTemplateRepository.class);

    private File m_folder;
    private boolean m_readonly;

    /** Templates grouped by meta category. */
    private Map<Class, Collection<JSnippetTemplate>> m_templates;


    /**
     * Create a new file base template repository.
     *
     * @param folder the folder with templates
     * @param readonly if the repository is read only
     * @throws IOException if a template cannot be read
     */

    private FileTemplateRepository(final File folder, final boolean readonly)
            throws IOException {
        super();
        m_folder = folder;
        m_readonly = readonly;

        m_templates = new HashMap<Class, Collection<JSnippetTemplate>>();

        Collection<JSnippetTemplate> templates =
            new ArrayList<JSnippetTemplate>();

        if (m_folder.exists()) {
            for (File meta : m_folder.listFiles()) {
                if (meta.isDirectory()) {
                    for (File file : meta.listFiles()) {
                        addIfTemplate(templates, file);
                    }
                }
            }
        }
        appendTemplates(templates);

    }

    /**
     * Adds the template to the give collection o a successful read. The
     * template file is supposed to end with ".xml".
     * @param templates the templates to add to
     * @param file the file to read
     */
    private void addIfTemplate(final Collection<JSnippetTemplate> templates,
            final File file) {
        if (file.getName().endsWith(".xml")) {
            try {
                NodeSettingsRO settings =
                    NodeSettings.loadFromXML(new FileInputStream(file));
                templates.add(JSnippetTemplate.create(settings));
            } catch (Exception e) {
                logger.error("The following file seems to be no template. "
                        + file.getAbsolutePath(), e);
            }
        }

    }

    /**
     * Append given templates to the list of templates.
     * @param templates the templates to append.
     */
    private void appendTemplates(final Collection<JSnippetTemplate> templates) {
        for (JSnippetTemplate template : templates) {
            Class key = template.getMetaCategory();
            Collection<JSnippetTemplate> collection = m_templates.get(key);
            if (null == collection) {
                collection = new ArrayList<JSnippetTemplate>();
            }
            collection.add(template);
            m_templates.put(key, collection);
        }

    }

    /** Create a repository from the templates in the given folder. Templates
     * in this repository cannot be removed or replaced.
     * @param folder the folder with the repositories
     * @return the template repository
     * @throws IOException if a template cannot be read
     */
    public static FileTemplateRepository createProtected(final File folder)
            throws IOException {
        return new FileTemplateRepository(folder, true);
    }

    /** Create a repository from the templates in the given folder. Templates
     * may be removed or replaced. Use <code>createProtected</code> for a
     * repository that is read only.
     * @param folder the folder with the repositories
     * @return the template repository
     * @throws IOException if a template cannot be read
     */
    public static FileTemplateRepository create(final File folder)
            throws IOException {
        return new FileTemplateRepository(folder, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<JSnippetTemplate> getTemplates(
            final Collection<Class> metaCategories) {
        if (metaCategories.size() == 1) {
            return m_templates.get(metaCategories.iterator().next());
        } else {
            Collection<JSnippetTemplate> templates =
                new ArrayList<JSnippetTemplate>();
            for (Class c : metaCategories) {
                if (m_templates.containsKey(c)) {
                    templates.addAll(m_templates.get(c));
                }
            }
            return templates;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRemoveable(final JSnippetTemplate template) {
        if (!m_readonly) {
            return isInRepository(template);
        } else {
            return false;
        }
    }

    /** Returns true when the given template is in this repository. */
    private boolean isInRepository(final JSnippetTemplate template) {
        Collection<JSnippetTemplate> templates =
            m_templates.get(template.getMetaCategory());
        return null != templates ? templates.contains(template) : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeTemplate(final JSnippetTemplate template) {
        if (m_readonly) {
            return false;
        }
        Collection<JSnippetTemplate> templates =
            m_templates.get(template.getMetaCategory());
        boolean removed = templates.remove(template);
        if (removed) {
            File file = getFile(template);
            if (file.exists()) {
                file.delete();
            }
            fireStateChanged();
        }
        return removed;
    }

    /**
     * Add a template to the default location.
     * @param template the template
     */
    void addTemplate(final JSnippetTemplate template) {
        if (m_readonly) {
            throw new RuntimeException("This repository is read only."
                    + "Cannot add a template.");
        }
        try {
            File file = getFile(template);
            boolean isNew = file.createNewFile();
            if (isNew) {
                NodeSettings settings = new NodeSettings(file.getName());
                template.saveSettings(settings);
                settings.saveToXML(new FileOutputStream(file));
                // reload settings
                NodeSettingsRO settingsro = NodeSettings.loadFromXML(
                        new FileInputStream(file));
                // set the reloaded settings so that all references to existing
                // objects are broken. This makes sure, that the template is not
                // changed from outside.
                template.loadSettings(settingsro);
                appendTemplates(Collections.singletonList(template));
            } else {
                throw new IOException("A file with this name does "
                        + "already exist: " + file.getAbsolutePath());
            }
        } catch (IOException e1) {
            NodeLogger.getLogger(this.getClass()).error(
                    "Could not create template at the default location.", e1);
        }

    }

    /**
     * Get the templates file.
     * @param template the file
     */
    private File getFile(final JSnippetTemplate template) {
        String meta = template.getMetaCategory().getName();
        File metaFile = new File(m_folder, meta);
        metaFile.mkdir();
        String name = template.getName().replaceAll("[^a-zA-Z0-9 ]", "_")
            + "_" + template.getUUID() + ".xml";
        File file = new File(metaFile, name);
        return file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSnippetTemplate getTemplate(final UUID id) {
        String refID = id.toString();
        for (Collection<JSnippetTemplate> templates : m_templates.values()) {
            for (JSnippetTemplate template : templates) {
                if (template.getUUID().equals(refID)) {
                    return template;
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayLocation(final JSnippetTemplate template) {
        return isInRepository(template) ? getFile(template).getPath() : null;
    }
}
