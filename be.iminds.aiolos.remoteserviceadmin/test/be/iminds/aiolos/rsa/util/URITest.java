package be.iminds.aiolos.rsa.util;

import junit.framework.Assert;

import org.junit.Test;

import be.iminds.aiolos.rsa.util.URI;

public class URITest {

	@Test
	public void testIpv6() {
		String s = "r-osgi://[2001:6a8:1d80:1128:5e51:4fff:fe11:86ef]:9278#77";
		URI uri = new URI(s);
		
		Assert.assertEquals("r-osgi", uri.getProtocol());
		Assert.assertEquals("2001:6a8:1d80:1128:5e51:4fff:fe11:86ef", uri.getIP());
		Assert.assertEquals(9278, uri.getPort());
		Assert.assertEquals("77", uri.getServiceId());
		
		Assert.assertEquals(s, uri.toString());
	}

	
	@Test
	public void testIpv4() {
		String s = "r-osgi://127.0.0.1:9278#77";
		URI uri = new URI(s);
		
		Assert.assertEquals("r-osgi", uri.getProtocol());
		Assert.assertEquals("127.0.0.1", uri.getIP());
		Assert.assertEquals(9278, uri.getPort());
		Assert.assertEquals("77", uri.getServiceId());
		
		Assert.assertEquals(s, uri.toString());
	}
}
