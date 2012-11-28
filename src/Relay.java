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

import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;

public class Relay {

	
	public static void StartMessage()
	{
		System.out.println("Please run with the following arguments:");
		System.out.println("\t\"client\" <NDN-name>; to start a local DNS server that relays to the server at <NDN-name>");
		System.out.println("\t\"server\" <NDN-name> [<IP-address>]; to start a local NDN DNS-relay that either uses the local DNS server, or the server specified in <IP-address>");
		System.exit(-1);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length < 2 || args.length > 3)
		{
			StartMessage();
		}
		
		if(args[0].equals("client") && args.length!=2 )
		{
			StartMessage();
		}
		
		try {
			if(args[0].equals("client"))
			{
				ContentName _name = ContentName.fromURI(args[1]);
				new RelayClient(_name);
			}
			
			if(args[0].equals("server"))
			{
				ContentName _name = ContentName.fromURI(args[1]);
				if(args.length == 3)
					new RelayServer(_name, args[2]);
				else
					new RelayServer(_name);
				
			}
		} catch (MalformedContentNameStringException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
