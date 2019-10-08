package com.webank.wecross.core;

import java.util.List;
import com.webank.wecross.bcp.Resource;

import com.webank.wecross.bcp.Stub;
import com.webank.wecross.bcp.URI;

public class StubManager {
	private List<Stub> stubs;
	
	public Stub getStub(URI uri) {
		for(Stub stub: stubs) {
			if(uri.getResource().equals(stub.getPattern())) {
				return stub;
			}
		}
		
		return null;
	}
	
	public Resource getResource(URI uri) throws Exception {
		Stub stub = getStub(uri);
		return stub.getResource(uri);
	}

	public List<Stub> getStubs() {
		return stubs;
	}

	public void setStubs(List<Stub> stubs) {
		this.stubs = stubs;
	}
}