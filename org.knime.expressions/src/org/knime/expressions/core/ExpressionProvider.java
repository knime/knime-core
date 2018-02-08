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
 */
package org.knime.expressions.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.node.preproc.stringmanipulation.manipulator.Manipulator;
import org.knime.base.node.util.ManipulatorProvider;

/**
 * A provider for all registered expressions.
 * 
 * @author Moritz Heine, KNIME GmbH, Konstanz
 * @author Christian Dietz, KNIME GmbH, Konstanz
 *
 */
public final class ExpressionProvider implements ManipulatorProvider {

	private static final ExpressionProvider MANIPULATOR_PROVIDER = new ExpressionProvider();
	private final Map<String, List<Expression>> m_manipulatorMap;

	/**
	 * Private constructor to ensure no one instantiates it.
	 */
	private ExpressionProvider() {
		m_manipulatorMap = new HashMap<>();
		final List<Expression> all = new ArrayList<>();
		m_manipulatorMap.put(ALL_CATEGORY, all);

		for (final ExpressionSet set : ExpressionSetRegistry.getExpressionSets()) {
			List<Expression> list = m_manipulatorMap.putIfAbsent(set.getCategory(), new ArrayList<>());
			list = list == null ? m_manipulatorMap.get(set.getCategory()) : list;
			final Collection<? extends Expression> manipulators = set.getExpressions();
			list.addAll(manipulators);
			all.addAll(manipulators);
		}
	}

	/**
	 * @return All registered categories.
	 */
	@Override
	public Collection<String> getCategories() {
		return m_manipulatorMap.keySet();
	}

	/**
	 * Returns the registered expressions for the given category.
	 * 
	 * @param category
	 *            The category for which we want registered expressions.
	 * @return The registered expressions for the given category.
	 */
	@Override
	public Collection<? extends Manipulator> getManipulators(String category) {
		return m_manipulatorMap.get(category);
	}

	/**
	 * @return Returns an instance of the ExpressionProvider.
	 */
	public static ExpressionProvider getInstance() {
		return MANIPULATOR_PROVIDER;
	}
}
