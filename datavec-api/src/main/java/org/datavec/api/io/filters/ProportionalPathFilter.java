package org.datavec.api.io.filters;

import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.io.labels.PathLabelGenerator;
import org.datavec.api.writable.Writable;

import java.net.URI;
import java.util.*;
/**
 * This is an EXPERIMENTAL work, no stability is guaranteed in any shape or form.
 * This path filter is supposed to extend the functionality of the existing BalancedPathFilter by allowing certain labels to have more data than others. 
 * 
 * The proportionality is taken in as an integer array ratio. 
 * For example, the dataset contains label A and B where A is needed to have twice the data point compared to B. The ratio array would be {2,1}
 * The filter would take in the path, shuffle, group the path by labels, and interleave the labels as normally would in BalancedPathFilter.
 * In the case above, the labels would be interleaved in BalancedPathFilter as: AB AB AB AB ..., and in this filter as: AAB AAB AAB AAB ...
 * 
 * The minimum and maximum labels per path are also taken in as arrays. The change in one minimum or maximum for one label will proportionally affect other labels.
 * The number of data points for each label can be calculated by the following:
 * 
 * Maximum: minimum(r_i)*ratio_i; r_i = round(max_i/ratio_i)
 * Minimum: maximum(r_i)*ratio_i; r_i = round(min_i/ratio_i)
 *  
 * In the same example, supposed that the maximum of A is set to 50 and B is set to 30, this filter will only attempt to find 25 from B.
 * **/
public class ProportionalPathFilter extends RandomPathFilter{
	protected PathLabelGenerator labelGenerator;
	protected int maxLabels;
	protected int[] minPathsPerLabel, maxPathsPerLabel, ratio;
	protected String[] labels = null;

	/** Calls {@code this(random, extensions, labelGenerator, 0, 0, null, null, ratio)}. */
	public ProportionalPathFilter(Random random, String[] extensions, PathLabelGenerator labelGenerator, int[] ratio) {
        this(random, extensions, labelGenerator, 0, 0, null, null, ratio);
    }

    /** Calls {@code this(random, null, labelGenerator, 0, 0, ratio, null, maxPathsPerLabel)}. */
    public ProportionalPathFilter(Random random, PathLabelGenerator labelGenerator, int[] ratio, int[] maxPathsPerLabel) {
        this(random, null, labelGenerator, 0, 0, ratio, null, maxPathsPerLabel);
    }

    /** Calls {@code this(random, extensions, labelGenerator, 0, 0, ratio, null, maxPathsPerLabel)}. */
    public ProportionalPathFilter(Random random, String[] extensions, PathLabelGenerator labelGenerator, int[] ratio,
                    int[] maxPathsPerLabel) {
        this(random, extensions, labelGenerator, 0, 0, ratio, null, maxPathsPerLabel);
    }

    /** Calls {@code  this(random, null, labelGenerator, maxPaths, maxLabels, ratio, null, maxPathsPerLabel)}. */
    public ProportionalPathFilter(Random random, PathLabelGenerator labelGenerator, int maxPaths, int maxLabels, int[] ratio,
                    int[] maxPathsPerLabel) {
        this(random, null, labelGenerator, maxPaths, maxLabels, ratio, null, maxPathsPerLabel);
    }

    /** Calls {@code this(random, extensions, labelGenerator, 0, maxLabels, ratio, null, maxPathsPerLabel)}. */
    public ProportionalPathFilter(Random random, String[] extensions, PathLabelGenerator labelGenerator, int maxLabels, int[] ratio,
                    int[] maxPathsPerLabel) {
        this(random, extensions, labelGenerator, 0, maxLabels, ratio, null, maxPathsPerLabel);
    }
	public ProportionalPathFilter(Random random, String[] extensions, PathLabelGenerator labelGenerator, int maxPaths,
        int maxLabels,int[] ratio, int[] minPathsPerLabel, int[] maxPathsPerLabel, String... labels){
		super(random, extensions, maxPaths);
		this.labelGenerator = labelGenerator;
        this.maxLabels = maxLabels;
        this.minPathsPerLabel = minPathsPerLabel==null? new int[]{}:minPathsPerLabel;
        this.maxPathsPerLabel = maxPathsPerLabel==null? new int[]{}:maxPathsPerLabel;
        this.ratio = ratio==null? new int[]{}:ratio;
        this.labels = labels;
	}
	protected boolean acceptLabel(String name) {
        if (labels == null || labels.length == 0) {
            return true;
        }
        for (String label : labels) {
            if (name.equals(label)) {
                return true;
            }
        }
        return false;
    }
	@Override
    public URI[] filter(URI[] paths) {
        paths = super.filter(paths);
        if (labelGenerator == null)
            labelGenerator = new ParentPathLabelGenerator();
        Map<Writable, List<URI>> labelPaths = new LinkedHashMap<Writable, List<URI>>();
        for (int i = 0; i < paths.length; i++) {
            URI path = paths[i];
            Writable label = labelGenerator.getLabelForPath(path);
            if (!acceptLabel(label.toString())) {
                continue;
            }
            List<URI> pathList = labelPaths.get(label);
            if (pathList == null) {
                if (maxLabels > 0 && labelPaths.size() >= maxLabels) {
                    continue;
                }
                labelPaths.put(label, pathList = new ArrayList<URI>());
            }
            pathList.add(path);
        }
        int labelNum = labelPaths.size();
        int[] proportion = new int[labelNum], 
        		minpl = new int[labelNum], 
        		maxpl = new int[labelNum], 
        		npath = new int[labelNum],
        		carriage = new int[labelNum];
        double[] maxlist = new double[labelNum], minlist = new double[labelNum];
        ArrayList<List<URI>> rpaths = new ArrayList<>(labelPaths.values());
        for(int i = 0; i< labelNum; i++){
        	if(i<ratio.length){
        		assert(ratio[i]>=0);
        		proportion[i] = ratio[i];
        	}
        	else{
        		proportion[i] = 1;
        	}
        	if(i<minPathsPerLabel.length){
        		assert(minPathsPerLabel[i]>=0);
        		minpl[i] = minPathsPerLabel[i];
        	}
        	else{
        		minpl[i] = 0;
        	}
        	if(i<maxPathsPerLabel.length){
        		assert(maxPathsPerLabel[i]>=0);
        		maxpl[i] = maxPathsPerLabel[i];
        	}
        	else{
        		maxpl[i] = 0;
        	}
        	npath[i] = rpaths.get(i).size();
        	npath[i] = maxpl[i]>0? Math.min(npath[i], maxpl[i]):npath[i];
        	maxlist[i] = proportion[i]>0? (double)npath[i]/(double)proportion[i]:Integer.MAX_VALUE;
        	minlist[i] = proportion[i]>0? (double)minpl[i]/(double)proportion[i]:0;
        }
        
        int minCount = Integer.MAX_VALUE;
        for(double i:maxlist){
        	minCount = (int) Math.min(minCount, Math.round(i));
        }
        for(double i:minlist){
        	minCount = (int) Math.max(minCount, Math.round(i));
        }
       
        ArrayList<URI> newpaths = new ArrayList<URI>();
        for (int i = 0; i < minCount; i++) {
        	for(int j = 0; j<rpaths.size(); j++){
        		for(int k=0; k<proportion[j]; carriage[j]++,k++){
        			if(carriage[j]<rpaths.get(j).size()){
        				newpaths.add(rpaths.get(j).get(carriage[j]));
        			}
            	}
        	}
        }
        return newpaths.toArray(new URI[newpaths.size()]);
    }
}
