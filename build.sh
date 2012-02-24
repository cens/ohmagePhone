#!/bin/bash
# A script which will generate all the packages we currently want to release
set -e

# start with a clean
ant clean

##### Multi Campaign Version #####
# checkout cuttingedge
ant checkout-cuttingedge
# make test versions
ant ohmage-debug
ant ohmage-debug -Dconfig.server.url=https://dev.mobilizingcs.org/ -Dconfig.server.shortname=mdev
ant ohmage-debug -Dconfig.server.url=https://test.mobilizingcs.org/ -Dconfig.server.shortname=mtest
# make release versions
ant ohmage-release -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal

##### Mobilize Version #####
# checkout mobilize branch
ant checkout-mobilize
# make mobilize release
ant ohmage-release

##### NIH Version #####
# checkout NIH branch
ant checkout-NIH
# make test versions
ant ohmage-debug
ant ohmage-debug -Dconfig.server.url=https://dev.mobilizingcs.org/ -Dconfig.server.shortname=mdev
ant ohmage-debug -Dconfig.server.url=https://test.mobilizingcs.org/ -Dconfig.server.shortname=mtest
# make release versions
ant ohmage-release -Dconfig.server.url=https://pilots.ohmage.org/ -Dconfig.server.shortname=pilots -Dconfig.admin_mode=false
ant ohmage-release -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal

# tag the release
ant tag-release
# clean everything except the release apks
ant clean-for-release