package org.bir;

import java.util.List;

public class MixedDistance {
    public static double calculateDistance(
            FeatureVector fv1,
            FeatureVector fv2,
            List<FeatureSpec> specs
    ) {
        double sum = 0.0;

        for (FeatureSpec spec : specs) {
            double w = spec.weight();
            if (spec.type() == FeatureType.NUMERIC) {
                double d = Math.abs(
                        fv1.getNumeric(spec.name()) - fv2.getNumeric(spec.name())
                );
                sum += w * d;
            } else  {
                double d = fv1.getText(spec.name()).equals(fv2.getText(spec.name())) ? 0.0 : 1.0;
                sum += w * d;
            }
        }

        return sum;
    }
}
