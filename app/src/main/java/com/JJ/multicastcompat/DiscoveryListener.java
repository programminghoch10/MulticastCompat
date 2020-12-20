package com.JJ.multicastcompat;

import android.net.nsd.NsdServiceInfo;

public interface DiscoveryListener {
	void onStartDiscoveryFailed(String serviceType, int errorCode);
	void onStopDiscoveryFailed(String serviceType, int errorCode);
	void onDiscoveryStarted(String serviceType);
	void onDiscoveryStopped(String serviceType);
	void onServiceFound(NsdServiceInfo serviceInfo);
	void onServiceLost(NsdServiceInfo serviceInfo);
}
