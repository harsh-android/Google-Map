package com.hinfo.googlemap.Fragments

import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.hinfo.googlemap.DirectionsJSONParser
import com.hinfo.googlemap.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class MapsFragment : Fragment() {
    var markerPoints = ArrayList<LatLng>()
    companion object {
        lateinit var googleMap: GoogleMap
    }
    private val callback = OnMapReadyCallback { googleMap ->
      MapsFragment.googleMap = googleMap
        val sydney = LatLng(-34.0, 151.0)
        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))


        googleMap.setOnMapClickListener {latLng ->
            if (markerPoints.size > 1) {
                markerPoints.clear();
                googleMap.clear();
            }
            markerPoints.add(latLng);

            val options = MarkerOptions()
            options.position(latLng)

            if (markerPoints.size == 1) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            } else if (markerPoints.size == 2) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            }

            googleMap.addMarker(options);
            if (markerPoints.size >= 2) {

                // Getting URL to the Google Directions API
                val url: String = getDirectionsUrl(markerPoints[0], markerPoints[1])!!
                val downloadTask = DownloadTask()

                // Start downloading json data from Google Directions API
                downloadTask.execute(url)
            }
        }


    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String? {

        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude

        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude

        // Sensor enabled
        val sensor = "sensor=false"
        val mode = "mode=driving"

        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$sensor&$mode"

        // Output format
        val output = "json"

        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }



    class DownloadTask : AsyncTask<Any?, Any?, Any?>() {
        override fun doInBackground(vararg url: Any?): String {
            var data = ""
            try {
                data = downloadUrl(url[0].toString())!!
            } catch (e: Exception) {
                Log.d("Background Task", e.toString())
            }
            return data
        }

        protected fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val parserTask = ParserTask()
            parserTask.execute(result)
        }

        @Throws(IOException::class)
        private fun downloadUrl(strUrl: String): String? {
            var data = ""
            var iStream: InputStream? = null
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(strUrl)
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connect()
                iStream = urlConnection.inputStream
                val br = BufferedReader(InputStreamReader(iStream))
                val sb = StringBuffer()
                var line: String? = ""
                while (br.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                data = sb.toString()
                br.close()
            } catch (e: java.lang.Exception) {
                Log.d("Exception", e.toString())
            } finally {
                iStream!!.close()
                urlConnection!!.disconnect()
            }
            return data
        }


    }

    private class ParserTask :
        AsyncTask<String?, Int?, List<List<HashMap<*, *>>>?>() {
        // Parsing the data in non-ui threa

        override fun onPostExecute(result: List<List<HashMap<*, *>>>?) {
            var points: ArrayList<LatLng>? = null
            var lineOptions: PolylineOptions? = null
            val markerOptions = MarkerOptions()
            for (i in result!!.indices) {
                points = ArrayList<LatLng>()
                lineOptions = PolylineOptions()
                val path = result[i]
                for (j in path.indices) {
                    val point = path[j]
                    val lat: Double = point["lat"].toString().toDouble()
                    val lng: Double = point["lng"].toString().toDouble()
                    val position = LatLng(lat, lng)
                    points.add(position)
                }
                lineOptions.addAll(points)
                lineOptions.width(12f)
                lineOptions.color(Color.RED)
                lineOptions.geodesic(true)
            }

// Drawing polyline in the Google Map for the i-th route
            googleMap.addPolyline(lineOptions!!)
        }

        override fun doInBackground(vararg params: String?): List<List<HashMap<*, *>>>? {
            val jObject: JSONObject
            var routes: List<List<HashMap<*, *>>>? = null
            try {
                jObject = JSONObject(params[0])
                val parser = DirectionsJSONParser()
                routes = parser.parse(jObject)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return routes
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }



}