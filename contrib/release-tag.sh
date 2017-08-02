function exit_with_usage {
  cat << EOF
usage: tag-release.sh
Tags a release on a particular branch.

Inputs are specified with the following environment variables:
GIT_BRANCH - Git branch on which to make a release
RELEASE_VERSION - Version used in pom files for release
RELEASE_TAG - Name of release tag
NEXT_VERSION - Development version after release
EOF
  exit 1
}


for env in RELEASE_VERSION RELEASE_TAG NEXT_VERSION GIT_BRANCH; do
  if [ -z "${!env}" ]; then
    echo "$env must be set to run this script"
    exit 1
  fi
done

MVN="mvn"

# Create release version
$MVN versions:set -DnewVersion=$RELEASE_VERSION | grep -v "no value" # silence logs

# Set version in datavec-spark-inference-parent/datavec-spark-inference-client/pom.xml
sed -i".tmp1" -Ee 's/<version>([0-9.]+)_(spark_[0-9])(-SNAPSHOT)?/<version>'"$RELEASE_VERSION"'_\2/g' datavec-spark-inference-parent/datavec-spark-inference-client/pom.xml
sed -i".tmp1" -Ee 's/<version>([0-9.]+)_(spark_[0-9])(-SNAPSHOT)?/<version>'"$RELEASE_VERSION"'_\2/g' datavec-spark-inference-parent/datavec-spark-inference-server/pom.xml

git commit -a -m "Preparing release $RELEASE_TAG"
echo "Creating tag $RELEASE_TAG at the head of $GIT_BRANCH"
git tag $RELEASE_TAG

# Create next version
$MVN versions:set -DnewVersion=$NEXT_VERSION | grep -v "no value" # silence logs
sed -i".tmp1" -Ee 's/<version>'"$RELEASE_VERSION"'_(spark_[0-9])/<version>'"${NEXT_VERSION%-SNAPSHOT}"'_\1-SNAPSHOT/g' datavec-spark-inference-parent/datavec-spark-inference-client/pom.xml
sed -i".tmp1" -Ee 's/<version>'"$RELEASE_VERSION"'_(spark_[0-9])/<version>'"${NEXT_VERSION%-SNAPSHOT}"'_\1-SNAPSHOT/g' datavec-spark-inference-parent/datavec-spark-inference-server/pom.xml

git commit -a -m "Preparing development version $NEXT_VERSION"
