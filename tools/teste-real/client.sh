#!/bin/bash
# client.sh <gameDir> <username> [extra jvm args...]: runs the TEOF client headless.
GD="$1"; USER_NAME="$2"; shift 2
T=/home/user/teof
J8=/root/.gradle/jdks/temurin-8-amd64-linux.2/bin/java
CP="$GD/bin/modpack.jar:$(cat $T/libs/classpath.txt):$GD/bin/minecraft.jar"
cd "$GD"
export LIBGL_ALWAYS_SOFTWARE=1 LANG=C.UTF-8
exec xvfb-run -a -s "-screen 0 1280x720x24 +extension RANDR" $J8 -Xms2G -Xmx${XMX:-5G} -XX:+UseG1GC \
  -Djava.library.path=/home/user/RyoikiTenkai/run/natives/lwjgl2 -Dfml.ignoreInvalidMinecraftCertificates=true "$@" \
  -cp "$CP" net.minecraft.launchwrapper.Launch --username "$USER_NAME" --version 1.7.10 --gameDir "$GD" \
  --assetsDir /root/.gradle/caches/retro_futura_gradle/assets --assetIndex 1.7.10 \
  --uuid 00000000-0000-0000-0000-00000000000${USER_NAME: -1} --accessToken 0 --userProperties '{}' --userType legacy \
  --tweakClass cpw.mods.fml.common.launcher.FMLTweaker
