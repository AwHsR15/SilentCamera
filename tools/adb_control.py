"""Portable, device-scoped ADB companion. Requires Python 3.10+ and official platform-tools."""
import argparse
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path

CSC_KEY="csc_pref_camera_forced_shuttersound_key"
def read_value(run,user):
    value=run(["settings","--user",str(user),"get","system",CSC_KEY]).strip()
    if value=="null":return None
    if value not in {"0","1"}:raise RuntimeError("Unrecognized CSC read: "+repr(value))
    return value

def original(backup,user,device):
    data=json.loads(backup.read_text(encoding="utf-8"))
    if data.get("device")!=device or data.get("user")!=user:raise RuntimeError("Backup does not match this device or Android user")
    if "value" not in data or data["value"] not in (None,"0","1"):raise RuntimeError("Invalid original value")
    return data

def save_original(backup,user,device,value):
    backup.parent.mkdir(parents=True,exist_ok=True)
    temp=backup.with_suffix(".tmp")
    temp.write_text(json.dumps({"device":device,"user":user,"value":value},indent=2),encoding="utf-8")
    os.replace(temp,backup)

def set_value(run,user,value):
    args=["settings","--user",str(user),"delete" if value is None else "put","system",CSC_KEY]
    if value is not None:args.append(value)
    run(args)
    if read_value(run,user)!=value:raise RuntimeError("Read-back does not match requested configuration")

def csc_apply(run,user,backup,device):
    before=read_value(run,user)
    if backup.exists():original(backup,user,device)
    else:save_original(backup,user,device,before)
    try:set_value(run,user,"0")
    except Exception as failure:
        try:set_value(run,user,before)
        except Exception as rollback:raise RuntimeError(f"Apply failed: {failure}; rollback failed: {rollback}; backup retained") from failure
        raise RuntimeError(f"Apply failed: {failure}; previous state restored") from failure

def csc_restore(run,user,backup,device):
    if not backup.exists():raise RuntimeError("No saved original; no default value will be guessed")
    state=original(backup,user,device);before=read_value(run,user)
    try:set_value(run,user,state["value"])
    except Exception as failure:
        try:set_value(run,user,before)
        except Exception as rollback:raise RuntimeError(f"Restore failed: {failure}; rollback failed: {rollback}; backup retained") from failure
        raise RuntimeError(f"Restore failed: {failure}; backup retained") from failure
    backup.unlink()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action",choices=["detect","apply-csc","restore-csc","install"])
    parser.add_argument("--serial",help="ADB device serial; required when multiple devices are connected")
    parser.add_argument("--user",type=int,help="Android user; defaults to the current foreground user")
    parser.add_argument("--backup",type=Path,default=Path("silentcamera-original.json"))
    parser.add_argument("--apk",type=Path,default=Path(__file__).resolve().parent.parent/"SilentCamera-v2.0-preview.apk")
    args=parser.parse_args()
    devices=subprocess.run(["adb","devices"],capture_output=True,text=True,check=True,timeout=15).stdout
    online=[line.split()[0] for line in devices.splitlines()[1:] if len(line.split())==2 and line.split()[1]=="device"]
    if args.serial:
        if args.serial not in online:raise RuntimeError("Selected device is not connected and authorized")
        serial=args.serial
    elif len(online)==1:serial=online[0]
    else:raise RuntimeError("Connect one authorized device, or specify --serial")
    prefix=["adb","-s",serial]
    def run(command):
        result=subprocess.run(prefix+["shell",*command],capture_output=True,text=True,timeout=15)
        if result.returncode:raise RuntimeError(result.stderr.strip() or f"ADB command failed: {result.returncode}")
        return result.stdout.strip()
    user=args.user if args.user is not None else int(run(["cmd","activity","get-current-user"]))
    if user<0:raise RuntimeError("Invalid Android user")
    fingerprint=run(["getprop","ro.build.fingerprint"])
    device=hashlib.sha256((serial+":"+fingerprint).encode()).hexdigest()
    manufacturer=run(["getprop","ro.product.manufacturer"])
    if args.action=="install":
        if not args.apk.is_file():raise RuntimeError("Preview APK not found; specify --apk")
        subprocess.run(prefix+["install","-r",str(args.apk)],check=True,timeout=120);return
    if args.action=="detect":
        print(json.dumps({"manufacturer":manufacturer,"model":run(["getprop","ro.product.model"]),"android":run(["getprop","ro.build.version.release"]),"user":user,"csc_supported":manufacturer.lower()=="samsung"},indent=2,ensure_ascii=False))
        print("Use the app for dynamic volume-group mapping, advanced modes and alternate photography.");return
    if manufacturer.lower()!="samsung":raise RuntimeError("CSC mode only applies to Samsung firmware; use the app for other modes")
    if args.action=="apply-csc":csc_apply(run,user,args.backup,device);print("Configuration applied and read back. Verify actual sound with a photo.")
    else:csc_restore(run,user,args.backup,device);print("Original setting restored and read back.")

if __name__=="__main__":
    try:main()
    except (OSError,ValueError,RuntimeError,subprocess.SubprocessError) as error:print(str(error),file=sys.stderr);sys.exit(1)
