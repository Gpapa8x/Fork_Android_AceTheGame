package modder;

import java.io.File;

public class Injector {

	public static String GetSmaliSubPathFromLaunchableActivity(String launchableActivity) {

		// replace the '.' in launchableActivity class
		// to a near complete path
		String subPath = launchableActivity.replace(".", File.separator);
		// don't forget the file extension
		subPath += ".smali";
		return subPath;
	}

	public static String GetSmaliPathFromLaunchableActivity

	(String launchableActivity, String decompiledApkDirStr) {

		String relativeSmaliFilePath = GetSmaliSubPathFromLaunchableActivity(launchableActivity);

		// when decompiling with apktool
		// the smali classes in subPath will be contained in
		// the folder starting with smali
		// like smali, smali_classes2, smali_classes3 and ect
		File decompiledApkDir = new File(decompiledApkDirStr);
		File[] files = decompiledApkDir.listFiles();

		for (int i = 0; i < files.length; i++) {

			if (!files[i].isDirectory())
				continue;

			String dirName = files[i].getName();

			if (!dirName.startsWith("smali"))
				continue;

			String basePathStr = files[i].getAbsolutePath();
			File smaliFile = new File(basePathStr, relativeSmaliFilePath);
			// check if this thing actually exist
			if (smaliFile.exists())
				return smaliFile.getAbsolutePath();

		}

		return "";
	}

}
