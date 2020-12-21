package com.JJ.multicastcompat;

import android.net.nsd.NsdServiceInfo;
import android.os.Build;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MulticastServiceInfo {
	public int port = 0;
	public InetAddress host;
	public String serviceName;
	public String serviceType;
	public HashMap<String, String> attributes;
	
	public MulticastServiceInfo() {
	}
	
	public MulticastServiceInfo(String serviceName, String serviceType, InetAddress host, int port) {
		this.serviceName = serviceName;
		this.serviceType = serviceType;
		this.host = host;
		this.port = port;
	}
	
	public MulticastServiceInfo(NsdServiceInfo serviceInfo) {
		this.serviceType = serviceInfo.getServiceType();
		this.serviceName = serviceInfo.getServiceName();
		this.port = serviceInfo.getPort();
		this.host = serviceInfo.getHost();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			attributes = new HashMap<>();
			Map<String, byte[]> map = serviceInfo.getAttributes();
			for (String key : map.keySet()) {
				attributes.put(key, new String(map.get(key)));
			}
		}
	}
	
	/**
	 * Merge two MulticastServiceInfo objects, preferring values from serviceInfo2.
	 *
	 * @param serviceInfo1 first (base) object
	 * @param serviceInfo2 second object to merge on top of first one
	 * @return Merged new MulticastServiceInfo object
	 */
	public static MulticastServiceInfo merge(MulticastServiceInfo serviceInfo1, MulticastServiceInfo serviceInfo2) {
		if (serviceInfo1 == null) return serviceInfo2;
		if (serviceInfo2 == null) return serviceInfo1;
		MulticastServiceInfo serviceInfo = new MulticastServiceInfo();
		if (serviceInfo2.host == null) {
			serviceInfo.host = serviceInfo1.host;
		} else {
			serviceInfo.host = serviceInfo2.host;
		}
		if (serviceInfo2.serviceName == null) {
			serviceInfo.serviceName = serviceInfo1.serviceName;
		} else {
			serviceInfo.serviceName = serviceInfo2.serviceName;
		}
		if (serviceInfo2.serviceType == null) {
			serviceInfo.serviceType = serviceInfo1.serviceType;
		} else {
			serviceInfo.serviceType = serviceInfo2.serviceType;
		}
		if (serviceInfo2.port == 0) {
			serviceInfo.port = serviceInfo1.port;
		} else {
			serviceInfo.port = serviceInfo2.port;
		}
		serviceInfo.attributes = new LinkedHashMap<>();
		if (serviceInfo1.attributes != null) {
			for (String key : serviceInfo1.attributes.keySet()) {
				serviceInfo.attributes.put(key, serviceInfo1.attributes.get(key));
			}
		}
		if (serviceInfo2.attributes != null) {
			for (String key : serviceInfo2.attributes.keySet()) {
				serviceInfo.attributes.put(key, serviceInfo2.attributes.get(key));
			}
		}
		return serviceInfo;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public void setAttributes(Map<String, String> attributes) {
		this.attributes = (HashMap<String, String>) attributes;
	}
	
	public NsdServiceInfo toNsdServiceInfo() {
		NsdServiceInfo serviceInfo = new NsdServiceInfo();
		serviceInfo.setServiceName(this.serviceName);
		serviceInfo.setServiceType(this.serviceType);
		serviceInfo.setHost(this.host);
		serviceInfo.setPort(this.port);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			for (String key : attributes.keySet()) {
				serviceInfo.setAttribute(key, attributes.get(key));
			}
		}
		return serviceInfo;
	}
	
	//TODO: overwrite toString
}
