/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sep 7, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.xml.sax.SAXException;

/**
 * False predicate as specified in PMML
 * (<a>http://www.dmg.org/v4-0/TreeModel.html</a>).
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public class PMMLFalsePredicate extends PMMLPredicate {
    /** The string representation of the predicate's XML-element. */
    public static final String NAME = "False";

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean evaluate(final DataRow row,
            final DataTableSpec spec) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSplitAttribute() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     * @throws SAXException
     */
    @Override
    public void writePMML(final TransformerHandler handler)
            throws SAXException {
        handler.startElement(null, null, NAME, null);
        handler.endElement(null, null, NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromPredParams(final Config conf)
            throws InvalidSettingsException {
        assert conf.getString(PMMLPredicate.TYPE_KEY).equals(NAME);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToPredParams(final Config conf) {
        conf.addString(PMMLPredicate.TYPE_KEY, NAME);
    }



}
