#!/bin/bash

VERSION=4.0.0
OS=solaris-x86
PKG=/u1/hudson/pkg-tools/bin/pkg
BUILD_TEMP=/u1/hudson/${OS}/trunk

case "$1" in

    'dev')
        REPOS=http://pkg.collab.net/dev/${OS}
        UPDATES=$REPOS
        TAR=CollabNetSubversionEdge-${VERSION}-dev_${OS}.tar
        ;;

    'stage')
        REPOS=http://pkg.collab.net/qa/${OS}/
        UPDATES=$REPOS
        TAR=CollabNetSubversionEdge-${VERSION}-RC_${OS}.tar
        ;;

    'release')
        REPOS=http://pkg.collab.net/qa/${OS}/
        UPDATES=http://pkg.collab.net/release/${OS}/
        TAR=CollabNetSubversionEdge-${VERSION}_${OS}.tar
        ;;

    *)
        echo "Usage: $0 { dev | stage | release }"
        exit 1
        ;;
esac

rm -Rf $BUILD_TEMP
mkdir -p $BUILD_TEMP
cd $BUILD_TEMP

# Start with a fresh folder
rm -Rf csvn
mkdir csvn

# Initialize the local image
$PKG image-create -U -a collab.net=$REPOS csvn
$PKG -R `pwd`/csvn set-property title "CollabNet Subversion Edge"
$PKG -R `pwd`/csvn set-property description "Package repository for CollabNet Subversion Edge."
$PKG -R `pwd`/csvn set-property send-uuid True
$PKG -R `pwd`/csvn set-authority -O $REPOS collab.net

# Install our application and required packages
$PKG -R `pwd`/csvn install pkg
$PKG -R `pwd`/csvn install csvn
$PKG -R `pwd`/csvn image-update

# Now prepare image for distribution
$PKG -R `pwd`/csvn set-authority -O $UPDATES collab.net
$PKG -R `pwd`/csvn rebuild-index
$PKG -R `pwd`/csvn purge-history

# Remove the UUID
cp csvn/.org.opensolaris,pkg/cfg_cache /tmp/cfg_cache
grep -v "^uuid =" /tmp/cfg_cache > csvn/.org.opensolaris,pkg/cfg_cache

# Remove the variant ARCH
cp csvn/.org.opensolaris,pkg/cfg_cache /tmp/cfg_cache
grep -v "variant.arch" /tmp/cfg_cache > csvn/.org.opensolaris,pkg/cfg_cache

# Cleanup content within the image
cd csvn
mv temp-data data
cd ".org.opensolaris,pkg"
rm -Rf download

# Build and upload the tarball
cd $BUILD_TEMP
rm $TAR
rm ${TAR}.gz
tar -cf $TAR csvn/
gzip $TAR
pbl.py upload -u markphip -k f04b2c60-2650-1378-80a8-4350af7da540 -l https://mgr.cloud.sp.collab.net/cubit_api/1 -p svnedge -t pub -r /Installers/solaris --force ${TAR}.gz