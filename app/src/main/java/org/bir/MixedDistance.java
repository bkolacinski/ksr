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
            double w = spec.getWeight();
            if (spec.getType() == FeatureType.NUMERIC) {
                double d = Math.abs(
                        fv1.getNumeric(spec.getName()) - fv2.getNumeric(spec.getName())
                );
                sum += w * d;
            } else  {
                double d = fv1.getText(spec.getName()).equals(fv2.getText(spec.getName())) ? 0.0 : 1.0;
                sum += w * d;
            }
        }

        return sum;
    }
}
