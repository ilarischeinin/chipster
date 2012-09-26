package fi.csc.microarray.client.visualisation.methods.gbrowser.dataFetcher;

import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Region;

public class Exon implements Comparable<Exon> {
	
	private Region region;
	private Feature feature;
	private int exonNumber;
	private Transcript transcript;

	public static enum Feature {

		CDS("CDS"), 
		EXON("exon"),
		START_CODON("start_codon"),
		STOP_CODON("stop_codon"),
		UNRECOGNIZED(null);

		private String id;

		Feature(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}
	
	private Feature getFeature(String id) {
		for (Feature feature : Feature.values()) {
			if (feature.getId().equals(id)) {
				return feature;
			}
		}
		return Feature.UNRECOGNIZED;
	}
	
	public Exon(Region region, String feature, int exonNumber) {
		this.region = region;
		this.feature = getFeature(feature);
		this.exonNumber = exonNumber;
	}

	public int compareTo(Exon other) {
		
		int transcriptComparison = this.transcript.compareTo(other.transcript);
		int exonNumberComparison = ((Integer)this.exonNumber).compareTo((Integer)other.getExonNumber());
		int featureComparison = this.feature.compareTo(other.feature);
		
		if (transcriptComparison != 0) {
			return transcriptComparison;
		} else if (exonNumberComparison != 0){
			return exonNumberComparison;
		} else {
			return featureComparison;
		}

//		int featureComparison = 0;
//		int exonNumberComparison = 0;
//
//		int regionComparison = this.region.compareTo(other.region);
//		
//		if (regionComparison != 0) {
//			return regionComparison;
//		}
//
//		if (this.feature != null) {
//			featureComparison = this.feature.compareTo(other.getFeature());
//		} else if (other.getFeature() != null) {
//			featureComparison = 1;
//		}
//		
//		exonNumberComparison = ((Integer)this.exonNumber).compareTo((Integer)other.getExonNumber());
//
//		if (featureComparison != 0) {
//			return featureComparison;
//		}
//		return exonNumberComparison;
	}	
	
	private Object getExonNumber() {
		return exonNumber;
	}

	public Feature getFeature() {
		return feature;
	}

	@Override
	public int hashCode() {
		return transcript.hashCode() << 8 + exonNumber << 2 + feature.ordinal();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Exon) {
			return this.compareTo((Exon) o) == 0;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		
		return region.toString(true) + ", " + feature;
	}

	public Region getRegion() {
		return region;
	}

	public Integer getIndex() {
		return exonNumber;
	}

	public void setTranscript(Transcript transc) {
		this.transcript = transc;
	}
}