#!/bin/sh
# The superflat self-test (FlatSelfTest) in a fresh superflat world: points eclipse/server.properties at
# level-type FLAT with a preset, on a world of its own (eclipse/flatworld, made new each run), runs the flat group,
# then puts level-type, generator-settings and level-name back (the rest of the suite runs in a Default world).
#   sh tools/flat/selftest.sh [preset name | preset string]
# default:          3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village         (1.12's Classic Flat)
# decoration:       3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village,decoration
# ocean:            3;minecraft:bedrock,5*minecraft:stone,5*minecraft:sand,40*minecraft:water;0;
# ocean-decoration: 3;minecraft:bedrock,5*minecraft:stone,5*minecraft:sand,40*minecraft:water;0;decoration
# deep-decoration:  the same in a deep ocean (24), with decoration
# frozen:           3;minecraft:bedrock,5*minecraft:stone,5*minecraft:sand,30*minecraft:water,minecraft:ice;10;
#                   (a frozen ocean with ice on top, where chunk generation would otherwise spawn polar bears)
# Anything with a ';' in it is taken as the preset itself (for this mod's own oceans, whose ids the run logs).
set -e
root="$(cd "$(dirname "$0")/../.." && pwd)"
props="$root/eclipse/server.properties"
ocean='3;minecraft:bedrock,5*minecraft:stone,5*minecraft:sand,40*minecraft:water'
case "${1:-default}" in
    default) preset='3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village' ;;
    decoration) preset='3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village,decoration' ;;
    ocean) preset="$ocean;0;" ;;
    ocean-decoration) preset="$ocean;0;decoration" ;;
    deep-decoration) preset="$ocean;24;decoration" ;;
    frozen) preset='3;minecraft:bedrock,5*minecraft:stone,5*minecraft:sand,30*minecraft:water,minecraft:ice;10;' ;;
    *\;*) preset="$1" ;;
    *) echo "unknown preset $1" >&2; exit 2 ;;
esac
old_level="$(sed -n 's/^level-name=//p' "$props")"
# A literal path: the flat world is always eclipse/flatworld.
(cd "$root/eclipse" && rm -rf flatworld)
sed -i "s/^level-type=.*/level-type=FLAT/; s|^generator-settings=.*|generator-settings=$preset|; s/^level-name=.*/level-name=flatworld/" "$props"
status=0
(cd "$root" && ./gradlew runServer -Pselftest=flat -PdeployDir= -PcoreDeployDir=) || status=$?
sed -i "s/^level-type=.*/level-type=DEFAULT/; s|^generator-settings=.*|generator-settings=|; s/^level-name=.*/level-name=${old_level:-world}/" "$props"
grep -h "SELFTEST DONE" "$root/eclipse/logs/latest.log" 2>/dev/null | tail -1 || true
exit $status
