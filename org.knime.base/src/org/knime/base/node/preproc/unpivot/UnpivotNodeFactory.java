/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.base.node.preproc.unpivot;

import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class UnpivotNodeFactory extends NodeFactory<UnpivotNodeModel> {

	/**
	 * Default constructor.
	 */
	public UnpivotNodeFactory() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new UnpivotNodeDialogPane();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public UnpivotNodeModel createNodeModel() {
		return new UnpivotNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractNodeView<UnpivotNodeModel> createNodeView(
			final int viewIndex, final UnpivotNodeModel nodeModel) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean hasDialog() {
		return true;
	}

}
