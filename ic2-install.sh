#!/bin/sh -x
# Install IC2 API as Maven artifact
export VER=1.115.222-lf
curl -O http://ic2api.player.to:8080/job/IC2_lf/222/artifact/packages/industrialcraft-2_$VER.jar
java -jar ../SpecialSource/target/SpecialSource-1.3-SNAPSHOT-shaded.jar --in-jar industrialcraft-2_$VER.jar --out-jar pkgmcp-industrialcraft-2_$VER.jar --srg-in ../MinecraftForge/mcp/conf/
mvn install:install-file -Dfile=pkgmcp-industrialcraft-2_$VER.jar -DgroupId=net.industrial-craft -DartifactId=ic2 -Dpackaging=jar -Dversion=$VER
