"""Offline rendering against installed OpenAL; never opens a hardware output device."""
import argparse
import ctypes as c
import math
import platform
import tempfile
from pathlib import Path
from zipfile import ZipFile


def verify(native):
    with tempfile.TemporaryDirectory(prefix="mine-strike-audio-") as directory:
        with ZipFile(native) as jar:
            name = next(n for n in jar.namelist() if n.endswith("libopenal.dylib"))
            path = Path(directory) / "libopenal.dylib"
            path.write_bytes(jar.read(name))
        library = c.CDLL(str(path))

        def bind(name, result, args):
            function = getattr(library, name)
            function.restype, function.argtypes = result, args
            return function

        address = bind("alcGetProcAddress", c.c_void_p, [c.c_void_p, c.c_char_p])

        def extension(name, result, args):
            pointer = address(None, name.encode())
            assert pointer, name
            return c.CFUNCTYPE(result, *args)(pointer)

        device = extension("alcLoopbackOpenDeviceSOFT", c.c_void_p, [c.c_char_p])(None)
        assert device
        attributes = (c.c_int * 7)(0x1990, 0x1501, 0x1991, 0x1406, 0x1007, 48000, 0)
        context = bind("alcCreateContext", c.c_void_p, [c.c_void_p, c.POINTER(c.c_int)])(device, attributes)
        assert context
        current = bind("alcMakeContextCurrent", c.c_bool, [c.c_void_p])
        current(context)
        try:
            assert bind("alIsExtensionPresent", c.c_bool, [c.c_char_p])(b"AL_SOFT_source_spatialize")
            bind("alEnable", None, [c.c_int])(0x200)
            render = extension("alcRenderSamplesSOFT", None, [c.c_void_p, c.c_void_p, c.c_int])
            sourcei = bind("alSourcei", None, [c.c_uint, c.c_int, c.c_int])
            sourcef = bind("alSourcef", None, [c.c_uint, c.c_int, c.c_float])
            source3 = bind("alSource3f", None, [c.c_uint, c.c_int, c.c_float, c.c_float, c.c_float])
            listener = bind("alListenerfv", None, [c.c_int, c.POINTER(c.c_float)])
            play = bind("alSourcePlay", None, [c.c_uint])
            stop = bind("alSourceStop", None, [c.c_uint])
            error = bind("alGetError", c.c_int, [])
            buffer, source = c.c_uint(), c.c_uint()
            bind("alGenBuffers", None, [c.c_int, c.POINTER(c.c_uint)])(1, c.byref(buffer))
            bind("alGenSources", None, [c.c_int, c.POINTER(c.c_uint)])(1, c.byref(source))
            pcm = (c.c_short * 96000)(*[int(12000 * math.sin(2 * math.pi * 700 * (i // 2) / 48000)) for i in range(96000)])
            bind("alBufferData", None, [c.c_uint, c.c_int, c.c_void_p, c.c_int, c.c_int])(
                buffer, 0x1103, pcm, c.sizeof(pcm), 48000)
            sourcei(source, 0x1009, buffer.value)

            def rms(spatial, facing, distance, maximum):
                stop(source)
                sourcei(source, 0x1025, 0)
                sourcei(source, 0x1214, spatial)
                sourcei(source, 0xD000, 0xD003)
                sourcef(source, 0x1023, maximum)
                sourcef(source, 0x1021, 1)
                sourcef(source, 0x1020, 0)
                source3(source, 0x1004, -distance, 0, 0)
                listener(0x100F, (c.c_float * 6)(0, 0, facing, 0, 1, 0))
                play(source)
                output = (c.c_float * 48000)()
                render(device, output, 24000)
                values = [math.sqrt(sum(output[i] ** 2 for i in range(4800 + j, 48000, 2)) / 21600) for j in [0, 1]]
                assert error() == 0
                return values

            native_stereo = rms(2, -1, 8, 32)
            assert abs(native_stereo[0] - native_stereo[1]) < .0001
            left, right = rms(1, -1, 8, 32), rms(1, 1, 8, 32)
            assert left[0] > .001 and left[1] < left[0] * .05
            assert right[1] > .001 and right[0] < right[1] * .05
            outside_native = rms(1, -1, 24, 19.2)
            extended = rms(1, -1, 24, 32)
            assert max(outside_native) < .0001 and extended[0] > .001
            restored = rms(2, -1, 8, 32)
            assert abs(restored[0] - restored[1]) < .0001
            print(f"PASS: {native.name}: stereo spatialization, 180-degree rotation, 24/32-block falloff, reset to native")
        finally:
            current(None)
            bind("alcDestroyContext", None, [c.c_void_p])(context)
            bind("alcCloseDevice", c.c_bool, [c.c_void_p])(device)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--libraries", type=Path, required=True)
    args = parser.parse_args()
    if platform.system() != "Darwin":
        print("SKIP: offline native audio renderer currently supports macOS only")
    else:
        architecture = "arm64" if platform.machine() == "arm64" else ""
        suffix = "-arm64" if architecture else ""
        natives = sorted((args.libraries / "org/lwjgl/lwjgl-openal").glob(f"*/lwjgl-openal-*-natives-macos{suffix}.jar"))
        assert natives, "No installed native OpenAL libraries"
        for native in natives:
            verify(native)
