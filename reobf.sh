#!/bin/sh -x
# Reobfuscate compiled mod pkgmcp->obf mappings
# TODO: move to Maven
java -jar ../SpecialSource/target/SpecialSource-1.3-SNAPSHOT-shaded.jar --in-jar target/OreDupeFix-2.0.jar --out-jar ../test-server/mods/oredupefix-2.0.jar --reverse --srg-in ../MinecraftForge/mcp/conf/
