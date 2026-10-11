"""Behavior checks for distributable archives and ADB transactions."""
import hashlib
import importlib.util
import json
import tempfile
import subprocess
import unittest
import zipfile
from pathlib import Path

def load(name):
    path=Path(__file__).with_name(name+".py")
    spec=importlib.util.spec_from_file_location(name,path)
    mod=importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

class PackagingTest(unittest.TestCase):
    def test_archives_exclude_keys_caches_and_recursive_release(self):
        module=load("package_release")
        for path in [".git/config",".gradle/cache","app/build/a.apk","release/old.zip","secret.jks",".env",".worktrees/test/x","local.properties"]:
            self.assertFalse(module.source_allowed(path),path)
        self.assertTrue(module.source_allowed("app/src/main/AndroidManifest.xml"))
        self.assertTrue(module.source_allowed("packaging/magisk/assets/silent.ogg"))

    def test_archive_is_valid_and_reproducible(self):
        module=load("package_release")
        with tempfile.TemporaryDirectory() as tmp:
            one=Path(tmp)/"one.zip";two=Path(tmp)/"two.zip"
            entries={"README.md":b"guide","app.apk":b"apk"}
            module.write_zip(one,entries);module.write_zip(two,entries)
            self.assertEqual(one.read_bytes(),two.read_bytes())
            with zipfile.ZipFile(one) as z:
                self.assertIsNone(z.testzip());self.assertEqual(z.read("README.md"),b"guide")
            self.assertEqual(module.sha256(one),hashlib.sha256(one.read_bytes()).hexdigest())

class AdbTest(unittest.TestCase):
    def test_corrupt_backup_cannot_be_interpreted_as_an_absent_setting(self):
        module=load("adb_control")
        with tempfile.TemporaryDirectory() as tmp:
            backup=Path(tmp)/"backup.json";backup.write_text(json.dumps({"device":"device","user":0}))
            writes=[]
            with self.assertRaisesRegex(RuntimeError,"Invalid original"):
                module.csc_restore(lambda args:writes.append(args) or "0",0,backup,"device")
            self.assertEqual(writes,[])

    def test_failed_read_does_not_write_or_save_a_fake_original(self):
        module=load("adb_control")
        with tempfile.TemporaryDirectory() as tmp:
            writes=[]
            def run(args):
                if "get" in args: raise RuntimeError("denied")
                writes.append(args);return ""
            with self.assertRaisesRegex(RuntimeError,"denied"):module.csc_apply(run,10,Path(tmp)/"backup.json","device")
            self.assertEqual(writes,[]);self.assertFalse((Path(tmp)/"backup.json").exists())

    def test_missing_setting_restores_by_delete_and_reapply_keeps_original(self):
        module=load("adb_control")
        with tempfile.TemporaryDirectory() as tmp:
            state={"value":"null"};commands=[]
            def run(args):
                commands.append(args)
                if "get" in args:return state["value"]
                if "put" in args:state["value"]=args[-1]
                if "delete" in args:state["value"]="null"
                return ""
            backup=Path(tmp)/"backup.json"
            module.csc_apply(run,10,backup,"device");module.csc_apply(run,10,backup,"device")
            self.assertIsNone(json.loads(backup.read_text())["value"])
            module.csc_restore(run,10,backup,"device")
            self.assertEqual(state["value"],"null")
            self.assertTrue(any(c==["settings","--user","10","delete","system",module.CSC_KEY] for c in commands))

    def test_restore_rejects_another_device_or_user(self):
        module=load("adb_control")
        with tempfile.TemporaryDirectory() as tmp:
            backup=Path(tmp)/"backup.json";backup.write_text(json.dumps({"device":"other","user":0,"value":"1"}))
            with self.assertRaisesRegex(RuntimeError,"does not match"):module.csc_restore(lambda args:"0",10,backup,"device")

class ModuleTest(unittest.TestCase):
    def test_only_shutter_audio_in_known_partitions_is_overlaid(self):
        policy=Path(__file__).resolve().parent.parent/"packaging/magisk/path-policy.sh"
        cases={"/system/media/audio/ui/camera_click.ogg":"system/media/audio/ui/camera_click.ogg",
               "/product/media/audio/ui/Shutter.ogg":"system/product/media/audio/ui/Shutter.ogg",
               "/system/vendor/media/audio/ui/shutter.ogg":"system/vendor/media/audio/ui/shutter.ogg",
               "/vendor/media/audio/ui/camera_shutter.ogg":"system/vendor/media/audio/ui/camera_shutter.ogg"}
        for path,expected in cases.items():
            r=subprocess.run(["sh","-c",'. "$1"; overlay_path "$2"',"test",str(policy),path],capture_output=True,text=True)
            self.assertEqual(r.returncode,0,r.stderr);self.assertEqual(r.stdout.strip(),expected)
        for path in ["/system/media/audio/ui/Lock.ogg","/data/app/camera_click.ogg","/vendor/media/audio/ui/alarm.ogg","/system/media/audio/ui/../camera_click.ogg","/system/media/audio/ui/camera_click.wav"]:
            r=subprocess.run(["sh","-c",'. "$1"; overlay_path "$2"',"test",str(policy),path],capture_output=True,text=True)
            self.assertNotEqual(r.returncode,0,path)

if __name__=="__main__":unittest.main()
