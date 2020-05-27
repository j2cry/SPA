package ru.bioresourceslab;

import java.util.EventObject;

public class ShipmentEvent extends EventObject {

    /** Identifiers of shipment class event types */
    public static final int EVENT_SAMPLE_ADDED = 0;
    public static final int EVENT_SAMPLE_REMOVED = 1;
    public static final int EVENT_SAMPLE_MOVED = 2;
    public static final int EVENT_SAMPLE_CHANGED = 3;

//    private final int typeOfEvent;
    /** Index of modified sample. It is '-1' if no samples left */
    private final int target;

    public ShipmentEvent(Object source, int target) {
        super(source);
//        this.typeOfEvent = typeOfEvent;
        this.target = target;
    }

//    public int getType() {
//        return typeOfEvent;
//    }

    public int getTarget() {
        return target;
    }
}
