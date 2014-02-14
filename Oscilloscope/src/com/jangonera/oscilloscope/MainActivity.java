package com.jangonera.oscilloscope;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.example.oscilloscope.R;
import com.jangonera.oscilloscope.ExternalDataService.ExternalDataServiceBinder;

public class MainActivity extends FragmentActivity {
	private FragmentTransaction ft;
	private SetupFragment setupFRAG;
	private GraphsFragment graphsFRAG;
	private boolean inSetup;
	private boolean displayingGraphs;
	//private boolean smallScreen;
	private BluetoothManager bluetoothManager;
	private ExternalDataService myService;
	private ExternalDataContainer externalDataContainer;
	// use mBound to check if the service is available
	private boolean mBound;

    private DrawerLayout mDrawerLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Remember whether we were scanning or not

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		// restore block
		displayingGraphs = false;
		externalDataContainer = ExternalDataContainer.getContainer();
		if (savedInstanceState != null) {

		}
        loadGraphs();
        loadSetup();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle abdt = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_launcher, 1, 2){

        };
        mDrawerLayout.setDrawerListener(abdt);

        bluetoothManager = BluetoothManager.getBluetoothManager();
		bluetoothManager.registerContext(this);
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(bluetoothManager, filter);

		connectToService();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
		unregisterReceiver(bluetoothManager);
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//outState.putBoolean(Const.WORKING_ON_MONO_SCREEN, smallScreen);
		outState.putBoolean(Const.IN_SETUP, inSetup);
	}

	// Scan for probes button in setup
	public void scanForDevices(View view) {
		if (isBound()) {
			bluetoothManager.scanForDevices();
			invalidateScannedDeviceList();
			markAsDiscovering();
		}
	}

	public ExternalDataService getService() {
		return myService;
	}

	public boolean isBound() {
		return mBound;
	}

	public void invalidateScannedDeviceList() {
		if (setupFRAG != null)
			setupFRAG.invalidateList();
	}
	
	public void invalidateGraphsList() {
		if (graphsFRAG != null){
			graphsFRAG.invalidateList();
		}
	}

	public void loadSetup() {
		// if (setupFRAG == null)
		setupFRAG = new SetupFragment();
		ft = getSupportFragmentManager().beginTransaction();
		// Check the screen size. Area_setup available on large screens only
		View setupArea = findViewById(R.id.area_setup);

		// depending on the size of the screen load the fragment to different
		// parts
		

			ft.replace(R.id.area_setup, setupFRAG);

		
		ft.commit();
		inSetup = true;
	}

	public void addGraph(int index) {
		if(externalDataContainer.getScanProbe(index).addToReadyProbes()){
			display(Const.NEW_GRAPH_ADDED);
			loadGraphs();
			invalidateGraphsList();
			//refresh to display which probe is "displaying"
			invalidateScannedDeviceList();
		}
		else loadGraphs();
		//Log.i(Const.tag_MA, "clicked " + Integer.toString(index));
	}
	
	public void removeGraph(int index){
		if(externalDataContainer.removeReadyProbe(index)){
			display(Const.GRAPH_REMOVED);
			
			//TODO
			//why isn't mono screen working without this line (after rotation) ?
			loadGraphs();
			
			invalidateGraphsList();
			//refresh to display which probe is "displaying"
			invalidateScannedDeviceList();
		}
	}

	private void loadGraphs() {
		// on big screen, load graphs only once a turn
		View graphArea = findViewById(R.id.area_graphs);
		if((graphArea != null)&&(displayingGraphs)){
			return;
		}
		
		// on mono screen, load only graphs without setup
		// on multi screen, keep the setup and load the graphs beside
		// Check the screen size. Area_graphs available on large-land screens only
		// if (graphsFRAG == null)
		graphsFRAG = new GraphsFragment();
		ft = getSupportFragmentManager().beginTransaction();

			ft.replace(R.id.area_graphs, graphsFRAG);

		ft.commit();
		inSetup = false;
		displayingGraphs = true;
	}

	// DATA EXCHANGE///////////////////////////////
	private void connectToService() {
		Intent intent = new Intent(this, ExternalDataService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			ExternalDataServiceBinder binder = (ExternalDataServiceBinder) service;
			myService = binder.getService();
			// bluetoothManager.registerService(myService);
			mBound = true;
			invalidateScannedDeviceList();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	public void display(int message) {
		switch (message) {
		case Const.BLUETOOTH_UNAVAILABLE:
			Toast.makeText(this, R.string.blueetooth_unavailable,
					Toast.LENGTH_SHORT).show();
			break;
		case Const.BLUETOOTH_NEW_DEVICE:
			Toast.makeText(this, R.string.new_device_found, Toast.LENGTH_SHORT)
					.show();
			break;
		case Const.NEW_GRAPH_ADDED:
			Toast.makeText(this, R.string.new_graph_added, Toast.LENGTH_SHORT)
					.show();
			break;
		case Const.GRAPH_REMOVED:
			Toast.makeText(this, R.string.graph_removed, Toast.LENGTH_SHORT)
					.show();
			break;
		}
	}

	public void markAsDiscovering() {
		if (setupFRAG != null)
			setupFRAG.markAsDiscovering();
	}

	public void markAsFinishedDiscovery() {
		if (setupFRAG != null)
			setupFRAG.markAsFinishedDiscovery();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == Const.REQUEST_BLUETOOTH_ON)
				&& (resultCode == Activity.RESULT_OK)) {
			scanForDevices(null);
		}

	}

	public BluetoothManager getBluetoothManager() {
		return bluetoothManager;
	}

    public void openDrawer() {
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }
}
