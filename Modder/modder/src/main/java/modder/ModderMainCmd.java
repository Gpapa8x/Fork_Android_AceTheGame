package modder;

import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.List;

@CommandLine.Command(name = "Modder", subcommands = {
        CommandLine.HelpCommand.class}, description = "Utilities for hacking android apk")
public class ModderMainCmd {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    final String DECOMPILED_DIR_EXT = ".decompiled";
    final String RECOMPILED_DIR_EXT = ".recompiled";

    void ShowAdbShellError(Adb.Output out) {
        System.out.println("can't connect to adb shell:");
        out.strings.forEach(s -> System.out.println(s));

    }

    @CommandLine.Command(name = "listApk", description = "List installed apks")
    void ListApk() {

        Adb adb = new Adb();
        Adb.Output out = adb.ListApk();
        if (out.error != Adb.Error.ok) {
            ShowAdbShellError(out);
            return;
        }
        // output will look like
        // package:com.android.offfice
        // package:com.vivo.appstore
        // "package:" should be trimmed for better view
        for (int i = 0; i < out.strings.size(); i++) {
            // use the caret symbol '^'
            // to match the beggining of the pattern
            String new_str = out.strings.get(i).replaceFirst("^package:", "");
            System.out.printf("%d %s\n", i, new_str);
        }
        System.out.printf("Found %d packages\n", out.strings.size());

    }

    @CommandLine.Command(name = "apkInfo", description = "information about apk")
    void ApkInfo(

            @CommandLine.Parameters(paramLabel = "apkPath", description = "path to apk")

            String apkPathStr

    ) throws IOException {

        List<String> output = Aapt.DumpBadging(apkPathStr);

        output.forEach(System.out::println);

    }

    /*
     *
     * for decompilation and recompilation output directory
     * we have to pass the path to a File object first
     * and then use toString, to make sure the path doesn't contain '/'
     *
     * if the output Folder from user contains '/'
     * then the output will not be put in the same directory as
     * apk folder or decompiled apk folder because
     *
     * ussualy an output for decompilation and recompilation are
     * "[apkDir]+ApkMod.DECOMPILED_DIR_EXT" or
     * "[decompiledApkDir]+ApkMod.RECOMPILED_DIR_EXT"
     * so the output will be put inside [apkDir] or [decompiledApkDir]
     * as .decompiled or .recompiled
     *
     */
    @CommandLine.Command(name = "decompile", description = "Decompile an apk")
    void Decompile(

            @CommandLine.Parameters(paramLabel = "ApkFilePath", description = "Path to apk file or a directory containing apks")

            String apkPathStr

    ) {
        File apkDir = new File(apkPathStr);
        ApkMod.Decompile(apkPathStr, apkDir.toString() + ApkMod.DECOMPILED_DIR_EXT);
    }

    @CommandLine.Command(name = "recompile", description = "recompile apks")
    void Recompile(

            @CommandLine.Parameters(paramLabel = "decompiledFolder", description = "Folder to decompiled apks")

            String decompiledFolderStr

    ) {

        File decompiledApkDir = new File(decompiledFolderStr);

        ApkMod.Recompile(decompiledFolderStr, decompiledApkDir.toString() + ApkMod.RECOMPILED_DIR_EXT);
    }

    @CommandLine.Command(name = "Patch", description = "recompile apks")
    void Patch(
            @CommandLine.Parameters(paramLabel = "ApkFolderPath", description = "Path to directory containing apks")
            String apkDirStr

    ) throws IOException {

        // check if the directory exist
        File apkSrcDir = new File(apkDirStr);
        Assert.AssertExistAndIsDirectory(apkSrcDir);
        // copy apk folder so we don't write to the original one
        File patchedApkDir = new File(apkSrcDir.getAbsolutePath() + ".patched");
        org.apache.commons.io.FileUtils.copyDirectory(apkSrcDir, patchedApkDir);
        // get the base apk for patching
        File baseApkFile = new File(patchedApkDir.getAbsolutePath(), Patcher.BASE_APK_FILE_NAME);
        Assert.AssertExistAndIsFile(baseApkFile);
        // ========== add patch ===========================
        Patcher patcher = new Patcher(baseApkFile.getAbsolutePath());
        patcher.AddMemScanner();
        // fix [INSTALL_FAILED_INVALID_APK: Failed to extract native libraries, res=-2] after recompile
        // https://github.com/iBotPeaches/Apktool/issues/1626
        patcher.RemoveExtractNativeLibOptions();
        // ================== export ===================
        // String patchedApkPath = baseApkFile.getAbsolutePath() + "-patched.apk";
        String patchedApkPath = baseApkFile.getAbsolutePath();
        patcher.Export(patchedApkPath);

        // ============ sign all the apk in the directory ==========
        File[] files = patchedApkDir.listFiles();
        for (File f : files) {
            if (f.isFile())
                ApkSigner.Sign(f);
        }

        System.out.printf("exported apk to %s\n", patchedApkPath);

    }

    /*
     * Download apk from device specified by [package_name]
     * and put it in a folder with the same name as [package_name]
     */
    @CommandLine.Command(name = "download", description = "Download an apk from device")
    void Download(

            @CommandLine.Parameters(paramLabel = "packageName", description = "Package to download") String package_name

    ) {
        String downloadDir = package_name;
        // if folder with name [package_name] exist
        // then remove it and recreate an empty one
        File downloadFile = new File(downloadDir);
        if (downloadFile.exists() && downloadFile.isDirectory()) {
            try {
                System.out.printf("directory %s exist, removing it...\n", downloadDir);
                FileUtils.deleteDirectory(downloadFile);
            } catch (IOException e) {
                System.out.printf("Error while deleting directory \n");
                System.out.println(e.getMessage());
            }
        }
        // create dir for storing downloaded apk
        (new File(downloadDir)).mkdirs();
        System.out.printf("created directory %s for storing downloaded apk\n", downloadDir);

        //
        Adb adb = new Adb();
        // check if [package_name] exists
        Adb.Output out = adb.ListApk();
        if (out.error != Adb.Error.ok) {
            ShowAdbShellError(out);
            return;
        }

        if (!out.strings.contains(package_name)) {
            System.out.printf("package %s doesn't exist in the device\n", package_name);
            System.out.println("use listApk command to list installed packages");
            return;

        }
        out = adb.GetApkPathAtDevice(package_name);
        if (out.error != Adb.Error.ok) {
            ShowAdbShellError(out);
            return;
        }
        // we need to loop when downloading the app
        // in case the apk is splitted apks (have multiple paths)
        System.out.println("Downloading apks ...");
        for (int i = 0; i < out.strings.size(); i++) {

            String apkPath = out.strings.get(i);
            System.out.printf("Downloading apk (%d/%d) at %s", i + 1, out.strings.size(), apkPath);
            Adb.Output downloadOut = adb.DownloadApk(apkPath, downloadDir);
            if (downloadOut.error != Adb.Error.ok) {
                ShowAdbShellError(out);
                return;
            }
            downloadOut.strings.forEach(System.out::println);
            System.out.printf("...done\n");
        }

    }

    /*
     * Download apk from device specified by [package_name]
     * and put it in a folder with the same name as [package_name]
     */
    @CommandLine.Command(name = "install", description = "install all apk in a folder")
    void Install(

            @CommandLine.Parameters(paramLabel = "apkDir", description = "Directory that contains apk") String apkDirStr

    ) throws IOException {

        Adb adb = new Adb();
        Adb.Output out = adb.InstallApk(apkDirStr);
        out.strings.forEach(System.out::println);

    }
}
