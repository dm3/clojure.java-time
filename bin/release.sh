#!/bin/sh
# Releases the current version in project.clj and bump to the next :minor version.
# Updates changelog and readme to released version.
# Does not push anything if deployment fails.
# If deployment fails, reset to origin/master and remove the latest tag to reset local repo.
lein release :minor
