package com.google.watermap.p.gary.fogliftver6;

import android.*;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleMap.OnMarkerDragListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnPoiClickListener, PlaceSelectionListener {


    private GoogleMap mMap;

    //Share
    private static final String TAG = MainActivity.class.getSimpleName();
    private final static String KEY_LOCATION = "location";
    private LatLng mCurrentLocation;
    private FusedLocationProviderClient mFusedLocationClient;


    private float mCameraDefaultZoom = 15;

    //Current Position
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // UI
    private final static String KEY_CAMERA_LOCATION = "camera_location";
    private final static String KEY_CAMERA_ZOOM = "camera_zoom";

    private LatLng mCameraLocation;
    private float mCameraZoom;

    // Label


    private final LatLng mDefaultLocation = new LatLng(35.652832, 139.839478);
    private final LatLng tsukuba = new LatLng(36.082736, 140.111592);

    //Preference
    private SharedPreferences preferences;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;
    private Boolean serviceAvailble;
    private MenuItem serviceSwitch;

    //Firebase
    private FragmentActivity fragmentActivity = this;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseReference;
    private List<DatabasePlace> dbPlaceList = new ArrayList<>();
    private boolean onDataChange = false;
    private LongSparseArray<Marker> markerHashArray = new LongSparseArray<>();

    Intent intent;
    private double earth_dis = 6378137;
    private GoogleApiClient mGoogleApiClient;


    private Place selectedPlace;
    private boolean selected_place;
    private Marker selectedPlaceMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton drawerButton = (ImageButton) findViewById(R.id.drawer_button);
        drawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        preferences = getSharedPreferences("DATA", Context.MODE_PRIVATE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("location_service_switch")) {
                    serviceAvailble = sharedPreferences.getBoolean(key, false);
                    Log.i("onSharedPreference", String.valueOf(sharedPreferences.getBoolean(key, false)));
                    if (serviceAvailble) {
                        startService(new Intent(getBaseContext(), CurrentLocationService.class));
                    } else {
                        stopService(new Intent(getBaseContext(), CurrentLocationService.class));
                    }
                    preferences.edit().putBoolean("SERVICE", serviceAvailble).apply();

                } else {
                    Log.i("onSharedPreference", "else");
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();

        mGoogleApiClient.connect();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(this);


        serviceAvailble = sharedPreferences.getBoolean("location_service_switch", false);

        updateValuesFromSharedPreferences(preferences);
        updateValuesFromBundle(savedInstanceState);


        mDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mDatabase.getReference("Places");


        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(TAG, "onDataChange");
                onDataChange = true;
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    putPlaceList(data, dbPlaceList);
                }
                if (mMap != null) {
                    addMakerAll();
                    updateUI();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i(TAG, "Database Error");
            }
        });

        intent = getIntent();

        selected_place = false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.foglift_map) {
            // Handle the camera action
        } else if (id == R.id.world_map) {
            startActivity(new Intent(getApplication(), WorldMapsActivity.class));
        } else if (id == R.id.nav_preference) {
            startActivity(new Intent(getApplication(), SettingsActivity.class));
        } else if (id == R.id.nav_help) {

        } else if (id == R.id.nav_faq) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setPadding(0, 200, 0, 0);
        Log.i(TAG, "onMapReady");
        mMap = googleMap;

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);

        mMap.setOnPoiClickListener(this);

        mMap.setInfoWindowAdapter(new CustomWindowViewer(fragmentActivity));
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.i(TAG, "onMarkerClick");
                if(!marker.equals(selectedPlaceMarker)) {
                    CameraPosition cameraPos = mMap.getCameraPosition();
                    VisibleRegion screenRegion = mMap.getProjection().getVisibleRegion();
                    LatLng topRight = screenRegion.latLngBounds.northeast;
                    LatLng bottomLeft = screenRegion.latLngBounds.southwest;
                    double screenDistance = SphericalUtil.computeDistanceBetween(topRight, bottomLeft) * sin(40) * 25;
                    double theta = cameraPos.tilt;
                    double distance = screenDistance / earth_dis;
                    double moveLat = distance * cos(theta);
                    double moveLng = distance * sin(theta);
                    marker.showInfoWindow();
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .tilt(cameraPos.tilt)
                            .zoom(cameraPos.zoom)
                            .target(new LatLng(marker.getPosition().latitude + moveLat, marker.getPosition().longitude + moveLng))      // Sets the center of the map to Mountain View
                            .build();                   // Creates a CameraPosition from the builder

                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    //mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(marker.getPosition().latitude + moveLat, marker.getPosition().longitude + moveLng)));

                }
                return true;
            }
        });

        getDeviceLocation();

        if (checkPermissions()) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onPlaceSelected(Place place) {
        Log.i(TAG, "onPlaceSelected");
        selected_place = true;
        selectedPlace = place;
        Toast.makeText(getApplicationContext(), "Clicked: " + "Clicked: " +
                        place.getName() + "\nPlace ID:" + place.getId() +
                        "\nLatitude:" + place.getLatLng().latitude +
                        " Longitude:" + place.getLatLng().longitude,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(Status status) {

    }

    @Override
    public void onPoiClick(PointOfInterest pointOfInterest) {
        if (selectedPlaceMarker != null) {
            selectedPlaceMarker.remove();
        }
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(selectedPlace.getLatLng())      // Sets the center of the map to Mountain View
                    .zoom(17)                   // Sets the zoom
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            selectedPlaceMarker = mMap.addMarker(new MarkerOptions().position(selectedPlace.getLatLng())
                    .icon(BitmapDescriptorFactory.defaultMarker()));
        CameraPosition cameraPos = new CameraPosition.Builder()
                .target(pointOfInterest.latLng)      // Sets the center of the map to Mountain View
                .zoom(17)                   // Sets the zoom
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos));
        Toast.makeText(getApplicationContext(), "Clicked: " +
                        pointOfInterest.name + "\nPlace ID:" + pointOfInterest.placeId +
                        "\nLatitude:" + pointOfInterest.latLng.latitude +
                        " Longitude:" + pointOfInterest.latLng.longitude,
                Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        if (checkPermissions()) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    mCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    updateUI();
                }
            });
        }
    }

    public void addMakerAll() {
        Log.i(TAG, "addMarkerAll");
        for (DatabasePlace dbPlace : dbPlaceList) {
            if (dbPlace.getKind().equals("狂犬病")) {
                Resources r = getResources();
                Bitmap bmp = BitmapFactory.decodeResource(r, R.drawable.dogmarker);
                markerHashArray.put(dbPlace.getId(), mMap.addMarker(new MarkerOptions().position(dbPlace.getLocation()).title(dbPlace.getName())
                        .icon(BitmapDescriptorFactory.fromBitmap(bmp))));
                markerHashArray.get(dbPlace.getId()).setTag(dbPlace);
            } else {
                markerHashArray.put(dbPlace.getId(), mMap.addMarker(new MarkerOptions().position(dbPlace.getLocation()).title(dbPlace.getName())
                        .icon(BitmapDescriptorFactory.defaultMarker(dbPlace.getMakerColor()))));
                markerHashArray.get(dbPlace.getId()).setTag(dbPlace);
            }
        }
    }

    private void putPlaceList(DataSnapshot dataSnapshot, List<DatabasePlace> dbPlaceList) {
        Log.i(TAG, "putPlaceList");
        String key = dataSnapshot.getKey();
        Object kind = dataSnapshot.child("Kind").getValue();
        Object level = dataSnapshot.child("Level").getValue();
        Object latitude = dataSnapshot.child("Location").child("Latitude").getValue();
        Object longitude = dataSnapshot.child("Location").child("Longitude").getValue();
        Object uri = dataSnapshot.child("ImageURI").getValue();
        Object id = dataSnapshot.child("ID").getValue();
        Object information = dataSnapshot.child("Information").getValue();
        Log.i("putPlaceList", key + ":[" + kind + ":" + level + ":" + latitude + ":" + longitude + ":" + id + "]");
        if (latitude != null && longitude != null) {
            DatabasePlace dbPlace = new DatabasePlace(key, (String) kind, (long) level, (Double) latitude, (Double) longitude, (long) id, (String) uri, (String) information);
            dbPlaceList.add(dbPlace);
        }
    }

    private void putMarker(String name, Marker marker) {

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed");
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
    }

    /**
     * 以前の情報の復元
     *
     * @param savedInstanceState
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }
            if (savedInstanceState.keySet().contains(KEY_CAMERA_ZOOM)) {
                mCameraZoom = savedInstanceState.getFloat(KEY_CAMERA_ZOOM);
            }
            if (savedInstanceState.keySet().contains(KEY_CAMERA_LOCATION)) {
                mCameraLocation = savedInstanceState.getParcelable(KEY_CAMERA_LOCATION);
            }
            //UIの更新
            updateUI();
        }
    }

    private void updateValuesFromSharedPreferences(SharedPreferences data) {
        serviceAvailble = data.getBoolean("SERVICE", false);

    }


    /**
     * 現在の状態の保存
     *
     * @param savedInstanceState
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putFloat(KEY_CAMERA_ZOOM, mCameraZoom);
        savedInstanceState.putParcelable(KEY_CAMERA_LOCATION, mCameraLocation);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * UI更新
     */
    private void updateUI() {
        intent = getIntent();
        Log.i(TAG, "updateUI");
        long dangerPlaceId = intent.getLongExtra("DANGER_MARKER_ID", 0);
        boolean fromNotificationCheck = intent.getBooleanExtra("FROM_NOTIFICATION", false);
        if (mMap != null) {
            if (fromNotificationCheck) {
                Log.i("updateUI", "fromNotification");
                if (onDataChange) {
                    Log.i("updateUI", "dangerPlaceId:" + dangerPlaceId);
                    Marker dangerMarker = markerHashArray.get(dangerPlaceId);
                    dangerMarker.showInfoWindow();
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(dangerMarker.getPosition().latitude + 0.007, dangerMarker.getPosition().longitude)));
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mCameraDefaultZoom));
                }
            } else if (selected_place) {
                Log.i("updateUI", "selected_place");
                if (selectedPlaceMarker != null) {
                    selectedPlaceMarker.remove();
                }
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(selectedPlace.getLatLng())      // Sets the center of the map to Mountain View
                        .zoom(17)                   // Sets the zoom
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                selectedPlaceMarker = mMap.addMarker(new MarkerOptions().position(selectedPlace.getLatLng())
                        .icon(BitmapDescriptorFactory.defaultMarker()));
                selected_place = false;
            } else {
                if (mCameraLocation != null) {
                    Log.i("updateUI", "mCameraLocation");
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCameraLocation, mCameraZoom));
                } else if (mCurrentLocation != null) {
                    Log.i("updateUI", "mCurrentLocation");
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(mCurrentLocation));
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(mCameraDefaultZoom));
                } else {
                    Log.i("updateUI", "else");
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tsukuba, mCameraDefaultZoom));
                }
            }

        }

    }


    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (!checkPermissions()) {
            requestPermissions();
        }
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (mMap != null) {
            CameraPosition mCameraPosition = mMap.getCameraPosition();
            mCameraLocation = mCameraPosition.target;
            Log.i(TAG, mCameraLocation.latitude + ":" + mCameraLocation.longitude);
            mCameraZoom = mCameraPosition.zoom;
            Log.i(TAG, mCameraZoom + "");
        }
    }


    /**
     * Snackbar表示
     *
     * @param mainTextStringId
     * @param actionStringId
     * @param listener
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * 権限確認
     *
     * @return
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 権限リクエスト
     */
    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * 権限リクエスト後のコールバック
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "Permission granted, updates requested, starting location updates");
                if (checkPermissions()) {
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(true);
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                    mMap.getUiSettings().setCompassEnabled(true);
                }
            } else {
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.action_settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }


}
