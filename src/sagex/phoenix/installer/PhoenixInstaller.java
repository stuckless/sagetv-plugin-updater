package sagex.phoenix.installer;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.google.common.base.Throwables;

public class PhoenixInstaller {
	public File sageHome;
	public IOutput out;
	public PluginManager pm;
	public String extraResolver = "packages.xml";
	public String pluginsFile = "SageTVPlugins.xml";

	public PhoenixInstaller(IOutput out) throws Exception {
		this.out=out;
		URL plugins = null;
		
		File f = new File(pluginsFile);
		if (f.exists()) {
			plugins = f.toURL();
		}
		
		if (plugins==null) {
			plugins = PhoenixInstaller.class.getResource("SageTVPlugins.xml");
		}
		
		SageTVPluginModelImpl model = new SageTVPluginModelImpl(plugins);
		
		DefaultPackageResolver resolver = new DefaultPackageResolver();
		File res = new File(extraResolver);
		if (res.exists()) {
			resolver.loadPackages(res.toURL());
		}
		pm = new PluginManager(model, resolver);
		pm.loadPlugins();
	}

	public void install(String plugin) {
		out.msg("Downloading and extracting Plugins files to " + sageHome);
		try {
			if (sageHome==null) {
				throw new Exception("Sage Home is not set!");
			}
			
			if (!sageHome.exists()) {
				throw new Exception("Sage Home does not exist: " + sageHome);
			}

			if (!(new File(sageHome, "JARs")).exists()) {
				throw new Exception("Doesn't Appear to be a valid Sage Home: " + sageHome);
			}

			if (!sageHome.canWrite()) {
				throw new Exception("You don't have permissions to update Sage Home: " + sageHome);
			}
			
			List<Plugin> toInstall = pm.resolvePlugin(plugin);
			
			pm.dumpPlugins(toInstall, out);
			out.msg("");
			pm.installPlugins(toInstall, sageHome, out);
			
			out.msg("");
			out.msg("Done");
		} catch (Throwable t) {
			out.msg(Throwables.getStackTraceAsString(t));
		}
	}
	
	public PluginManager getPluginManager() {
		return pm;
	}
}
