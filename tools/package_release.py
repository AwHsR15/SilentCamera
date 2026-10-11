"""Build deterministic distribution/source archives from a committed source tree."""
import argparse
import hashlib
import json
import subprocess
import zipfile
from pathlib import Path, PurePosixPath

VERSION="2.0"
def source_allowed(path:str)->bool:
    parts=PurePosixPath(path).parts
    if any(part in {".git",".gradle",".worktrees",".superpowers","__pycache__","build","release"} for part in parts):return False
    name=PurePosixPath(path).name
    if name in {"local.properties",".env"} or name.startswith(".env."):return False
    return PurePosixPath(path).suffix.lower() not in {".jks",".keystore",".p12",".pem",".key",".log",".pyc"}

def write_zip(path:Path,entries:dict[str,bytes])->None:
    path.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(path,"w",zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
        for name,content in sorted(entries.items()):
            if name.startswith("/") or ".." in PurePosixPath(name).parts:raise ValueError("Unsafe archive path")
            info=zipfile.ZipInfo(name,(2026,1,1,0,0,0))
            info.compress_type=zipfile.ZIP_DEFLATED
            executable=name.endswith(".sh") or PurePosixPath(name).name in {"gradlew","update-binary"}
            info.external_attr=(0o100755 if executable else 0o100644)<<16
            archive.writestr(info,content,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)

def sha256(path:Path)->str:
    digest=hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda:stream.read(1024*1024),b""):digest.update(block)
    return digest.hexdigest()

def git(root:Path,*args:str)->bytes:
    return subprocess.check_output(["git","-C",str(root),*args])

def package(root:Path,apk:Path,out:Path)->dict:
    source_commit=git(root,"rev-parse","HEAD").decode().strip()
    changes=git(root,"status","--porcelain","--untracked-files=all").decode().splitlines()
    if any(source_allowed(line[3:]) for line in changes):raise RuntimeError("Commit all source changes before packaging")
    if not apk.is_file():raise RuntimeError("Build the preview APK first")
    with zipfile.ZipFile(apk) as z:
        if z.testzip() is not None or "AndroidManifest.xml" not in z.namelist():raise RuntimeError("Invalid APK")
        if not any(source_commit.encode() in z.read(name) for name in z.namelist() if name.endswith(".dex")):
            raise RuntimeError("APK does not contain the current source commit; use tools/build-release.sh")
    files=[p for p in git(root,"ls-tree","-r","--name-only",source_commit).decode().splitlines() if source_allowed(p)]
    source={p:git(root,"show",f"{source_commit}:{p}") for p in files}
    source["SOURCE_COMMIT.txt"]=(source_commit+"\n").encode()
    out.mkdir(parents=True,exist_ok=True)
    apk_name=f"SilentCamera-v{VERSION}-preview.apk"; (out/apk_name).write_bytes(apk.read_bytes())
    module_name=f"SilentCamera-v{VERSION}-magisk.zip"
    module={p.removeprefix("packaging/magisk/"):data for p,data in source.items() if p.startswith("packaging/magisk/")}
    if not module or "assets/silent.ogg" not in module:raise RuntimeError("Module is incomplete")
    write_zip(out/module_name,module)
    source_name=f"SilentCamera-v{VERSION}-source.zip";write_zip(out/source_name,source)
    artifact_names=[apk_name,module_name,source_name]
    manifest={"version":VERSION,"channel":"preview","source_commit":source_commit,
        "application_id":"com.marvis.silentcamera.preview","min_sdk":26,"target_sdk":35,
        "device_verification":"Emulator checks are documented separately; Japanese OEM hardware unverified.",
        "artifacts":{name:{"sha256":sha256(out/name),"bytes":(out/name).stat().st_size} for name in artifact_names}}
    manifest_text=(json.dumps(manifest,indent=2,ensure_ascii=False)+"\n").encode()
    (out/"manifest.json").write_bytes(manifest_text)
    delivery={apk_name:(out/apk_name).read_bytes(),module_name:(out/module_name).read_bytes(),"manifest.json":manifest_text}
    for p,data in source.items():
        if p.startswith("tools/adb") or p in {"LICENSE","README.md","README.ja.md","README.en.md","docs/MAGISK.md","docs/COMPATIBILITY.md","docs/RELEASE.md"}:delivery[p]=data
    delivery["SHA256SUMS"]="".join(f"{sha256(out/n)}  {n}\n" for n in artifact_names[:2]).encode()
    bundle_name=f"SilentCamera-v{VERSION}-bundle.zip";write_zip(out/bundle_name,delivery)
    artifact_names.append(bundle_name)
    (out/"SHA256SUMS").write_text("".join(f"{sha256(out/n)}  {n}\n" for n in artifact_names)+f"{sha256(out/'manifest.json')}  manifest.json\n")
    return manifest

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk",type=Path,default=Path("app/build/outputs/apk/debug/app-debug.apk"))
    parser.add_argument("--out",type=Path,default=Path("release"))
    args=parser.parse_args();root=Path(__file__).resolve().parent.parent
    manifest=package(root,args.apk.resolve(),args.out.resolve());print(json.dumps(manifest,indent=2))
if __name__=="__main__":main()
