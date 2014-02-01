package sagex.phoenix.installer;

import java.net.URL;

import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;

import sagex.phoenix.installer.Plugin.Dependency;
import sagex.phoenix.installer.Plugin.Package;

public class SageTVPluginModelImpl implements IPluginModel {
	private URL url = null;
	public SageTVPluginModelImpl(URL url) {
		this.url=url;
	}

	@Override
	public void loadPlugins(final PluginManager pluginManager) throws Exception {
		SAXReader reader =new SAXReader();
		reader.addHandler("/PluginRepository/SageTVPlugin", new ElementHandler() {
			@Override
			public void onStart(ElementPath arg0) {
			}
			
			@Override
			public void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				Plugin plugin = new Plugin(row.elementTextTrim("Identifier"), row.elementText("Version"));
				for (Element e: row.elements("Dependency")) {
					Dependency d = new Dependency(e.elementTextTrim("Plugin"), e.elementTextTrim("MinVersion"));
					if (d.plugin!=null) {
						plugin.dependencies.add(d);
					}
				}
				for (Element e: row.elements("Package")) {
					Package p = new Package(e.elementTextTrim("PackageType"), e.elementTextTrim("Location"), e.elementTextTrim("MD5"));
					plugin.packages.add(p);
				}
				// remove the row from the dom, since we have it
				row.detach();
				pluginManager.addPlugin(plugin);
			}
		});
		// read document
		reader.read(url);
	}
}
