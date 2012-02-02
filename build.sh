#!/bin/bash
# A script which will generate all the packages we currently want to release
set -e

# start with a clean
ant clean

# checkout cuttingedge
ant checkout-cuttingedge

# make mdev multi
ant ohmage-debug -Drelease.name=multi

# make internal multi release
ant ohmage-release -Drelease.name=multi -Dconfig.admin_mode=false -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal

# checkout NIH branch
ant checkout-NIH

# make mdev NIH
ant ohmage-debug -Drelease.name=NIH

# make NIH release
ant ohmage-release -Drelease.name=NIH -Dconfig.server.url=https://pilots.ohmage.org/ -Dconfig.server.shortname=pilots

# make internal NIH release
ant ohmage-release -Drelease.name=NIH -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal

# tag the release
ant tag-release

# clean everything except the release apks
ant clean-for-release