#!/usr/bin/env bash
SCDIR=$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")
SCDIR=$(realpath $SCDIR)
ROOT_DIR=$(realpath $SCDIR/..)

if [ "$1" != "" ]; then
    VER=$1
else
    VER=2.9.0-SNAPSHOT
fi

function download_deps() {
    DEP=$1
    TARGET=$2
    echo "Downloading $DEP"
    set +e
    SNAPSHOT=$(echo "$DEP" | grep -c "\-SNAPSHOT")
    MILESTONE=$(echo "$DEP" | grep -c "\-M")
    if ((SNAPSHOT > 0)); then
        INC_VER=true
        URL="https://repo.spring.io/libs-snapshot"
    elif ((MILESTONE > 0)); then
        INC_VER=false
        URL="https://repo.spring.io/libs-milestone-local"
    else
        INC_VER=false
        URL="https://repo.maven.apache.org/maven2"
    fi

    GROUP_ID=$(echo "$DEP" | awk -F":" '{split($0,a); print a[1]}')
    ARTIFACT_ID=$(echo "$DEP" | awk -F":" '{split($0,a); print a[2]}')
    VERSION=$(echo "$DEP" | awk -F":" '{split($0,a); print a[3]}')
    echo "Dependency: groupId: $GROUP_ID, artifactId: $ARTIFACT_ID, version: $VERSION"
    TS=
    if [ "$INC_VER" == "true" ]; then
        DEP_PATH="${DEP//\:/\/}"
        META_DATA="$URL/${GROUP_ID//\./\/}/$ARTIFACT_ID/$VERSION/maven-metadata.xml"
        echo "Reading $META_DATA"
        rm -f ./maven-metadata.xml
        wget -q -O maven-metadata.xml "$META_DATA"
        RC=$?
        if ((RC > 0)); then
            echo "Error downloading $META_DATA. Exit code $RC"
            exit $RC
        fi
        TS=$(xmllint --xpath "/metadata/versioning/snapshot/timestamp/text()" maven-metadata.xml)
        RC=$?
        if ((RC > 0)); then
            echo "Error extracting timestamp. Exit code $RC"
            exit $RC
        fi
        DS="${TS:0:4}-${TS:4:2}-${TS:6:2} ${TS:9:2}:${TS:11:2}:${TS:13:2}"
        VAL=$(xmllint --xpath "/metadata/versioning/snapshotVersions/snapshotVersion[1]/value/text()" maven-metadata.xml)
        RC=$?
        if ((RC > 0)); then
            echo "Error extracting build number. Exit code $RC"
            exit $RC
        fi
        EXT=$(xmllint --xpath "/metadata/versioning/snapshotVersions/snapshotVersion[1]/extension/text()" maven-metadata.xml)
        RC=$?
        if ((RC > 0)); then
            echo "Error extracting extension. Exit code $RC"
            exit $RC
        fi
        SOURCE="$URL/${GROUP_ID//\./\/}/$ARTIFACT_ID/$VERSION/${ARTIFACT_ID}-${VAL}.${EXT}"

    else
        EXT="jar"
        SOURCE="$URL/${GROUP_ID//\./\/}/$ARTIFACT_ID/$VERSION/${ARTIFACT_ID}-${VERSION}.${EXT}"
    fi
    mkdir -p $TARGET
    TARGET_FILE="${TARGET}/${ARTIFACT_ID}-${VERSION}.${EXT}"
    if [ "$TS" != "" ] && [ "$DS" != "" ] && [ -f "$TARGET_FILE" ]; then
        FD=$(date -r "$TARGET_FILE" +"%Y-%m-%d %H:%M:%S")
        if [ "$FD" == "$DS" ]; then
            echo "$TARGET_FILE has same timestamp ($FD) as $SOURCE."
            echo "Skipping download"
            return 0
        fi
    fi
    echo "Downloading to $TARGET_FILE from $SOURCE"
    wget -q -O "$TARGET_FILE" "$SOURCE"
    RC=$?
    if ((RC > 0)); then
        echo "Error downloading $SOURCE. Exit code $RC"
        exit $RC
    fi
    if [ "$TS" != "" ] && [ "$DS" != "" ]; then
        touch -d "$DS" "$TARGET_FILE"
    fi
    set -e
}

set -e
download_deps "org.springframework.cloud:spring-cloud-skipper-server:$VER" $ROOT_DIR/spring-cloud-skipper-server/target
