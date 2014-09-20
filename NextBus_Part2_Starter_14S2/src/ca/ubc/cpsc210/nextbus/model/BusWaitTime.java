package ca.ubc.cpsc210.nextbus.model;


/**
 * Wait time estimate at a particular stop for a particular bus serving a route
 */
public class BusWaitTime implements Comparable<BusWaitTime> {
	private BusRoute route;
	private int waitTime;
	private boolean isCancelled;

	/**
	 * Constructor 
	 * 
	 * @param route     the route that bus is servicing
	 * @param estimate  estimated wait time in minutes
	 * @param cancelledStatus  true if bus is cancelled, false otherwise
	 */
	public BusWaitTime(BusRoute route, int estimate, boolean cancelledStatus) {
		this.route = route;
		this.isCancelled = cancelledStatus;
		waitTime = estimate;
	}

	/**
	 * Gets bus route that bus with this wait time is servicing
	 * @return  bus route
	 */
	public BusRoute getRoute() {
		return route;
	}

	/**
	 * Gets wait time estimate in minutes
	 * @return  wait time
	 */
	public int getEstimate() {
		return waitTime;
	}
	
	/**
	 * Determine if bus is cancelled
	 * 
	 * @return  true if cancelled, false otherwise
	 */
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Get string representation of wait time in format:
	 * 
	 * <route name>: <wait time> mins
	 * or
	 * <route name>: <wait time> mins - cancelled
	 * 
	 * where <wait time> is printed as "NOW" if wait time is under 2 minutes
	 * 
	 * For example:
	 * 099: NOW
	 * 004: 17 mins
	 * 014: 5 mins - cancelled
	 * 
	 */
	@Override
	public String toString() {
		return route.getName() + ": " + (waitTime < 2 ? "NOW" : waitTime + " mins") + (isCancelled ? " - cancelled" : "");
	}
	
	/**
	 * Compare this wait time to another.  Buses with the same wait times and cancelled status 
	 * are ordered by route.  If wait times are the same but cancelled status is different,
	 * cancelled services are ordered before those that are not cancelled.  Otherwise,  
	 * ordered by increasing wait time.
	 */
    @Override
    public int compareTo(BusWaitTime other) {
        if(waitTime == other.waitTime && isCancelled == other.isCancelled)
            return this.route.compareTo(other.route);
        else if (waitTime == other.waitTime)
        	return (isCancelled ? -1 : 1);
        else
            return waitTime - other.waitTime;
    }
    
    /**
     * Generated by Eclipse
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isCancelled ? 1231 : 1237);
        result = prime * result + ((route == null) ? 0 : route.hashCode());
        result = prime * result + waitTime;
        return result;
    }

    /**
     * Generated by Eclipse
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BusWaitTime other = (BusWaitTime) obj;
        if (isCancelled != other.isCancelled)
            return false;
        if (route == null) {
            if (other.route != null)
                return false;
        }
        else if (!route.equals(other.route))
            return false;
        if (waitTime != other.waitTime)
            return false;
        return true;
    }
}
