package com.poly

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Address
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import android.widget.Toast
import android.location.Geocoder
import android.os.AsyncTask
import android.view.View
import android.widget.EditText
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.model.*
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import kotlinx.android.synthetic.main.activity_maps.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var instance: GetLocation
    private var lat = 0.0
    private var lng = 0.0
    private var address = ""
    var ischeck=false
    var is_check=0

    private lateinit var latLng_from:LatLng
    private lateinit var  latLng_to:LatLng

    private var markerList: ArrayList<Marker>? = null

    /* recevie Broadcast lattude and longtude*/
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent != null && !ischeck) {
                lat = intent.getDoubleExtra("lat", 0.0)
                lng = intent.getDoubleExtra("lng", 0.0)
                address = intent.getStringExtra("add")
                val sydney = LatLng(lat, lng)
                MapMaker(address,sydney,R.drawable.pin_location_pin)
            }
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        initBroadCastMap()
        instance.startLocation()



    }

    private fun initBroadCastMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        instance = GetLocation(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("key_action"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            GetLocation.REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> Log.e("TAG", "User agreed to make required location settings changes.")
                Activity.RESULT_CANCELED -> {
                    Log.e("TAG", "User chose not to make required location settings changes.")
                    instance.mRequestingLocationUpdates = false
                }
            }// Nothing to do. startLocationupdates() gets called in onResume again.

            GetLocation.PLACE_AUTOCOMPLETE_REQUEST_CODE -> when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = PlaceAutocomplete.getPlace(this, data)
                    val addre = "" + place.address


                    if (is_check == 1) {
                        latLng_from= place.latLng
                        tv_pickup_location.text = addre
                        MapMaker(addre, latLng_from, R.drawable.pin_location_pin)
                    }
                     if(is_check==2) {
                         latLng_to= place.latLng
                         tv_drop_location.text = addre
                         MapMaker(addre, latLng_to, R.drawable.drop_location_pin)

                     }
                }
                Activity.RESULT_CANCELED -> {
                    //else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {}
                    //else if (resultCode == RESULT_CANCELED) {}
                }


            }
        }
    }


   fun ok(v:View){
       showLocationOnmap()
   }


    public override fun onResume() {
        super.onResume()
        if (instance.mRequestingLocationUpdates!! && instance.checkPermissions()) {
            instance.startLocationUpdates()
        }
        instance.updateLocationUI()
    }

    override fun onPause() {
        super.onPause()
        if (instance.mRequestingLocationUpdates!!) {
            instance.stopLocationUpdates()
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.mMap = googleMap;
        val style = MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style);
        this.mMap.setMapStyle(style);
        mMap.uiSettings.isZoomControlsEnabled = false;
        mMap.uiSettings.isMyLocationButtonEnabled = true;
    }




    private fun MapMaker(address: String,latLng:LatLng,customMarker:Int) {
        mMap.clear()
        mMap.addMarker(MarkerOptions()
                .position(latLng)
                .title("Address")
                .snippet(address)
                .icon(BitmapDescriptorFactory
                        .fromResource(customMarker)))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10f), 2000, null)

    }


    /* Buttton Click view*/
     fun openAutoComplePickerPickup(view:View) {
         is_check=1
         openPikarSearch()
     }

     fun openAutoComplePickerDrop(view:View) {
         is_check=2
         openPikarSearch()
     }

    private fun openPikarSearch(){
        ischeck=true
        try {
            val typeFilter = AutocompleteFilter.Builder()
                    .setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE)
                    .setCountry("IN")
                    .build()
            val intent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .setFilter(typeFilter)
                    .setBoundsBias(LatLngBounds(LatLng(23.63936, 68.14712), LatLng(28.20453, 97.34466)))
                    .build(this)
            startActivityForResult(intent, GetLocation.PLACE_AUTOCOMPLETE_REQUEST_CODE)
        } catch (ignored: GooglePlayServicesRepairableException) {
        } catch (ignored: GooglePlayServicesNotAvailableException) {
        }
    }



    /*Poly Line Googe Map...*/

    private fun showLocationOnmap() {
        clearMap()
        markerList = ArrayList()
        addMarkere(latLng_from, "", R.drawable.pin_location_pin)
        addMarkere(latLng_to, "", R.drawable.drop_location_pin)
        prepareRouteUrl(latLng_from,latLng_to)
    }

    private fun addMarkere(sydney: LatLng, title: String, marker: Int) {
        val markerOptions = MarkerOptions()
                .position(sydney)
                .icon(BitmapDescriptorFactory.fromResource(marker))
                .anchor(0.5f, 0.5f)
        //.title(title);
        val marker1 = mMap.addMarker(markerOptions)
        markerList!!.add(marker1)
    }

    private fun clearMap() {
        mMap.clear()
    }

    private fun showAllMarkers(v: Marker, parseDouble: Marker) {
        val builder = LatLngBounds.Builder()
        builder.include(LatLng(v.position.latitude, v.position.longitude))
        builder.include(LatLng(parseDouble.position.latitude, parseDouble.position.longitude))
        val bounds = builder.build()
        mMap.setPadding(10, 110, 10, 110)
        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(v.position.latitude, v.position.longitude)).zoom(11f).tilt(40f).bearing(50f).build()
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        val cu = CameraUpdateFactory.newLatLngBounds(bounds, 10)
        mMap.moveCamera(cu)
    }

    private fun prepareRouteUrl(pikup:LatLng, drop:LatLng) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?"
        val urll = url + "origin=" + pikup.latitude + "," + pikup.longitude + "&destination=" + drop.latitude + "," + drop.longitude + "&mode=driving&key="+this.resources.getString(R.string.google_maps_key)
        callAPIForDrawRoute(urll)
    }

    private fun callAPIForDrawRoute(url: String) {
        Ion.with(this)
                .load(url)
                .asJsonObject()
                .withResponse()
                .setCallback(FutureCallback { e, result ->
                    if (e != null) {
                        Toast.makeText(applicationContext, "txt_Netork_error", Toast.LENGTH_SHORT).show()
                        return@FutureCallback
                    }
                    val status = result.headers.code()
                    val resultObject = result.result
                    when (status) {
                        200 -> ParserTask().execute(resultObject.toString())
                    }
                })
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ParserTask : AsyncTask<String, Int, List<List<HashMap<String, String>>>>() {
        override fun doInBackground(vararg jsonData: String): List<List<HashMap<String, String>>>? {
            val jObject: JSONObject
            var routes: List<List<HashMap<String, String>>>? = null
            try {
                jObject = JSONObject(jsonData[0])
                Log.d("ParserTask", jsonData[0])
                val parser = DataParser()
                Log.d("ParserTask", parser.toString())

                // Starts parsing data
                routes = parser.parse(jObject)
                Log.d("ParserTask", "Executing routes")
                Log.d("ParserTask", routes.toString())

            } catch (e: Exception) {
                Log.d("ParserTask", e.toString())
                e.printStackTrace()
            }

            return routes
        }

        // Executes in UI thread, after the parsing process
        override fun onPostExecute(result: List<List<HashMap<String, String>>>) {
            var points: ArrayList<LatLng>
            var lineOptions: PolylineOptions? = null

            // Traversing through all the routes
            for (i in result.indices) {
                points = ArrayList()
                lineOptions = PolylineOptions()

                // Fetching i-th route
                val path = result[i]

                // Fetching all the points in i-th route
                for (j in path.indices) {
                    val point = path[j]

                    val lat = java.lang.Double.parseDouble(point["lat"])
                    val lng = java.lang.Double.parseDouble(point["lng"])
                    val position = LatLng(lat, lng)

                    points.add(position)
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points)
                lineOptions.color(application.resources.getColor(R.color.colorAccent))

                Log.d("onPostExecute", "onPostExecute lineoptions decoded")

            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                val polyline = mMap.addPolyline(lineOptions)
                polyline.width = 15f
                showAllMarkers(markerList!![0], markerList!![1])
            } else {
                Log.d("onPostExecute", "without Polylines drawn")
            }
        }
    }

    inner class DataParser {
        fun parse(jObject: JSONObject): List<List<HashMap<String, String>>> {

            val routes = ArrayList<List<HashMap<String, String>>>()
            val jRoutes: JSONArray
            var jLegs: JSONArray
            var jSteps: JSONArray

            try {

                jRoutes = jObject.getJSONArray("routes")

                /** Traversing all routes  */
                for (i in 0 until jRoutes.length()) {
                    jLegs = (jRoutes.get(i) as JSONObject).getJSONArray("legs")
                    val path = ArrayList<HashMap<String,String>>()

                    /** Traversing all legs  */
                    for (j in 0 until jLegs.length()) {
                        jSteps = (jLegs.get(j) as JSONObject).getJSONArray("steps")

                        /** Traversing all steps  */
                        for (k in 0 until jSteps.length()) {
                            var polyline = ""
                            polyline = ((jSteps.get(k) as JSONObject).get("polyline") as JSONObject).get("points") as String
                            val list = decodePoly(polyline)

                            /** Traversing all points  */
                            for (l in list.indices) {
                                val hm = HashMap<String, String>()
                                hm["lat"] = java.lang.Double.toString(list[l].latitude)
                                hm["lng"] = java.lang.Double.toString(list[l].longitude)
                                path.add(hm)
                            }
                        }
                        routes.add(path)
                    }
                }

            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: Exception) {
            }


            return routes
        }

        private fun decodePoly(encoded: String): List<LatLng> {

            val poly = ArrayList<LatLng>()
            var index = 0
            val len = encoded.length
            var lat = 0
            var lng = 0

            while (index < len) {
                var b: Int
                var shift = 0
                var result = 0
                do {
                    b = encoded[index++].toInt() - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lat += dlat

                shift = 0
                result = 0
                do {
                    b = encoded[index++].toInt() - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
                lng += dlng

                val p = LatLng(lat.toDouble() / 1E5,
                        lng.toDouble() / 1E5)
                poly.add(p)
            }

            return poly
        }
    }


}
