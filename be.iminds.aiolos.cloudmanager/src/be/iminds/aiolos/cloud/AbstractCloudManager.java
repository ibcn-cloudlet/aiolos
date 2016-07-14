package be.iminds.aiolos.cloud;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.cloud.api.CloudManager;


public abstract class AbstractCloudManager implements CloudManager {

	public abstract void configure(Dictionary<String,?> properties);
	
	protected Collection<File> getFilteredBndRunFiles() {
		Collection<File> bndruns = new ArrayList<File>();
		File f = new File("."); // current directory
		Activator.logger.log(LogService.LOG_DEBUG, "Current directory: " + f.getAbsolutePath());
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				// ignore hidden files, launch.bndrun and select all run-descriptor files
				return (!lowercaseName.startsWith(".") && lowercaseName.endsWith(".bndrun") && !lowercaseName.equals("launch.bndrun"));
			}
		};
		bndruns.addAll(Arrays.asList(f.listFiles(filter)));
		return bndruns;
	}

	@Override
	public Collection<String> getBndrunFiles() {
		Collection<String> bndruns = new ArrayList<String>();
	    for (File file : getFilteredBndRunFiles()) {
        	bndruns.add(file.getName());
	    }
	    Activator.logger.log(LogService.LOG_DEBUG, "Accessible bndrun files: " + bndruns.toString());
		return bndruns;
	}
}
