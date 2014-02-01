package sagex.phoenix.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import sagex.phoenix.installer.Plugin.Dependency;
import sagex.phoenix.installer.Plugin.Package;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

public class PluginManager {
	public static final String PLUGINS_FILE = "SageTVPlugins.xml";
	
	public class PluginDependencyComparator implements Comparator<Plugin> {
		@Override
		public int compare(Plugin o1, Plugin o2) {
			if (o1.identifier.equalsIgnoreCase(o2.identifier)) return 0;
			
			if (o1.dependOn(o2)) {
				return -1;
			} else if (o2.dependOn(o1)) {
				return 1;
			}
			
			return 0;
		}
	}
	private IPluginModel model;
	private IPackageResolver urlResolver;
	private boolean overwrite=false;
	private boolean useResolver = false;
	
	// map of plugins containing a map of versions for plugins
	private Map<String, SortedMap<String, Plugin>> plugins = new HashMap<String, SortedMap<String, Plugin>>();
	
	public Map<String, SortedMap<String, Plugin>> getPlugins() {
		return plugins;
	}

	public PluginManager(IPluginModel model, IPackageResolver urlResolver) {
		this.model = model;
		this.urlResolver = urlResolver;
		if (urlResolver==null) {
			useResolver=false;
		} else {
			useResolver=true;
		}
	}
	
	public void loadPlugins() throws Exception {
		plugins.clear();
		model.loadPlugins(this);		
	}
	
	public List<Plugin> resolvePlugin(String name) throws Exception {
		Plugin p = getPlugin(new Dependency(name, null));
		List<Plugin> plugins = new ArrayList<Plugin>();
		resolve(p, plugins);
		
		Collections.sort(plugins, new PluginDependencyComparator());
		Collections.reverse(plugins);
		return plugins;
	}

	private void resolve(Plugin p, List<Plugin> pluginList) throws Exception {
		if (!pluginList.contains(p)) {
			pluginList.add(p);
			for (Dependency d: p.dependencies) {
				resolve(getPlugin(d), pluginList);
			}
		} else {
			// we already have it, and it's dependencies
		}
	}

	private Plugin getPlugin(Dependency dep) throws Exception {
		SortedMap<String, Plugin> versions = getPluginVersions(dep.plugin);
		return getLatestVersion(versions);
	}

	private Plugin getLatestVersion(SortedMap<String, Plugin> versions) {
		return versions.get(versions.lastKey());
	}

	private SortedMap<String, Plugin> getPluginVersions(String name) throws Exception {
		SortedMap<String, Plugin> versions = plugins.get(name.toLowerCase());
		if (versions == null) {
			throw new Exception("Plugin Not Found: " + name);
		}
		return versions;
	}

	public void addPlugin(Plugin plugin) {
		SortedMap<String, Plugin> p = plugins.get(plugin.identifier.toLowerCase());
		if (p==null) {
			p = new TreeMap<String, Plugin>(Versions.COMPARATOR);
			plugins.put(plugin.identifier.toLowerCase(), p);
		}
		p.put(plugin.version, plugin);
	}

	public void dumpPlugins(PrintStream out) {
		out.println("Begin Dumping Plugins");
		for (Map.Entry<String, SortedMap<String, Plugin>> pe: plugins.entrySet()) {
			out.printf("   Plugin [%s]\n", pe.getKey());
			for (Map.Entry<String, Plugin> p: pe.getValue().entrySet()) {
				out.printf("      Version: %s\n", p.getKey());
				for (Dependency d: p.getValue().dependencies) {
					out.printf("      Depends: %s\n", d.plugin );
				}
				for (sagex.phoenix.installer.Plugin.Package pp: p.getValue().packages) {
					out.printf("      Package: [%s][%s]\n", pp.packageType, pp.location );
				}
			}
			out.println("");
		}
		out.println("End Dumping Plugins");
	}
	
	public void dumpPlugins(List<Plugin> plugins, IOutput out) {
		out.msg("BEGIN: Dumping Plugin List");
		for (Plugin p: plugins) {
			out.msg(String.format("Plugin: [%s, %s]", p.identifier, p.version));
			for (Dependency d: p.dependencies) {
				out.msg(String.format("   Depends: %s", d.plugin ));
			}
		}
		out.msg("END: Dumping Plugin List");
	}

	public void installPlugins(List<Plugin> install, File sageHome, IOutput out) throws Exception {
		if (!sageHome.exists() || !sageHome.isDirectory()) throw new Exception("Not a SageTV HOME: " + sageHome);
		for (Plugin p: install) {
			installPlugin(p, sageHome, out);
		}
	}

	private void installPlugin(Plugin p, File sageHome, IOutput out) throws Exception {
		out.msg("Installing Plugin: " + p.identifier);
		for (Package pk: p.packages) {
			installPackage(pk, sageHome, out);
		}
		out.msg("");
	}

	private void installPackage(Package pk, File sageHome, IOutput out) throws Exception {
		if (useResolver) {
			pk = urlResolver.resolvePackage(pk);
		}
		File tmpDir = new File(sageHome,"pluginstmp");
		tmpDir.mkdirs();
		if (!tmpDir.exists()) {
			throw new Exception("Failed to create tmp area for plugins: " + tmpDir.getAbsolutePath());
		}
		
		// use hash for the name, since the url may not be to a filename
		HashFunction hf = Hashing.md5();
		String fname = hf.hashString(pk.location).toString() + ".zip";
		File dest = new File(tmpDir, fname);
		downloadFile(pk.location, dest, out);
		validatePackage(pk, dest, out);
		extractPackage(pk, dest, sageHome, out);
		out.msg("Removing " + dest);
		dest.delete();
	}

	private void extractPackage(Package pk, File out, File sageHome, IOutput out2) throws Exception {
		out2.msg("Extracting Files...");
		if ("jar".equalsIgnoreCase(Files.getFileExtension(out.getName()))) {
			File dir = new File(sageHome, "JARs");
			dir.mkdirs();
			Files.copy(out, new File(dir, out.getName()));
			out2.msg("> JAR: " + out);
			return;
		}
		if ("jar".equalsIgnoreCase(pk.packageType)) {
			unzip(out, new File(sageHome, "JARs"), out2);
			return;
		}
		if ("system".equalsIgnoreCase(pk.packageType)) {
			unzip(out, sageHome, out2);
			return;
		}
		if ("stvi".equalsIgnoreCase(pk.packageType)) {
			unzip(out, new File(sageHome, "STVs/SageTV7"), out2);
			return;
		}
		if ("stv".equalsIgnoreCase(pk.packageType)) {
			unzip(out, new File(sageHome, "STVs"), out2);
			return;
		}
		
		throw new UnsupportedOperationException("Unknown Package Type: " + pk.packageType);
	}
	
	public void unzip(File zip, File baseDir, IOutput out) throws Exception {
		final ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			if (!entry.isDirectory()) {
				final File file = new File(baseDir, entry.getName());
				Files.createParentDirs(file);
				if (file.exists()) {
					out.msg(" > " + file);
				} else {
					out.msg(" + " + file);
				}
				Files.write(ByteStreams.toByteArray(zis), file);
			}
		}
	}

	private void validatePackage(Package pk, File out, IOutput out2) throws Exception {
		if ("--".equals(pk.MD5)) {
			out2.msg("Skipping Validation for " + out);
			return;
		}
		
		if (pk.MD5!=null) {
			HashCode md5 = Files.hash(out, Hashing.md5());
			if (!md5.toString().equalsIgnoreCase(pk.MD5)) {
				throw new Exception("CheckSum for " + out + " (downloaded from " + pk.location + ") was " + md5.toString() + ", but we expected " + pk.MD5);
			}
			out2.msg("Package is valid for " + out);
		}
	}

	private void downloadFile(String location, File file, IOutput out) throws Exception {
		out.msg("");
		if (useResolver && location.contains("download.sage.tv")) {
			throw new Exception("Need to add a resolver for " + location);
		}
		
		if (useResolver && location.contains("sagetv.com")) {
			throw new Exception("Need to add a resolver for " + location);
		}
		
		out.msg("Downloading File: " + location);
		if (overwrite && file.exists()) {
			if (!file.delete()) {
				throw new Exception("Was unable to remove an older copy of " + file + ".  Please ensure that you have the correct permissions");
			}
		}
		
		if (!overwrite && file.exists()) {
			out.msg("Already Downloaded: " + file);
			return;
		}
		
		URL website = new URL(location);
	    FileOutputStream fos = new FileOutputStream(file);
	    ByteStreams.copy(website.openStream(), fos);
	    fos.flush();
	    fos.close();
	}

	public void setPluginModel(SageTVPluginModelImpl pluginModel) throws Exception {
		this.model = pluginModel;
		loadPlugins();
	}

	public void setUseResolvers(boolean value) {
		this.useResolver=value;
	}
}
