#!/bin/bash
# A script which will generate all the packages we currently want to release
set -e

all() {
	##### Multi Campaign Version #####
	# checkout cuttingedge
	ant checkout-cuttingedge
	# make test versions
	ant ohmage-debug -Dconfig.server.url=https://dev.mobilizingcs.org/ -Dconfig.server.shortname=mdev
	ant ohmage-debug -Dconfig.server.url=https://dev.andwellness.org/ -Dconfig.server.shortname=adev
	# make release versions
	ant ohmage-release -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal

	##### NIH Version #####
	# checkout NIH branch
	ant checkout-NIH
	# make test versions
	ant ohmage-debug -Dconfig.server.url=https://dev.mobilizingcs.org/ -Dconfig.server.shortname=mdev
	ant ohmage-debug -Dconfig.server.url=https://dev.andwellness.org/ -Dconfig.server.shortname=adev
	# make release versions
	ant ohmage-release -Dconfig.server.url=https://pilots.ohmage.org/ -Dconfig.server.shortname=pilots -Dconfig.admin_mode=false
	ant ohmage-release -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal
}

mdev() {
	ant ohmage-debug -Dconfig.server.url=https://dev.mobilizingcs.org/ -Dconfig.server.shortname=mdev
}

adev() {
	ant ohmage-debug -Dconfig.server.url=https://dev.andwellness.org/ -Dconfig.server.shortname=adev
}

internal() {
	ant ohmage-release -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal
}

NIH() {
	ant ohmage-release -Dconfig.server.url=https://pilots.ohmage.org/ -Dconfig.server.shortname=pilots -Dconfig.admin_mode=false
}

# start with a clean
ant clean

for thing in "$@"
do
    case "$thing" in
	     mdev)
	           mdev
	           ;;
	     adev)
	           adev
	           ;;
	     internal)
	           internal
	           ;;
	     NIH)
	           NIH
	           ;;
	     all)
	           all
	           ;;
	esac
done

# clean everything except the release apks
ant clean-for-release

# push the apks
ant ohmage-push