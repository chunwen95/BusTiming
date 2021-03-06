package ntu.bustiming;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class NearbyFragment extends Fragment implements OnMapReadyCallback {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String MAP_API_KEY= "AIzaSyCGTJlaMA9L-kNPGPD06A3Y-McEPDkmHBU";
    private OnFragmentInteractionListener mListener;
    double Longitude=0.0;
    double Latitude=0.0;
    ProgressDialog p_dialog;

    //SupportMapFragment mapFragment;
    GoogleMap g_map;
    LocationManager locationManager;
    Context mContext;
    Activity mainAct;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = this.getContext();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AccountFragment.
     */


    // TODO: Rename and change types and number of parameters
    public static NearbyFragment newInstance(String param1, String param2) {
        NearbyFragment fragment = new NearbyFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public NearbyFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.nearby_fragment, container, false);
        CheckBusStopFile();
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mainAct = ((MainActivity) getActivity());
        if(mainAct!= null){

            //set default map to singapore
            CameraUpdate point = CameraUpdateFactory.newLatLngZoom(new LatLng(1.290270, 103.851959), 11.0f);
            // moves camera to coordinates
            googleMap.moveCamera(point);
            // animates camera to coordinates
            googleMap.animateCamera(point);
            if(((MainActivity) mainAct).checkLocationPermission()){
                g_map=((MainActivity) mainAct).setUpMap(googleMap);
                g_map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        if (marker.getTitle().toString() != "You") {
                            try {
                                String BusStopCode = marker.getTitle().substring(marker.getTitle().lastIndexOf("(") + 1, marker.getTitle().length() - 1);
                                String BusStopDescription = marker.getTitle().substring(0, marker.getTitle().lastIndexOf("(") - 1);
                                setUpAndDisplayBusTiming(BusStopCode, BusStopDescription, marker.getPosition().latitude, marker.getPosition().longitude);
                            }catch(Exception e){
                                e.printStackTrace();
                                return false;
                            }
                        }

                        return false;
                    }
                });
            }
        }
    }
    public void setUpAndDisplayBusTiming(String Busstop_code, String Busstop_description, double lat, double lng){
        LTADatamallController LTADatamallController = new LTADatamallController(this.getContext());
        JSONObject bus_arrival_timing = LTADatamallController.getBusArrivalByBusStopCode(Busstop_code);
        if(bus_arrival_timing!=null){
            //display bus timing
            try {
                ((MainActivity) mainAct).displayBusTiming(bus_arrival_timing, Busstop_code, Busstop_description, lat, lng);
                //mListener.onNearbyFragmentInteraction();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void CheckBusStopFile(){
            LTADatamallController api = new LTADatamallController(this.getContext());
            File BusStop_file = new File(getContext().getFilesDir(), "BusTiming/BusStops.txt");
            if (BusStop_file.exists()) {
                try {
                    //check total bus stop count is the same
                    StringBuilder total = new StringBuilder();
                    FileInputStream fis = new FileInputStream(BusStop_file);
                    int numRead =0;
                    byte[] bytes = new byte[fis.available()];
                    while ((numRead = fis.read(bytes)) >= 0) {
                        total.append(new String(bytes, 0, numRead));
                    }
                    fis.close();
                    JSONObject jsonraw = new JSONObject(total.toString());
                    BusStopStruct bs_struct = new BusStopStruct(jsonraw);
                    JSONObject temp = api.getBusStops(bs_struct.getBs_Last_Skip_Count());
                    JSONArray current = temp.getJSONArray("value");
                    int count = current.length();


                    if (count == bs_struct.getBs_Last_Count()) {
                        //latest
                        return;
                    } else {
                        //re-fetch file;
                        try {
                            api.fetchAllBusStop();
                        } catch(Exception e){ e.printStackTrace(); }

                    }
                } catch (FileNotFoundException e) {
                    Log.e("get bus stop file", "File not found: " + e.toString());
                } catch (IOException e) {
                    Log.e("get bus stop file", "Can not read file: " + e.toString());
                }catch (JSONException e) {
                    Log.e("get bus stop file", "JSON error: " + e.toString());
                }
            } else { //busstop file not yet fetched
                try {
                    api.fetchAllBusStop();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onNearbyFragmentInteraction();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onNearbyFragmentInteraction();
    }
}
