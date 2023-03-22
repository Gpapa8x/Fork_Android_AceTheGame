import ACE_release
import argparse

RELEASE_DIR = "./release"
if __name__ == "__main__":
    # ===================== making commands ========
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "android_toolchain_file",
        help="path to android's cmake toolchain file, usually come with android ndk",
    )
    args = parser.parse_args()
    android_toolchain_file = args.android_toolchain_file
    # ==============================================
    ACE_release.make_release(
        release_dir=RELEASE_DIR,
        android_toolchain_file=android_toolchain_file,
    )
