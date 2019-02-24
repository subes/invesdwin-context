package de.invesdwin.context.jfreechart.panel.helper.config;

import java.awt.Color;
import java.awt.Stroke;

import javax.annotation.concurrent.NotThreadSafe;

import org.jfree.chart.renderer.xy.XYItemRenderer;

import de.invesdwin.context.jfreechart.panel.helper.legend.HighlightedLegendInfo;
import de.invesdwin.context.jfreechart.renderer.IUpDownColorRenderer;
import de.invesdwin.context.jfreechart.renderer.custom.ICustomRendererType;

@NotThreadSafe
public class SeriesInitialSettings {

    private final IRendererType rendererType;
    private final Color seriesColor;
    private final Color upColor;
    private final Color downColor;
    private final LineStyleType lineStyleType;
    private final LineWidthType lineWidthType;

    public SeriesInitialSettings(final XYItemRenderer initialRenderer) {
        final SeriesRendererType seriesRendererType = SeriesRendererType.valueOf(initialRenderer);
        if (seriesRendererType == SeriesRendererType.Custom) {
            rendererType = (ICustomRendererType) initialRenderer;
        } else {
            rendererType = seriesRendererType;
        }
        seriesColor = (Color) initialRenderer.getSeriesPaint(0);
        if (initialRenderer instanceof IUpDownColorRenderer) {
            final IUpDownColorRenderer cRenderer = (IUpDownColorRenderer) initialRenderer;
            upColor = cRenderer.getUpColor();
            downColor = cRenderer.getDownColor();
        } else {
            upColor = null;
            downColor = null;
        }
        final Stroke stroke = initialRenderer.getSeriesStroke(0);
        lineStyleType = LineStyleType.valueOf(stroke);
        lineWidthType = LineWidthType.valueOf(stroke);
    }

    public void reset(final HighlightedLegendInfo highlighted) {
        rendererType.reset(highlighted, this);
    }

    public IRendererType getRendererType() {
        return rendererType;
    }

    public IRendererType getCurrentRendererType(final HighlightedLegendInfo highlighted) {
        final SeriesRendererType seriesRendererType = SeriesRendererType.valueOf(highlighted.getRenderer());
        if (seriesRendererType == SeriesRendererType.Custom) {
            return getRendererType();
        } else {
            return seriesRendererType;
        }
    }

    public boolean isCustomSeriesType() {
        return rendererType instanceof ICustomRendererType;
    }

    public Color getSeriesColor() {
        return seriesColor;
    }

    public Color getUpColor() {
        return upColor;
    }

    public Color getDownColor() {
        return downColor;
    }

    public LineStyleType getLineStyleType() {
        return lineStyleType;
    }

    public LineWidthType getLineWidthType() {
        return lineWidthType;
    }

    public Stroke getSeriesStroke() {
        return lineStyleType.getStroke(lineWidthType);
    }

}