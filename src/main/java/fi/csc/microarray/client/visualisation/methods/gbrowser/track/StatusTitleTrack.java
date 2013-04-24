package fi.csc.microarray.client.visualisation.methods.gbrowser.track;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fi.csc.microarray.client.visualisation.methods.gbrowser.gui.Drawable;
import fi.csc.microarray.client.visualisation.methods.gbrowser.gui.GBrowserConstants;
import fi.csc.microarray.client.visualisation.methods.gbrowser.gui.RectDrawable;
import fi.csc.microarray.client.visualisation.methods.gbrowser.gui.TextDrawable;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.AreaRequestHandler;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.AreaResult;
import fi.csc.microarray.client.visualisation.methods.gbrowser.message.ColumnType;

/**
 * Track for placing title texts on top of other tracks.
 *
 */
public class StatusTitleTrack extends Track {

	private static final long VISIBLE_AFTER = 100; //ms
	private Color color;
	private String title;
	private Color bgColor;
	private Map<AreaRequestHandler, Long> queueLengths = new HashMap<AreaRequestHandler, Long>();
	private double angle;
	private long previousTime;
	private long hideTime = 0;

	public StatusTitleTrack(String title, Color color) {

		this.color = color;
		this.title = title;
		layoutHeight = 10;
	}
	
	public StatusTitleTrack(String title, Color color, Color bgColor) {

		this(title, color);
		this.bgColor = bgColor;
	}

	@Override
	public Collection<Drawable> getDrawables() {
		Collection<Drawable> drawables = getEmptyDrawCollection();
		
		if (bgColor != null) {
			drawables.add(new RectDrawable(0, 0, view.getWidth(), getHeight(), bgColor, bgColor));
		}
		
		long queueLength = getMaxQueueLength();
		
		if (queueLength > 0) {
		
			if (System.currentTimeMillis() - hideTime > VISIBLE_AFTER) {
				for (double d = 0; d < 1; d += 0.1) {
					int x = (int) (Math.sin(angle + d * Math.PI * 1.5) * 4);
					int y = (int) (Math.cos(angle + d * Math.PI * 1.5) * 4);			

					angle += 0.005 * (System.currentTimeMillis() - previousTime);
					previousTime = System.currentTimeMillis();

					if (angle < 0) {
						angle += Math.PI * 2;
					}

					Color c = GBrowserConstants.COLOR_BLUE;
					c = new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (d * 128));

					drawables.add(new RectDrawable(new Rectangle(x + 4, y + 3,  3, 3), c, c));
				} 		
			}
		} else {			
			hideTime = System.currentTimeMillis();
		}	
		
		drawables.add(new TextDrawable(13, 10, title, color));				
		return drawables;
	}

	private long getMaxQueueLength() {
		long max = 0; 
		
		for (Long value : queueLengths.values()) {
			max = Math.max(max, value);
		}
		
		return max;
	}

	public void processAreaResult(AreaResult areaResult) {

		AreaRequestHandler handler = areaResult.getStatus().areaRequestHandler;
		
		if (areaResult.getStatus().areaRequestCount >= 0) {
			
			if (!queueLengths.containsKey(handler)) {
				queueLengths.put(handler, 0l);
			}
			
			Long value = areaResult.getStatus().areaRequestCount; 
			queueLengths.put(handler, value);			 
		}
	}

	@Override
	public int getHeight() {
		return 10;
	}

    @Override
    public Map<AreaRequestHandler, Set<ColumnType>> requestedData() {
        return null;
    }

	@Override
	public String getName() {
		return "title";
	}
}