/*
* Copyright (c) Joan-Manuel Marques 2013. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This file is part of the practical assignment of Distributed Systems course.
*
* This code is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This code is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this code.  If not, see <http://www.gnu.org/licenses/>.
*/

package recipes_service.tsae.data_structures;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import recipes_service.data.Operation;

/**
 * @author Joan-Manuel Marques, Daniel Lázaro Iglesias
 * December 2012
 *
 */
public class Log implements Serializable{

	private static final long serialVersionUID = -4864990265268259700L;
	/**
	 * This class implements a log, that stores the operations
	 * received  by a client.
	 * They are stored in a ConcurrentHashMap (a hash table),
	 * that stores a list of operations for each member of 
	 * the group.
	 */
	private ConcurrentHashMap<String, List<Operation>> log= new ConcurrentHashMap<String, List<Operation>>();
	private Semaphore mutex = new Semaphore(1);

	public Log(List<String> participants){
		// create an empty log
		for (Iterator<String> it = participants.iterator(); it.hasNext(); ){
			log.put(it.next(), new Vector<Operation>());
		}
	}

	/**
	 * inserts an operation into the log. Operations are 
	 * inserted in order. If the last operation for 
	 * the user is not the previous operation than the one 
	 * being inserted, the insertion will fail.
	 * 
	 * @param op
	 * @return true if op is inserted, false otherwise.
	 */
	public synchronized boolean add(Operation op){
		try {
			mutex.acquire();

			//Obtengo el nombre del host
			Timestamp ts_op = op.getTimestamp();
			String host_name = ts_op.getHostid();
			//Obtengo la última operacion
			List<Operation> operaciones = log.get(host_name);
			if (operaciones.size() > 0) {
				Operation ultima = operaciones.get(operaciones.size() - 1);

				if (ts_op.compare(ultima.getTimestamp()) > 0) {
					log.get(host_name).add(op);
				}
				else{
					return false;
				}
			}
			else {
				log.get(host_name).add(op);
			}

		} catch (InterruptedException e) {
			return false;
		} finally {
			mutex.release();
		}

		return true;
	}

	/**
	 * This method return the LastTimestamp
	 * @param: String with the host ID
	 * @return: null or the lastTimesTamp 
	 * */
	private Timestamp returnLastTimestamp(String hostID) {
		//Create a List of Operations
      List<Operation> operations = this.log.get(hostID);
      //If operations is null or is empty(method of interface List) return null
      if (operations == null || operations.isEmpty()) {
          return null;
      } else {
    	  //the last is the list is their size -1
    	  int lastOperationsList=operations.size() - 1;
    	//if is OK return the Timestamp the last of list 
    	  return operations.get(lastOperationsList).getTimestamp();
      }
		
	}
	
	/**
	 * Checks the received summary (sum) and determines the operations
	 * contained in the log that have not been seen by
	 * the proprietary of the summary.
	 * Returns them in an ordered list.
	 * @param sum
	 * @return list of operations
	 */
	public synchronized List<Operation> listNewer(TimestampVector sum){
		List<Operation> operations = new ArrayList<>();

		for (String host_name : this.log.keySet()) {
			Timestamp last_timestamp = sum.getLast(host_name);
			List<Operation> operacions_host = this.log.get(host_name);
			for (Operation o : operacions_host) {
				if (o.getTimestamp().compare(last_timestamp) < 1) {
					operations.add(o);
				}
			}
		}

		return operations;

	}
	
	/**
	 * Removes from the log the operations that have
	 * been acknowledged by all the members
	 * of the group, according to the provided
	 * ackSummary. 
	 * @param ack: ackSummary.
	 */
	public synchronized void purgeLog(TimestampMatrix ack){
		TimestampVector minTSV=ack.minTimestampVector();
		for(Map.Entry<String, List<Operation>> entry : log.entrySet()){
			String implicated=entry.getKey();
			List <Operation> ops=entry.getValue();
			Timestamp lastTS=minTSV.getLast(implicated);
			for(int i=ops.size()-1;i>=0;i--){
				Operation op=ops.get(i);
				if (op.getTimestamp().compare(lastTS)<0){
					ops.remove(i);
				}
			}
			
		}
	}

	/**
	 * equals
	 */
	@Override
	public synchronized boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Log other = (Log) obj;
		if (log == null) {
			return other.log == null;
		} else {
			if (log.size() != other.log.size()){
				return false;
			}
			boolean equal = true;
			for (Iterator<String> it = log.keySet().iterator(); it.hasNext() && equal; ){
				String host_name = it.next();
				equal = log.get(host_name).equals(other.log.get(host_name));
				if (!equal){
				}
			}
			return equal;
		}

	}

	/**
	 * toString
	 */
	@Override
	public synchronized String toString() {
		String name="";
		for(Enumeration<List<Operation>> en=log.elements();
		en.hasMoreElements(); ){
		List<Operation> sublog=en.nextElement();
		for(ListIterator<Operation> en2=sublog.listIterator(); en2.hasNext();){
			name+=en2.next().toString()+"\n";
		}
	}
		
		return name;
	}

}
