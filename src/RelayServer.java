/**
#``DNS Relay for CCNx''
#
#Copyright (C) 2012, Delft University of Technology, Faculty of Electrical Engineering, Mathematics and Computer Science, Network Architectures and Services, Niels van Adrichem
#
#    This file is part of ``DNS Relay for CCNx''.
#
#    ``DNS Relay for CCNx'' is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License version 3 as published by
#    the Free Software Foundation.
#
#    ``DNS Relay for CCNx'' is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with ``DNS Relay for CCNx''.  If not, see <http:#www.gnu.org/licenses/>.
**/

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.CCNInterestHandler;
import org.ccnx.ccn.KeyManager;
import org.ccnx.ccn.config.ConfigurationException;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.SignedInfo;
import org.ccnx.ccn.protocol.SignedInfo.ContentType;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;


public class RelayServer implements CCNInterestHandler {

	private Resolver _resolver = null;
	private CCNHandle ccnHandle;
	
	public RelayServer(ContentName _name) {
		this(_name, null);
	}

	public RelayServer(ContentName tServerName, String tServerIP) {
		
		try {
		
			if( tServerIP != null)
				_resolver = new SimpleResolver(tServerIP);
			else
				_resolver = new SimpleResolver();
		
			ccnHandle = CCNHandle.open();
			
			ccnHandle.registerFilter(tServerName, this);
			
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	private ContentObject createCO(Interest intrst, Message resp) throws InvalidKeyException, SignatureException
	{
		
		KeyManager bkm = CCNHandle.getHandle().keyManager();
		SignedInfo si = new SignedInfo(bkm.getDefaultKeyID(), CCNTime.now(), ContentType.DATA, bkm.getDefaultKeyLocator());
		
		ContentObject co = new ContentObject(intrst.name(), si, resp.toWire(), bkm.getDefaultSigningKey());
		
		return co;
	}
	
	@Override
	public boolean handleInterest(Interest intrst) {
		ContentName cn = intrst.name();
		String type = cn.stringComponent(cn.count() - 1);
		String name = cn.stringComponent(cn.count() - 2);
		
		System.out.println("We received an " + type + " request for " + name);
		
		try {
			Record question = Record.newRecord(Name.fromString(name), Type.value(type), DClass.IN);
			Message query = Message.newQuery(question);
			Message response = _resolver.send(query);
			
			System.out.println("We resolved it.");
			
			ccnHandle.put(createCO(intrst, response));
			System.out.println("We returned it");
			
		} catch (TextParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

}
