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
package be.iminds.aiolos.repository.command;

import org.osgi.resource.Capability;

import be.iminds.aiolos.repository.RepositoryImpl;

/**
 * CLI Commands for the Repository
 */
public class RepoCommands {

	private final RepositoryImpl repo;
	
	public RepoCommands(RepositoryImpl r){
		this.repo = r;
	}
	
	public void list(){
		System.out.println("Contents of repository "+repo.getName());
		System.out.println("===");
		for(Capability c : repo.listCapabilities(null)){
			printCapability(c);
		}
	}
	
	public void list(String namespace){
		System.out.println("Contents of repository "+repo.getName()+", namespace "+namespace);
		System.out.println("===");
		for(Capability c : repo.listCapabilities(namespace)){
			printCapability(c);
		}
	}
	
	private void printCapability(Capability c){
		for(String key : c.getAttributes().keySet()){
			System.out.println(key +" : "+c.getAttributes().get(key));
		}
		System.out.println("---");
	}
}
