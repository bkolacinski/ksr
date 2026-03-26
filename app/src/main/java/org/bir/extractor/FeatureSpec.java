package org.bir.extractor;

public abstract class FeatureSpec<T> {
    protected String name;
    protected Double weight;
    protected FeatureType type;

    public FeatureSpec(Double weight) {
        this.weight = weight;
    }

    public abstract T calculate(ReutersArticle article);
    
    public Double getWeight() {
        return this.weight;
    }

    public FeatureType getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }
}
