package de.invesdwin.context.jfreechart.plot.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.annotation.concurrent.NotThreadSafe;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.PaintUtils;
import org.jfree.data.Range;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

import de.invesdwin.context.jfreechart.panel.helper.config.PriceInitialSettings;
import de.invesdwin.context.jfreechart.plot.annotation.priceline.IDelegatePriceLineXYItemRenderer;
import de.invesdwin.context.jfreechart.plot.annotation.priceline.IPriceLineRenderer;
import de.invesdwin.context.jfreechart.plot.annotation.priceline.XYPriceLineAnnotation;
import de.invesdwin.context.jfreechart.plot.dataset.IndexedDateTimeOHLCDataset;
import de.invesdwin.util.error.UnknownArgumentException;
import de.invesdwin.util.math.Floats;

/**
 * A renderer that draws candlesticks on an {@link XYPlot} (requires a {@link OHLCDataset}). The example shown here is
 * generated by the <code>CandlestickChartDemo1.java</code> program included in the JFreeChart demo collection: <br>
 * <br>
 * <img src="../../../../../images/CandlestickRendererSample.png" alt="CandlestickRendererSample.png">
 * <P>
 * This renderer does not include code to calculate the crosshair point for the plot.
 */
//CHECKSTYLE:OFF
@NotThreadSafe
public class FastCandlestickRenderer extends AbstractXYItemRenderer
        implements IUpDownColorRenderer, IDelegatePriceLineXYItemRenderer {

    private static final double SMALL_AUTO_WIDTH_SCALING_MIN_ITEMS = 10;
    private static final double SMALL_AUTO_WIDTH_SCALING_MAX_ITEMS = 200;

    private static final float STROKE_SCALING_MIN_WIDTH = 0.3f;
    private static final float STROKE_SCALING_MIN_ITEMS = 200;
    private static final float STROKE_SCALING_MAX_ITEMS = 2500;

    /** For serialization. */
    private static final long serialVersionUID = 50390395841817121L;

    /**
     * The number (generally between 0.0 and 1.0) by which the available space automatically calculated for the candles
     * will be multiplied to determine the actual width to use.
     */
    private final double autoWidthFactor = 4.5 / 7;
    private final double autoWidthFactorSmall = 0.9;

    /** The maximum candlewidth in milliseconds. */
    private double maxCandleWidthInMilliseconds = 1000.0 * 60.0 * 60.0 * 20.0;

    /** Temporary storage for the maximum candle width. */
    private double maxCandleWidth;

    /**
     * The paint used to fill the candle when the price moved up from open to close.
     */
    private Color upColor;

    /**
     * The paint used to fill the candle when the price moved down from open to close.
     */
    private Color downColor;

    private final IndexedDateTimeOHLCDataset dataset;

    private BasicStroke itemStroke;

    private final XYPriceLineAnnotation priceLineAnnotation;

    public FastCandlestickRenderer(final IndexedDateTimeOHLCDataset dataset) {
        super();
        this.upColor = PriceInitialSettings.DEFAULT_UP_COLOR;
        this.downColor = PriceInitialSettings.DEFAULT_DOWN_COLOR;
        setSeriesPaint(0, upColor);
        setSeriesStroke(0, PriceInitialSettings.DEFAULT_SERIES_STROKE);
        this.dataset = dataset;
        this.priceLineAnnotation = new XYPriceLineAnnotation(dataset, this);
        addAnnotation(priceLineAnnotation);
    }

    @Override
    public IPriceLineRenderer getDelegatePriceLineRenderer() {
        return priceLineAnnotation;
    }

    @Override
    public IndexedDateTimeOHLCDataset getDataset() {
        return dataset;
    }

    /**
     * Returns the maximum width (in milliseconds) of each candle.
     *
     * @return The maximum candle width in milliseconds.
     *
     * @see #setMaxCandleWidthInMilliseconds(double)
     */
    public double getMaxCandleWidthInMilliseconds() {
        return this.maxCandleWidthInMilliseconds;
    }

    /**
     * Sets the maximum candle width (in milliseconds) and sends a {@link RendererChangeEvent} to all registered
     * listeners.
     *
     * @param millis
     *            The maximum width.
     *
     * @see #getMaxCandleWidthInMilliseconds()
     * @see #setCandleWidth(double)
     * @see #setAutoWidthMethod(int)
     * @see #setAutoWidthGap(double)
     * @see #setAutoWidthFactor(double)
     */
    public void setMaxCandleWidthInMilliseconds(final double millis) {
        this.maxCandleWidthInMilliseconds = millis;
        fireChangeEvent();
    }

    /**
     * Returns the paint used to fill candles when the price moves up from open to close.
     *
     * @return The paint (possibly <code>null</code>).
     *
     * @see #setUpColor(Paint)
     */
    @Override
    public Color getUpColor() {
        return this.upColor;
    }

    /**
     * Sets the paint used to fill candles when the price moves up from open to close and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param paint
     *            the paint (<code>null</code> permitted).
     *
     * @see #getUpColor()
     */
    @Override
    public void setUpColor(final Color upColor) {
        this.upColor = upColor;
        fireChangeEvent();
    }

    /**
     * Returns the paint used to fill candles when the price moves down from open to close.
     *
     * @return The paint (possibly <code>null</code>).
     *
     * @see #setDownColor(Paint)
     */
    @Override
    public Color getDownColor() {
        return this.downColor;
    }

    /**
     * Sets the paint used to fill candles when the price moves down from open to close and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param paint
     *            The paint (<code>null</code> permitted).
     */
    @Override
    public void setDownColor(final Color downColor) {
        this.downColor = downColor;
        fireChangeEvent();
    }

    /**
     * Returns the range of values the renderer requires to display all the items from the specified dataset.
     *
     * @param dataset
     *            the dataset (<code>null</code> permitted).
     *
     * @return The range (<code>null</code> if the dataset is <code>null</code> or empty).
     */
    @Override
    public Range findRangeBounds(final XYDataset dataset) {
        return findRangeBounds(dataset, true);
    }

    /**
     * Initialises the renderer then returns the number of 'passes' through the data that the renderer will require
     * (usually just one). This method will be called before the first item is rendered, giving the renderer an
     * opportunity to initialise any state information it wants to maintain. The renderer can do nothing if it chooses.
     *
     * @param g2
     *            the graphics device.
     * @param dataArea
     *            the area inside the axes.
     * @param plot
     *            the plot.
     * @param dataset
     *            the data.
     * @param info
     *            an optional info collection object to return data back to the caller.
     *
     * @return The number of passes the renderer requires.
     */
    @Override
    public XYItemRendererState initialise(final Graphics2D g2, final Rectangle2D dataArea, final XYPlot plot,
            final XYDataset dataset, final PlotRenderingInfo info) {

        // calculate the maximum allowed candle width from the axis...
        final ValueAxis axis = plot.getDomainAxis();
        final double x1 = axis.getLowerBound();
        final double x2 = x1 + this.maxCandleWidthInMilliseconds;
        final RectangleEdge edge = plot.getDomainAxisEdge();
        final double xx1 = axis.valueToJava2D(x1, dataArea, edge);
        final double xx2 = axis.valueToJava2D(x2, dataArea, edge);
        this.maxCandleWidth = Math.abs(xx2 - xx1);
        // Absolute value, since the relative x
        // positions are reversed for horizontal orientation

        return new XYItemRendererState(info);
    }

    public double getAutoWidthFactor() {
        return autoWidthFactor;
    }

    public double getAutoWidthFactorSmall() {
        return autoWidthFactorSmall;
    }

    /**
     * Draws the visual representation of a single data item.
     *
     * @param g2
     *            the graphics device.
     * @param state
     *            the renderer state.
     * @param dataArea
     *            the area within which the plot is being drawn.
     * @param info
     *            collects info about the drawing.
     * @param plot
     *            the plot (can be used to obtain standard color information etc).
     * @param domainAxis
     *            the domain axis.
     * @param rangeAxis
     *            the range axis.
     * @param dataset
     *            the dataset.
     * @param series
     *            the series index (zero-based).
     * @param item
     *            the item index (zero-based).
     * @param crosshairState
     *            crosshair information for the plot (<code>null</code> permitted).
     * @param pass
     *            the pass index.
     */
    @Override
    public void drawItem(final Graphics2D g2, final XYItemRendererState state, final Rectangle2D dataArea,
            final PlotRenderingInfo info, final XYPlot plot, final ValueAxis domainAxis, final ValueAxis rangeAxis,
            final XYDataset dataset, final int series, final int item, final CrosshairState crosshairState,
            final int pass) {

        final boolean horiz = isHorizontal(plot);

        if (!(dataset instanceof OHLCDataset)) {
            return;
        }
        final OHLCDataset highLowData = (OHLCDataset) dataset;

        final double x = highLowData.getXValue(series, item);
        final double yHigh = highLowData.getHighValue(series, item);
        final double yLow = highLowData.getLowValue(series, item);
        final double yOpen = highLowData.getOpenValue(series, item);
        final double yClose = highLowData.getCloseValue(series, item);

        final RectangleEdge domainEdge = plot.getDomainAxisEdge();
        final double xx = domainAxis.valueToJava2D(x, dataArea, domainEdge);

        final RectangleEdge edge = plot.getRangeAxisEdge();
        final double yyHigh = rangeAxis.valueToJava2D(yHigh, dataArea, edge);
        final double yyLow = rangeAxis.valueToJava2D(yLow, dataArea, edge);
        final double yyOpen = rangeAxis.valueToJava2D(yOpen, dataArea, edge);
        final double yyClose = rangeAxis.valueToJava2D(yClose, dataArea, edge);

        calculateItemStroke(state, getSeriesStroke(0));
        final double stickWidth = calculateStickWidth(state, dataArea, horiz);

        final Paint p = getItemPaint(series, item);
        final Stroke s = getItemStroke(series, item);

        g2.setStroke(s);
        g2.setPaint(p);

        final double yyMaxOpenClose = Math.max(yyOpen, yyClose);
        final double yyMinOpenClose = Math.min(yyOpen, yyClose);
        final double maxOpenClose = Math.max(yOpen, yClose);
        final double minOpenClose = Math.min(yOpen, yClose);

        // draw the upper shadow
        if (yHigh > maxOpenClose) {
            if (horiz) {
                g2.draw(new Line2D.Double(yyHigh, xx, yyMinOpenClose, xx));
            } else {
                g2.draw(new Line2D.Double(xx, yyHigh, xx, yyMinOpenClose));
            }
        }

        // draw the lower shadow
        if (yLow < minOpenClose) {
            if (horiz) {
                g2.draw(new Line2D.Double(yyLow, xx, yyMaxOpenClose, xx));
            } else {
                g2.draw(new Line2D.Double(xx, yyLow, xx, yyMaxOpenClose));
            }
        }

        // draw the body
        Rectangle2D body;
        if (horiz) {
            body = new Rectangle2D.Double(yyMinOpenClose, xx - stickWidth / 2, yyMaxOpenClose - yyMinOpenClose,
                    stickWidth);
        } else {
            body = new Rectangle2D.Double(xx - stickWidth / 2, yyMinOpenClose, stickWidth,
                    yyMaxOpenClose - yyMinOpenClose);
        }
        if (yClose > yOpen) {
            if (this.upColor != null) {
                g2.setPaint(this.upColor);
            } else {
                g2.setPaint(p);
            }
            g2.fill(body);
        } else {
            if (this.downColor != null) {
                g2.setPaint(this.downColor);
            } else {
                g2.setPaint(p);
            }
            g2.fill(body);
        }
    }

    @Override
    public Stroke getItemStroke(final int row, final int column) {
        return itemStroke;
    }

    public boolean isHorizontal(final XYPlot plot) {
        final boolean horiz;
        final PlotOrientation orientation = plot.getOrientation();
        if (orientation == PlotOrientation.HORIZONTAL) {
            horiz = true;
        } else if (orientation == PlotOrientation.VERTICAL) {
            horiz = false;
        } else {
            throw UnknownArgumentException.newInstance(PlotOrientation.class, orientation);
        }
        return horiz;
    }

    public void calculateItemStroke(final XYItemRendererState state, final Stroke seriesStroke) {
        final BasicStroke cStroke = (BasicStroke) seriesStroke;
        final float strokeScalingMaxWidth = cStroke.getLineWidth();

        final int itemCount = state.getLastItemIndex() - state.getFirstItemIndex();
        final float strokeWidth;
        if (itemCount > STROKE_SCALING_MAX_ITEMS) {
            strokeWidth = STROKE_SCALING_MIN_WIDTH;
        } else if (itemCount <= STROKE_SCALING_MIN_ITEMS) {
            strokeWidth = strokeScalingMaxWidth;
        } else {
            final float widthDifference = strokeScalingMaxWidth - STROKE_SCALING_MIN_WIDTH;
            final float itemDifference = itemCount / (STROKE_SCALING_MAX_ITEMS);
            strokeWidth = Floats.max(STROKE_SCALING_MIN_WIDTH,
                    strokeScalingMaxWidth - widthDifference * itemDifference);
        }
        this.itemStroke = new BasicStroke(strokeWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
    }

    public double calculateStickWidth(final XYItemRendererState state, final Rectangle2D dataArea,
            final boolean horiz) {
        double stickWidth;
        double xxWidth = 0;
        final int itemCount = state.getLastItemIndex() - state.getFirstItemIndex();
        if (horiz) {
            xxWidth = dataArea.getHeight() / itemCount;
        } else {
            xxWidth = dataArea.getWidth() / itemCount;
        }
        if (itemCount > SMALL_AUTO_WIDTH_SCALING_MAX_ITEMS) {
            //add a dynamic gap to remove the body on lots of items
            xxWidth -= 2 * (itemCount / 1000D);
        }

        if (itemCount > SMALL_AUTO_WIDTH_SCALING_MAX_ITEMS) {
            xxWidth *= autoWidthFactor;
        } else if (itemCount > SMALL_AUTO_WIDTH_SCALING_MIN_ITEMS) {
            final double autoWidthFactorDifference = autoWidthFactorSmall - autoWidthFactor;
            final double itemDifference = itemCount / SMALL_AUTO_WIDTH_SCALING_MAX_ITEMS;
            final double autoWidthFactorScaled = autoWidthFactorSmall - autoWidthFactorDifference * itemDifference;
            xxWidth *= autoWidthFactorScaled;
        } else {
            xxWidth *= autoWidthFactorSmall;
        }
        xxWidth = Math.min(xxWidth, this.maxCandleWidth);
        stickWidth = Math.max(Math.min(STROKE_SCALING_MIN_WIDTH, this.maxCandleWidth), xxWidth);
        return stickWidth;
    }

    @Override
    public Paint getItemPaint(final int row, final int column) {
        //determine up or down candle
        final int series = row, item = column;
        final Number yOpen = dataset.getOpen(series, item);
        final Number yClose = dataset.getClose(series, item);
        final boolean isUpCandle = yClose.doubleValue() > yOpen.doubleValue();

        //return the same color as that used to fill the candle
        if (isUpCandle) {
            return getUpColor();
        } else {
            return getDownColor();
        }
    }

    /**
     * Tests this renderer for equality with another object.
     *
     * @param obj
     *            the object (<code>null</code> permitted).
     *
     * @return <code>true</code> or <code>false</code>.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FastCandlestickRenderer)) {
            return false;
        }
        final FastCandlestickRenderer that = (FastCandlestickRenderer) obj;
        if (!PaintUtils.equal(this.upColor, that.upColor)) {
            return false;
        }
        if (!PaintUtils.equal(this.downColor, that.downColor)) {
            return false;
        }
        if (this.maxCandleWidthInMilliseconds != that.maxCandleWidthInMilliseconds) {
            return false;
        }
        if (this.autoWidthFactor != that.autoWidthFactor) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Returns a clone of the renderer.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException
     *             if the renderer cannot be cloned.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    protected void updateCrosshairValues(final CrosshairState crosshairState, final double x, final double y,
            final int datasetIndex, final double transX, final double transY, final PlotOrientation orientation) {
        //noop
    }

    @Override
    protected void addEntity(final EntityCollection entities, final Shape hotspot, final XYDataset dataset,
            final int series, final int item, final double entityX, final double entityY) {
        //noop
    }

    @Override
    public void drawAnnotations(final Graphics2D g2, final Rectangle2D dataArea, final ValueAxis domainAxis,
            final ValueAxis rangeAxis, final Layer layer, final PlotRenderingInfo info) {
        super.drawAnnotations(g2, dataArea, domainAxis, rangeAxis, layer, info);
    }

}
