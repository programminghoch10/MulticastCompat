package com.JJ.multicastcompat;

import android.net.nsd.NsdServiceInfo;

public interface ResolveListener {
	void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode);
	void onServiceResolved(NsdServiceInfo serviceInfo);
}
