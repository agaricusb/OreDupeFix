#!/bin/sh
echo -------------------------------- Building OreDupeFix -----------------------------------
echo Backing up src
cp -R src src-bak 
echo
echo Copying source
cp -R oredupefix/src src/minecraft
echo
./recompile.sh
./reobfuscate.sh
echo
echo Adding release assets
cp oredupefix/*.txt reobf/minecraft/
cp oredupefix/mcmod.info reobf/minecraft/
echo
echo Restoring src-bak
rm -rf src
mv src-bak src
echo
echo Archiving
pushd reobf/minecraft
zip -r ../../oredupefix-snapshot.zip .
popd
