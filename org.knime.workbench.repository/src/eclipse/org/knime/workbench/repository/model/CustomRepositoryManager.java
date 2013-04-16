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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   29.05.2012 (meinl): created
 */
package org.knime.workbench.repository.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.repository.model.customNodeRepository.AbstractCategory;
import org.knime.workbench.repository.model.customNodeRepository.CustomCategory;
import org.knime.workbench.repository.model.customNodeRepository.CustomNode;
import org.knime.workbench.repository.model.customNodeRepository.RootDocument;

/**
 * Manager for the custom node repository. It offers functionality to transform
 * the standard node repository into a custom structure using a definition.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class CustomRepositoryManager {
    private static final NodeLogger logger = NodeLogger
            .getLogger(CustomRepositoryManager.class);

    private AbstractCategory m_customRoot;

    private final Root m_root = new Root();

    private static final String CUSTOM_CATEGORY_PREFIX = "###";

    /**
     * Creates a new custom repository manager with no definition file. The
     * repository returned by {@link #getRoot()} will be empty.
     */
    public CustomRepositoryManager() {
        m_customRoot = null;
    }

    /**
     * Creates a new custom repository manager using the definition from the
     * given file.
     *
     * @param definition an XML file containing the custom definition
     *
     * @throws XmlException if an error occurs while parsing the XML file
     * @throws IOException if an error occurs while reading the file
     */
    public CustomRepositoryManager(final File definition) throws XmlException,
            IOException {
        loadDefinition(definition);
    }

    /**
     * Creates a new custom repository manager using the definition from the
     * given string.
     *
     * @param definition an XML string containing the custom definition
     *
     * @throws XmlException if an error occurs while parsing the XML string
     */
    public CustomRepositoryManager(final String definition) throws XmlException {
        m_customRoot = RootDocument.Factory.parse(definition).getRoot();
    }

    /**
     * Loads a new custom repository definition from the given file.
     *
     * @param definition an XML file containing the custom definition
     *
     * @throws XmlException if an error occurs while parsing the XML file
     * @throws IOException if an error occurs while reading the file
     */
    public void loadDefinition(final File definition) throws XmlException,
            IOException {
        m_customRoot = RootDocument.Factory.parse(definition).getRoot();
    }

    /**
     * Transforms an existing repository into a custom structure using the
     * loaded definition. If no definition has been loaded an empty repository
     * is created. This process creates a complete copy of all elements in the
     * existing repository.
     *
     * @param oldRoot the root of an existing repository
     * @return the root of the transformed repository.
     */
    public Root transformRepository(final Root oldRoot) {
        m_root.removeAllChildren();
        m_root.setSortChildren(false);
        if (m_customRoot != null) {
            // transform the repository according to the definition
            Map<String, IRepositoryObject> nodeMap =
                    new HashMap<String, IRepositoryObject>();
            traverseRepository(oldRoot, "", nodeMap);

            processCategory(m_root, m_customRoot, nodeMap);
        }
        return m_root;
    }

    /**
     * Returns the current repository root.
     *
     * @return the repository root
     */
    public Root getRoot() {
        return m_root;
    }

    private void processCategory(final AbstractContainerObject parent,
            final AbstractCategory category,
            final Map<String, IRepositoryObject> nodeMap) {
        for (CustomCategory cat : category.getCategoryList()) {
            if (cat.getCustom()) {
                Category newCategory;
                if ((cat.getId() != null)
                        && cat.getId().startsWith(CUSTOM_CATEGORY_PREFIX)) {
                    newCategory = new Category(cat.getId(), cat.getName());
                } else {
                    newCategory = createCustomCategory(cat.getName());
                }
                newCategory.setSortChildren(false);
                newCategory.setParent(parent);
                parent.addChild(newCategory);
                newCategory.setIcon(ImageRepository
                        .getImage(SharedImages.DefaultCategoryIcon));

                processCategory(newCategory, cat, nodeMap);
            } else {
                Category oldCategory =
                        (Category)nodeMap.get(cat.getOriginalPath());
                if (oldCategory == null) {
                    logger.warn("Category '" + cat.getOriginalPath()
                            + "' does not exist in node repository");
                } else {
                    Category newCategory = (Category)oldCategory.deepCopy();
                    newCategory.setParent(parent);
                    parent.addChild(newCategory);
                }
            }
        }

        for (CustomNode node : category.getNodeList()) {
            NodeTemplate oldNode = (NodeTemplate)nodeMap.get(node.getId());
            if (oldNode == null) {
                logger.warn("Node '" + node.getId()
                        + "' does not exist in node repository");
            } else {
                NodeTemplate newNode = (NodeTemplate)oldNode.deepCopy();
                newNode.setParent(parent);
                parent.addChild(newNode);
            }
        }
    }

    private void traverseRepository(final IContainerObject node,
            final String path, final Map<String, IRepositoryObject> nodeMap) {
        for (IRepositoryObject child : node.getChildren()) {
            if (child instanceof IContainerObject) {
                String id = path + "/" + child.getID();
                nodeMap.put(id, child);
                traverseRepository((IContainerObject)child, id, nodeMap);
            } else {
                nodeMap.put(child.getID(), child);
            }
        }
    }

    /**
     * Serializes the custom repository into the given file.
     *
     * @param destination the destination file
     * @throws IOException if an error occurs while writing the file
     */
    public void serializeRepository(final File destination) throws IOException {
        RootDocument doc = RootDocument.Factory.newInstance();
        AbstractCategory root = doc.addNewRoot();
        serializeCustomCategory(m_root, root);

        doc.save(destination);
    }

    /**
     * Serializes the custom repository into a string.
     *
     * @return an XML string
     */
    public String serializeRepository() {
        RootDocument doc = RootDocument.Factory.newInstance();
        AbstractCategory root = doc.addNewRoot();
        serializeCustomCategory(m_root, root);

        return doc.xmlText();
    }

    private void serializeCustomCategory(final IContainerObject reposCategory,
            final AbstractCategory parent) {
        for (IRepositoryObject child : reposCategory.getChildren()) {
            if (child instanceof Category) {
                Category cat = (Category)child;

                CustomCategory newCategory = parent.addNewCategory();
                if (isCustomCategory(cat)) {
                    // custom category
                    newCategory.setName(cat.getName());
                    newCategory.setCustom(true);
                    newCategory.setId(cat.getID());
                    serializeCustomCategory(cat, newCategory);
                } else {
                    // reference to existing category
                    newCategory.setCustom(false);
                    newCategory.setOriginalPath(cat.getPath()
                            + (cat.getPath().endsWith("/") ? "" : "/")
                            + cat.getID());
                }
            } else if (child instanceof NodeTemplate) {
                NodeTemplate node = (NodeTemplate)child;

                CustomNode newNode = parent.addNewNode();
                newNode.setId(node.getID());
            }
        }
    }

    /**
     * Checks if the given category is a custom category or the copy of a
     * pre-defined category.
     *
     * @param cat any category
     * @return <code>true</code> if it is a custom category, <code>false</code>
     *         otherwise
     */
    public static boolean isCustomCategory(final Category cat) {
        return cat.getID().startsWith(CUSTOM_CATEGORY_PREFIX);
    }

    private static final AtomicInteger idCounter = new AtomicInteger();

    /**
     * Creates a new custom category with the given name.
     *
     * @param name the new category's name
     *
     * @return a new custom category
     */
    public static Category createCustomCategory(final String name) {
        return new Category(CUSTOM_CATEGORY_PREFIX
                + idCounter.incrementAndGet(), name);
    }

    /**
     * Returns the custom name for the repository.
     *
     * @return a custom repository name
     */
    public String getCustomName() {
        return ((org.knime.workbench.repository.model.customNodeRepository.Root)m_customRoot)
                .getName();
    }
}
