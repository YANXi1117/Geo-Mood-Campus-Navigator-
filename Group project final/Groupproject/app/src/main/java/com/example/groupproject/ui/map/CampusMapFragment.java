package com.example.groupproject.ui.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.groupproject.R;
import com.example.groupproject.model.Building;
import com.example.groupproject.model.CommentStore;
import com.example.groupproject.receiver.GeofenceBroadcastReceiver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CampusMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private GeofencingClient geofencingClient;
    private FusedLocationProviderClient fusedLocationClient;
    private PendingIntent geofencePendingIntent;
    private final Map<String, Circle> circleMap = new HashMap<>();
    private final Map<String, Building> buildingMap = new HashMap<>();
    private Polyline currentPolyline;

    public CampusMapFragment() {
        super(R.layout.fragment_campus_map);
    }

    private final List<Building> buildings = Arrays.asList(
            new Building("UG",  "U Garden",         22.302780,  114.179455,  30f, "U garden Canteen",       "🍕"),
            new Building("WK",  "Communal Canteen", 22.3049080, 114.1800452, 30f, "Communal Canteen",        "🍞"),
            new Building("VA",  "VA Canteen",        22.3044089, 114.1791721, 30f, "VA dining area",          "🍜"),
            new Building("Z",   "Block Z",           22.306436,  114.179501,  30f, "Block Z",                 "📚"),
            new Building("L",   "Lawn Restaurant",   22.304367,  114.179469,  30f, "Lawn Restaurant",         "🍝"),
            new Building("H",   "H Cafe",            22.303808,  114.179337,  30f, "H Cafe",                  "☕️"),
            new Building("JC",  "Auditorium",        22.304674,  114.180653,  30f, "Jockey Club Auditorium",  "⛪️"),
            new Building("lib", "Library",           22.303575,  114.179693,  30f, "Pao Yue-Kong Library",    "📚")
    );

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (Boolean.TRUE.equals(fine)) {
                    enableMyLocation();
                    registerGeofences();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        requestBackgroundLocation();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Location permission is required to show your position and unlock zones",
                            Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> bgPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(requireContext(),
                            "Background location denied, geofences cannot be triggered",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        geofencingClient    = LocationServices.getGeofencingClient(requireActivity());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        for (Building b : buildings) buildingMap.put(b.getId(), b);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
            registerGeofences();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (googleMap == null) return;
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        LatLng campus = new LatLng(22.30333, 114.17972);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campus, 17f));

        for (Building building : buildings) {
            LatLng point = new LatLng(building.getLat(), building.getLng());

            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(point)
                    .icon(emojiToBitmap(building.getMoodEmoji()))
                    .title(building.getName())
                    .snippet(building.getMoodEmoji() + "  " + building.getIntro()));
            if (marker != null) marker.setTag(building.getId());

            Circle circle = googleMap.addCircle(new CircleOptions()
                    .center(point)
                    .radius(building.getRadius())
                    .strokeWidth(4f)
                    .strokeColor(Color.parseColor("#2979FF"))
                    .fillColor(0x00000000));
            circleMap.put(building.getId(), circle);
        }

        // Dialog with three options
        googleMap.setOnInfoWindowClickListener(marker -> {
            String buildingId = (String) marker.getTag();
            Building building = buildingMap.get(buildingId);
            if (building == null) return;

            new AlertDialog.Builder(requireContext())
                    .setTitle(marker.getTitle())
                    .setMessage("Choose an action")
                    // Positive button: navigation
                    .setPositiveButton("🗺 Navigate", (d, w) ->
                            showNavigationModeDialog(building))
                    // Negative button: comments
                    .setNegativeButton("💬 View Comments", (d, w) -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("buildingId",    buildingId);
                        bundle.putString("buildingName",  marker.getTitle());
                        bundle.putString("buildingIntro", marker.getSnippet());
                        String snippet = marker.getSnippet() != null ? marker.getSnippet() : "";
                        bundle.putString("buildingEmoji", snippet.substring(0, Math.min(2, snippet.length())));
                        Navigation.findNavController(requireView())
                                .navigate(R.id.action_map_to_detail, bundle);
                    })
                    // Added neutral button: street view
                    .setNeutralButton("🌆 Street View", (d, w) ->
                            launchStreetView(building.getLat(), building.getLng()))
                    .show();
        });

        refreshMoodColors();
        enableMyLocation();
    }

    // Added: launch Google Maps Street View mode
    private void launchStreetView(double lat, double lng) {
        // google.streetview:cbll=lat,lng is the official Street View URI
        Uri uri = Uri.parse("google.streetview:cbll=" + lat + "," + lng);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Fallback: if Google Maps is not installed, open Street View in browser
            Uri webUri = Uri.parse(
                    "https://www.google.com/maps/@?api=1&map_action=pano&viewpoint="
                            + lat + "," + lng);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    // emoji → BitmapDescriptor
    private BitmapDescriptor emojiToBitmap(String emoji) {
        int size = 120;
        float textSize = 72f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        paint.getTextBounds(emoji, 0, emoji.length(), bounds);

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xCCFFFFFF);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint);

        float x = size / 2f;
        float y = size / 2f - (bounds.top + bounds.bottom) / 2f;
        canvas.drawText(emoji, x, y, paint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // ── Navigation ──────────────────────────────────────────────

    private void showNavigationModeDialog(Building building) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Navigation Mode")
                .setMessage("Navigate to " + building.getName())
                .setPositiveButton("📍 In-App Navigation", (d, w) ->
                        startInAppNavigation(building))
                .setNegativeButton("🌐 Google Maps", (d, w) ->
                        launchGoogleMapsNavigation(building.getLat(), building.getLng(), building.getName()))
                .show();
    }

    @SuppressLint("MissingPermission")
    private void startInAppNavigation(Building destination) {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(requireContext(),
                        "Unable to get current location, please ensure location is enabled",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            fetchAndDrawRoute(
                    location.getLatitude(), location.getLongitude(),
                    destination.getLat(),   destination.getLng()
            );
        });
    }

    private void fetchAndDrawRoute(double fromLat, double fromLng, double toLat, double toLng) {
        String apiKey = getString(R.string.google_maps_key);
        String requestUrl = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin="      + fromLat + "," + fromLng
                + "&destination=" + toLat   + "," + toLng
                + "&mode=walking"
                + "&key="         + apiKey;

        Toast.makeText(requireContext(), "Planning route...", Toast.LENGTH_SHORT).show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json   = new JSONObject(sb.toString());
                JSONArray  routes = json.getJSONArray("routes");

                if (routes.length() == 0) {
                    handler.post(() -> Toast.makeText(requireContext(),
                            "No walking route found", Toast.LENGTH_SHORT).show());
                    return;
                }

                String encodedPolyline = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points");
                List<LatLng> path = decodePolyline(encodedPolyline);

                handler.post(() -> {
                    if (currentPolyline != null) currentPolyline.remove();
                    currentPolyline = googleMap.addPolyline(new PolylineOptions()
                            .addAll(path)
                            .width(14f)
                            .color(Color.parseColor("#2979FF"))
                            .geodesic(true));
                    Toast.makeText(requireContext(), "Walking route displayed ✅", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                handler.post(() -> Toast.makeText(requireContext(),
                        "Failed to fetch route, please check your network or ensure Directions API is enabled\n"
                                + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        });
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lat += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            shift = 0; result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lng += ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            poly.add(new LatLng(lat / 1E5, lng / 1E5));
        }
        return poly;
    }

    private void launchGoogleMapsNavigation(double lat, double lng, String label) {
        Uri uri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=w");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + lat + "," + lng + "&travelmode=walking");
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    // ── Color refresh ───────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        refreshMoodColors();
    }

    private void refreshMoodColors() {
        if (googleMap == null) return;
        for (Building building : buildings) {
            Circle circle = circleMap.get(building.getId());
            if (circle == null) continue;
            int fillColor = CommentStore.getInstance().getMoodColor(building.getId());
            circle.setFillColor(fillColor);
            if      (fillColor == 0x4400CC44) circle.setStrokeColor(Color.parseColor("#00CC44"));
            else if (fillColor == 0x44CC2200) circle.setStrokeColor(Color.parseColor("#CC2200"));
            else if (fillColor == 0x44FFCC00) circle.setStrokeColor(Color.parseColor("#FFCC00"));
            else                              circle.setStrokeColor(Color.parseColor("#2979FF"));
        }
    }

    // ── Geofence ────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private void registerGeofences() {
        List<Geofence> list = new ArrayList<>();
        for (Building b : buildings) {
            list.add(new Geofence.Builder()
                    .setRequestId(b.getId())
                    .setCircularRegion(b.getLat(), b.getLng(), b.getRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER |
                                    Geofence.GEOFENCE_TRANSITION_DWELL)
                    .setLoiteringDelay(3000)
                    .build());
        }
        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(list)
                .build();
        geofencingClient.addGeofences(request, getGeofencePendingIntent())
                .addOnSuccessListener(u ->
                        Toast.makeText(requireContext(),
                                "Geofences registered successfully ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Geofence registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) return geofencePendingIntent;
        Intent intent = new Intent(requireContext(), GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
        geofencePendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, flags);
        return geofencePendingIntent;
    }

    private void requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            bgPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }
}