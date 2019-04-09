package de.invesdwin.context.jfreechart.panel.helper.config.dialog.parameter.modifier;

import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JComponent;
import javax.swing.JTextField;

import de.invesdwin.context.jfreechart.panel.helper.config.series.ISeriesParameter;

@NotThreadSafe
public class DoubleParameterSettingsModifier extends AParameterSettingsModifier {

    public DoubleParameterSettingsModifier(final ISeriesParameter parameter) {
        super(parameter);
    }

    @Override
    public JComponent newComponent(final Runnable modificationListener) {
        return new JTextField();
    }

}
