package de.invesdwin.context.jfreechart.panel.helper.legend;

import javax.annotation.concurrent.Immutable;

import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import de.invesdwin.context.jfreechart.panel.InteractiveChartPanel;
import de.invesdwin.context.jfreechart.panel.basis.CustomLegendTitle;
import de.invesdwin.context.jfreechart.plot.XYPlots;
import de.invesdwin.context.jfreechart.plot.dataset.DisabledXYDataset;
import de.invesdwin.context.jfreechart.plot.renderer.DisabledXYItemRenderer;
import de.invesdwin.util.assertions.Assertions;

@Immutable
public class HighlightedLegendInfo {

    private final InteractiveChartPanel chartPanel;
    private final int subplotIndex;
    private final XYPlot plot;
    private final CustomLegendTitle title;
    private final int datasetIndex;

    public HighlightedLegendInfo(final InteractiveChartPanel chartPanel, final int subplotIndex, final XYPlot plot,
            final CustomLegendTitle title, final int datasetIndex) {
        this.chartPanel = chartPanel;
        this.subplotIndex = subplotIndex;
        this.plot = plot;
        this.title = title;
        this.datasetIndex = datasetIndex;
    }

    public int getSubplotIndex() {
        return subplotIndex;
    }

    public XYPlot getPlot() {
        return plot;
    }

    public CustomLegendTitle getTitle() {
        return title;
    }

    public int getDatasetIndex() {
        return datasetIndex;
    }

    public String getSeriesKey() {
        return String.valueOf(getDataset().getSeriesKey(0));
    }

    public boolean isPriceSeries() {
        return chartPanel.getDataset() == DisabledXYDataset.maybeUnwrap(getDataset());
    }

    public XYDataset getDataset() {
        return plot.getDataset(datasetIndex);
    }

    public XYItemRenderer getRenderer() {
        return plot.getRenderer(datasetIndex);
    }

    public void setRenderer(final XYItemRenderer renderer) {
        plot.setRenderer(datasetIndex, renderer);
    }

    public boolean isHidden() {
        return getRenderer() instanceof DisabledXYItemRenderer;
    }

    public void setHidden(final boolean hidden) {
        chartPanel.getPlotLegendHelper().setDatasetHidden(plot, datasetIndex, hidden);
    }

    public boolean isRemovable() {
        return chartPanel.getPlotLegendHelper().isDatasetRemovable(getDataset());
    }

    public void removeSeries() {
        Assertions.checkTrue(isRemovable());
        chartPanel.getPlotConfigurationHelper().removeInitialSeriesSettings(getSeriesKey());
        XYPlots.removeDataset(plot, datasetIndex);
        XYPlots.updateRangeAxes(plot);
        chartPanel.getCombinedPlot().removeEmptyPlotsAndResetTrashPlot();
    }

}
