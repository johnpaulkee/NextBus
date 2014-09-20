package ca.ubc.cpsc210.nextbus;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.PathOverlay;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import ca.ubc.cpsc210.exception.TranslinkException;
import ca.ubc.cpsc210.nextbus.model.BusLocation;
import ca.ubc.cpsc210.nextbus.model.BusRoute;
import ca.ubc.cpsc210.nextbus.model.BusStop;
import ca.ubc.cpsc210.nextbus.translink.ITranslinkService;
import ca.ubc.cpsc210.nextbus.translink.TranslinkService;
import ca.ubc.cpsc210.nextbus.util.LatLon;
import ca.ubc.cpsc210.nextbus.util.Segment;
import ca.ubc.cpsc210.nextbus.util.TextOverlay;

/**
 * Fragment holding the map in the UI.
 */
public class MapDisplayFragment extends Fragment {

	/**
	 * Log tag for LogCat messages
	 */
	private final static String LOG_TAG = "MapDisplayFragment";
	
	/**
	 * Location of Nelson & Granville, downtown Vancouver
	 */
	private final static GeoPoint NELSON_GRANVILLE 
							= new GeoPoint(49.279285, -123.123007);
	
	/**
	 * Size of border on map view around area that contains buses and bus stop
	 * in dimensions of latitude * 1E6 (or lon * 1E6)
	 */
	private final static int BORDER = 500;

	/**
	 * Overlay for POI markers.
	 */
	private ItemizedIconOverlay<OverlayItem> busLocnOverlay;

	/**
	 * Overlay for bus stop location
	 */
	private ItemizedIconOverlay<OverlayItem> busStopLocationOverlay;

	/**
	 * Overlay for legend
	 */
	private TextOverlay legendOverlay;
	
	/**
	 * Overlays for displaying bus route - one PathOverlay for each segment of route
	 */
	private List<PathOverlay> routeOverlays;
	
	/**
	 * View that shows the map
	 */
	private MapView mapView;

	/**
	 * Selected bus stop
	 */
	private BusStop selectedStop;

	/**
	 * Wraps Translink web service
	 */
	private ITranslinkService tlService;

	/**
	 * Map controller for zooming in/out, centering
	 */
	private IMapController mapController;

	/**
	 * True if and only if map should zoom to fit displayed route.
	 */
	private boolean zoomToFit;

	/**
	 * Overlay item corresponding to bus selected by user
	 */
	private OverlayItem selectedBus;
	

	/**
	 * Set up Translink service and location listener
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG, "onActivityCreated");

		setHasOptionsMenu(true);

		tlService = new TranslinkService(getActivity());
		routeOverlays = new ArrayList<PathOverlay>();

		Log.d(LOG_TAG, "Stop number for mapping: " + (selectedStop == null ? "not set" : selectedStop.getStopNum()));
	}
	
	/**
	 * Set up map view with overlays for buses, selected bus stop, bus route and current location.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreateView");

		if (mapView == null) {
			mapView = new MapView(getActivity(), null);

			mapView.setTileSource(TileSourceFactory.MAPNIK);
			mapView.setClickable(true);
			mapView.setBuiltInZoomControls(true);

			// set default view for map (this seems to be important even when
			// it gets overwritten by plotBuses)
			mapController = mapView.getController();
			mapController.setZoom(mapView.getMaxZoomLevel() - 4);
			mapController.setCenter(NELSON_GRANVILLE);

			busLocnOverlay = createBusLocnOverlay();
			busStopLocationOverlay = createBusStopLocnOverlay();
			legendOverlay = createTextOverlay();

			// Order matters: overlays added later are displayed on top of
			// overlays added earlier.
			mapView.getOverlays().add(busStopLocationOverlay);
			mapView.getOverlays().add(busLocnOverlay);
			mapView.getOverlays().add(legendOverlay);
		}

		return mapView;
	}
	

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_map_refresh, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.map_refresh) {
			update(false);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * When view is destroyed, remove map view from its parent so that it can be
	 * added again when view is re-created.
	 */
	@Override
	public void onDestroyView() {
		Log.d(LOG_TAG, "onDestroyView");

		((ViewGroup) mapView.getParent()).removeView(mapView);

		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "onDestroy");

		super.onDestroy();
	}

	/**
	 * Update map when app resumes.
	 */
	@Override
	public void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume");

		update(true);
	}

	/**
	 * Set selected bus stop
	 * @param selectedStop  the selected stop
	 */
	public void setBusStop(BusStop selectedStop) {
		this.selectedStop = selectedStop;
	}

	/**
	 * Update bus location info for selected stop,
	 * update user location, zoomToFit status and repaint.
	 * 
	 * @Param zoomToFit  true if map must be zoomed to fit (when new bus stop has been selected)
	 */
	void update(boolean zoomToFit) {
		Log.d(LOG_TAG, "update - zoomToFit: " + zoomToFit);
		
		this.zoomToFit = zoomToFit;

		if(selectedStop != null) {
			selectedBus = null;
			new GetBusInfo().execute(selectedStop);
		}

		mapView.invalidate();
	}

	/**
	 * Create the overlay for bus markers.
	 */
	private ItemizedIconOverlay<OverlayItem> createBusLocnOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

		OnItemGestureListener<OverlayItem> gestureListener = new OnItemGestureListener<OverlayItem>() {
			/**
			 * Display bus route and description in dialog box when user taps
			 * bus.
			 * 
			 * @param index  index of item tapped
			 * @param oi the OverlayItem that was tapped
			 * @return true to indicate that tap event has been handled
			 */
			@Override
			public boolean onItemSingleTapUp(int index, OverlayItem oi) {
				// TODO: complete method implementation
				
				AlertDialog dlg = createSimpleDialog(oi.getTitle(), oi.getSnippet());
				if (selectedBus != null){
					if (selectedBus !=  oi){
						selectedBus.setMarker(getResources().getDrawable(R.drawable.bus));
						oi.setMarker(getResources().getDrawable(R.drawable.selected_bus));
						routeOverlays.clear();
					}
					else {
						selectedBus.setMarker(getResources().getDrawable(R.drawable.bus));
						oi.setMarker(getResources().getDrawable(R.drawable.bus));
						OverlayManager om = mapView.getOverlayManager();
						om.clear();
						om.add(busStopLocationOverlay);
						om.add(busLocnOverlay);
						routeOverlays.clear();
						selectedBus = null;
						mapView.postInvalidate();
						return true;
					}
					
				}
				else {
					oi.setMarker(getResources().getDrawable(R.drawable.selected_bus));
					routeOverlays.clear();
				}
				selectedBus = oi;
				for(BusLocation bl : selectedStop.getBusLocations()){
					if(bl.getDescription().equals(oi.getSnippet())){
						retrieveBusRoute(bl);
					}
				}
				mapView.postInvalidate();
				dlg.show();
				return true;
				
			}

			@Override
			public boolean onItemLongPress(int index, OverlayItem oi) {
				// do nothing
				return false;
			}
		};

		return new ItemizedIconOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(), 
				        getResources().getDrawable(R.drawable.map_pin_blue), 
				        gestureListener, rp);
	}

	/**
	 * Create the overlay for bus stop marker.
	 */
	private ItemizedIconOverlay<OverlayItem> createBusStopLocnOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

		OnItemGestureListener<OverlayItem> gestureListener = new OnItemGestureListener<OverlayItem>() {
			/**
			 * Display bus stop description in dialog box when user taps
			 * stop.
			 * 
			 * @param index  index of item tapped
			 * @param oi the OverlayItem that was tapped
			 * @return true to indicate that tap event has been handled
			 */
			@Override
			public boolean onItemSingleTapUp(int index, OverlayItem oi) {
				AlertDialog dlg = createSimpleDialog(oi.getTitle(), oi.getSnippet());
				dlg.show();
				return true;
			}

			@Override
			public boolean onItemLongPress(int index, OverlayItem oi) {
				// do nothing
				return false;
			}
		};

		return new ItemizedIconOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(), 
				        getResources().getDrawable(R.drawable.stop), 
				        gestureListener, rp);
	}

	/**
	 * Create the overlay for disclaimers displayed on top of map. 
	 * @return  text overlay used to display disclaimers
	 */
	private TextOverlay createTextOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());
		Resources res = getResources();
		String legend = res.getString(R.string.legend);
		String osmCredit = res.getString(R.string.osm_credit);
		
		return new TextOverlay(rp, legend, osmCredit);
	}
	
	/**
	 * Create overlay for a single segment of a bus route.
	 * @returns a path overlay that can be used to plot a segment of a bus route
	 */
	private PathOverlay createRouteSegmentOverlay() {
		PathOverlay po = new PathOverlay(Color.RED, getActivity());
		Paint pathPaint = new Paint();
		pathPaint.setColor(Color.RED);
		pathPaint.setStrokeWidth(4.0f);
		pathPaint.setStyle(Style.STROKE);
		po.setPaint(pathPaint);
		return po;
	}


	/**
	 * Plot bus stop
	 */
	private void plotBusStop() {
		LatLon latlon = selectedStop.getLatLon();
		GeoPoint point = new GeoPoint(latlon.getLatitude(),
				latlon.getLongitude());
		OverlayItem overlayItem = new OverlayItem(Integer.valueOf(selectedStop.getStopNum()).toString(), 
				selectedStop.getLocationDesc(), point);
		busStopLocationOverlay.removeAllItems(); // make sure not adding
											     // bus stop more than once
		busStopLocationOverlay.addItem(overlayItem);
	}

	/**
	 * Plot buses onto bus location overlay
	 * 
	 * @param zoomToFit  determines if map should be zoomed to bounds of plotted buses
	 */
	private void plotBuses(boolean zoomToFit) {
		// TODO: modify method so that map zooms to bounds of plotted buses if zoomToFit is true
		
		LatLon stopLatLon = selectedStop.getLatLon();
		double stopLat = stopLatLon.getLatitude();
		double stopLon = stopLatLon.getLongitude();
		
		double minLon = 200.0;
		double minLat = 200.0;
		double maxLon = -200.0;
		double maxLat = -200.0;
		double ctrLon;
		double ctrLat;
		int latSpan;
		int lonSpan;
		

		// clear existing buses from overlay
		busLocnOverlay.removeAllItems();
		
		// remove route overlays as there is now no bus selected
		OverlayManager om = mapView.getOverlayManager();
		om.removeAll(routeOverlays);
		
		List<BusLocation> busLocations = selectedStop.getBusLocations();
		
		if (busLocations.size() > 0) {
			if (zoomToFit) {
			for (BusLocation next : busLocations) {
				LatLon bLLatLon = next.getLatLon();
				if(bLLatLon.getLatitude() < minLat){
					minLat = bLLatLon.getLatitude();
					}
				if(bLLatLon.getLatitude() > maxLat){
					maxLat = bLLatLon.getLatitude();
					}
				if(bLLatLon.getLongitude() < minLon){
					minLon = bLLatLon.getLongitude();
					}
				if (bLLatLon.getLongitude() > maxLon) {
					maxLon = bLLatLon.getLongitude();
					}
				
				plotBus(busLocnOverlay, next);
			}
			if(selectedStop.getLatLon().getLatitude()<minLat){
				minLat = selectedStop.getLatLon().getLatitude();
			}
			if(selectedStop.getLatLon().getLatitude()>maxLat) {
				maxLat = selectedStop.getLatLon().getLatitude();
			}
			if(selectedStop.getLatLon().getLongitude()<minLon) {
				minLon = selectedStop.getLatLon().getLongitude();
			}
			if(selectedStop.getLatLon().getLongitude()>maxLon) {
				maxLon = selectedStop.getLatLon().getLongitude();
			}
			
			latSpan = (int) ((maxLat - minLat)* 1000000);
			lonSpan = (int) ((maxLon - minLon)* 1000000);
			mapController.zoomToSpan(latSpan, lonSpan);
			ctrLat = (minLat + maxLat)/2;
			ctrLon = (minLon + maxLon)/2;
			mapController.setCenter(new GeoPoint(ctrLat, ctrLon));
			}
			else {
				for (BusLocation next: busLocations) {
					plotBus(busLocnOverlay,next);
				}
			}
		}
		else {
			// no buses to plot so centre map on bus stop
			mapController.setCenter(new GeoPoint(stopLat, stopLon));
		}
	}

	/**
	 * Plot a bus on the specified overlay.
	 */
	private void plotBus(ItemizedIconOverlay<OverlayItem> overlay,
			BusLocation bl) {
		LatLon latlon = bl.getLatLon();
		GeoPoint point = new GeoPoint(latlon.getLatitude(), latlon.getLongitude());
		OverlayItem overlayItem = new OverlayItem(bl.getRoute().toString(),
				bl.getDescription(), point);
		
		overlayItem.setMarker(getResources().getDrawable(R.drawable.bus));
		overlayItem.setMarkerHotspot(HotspotPlace.CENTER);
		overlay.addItem(overlayItem);
	}

	
	/**
	 * Get bus route for a particular bus and plot it.
	 * Use cached route, if available.
	 * @param bus  the bus location object associated with the route to be retrieved
	 */
	private void retrieveBusRoute(BusLocation bus) {
		if (bus.getRoute().hasSegments())
			plotBusRoute(bus.getRoute());
		else
			new GetRouteInfo().execute(bus.getRoute());
	}

	/**
	 * Plot bus route onto route overlays.  If rte is null,
	 * no route is plotted (any existing route is cleared).
	 * 
	 * @param rte  the bus route
	 */
	private void plotBusRoute(BusRoute rte) {
		// TODO: provide method implementation
		if (rte == null)
			rte.getSegments().clear();
		else {
			List<Segment> segments = rte.getSegments();
			for (Segment seg : segments) {
				
				PathOverlay po = createRouteSegmentOverlay();
				for (LatLon latLon : seg.getPoints()) {
					int newLat = (int) (latLon.getLatitude() * 1000000);
					int newLon = (int) (latLon.getLongitude() * 1000000);
					po.addPoint(newLat, newLon);
				}
				routeOverlays.add(po);
				
			}
			OverlayManager om = mapView.getOverlayManager();
			om.clear();
			om.addAll(routeOverlays);
			om.add(busStopLocationOverlay);
			om.add(busLocnOverlay);
			mapView.postInvalidate();
		}
	}

	/**
	 * Helper to create simple alert dialog to display message
	 * @param title  the title to be displayed at top of dialog
	 * @param msg  message to display in dialog
	 * @return  the alert dialog
	 */
	private AlertDialog createSimpleDialog(String title, String msg) {
		AlertDialog.Builder dialogBldr = new AlertDialog.Builder(getActivity());
		dialogBldr.setTitle(title);
		dialogBldr.setMessage(msg);
		dialogBldr.setNeutralButton(R.string.ok, null);

		return dialogBldr.create();
	}

	/** 
	 * Asynchronous task to get bus location estimates from Translink service.
	 * Displays progress dialog while running in background.  
	 */
	private class GetBusInfo extends
			AsyncTask<BusStop, Void, Void> {
		private ProgressDialog dialog = new ProgressDialog(getActivity());
		private boolean success = true;
		private String errorMsg;

		@Override
		protected void onPreExecute() {
			dialog.setMessage("Retrieving bus info...");
			dialog.show();
		}

		@Override
		protected Void doInBackground(BusStop... selectedStops) {
			BusStop selectedStop = selectedStops[0];

			try {
				tlService.addBusLocationsForStop(selectedStop);
			} catch (TranslinkException e) {
				e.printStackTrace();
				errorMsg = e.getMessage();
				errorMsg += "\n(code: " + e.getCode() + ")";
				success = false;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void dummy) {
			dialog.dismiss();

			if (success) {
				plotBuses(zoomToFit);
				plotBusStop();
				mapView.invalidate();
			} else {
				String msg = "Unable to get information for stop #: " + selectedStop;
				msg += "\n\n" + errorMsg;
				
				AlertDialog dialog = createSimpleDialog("Error", msg);
				dialog.show();
			}
		}
	}

	/** 
	 * Asynchronous task to get bus route from Translink service.
	 * Adds bus route to cache so we don't have to go get it again if user
	 * decides to display this route again. 
	 * Displays progress dialog while running in background.  
	 */
	private class GetRouteInfo extends AsyncTask<BusRoute, Void, Void> {
		private BusRoute route;
		private boolean success = true;
		
		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(BusRoute... routes) {
			route = routes[0];

			try {
				tlService.parseKMZ(route);
			} catch (TranslinkException e) {
				e.printStackTrace();
				success = false;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void dummy) {
			if (success) {
				plotBusRoute(route);
			} else {
				AlertDialog dialog = createSimpleDialog("Error", "Unable to retrieve route...");
				dialog.show();
			}
		}
	}
}
