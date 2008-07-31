/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * History
 *   Dec 20, 2006 (wiswedel): created
 */
package org.knime.deprecated;

import org.eclipse.core.runtime.Plugin;
import org.knime.base.node.mine.mds.MDSPivotDataNodeFactory;
import org.knime.base.node.mine.mds.MDSPivotNodeFactory;
import org.knime.base.node.mine.scorer.ScorerNodeFactory;
import org.knime.base.node.mine.scorer.entrop.EntropyNodeFactory;
import org.knime.base.node.mine.sota.SotaNodeFactory;
import org.knime.base.node.preproc.join.JoinerNodeFactory;
import org.knime.base.node.preproc.nominal.NominalValueFactory;
import org.knime.base.node.preproc.normalize.NormalizeNodeFactory;
import org.knime.base.node.viz.property.color.ColorAppenderNodeFactory;
import org.knime.base.node.viz.property.color.ColorManagerNodeFactory;
import org.knime.base.node.viz.property.size.SizeManagerNodeFactory;
import org.knime.base.node.viz.rulevis2d.Rule2DNodeFactory;
import org.knime.core.node.NodeFactory;
import org.osgi.framework.BundleContext;


/**
 * This class is solely for registering the deprecated node factories.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DeprecatedPlugin extends Plugin {
    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        NodeFactory.addLoadedFactory(NominalValueFactory.class);
        NodeFactory.addLoadedFactory(NormalizeNodeFactory.class);
        NodeFactory.addLoadedFactory(ScorerNodeFactory.class);
        NodeFactory.addLoadedFactory(JoinerNodeFactory.class);
        NodeFactory.addLoadedFactory(SotaNodeFactory.class);
        NodeFactory.addLoadedFactory(SizeManagerNodeFactory.class);
        NodeFactory.addLoadedFactory(ColorManagerNodeFactory.class);
        NodeFactory.addLoadedFactory(ColorAppenderNodeFactory.class);
        NodeFactory.addLoadedFactory(EntropyNodeFactory.class);
        NodeFactory.addLoadedFactory(Rule2DNodeFactory.class);
        NodeFactory.addLoadedFactory(MDSPivotNodeFactory.class);
        NodeFactory.addLoadedFactory(MDSPivotDataNodeFactory.class);
    }
}
