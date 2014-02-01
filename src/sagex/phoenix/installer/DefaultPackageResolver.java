package sagex.phoenix.installer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;

import sagex.phoenix.installer.Plugin.Package;

import com.google.common.base.Strings;

public class DefaultPackageResolver implements IPackageResolver {
	public Map<String,sagex.phoenix.installer.Plugin.Package> urls = new HashMap<String, sagex.phoenix.installer.Plugin.Package>();
	
	public DefaultPackageResolver() throws Exception {
		// http://mvnrepository.com/artifact
		// http://mirrors.ibiblio.org/maven2/
		
		loadPackages(DefaultPackageResolver.class.getResource("packages.xml"));
	}
	
	public void loadPackages(URL url) throws Exception {
		SAXReader reader =new SAXReader();
		reader.addHandler("/packages/Package", new ElementHandler() {
			@Override
			public void onStart(ElementPath arg0) {
			}
			
			@Override
			public void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				Package p = new Package(value(row.elementTextTrim("PackageType"),"JAR"), row.elementTextTrim("Location"), value(row.elementTextTrim("MD5"),"--"));
				urls.put(row.attributeValue("url"), p);
				// remove the row from the dom, since we have it
				row.detach();
			}
		});
		// read document
		reader.read(url);
	}		
	
	private String value(String v, String d) {
		if (Strings.isNullOrEmpty(v)) return d;
		return v;
	}

	@Override
	public sagex.phoenix.installer.Plugin.Package resolvePackage(sagex.phoenix.installer.Plugin.Package pkg) {
		if (pkg==null) return null;
		sagex.phoenix.installer.Plugin.Package newUrl = urls.get(pkg.location);
		if (newUrl!=null) {
			return newUrl;
		}
		
		return pkg;
	}

}
