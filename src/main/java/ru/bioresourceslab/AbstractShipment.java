package ru.bioresourceslab;

import org.intellij.lang.annotations.MagicConstant;

import javax.swing.event.EventListenerList;

import static ru.bioresourceslab.ShipmentEvent.*;

public abstract class AbstractShipment {
    /** list of listeners */
    protected EventListenerList listenerList = new EventListenerList();

    /** add a listener */
    public void addListener(ShipmentListener listener) {
        listenerList.add(ShipmentListener.class, listener);
    };

    /** remove a listener */
    public void removeListener(ShipmentListener listener) {
        listenerList.remove(ShipmentListener.class, listener);
    };


    /** Universal method for calling event handlers
     * @param source - an object, which called the event
     * @param typeOfEvent - type of event, defines the event handler
     * @param target - index of sample, which is modified (RECOMMEND: -1 if there are no samples left) */
    protected void fireEvent(Shipment source, @MagicConstant(intValues =
            {EVENT_SAMPLE_ADDED, EVENT_SAMPLE_REMOVED, EVENT_SAMPLE_MOVED, EVENT_SAMPLE_CHANGED}) int typeOfEvent, int target) {
        Object[] listeners = listenerList.getListenerList();
        ShipmentEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ShipmentListener.class) {
                if (e == null) {
                    e = new ShipmentEvent(source, typeOfEvent, target);
                }
                ((ShipmentListener)listeners[i+1]).defaultAction(e);
                switch (typeOfEvent) {
                    case EVENT_SAMPLE_ADDED: {
                        ((ShipmentListener)listeners[i+1]).dataAdded(e);
                        continue;
                    }
                    case EVENT_SAMPLE_REMOVED: {
                        ((ShipmentListener)listeners[i+1]).dataRemoved(e);
                        continue;
                    }
                    case EVENT_SAMPLE_MOVED: {
                        ((ShipmentListener)listeners[i+1]).dataMoved(e);
                        continue;
                    }
                    case EVENT_SAMPLE_CHANGED: {
                        ((ShipmentListener)listeners[i+1]).dataChanged(e);
//                        continue;
                    }
                } // end switch
            }
        } // end for
    }
}
