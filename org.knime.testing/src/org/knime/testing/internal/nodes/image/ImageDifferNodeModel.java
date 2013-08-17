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
 *   16.08.2013 (thor): created
 */
package org.knime.testing.internal.nodes.image;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.image.ImageValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceChecker.Result;

/**
 * Model for the image difference checker node.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class ImageDifferNodeModel extends NodeModel {
    private final ImageDifferNodeSettings m_settings = new ImageDifferNodeSettings();

    ImageDifferNodeModel() {
        super(new PortType[]{ImagePortObject.TYPE, ImagePortObject.TYPE}, new PortType[]{});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[1] == null) {
            throw new InvalidSettingsException("No reference image available");
        }

        if (m_settings.checkerFactoryClassName() == null) {
            throw new InvalidSettingsException("No image checker selected");
        }
        if (m_settings.checkerFactory() == null) {
            throw new InvalidSettingsException("Image checker factory '" + m_settings.checkerFactoryClassName()
                                               + "' does not exist");
        }

        if (inSpecs[0] != null) {
            checkImageSpecs(inSpecs[0], inSpecs[1]);
        }

        return new PortObjectSpec[0];
    }

    private void checkImageSpecs(final PortObjectSpec testSpec, final PortObjectSpec refSpec) {
        if (!testSpec.equals(refSpec)) {
            throw new IllegalStateException("Wrong image spec: expected '" + refSpec + "', got '" + testSpec + "'");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ImagePortObject testPortObject = (ImagePortObject)inObjects[0];
        ImagePortObject refPortObject = (ImagePortObject)inObjects[1];

        checkImageSpecs(testPortObject.getSpec(), refPortObject.getSpec());

        DifferenceChecker<ImageValue> checker = (DifferenceChecker<ImageValue>)m_settings.createChecker();


        ImageValue refImage = (ImageValue)refPortObject.toDataCell();
        ImageValue testImage = (ImageValue)testPortObject.toDataCell();

        Result res = checker.check(refImage, testImage);
        if (!res.ok()) {
            throw new IllegalStateException("Images are not equal: " + res.getMessage());
        }

        return new PortObject[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new ImageDifferNodeSettings().loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }
}
