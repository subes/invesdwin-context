package de.invesdwin.context.report.jfreechart;

import java.awt.Dimension;

import javax.annotation.concurrent.NotThreadSafe;

import de.invesdwin.util.bean.AValueObject;

@NotThreadSafe
public class JFreeChartExporterSettings extends AValueObject {

    private final Dimension bounds;
    private Double fontMultiplier;

    public JFreeChartExporterSettings(final Dimension bounds) {
        this.bounds = bounds;
    }

    public Dimension getBounds() {
        return bounds;
    }

    public JFreeChartExporterSettings withFontMultiplier(final Double fontMultiplier) {
        this.fontMultiplier = fontMultiplier;
        return this;
    }

    public Double getFontMultiplier() {
        return fontMultiplier;
    }

}
