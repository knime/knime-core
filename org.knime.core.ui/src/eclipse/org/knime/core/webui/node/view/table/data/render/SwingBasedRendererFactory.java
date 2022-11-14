/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 14, 2022 (hornm): created
 */
package org.knime.core.webui.node.view.table.data.render;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;
import org.knime.core.data.renderer.ImageValueRenderer;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.view.table.data.TableViewDataServiceImpl;

/**
 * A renderer factory that uses and returns the swing-based
 * {@link org.knime.core.data.renderer.DataValueRenderer}-implementations.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class SwingBasedRendererFactory implements DataValueRendererFactory {

    @Override
    public DataValueRenderer createDataValueRenderer(final DataColumnSpec colSpec, final String rendererId) {
        var dataType = colSpec.getType();
        if (rendererId == null) {
            var it = dataType.getRendererFactories().iterator();
            return createRenderer(it.next(), colSpec);
        } else {
            return dataType.getRendererFactories().stream().filter(f -> f.getId().equals(rendererId))
                .map(f -> createRenderer(f, colSpec)).findFirst().orElseGet(() -> {
                    NodeLogger.getLogger(getClass())
                        .warn("No renderer found for id " + rendererId + ". Falling back to default renderer.");
                    return createDataValueRenderer(colSpec, null);
                });
        }
    }

    @Override
    public String[] getDataValueRendererIds(final DataType dataType) {
        return dataType.getRendererFactories().stream()
            .map(org.knime.core.data.renderer.DataValueRendererFactory::getId).toArray(String[]::new);
    }

    private static DataValueRenderer createRenderer(
        final org.knime.core.data.renderer.DataValueRendererFactory rendererFactory, final DataColumnSpec colSpec) {
        var renderer = rendererFactory.createRenderer(colSpec);
        var id = rendererFactory.getId();
        if (renderer instanceof AbstractPainterDataValueRenderer) {
            return new SwingBasedImageRenderer((AbstractPainterDataValueRenderer)renderer, id);
        } else if (renderer instanceof DefaultDataValueRenderer) {
            var defaultRenderer = (DefaultDataValueRenderer)renderer;
            if (defaultRenderer.getIcon() != null) {
                return new SwingBasedImageRenderer(defaultRenderer, id);
            } else {
                return new SwingBasedTextRenderer(defaultRenderer);
            }
        } else if (renderer instanceof ImageValueRenderer) {
            return new SwingBasedImageRenderer((ImageValueRenderer)renderer, id);
        } else {
            throw new UnsupportedOperationException(
                "The renderer of type '" + renderer.getClass().getName() + "' is currently not supported.");
        }
    }

    static class SwingBasedTextRenderer implements DataValueTextRenderer {

        private final DefaultDataValueRenderer m_renderer;

        SwingBasedTextRenderer(final DefaultDataValueRenderer renderer) {
            m_renderer = renderer;
        }

        @Override
        public String renderText(final DataValue value) {
            m_renderer.setValueCatchingException(value);
            return m_renderer.getText();
        }

    }

    static class SwingBasedImageRenderer implements DataValueImageRenderer {

        private final org.knime.core.data.renderer.DataValueRenderer m_renderer;
        private final String m_id;

        SwingBasedImageRenderer(final AbstractPainterDataValueRenderer swingBasedPainterRenderer, final String id) {
            m_renderer = swingBasedPainterRenderer;
            m_id = id;
        }

        SwingBasedImageRenderer(final DefaultDataValueRenderer swingBasedDefaultRenderer, final String id) {
            m_renderer = swingBasedDefaultRenderer;
            m_id = id;
        }

        SwingBasedImageRenderer(final ImageValueRenderer swingBasedImageValueRenderer, final String id) {
            m_renderer = swingBasedImageValueRenderer;
            m_id = id;
        }

        @Override
        public byte[] renderImage(final DataValue value, final int width, final int height) {
            // NOTE: mostly copied from Renderer2ImageNodeModel#createPngCell
            var comp = getRendererComponent(value, m_renderer);
            var size = new Dimension(width, height);
            comp.setSize(size);
            var image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
            // create graphics object to paint in
            Graphics2D graphics = image.createGraphics();
            comp.paint(graphics);
            try (var bos = new ByteArrayOutputStream(4096)){
                ImageIO.write(image, "png", bos);
                return bos.toByteArray();
            } catch (IOException e) {
                // should never happen
                NodeLogger.getLogger(TableViewDataServiceImpl.class)
                    .error("Problem rendering data cell into a png image", e);
                return new byte[0];
            }
        }

        private static Component getRendererComponent(final DataValue val,
            final org.knime.core.data.renderer.DataValueRenderer renderer) {
            if (renderer instanceof AbstractPainterDataValueRenderer) {
                return renderer.getRendererComponent(val);
            } else if (renderer instanceof DefaultDataValueRenderer) {
                var defaultRenderer = (DefaultDataValueRenderer)renderer;
                defaultRenderer.setValueCatchingException(val);
                return defaultRenderer;
            } else if (renderer instanceof ImageValueRenderer) {
                return renderer.getRendererComponent(val);
            } else {
                // should never happen
                throw new IllegalStateException("Unsupported renderer " + renderer.getClass().getName());
            }
        }

        @Override
        public String getId() {
            return m_id;
        }

    }

}
