#!/bin/sh -x
set -e
pushd mcp
echo -------------------------------- Building OreDupeFix -----------------------------------
./recompile.sh
./reobfuscate.sh
echo
echo Adding release assets
cp mcmod.info reobf/minecraft/
echo
echo Archiving
pushd reobf/minecraft
zip -r ../../../oredupefix-snapshot.zip .
popd
popd
