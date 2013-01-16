#!/bin/sh -x
set -e
pushd mcp
echo -------------------------------- Building OreDupeFix -----------------------------------
./recompile.sh
./reobfuscate.sh
echo
echo Adding release assets
cp ../mcmod.info reobf/minecraft/
echo
echo Removing API
rm -rf reobf/minecraft/ic2
rm -rf reobf/minecraft/thermalexpansion
echo
echo Archiving
pushd reobf/minecraft
zip -r ../../../oredupefix-snapshot.zip .
popd
popd
cp oredupefix-snapshot.zip /Applications/MultiMC.app/Contents/Resources/instances/ModTest/minecraft/mods/
