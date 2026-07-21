package com.example.groupproject.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.groupproject.model.CommentStore;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);

        if (event == null || event.hasError()) {
            Log.e(TAG, "Geofencing event error");
            return;
        }

        int transition = event.getGeofenceTransition();

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                transition == Geofence.GEOFENCE_TRANSITION_DWELL) {

            List<Geofence> triggered = event.getTriggeringGeofences();
            if (triggered == null) return;

            for (Geofence geofence : triggered) {
                String buildingId = geofence.getRequestId();
                CommentStore.getInstance().unlockBuilding(buildingId);
                Log.i(TAG, "Unlocked: " + buildingId);
            }
        }
    }
}
