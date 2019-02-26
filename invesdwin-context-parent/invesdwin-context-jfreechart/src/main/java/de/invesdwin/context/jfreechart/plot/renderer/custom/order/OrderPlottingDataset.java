package de.invesdwin.context.jfreechart.plot.renderer.custom.order;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.xy.AbstractXYDataset;

import de.invesdwin.context.jfreechart.plot.dataset.IIndexedDateTimeXYDataset;
import de.invesdwin.context.jfreechart.plot.dataset.IPlotSourceDataset;
import de.invesdwin.context.jfreechart.plot.dataset.IndexedDateTimeOHLCDataset;

@NotThreadSafe
public class OrderPlottingDataset extends AbstractXYDataset implements IPlotSourceDataset, IIndexedDateTimeXYDataset {

    private final String seriesKey;
    private final IndexedDateTimeOHLCDataset ohlcDataset;
    private final Set<DatasetChangeListener> changeListeners = new LinkedHashSet<>();
    private int precision;
    private XYPlot plot;
    private DatasetGroup group;
    private String rangeAxisId;
    private final Map<String, OrderPlottingDataItem> orderId_item = new LinkedHashMap<>();

    public OrderPlottingDataset(final String seriesKey, final IndexedDateTimeOHLCDataset ohlcDataset) {
        this.seriesKey = seriesKey;
        this.ohlcDataset = ohlcDataset;
    }

    @Override
    public void addChangeListener(final DatasetChangeListener listener) {
        changeListeners.add(listener);
    }

    @Override
    public void removeChangeListener(final DatasetChangeListener listener) {
        changeListeners.remove(listener);
    }

    @Override
    public DatasetGroup getGroup() {
        return group;
    }

    @Override
    public void setGroup(final DatasetGroup group) {
        this.group = group;
    }

    @Override
    public XYPlot getPlot() {
        return plot;
    }

    @Override
    public void setPlot(final XYPlot plot) {
        this.plot = plot;
    }

    @Override
    public int getPrecision() {
        return precision;
    }

    @Override
    public void setPrecision(final int precision) {
        this.precision = precision;
    }

    @Override
    public String getRangeAxisId() {
        return rangeAxisId;
    }

    @Override
    public void setRangeAxisId(final String rangeAxisId) {
        this.rangeAxisId = rangeAxisId;
    }

    @Override
    public int getItemCount(final int series) {
        return ohlcDataset.getItemCount(series);
    }

    @Override
    public Number getX(final int series, final int item) {
        return ohlcDataset.getX(series, item);
    }

    @Override
    public Number getY(final int series, final int item) {
        //need to return the price here in orger to get axis scaling done properly
        return ohlcDataset.getY(series, item);
    }

    @Override
    public int getSeriesCount() {
        return 1;
    }

    @Override
    public String getSeriesKey(final int series) {
        return seriesKey;
    }

    @Override
    public double getXValueAsDateTime(final int series, final int item) {
        return ohlcDataset.getXValueAsDateTime(series, item);
    }

    @Override
    public int getDateTimeAsItemIndex(final int series, final Date time) {
        return ohlcDataset.getDateTimeAsItemIndex(series, time);
    }

    public void addOrUpdate(final OrderPlottingDataItem item) {
        orderId_item.put(item.getOrderId(), item);
    }

    public void remove(final String orderId) {
        orderId_item.remove(orderId);
    }

    @Override
    public boolean isLegendValueVisible() {
        return false;
    }

}