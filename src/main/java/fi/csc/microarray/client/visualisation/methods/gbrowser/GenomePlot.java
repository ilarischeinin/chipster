package fi.csc.microarray.client.visualisation.methods.gbrowser;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.data.general.DatasetChangeEvent;

import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoordRegion;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.BpCoordRegionDouble;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.Chromosome;
import fi.csc.microarray.client.visualisation.methods.gbrowser.track.EmptyTrack;

/**
 * The main visual component for Genome Browser. Compatible with JFreeChart. 
 * 
 * @author Petri Klemelä, Aleksi Kallio
 */
public class GenomePlot extends Plot implements ChartMouseListener, Cloneable, Serializable {

	private List<View> views = new LinkedList<View>();
	private View dataView = null;
	private View overviewView = null;
	public ChartPanel chartPanel;

	public GenomePlot(boolean horizontal) throws FileNotFoundException, MalformedURLException {

		// add overview view
		this.overviewView = new HorizontalView(this, false, false, true);
		this.overviewView.margin = 0;
		this.views.add(overviewView);

		// add horizontal or circular data view
		if (horizontal) {
			this.dataView = new HorizontalView(this, true, true, false);

		} else {
			this.dataView = new CircularView(this, true, true, false);
			this.dataView.margin = 20;
			this.dataView.addTrack(new EmptyTrack(dataView, 30));
		}

		this.views.add(dataView);

		dataView.addRegionListener(new RegionListener() {
			public void regionChanged(BpCoordRegion bpRegion) {
				overviewView.highlight = bpRegion;
			}
		});
	}

	public View getDataView() {
		return dataView;
	}

	public View getOverviewView() {
		return overviewView;
	}

	public void start(String chromosome, Double chromosomeSizeBp) {
		overviewView.setBpRegion(new BpCoordRegionDouble(0d, chromosomeSizeBp, new Chromosome(chromosome)), false);
		dataView.setBpRegion(new BpCoordRegionDouble(0d, chromosomeSizeBp, new Chromosome(chromosome)), false);
	}

	public void moveDataBpRegion(Long moveTo, Long length) {
		
		BpCoordRegionDouble bpCoordRegion = new BpCoordRegionDouble(
				new Double(moveTo - (length/2)),
				new Double(moveTo + (length/2)), 
				dataView.getBpRegion().start.chr
		);
		dataView.setBpRegion(bpCoordRegion, false);
	}
	
	public void addDataRegionListener(RegionListener regionListener) {
		dataView.addRegionListener(regionListener);		
	}
	
	public String getPlotType() {
		return "GeneBrowser";
	}

	/**
	 * Draws the plot on a Java2D graphics device (such as the screen or a printer).
	 * 
	 * @param g2
	 *            the graphics device.
	 * @param area
	 *            the area within which the plot should be drawn.
	 * @param anchor
	 *            the anchor point (<code>null</code> permitted).
	 * @param parentState
	 *            the state from the parent plot, if there is one (<code>null</code> permitted.)
	 * @param info
	 *            collects info about the drawing (<code>null</code> permitted).
	 * @throws NullPointerException
	 *             if g2 or area is null.
	 */
	public void draw(java.awt.Graphics2D g2, java.awt.geom.Rectangle2D area, java.awt.geom.Point2D anchor, PlotState parentState, PlotRenderingInfo info) {

		if (info != null) {
			info.setPlotArea(area);
			info.setDataArea(area);
		}

		// this.setBackgroundPaint(Color.black);

		drawBackground(g2, area);
		drawOutline(g2, area);

		Shape savedClip = g2.getClip();
		g2.clip(area);

		Rectangle viewArea = (Rectangle) area.getBounds().clone();

		// Horizontal or vertical split
		if (true) {

			float[] viewHeights = new float[] { 0.1f, 0.9f };

			for (int i = 0; i < views.size(); i++) {
				if (i > 0) {
					viewArea.y += viewArea.height;
				}
				viewArea.height = (int) (area.getBounds().getHeight() * viewHeights[i]);
				g2.setClip(viewArea);
				views.get(i).drawView(g2, false);
			}

		} else {
			float[] viewWidths = new float[] { 0.05f, 0.95f };
			Rectangle lastArea = null;

			for (int i = 0; i < views.size(); i++) {
				if (lastArea != null) {
					viewArea.x = lastArea.x + lastArea.width;
					viewArea.y = lastArea.y;
					viewArea.height = lastArea.height;
				}
				g2.setColor(Color.black);
				viewArea.width = (int) (area.getBounds().getWidth() * viewWidths[i]);
				lastArea = (Rectangle) (viewArea.clone());

				View view = views.get(i);

				if (view instanceof VerticalView) {
					viewArea.grow(0, -view.margin);
				} else if (view instanceof HorizontalView) {
					viewArea.grow(-view.margin, 0);
				}

				g2.setClip(savedClip);
				g2.drawLine(viewArea.x - 1, 0, viewArea.x - 1, viewArea.height);

				g2.setClip(viewArea);
				view.drawView(g2, false);
			}
		}
		g2.setClip(savedClip);

		drawOutline(g2, area);
	}

	/**
	 * Implements the ChartMouseListener interface. This method does nothing.
	 * 
	 * @param event
	 *            the mouse event.
	 */
	public void chartMouseMoved(ChartMouseEvent event) {
	}

	public void chartMouseClicked(ChartMouseEvent e) {
	}

	/**
	 * Tests this plot for equality with an arbitrary object. Note that the plot's dataset is NOT included in the test for equality.
	 * 
	 * @param obj
	 *            the object to test against (<code>null</code> permitted).
	 * 
	 * @return <code>true</code> or <code>false</code>.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof GenomePlot)) {
			return false;
		}

		// can't find any difference...
		return true;
	}

	/**
	 * Provides serialization support.
	 * 
	 * @param stream
	 *            the output stream.
	 * 
	 * @throws IOException
	 *             if there is an I/O error.
	 * @throws NullPointerException
	 *             if stream is null.
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

	/**
	 * Provides serialization support.
	 * 
	 * @param stream
	 *            the input stream.
	 * 
	 * @throws IOException
	 *             if there is an I/O error.
	 * @throws ClassNotFoundException
	 *             if there is a classpath problem.
	 * @throws NullPointerException
	 *             if stream is null.
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
	}

	public void redraw() {
		this.datasetChanged(new DatasetChangeEvent(this, null));
	}

	public Collection<View> getViews() {
		return views;
	}
}