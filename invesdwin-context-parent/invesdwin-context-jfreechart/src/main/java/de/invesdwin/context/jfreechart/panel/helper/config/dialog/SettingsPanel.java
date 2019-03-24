package de.invesdwin.context.jfreechart.panel.helper.config.dialog;

import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JDialog;
import javax.swing.JPanel;

import de.invesdwin.context.jfreechart.panel.helper.config.PlotConfigurationHelper;
import de.invesdwin.context.jfreechart.panel.helper.config.dialog.style.StyleSettingsPanel;
import de.invesdwin.context.jfreechart.panel.helper.legend.HighlightedLegendInfo;

@NotThreadSafe
public class SettingsPanel extends JPanel {

    private final StyleSettingsPanel styleSettings;

    public SettingsPanel(final PlotConfigurationHelper plotConfigurationHelper, final HighlightedLegendInfo highlighted,
            final JDialog dialog) {
        this.styleSettings = new StyleSettingsPanel(plotConfigurationHelper, highlighted, dialog);
        add(styleSettings);
    }

}
