package fi.csc.microarray.client.visualisation.methods.gbrowser;

import java.awt.Cursor;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import fi.csc.microarray.client.visualisation.NonScalableChartPanel;



public class GenomeBrowserStarter {

//	private static final File BED_READ_DATA_FILE = new File("/home/akallio/Desktop/Bcell_expression.reads");
	private static final File ELAND_DATA_FILE = new File("/home/akallio/chipster-share/ngs/STAT1/STAT1_treatment_aggregated_filtered_sorted_chr1.txt");
	private static final File MACS_DATA_FILE = new File("/home/akallio/chipster-share/ngs/STAT1/STAT1_peaks_sorted.bed");
	private static final File URL_ROOT;

	static {
			URL_ROOT = new File("/home/akallio/chipster-share/ngs/annotations");
	}

	public static void main(String[] args) throws IOException {
		GenomePlot plot = new GenomePlot(true);
		TrackFactory.addCytobandTracks(plot, new DataSource(URL_ROOT, "Homo_sapiens.GRCh37.57_karyotype.tsv"));
		
		TrackFactory.addThickSeparatorTrack(plot);
		TrackFactory.addTitleTrack(plot, "Annotations");
		
		TrackFactory.addGeneTracks(plot, new DataSource(URL_ROOT, "Homo_sapiens.NCBI36.54_genes.tsv"), new DataSource(URL_ROOT, "Homo_sapiens.NCBI36.54_transcripts.tsv"));
//		TrackFactory.addMirnaTracks(plot, new DataSource(URL_ROOT, "Homo_sapiens.NCBI36.54_miRNA.tsv"));

		// Example peak: choromosome 21 in front of IFNAR2 gene (location 33,525,000)
		// Example peak: choromosome 21 in front of IFNAR1 gene (location 33,620,000)
		
		TrackFactory.addThickSeparatorTrack(plot);
		TrackFactory.addTitleTrack(plot, "Peaks");
		
		TrackFactory.addPeakTrack(plot, new DataSource(MACS_DATA_FILE));

		TrackFactory.addThickSeparatorTrack(plot);
		TrackFactory.addTitleTrack(plot, "Reads");

		TrackFactory.addReadTracks(
				plot, 
				new DataSource(ELAND_DATA_FILE),
				new DataSource(URL_ROOT, "Homo_sapiens.NCBI36.54_seq.tsv"),
				true
		);

//		TrackFactory.addReadTracks(
//				plot, 
//				new DataSource(BED_READ_DATA_FILE),
//				new DataSource(URL_ROOT, "Homo_sapiens.NCBI36.54_seq.tsv"),
//				true,
//				new BEDReadParser()
//		);

		TrackFactory.addRulerTrack(plot);
		plot.start("1", 1024 * 1024 * 250d);
		plot.moveDataBpRegion(1000000L, 100000L);
		
		//ChartPanel panel = new ChartPanel(new JFreeChart(plot));
		ChartPanel panel = new NonScalableChartPanel(new JFreeChart(plot));
		panel.setPreferredSize(new Dimension(800, 2000));
		plot.chartPanel = panel;

		panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		
		for (View view : plot.getViews()){
			panel.addMouseListener(view);
			panel.addMouseMotionListener(view);
			panel.addMouseWheelListener(view);
		}

		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
}