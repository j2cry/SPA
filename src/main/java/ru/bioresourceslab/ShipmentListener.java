package ru.bioresourceslab;

import java.util.EventListener;

public class ShipmentListener implements EventListener {

    /** This method is called always when an event happens, in addition to special method based on event type
     * Thus, on event there are called two methods: this and on-type-based.
     * !!! NOTICE !!!: This method is called first! */
    public void defaultAction(ShipmentEvent source) {};

    public void dataAdded(ShipmentEvent source) {};

    public void dataMoved(ShipmentEvent source) {};

    public void dataRemoved(ShipmentEvent source) {};

    public void dataChanged(ShipmentEvent source) {};

}
