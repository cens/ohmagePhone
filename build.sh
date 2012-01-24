#!/bin/bash
# A script which will generate all the packages we currently want to release

# first clean
ant clean

# make mdev multi
ant cuttingedge

# make internal multi release
ant cuttingedge-release -Dconfig.admin_mode=false -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal

# make mdev NIH
ant NIH

# make NIH release
ant NIH-release -Dconfig.server.url=https://pilots.ohmage.org/ -Dconfig.server.shortname=pilots

# make internal NIH release
ant NIH-release -Dconfig.server.url=https://internal.ohmage.org/ -Dconfig.server.shortname=internal

# tag the release
ant tag-release