/*******************************************************************************
 * AIOLOS  - Framework for dynamic distribution of software components at runtime.
 * Copyright (C) 2014-2016  iMinds - IBCN - UGent
 *
 * This file is part of AIOLOS.
 *
 * AIOLOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Tim Verbelen, Steven Bohez, Elias Deconinck
 *******************************************************************************/
package be.iminds.aiolos.monitor.service.api;

public class Statistic {

	private int n;
	private double oldM = 0;
	private double newM = 0;
	private double oldS = 0;
	private double newS = 0;
	
	public void addValue(double x){
		n++;
		
		if(n==1){
			oldM = x;
			newM = x;
			oldS = 0;
		} else {
			newM = oldM + (x - oldM)/n;
			newS = oldS + (x - oldM)*(x-newM);
			
			oldM = newM;
			oldS = newS;
		}
	}
	
	public int noValues(){
		return n;
	}
	
	double mean(){
		return newM;
	}
	
	double variance(){
		return (n>1)? newS/(n-1) : 0;
	}
	
	double stdev(){
		return Math.sqrt(variance());
	}
}
