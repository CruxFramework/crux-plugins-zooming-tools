/*
 * Copyright 2014 cruxframework.org.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.cruxframework.crux.plugin.zoomingtools.client;

import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.PartialSupport;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Samuel Almeida Cardoso (samuel@cruxframework.org)
 * @see http://lab.hakim.se/zoom-js/
 * Licensed by http://opensource.org/licenses/MIT
 */
@PartialSupport
public class Zoom
{
	private static final Logger LOG = Logger.getLogger(Zoom.class.getName());
	
	private static Zoom instance;

	private Zoom()
	{
	}

	public static Zoom getInstance()
	{
		if(isSupported())
		{
			if(instance == null)
			{
				instance = new Zoom();
				instance.setEasing();
				Scheduler.get().scheduleDeferred(new ScheduledCommand() 
				{
					@Override
					public void execute() 
					{
						//at this point, instance var is already created
						instance.addEventListeners();		
					}
				});
			}
			return instance;
		}
		return null;
	}

	// Private variable that controls pan's flow
	private boolean keepPan = true;
	
	// The current zoom level (scale)
	private double level = 1d;

	// The current mouse position, used for panning
	private	double mouseX = 0, mouseY = 0;

	// Timeout before pan is activated
	private Integer panEngageTimeout = -1, panUpdateInterval = -1;

	public native double getXScrollOffset() /*-{
		return $wnd.scrollX !== undefined ? $wnd.scrollX : $wnd.pageXOffset;
	}-*/;

	public native double getYScrollOffset() /*-{
		return $wnd.scrollY !== undefined ? $wnd.scrollY : $wnd.pageYOffset;
	}-*/;

	public native double getWindowInnerWidth() /*-{
		return $wnd.innerWidth;
	}-*/;

	public native double getWindowInnerHeight() /*-{
		return $wnd.innerHeight;
	}-*/;

	public native double getWidthBoundingClientRect(Element element) /*-{
		return element.getBoundingClientRect().width;
	}-*/;

	public native double getLeftBoundingClientRect(Element element) /*-{
		return element.getBoundingClientRect().left;
	}-*/;

	public native double getTopBoundingClientRect(Element element) /*-{
		return element.getBoundingClientRect().top;
	}-*/;
	
	public native double getRightBoundingClientRect(Element element) /*-{
		return element.getBoundingClientRect().right;
	}-*/;

	public native double getHeightBoundingClientRect(Element element) /*-{
		return element.getBoundingClientRect().height;
	}-*/;

	private static native boolean isSupported() /*-{
	 	return 'WebkitTransform' in $doc.body.style ||
			   'MozTransform' in $doc.body.style    ||
			   'msTransform' in $doc.body.style     ||
			   'OTransform' in $doc.body.style      ||
			   'transform' in $doc.body.style;
  	}-*/;

	// The easing that will be applied when we zoom in/out
	private native void setEasing() /*-{
	 	$doc.body.style.transition = 'transform 0.8s ease';
		$doc.body.style.OTransition = '-o-transform 0.8s ease';
		$doc.body.style.msTransition = '-ms-transform 0.8s ease';
		$doc.body.style.MozTransition = '-moz-transform 0.8s ease';
		$doc.body.style.WebkitTransition = '-webkit-transform 0.8s ease';
	}-*/;

	private native void addEventListeners() /*-{
		// Zoom out if the user hits escape
		$doc.addEventListener( 'keyup', function( event ) {
			if( @org.cruxframework.crux.plugin.zoomingtools.client.Zoom::getInstance()().@org.cruxframework.crux.plugin.zoomingtools.client.Zoom::getLevel()() !== 1 && event.keyCode === 27 ) {
				@org.cruxframework.crux.plugin.zoomingtools.client.Zoom::getInstance()().@org.cruxframework.crux.plugin.zoomingtools.client.Zoom::out()();
			}
		} );

		// Monitor mouse movement for panning
		$doc.addEventListener( 'mousemove', function( event ) {
			if( @org.cruxframework.crux.plugin.zoomingtools.client.Zoom::getInstance()().@org.cruxframework.crux.plugin.zoomingtools.client.Zoom::getLevel()() !== 1 ) {
				@org.cruxframework.crux.plugin.zoomingtools.client.Zoom::getInstance()().@org.cruxframework.crux.plugin.zoomingtools.client.Zoom::setMouseX(D)(event.clientX);
				@org.cruxframework.crux.plugin.zoomingtools.client.Zoom::getInstance()().@org.cruxframework.crux.plugin.zoomingtools.client.Zoom::setMouseY(D)(event.clientY);
			}
		} );
	}-*/;

	private void magnify(double pageOffsetX, double pageOffsetY, double elementOffsetX, double elementOffsetY, double scale)
	{
		doMagnify(pageOffsetX, pageOffsetY, elementOffsetX, elementOffsetY, scale);
		setLevel(scale);
	}
	
	private static native boolean isElementInDOM(Element element) /*-{
		return element.getBoundingClientRect().width  != 0 ||
			   element.getBoundingClientRect().height != 0;
	}-*/;
	
	/**
	 * Applies the CSS required to zoom in, prioritizes use of CSS3
	 * transforms but falls back on zoom for IE.
	 *
	 * @param pageOffsetX
	 * @param pageOffsetY
	 * @param elementOffsetX
	 * @param elementOffsetY
	 * @param scale
	 */
	private native void doMagnify(double pageOffsetX, double pageOffsetY, double elementOffsetX, double elementOffsetY, double scale) /*-{
		if( @org.cruxframework.crux.plugin.zoomingtools.client.Zoom::isSupported()() ) {
			var origin = pageOffsetX +'px '+ pageOffsetY +'px',
				transform = 'translate('+ -elementOffsetX +'px,'+ -elementOffsetY +'px) scale('+ scale +')';

			$doc.body.style.transformOrigin = origin;
			$doc.body.style.OTransformOrigin = origin;
			$doc.body.style.msTransformOrigin = origin;
			$doc.body.style.MozTransformOrigin = origin;
			$doc.body.style.WebkitTransformOrigin = origin;

			$doc.body.style.transform = transform;
			$doc.body.style.OTransform = transform;
			$doc.body.style.msTransform = transform;
			$doc.body.style.MozTransform = transform;
			$doc.body.style.WebkitTransform = transform;
		}
		else {
			// Reset all values
			if( scale === 1 ) {
				$doc.body.style.position = '';
				$doc.body.style.left = '';
				$doc.body.style.top = '';
				$doc.body.style.width = '';
				$doc.body.style.height = '';
				$doc.body.style.zoom = '';
			}
			// Apply scale
			else 
			{
				$doc.body.style.position = 'relative';
				$doc.body.style.left = ( - ( pageOffsetX + elementOffsetX ) / scale ) + 'px';
				$doc.body.style.top = ( - ( pageOffsetY + elementOffsetY ) / scale ) + 'px';
				$doc.body.style.width = ( scale * 100 ) + '%';
				$doc.body.style.height = ( scale * 100 ) + '%';
				$doc.body.style.zoom = scale;
			}
		}
	}-*/;

	/**
	 * Pan the document when the mouse cursor approaches the edges of the $wnd.
	 */
	public void pan()
	{
		Double range = 0.12;
		Double rangeX = getWindowInnerWidth() * range;
		Double rangeY = getWindowInnerHeight() * range;

		// Up
		if( mouseY < rangeY ) 
		{
			Window.scrollTo( new Double( getXScrollOffset() ).intValue(), new Double( getYScrollOffset() - ( 1 - ( mouseY / rangeY ) ) * ( 14 / level ) ).intValue());
		}
		// Down
		else if( mouseY > getWindowInnerHeight() - rangeY ) 
		{
			Window.scrollTo( new Double( getXScrollOffset() ).intValue(), new Double( getYScrollOffset() + ( 1 - ( getWindowInnerHeight() - mouseY ) / rangeY ) * ( 14 / level ) ).intValue() );
		}
		// Left
		if( mouseX < rangeX ) 
		{
			Window.scrollTo( new Double( getXScrollOffset() - ( 1 - ( mouseX / rangeX ) ) * ( 14 / level ) ).intValue(), new Double( getYScrollOffset() ).intValue() );
		}
		// Right
		else if( mouseX > getWindowInnerWidth() - rangeX ) 
		{
			Window.scrollTo( new Double( getXScrollOffset() + ( 1 - ( getWindowInnerWidth() - mouseX ) / rangeX ) * ( 14 / level ) ).intValue(), new Double( getYScrollOffset() ).intValue() );
		}
	}

	public void to(Widget widget)
	{
		to(widget.getElement());
	}

	
	public void to(Element element)
	{
		to(element, true);
	}

	/**
	 * Zooms in on either a rectangle or HTML element.
	 *
	 * @param {Object} options
	 *   - element: HTML element to zoom in on
	 *   OR
	 *   - x/y: coordinates in non-transformed space to zoom in on
	 *   - width/height: the portion of the screen to zoom in on
	 *   - scale: can be used instead of width/height to explicitly set scale
	 */
	public void to(final Element element, final boolean pan)
	{
		Scheduler.get().scheduleFixedDelay(new RepeatingCommand() 
		{
			@Override
			public boolean execute() 
			{
				LOG.info("Zooming activated: waiting for element present in DOM.");
		
				if(isElementInDOM(element))
				{
					doTo(element, pan);
					return false;
				}
				return true;
			}
		}, 100);		
	}
	
	private void doTo(Element element, boolean pan)
	{
		// Due to an implementation limitation we can't zoom in
		// to another element without zooming out first
		if(level != 1) 
		{
			out();
			return;
		}

		// Space around the zoomed in element to leave on screen
		int padding = 20;
		double width = getWidthBoundingClientRect(element) + ( padding * 2 );
		double height = getHeightBoundingClientRect(element) + ( padding * 2 );
		double x = getLeftBoundingClientRect(element) - padding;
		double y = getTopBoundingClientRect(element) - padding;
		
		// If width/height values are set, calculate scale from those values
		double scale = Math.max( Math.min( getWindowInnerWidth() / width, getWindowInnerHeight() / height ), 1 );

		if(scale > 1)
		{
			x *= scale;
		}

		magnify(getXScrollOffset(), getYScrollOffset(), x, y, scale);

		if(pan) 
		{
			keepPan = true;
			// Wait with engaging panning as it may conflict with the
			// zoom transition
			Scheduler.get().scheduleFixedDelay(new RepeatingCommand() 
			{
				@Override
				public boolean execute() 
				{
					Scheduler.get().scheduleFixedPeriod(new RepeatingCommand() 
					{
						@Override
						public boolean execute() 
						{
							pan();
							if(keepPan)
							{
								return true;
							}
							return false;
						}
					}, 1000/60);
					return false;
				}
			}, 800);
		}
	}

	//	public void to(Integer x, Integer y)
	//	{
	//		
	//	}

	public void out()
	{
		keepPan = false;
		if(level != 1d)
		{
			magnify( getXScrollOffset(), getYScrollOffset(), 0d, 0d, 1d );
			level = 1d;
		}
	}

	public void setMouseX(double mouseX) 
	{
		this.mouseX = mouseX;
	}

	public double getMouseX() 
	{
		return mouseX;
	}

	public void setMouseY(double mouseY) 
	{
		this.mouseY = mouseY;
	}

	public double getMouseY() 
	{
		return mouseY;
	}

	public void setLevel(double level) 
	{
		this.level = level;
	}

	public double getLevel() 
	{
		return level;
	}
}
