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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.MalformedContentNameStringException;
import org.xbill.DNS.Address;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;


public class RelayClient {

	private CCNHandle ccnHandle;
	private ContentName _serverName;
	private DatagramSocket sock;

	private Message errorMessage(Message incMsg, int rcode)
	{
		Message response = new Message();
		response.setHeader(incMsg.getHeader());
		
		for(int i = 0; i < 4; i++)
			response.removeAllRecords(i); //remove all records from all sections
		
		if(rcode == Rcode.SERVFAIL)
			response.addRecord(incMsg.getQuestion(), Section.QUESTION);
		
		response.getHeader().setRcode(rcode); //set the appropriate error code.
		response.getHeader().setFlag(Flags.QR); //it's a query response, isn't it?
		return response;
	}
	
	private Message generateReply(Message incMsg) throws MalformedContentNameStringException, IOException
	{
		Header header;
				
		header = incMsg.getHeader();
		if(header.getFlag(Flags.QR)) 						//we received a Query Response instead of a query.
			return null;										//Return nothing
		if(header.getRcode() != Rcode.NOERROR)				//We received a query containing an error
			return errorMessage(incMsg, Rcode.FORMERR);			//Return a Format Error
		if(header.getOpcode() != Opcode.QUERY)				//We received something different than a regular query
			return errorMessage(incMsg, Rcode.NOTIMP);			//Return a Not Implemented error
		
		if(header.getCount(Section.ADDITIONAL) != 0 || header.getCount(Section.AUTHORITY) != 0 || header.getCount(Section.ANSWER)!= 0 || header.getCount(Section.QUESTION) != 1)
			return errorMessage(incMsg, Rcode.FORMERR);		//We only support 1 question-queries.
		
		Record rec = incMsg.getQuestion();
		System.out.println("Request " + Type.string(rec.getType()) + " for " + rec.getName());
		
		ContentName req = _serverName.append(rec.getName().toString()).append(Type.string(rec.getType()));
		ContentObject response = ccnHandle.get(req, 10000);
		
		if(response == null)
		{
			return null;
		}
		else
		{	
			Message outMsg = new Message( response.content() );
			outMsg.getHeader().setID(incMsg.getHeader().getID());
			return outMsg;
		}
		
		
	}
	
	public RelayClient(ContentName tServerName)  {
		_serverName = tServerName;
		try{
			ccnHandle = CCNHandle.getHandle();
			
			InetAddress ia = Address.getByAddress("0.0.0.0");
			sock = new DatagramSocket(53, ia);
			byte[] buffer = new byte[65536];
			DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
			
			while(true)
			{
				try {
					sock.receive(incoming);
					
					//byte[] data = incoming.getData();
					//String sData = new String(data, 0, data.length);
					int port = incoming.getPort();
					InetAddress addr = incoming.getAddress();
					
					System.out.println("Received packet from " + addr + ":" + port);
					Message query = new Message (buffer);
					
					Message response = generateReply(query);
					
					if(response != null)
					{
						byte[] byteResponse = response.toWire();
						System.out.println("Non-empty reply");
						sock.send(new DatagramPacket(byteResponse, byteResponse.length, addr, port));
					}
					else
					{
						System.out.println("Empty-reply");
					}
					
					
					//System.out.println(sData);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MalformedContentNameStringException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (SocketException e)
		{
			System.out.println("***");
			System.out.println("   Make sure that you have permission to open low-numbered port 53.");
			System.out.println("   Either run program as root or using sudo, or use setcap to allow the program to open low-numbered ports.");
			System.out.println("***");
			ccnHandle.close();
			e.printStackTrace();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
		
		
	}

}
