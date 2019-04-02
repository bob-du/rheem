#!/usr/bin/env bash

. ./../base/restart.sh all
NAME_CONF_RHEEM="rheem.properties"
. ./base_sgd.sh spark "0100" 1

. ./../base/restart.sh all
NAME_CONF_RHEEM="rheem.properties"
. ./base_sgd.sh spark "0100" 2

. ./../base/restart.sh all
NAME_CONF_RHEEM="rheem.properties"
. ./base_sgd.sh spark "0100" 3

. ./../base/restart.sh all
NAME_CONF_RHEEM="rheem.properties"
. ./base_sgd.sh java "0100" 1

. ./../base/restart.sh all
NAME_CONF_RHEEM="rheem.properties"
. ./base_sgd.sh java "0100" 2

. ./../base/restart.sh all
NAME_CONF_RHEEM="rheem.properties"
. ./base_sgd.sh java "0100" 3