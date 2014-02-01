package sagex.phoenix.installer;

import sagex.phoenix.installer.Plugin.Package;

public interface IPackageResolver {
	public sagex.phoenix.installer.Plugin.Package resolvePackage(Package pkg); 
}
