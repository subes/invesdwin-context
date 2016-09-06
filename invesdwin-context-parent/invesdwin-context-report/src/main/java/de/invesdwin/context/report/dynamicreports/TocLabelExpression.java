package de.invesdwin.context.report.dynamicreports;

import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import net.sf.dynamicreports.jasper.base.tableofcontents.JasperTocHeading;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.definition.DRICustomValues;
import net.sf.dynamicreports.report.definition.ReportParameters;

@NotThreadSafe
public class TocLabelExpression extends AbstractSimpleExpression<String> {
    private static final long serialVersionUID = 1L;

    @Override
    public String evaluate(final ReportParameters reportParameters) {
        final DRICustomValues customValues = (DRICustomValues) reportParameters.getParameterValue(DRICustomValues.NAME);
        final Map<String, JasperTocHeading> tocHeadings = customValues.getTocHeadings();
        JasperTocHeading maxHeading = null;
        for (final JasperTocHeading heading : tocHeadings.values()) {
            maxHeading = heading;
        }
        if (maxHeading != null) {
            return maxHeading.getText().replace('\t', ' ').replace("  ", " ");
        } else {
            return "";
        }
    }
}