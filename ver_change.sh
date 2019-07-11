#!/bin/bash
#Just simple script to change version in various file's
#
#usage:
##scripts ./ver.sh <version>


if [ $# -ne 1 ] 
then
  echo 1>&2 "Usage: $0 <new_ver>"
  exit 2
fi


# Files that are going to be changed
pom="pom.xml"
main="src/main/java/org/lz1aq/lzlog/MainWindow.java"
startup_cmd="src/main/scripts/startup.cmd"
startup_sh="src/main/scripts/startup.sh"

# Get the current version from the pom.xml file
function get_old_version()
{
	echo | grep -Po "(?<=version\>)(.*?)(?=\<\/version>)" $pom  -m 1
}

old_ver=$(get_old_version)
new_ver=$1


echo Old version was: $old_ver
echo New version is: $new_ver


sed -e "s/$old_ver/$new_ver/g" $pom > $pom.tmp                 && mv $pom.tmp $pom 
sed -e "s/$old_ver/$new_ver/g" $startup_cmd > $startup_cmd.tmp && mv $startup_cmd.tmp $startup_cmd
sed -e "s/$old_ver/$new_ver/g" $startup_sh > $startup_sh.tmp   && mv $startup_sh.tmp  $startup_sh
sed -e "s/$old_ver/$new_ver/g" $main > $main.tmp               && mv $main.tmp $main
