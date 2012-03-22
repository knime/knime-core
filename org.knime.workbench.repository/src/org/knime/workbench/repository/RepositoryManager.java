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
 * -------------------------------------------------------------------
 *
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.repository.model.AbstractContainerObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.DynamicNodeTemplate;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.osgi.framework.Bundle;

/**
 * Manages the (global) KNIME Repository. This class collects all the
 * contributed extensions from the extension points and creates an arbitrary
 * model. The repository is created on-demand as soon as one of the three public
 * methods is called. Thus the first call can take some time to return.
 * Subsequent calls will return immediately with the full repository tree.
 *
 * @author Florian Georg, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public final class RepositoryManager {
	/**
	 * Listener interface for acting on events while the repository is read.
	 *
	 * @author Thorsten Meinl, University of Konstanz
	 * @since 2.4
	 */
	public interface Listener {
		/**
		 * Called when a new category has been created.
		 *
		 * @param root
		 *            the repository root
		 * @param category
		 *            the new category
		 */
		public void newCategory(Root root, Category category);

		/**
		 * Called when a new node has been created.
		 *
		 * @param root
		 *            the repository root
		 * @param node
		 *            the new node
		 */
		public void newNode(Root root, NodeTemplate node);

		/**
		 * Called when a new meta node has been created.
		 *
		 * @param root
		 *            the repository root
		 * @param metanode
		 *            the new category
		 */
		public void newMetanode(Root root, MetaNodeTemplate metanode);
	}

	private static final Listener NULL_LISTENER = new Listener() {
		@Override
		public void newCategory(final Root root, final Category category) {
		}

		@Override
		public void newNode(final Root root, final NodeTemplate node) {
		}

		@Override
		public void newMetanode(final Root root, final MetaNodeTemplate meta) {
		}
	};

	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(RepositoryManager.class);

	/** The singleton instance. */
	public static final RepositoryManager INSTANCE = new RepositoryManager();

	// ID of "node" extension point
	private static final String ID_NODE = "org.knime.workbench.repository"
			+ ".nodes";

	// ID of "category" extension point
	private static final String ID_CATEGORY = "org.knime.workbench."
			+ "repository.categories";

	private static final String ID_META_NODE = "org.knime.workbench.repository.metanode";

	private static final String ID_NODE_SET = "org.knime.workbench.repository.nodesets";

	private final Root m_root = new Root();

	private final Map<String, NodeTemplate> m_nodesById = new HashMap<String, NodeTemplate>();

	/**
	 * Creates the repository model. This instantiates all contributed
	 * category/node extensions found in the global Eclipse PluginRegistry, and
	 * attaches them to the repository tree.
	 */
	private RepositoryManager() {
	}

	private void readRepository(final Listener listener) {
		boolean isInExpertMode = Boolean
				.getBoolean(KNIMEConstants.PROPERTY_EXPERT_MODE);

		readCategories(listener);
		readNodes(listener, isInExpertMode);
		readNodeSets(listener, isInExpertMode);
		readMetanodes(listener, isInExpertMode);
		removeEmptyCategories(m_root);
	}

	private void readMetanodes(final Listener l, final boolean isInExpertMode) {
		// iterate over the meta node config elements
		// and create meta node templates
		IExtension[] metanodeExtensions = getExtensions(ID_META_NODE);
		for (IExtension mnExt : metanodeExtensions) {
			IConfigurationElement[] mnConfigElems = mnExt
					.getConfigurationElements();
			for (IConfigurationElement mnConfig : mnConfigElems) {
				if (!Platform.isRunning()) { // shutdown was initiated
					return;
				}

				try {
					MetaNodeTemplate metaNode = RepositoryFactory
							.createMetaNode(mnConfig);
					boolean skip = !isInExpertMode && metaNode.isExpertNode();
					if (skip) {
						LOGGER.debug("Skipping meta node definition '"
								+ metaNode.getID() + "': " + metaNode.getName()
								+ " (not in expert mode)");
						continue;
					} else {
						LOGGER.debug("Found meta node definition '"
								+ metaNode.getID() + "': " + metaNode.getName());
					}
					l.newMetanode(m_root, metaNode);

					IContainerObject parentContainer = m_root
							.findContainer(metaNode.getCategoryPath());
					// If parent category is illegal, log an error and
					// append the node to the repository root.
					if (parentContainer == null) {
						LOGGER.warn("Invalid category-path for node "
								+ "contribution: '"
								+ metaNode.getCategoryPath()
								+ "' - adding to root instead");
						m_root.addChild(metaNode);
					} else {
						// everything is fine, add the node to its parent
						// category
						parentContainer.addChild(metaNode);
					}
				} catch (Throwable t) {
					String message = "MetaNode " + mnConfig.getAttribute("id")
							+ "' from plugin '"
							+ mnConfig.getNamespaceIdentifier()
							+ "' could not be created.";
					Bundle bundle = Platform.getBundle(mnConfig
							.getNamespaceIdentifier());

					if ((bundle == null)
							|| (bundle.getState() != Bundle.ACTIVE)) {
						// if the plugin is null, the plugin could not
						// be activated maybe due to a not
						// activateable plugin
						// (plugin class can not be found)
						message = message + " The corresponding plugin "
								+ "bundle could not be activated!";
					}

					LOGGER.error(message, t);
				}
			}
		}
	}

	private void readCategories(final Listener l) {
		//
		// First, process the contributed categories
		//
		IExtension[] categoryExtensions = getExtensions(ID_CATEGORY);
		ArrayList<IConfigurationElement> allElements = new ArrayList<IConfigurationElement>();

		for (IExtension ext : categoryExtensions) {
			// iterate through the config elements and create 'Category' objects
			IConfigurationElement[] elements = ext.getConfigurationElements();
			allElements.addAll(Arrays.asList(elements));
		}

		// remove duplicated categories
		removeDuplicatesFromCategories(allElements);

		// sort first by path-depth, so that everything is there in the
		// right order
		Collections.sort(allElements, new Comparator<IConfigurationElement>() {
			@Override
			public int compare(final IConfigurationElement o1,
					final IConfigurationElement o2) {
				String element1 = o1.getAttribute("path");
				if ((element1 == null) || element1.equals("/")) {
					return -1;
				}
				String element2 = o2.getAttribute("path");
				if ((element2 == null) || element2.equals("/")) {
					return +1;
				}

				int countSlashes1 = element1.length()
						- element1.replace("/", "").length();

				int countSlashes2 = element2.length()
						- element2.replace("/", "").length();

				return countSlashes1 - countSlashes2;
			}

		});

		for (IConfigurationElement e : allElements) {
			try {
				Category category = RepositoryFactory.createCategory(m_root, e);
				LOGGER.debug("Found category extension '" + category.getID()
						+ "' on path '" + category.getPath() + "'");
				l.newCategory(m_root, category);
			} catch (Exception ex) {
				String message = "Category '" + e.getAttribute("level-id")
						+ "' from plugin '"
						+ e.getDeclaringExtension().getNamespaceIdentifier()
						+ "' could not be created in parent path '"
						+ e.getAttribute("path") + "'.";
				LOGGER.error(message, ex);
			}
		}
	}

	/**
	 * @param isInExpertMode
	 */
	private void readNodes(final Listener l, final boolean isInExpertMode) {
		//
		// Second, process the contributed nodes
		//
		IExtension[] nodeExtensions = RepositoryManager.getExtensions(ID_NODE);
		for (IExtension ext : nodeExtensions) {
			// iterate through the config elements and create 'NodeTemplate'
			// objects
			IConfigurationElement[] elements = ext.getConfigurationElements();
			for (IConfigurationElement e : elements) {
				if (!Platform.isRunning()) { // shutdown was initiated
					return;
				}

				try {
					NodeTemplate node = RepositoryFactory.createNode(e);

					boolean skip = !isInExpertMode && node.isExpertNode();
					if (skip) {
						LOGGER.debug("Skipping node extension '" + node.getID()
								+ "': " + node.getName()
								+ " (not in expert mode)");
						continue;
					} else {
						LOGGER.debug("Found node extension '" + node.getID()
								+ "': " + node.getName());
					}
					l.newNode(m_root, node);

					m_nodesById.put(node.getID(), node);
					String nodeName = node.getID();
					nodeName = nodeName
							.substring(nodeName.lastIndexOf('.') + 1);

					// Ask the root to lookup the category-container located at
					// the given path
					IContainerObject parentContainer = m_root
							.findContainer(node.getCategoryPath());

					// If parent category is illegal, log an error and append
					// the node to the repository root.
					if (parentContainer == null) {
						LOGGER.warn("Invalid category-path for node "
								+ "contribution: '" + node.getCategoryPath()
								+ "' - adding to root instead");
						m_root.addChild(node);
					} else {
						// everything is fine, add the node to its parent
						// category
						parentContainer.addChild(node);
					}

				} catch (Throwable t) {
					String message = "Node " + e.getAttribute("id")
							+ "' from plugin '" + ext.getNamespaceIdentifier()
							+ "' could not be created.";
					Bundle bundle = Platform.getBundle(ext
							.getNamespaceIdentifier());

					if ((bundle == null)
							|| (bundle.getState() != Bundle.ACTIVE)) {
						// if the plugin is null, the plugin could not
						// be activated maybe due to a not
						// activateable plugin (plugin class can not be found)
						message += " The corresponding plugin "
								+ "bundle could not be activated!";
					}
					LOGGER.error(message, t);
				}

			} // for configuration elements
		} // for node extensions
	}

	/**
	 * @param isInExpertMode
	 */
	private void readNodeSets(final Listener l, final boolean isInExpertMode) {
		//
		// Process the contributed node sets
		//
		IExtension[] nodeExtensions = RepositoryManager
				.getExtensions(ID_NODE_SET);
		for (IExtension ext : nodeExtensions) {
			// iterate through the config elements and create 'NodeTemplate'
			// objects
			IConfigurationElement[] elements = ext.getConfigurationElements();
			for (IConfigurationElement elem : elements) {
				if (!Platform.isRunning()) { // shutdown was initiated
					return;
				}

				try {
					Collection<DynamicNodeTemplate> dynamicNodeTemplates = RepositoryFactory
							.createNodeSet(m_root, elem);

					for (DynamicNodeTemplate node : dynamicNodeTemplates) {

						l.newNode(m_root, node);

						m_nodesById.put(node.getID(), node);
						String nodeName = node.getID();
						nodeName = nodeName
								.substring(nodeName.lastIndexOf('.') + 1);

						// Ask the root to lookup the category-container located
						// at
						// the given path
						IContainerObject parentContainer = m_root
								.findContainer(node.getCategoryPath());

						// If parent category is illegal, log an error and
						// append
						// the node to the repository root.
						if (parentContainer == null) {
							LOGGER.warn("Invalid category-path for node "
									+ "contribution: '"
									+ node.getCategoryPath()
									+ "' - adding to root instead");
							m_root.addChild(node);
						} else {
							// everything is fine, add the node to its parent
							// category
							parentContainer.addChild(node);
						}

					}

				} catch (Throwable t) {
					String message = "Node " + elem.getAttribute("id")
							+ "' from plugin '" + ext.getNamespaceIdentifier()
							+ "' could not be created.";
					Bundle bundle = Platform.getBundle(ext
							.getNamespaceIdentifier());

					if ((bundle == null)
							|| (bundle.getState() != Bundle.ACTIVE)) {
						// if the plugin is null, the plugin could not
						// be activated maybe due to a not
						// activateable plugin (plugin class can not be found)
						message += " The corresponding plugin "
								+ "bundle could not be activated!";
					}
					LOGGER.error(message, t);
				}

			} // for configuration elements
		} // for node extensions
	}

	/**
	 * Returns the extensions for a given extension point.
	 *
	 * @param pointID
	 *            The extension point ID
	 *
	 * @return The extensions
	 */
	private static IExtension[] getExtensions(final String pointID) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(pointID);
		if (point == null) {
			throw new IllegalStateException("Invalid extension point : "
					+ pointID);

		}
		return point.getExtensions();
	}

	private static void removeDuplicatesFromCategories(
			final ArrayList<IConfigurationElement> allElements) {

		// brute force search
		for (int i = 0; i < allElements.size(); i++) {
			for (int j = allElements.size() - 1; j > i; j--) {

				String pathOuter = allElements.get(i).getAttribute("path");
				String levelIdOuter = allElements.get(i).getAttribute(
						"level-id");
				String pathInner = allElements.get(j).getAttribute("path");
				String levelIdInner = allElements.get(j).getAttribute(
						"level-id");

				if (pathOuter.equals(pathInner)
						&& levelIdOuter.equals(levelIdInner)) {

					String nameI = allElements.get(i).getAttribute("name");
					String nameJ = allElements.get(j).getAttribute("name");

					// the removal is only reported in case the names
					// are not equal (if they are equal,the user will not
					// notice any difference (except possibly the picture))
					if (!nameI.equals(nameJ)) {
						String pluginI = allElements.get(i)
								.getDeclaringExtension()
								.getNamespaceIdentifier();
						String pluginJ = allElements.get(j)
								.getDeclaringExtension()
								.getNamespaceIdentifier();

						String message = "Category '" + pathOuter + "/"
								+ levelIdOuter
								+ "' was found twice. Names are '" + nameI
								+ "'(Plugin: " + pluginI + ") and '" + nameJ
								+ "'(Plugin: " + pluginJ
								+ "). The category with name '" + nameJ
								+ "' is ignored.";

						LOGGER.warn(message);
					}

					// remove from the end of the list
					allElements.remove(j);

				}
			}
		}
	}

	private static void removeEmptyCategories(
			final AbstractContainerObject treeNode) {
		for (IRepositoryObject object : treeNode.getChildren()) {
			if (object instanceof AbstractContainerObject) {
				AbstractContainerObject cat = (AbstractContainerObject) object;
				removeEmptyCategories(cat);
				if (!cat.hasChildren() && (cat.getParent() != null)) {
					cat.getParent().removeChild(
							(AbstractContainerObject) object);
				}
			}
		}
	}

	/**
	 * Returns the repository root. If the repository has not yet read, it will
	 * be created during the call. Thus the first call to this method can take
	 * some time.
	 *
	 * @return the root object
	 */
	public synchronized Root getRoot() {
		if (!m_root.hasChildren()) {
			readRepository(NULL_LISTENER);
		}
		return m_root;
	}

	/**
	 * Returns the repository root. If the repository has not yet read, it will
	 * be created during the call. If the listener is non-<code>null</code>, it
	 * will be notified of all read items (categories, nodes, metanodes).
	 *
	 *
	 * @param listener
	 *            a listener that is notified of newly read items
	 * @return the root object
	 * @since 2.4
	 */
	public synchronized Root getRoot(final Listener listener) {
		if (!m_root.hasChildren()) {
			readRepository(listener);
		}
		return m_root;
	}

	/**
	 * Returns the node template with the given id, or <code>null</code> if no
	 * such node exists.
	 *
	 * @param id
	 *            the node's id
	 * @return a node template or <code>null</code>
	 * @since 2.4
	 */
	public synchronized NodeTemplate getNodeTemplate(final String id) {
		if (!m_root.hasChildren()) {
			readRepository(NULL_LISTENER);
		}
		return m_nodesById.get(id);
	}
}
